package com.example.secviz.viewmodel;

import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.secviz.data.Level;
import com.example.secviz.data.RegisterSnapshot;
import com.example.secviz.data.StackBlock;

import java.util.ArrayList;
import java.util.List;

public class LevelViewModel extends ViewModel {

    // Step is a float to allow fractional steps (2.5, 3.5) like the web app
    private final MutableLiveData<Float> _step = new MutableLiveData<>(0f);
    public final LiveData<Float> step = _step;

    private final MutableLiveData<Integer> _activeLineIndex = new MutableLiveData<>(0);
    public final LiveData<Integer> activeLineIndex = _activeLineIndex;

    private final MutableLiveData<List<StackBlock>> _stack = new MutableLiveData<>();
    public final LiveData<List<StackBlock>> stack = _stack;

    private final MutableLiveData<Integer> _espIndex = new MutableLiveData<>(0);
    public final LiveData<Integer> espIndex = _espIndex;

    private final MutableLiveData<Integer> _ebpIndex = new MutableLiveData<>(0);
    public final LiveData<Integer> ebpIndex = _ebpIndex;

    private final MutableLiveData<String> _statusTitle = new MutableLiveData<>("Program Ready");
    public final LiveData<String> statusTitle = _statusTitle;

    private final MutableLiveData<String> _statusDesc = new MutableLiveData<>("Click 'Next Step' to start execution.");
    public final LiveData<String> statusDesc = _statusDesc;

    /** "info" | "success" | "warn" | "danger" */
    private final MutableLiveData<String> _statusType = new MutableLiveData<>("info");
    public final LiveData<String> statusType = _statusType;

    private final MutableLiveData<List<Pair<String, Boolean>>> _consoleOut = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Pair<String, Boolean>>> consoleOut = _consoleOut;

    private final MutableLiveData<Boolean> _isPatched = new MutableLiveData<>(false);
    public final LiveData<Boolean> isPatched = _isPatched;

    /** Fires a one-shot toast message. Pair<message, isSuccess> */
    private final MutableLiveData<Pair<String, Boolean>> _toast = new MutableLiveData<>();
    public final LiveData<Pair<String, Boolean>> toast = _toast;

    private final MutableLiveData<String> _userInput = new MutableLiveData<>("");
    public final LiveData<String> userInput = _userInput;

    /** Hex dump rows derived from the live stack. List of HexRow-like bundles as raw objects
     *  to avoid a circular import — encoded as Object[] { address:String, bytes:byte[], status:int[] } */
    private final MutableLiveData<List<Object[]>> _hexRows = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Object[]>> hexRows = _hexRows;

    /** Register state snapshots — one per executed step, for the timeline UI */
    private final MutableLiveData<List<RegisterSnapshot>> _snapshots =
            new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<RegisterSnapshot>> snapshots = _snapshots;
    private int snapshotCount = 0;

    /** The timeline index currently being viewed/simulated. Allows scrubbing time without deleting the future. */
    private final MutableLiveData<Integer> _activeSnapshotIndex = new MutableLiveData<>(-1);
    public final LiveData<Integer> activeSnapshotIndex = _activeSnapshotIndex;
    
    // Feature Toggles (from Burger Menu)
    private final MutableLiveData<Boolean> _captureEnabled = new MutableLiveData<>(false);
    public final LiveData<Boolean> captureEnabled = _captureEnabled;

    private final MutableLiveData<Boolean> _timelineVisible = new MutableLiveData<>(false);
    public final LiveData<Boolean> timelineVisible = _timelineVisible;

    /** Level 2B: fires true when execution reaches gets() — tells LevelFragment to show the dialog */
    private final MutableLiveData<Boolean> _getsReached = new MutableLiveData<>(false);
    public final LiveData<Boolean> getsReached = _getsReached;

    public void toggleCaptureEnabled() {
        _captureEnabled.setValue(!Boolean.TRUE.equals(_captureEnabled.getValue()));
    }

    public void toggleTimelineVisible() {
        _timelineVisible.setValue(!Boolean.TRUE.equals(_timelineVisible.getValue()));
    }

    private Level level;
    private int initialEsp = 0;

    // ── Level 3 ROP simulation state ────────────────────────────────────────
    private String rop_rdi = "0x0000000000000000";
    private String rop_rsi = "0x0000000000000000";
    private String rop_rdx = "0x0000000000000000";
    private String rop_rax = "0x0000000000000000";
    private String rop_rip = "main";
    // Stack pointer index used by the gadget-hopper (advances as we 'pop')
    private int ropEspIdx = -1;
    // The resolved payload addresses in order (filled by submitRopPayload)
    private List<String> ropPayload = new ArrayList<>();
    private int ropPayloadPos = 0; // current pos in ropPayload (RIP = ropPayload[ropPayloadPos])
    private int ropHopCount  = 0; // counts each gadget execution for the GDB "hop #N" header

    // ── Level Initialization ──────────────────────────────────────────────────

    public void init(Level lvl) {
        this.level = lvl;
        this.initialEsp = findInitialEsp(lvl);
        reset();
    }

    private int findInitialEsp(Level lvl) {
        for (int i = 0; i < lvl.initialStack.size(); i++) {
            String lbl = lvl.initialStack.get(i).label;
            if (lbl.contains("main Saved EBP") || lbl.contains("main Frame Data")) {
                return i;
            }
        }
        return 0;
    }

    public void reset() {
        _step.setValue(0f);
        _activeLineIndex.setValue(level.startCodeLine);
        _stack.setValue(deepCopyStack(level.initialStack));
        updateHexDump();
        _espIndex.setValue(initialEsp);
        _ebpIndex.setValue(initialEsp);
        _statusTitle.setValue("Program Ready");
        _statusDesc.setValue("Click 'Next Step' to start execution.");
        _statusType.setValue("info");
        _consoleOut.setValue(new ArrayList<>());
        _userInput.setValue("");
        _isPatched.setValue(Boolean.TRUE.equals(_isPatched.getValue())); // preserve patch state
        // Clear timeline
        _activeSnapshotIndex.setValue(-1);
        snapshotCount = 0;
        _snapshots.setValue(new ArrayList<>());
        // Reset ROP simulation registers
        rop_rdi = "0x0000000000000000";
        rop_rsi = "0x0000000000000000";
        rop_rdx = "0x0000000000000000";
        rop_rax = "0x0000000000000000";
        rop_rip = "main";
        ropPayload.clear();
        ropPayloadPos = 0;
        ropEspIdx = -1;
        ropHopCount = 0;
        recordSnapshot("Program Ready", "main");
    }

    public void setUserInput(String input) {
        _userInput.setValue(input);
    }

    public void togglePatch() {
        _isPatched.setValue(!Boolean.TRUE.equals(_isPatched.getValue()));
    }

    public boolean isPatched() {
        return Boolean.TRUE.equals(_isPatched.getValue());
    }

    public Level getLevel() {
        return level;
    }

    public float getStep() {
        Float v = _step.getValue();
        return v == null ? 0 : v;
    }

    public List<StackBlock> getStack() {
        return _stack.getValue();
    }

    // ── Step Execution Engine ─────────────────────────────────────────────────

