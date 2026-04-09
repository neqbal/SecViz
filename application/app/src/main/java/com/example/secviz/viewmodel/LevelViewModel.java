package com.example.secviz.viewmodel;

import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.secviz.data.AsmLine;
import com.example.secviz.data.CodeLine;
import com.example.secviz.data.Level;
import com.example.secviz.data.RegisterSnapshot;
import com.example.secviz.data.StackBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LevelViewModel extends ViewModel {

    // Kept for StackCanvasView.setStep() and RegisterSnapshot compat
    private final MutableLiveData<Float> _step = new MutableLiveData<>(0f);
    public  final LiveData<Float>         step  = _step;

    // NEW: replaces _step==40 check in Fragment for showing input UI
    private final MutableLiveData<Boolean> _waitingForInput = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         waitingForInput  = _waitingForInput;

    private final MutableLiveData<Integer> _activeLineIndex = new MutableLiveData<>(0);
    public  final LiveData<Integer>         activeLineIndex  = _activeLineIndex;

    private final MutableLiveData<List<StackBlock>> _stack = new MutableLiveData<>();
    public  final LiveData<List<StackBlock>>          stack  = _stack;

    private final MutableLiveData<Integer> _espIndex = new MutableLiveData<>(0);
    public  final LiveData<Integer>         espIndex  = _espIndex;

    private final MutableLiveData<Integer> _ebpIndex = new MutableLiveData<>(0);
    public  final LiveData<Integer>         ebpIndex  = _ebpIndex;

    private final MutableLiveData<String> _statusTitle = new MutableLiveData<>("Program Ready");
    public  final LiveData<String>         statusTitle  = _statusTitle;

    private final MutableLiveData<String> _statusDesc = new MutableLiveData<>("Click 'Next Step' to start execution.");
    public  final LiveData<String>         statusDesc  = _statusDesc;

    private final MutableLiveData<String> _statusType = new MutableLiveData<>("info");
    public  final LiveData<String>         statusType  = _statusType;

    private final MutableLiveData<List<Pair<String,Boolean>>> _consoleOut = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<Pair<String,Boolean>>>          consoleOut  = _consoleOut;

    private final MutableLiveData<Boolean> _isPatched = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         isPatched   = _isPatched;

    private final MutableLiveData<Pair<String,Boolean>> _toast = new MutableLiveData<>();
    public  final LiveData<Pair<String,Boolean>>          toast  = _toast;

    private final MutableLiveData<String> _userInput = new MutableLiveData<>("");
    public  final LiveData<String>         userInput  = _userInput;

    private final MutableLiveData<List<Object[]>> _hexRows = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<Object[]>>          hexRows  = _hexRows;

    private final MutableLiveData<List<RegisterSnapshot>> _snapshots = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<RegisterSnapshot>>          snapshots  = _snapshots;
    private int snapshotCount = 0;

    private final MutableLiveData<Integer> _activeSnapshotIndex = new MutableLiveData<>(-1);
    public  final LiveData<Integer>         activeSnapshotIndex  = _activeSnapshotIndex;

    private final MutableLiveData<Boolean> _captureEnabled = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         captureEnabled  = _captureEnabled;

    private final MutableLiveData<Boolean> _timelineVisible = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         timelineVisible  = _timelineVisible;

    private final MutableLiveData<Boolean> _getsReached = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         getsReached  = _getsReached;

    // NEW: live simulated registers for the auto-updating register panel
    private final MutableLiveData<Map<String,String>> _simRegs = new MutableLiveData<>();
    public  final LiveData<Map<String,String>>          simRegs  = _simRegs;

    // ── Assembly state ───────────────────────────────────────────────────────
    private List<AsmLine>       asmFlat         = new ArrayList<>();
    private final Map<String,Integer> addrToAsmIdx    = new HashMap<>();
    private final Map<Integer,String> srcLineToFirstAddr = new HashMap<>();
    private final Map<String,Integer> addrToSrcLine   = new HashMap<>();

    /** Index of the NEXT instruction to execute (currently highlighted). -1 = not ready. */
    private int pendingAsmIdx = -1;

    private final MutableLiveData<Integer> _activeAsmInstrIdx = new MutableLiveData<>(-1);
    public  final LiveData<Integer>         activeAsmInstrIdx  = _activeAsmInstrIdx;

    // ── Execution state ──────────────────────────────────────────────────────
    private boolean execWaiting  = false; // local mirror of _waitingForInput
    private boolean isTerminal   = false; // program has ended

    // ── Simulated register strings kept for ROP win-condition check ─────────
    private String rop_rax = "0x0000000000000000";
    private String rop_rdi = "0x0000000000000000";
    private String rop_rsi = "0x0000000000000000";
    private String rop_rdx = "0x0000000000000000";
    private String rop_rip = "main";
    private List<String> ropPayload  = new ArrayList<>();
    private int ropPayloadPos = 0;
    private int ropEspIdx     = -1;
    private int ropHopCount   = 0;

    // ── General-purpose register simulation (display only) ────────────────────
    // Tracks all 9 visible registers by parsing asm instruction text.
    // Does NOT affect execution flow — purely for the register panel.
    private final Map<String, Long> simGPR = new LinkedHashMap<>();

    // ── Level data ───────────────────────────────────────────────────────────
    private Level level;
    private int   initialEsp = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // Init / Reset
    // ─────────────────────────────────────────────────────────────────────────

    public void toggleCaptureEnabled() {
        _captureEnabled.setValue(!Boolean.TRUE.equals(_captureEnabled.getValue()));
    }
    public void toggleTimelineVisible() {
        _timelineVisible.setValue(!Boolean.TRUE.equals(_timelineVisible.getValue()));
    }

    public void init(Level lvl) {
        this.level = lvl;
        this.initialEsp = findInitialEsp(lvl);
        buildAsmMaps();
        reset();
    }

    private int findInitialEsp(Level lvl) {
        for (int i = 0; i < lvl.initialStack.size(); i++) {
            String lbl = lvl.initialStack.get(i).label;
            if (lbl.contains("main Saved EBP") || lbl.contains("main Frame Data")) return i;
        }
        return 0;
    }

    public void reset() {
        _step.setValue(0f);
        _waitingForInput.setValue(false);
        execWaiting = false;
        isTerminal  = false;
        _activeLineIndex.setValue(level.startCodeLine);
        _stack.setValue(deepCopyStack(level.initialStack));
        updateHexDump();
        _espIndex.setValue(initialEsp);
        _ebpIndex.setValue(initialEsp);
        _statusTitle.setValue("Program Ready");
        _statusDesc.setValue("Highlighted instruction is NEXT to execute. Press ni or n.");
        _statusType.setValue("info");
        _consoleOut.setValue(new ArrayList<>());
        _userInput.setValue("");
        _isPatched.setValue(Boolean.TRUE.equals(_isPatched.getValue()));
        _activeSnapshotIndex.setValue(-1);
        snapshotCount = 0;
        _snapshots.setValue(new ArrayList<>());
        rop_rax = "0x0000000000000000";
        rop_rdi = "0x0000000000000000";
        rop_rsi = "0x0000000000000000";
        rop_rdx = "0x0000000000000000";
        rop_rip = "main";
        ropPayload.clear();
        ropPayloadPos = 0;
        ropEspIdx     = -1;
        ropHopCount   = 0;
        initSimGPR();  // reset register display state
        recordSnapshot("Program Ready", "main");
        // Restore asm cursor to start line
        pendingAsmIdx = -1;
        _activeAsmInstrIdx.setValue(-1);
        syncPendingToSourceLine(level.startCodeLine);
        emitSimRegs();
    }

    public void setUserInput(String input) { _userInput.setValue(input); }
    public void togglePatch()   { _isPatched.setValue(!Boolean.TRUE.equals(_isPatched.getValue())); }
    public boolean isPatched()  { return Boolean.TRUE.equals(_isPatched.getValue()); }
    public Level   getLevel()   { return level; }
    public float   getStep()    { Float v = _step.getValue(); return v == null ? 0 : v; }
    public List<StackBlock> getStack() { return _stack.getValue(); }
    public List<AsmLine>    getAsmFlat() { return asmFlat; }

    // ─────────────────────────────────────────────────────────────────────────
    // GDB-style execution — ni (Next Instruction)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Execute the currently highlighted (pending) instruction, then advance the
     * highlight to the next instruction. The highlighted instruction is the NEXT
     * one to run — it has NOT executed yet.
     */
    public void handleNextInstruction() {
        if (execWaiting || isTerminal) return;
        if (pendingAsmIdx < 0 || pendingAsmIdx >= asmFlat.size()) return;

        AsmLine toExec = asmFlat.get(pendingAsmIdx);
        int nextIdx = dispatchAndGetNext(toExec);
        updateRegistersForInstr(toExec.rawText); // visual register update

        if (!execWaiting && !isTerminal) {
            nextIdx = skipToNextRealInstr(nextIdx);
            pendingAsmIdx = nextIdx;
            _activeAsmInstrIdx.setValue(pendingAsmIdx);
            syncSourceToAsm();
            emitSimRegs();
        }
        recordSnapshot(_statusTitle.getValue() == null ? "" : _statusTitle.getValue(),
                pendingAsmIdx < asmFlat.size() ? asmFlat.get(pendingAsmIdx).address : "?");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GDB-style execution — n (Next Source Line)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Execute all assembly instructions belonging to the currently highlighted
     * C source line, then advance the C source highlight to the next line.
     */
    public void handleNextStep() {
        if (execWaiting || isTerminal) return;
        if (pendingAsmIdx < 0 || pendingAsmIdx >= asmFlat.size()) return;

        Integer curSrc = addrToSrcLine.get(asmFlat.get(pendingAsmIdx).address);

        // Execute until we land on a different (non-null) source line
        do {
            AsmLine toExec = asmFlat.get(pendingAsmIdx);
            // Track the mnemonic BEFORE dispatch (needed for gadget-territory break)
            String mnemBeforeDispatch = extractMnem(
                    toExec.rawText.toLowerCase(java.util.Locale.US));

            int nextIdx = dispatchAndGetNext(toExec);
            updateRegistersForInstr(toExec.rawText); // visual register update
            if (execWaiting || isTerminal) break;

            nextIdx = skipToNextRealInstr(nextIdx);
            if (nextIdx >= asmFlat.size()) { isTerminal = true; break; }

            pendingAsmIdx = nextIdx;
            _activeAsmInstrIdx.setValue(pendingAsmIdx);

            Integer newSrc = addrToSrcLine.get(asmFlat.get(pendingAsmIdx).address);
            // Stop when we arrive at a known, different source line
            if (newSrc != null && !newSrc.equals(curSrc)) break;
            // In gadget territory (no src line on either side): keep going until after
            // a ret instruction fires — that completes one full gadget and lands on the
            // next one. Stopping at pop rdi would leave the ret unexecuted.
            if (newSrc == null && curSrc == null && "ret".equals(mnemBeforeDispatch)) break;
        } while (!execWaiting && !isTerminal);

        if (!execWaiting) {
            syncSourceToAsm();
            emitSimRegs();
        }
        recordSnapshot(_statusTitle.getValue() == null ? "" : _statusTitle.getValue(),
                pendingAsmIdx < asmFlat.size() ? asmFlat.get(pendingAsmIdx).address : "?");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Instruction Dispatch Engine
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Apply the simulation effects of {@code instr} and return the index of the
     * next instruction to highlight (= the new pendingAsmIdx).
     * Returns {@code pendingAsmIdx} unchanged when execution must pause.
     */
    private int dispatchAndGetNext(AsmLine instr) {
        String raw = instr.rawText.toLowerCase(java.util.Locale.US);

        // ── CALL ─────────────────────────────────────────────────────────────
        if (instr.callTarget != null || raw.contains("call ")) {
            return handleCall(instr, raw);
        }

        String mnem = extractMnem(raw);
        if (mnem == null) return pendingAsmIdx + 1;

        switch (mnem) {
            case "push":
                if (raw.contains("rbp") || raw.contains("ebp")) simulatePushRbp();
                break;
            case "mov":
                if (raw.contains("rbp,rsp") || raw.contains("ebp,esp")) simulateMovRbpRsp();
                break;
            case "sub":
                if (raw.contains("rsp,") || raw.contains("esp,")) simulateSubRsp(raw);
                break;
            case "leave":
                simulateLeave();
                break;
            case "ret":
                return simulateRet();
            case "pop":
                simulatePop(raw);
                break;
            case "syscall":
                simulateSyscall();
                isTerminal = true;
                return pendingAsmIdx;
            default:
                break; // no effect, just advance
        }
        return pendingAsmIdx + 1;
    }

    private int handleCall(AsmLine instr, String raw) {
        // gets@plt (Levels 2B/3) — pause and show the payload/ROP dialog
        if (raw.contains("gets@plt") || raw.contains("<gets")) {
            execWaiting = true;
            _waitingForInput.setValue(true);
            _getsReached.setValue(true);  // ← triggers showGetsTriggerDialog in Fragment
            _step.setValue(4f);
            _statusTitle.setValue("gets() — Awaiting Input");
            _statusType.setValue("warn");
            _statusDesc.setValue("gets() reads from stdin with NO bounds check. Choose how to respond.");
            return pendingAsmIdx;
        }

        // read@plt (Levels 1/2A) — pause and show simple input layout (no payload dialog)
        if (raw.contains("read@plt") || raw.contains("<read")) {
            execWaiting = true;
            _waitingForInput.setValue(true);
            // Do NOT fire _getsReached — Fragment's waitingForInput observer shows layoutInput directly
            _step.setValue(4f);
            _statusTitle.setValue("read() — Awaiting Input");
            _statusType.setValue("warn");
            String desc = isPatched()
                    ? "read() is bounded to 16 bytes — safe input only."
                    : "read() reads up to 100 bytes into a 16-byte buffer. Overflow it!";
            if ("2a".equals(level.id)) {
                desc = isPatched()
                        ? "read() bounded to 15 bytes + null-terminator — secret protected."
                        : "read() reads 16 bytes with no null-terminator. Fill exactly 16 to leak the secret!";
            }
            _statusDesc.setValue(desc);
            return pendingAsmIdx;
        }

        // puts@plt → print quoted string from C source line to console
        if (raw.contains("puts@plt") || raw.contains("<puts")) {
            simulatePuts(raw);
            return pendingAsmIdx + 1;
        }

        // printf@plt
        if (raw.contains("printf@plt") || raw.contains("<printf")) {
            simulatePrintf();
            return pendingAsmIdx + 1;
        }

        // win() detection for Level 2B — reached via normal call before ret
        String target = instr.callTarget != null ? instr.callTarget : "";
        if (raw.contains("<win>") || target.equals("400476")) {
            simulateWin2b();
            isTerminal = true;
            return pendingAsmIdx;
        }

        // Local function call → enter it
        Integer targetIdx = addrToAsmIdx.get(target);
        if (targetIdx != null) {
            simulateCallPushReturnAddr();
            String funcName = extractFuncName(raw);
            _statusTitle.setValue("call " + funcName);
            _statusDesc.setValue("CPU pushes return address onto the stack and jumps to " + funcName + ".");
            _step.setValue(2f);
            return skipToNextRealInstr(targetIdx);
        }

        // Unknown PLT stub: skip (simulate transparent return)
        return pendingAsmIdx + 1;
    }

    // ── Instruction simulation helpers ────────────────────────────────────────

    private void simulatePushRbp() {
        int newEsp = getEspIdx() + 1;
        List<StackBlock> s = deepCopyStack(getStack());
        if (newEsp < s.size()) {
            StackBlock b = s.get(newEsp);
            // Only write to a truly empty slot — leave all pre-populated frame slots untouched.
            // This prevents main()'s push rbp from corrupting "main Ret Addr" (which already
            // holds the correct return-to-libc value and is needed by the ROP chain).
            boolean isEmpty = b.type.equals(StackBlock.TYPE_NEUTRAL)
                    || b.value.equals("0x0") || b.value.equals("0x...") || b.value.isEmpty();
            if (isEmpty) {
                b.value = formatHex(parseRbpValue());
                b.type  = StackBlock.TYPE_SAFE;
                _stack.setValue(s);
                updateHexDump();
            }
        }
        _espIndex.setValue(newEsp); // always track RSP movement
        _statusTitle.setValue("push rbp");
        _statusDesc.setValue("Saves caller's base pointer on the stack. RSP decrements by 8.");
    }

    private void simulateMovRbpRsp() {
        _ebpIndex.setValue(getEspIdx());
        _statusTitle.setValue("mov rbp, rsp");
        _statusDesc.setValue("Sets the new frame pointer equal to the current stack pointer.");
    }

    private void simulateSubRsp(String raw) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile(",0[xX]([0-9a-fA-F]+)").matcher(raw);
        if (m.find()) {
            int n      = (int) Long.parseLong(m.group(1), 16);
            int blocks = n / 8;
            _espIndex.setValue(Math.min(getEspIdx() + blocks, safeStackMax()));
            _statusTitle.setValue("sub rsp, 0x" + Integer.toHexString(n));
            _statusDesc.setValue("Allocates " + n + " bytes on the stack for local variables.");
        }
    }

    private void simulateLeave() {
        // leave = mov rsp,rbp ; pop rbp
        int ebp = getEbpIdx();
        _espIndex.setValue(ebp);        // mov rsp, rbp
        _espIndex.setValue(ebp - 1);    // pop rbp (RSP += 8 → espIndex--)
        _statusTitle.setValue("leave — restore stack frame");
        _statusDesc.setValue("mov rsp, rbp restores RSP. pop rbp restores the caller's frame pointer.");
        _step.setValue(5f);
    }

    private int simulateRet() {
        List<StackBlock> s = getStack();
        int esp = getEspIdx();
        if (s == null || esp < 0 || esp >= s.size()) { isTerminal = true; return pendingAsmIdx; }

        StackBlock retBlock = s.get(esp);
        String retVal  = retBlock.value;
        _espIndex.setValue(esp - 1); // pop = RSP += 8 = espIndex--

        // Normalize: strip "0x" prefix and leading zeros before lookup
        // (e.g. "0x000000000040046a" → "40046a").
        String retAddr = retVal.replace("0x","").replace("0X","")
                               .replaceAll("[^0-9a-fA-F]","").toLowerCase(java.util.Locale.US).trim();
        // Strip leading zeros so the key matches addrToAsmIdx (which stores bare hex)
        retAddr = retAddr.replaceFirst("^0+([0-9a-fA-F])", "$1");

        Integer targetFlatIdx = addrToAsmIdx.get(retAddr);

        if (targetFlatIdx == null) {
            // Corrupted / unknown address
            boolean corrupted = retBlock.type.equals(StackBlock.TYPE_DANGER)
                             || retBlock.type.equals(StackBlock.TYPE_JUNK)
                             || retBlock.type.equals(StackBlock.TYPE_TARGET)
                             || (!retVal.startsWith("0x") && !retVal.isEmpty());
            if (corrupted || !retVal.startsWith("0x")) {
                _statusTitle.setValue("Segmentation Fault!");
                _statusType.setValue("danger");
                _statusDesc.setValue("RIP → " + retVal + " is not valid. SIGSEGV.");
                addConsole("[1]    killed  Segmentation fault", false);
                if (level != null && "CRASH".equals(level.goal)) triggerWin(false);
                _step.setValue(100f);
                isTerminal = true;
                return pendingAsmIdx;
            }
            isTerminal = true;
            _step.setValue(100f);
            _statusTitle.setValue("Program exited");
            return pendingAsmIdx;
        }

        // Successful jump — determine context
        if ("2b".equals(level.id) && retAddr.equals("400476")) {
            // Level 2B: jumped to win()!
            _statusTitle.setValue("RIP hijacked → win()!");
            _statusType.setValue("success");
            _statusDesc.setValue("leave ; ret loaded 0x400476. Jumping to win().");
            _step.setValue(6f);
        } else if (!ropPayload.isEmpty()) {
            // Level 3: ROP chain hop
            _statusTitle.setValue("ret → ROP gadget " + retVal);
            _statusType.setValue("info");
            _step.setValue(6f);
        } else {
            // Normal return
            _statusTitle.setValue("ret — returned to caller");
            _statusType.setValue("success");
            _statusDesc.setValue("Stack frame cleaned up. RIP = " + retVal + ".");
            _step.setValue(7f);
        }
        return skipToNextRealInstr(targetFlatIdx);
    }

    private void simulatePop(String raw) {
        List<StackBlock> s = getStack();
        int esp = getEspIdx();
        String val = (s != null && esp >= 0 && esp < s.size()) ? s.get(esp).value : "0x0";
        _espIndex.setValue(esp - 1);

        String regName = "";
        if (raw.contains("pop") && raw.contains("rax")) { rop_rax = val; regName = "RAX"; }
        else if (raw.contains("pop") && raw.contains("rdi")) { rop_rdi = val; regName = "RDI"; }
        else if (raw.contains("pop") && raw.contains("rsi")) { rop_rsi = val; regName = "RSI"; }
        else if (raw.contains("pop") && raw.contains("rdx")) { rop_rdx = val; regName = "RDX"; }
        // pop rbp: just advance esp, no register to update in sim

        if (!regName.isEmpty()) {
            _statusTitle.setValue("pop " + regName.toLowerCase(java.util.Locale.US));
            _statusType.setValue("info");
            _statusDesc.setValue(regName + " ← " + val);
            // emit GDB console line for ROP chain visibility
            if (!ropPayload.isEmpty()) {
                String H = "────────────────────────────";
                ropHopCount++;
                addConsole("", false);
                addConsole(H + "[ Gadget #" + ropHopCount + " ]" + H, false);
                addConsole(" ►  " + asmFlat.get(pendingAsmIdx).address
                           + "    pop " + regName.toLowerCase(java.util.Locale.US), false);
                addConsole("   " + regName + " ← " + val, false);
            }
        }
    }

    private void simulateSyscall() {
        List<StackBlock> stk = deepCopyStack(getStack());
        execSyscall(pendingAsmIdx < asmFlat.size()
                ? "0x" + asmFlat.get(pendingAsmIdx).address : "0x40048e", stk);
        _step.setValue(100f);
    }

    private void simulateCallPushReturnAddr() {
        List<StackBlock> s = deepCopyStack(getStack());
        int newEsp = getEspIdx() + 1;
        // Return address = instruction right after the call
        int retInstIdx = skipToNextRealInstr(pendingAsmIdx + 1);
        String retAddr = retInstIdx < asmFlat.size() ? "0x" + asmFlat.get(retInstIdx).address : "0x0";
        if (newEsp < s.size()) {
            s.get(newEsp).value = retAddr;
            s.get(newEsp).type  = StackBlock.TYPE_SAFE;
        }
        _stack.setValue(s); updateHexDump();
        _espIndex.setValue(newEsp);
    }

    private void simulatePuts(String raw) {
        // Try to get the string from the corresponding C source line
        String curAddr = asmFlat.get(pendingAsmIdx).address;
        Integer srcLine = addrToSrcLine.get(curAddr);
        if (srcLine != null && level != null && srcLine < level.code.size()) {
            String lineText = level.code.get(srcLine).text;
            String quoted = extractQuoted(lineText);
            if (!quoted.isEmpty()) {
                addConsole(quoted, false);
                _statusTitle.setValue("puts()");
                _statusDesc.setValue("Printed: \"" + quoted + "\"");
                return;
            }
        }
        // win() puts for Level 2B (RIP jumped to win, now executing puts inside it)
        if ("2b".equals(level.id) && (raw.contains("puts@plt") || raw.contains("<puts"))) {
            addConsole("SHELL OBTAINED — flag{ctrl_flow_h1jack}", false);
            _statusTitle.setValue("puts() — flag printed!");
            _statusType.setValue("success");
        }
    }

    private void simulatePrintf() {
        String input   = _userInput.getValue() == null ? "" : _userInput.getValue();
        boolean patched = isPatched();
        int maxLen = "2a".equals(level.id) ? (patched ? 15 : 16) : 100;
        String out = input.substring(0, Math.min(maxLen, input.length()));
        addConsole(out, false);
        boolean shouldLeak = !patched && input.length() >= 16;
        if ("2a".equals(level.id) && shouldLeak) addConsole("SUPER_SECRET_KEY", true);
        _statusTitle.setValue("printf()");
    }

    private void simulateWin2b() {
        addConsole("SHELL OBTAINED — flag{ctrl_flow_h1jack}", false);
        _statusTitle.setValue("win() — RIP hijack succeeded!");
        _statusType.setValue("success");
        _statusDesc.setValue("Execution jumped to win(). Flag printed!");
        _toast.setValue(new Pair<>("💥 Shell obtained! flag{ctrl_flow_h1jack}", false));
        _step.setValue(100f);
    }

    // ── Dispatch helpers ──────────────────────────────────────────────────────

    private String extractMnem(String raw) {
        int colon = raw.indexOf(':');
        if (colon < 0) return null;
        String rest = raw.substring(colon + 1).trim();
        String[] parts = rest.split("\\s{2,}");
        if (parts.length < 2) return null;
        String instrPart = parts[1].trim();
        int sp = instrPart.indexOf(' ');
        return (sp > 0 ? instrPart.substring(0, sp) : instrPart).trim();
    }

    private String extractFuncName(String raw) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("<([^>]+)>").matcher(raw);
        return m.find() ? m.group(1) : "function";
    }

    private int skipToNextRealInstr(int from) {
        while (from < asmFlat.size()) {
            AsmLine a = asmFlat.get(from);
            if (!a.isHeader && !a.address.isEmpty()) return from;
            from++;
        }
        return from;
    }

    private int getEspIdx() { Integer v = _espIndex.getValue(); return v == null ? 0 : v; }
    private int getEbpIdx() { Integer v = _ebpIndex.getValue(); return v == null ? 0 : v; }
    private int safeStackMax() {
        List<StackBlock> s = getStack(); return s == null ? 0 : s.size() - 1;
    }

    private long parseRbpValue() {
        List<StackBlock> s = getStack();
        int ebp = getEbpIdx();
        if (s != null && ebp >= 0 && ebp < s.size()) return parseAddr(s.get(ebp).address);
        return 0x7fff0L;
    }

    private String formatHex(long val) { return String.format("0x%x", val); }

    private void syncPendingToSourceLine(int srcIdx) {
        String addr = srcLineToFirstAddr.get(srcIdx);
        if (addr == null) return;
        Integer flatIdx = addrToAsmIdx.get(addr);
        if (flatIdx == null) return;
        pendingAsmIdx = flatIdx;
        _activeAsmInstrIdx.setValue(flatIdx);
    }

    private void syncSourceToAsm() {
        if (pendingAsmIdx < 0 || pendingAsmIdx >= asmFlat.size()) return;
        String addr   = asmFlat.get(pendingAsmIdx).address;
        Integer srcLine = addrToSrcLine.get(addr);
        if (srcLine != null) _activeLineIndex.setValue(srcLine);
    }

    /**
     * Per-instruction register override map.
     * Key = instruction address (bare hex, no 0x, e.g. "40047f").
     * Value = register map, keys must be upper-case: "RAX","RDI","RSI","RDX","RSP","RBP","RIP".
     *
     * When pendingAsmIdx points to an address present in this map, emitSimRegs() uses
     * those values instead of the default tracked values.
     *
     * Call this during level init or at any time to hardcode accurate register values.
     * Example:
     *   Map<String,String> regs = new LinkedHashMap<>();
     *   regs.put("RSP", "0x7ffc0");  regs.put("RBP", "0x7ffd0");
     *   viewModel.defineInstrRegs("40047f", regs);
     */
    private final Map<String, Map<String,String>> instrRegOverrides = new HashMap<>();

    public void defineInstrRegs(String hexAddr, Map<String,String> regs) {
        instrRegOverrides.put(hexAddr.toLowerCase(java.util.Locale.US).replace("0x",""), regs);
    }


    /**
     * Load pre-calculated register snapshots from a JSON string (asset file content).
     * Format: { "ADDRESS_HEX": { "RAX":"0x...", "RSP":"0x...", ... }, ... }
     * Keys beginning with '_' are treated as comments and ignored.
     * Call from LevelFragment after init(), passing contents of
     * assets/registers/<levelId>.json.
     */
    public void loadRegisterJson(String json) {
        if (json == null || json.isEmpty()) return;
        instrRegOverrides.clear();
        try {
            org.json.JSONObject root = new org.json.JSONObject(json);
            java.util.Iterator<String> addrs = root.keys();
            while (addrs.hasNext()) {
                String addr = addrs.next().toLowerCase(java.util.Locale.US);
                if (addr.startsWith("_")) continue;
                Object val = root.get(addr);
                if (!(val instanceof org.json.JSONObject)) continue;
                org.json.JSONObject regObj = (org.json.JSONObject) val;
                Map<String, String> regMap = new LinkedHashMap<>();
                java.util.Iterator<String> regs = regObj.keys();
                while (regs.hasNext()) {
                    String reg = regs.next().toUpperCase(java.util.Locale.US);
                    if (reg.startsWith("_")) continue; // skip _note, _comment
                    regMap.put(reg, regObj.getString(reg));
                }
                if (!regMap.isEmpty()) instrRegOverrides.put(addr, regMap);
            }
        } catch (Exception e) {
            android.util.Log.w("LevelVM", "loadRegisterJson error: " + e.getMessage());
        }
    }

    /** Push current simulated registers to LiveData for the auto-updating register panel. */
    private void emitSimRegs() {
        // Update RIP in simGPR from the current pending instruction
        if (pendingAsmIdx >= 0 && pendingAsmIdx < asmFlat.size()) {
            try { simGPR.put("rip", Long.parseUnsignedLong(asmFlat.get(pendingAsmIdx).address, 16)); }
            catch (Exception ignored) {}
        }

        Map<String, String> m = new LinkedHashMap<>();
        m.put("RAX", fmtGPR("rax"));
        m.put("RBX", fmtGPR("rbx"));
        m.put("RCX", fmtGPR("rcx"));
        m.put("RDX", fmtGPR("rdx"));
        m.put("RSI", fmtGPR("rsi"));
        m.put("RDI", fmtGPR("rdi"));
        m.put("RSP", fmtGPR("rsp"));
        m.put("RBP", fmtGPR("rbp"));
        m.put("RIP", fmtGPR("rip"));

        // Apply per-instruction overrides if the current address has hardcoded values
        if (pendingAsmIdx >= 0 && pendingAsmIdx < asmFlat.size()) {
            String curAddr = asmFlat.get(pendingAsmIdx).address;
            Map<String,String> overrides = instrRegOverrides.get(curAddr);
            if (overrides != null) m.putAll(overrides); // overrides win over simGPR
        }

        _simRegs.setValue(m);
    }

    private String fmtGPR(String reg) {
        return String.format("0x%x", simGPR.getOrDefault(reg, 0L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register evaluator: parses asm text and updates simGPR (display only)
    // ─────────────────────────────────────────────────────────────────────────

    private void initSimGPR() {
        simGPR.clear();
        // Initialize to zero; estimate initial RSP from top of the visual stack
        for (String r : new String[]{"rax","rbx","rcx","rdx","rsi","rdi","rsp","rbp","rip"})
            simGPR.put(r, 0L);
        if (level != null && level.initialStack != null && !level.initialStack.isEmpty()) {
            long topAddr = parseAddr(level.initialStack.get(0).address);
            // Before main()'s 'push rbp', RSP is one slot ABOVE the first block
            simGPR.put("rsp", topAddr + 8L);
            simGPR.put("rbp", topAddr + 8L);
        }
    }

    /**
     * Parse one asm instruction line and update simGPR accordingly.
     * Called for EVERY executed instruction — purely visual, does not affect
     * step engine, espIndex, or any execution state.
     */
    private void updateRegistersForInstr(String raw) {
        if (raw == null) return;
        raw = raw.toLowerCase(java.util.Locale.US);
        String mnem = extractMnem(raw);
        if (mnem == null) return;
        String ops = extractOperands(raw);

        switch (mnem) {
            case "mov": case "movzx": case "movsx": case "movabs": {
                if (ops == null) break;
                String[] p = ops.split(",", 2);
                if (p.length == 2) {
                    String dst = canonReg(p[0].trim());
                    if (dst != null) setGPR(dst, resolveOp(p[1].trim()));
                }
                break;
            }
            case "lea": {
                if (ops == null) break;
                String[] p = ops.split(",", 2);
                if (p.length == 2) {
                    String dst = canonReg(p[0].trim());
                    if (dst != null) setGPR(dst, resolveMemAddr(p[1].trim()));
                }
                break;
            }
            case "xor": {
                if (ops == null) break;
                String[] p = ops.split(",", 2);
                if (p.length == 2) {
                    String d = canonReg(p[0].trim()), s = canonReg(p[1].trim());
                    if (d != null && d.equals(s)) setGPR(d, 0L); // xor reg,reg = 0
                    else if (d != null) setGPR(d, gpr(d) ^ resolveOp(p[1].trim()));
                }
                break;
            }
            case "add": {
                if (ops == null) break;
                String[] p = ops.split(",", 2);
                if (p.length == 2) { String d = canonReg(p[0].trim()); if (d != null) setGPR(d, gpr(d) + resolveOp(p[1].trim())); }
                break;
            }
            case "sub": {
                if (ops == null) break;
                String[] p = ops.split(",", 2);
                if (p.length == 2) { String d = canonReg(p[0].trim()); if (d != null) setGPR(d, gpr(d) - resolveOp(p[1].trim())); }
                break;
            }
            case "and": {
                if (ops == null) break;
                String[] p = ops.split(",", 2);
                if (p.length == 2) { String d = canonReg(p[0].trim()); if (d != null) setGPR(d, gpr(d) & resolveOp(p[1].trim())); }
                break;
            }
            case "or": {
                if (ops == null) break;
                String[] p = ops.split(",", 2);
                if (p.length == 2) { String d = canonReg(p[0].trim()); if (d != null) setGPR(d, gpr(d) | resolveOp(p[1].trim())); }
                break;
            }
            case "push":
                setGPR("rsp", gpr("rsp") - 8);
                break;
            case "pop": {
                setGPR("rsp", gpr("rsp") + 8);
                // Also update the destination register from the stack if ops given
                // (exact value comes from simulatePop; here we just advance RSP)
                break;
            }
            case "leave":
                // leave = mov rsp, rbp ; pop rbp
                setGPR("rsp", gpr("rbp") + 8); // rsp = rbp (then +8 for pop rbp)
                // RBP value after leave is unknowable without memory model; leave unchanged
                break;
            case "call":
                setGPR("rsp", gpr("rsp") - 8); // push return address
                break;
            case "ret":
                setGPR("rsp", gpr("rsp") + 8); // pop return address
                break;
            default:
                break;
        }
    }

    /** Map any register name (any width) to its canonical 64-bit name. */
    private static String canonReg(String name) {
        switch (name.replaceAll("\\s.*","")) { // strip trailing junk like "ptr"
            case "rax": case "eax": case "ax": case "al": case "ah": return "rax";
            case "rbx": case "ebx": case "bx": case "bl": case "bh": return "rbx";
            case "rcx": case "ecx": case "cx": case "cl": case "ch": return "rcx";
            case "rdx": case "edx": case "dx": case "dl": case "dh": return "rdx";
            case "rsi": case "esi": case "si": case "sil":            return "rsi";
            case "rdi": case "edi": case "di": case "dil":            return "rdi";
            case "rsp": case "esp": case "sp": case "spl":            return "rsp";
            case "rbp": case "ebp": case "bp": case "bpl":            return "rbp";
            case "rip": case "eip":                                    return "rip";
            default: return null; // r8-r15, xmm, etc.
        }
    }

    /** Resolve an operand string to a long value using simGPR for register sources. */
    private long resolveOp(String op) {
        op = op.trim().replaceFirst("\\s*#.*$", "").trim(); // strip asm comments
        String reg = canonReg(op);
        if (reg != null) return gpr(reg);
        // Hex immediate: 0x...
        if (op.startsWith("0x")) {
            try { return Long.parseUnsignedLong(op.substring(2), 16); } catch (Exception e) {}
        }
        // Negative hex: -0x...
        if (op.startsWith("-0x")) {
            try { return -Long.parseUnsignedLong(op.substring(3), 16); } catch (Exception e) {}
        }
        // Decimal
        try { return Long.parseLong(op); } catch (Exception e) {}
        // Memory reference: try resolveMemAddr
        if (op.startsWith("[")) return resolveMemAddr(op);
        return 0L;
    }

    /** Resolve a memory-reference operand like [rbp-0x10] or [rip+0x2c35] to an address. */
    private long resolveMemAddr(String op) {
        op = op.trim().replaceFirst("\\s*#.*$", "").trim();
        if (op.startsWith("[") && op.endsWith("]"))
            op = op.substring(1, op.length() - 1).trim();
        // Find last +/- (index 0 means sign before the first token, skip that)
        int plus = op.lastIndexOf('+'), minus = op.lastIndexOf('-');
        int split = Math.max(plus, minus > 0 ? minus : -1); // ignore leading '-'
        if (split > 0) {
            String baseS = op.substring(0, split).trim();
            String offS  = op.substring(split).trim(); // "+-0x..."
            String baseReg = canonReg(baseS);
            long base = baseReg != null ? gpr(baseReg) : 0L;
            long off  = 0;
            try {
                offS = offS.replace("+","").trim();
                if (offS.startsWith("-0x")) off = -Long.parseUnsignedLong(offS.substring(3), 16);
                else if (offS.startsWith("0x")) off = Long.parseUnsignedLong(offS.substring(2), 16);
                else off = Long.parseLong(offS);
            } catch (Exception ignored) {}
            return base + off;
        }
        String reg = canonReg(op);
        return reg != null ? gpr(reg) : 0L;
    }

    /** Extract the operands string from an objdump asm line (everything after the mnemonic). */
    private String extractOperands(String rawLower) {
        int colon = rawLower.indexOf(':');
        if (colon < 0) return null;
        String rest = rawLower.substring(colon + 1).trim();
        String[] parts = rest.split("\\s{2,}");
        if (parts.length < 2) return null;
        String instrPart = parts[1].trim();
        int sp = instrPart.indexOf(' ');
        if (sp < 0) return null;
        String ops = instrPart.substring(sp + 1).trim();
        // Strip inline asm comments (# ...) and ← annotations
        ops = ops.replaceFirst("\\s*[#←].*$", "").trim();
        return ops.isEmpty() ? null : ops;
    }

    private long gpr(String reg) { return simGPR.getOrDefault(reg, 0L); }
    private void setGPR(String reg, long val) { simGPR.put(reg, val); }

    // ─────────────────────────────────────────────────────────────────────────
    // Input / Payload submission — clears waitingForInput and resumes execution
    // ─────────────────────────────────────────────────────────────────────────

    /** Level 1 / 2A: user typed a string into the read() prompt. */
    public void submitInput(String input) {
        _getsReached.setValue(false);
        _userInput.setValue(input);
        boolean patched = isPatched();

        String processed = input;
        if (patched) {
            int max = "2a".equals(level.id) ? 15 : 16;
            if (processed.length() > max) processed = processed.substring(0, max);
        }

        List<StackBlock> newStack = deepCopyStack(getStack());
        int bufferStartIdx   = findBlockIndex(newStack, "buff[0");
        int currentIdx       = bufferStartIdx;
        boolean hasOverflowed = false;
        String remaining      = processed;

        while (!remaining.isEmpty() && currentIdx >= 0) {
            String chunk = remaining.substring(0, Math.min(8, remaining.length()));
            remaining = remaining.substring(chunk.length());
            StackBlock block = newStack.get(currentIdx);
            if (!block.label.startsWith("buff")) {
                hasOverflowed = true;
                block.type = StackBlock.TYPE_DANGER;
                if (!block.label.contains("CORRUPT")) block.label += " (CORRUPTED)";
            } else { block.type = StackBlock.TYPE_FILLED; }
            block.value = chunk;
            currentIdx--;
            if ((patched || "2a".equals(level.id)) && currentIdx < bufferStartIdx - 1) break;
        }

        _stack.setValue(newStack); updateHexDump();
        _statusTitle.setValue(hasOverflowed ? "BUFFER OVERFLOW!" : "Input Received Safely");
        _statusType.setValue(hasOverflowed ? "danger" : "success");
        _statusDesc.setValue(hasOverflowed
                ? "Input overflowed the buffer into adjacent stack regions!"
                : "Input fit safely inside the buffer. No overflow.");
        _step.setValue(5f);

        // Resume execution past the call gets/read instruction
        resumeAfterGets();
    }

    /** Level 2B: payload builder submitted a crafted overflow targeting win(). */
    public void submitPayload(int junkStart, int junkEnd, int targetIdx, String address) {
        _getsReached.setValue(false);
        List<StackBlock> s = deepCopyStack(getStack());
        for (int i = junkStart; i <= junkEnd; i++) {
            if (i < s.size()) { s.get(i).type = StackBlock.TYPE_JUNK; s.get(i).value = "AAAAAAAA"; }
        }
        if (targetIdx >= 0 && targetIdx < s.size()) {
            s.get(targetIdx).type  = StackBlock.TYPE_TARGET;
            s.get(targetIdx).value = address;
        }
        _stack.setValue(s); updateHexDump();
        _statusTitle.setValue("BUFFER OVERFLOW!");
        _statusType.setValue("danger");
        _statusDesc.setValue("buf overflowed into saved RBP and return address! RIP → " + address + " on ret.");
        _step.setValue(5f);
        resumeAfterGets();
    }

    /** Level 2B: user chose 'Enter Normal Value' from the gets dialog. */
    public void submitNormalInput2b(String input) {
        _getsReached.setValue(false);
        _userInput.setValue(input);
        List<StackBlock> s    = deepCopyStack(getStack());
        int currentIdx        = 7;
        String remaining      = input;
        boolean overflowed    = false;
        while (!remaining.isEmpty() && currentIdx >= 0) {
            String chunk = remaining.substring(0, Math.min(8, remaining.length()));
            remaining = remaining.substring(chunk.length());
            StackBlock block = s.get(currentIdx);
            if (!block.label.startsWith("buf")) {
                overflowed = true;
                block.type = StackBlock.TYPE_DANGER;
                if (!block.label.contains("CORRUPT")) block.label += " (CORRUPTED)";
            } else { block.type = StackBlock.TYPE_FILLED; }
            block.value = chunk;
            currentIdx--;
        }
        _stack.setValue(s); updateHexDump();
        _statusTitle.setValue(overflowed ? "BUFFER OVERFLOW!" : "Input Received");
        _statusType.setValue(overflowed ? "danger" : "success");
        _statusDesc.setValue(overflowed
                ? "Input overflowed buf[] into adjacent stack regions!"
                : "Input fit safely inside buf[]. No overflow.");
        _step.setValue(5f);
        resumeAfterGets();
    }

    /** Level 3: NX demo — shellcode attempt that will SIGSEGV. */
    public void submitNxDemoPayload() {
        _getsReached.setValue(false);
        _statusTitle.setValue("Shellcode on Stack — Returning...");
        _statusType.setValue("danger");
        _statusDesc.setValue("NX Demo: saved return address overwritten with 0x7ffd8000 (stack addr). "
                + "vuln() will try to execute stack code — NX prevents it.");
        List<StackBlock> stk = deepCopyStack(getStack());
        markJunk(stk, 3, 7);
        if (stk.size() > 2) { stk.get(2).value = "0x7ffd8000"; stk.get(2).type = StackBlock.TYPE_DANGER; }
        _stack.setValue(stk); updateHexDump();
        _step.setValue(5f);
        resumeAfterGets();
    }

    /** Level 3: ROP editor submitted a chain. */
    public void submitRopPayload(List<String> payloadAddresses) {
        _getsReached.setValue(false);
        if (payloadAddresses == null || payloadAddresses.isEmpty()) return;

        List<StackBlock> stk = deepCopyStack(getStack());
        markJunk(stk, 3, 7);

        int insertIdx = 2;
        for (String addr : payloadAddresses) {
            if (insertIdx >= 0) {
                stk.get(insertIdx).value = addr;
                stk.get(insertIdx).type  = StackBlock.TYPE_TARGET;
                insertIdx--;
            } else {
                long topAddr = parseAddr(stk.get(0).address) + 8L;
                StackBlock nb = new StackBlock(
                        "0x" + Long.toHexString(topAddr), "ROP", addr, 8, StackBlock.TYPE_TARGET);
                stk.add(0, nb);
                Integer ce = _espIndex.getValue(), cb = _ebpIndex.getValue();
                if (ce != null) _espIndex.setValue(ce + 1);
                if (cb != null) _ebpIndex.setValue(cb + 1);
                // DO NOT increment insertIdx — it stays negative to mark all
                // subsequent addresses as prepend-needed
            }
        }
        _stack.setValue(stk); updateHexDump();

        ropPayload    = new ArrayList<>(payloadAddresses);
        ropPayloadPos = 0;
        ropEspIdx     = insertIdx + 1;

        _statusTitle.setValue("ROP Payload Injected!");
        _statusType.setValue("danger");
        _statusDesc.setValue("Return address overwritten with " + ropPayload.get(0)
                + ". Press n/ni to execute leave;ret and start hopping gadgets.");
        _step.setValue(5f);
        resumeAfterGets();
    }

    public void consumeGetsReached() { _getsReached.setValue(false); }

    /** Clears the waiting state and advances pendingAsmIdx past the call gets/read. */
    private void resumeAfterGets() {
        execWaiting = false;
        _waitingForInput.setValue(false);
        int nextIdx = skipToNextRealInstr(pendingAsmIdx + 1);
        pendingAsmIdx = nextIdx;
        _activeAsmInstrIdx.setValue(pendingAsmIdx);
        syncSourceToAsm();
        emitSimRegs();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Win condition helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void triggerWin(boolean defended) {
        _toast.setValue(defended
                ? new Pair<>("🛡️ Defended! The patch successfully blocked the exploit.", true)
                : new Pair<>("💥 Exploit succeeded! Swap the read syscall to defend.", false));
    }

    /** execve syscall win check — reused from Level 3 ROP engine. */
    private void execSyscall(String ripAddr, List<StackBlock> stk) {
        boolean win = false;
        try {
            long raxV     = parseAddr(rop_rax);
            long rdiV     = parseAddr(rop_rdi);
            long rsiV     = parseAddr(rop_rsi);
            long rdxV     = parseAddr(rop_rdx);
            long binshAddr = resolveBinshAddr();
            win = (raxV == 59 && rdiV == binshAddr && rsiV == 0 && rdxV == 0);
        } catch (Exception ignored) {}

        if (win) {
            _statusTitle.setValue("execve(\"/bin/sh\", 0, 0) — Shell!");
            _statusType.setValue("success");
            _statusDesc.setValue(
                    "syscall #59 executed — all args correct:\n"
                    + "  RAX = 59  (execve)\n"
                    + "  RDI = " + rop_rdi + "  (→ /bin/sh)\n"
                    + "  RSI = 0   (argv = NULL)\n"
                    + "  RDX = 0   (envp = NULL)\n"
                    + "NX bypassed — only existing binary code used.");
            addConsole("", false);
            addConsole("$ id", true);
            addConsole("uid=0(root) gid=0(root) groups=0(root)", false);
            addConsole("$ echo $0", true);
            addConsole("/bin/sh", false);
            addConsole("$ SHELL OBTAINED — flag{r0p_ch4in_nx_byp4ss}", false);
            _toast.setValue(new Pair<>("💥 Shell obtained via ROP!", true));
        } else {
            _statusTitle.setValue("syscall — Wrong Arguments");
            _statusType.setValue("danger");
            _statusDesc.setValue("syscall fired but args were wrong:\n"
                    + "  RAX = " + rop_rax + "  (need 59)\n"
                    + "  RDI = " + rop_rdi + "  (need " + String.format("0x%x", resolveBinshAddr()) + ")\n"
                    + "  RSI = " + rop_rsi + "  (need 0)\n"
                    + "  RDX = " + rop_rdx + "  (need 0)");
            addConsole("[1]  Killed  (bad syscall args)", false);
        }
        _step.setValue(100f);
    }

    private long resolveBinshAddr() {
        if (level != null) {
            for (com.example.secviz.data.RopGadget g : level.ropGadgets) {
                if ("/bin/sh".equals(g.alias)) {
                    try { return Long.parseUnsignedLong(g.address.replace("0x","").replace("0X",""), 16); }
                    catch (Exception ignored) {}
                }
            }
        }
        return 0x403010L;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // asm map builder
    // ─────────────────────────────────────────────────────────────────────────

    private void buildAsmMaps() {
        asmFlat.clear(); addrToAsmIdx.clear();
        srcLineToFirstAddr.clear(); addrToSrcLine.clear();
        if (level == null || level.objdump == null) return;

        java.util.regex.Pattern instrPat = java.util.regex.Pattern.compile("^\\s+([0-9a-fA-F]+):\\s+.*");
        java.util.regex.Pattern hdrPat   = java.util.regex.Pattern.compile("[0-9a-fA-F]{8,16}\\s+<[^>]+>:.*");
        java.util.regex.Pattern callPat  = java.util.regex.Pattern.compile("\\bcall\\s+([0-9a-fA-F]+)");

        for (String raw : level.objdump.split("\\n")) {
            int idx     = asmFlat.size();
            String trim = raw.trim();
            if (hdrPat.matcher(trim).matches()) {
                asmFlat.add(new AsmLine(idx, "", raw, true, null)); continue;
            }
            java.util.regex.Matcher m = instrPat.matcher(raw);
            if (m.matches()) {
                String addr = m.group(1).toLowerCase(java.util.Locale.US);
                String callTarget = null;
                java.util.regex.Matcher cm = callPat.matcher(raw);
                if (cm.find()) callTarget = cm.group(1).toLowerCase(java.util.Locale.US);
                asmFlat.add(new AsmLine(idx, addr, raw, false, callTarget));
                addrToAsmIdx.put(addr, idx); continue;
            }
            asmFlat.add(new AsmLine(idx, "", raw, false, null));
        }

        if (level.code != null) {
            java.util.regex.Pattern addrPat = java.util.regex.Pattern.compile("^\\s+([0-9a-fA-F]+):");
            for (int srcIdx = 0; srcIdx < level.code.size(); srcIdx++) {
                CodeLine cl = level.code.get(srcIdx);
                if (cl.asm == null || cl.asm.isEmpty()) continue;
                for (String asmLine : cl.asm) {
                    java.util.regex.Matcher m2 = addrPat.matcher(asmLine);
                    if (m2.find()) {
                        String addr = m2.group(1).toLowerCase(java.util.Locale.US);
                        addrToSrcLine.put(addr, srcIdx);
                        if (!srcLineToFirstAddr.containsKey(srcIdx)) srcLineToFirstAddr.put(srcIdx, addr);
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility: stack, code, hex, console, snapshot
    // ─────────────────────────────────────────────────────────────────────────

    public void togglePatchAndReset() {
        togglePatch();
        // Don't reset — patch is visual only; user can Reset manually
    }

    private void addConsole(String text, boolean isGarbage) {
        List<Pair<String,Boolean>> list = new ArrayList<>(
                _consoleOut.getValue() == null ? new ArrayList<>() : _consoleOut.getValue());
        list.add(new Pair<>(text, isGarbage));
        _consoleOut.setValue(list);
    }

    private int findInCode(String substr, int fromIndex) {
        for (int i = fromIndex; i < level.code.size(); i++)
            if (level.code.get(i).text.contains(substr)) return i;
        return -1;
    }

    private int findClosingBraceAfter(int from) {
        for (int i = from + 1; i < level.code.size(); i++)
            if (level.code.get(i).text.trim().equals("}")) return i;
        return level.code.size() - 1;
    }

    private int findBlock(String labelStartsWith) {
        List<StackBlock> s = getStack(); if (s == null) return -1;
        for (int i = 0; i < s.size(); i++) if (s.get(i).label.startsWith(labelStartsWith)) return i;
        return -1;
    }

    private int findBlockIndex(List<StackBlock> stack, String labelPrefix) {
        for (int i = 0; i < stack.size(); i++) if (stack.get(i).label.startsWith(labelPrefix)) return i;
        return -1;
    }

    private void setBlockValue(List<StackBlock> stack, String labelPrefix, String value) {
        for (StackBlock b : stack) if (b.label.startsWith(labelPrefix)) { b.value = value; return; }
    }

    private String extractQuoted(String text) {
        int start = text.indexOf('"'), end = text.lastIndexOf('"');
        if (start >= 0 && end > start) return text.substring(start + 1, end);
        return "";
    }

    private List<StackBlock> deepCopyStack(List<StackBlock> source) {
        List<StackBlock> copy = new ArrayList<>();
        for (StackBlock b : source) copy.add(b.copy());
        return copy;
    }

    private void markJunk(List<StackBlock> stk, int startIdx, int endIdx) {
        for (int i = startIdx; i <= endIdx && i < stk.size(); i++) {
            stk.get(i).type  = StackBlock.TYPE_JUNK;
            stk.get(i).value = "0x4141414141414141";
        }
    }

    private long parseAddr(String addr) {
        try { return Long.parseUnsignedLong(addr.toLowerCase().replace("0x",""), 16); }
        catch (Exception e) { return 0L; }
    }

    @Nullable
    private String aliasForAddr(String val) {
        if (level == null || val == null) return null;
        for (com.example.secviz.data.RopGadget g : level.ropGadgets) {
            try {
                long ga = Long.parseUnsignedLong(g.address.toLowerCase().replace("0x",""), 16);
                long va = Long.parseUnsignedLong(val.toLowerCase().replace("0x",""), 16);
                if (ga == va) return g.alias;
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Nullable
    private String gadgetAsmForAddr(String addr) {
        if (level == null) return null;
        try {
            long target = Long.parseUnsignedLong(addr.toLowerCase().replace("0x",""), 16);
            for (com.example.secviz.data.RopGadget g : level.ropGadgets) {
                long ga = Long.parseUnsignedLong(g.address.toLowerCase().replace("0x",""), 16);
                if (ga == target) return g.asm;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Register Snapshot Recorder ────────────────────────────────────────────

    public void recordSnapshot(String label, String ripSymbol) {
        if (!Boolean.TRUE.equals(_captureEnabled.getValue())) return;
        List<StackBlock> s = getStack();
        String rsp = "—", rbp = "—";
        Integer espIdx = _espIndex.getValue(), ebpIdx = _ebpIndex.getValue();
        if (s != null && espIdx != null && espIdx >= 0 && espIdx < s.size()) rsp = s.get(espIdx).address;
        if (s != null && ebpIdx != null && ebpIdx >= 0 && ebpIdx < s.size()) rbp = s.get(ebpIdx).address;
        String rip = ripSymbol;
        Integer lineIdx = _activeLineIndex.getValue();
        if (lineIdx != null && level != null && lineIdx < level.code.size()) {
            List<String> asm = level.code.get(lineIdx).asm;
            if (asm != null && !asm.isEmpty()) {
                String first = asm.get(0).trim(); int colon = first.indexOf(':');
                if (colon > 0) rip = "0x" + first.substring(0, colon).trim();
            }
        }
        String statusType = _statusType.getValue() == null ? "info" : _statusType.getValue();
        List<RegisterSnapshot> current = _snapshots.getValue();
        int active = _activeSnapshotIndex.getValue() == null ? -1 : _activeSnapshotIndex.getValue();
        if (current != null && active >= 0 && active < current.size() - 1) {
            current = new ArrayList<>(current.subList(0, active + 1));
            snapshotCount = current.size();
        }
        List<RegisterSnapshot> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
        int codeLine = lineIdx == null ? 0 : lineIdx;
        float simStep = _step.getValue() == null ? 0f : _step.getValue();
        List<StackBlock> simStack = s == null ? new ArrayList<>() : deepCopyStack(s);
        int simEsp = espIdx == null ? 0 : espIdx, simEbp = ebpIdx == null ? 0 : ebpIdx;
        String simStatusTitle = _statusTitle.getValue() == null ? "" : _statusTitle.getValue();
        String simStatusDesc  = _statusDesc.getValue()  == null ? "" : _statusDesc.getValue();
        List<Pair<String,Boolean>> simConsole = _consoleOut.getValue() == null
                ? new ArrayList<>() : new ArrayList<>(_consoleOut.getValue());
        next.add(new RegisterSnapshot(snapshotCount++, label, rip, rsp, rbp, statusType,
                codeLine, simStep, simStack, simEsp, simEbp, simStatusTitle, simStatusDesc, simConsole));
        _snapshots.setValue(next);
        _activeSnapshotIndex.setValue(snapshotCount - 1);
    }

    public void restoreSnapshot(RegisterSnapshot snap) {
        _step.setValue(snap.simStep);
        _activeLineIndex.setValue(snap.codeLine);
        _stack.setValue(deepCopyStack(snap.simStack));
        _espIndex.setValue(snap.simEsp);
        _ebpIndex.setValue(snap.simEbp);
        _statusTitle.setValue(snap.simStatusTitle);
        _statusDesc.setValue(snap.simStatusDesc);
        _statusType.setValue(snap.statusType);
        _consoleOut.setValue(new ArrayList<>(snap.simConsole));
        updateHexDump();
        _activeSnapshotIndex.setValue(snap.stepIndex);
    }

    // ── Hex Dump Builder ──────────────────────────────────────────────────────

    private void updateHexDump() {
        List<StackBlock> s = getStack(); if (s == null) return;
        List<Object[]> rows = new ArrayList<>();
        for (int i = s.size() - 1; i >= 0; i--) {
            StackBlock block = s.get(i);
            if (block.type.equals(StackBlock.TYPE_MAIN_FRAME) && !block.label.contains("Return")) continue;
            String rawValue = block.value;
            byte[] blockBytes = new byte[8];
            int status = 0;
            if (!rawValue.equals("0x0") && !rawValue.equals("0x...") && !rawValue.isEmpty()) {
                if (block.type.equals(StackBlock.TYPE_DANGER)) status = 2;
                else if (block.type.equals(StackBlock.TYPE_FILLED) || block.type.equals(StackBlock.TYPE_WARN)
                        || block.type.equals(StackBlock.TYPE_SAFE)) status = 1;
                if (rawValue.startsWith("0x")) {
                    try {
                        long addr = Long.parseUnsignedLong(rawValue.substring(2), 16);
                        for (int b2 = 0; b2 < 8; b2++) { blockBytes[b2] = (byte)(addr & 0xFF); addr >>= 8; }
                        if (status == 0) status = 1;
                    } catch (NumberFormatException e) {
                        byte[] enc = rawValue.getBytes();
                        System.arraycopy(enc, 0, blockBytes, 0, Math.min(enc.length, 8));
                    }
                } else {
                    byte[] enc = rawValue.getBytes();
                    System.arraycopy(enc, 0, blockBytes, 0, Math.min(enc.length, 8));
                }
            }
            byte[] rowBytes = new byte[16]; int[] rowStatus = new int[16];
            System.arraycopy(blockBytes, 0, rowBytes, 0, 8);
            for (int j = 0; j < 8; j++) rowStatus[j] = status;
            rows.add(new Object[]{block.address, rowBytes, rowStatus});
        }
        _hexRows.setValue(rows);
    }
}
