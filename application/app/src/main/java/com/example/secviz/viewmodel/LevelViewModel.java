package com.example.secviz.viewmodel;

import android.util.Pair;

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

    private Level level;
    private int initialEsp = 0;

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
        snapshotCount = 0;
        _snapshots.setValue(new ArrayList<>());
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

    // ── Register Snapshot Recorder ────────────────────────────────────────────

    /**
     * Captures the current register state and appends it to the timeline.
     *
     * @param label     Short human-readable step description
     * @param ripSymbol A symbolic RIP hint (e.g., "main", "vuln", "read@plt")
     */
    public void recordSnapshot(String label, String ripSymbol) {
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

        // Truncate the timeline so future steps rewrite history
        List<RegisterSnapshot> current = _snapshots.getValue();
        if (current != null) {
            List<RegisterSnapshot> truncated = new ArrayList<>();
            for (RegisterSnapshot s : current) {
                truncated.add(s);
                if (s.stepIndex == snap.stepIndex) break;
            }
            snapshotCount = snap.stepIndex + 1;
            _snapshots.setValue(truncated);
        }
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