    public void handleNextStep() {
        float currentStep = getStep();
        boolean patched = isPatched();
        String input = _userInput.getValue() == null ? "" : _userInput.getValue();

        // Level 2B has its own step engine
        if ("2b".equals(level.id)) {
            handle2bNextStep(currentStep);
            return;
        }

        // Level 3 has its own step engine
        if ("3".equals(level.id)) {
            handle3NextStep(currentStep);
            return;
        }

        switch ((int)(currentStep * 10)) { // multiply by 10 to handle 2.5→25, 3.5→35
            case 0:  step0(input, patched); break;
            case 10: step1(patched); break;
            case 20: step2(); break;
            case 25: step2_5(); break;
            case 30: step3(patched); break;
            case 35: step3_5(patched); break;
            // step 4 = waiting for input, handled by submitInput()
            case 50: step5(input, patched); break;
            case 60: step6(input, patched); break;
            case 70: step7(); break;
            case 80: step8(input, patched); break;
        }
    }

    private void step0(String input, boolean patched) {
        int nextIdx = level.startCodeLine + 1;
        _activeLineIndex.setValue(nextIdx);
        String lineText = level.code.get(nextIdx).text;
        if (lineText.contains("puts")) {
            addConsole("I will echo whatever you say.", false);
            _statusTitle.setValue("Executing puts()");
            _step.setValue(1f);
            recordSnapshot("puts() in main", "puts@plt");
        } else {
            _statusTitle.setValue("Calling vuln()");
            pushReturnAddr(patched);
            _step.setValue(2f);
            recordSnapshot("call vuln()", "vuln");
        }
    }

    private void step1(boolean patched) {
        int callIdx = findInCode("vuln();", level.startCodeLine);
        _activeLineIndex.setValue(callIdx);
        _statusTitle.setValue("Calling vuln()");
        pushReturnAddr(patched);
        _step.setValue(2f);
        recordSnapshot("call vuln()", "vuln");
    }

    private void pushReturnAddr(boolean patched) {
        List<StackBlock> s = deepCopyStack(getStack());
        int retIdx = initialEsp + 1;
        if (retIdx < s.size()) {
            s.get(retIdx).value = level.id.equals("1") ? "0x400490" : "0x4004ec";
        }
        _stack.setValue(s);
        updateHexDump();
        _espIndex.setValue(retIdx);
    }

    private void step2() {
        int firstVarIdx = -1;
        for (int i = 0; i < level.code.size(); i++) {
            String t = level.code.get(i).text;
            if (t.contains("char secret_key") || t.contains("long fs_canary") || t.contains("char buff")) {
                firstVarIdx = i;
                break;
            }
        }
        if (firstVarIdx == -1) return;
        _activeLineIndex.setValue(firstVarIdx);
        _statusTitle.setValue("vuln() Local Variables Allocation");

        List<StackBlock> s = deepCopyStack(getStack());
        int vulnEbpIdx = initialEsp + 2;
        if (vulnEbpIdx < s.size()) s.get(vulnEbpIdx).value = "0x7ffe4";
        _stack.setValue(s);
        updateHexDump();
        _ebpIndex.setValue(vulnEbpIdx);

        int buff0Idx = findBlock("buff[0..7]");
        _espIndex.setValue(buff0Idx >= 0 ? buff0Idx : _espIndex.getValue());

        boolean firstIsNotBuff = !level.code.get(firstVarIdx).text.contains("char buff");
        _step.setValue(firstIsNotBuff ? 2.5f : 3f);
        recordSnapshot("vuln() prologue", "vuln");
    }

    private void step2_5() {
        int buffIdx = findInCode("char buff", 0);
        if (buffIdx == -1) buffIdx = (_activeLineIndex.getValue() == null ? 0 : _activeLineIndex.getValue()) + 1;
        _activeLineIndex.setValue(buffIdx);
        _statusTitle.setValue("vuln() Array Allocation");

        List<StackBlock> s = deepCopyStack(getStack());
        if (level.id.equals("2a")) {
            setBlockValue(s, "secret_key[0..7]", "SUPER_SE");
            setBlockValue(s, "secret_key[8..15]", "CRET_KEY");
        }
        _stack.setValue(s);
        updateHexDump();
        _step.setValue(3f);
        recordSnapshot("alloc buff[]", "vuln");
    }

    private void step3(boolean patched) {
        int readIdx = findInCode("read(1, ", 0);
        int curLine = _activeLineIndex.getValue() == null ? 0 : _activeLineIndex.getValue();
        int putsInVulnIdx = -1;
        for (int i = curLine + 1; i < readIdx && i < level.code.size(); i++) {
            if (level.code.get(i).text.contains("puts")) { putsInVulnIdx = i; break; }
        }

        if (putsInVulnIdx != -1) {
            _activeLineIndex.setValue(putsInVulnIdx);
            String lineText = level.code.get(putsInVulnIdx).text;
            String printed = extractQuoted(lineText);
            addConsole(printed, false);
            _statusTitle.setValue("Executing puts()");
            _step.setValue(3.5f);
            recordSnapshot("puts() in vuln", "puts@plt");
        } else {
            _activeLineIndex.setValue(readIdx);
            if (level.id.equals("2a")) {
                _statusDesc.setValue(patched
                        ? "read() securely terminates the buffer."
                        : "read() bounds-check to 16 bytes but no null-terminator.");
            }
            _statusTitle.setValue("read() Execution");
            _step.setValue(4f);
            recordSnapshot("read() ← awaiting input", "read@plt");
        }
    }

    private void step3_5(boolean patched) {
        int readIdx = findInCode("read(1, ", 0);
        _activeLineIndex.setValue(readIdx);
        if (level.id.equals("2a")) {
            _statusDesc.setValue(patched
                    ? "read() securely terminates the buffer. The secret is completely safe!"
                    : "read() bounds-check to 16 bytes but no null-terminator. Fill exactly 16 bytes to leak the secret!");
        }
        _statusTitle.setValue("read() Execution");
        _step.setValue(4f);
        recordSnapshot("read() ← awaiting input", "read@plt");
    }

    public void submitInput(String input) {
        _userInput.setValue(input);
        boolean patched = isPatched();

        String processedInput = input;
        if (patched) {
            int maxLen = level.id.equals("2a") ? 15 : 16;
            if (processedInput.length() > maxLen) processedInput = processedInput.substring(0, maxLen);
        }

        List<StackBlock> newStack = deepCopyStack(getStack());
        int bufferStartIdx = findBlockIndex(newStack, "buff[0");
        int currentIdx = bufferStartIdx;
        boolean hasOverflowed = false;

        String remaining = processedInput;
        while (!remaining.isEmpty() && currentIdx >= 0) {
            String chunk = remaining.substring(0, Math.min(8, remaining.length()));
            remaining = remaining.substring(chunk.length());
            StackBlock block = newStack.get(currentIdx);
            if (!block.label.startsWith("buff")) {
                hasOverflowed = true;
                block.type = StackBlock.TYPE_DANGER;
                if (!block.label.contains("CORRUPT")) block.label += " (CORRUPTED)";
            } else {
                block.type = StackBlock.TYPE_FILLED;
            }
            block.value = chunk;
            currentIdx--;
            if ((patched || level.id.equals("2a")) && currentIdx < bufferStartIdx - 1) break;
        }

        _stack.setValue(newStack);
        updateHexDump();
        _statusTitle.setValue(hasOverflowed ? "BUFFER OVERFLOW!" : "Input Received Safely");
        _statusType.setValue(hasOverflowed ? "danger" : "success");
        _step.setValue(5f);
        recordSnapshot(hasOverflowed ? "OVERFLOW!" : "Input safe", "read@plt");
    }

