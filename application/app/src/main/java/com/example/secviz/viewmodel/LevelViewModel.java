package com.example.secviz.viewmodel;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.secviz.data.Level;
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
        _espIndex.setValue(initialEsp);
        _ebpIndex.setValue(initialEsp);
        _statusTitle.setValue("Program Ready");
        _statusDesc.setValue("Click 'Next Step' to start execution.");
        _statusType.setValue("info");
        _consoleOut.setValue(new ArrayList<>());
        _userInput.setValue("");
        _isPatched.setValue(Boolean.TRUE.equals(_isPatched.getValue())); // preserve patch state
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
        } else {
            _statusTitle.setValue("Calling vuln()");
            pushReturnAddr(patched);
            _step.setValue(2f);
        }
    }

    private void step1(boolean patched) {
        int callIdx = findInCode("vuln();", level.startCodeLine);
        _activeLineIndex.setValue(callIdx);
        _statusTitle.setValue("Calling vuln()");
        pushReturnAddr(patched);
        _step.setValue(2f);
    }

    private void pushReturnAddr(boolean patched) {
        List<StackBlock> s = deepCopyStack(getStack());
        int retIdx = initialEsp + 1;
        if (retIdx < s.size()) {
            s.get(retIdx).value = level.id.equals("1") ? "0x400490" : "0x4004ec";
        }
        _stack.setValue(s);
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
        _ebpIndex.setValue(vulnEbpIdx);

        int buff0Idx = findBlock("buff[0..7]");
        _espIndex.setValue(buff0Idx >= 0 ? buff0Idx : _espIndex.getValue());

        boolean firstIsNotBuff = !level.code.get(firstVarIdx).text.contains("char buff");
        _step.setValue(firstIsNotBuff ? 2.5f : 3f);
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
        _step.setValue(3f);
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
        } else {
            _activeLineIndex.setValue(readIdx);
            if (level.id.equals("2a")) {
                _statusDesc.setValue(patched
                        ? "read() securely terminates the buffer. The secret is completely safe!"
                        : "read() bounds-check to 16 bytes but no null-terminator. Fill exactly 16 bytes to leak the secret!");
            }
            _statusTitle.setValue("read() Execution");
            _step.setValue(4f);
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
    }

    public void submitInput(String input) {
        _userInput.setValue(input);
        boolean patched = isPatched();

        String processedInput = input;
        if (patched) {
            int maxLen = level.id.equals("2a") ? 15 : 16;
            if (processedInput.length() > maxLen) processedInput = processedInput.substring(0, maxLen);
        }

        List<StackBlock> newStack = deepCopyStack(level.initialStack);
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
        _statusTitle.setValue(hasOverflowed ? "BUFFER OVERFLOW!" : "Input Received Safely");
        _statusType.setValue(hasOverflowed ? "danger" : "success");
        _step.setValue(5f);
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
            _statusDesc.setValue("Execution crashed. The Return Address did not point to a valid function.");
            _step.setValue(100f);
            if (level.goal.equals("CRASH")) {
                triggerWin(false);
            }
        } else {
            if (patched && input.length() > 16 && level.goal.equals("CRASH")) {
                _statusDesc.setValue("Defended! The boundaries prevented the payload from overflowing past buff.");
                _statusType.setValue("success");
                triggerWin(true);
            }
            _espIndex.setValue(Math.max(0, initialEsp - 1));
            _ebpIndex.setValue(initialEsp);
            _step.setValue(7f);
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
        } else {
            int endMain = findClosingBraceAfter(level.startCodeLine);
            _activeLineIndex.setValue(endMain);
            _statusTitle.setValue("Program Exited");
            _step.setValue(100f);
            checkLeakWin();
        }
    }

    private void step8(String input, boolean patched) {
        int endMain = findClosingBraceAfter(level.startCodeLine);
        _activeLineIndex.setValue(endMain);
        _statusTitle.setValue("Program Exited");
        _step.setValue(100f);
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
}