    private void step5(String input, boolean patched) {
        int printfIdx = findInCode("printf", 0);
        if (printfIdx != -1) {
            _activeLineIndex.setValue(printfIdx);
            _statusTitle.setValue("Executing printf()");
            int maxLen = level.id.equals("2a") ? (patched ? 15 : 16) : 100;
            String out = input.substring(0, Math.min(maxLen, input.length()));
            addConsole(out, false);
            boolean shouldLeak = !patched && input.length() >= 16;
            if (level.id.equals("2a") && shouldLeak) addConsole("SUPER_SECRET_KEY", true);
        } else {
            int rIdx = findInCode("read(1, ", 0);
            int endIdx = findClosingBraceAfter(rIdx);
            _activeLineIndex.setValue(endIdx);
            _statusTitle.setValue("vuln() Epilogue");
        }
        _step.setValue(6f);
        recordSnapshot("printf() output", "printf@plt");
    }

    private void step6(String input, boolean patched) {
        _statusTitle.setValue("vuln() Epilogue");

        boolean returnedSafely = true;
        List<StackBlock> currentStack = getStack();
        StackBlock retBlock = null;
        for (StackBlock b : currentStack) {
            if (b.label.contains("Return Addr") && !b.label.contains("main")) {
                retBlock = b;
                break;
            }
        }
        if (!patched && input.length() > 16 && retBlock != null && retBlock.type.equals(StackBlock.TYPE_DANGER)) {
            returnedSafely = false;
        }

        if (!returnedSafely) {
            _statusType.setValue("danger");
            _statusTitle.setValue("Segmentation Fault!");
            _statusDesc.setValue("Execution crashed.");
            _step.setValue(100f);
            recordSnapshot("SIGSEGV", "0x????????");
            if (level.goal.equals("CRASH")) triggerWin(false);
        } else {
            if (patched && input.length() > 16 && level.goal.equals("CRASH")) {
                _statusDesc.setValue("Defended!");
                _statusType.setValue("success");
                triggerWin(true);
            }
            _espIndex.setValue(Math.max(0, initialEsp - 1));
            _ebpIndex.setValue(initialEsp);
            _step.setValue(7f);
            recordSnapshot("ret from vuln()", "main");
        }
    }

    private void step7() {
        int putsAfterVuln = -1;
        int vulnCallIdx = findInCode("vuln();", level.startCodeLine);
        for (int i = vulnCallIdx + 1; i < level.code.size(); i++) {
            if (level.code.get(i).text.contains("puts(")) { putsAfterVuln = i; break; }
        }

        if (putsAfterVuln != -1) {
            _activeLineIndex.setValue(putsAfterVuln);
            _statusTitle.setValue("Returned Safely to main()");
            addConsole("Goodbye!!!", false);
            _step.setValue(8f);
            recordSnapshot("puts() in main", "puts@plt");
        } else {
            int endMain = findClosingBraceAfter(level.startCodeLine);
            _activeLineIndex.setValue(endMain);
            _statusTitle.setValue("Program Exited");
            _step.setValue(100f);
            recordSnapshot("main() exited", "exit");
            checkLeakWin();
        }
    }

    private void step8(String input, boolean patched) {
        int endMain = findClosingBraceAfter(level.startCodeLine);
        _activeLineIndex.setValue(endMain);
        _statusTitle.setValue("Program Exited");
        _step.setValue(100f);
        recordSnapshot("main() exited", "exit");
        if (level.goal.equals("LEAK")) checkLeakWin();
    }

    private void checkLeakWin() {
        String input = _userInput.getValue() == null ? "" : _userInput.getValue();
        boolean patched = isPatched();
        if (!patched && input.length() >= 16) {
            triggerWin(false);
        } else if (patched && input.length() >= 16) {
            _statusDesc.setValue("Defended! The null-terminator safely blocked the secret from leaking!");
            _statusType.setValue("success");
            triggerWin(true);
        } else if (!patched) {
            _statusDesc.setValue("Program finished safely, but we didn't leak the secret. Try submitting exactly 16 bytes.");
            _statusType.setValue("warn");
        }
    }

    private void triggerWin(boolean defended) {
        if (defended) {
            _toast.setValue(new Pair<>("🛡️ Defended! The patch successfully blocked the exploit.", true));
        } else {
            _toast.setValue(new Pair<>("💥 Exploit succeeded! Swap the read syscall to defend.", false));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ── Level 2B Step Engine ──────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════════

    private void handle2bNextStep(float currentStep) {
        int s = (int)(currentStep * 10);
        switch (s) {
            case 0:  step2b_0(); break;   // Show main()
            case 10: step2b_1(); break;   // call vuln — push ret addr
            case 20: step2b_2(); break;   // vuln prologue + sub rsp,0x20
            case 30: step2b_3(); break;   // gets() — fire getsReached
            // 40 = waiting for gets result (submitPayload or submitNormalInput2b)
            case 50: step2b_5(); break;   // after payload applied — show overflow
            case 60: step2b_6(); break;   // ret / win() branch
            case 70: step2b_7(); break;   // win prints flag / program exits
        }
    }

    /** Step 0: Program ready — main() is already shown on load, jump straight to step 1. */
    private void step2b_0() {
        // main() is already highlighted by reset() via level.startCodeLine.
        // Calling step2b_1() directly means the FIRST Next Step press shows
        // "call vuln" instead of redundantly re-highlighting main().
        step2b_1();
    }

    /** Step 1: call vuln — push return address */
    private void step2b_1() {
        int callIdx = findInCode("vuln();", 0);
        _activeLineIndex.setValue(callIdx >= 0 ? callIdx : level.startCodeLine);
        _statusTitle.setValue("call vuln()");
        _statusDesc.setValue("CPU pushes the return address (0x4004ac) onto the stack, then jumps to vuln().");

        List<StackBlock> s = deepCopyStack(getStack());
        // Block index 2 = "vuln Return Addr" — mark as filled
        if (s.size() > 2) {
            s.get(2).value = "0x4004ac";
            s.get(2).type  = StackBlock.TYPE_SAFE;
        }
        _stack.setValue(s);
        updateHexDump();
        _espIndex.setValue(2); // RSP now points at vuln ret addr
        _step.setValue(2f);
        recordSnapshot("call vuln()", "vuln");
    }

    /** Step 2: vuln() prologue — push rbp, sub rsp,0x20 */
    private void step2b_2() {
        int vulnIdx = findInCode("void vuln() {", 0);
        _activeLineIndex.setValue(vulnIdx >= 0 ? vulnIdx : level.startCodeLine);
        _statusTitle.setValue("vuln() Prologue");
        _statusDesc.setValue("push rbp saves caller's frame. sub rsp,0x20 reserves 32 bytes for buf.");

        List<StackBlock> s = deepCopyStack(getStack());
        if (s.size() > 3) {
            s.get(3).value = "0x7fff8";
            s.get(3).type  = StackBlock.TYPE_SAFE;
        }
        _stack.setValue(s);
        updateHexDump();
        _ebpIndex.setValue(3); // RBP = saved main's RBP
        _espIndex.setValue(7); // RSP = bottom of buf
        _step.setValue(3f);
        recordSnapshot("vuln() prologue", "vuln");
    }

    /** Step 3: gets(buf) called — notify LevelFragment */
    private void step2b_3() {
        int getsIdx = findInCode("gets(buf);", 0);
        _activeLineIndex.setValue(getsIdx >= 0 ? getsIdx : level.startCodeLine);
        _statusTitle.setValue("gets() called");
        _statusDesc.setValue("gets() reads from stdin with NO bounds check. Choose how to respond.");
        _step.setValue(4f); // pause — wait for dialog result
        recordSnapshot("gets() awaiting input", "gets@plt");
        _getsReached.setValue(true); // signal LevelFragment
    }

    /** Called by LevelFragment when payload builder completes. */
    public void submitPayload(int junkStart, int junkEnd, int targetIdx, String address) {
        _getsReached.setValue(false);

        List<StackBlock> s = deepCopyStack(getStack());

        // Mark junk blocks
        for (int i = junkStart; i <= junkEnd; i++) {
            if (i < s.size()) {
                s.get(i).type  = StackBlock.TYPE_JUNK;
                s.get(i).value = "AAAAAAAA";
            }
        }

        // Mark target block
        if (targetIdx >= 0 && targetIdx < s.size()) {
            s.get(targetIdx).type  = StackBlock.TYPE_TARGET;
            s.get(targetIdx).value = address;
        }

        _stack.setValue(s);
        updateHexDump();

        _statusTitle.setValue("BUFFER OVERFLOW!");
        _statusType.setValue("danger");
        _statusDesc.setValue("buf overflowed into saved RBP and return address! RIP will jump to " + address + " on ret.");
        _step.setValue(5f);
        recordSnapshot("OVERFLOW — ret addr overwritten", "gets@plt");
    }

    /** Called when player chooses 'Enter Normal Value'. */
    public void submitNormalInput2b(String input) {
        _getsReached.setValue(false);
        _userInput.setValue(input);

        List<StackBlock> s = deepCopyStack(getStack());
        int currentIdx = 7; // buf[0..7]
        String remaining = input;
        boolean overflowed = false;

        while (!remaining.isEmpty() && currentIdx >= 0) {
            String chunk = remaining.substring(0, Math.min(8, remaining.length()));
            remaining = remaining.substring(chunk.length());
            StackBlock block = s.get(currentIdx);
            if (!block.label.startsWith("buf")) {
                overflowed = true;
                block.type = StackBlock.TYPE_DANGER;
                if (!block.label.contains("CORRUPT")) block.label += " (CORRUPTED)";
            } else {
                block.type = StackBlock.TYPE_FILLED;
            }
            block.value = chunk;
            currentIdx--;
        }

        _stack.setValue(s);
        updateHexDump();

        if (overflowed) {
            _statusTitle.setValue("BUFFER OVERFLOW!");
            _statusType.setValue("danger");
            _statusDesc.setValue("Input overflowed buf[] into adjacent stack regions!");
        } else {
            _statusTitle.setValue("Input Received");
            _statusType.setValue("success");
            _statusDesc.setValue("Input fit safely inside buf[]. No overflow.");
        }
        _step.setValue(5f);
        recordSnapshot(overflowed ? "OVERFLOW" : "safe input", "gets@plt");
    }

    /** Step 5: show result of gets — highlight leave/ret */
    private void step2b_5() {
        int retIdx = findInCode("}", findInCode("gets(buf);", 0));
        _activeLineIndex.setValue(retIdx >= 0 ? retIdx : level.startCodeLine);
        _statusTitle.setValue("vuln() Epilogue — ret");

        // Check if ret addr was overwritten
        List<StackBlock> s = getStack();
        boolean hijacked = false;
        String ripTarget = "0x4004ac";
        if (s != null && s.size() > 2) {
            StackBlock retBlock = s.get(2);
            if (retBlock.type.equals(StackBlock.TYPE_TARGET) ||
                retBlock.type.equals(StackBlock.TYPE_DANGER)) {
                hijacked = true;
                ripTarget = retBlock.value;
            }
        }

        if (hijacked) {
            _statusDesc.setValue("leave; ret — RIP loads " + ripTarget + " from the (corrupted) stack top!");
            _statusType.setValue("danger");
        } else {
            _statusDesc.setValue("leave; ret — RIP safely loads 0x4004ac (return to main).");
            _statusType.setValue("info");
        }
        _step.setValue(6f);
        recordSnapshot("ret from vuln()", ripTarget);
    }

    /** Step 6: branch — win() or safe return */
    private void step2b_6() {
        List<StackBlock> s = getStack();
        boolean hijacked = s != null && s.size() > 2 &&
                (s.get(2).type.equals(StackBlock.TYPE_TARGET) ||
                 s.get(2).type.equals(StackBlock.TYPE_DANGER));
        String addr = s != null && s.size() > 2 ? s.get(2).value : "";
        boolean landedOnWin = "0x400476".equalsIgnoreCase(addr);

        if (hijacked && landedOnWin) {
            int winIdx = findInCode("puts(\"SHELL OBTAINED", 0);
            _activeLineIndex.setValue(winIdx >= 0 ? winIdx : 0);
            _statusTitle.setValue("RIP hijacked → win()!");
            _statusType.setValue("success");
            _statusDesc.setValue("Execution jumped to 0x400476. win() is now running.");
            addConsole("SHELL OBTAINED — flag{ctrl_flow_h1jack}", false);
            _toast.setValue(new Pair<>("💥 Shell obtained! flag{ctrl_flow_h1jack}", false));
            _step.setValue(70f); // terminal
            recordSnapshot("win() executed!", "0x400476");
        } else if (hijacked) {
            // Wrong address — show crash
            _statusTitle.setValue("Segmentation Fault!");
            _statusType.setValue("danger");
            _statusDesc.setValue("RIP jumped to " + addr + " — not a valid function. Hint: try 0x400476.");
            addConsole("[1]    killed  Segmentation fault", false);
            _step.setValue(70f);
            recordSnapshot("SIGSEGV", addr);
        } else {
            // Safe return
            int mainEndIdx = findInCode("return 0;", 0);
            if (mainEndIdx < 0) mainEndIdx = findClosingBraceAfter(findInCode("int main() {", 0));
            _activeLineIndex.setValue(mainEndIdx);
            _statusTitle.setValue("Returned Safely to main()");
            _statusType.setValue("success");
            _statusDesc.setValue("No overflow — program returned normally.");
            _step.setValue(70f);
            recordSnapshot("main() exited", "exit");
        }
    }

    /** Terminal step for 2B */
    private void step2b_7() {
        // Already at terminal
        _step.setValue(100f);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addConsole(String text, boolean isGarbage) {
        List<Pair<String, Boolean>> list = new ArrayList<>(_consoleOut.getValue() == null
                ? new ArrayList<>() : _consoleOut.getValue());
        list.add(new Pair<>(text, isGarbage));
        _consoleOut.setValue(list);
    }

    private int findInCode(String substr, int fromIndex) {
        for (int i = fromIndex; i < level.code.size(); i++) {
            if (level.code.get(i).text.contains(substr)) return i;
        }
        return -1;
    }

    private int findClosingBraceAfter(int from) {
        for (int i = from + 1; i < level.code.size(); i++) {
            if (level.code.get(i).text.trim().equals("}")) return i;
        }
        return level.code.size() - 1;
    }

    private int findBlock(String labelStartsWith) {
        List<StackBlock> s = getStack();
        if (s == null) return -1;
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).label.startsWith(labelStartsWith)) return i;
        }
        return -1;
    }

    private int findBlockIndex(List<StackBlock> stack, String labelPrefix) {
        for (int i = 0; i < stack.size(); i++) {
            if (stack.get(i).label.startsWith(labelPrefix)) return i;
        }
        return -1;
    }

    private void setBlockValue(List<StackBlock> stack, String labelPrefix, String value) {
        for (StackBlock b : stack) {
            if (b.label.startsWith(labelPrefix)) { b.value = value; return; }
        }
    }

    private String extractQuoted(String text) {
        int start = text.indexOf('"');
        int end = text.lastIndexOf('"');
        if (start >= 0 && end > start) return text.substring(start + 1, end);
        return "";
    }

    private List<StackBlock> deepCopyStack(List<StackBlock> source) {
        List<StackBlock> copy = new ArrayList<>();
        for (StackBlock b : source) copy.add(b.copy());
        return copy;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Level 3 — Return-Oriented Programming step engine
    // ══════════════════════════════════════════════════════════════════════════

    /** Called for every "Next Step" press while on Level 3. */
    private void handle3NextStep(float currentStep) {
        int s = (int)(currentStep * 10);
        switch (s) {
            case  0: step3_intro(); break;      // show intro / NX explanation
            case 10: step3_callVuln(); break;   // call vuln() → push return addr
            case 20: step3_prologue(); break;   // vuln prologue
            case 30: step3_gets(); break;       // gets() — trigger dialog
            // 40 = waiting for payload
            case 50: step3_ret(); break;        // leave ; ret
            case 60: step3_hop(); break;        // gadget-hop loop
            case 100: /* terminal — nothing */ break;
        }
    }

    /** Stage 0: intro — main() already shown on load; jump to callVuln on first press */
    private void step3_intro() {
        // reset() already highlights main() via level.startCodeLine = 16.
        // Delegate so the FIRST Next Step press is meaningful (call vuln).
        step3_callVuln();
    }


    /** Stage 1: call vuln() — push return address */
    private void step3_callVuln() {
        _activeLineIndex.setValue(findInCode("vuln();", 0));
        _statusTitle.setValue("call vuln()");
        _statusDesc.setValue("CPU pushes return address 0x4004b9 onto the stack, then jumps to vuln.");
        _statusType.setValue("info");

        List<StackBlock> stk = deepCopyStack(getStack());
        // Index 2 = vuln Ret Addr
        if (stk.size() > 2) {
            stk.get(2).value = "0x4004b9";
            stk.get(2).type  = StackBlock.TYPE_SAFE;
        }
        _stack.setValue(stk);
        updateHexDump();
        _espIndex.setValue(2);  // RSP now points at the saved return address
        _step.setValue(2f);
        recordSnapshot("call vuln()", "vuln");
    }

    /** Stage 2: vuln() prologue — push rbp, sub rsp */
    private void step3_prologue() {
        _activeLineIndex.setValue(findInCode("void vuln() {", 0));
        _statusTitle.setValue("vuln() Prologue");
        _statusDesc.setValue("push rbp — saved frame pointer pushed. sub rsp, 0x20 — 32-byte buffer allocated for buf.");
        _statusType.setValue("info");

        List<StackBlock> stk = deepCopyStack(getStack());
        // Index 3 = vuln Saved RBP
        if (stk.size() > 3) {
            stk.get(3).value = "0x7fff8";
            stk.get(3).type  = StackBlock.TYPE_SAFE;
        }
        _stack.setValue(stk);
        updateHexDump();
        _ebpIndex.setValue(3);
        _espIndex.setValue(7); // RSP now at bottom of buf
        _step.setValue(3f);
        recordSnapshot("vuln() prologue", "vuln");
    }

    /** Stage 3: gets() — pause and wait for player to build ROP chain or normal input */
    private void step3_gets() {
        _activeLineIndex.setValue(findInCode("gets(buf);", 0));
        _statusTitle.setValue("gets() — Vulnerable Read");
        _statusDesc.setValue("gets() reads into buf[32] with no bounds check. " +
                "Will you send a normal string, NX‑demo shellcode, or a ROP chain?");
        _statusType.setValue("warn");
        _step.setValue(4f);
        recordSnapshot("gets() awaiting input", "gets@plt");
        _getsReached.setValue(true);
    }

    /** Called from fragment when NX demo preset is selected/submitted (normal text path) */
    public void submitNxDemoPayload() {
        _getsReached.setValue(false);
        _statusTitle.setValue("Shellcode on Stack — Returning...");
        _statusType.setValue("danger");
        _statusDesc.setValue("NX Demo: saved return address overwritten with 0x7ffd8000 (stack address). "
                + "vuln() will try to execute code on the stack — but NX prevents it.");

        List<StackBlock> stk = deepCopyStack(getStack());
        markJunk(stk, 3, 7);
        // Overwrite return addr with a fake stack address
        if (stk.size() > 2) {
            stk.get(2).value = "0x7ffd8000";
            stk.get(2).type  = StackBlock.TYPE_DANGER;
        }
        _stack.setValue(stk);
        updateHexDump();
        _step.setValue(5f);
        recordSnapshot("NX Demo: shellcode payload", "gets@plt");
    }

    /**
     * Called from LevelFragment when the ROP editor fires its callback.
     * Fills the stack with junk + gadget addresses and arms the hop engine.
     */
    public void submitRopPayload(List<String> payloadAddresses) {
        _getsReached.setValue(false);
        if (payloadAddresses == null || payloadAddresses.isEmpty()) return;

        List<StackBlock> stk = deepCopyStack(getStack());
        // Mark buf + saved rbp as junk
        markJunk(stk, 3, 7);

        // Lay the ROP chain starting at the return-address slot (index 2) and
        // growing toward lower addresses (toward index 0 and beyond if needed).
        int insertIdx = 2;
        for (String addr : payloadAddresses) {
            if (insertIdx >= 0) {
                stk.get(insertIdx).value = addr;
                stk.get(insertIdx).type  = StackBlock.TYPE_TARGET;
                insertIdx--;
            } else {
                // Extend stack upward (lower addresses → prepend)
                long topAddr = parseAddr(stk.get(0).address) + 8L;
                StackBlock nb = new StackBlock(
                        "0x" + Long.toHexString(topAddr), "ROP", addr, 8, StackBlock.TYPE_TARGET);
                stk.add(0, nb);
                // ESP/EBP indices shift because we prepended
                Integer ce = _espIndex.getValue(), cb = _ebpIndex.getValue();
                if (ce != null) _espIndex.setValue(ce + 1);
                if (cb != null) _ebpIndex.setValue(cb + 1);
            }
        }

        _stack.setValue(stk);
        updateHexDump();

        // Arm the hop engine
        ropPayload = new ArrayList<>(payloadAddresses);
        // ropPayloadPos=0 holds the first gadget address.
        // step3_ret() will prime rop_rip = ropPayload[0] and advance ropPayloadPos to 1
        // so that execPopGadget starts reading from index 1 (the first value).
        ropPayloadPos = 0;
        ropEspIdx = insertIdx + 1; // visual stack RSP pointer

        _statusTitle.setValue("ROP Payload Injected!");
        _statusType.setValue("danger");
        _statusDesc.setValue("The return address was overwritten with gadget chain entry " +
                ropPayload.get(0) + ". Hit Next Step to execute leave;ret and begin hopping.");
        _step.setValue(5f);
        recordSnapshot("ROP chain injected", "gets@plt");
    }

    /** Dismiss the gets() dialog without submitting a payload (normal value path) */
    public void consumeGetsReached() {
        _getsReached.setValue(false);
    }

    /** Stage 5: leave ; ret — decide what RIP lands on */
    private void step3_ret() {
        _activeLineIndex.setValue(findInCode("}", findInCode("gets(buf);", 0)));
        _statusTitle.setValue("vuln() — leave ; ret");
        _statusType.setValue("info");

        List<StackBlock> stk = getStack();
        if (stk == null) return;

        // Find what the current return-address slot contains
        String ripTarget = "0x4004b9";
        Integer ebpIdx = _ebpIndex.getValue();
        if (ebpIdx != null && ebpIdx > 0 && ebpIdx < stk.size()) {
            int retSlot = ebpIdx - 1;
            if (retSlot >= 0) {
                ripTarget = stk.get(retSlot).value;
                _espIndex.setValue(retSlot);
            }
        }

        // Detect NX‑demo case: return address is a stack address
        if (ripTarget.startsWith("0x7ff") || ripTarget.startsWith("0x7FF")) {
            _statusTitle.setValue("SIGSEGV — NX Blocked Execution!");
            _statusType.setValue("danger");
            _statusDesc.setValue("RIP = " + ripTarget + " (stack address). NX (PROT_EXEC) is set on the stack. " +
                    "The CPU refused to execute this byte — signal SIGSEGV sent. " +
                    "\n\nConclusion: shellcode injection FAILS when NX is active. Use ROP instead.");
            addConsole("[1]  Killed  (SIGSEGV — NX: stack not executable)", false);
            _step.setValue(100f);
            recordSnapshot("SIGSEGV: NX blocked", ripTarget);
        } else if (!ropPayload.isEmpty()) {
            // ROP path: prime the hop engine and start hopping
            // ropPayload[0] is already the first gadget address (== ripTarget).
            // Advance pos to 1 so execPopGadget starts reading the first VALUE correctly.
            rop_rip = ropPayload.get(0);
            ropPayloadPos = 1;
            _statusDesc.setValue("leave + ret executed. RIP = " + rop_rip +
                    " (first gadget). Hit Next Step to execute the gadget.");
            _step.setValue(6f);
            recordSnapshot("ret → first gadget", rop_rip);
        } else {
            // Normal return
            _statusDesc.setValue("ret executed normally. RIP = 0x4004b9 (main after vuln call).");
            _step.setValue(100f);
            addConsole("Program exited normally.", false);
            recordSnapshot("normal return", "0x4004b9");
        }
    }

    // ── Stage 6: explicit per-gadget dispatcher ────────────────────────────────────────
    // Each known Level 3 gadget address has its own branch. Addresses are compared as
    // longs so zero-padding differences ("0x40046a" vs "0x000000000040046a") never cause issues.

    private void step3_hop() {
        if (ropPayload.isEmpty() || ropEspIdx < 0) return;

        String rip = rop_rip;
        List<StackBlock> stk = deepCopyStack(getStack());

        long ripLong;
        try {
            ripLong = Long.parseUnsignedLong(
                    rip.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
        } catch (Exception e) {
            _statusTitle.setValue("SIGSEGV");
            _statusType.setValue("danger");
            _statusDesc.setValue("RIP = " + rip + " is not a valid address.");
            addConsole("[1]  Killed  (SIGSEGV — bad RIP)", false);
            _step.setValue(100f);
            return;
        }

        if      (ripLong == 0x40046aL) execPopGadget("rax", "pop rax ; ret", rip, stk);
        else if (ripLong == 0x400473L) execPopGadget("rdi", "pop rdi ; ret", rip, stk);
        else if (ripLong == 0x40047cL) execPopGadget("rsi", "pop rsi ; ret", rip, stk);
        else if (ripLong == 0x400485L) execPopGadget("rdx", "pop rdx ; ret", rip, stk);
        else if (ripLong == 0x40048eL) execSyscall(rip, stk);
        else {
            _statusTitle.setValue("SIGSEGV — Unknown Address");
            _statusType.setValue("danger");
            _statusDesc.setValue(
                    "RIP = " + rip + " does not match any known gadget.\n" +
                    "Known: 0x40046a (pop rax), 0x400473 (pop rdi), " +
                    "0x40047c (pop rsi), 0x400485 (pop rdx), 0x40048e (syscall).");
            addConsole("[1]  Killed  (SIGSEGV — invalid gadget " + rip + ")", false);
            _step.setValue(100f);
            recordSnapshot("SIGSEGV", rip);
        }
    }

    /**
     * pop <reg> ; ret
     *
     * Execution model (mirrors real x86-64):
     *   pop reg  →  reg = ropPayload[ropPayloadPos]  ;  RSP += 8  (ropPayloadPos++)
     *   ret      →  RIP = ropPayload[ropPayloadPos]  ;  RSP += 8  (ropPayloadPos++)
     *
     * ropPayload is the canonical source of truth; stk is for visual output only.
     */
    private void execPopGadget(String reg, String asmText, String ripAddr,
                               List<StackBlock> stk) {
        // pop <reg>: consume one value from the payload stream
        String val = (ropPayloadPos < ropPayload.size()) ? ropPayload.get(ropPayloadPos) : "0x0";
        setRopReg(reg, val);
        ropPayloadPos++;
        ropEspIdx++;   // keep visual RSP in sync

        // ret: next item is the new RIP
        if (ropPayloadPos < ropPayload.size()) {
            rop_rip = ropPayload.get(ropPayloadPos);
            ropPayloadPos++;
            ropEspIdx++;
        } else {
            rop_rip = "END";
        }
        _espIndex.setValue(ropEspIdx);

        String regUp    = reg.toUpperCase(java.util.Locale.US);
        String valShown = getRopReg(reg);
        _statusTitle.setValue(asmText);
        _statusType.setValue("info");
        _statusDesc.setValue(regUp + " ← " + valShown + "   |   next RIP → " + rop_rip);
        _stack.setValue(stk);
        updateHexDump();
        emitGdbContext(ripAddr, asmText, regUp, stk);
        _step.setValue(6f);
        recordSnapshot(asmText, rop_rip);
    }

    /** syscall: check win condition, emit GDB context, print result. */
    private void execSyscall(String ripAddr, List<StackBlock> stk) {
        boolean win = false;
        try {
            long raxV = parseAddrLong(rop_rax);
            long rdiV = parseAddrLong(rop_rdi);
            long rsiV = parseAddrLong(rop_rsi);
            long rdxV = parseAddrLong(rop_rdx);
            // Resolve the real binsh address from the gadget list at runtime
            long binshAddr = resolveBinshAddr();
            win = (raxV == 59 && rdiV == binshAddr && rsiV == 0 && rdxV == 0);
        } catch (Exception ignored) {}

        emitGdbContext(ripAddr, "syscall", null, stk);

        if (win) {
            _statusTitle.setValue("execve(\"/bin/sh\", 0, 0) — Shell!");
            _statusType.setValue("success");
            _statusDesc.setValue(
                    "syscall #59 executed — all args correct:\n" +
                    "  RAX = 59  (execve)\n" +
                    "  RDI = " + String.format("0x%x", resolveBinshAddr()) + "  (→ /bin/sh)\n" +
                    "  RSI = 0  (argv = NULL)\n" +
                    "  RDX = 0  (envp = NULL)\n" +
                    "NX bypassed — no shellcode injected, only existing binary code used.");
            addConsole("", false);
            addConsole("$ id", true);
            addConsole("uid=0(root) gid=0(root) groups=0(root)", false);
            addConsole("$ echo $0", true);
            addConsole("/bin/sh", false);
            addConsole("$ SHELL OBTAINED — flag{r0p_ch4in_nx_byp4ss}", false);
            _toast.setValue(new Pair<>("\uD83D\uDCA5 Shell obtained via ROP!", true));
        } else {
            _statusTitle.setValue("syscall — Wrong Arguments");
            _statusType.setValue("danger");
            _statusDesc.setValue(
                    "syscall fired but args were wrong:\n" +
                    "  RAX = " + rop_rax + "  (need 59 / 0x3b)\n" +
                    "  RDI = " + rop_rdi + "  (need " + String.format("0x%x", resolveBinshAddr()) + " = /bin/sh)\n" +
                    "  RSI = " + rop_rsi + "  (need 0)\n" +
                    "  RDX = " + rop_rdx + "  (need 0)");
            addConsole("[1]  Killed  (bad syscall args)", false);
        }
        _step.setValue(100f);
        recordSnapshot("syscall", "0x40048e");
    }

    /**
     * Emits a pwndbg-style GDB context block to the console.
     * Code → Registers → Stack, one addConsole line at a time.
     */
    private void emitGdbContext(String ripAddr, String asmText,
                                @Nullable String changedReg,
                                List<StackBlock> stk) {
        ropHopCount++;
        String H = "────────────────────────────────────────";

        // ── CODE ─────────────────────────────────────────────────────────────
        addConsole("", false);
        addConsole(H + "[ code ]" + H, false);

        // Split multi-instruction gadgets on " ; "
        String[] instrs = asmText.split(" ; ");
        for (int i = 0; i < instrs.length; i++) {
            String prefix = (i == 0) ? " ►  " : "     ";
            addConsole(prefix + ripAddr + "    " + instrs[i].trim(), false);
        }

        // Show the next gadget address if known
        if (!"END".equals(rop_rip) && !rop_rip.equals(ripAddr)) {
            String nextAsm = gadgetAsmForAddr(rop_rip);
            String nextLabel = (nextAsm != null) ? nextAsm : "??";
            addConsole("      " + rop_rip + "    " + nextLabel + "  ← next", false);
        }

        // ── REGISTERS ────────────────────────────────────────────────────────
        addConsole(H + "[ regs ]" + H, false);
        addConsole(regLine("RIP", ripAddr,  false,         null),         false);
        addConsole(regLine("RAX", rop_rax,  "RAX".equals(changedReg), "execve" ), false);
        addConsole(regLine("RDI", rop_rdi,  "RDI".equals(changedReg), "/bin/sh"), false);
        addConsole(regLine("RSI", rop_rsi,  "RSI".equals(changedReg), null     ), false);
        addConsole(regLine("RDX", rop_rdx,  "RDX".equals(changedReg), null     ), false);

        String rspAddr = (ropEspIdx >= 0 && ropEspIdx < stk.size())
                         ? stk.get(ropEspIdx).address : "0x???";
        addConsole(regLine("RSP", rspAddr, false, null), false);

        // ── STACK ─────────────────────────────────────────────────────────────
        addConsole(H + "[ stack ]" + H, false);
        long baseRsp;
        try {
            baseRsp = Long.parseUnsignedLong(
                    rspAddr.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
        } catch (Exception e) { baseRsp = 0; }

        int startSlot = Math.max(0, ropEspIdx - 1);
        int endSlot   = Math.min(stk.size() - 1, startSlot + 5);
        for (int i = startSlot; i <= endSlot; i++) {
            StackBlock b = stk.get(i);
            long slotAddr;
            try {
                slotAddr = Long.parseUnsignedLong(
                        b.address.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
            } catch (Exception e) { slotAddr = 0; }

            long delta = slotAddr - baseRsp;
            // Format offset as signed decimal (+8, -8, +0x10 …)
            String offsetStr = (delta >= 0 ? "+" : "") + delta;

            String rspTag = (i == ropEspIdx) ? "  ← $rsp" : "";
            String alias  = aliasForAddr(b.value);
            String note   = (alias != null) ? "  [" + alias + "]" : "";
            addConsole("  " + b.address + "│" + offsetStr + ": "
                    + b.value + rspTag + note, false);
        }
        addConsole(H + H, false);
    }

    /** One "REG  0x…  ← changed  hint" line */
    private String regLine(String name, String val, boolean changed, @Nullable String hint) {
        String arrow   = changed ? "  \u2190 changed" : "";
        String hintStr = (hint != null && changed) ? "  (\"" + hint + "\")" : "";
        return String.format(java.util.Locale.US,
                "   %-3s  %-20s%s%s", name, val, arrow, hintStr);
    }

    /** Full ASM string (with instructions split on newlines) for a gadget address */
    private String fullGadgetAsmForAddr(String addr) {
        if (level == null) return addr;
        try {
            long target = Long.parseUnsignedLong(
                    addr.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
            for (com.example.secviz.data.RopGadget g : level.ropGadgets) {
                long ga = Long.parseUnsignedLong(
                        g.address.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
                if (ga == target) return g.asm.replace(" ; ", "\n         ");
            }
        } catch (Exception ignored) {}
        return addr;
    }

    /** Short ASM string for a gadget address, or null if not found */
    @Nullable
    private String gadgetAsmForAddr(String addr) {
        if (level == null) return null;
        try {
            long target = Long.parseUnsignedLong(
                    addr.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
            for (com.example.secviz.data.RopGadget g : level.ropGadgets) {
                long ga = Long.parseUnsignedLong(
                        g.address.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
                if (ga == target) return g.asm;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Returns the binsh address from the Level's gadget list (the gadget whose alias
     * is "/bin/sh"). Falls back to 0x403010 if not found.
     */
    private long resolveBinshAddr() {
        if (level != null) {
            for (com.example.secviz.data.RopGadget g : level.ropGadgets) {
                if ("/bin/sh".equals(g.alias)) {
                    try {
                        return Long.parseUnsignedLong(
                                g.address.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
                    } catch (Exception ignored) {}
                }
            }
        }
        return 0x403010L; // fallback
    }

    /** Return the chain alias for a value, or null */
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

    private void markJunk(List<StackBlock> stk, int startIdx, int endIdx) {
        for (int i = startIdx; i <= endIdx && i < stk.size(); i++) {
            stk.get(i).type  = StackBlock.TYPE_JUNK;
            stk.get(i).value = "0x4141414141414141";
        }
    }

    @Nullable
    private String gadgetNameForAddr(String addr) {
        if (level == null) return null;
        try {
            long target = Long.parseUnsignedLong(
                    addr.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
            for (com.example.secviz.data.RopGadget g : level.ropGadgets) {
                long ga = Long.parseUnsignedLong(
                        g.address.toLowerCase(java.util.Locale.US).replace("0x", ""), 16);
                if (ga == target) {
                    // Return just the first instruction (before ;)
                    int sc = g.asm.indexOf(';');
                    return sc > 0 ? g.asm.substring(0, sc).trim() : g.asm.trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }


    private void setRopReg(String reg, String val) {
        switch (reg.toLowerCase()) {
            case "rdi": rop_rdi = val; break;
            case "rsi": rop_rsi = val; break;
            case "rdx": rop_rdx = val; break;
            case "rax": rop_rax = val; break;
        }
    }

    private String getRopReg(String reg) {
        switch (reg.toLowerCase()) {
            case "rdi": return rop_rdi;
            case "rsi": return rop_rsi;
            case "rdx": return rop_rdx;
            case "rax": return rop_rax;
            default:    return "0x0000000000000000";
        }
    }

    private long parseAddr(String addr) {
        try {
            return Long.parseUnsignedLong(addr.toLowerCase().replace("0x", ""), 16);
        } catch (Exception e) { return 0L; }
    }

    private long parseAddrLong(String addr) {
        return parseAddr(addr);
    }

    // ── Register Snapshot Recorder ────────────────────────────────────────────

    /**
     * Captures the current register state and appends it to the timeline.
     *
     * @param label     Short human-readable step description
     * @param ripSymbol A symbolic RIP hint (e.g., "main", "vuln", "read@plt")
     */
    public void recordSnapshot(String label, String ripSymbol) {
        if (!Boolean.TRUE.equals(_captureEnabled.getValue())) return;

        List<StackBlock> s = getStack();

        // Derive RSP address from current ESP block
        String rsp = "—", rbp = "—";
        Integer espIdx = _espIndex.getValue();
        Integer ebpIdx = _ebpIndex.getValue();
        if (s != null && espIdx != null && espIdx >= 0 && espIdx < s.size())
            rsp = s.get(espIdx).address;
        if (s != null && ebpIdx != null && ebpIdx >= 0 && ebpIdx < s.size())
            rbp = s.get(ebpIdx).address;

        // RIP: derive from active code line's first asm instruction or use symbol
        String rip = ripSymbol;
        Integer lineIdx = _activeLineIndex.getValue();
        if (lineIdx != null && level != null && lineIdx < level.code.size()) {
            java.util.List<String> asm = level.code.get(lineIdx).asm;
            if (asm != null && !asm.isEmpty()) {
                String firstAsm = asm.get(0).trim();
                // Extract address from "  401234:   push rbp" format
                int colon = firstAsm.indexOf(':');
                if (colon > 0) rip = "0x" + firstAsm.substring(0, colon).trim();
            }
        }

        String statusType = _statusType.getValue() == null ? "info" : _statusType.getValue();

        List<RegisterSnapshot> current = _snapshots.getValue();
        int active = _activeSnapshotIndex.getValue() == null ? -1 : _activeSnapshotIndex.getValue();

        // If we rewound the timeline and are now taking a new action, overwrite the future
        if (current != null && active >= 0 && active < current.size() - 1) {
            current = new ArrayList<>(current.subList(0, active + 1));
            snapshotCount = current.size();
        }

        List<RegisterSnapshot> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
        int codeLine = lineIdx == null ? 0 : lineIdx;

        float simStep = _step.getValue() == null ? 0f : _step.getValue();
        List<StackBlock> simStack = s == null ? new ArrayList<>() : deepCopyStack(s);
        int simEsp = espIdx == null ? 0 : espIdx;
        int simEbp = ebpIdx == null ? 0 : ebpIdx;
        String simStatusTitle = _statusTitle.getValue() == null ? "" : _statusTitle.getValue();
        String simStatusDesc = _statusDesc.getValue() == null ? "" : _statusDesc.getValue();
        List<Pair<String, Boolean>> simConsole = _consoleOut.getValue() == null ? new ArrayList<>() : new ArrayList<>(_consoleOut.getValue());

        next.add(new RegisterSnapshot(snapshotCount++, label, rip, rsp, rbp, statusType, codeLine,
                simStep, simStack, simEsp, simEbp, simStatusTitle, simStatusDesc, simConsole));
        _snapshots.setValue(next);
        _activeSnapshotIndex.setValue(snapshotCount - 1);
    }

    /**
     * Time-travel Debugging: Restores the simulation exactly to the captured state.
     * Truncates the timeline history back to this point so execution diverges from here.
     */
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

        // Do not truncate the timeline yet. Just move the active viewing index.
        _activeSnapshotIndex.setValue(snap.stepIndex);
    }


    // ── Hex Dump Builder ──────────────────────────────────────────────────────

    /**
     * Converts the current stack into rows of 16 bytes for the hex dump panel.
     * Each Object[] entry: { address:String, bytes:byte[16], status:int[16] }
     * status values: 0=empty, 1=filled(safe), 2=overflow(danger)
     */
    private void updateHexDump() {
        List<StackBlock> s = getStack();
        if (s == null) return;

        // Collect all blocks that are part of the visible region (buff + adjacent)
        // We build a flat byte array representing them from bottom-address to top
        // For simplicity, present each 8-byte block as one hex row half.
        List<Object[]> rows = new ArrayList<>();

        // Walk stack in reverse (ascending address order = bottom to top on screen)
        for (int i = s.size() - 1; i >= 0; i--) {
            StackBlock block = s.get(i);
            // Skip the very top main frame data block
            if (block.type.equals(StackBlock.TYPE_MAIN_FRAME) && !block.label.contains("Return")) continue;

            String rawValue = block.value;
            byte[] blockBytes = new byte[8];
            int status = 0; // empty

            if (!rawValue.equals("0x0") && !rawValue.equals("0x...") && !rawValue.isEmpty()) {
                // Determine status from block type
                if (block.type.equals(StackBlock.TYPE_DANGER)) status = 2;
                else if (block.type.equals(StackBlock.TYPE_FILLED) ||
                         block.type.equals(StackBlock.TYPE_WARN) ||
                         block.type.equals(StackBlock.TYPE_SAFE)) status = 1;
                else status = 0;

                // Encode the string value as UTF-8 bytes (truncated/padded to 8)
                if (rawValue.startsWith("0x")) {
                    // Looks like an address hex value — encode as little-endian
                    try {
                        long addr = Long.parseUnsignedLong(rawValue.substring(2), 16);
                        for (int b2 = 0; b2 < 8; b2++) {
                            blockBytes[b2] = (byte)(addr & 0xFF);
                            addr >>= 8;
                        }
                        if (status == 0) status = 1; // addresses count as filled
                    } catch (NumberFormatException e) {
                        byte[] encoded = rawValue.getBytes();
                        System.arraycopy(encoded, 0, blockBytes, 0,
                                Math.min(encoded.length, 8));
                    }
                } else {
                    byte[] encoded = rawValue.getBytes();
                    System.arraycopy(encoded, 0, blockBytes, 0,
                            Math.min(encoded.length, 8));
                }
            }

            // Emit one row per 8-byte block (addr | 8 hex bytes | 8 ascii)
            // We pair two consecutive blocks into one 16-byte row if possible,
            // but for clarity emit 8-byte half-rows here and let the adapter pair them.
            byte[] rowBytes = new byte[16];
            int[] rowStatus = new int[16];
            System.arraycopy(blockBytes, 0, rowBytes, 0, 8);
            // second half empty — adapter shows partial rows fine
            for (int j = 0; j < 8; j++) rowStatus[j] = status;
            rows.add(new Object[]{block.address, rowBytes, rowStatus});
        }

        _hexRows.setValue(rows);
    }
}
