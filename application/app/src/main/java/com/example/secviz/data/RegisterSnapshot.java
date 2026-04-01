package com.example.secviz.data;

import com.example.secviz.data.StackBlock;
import java.util.List;
import android.util.Pair;

/**
 * Immutable snapshot of the entire CPU and simulation state at a single step.
 * Allows time-travel debugging (rewinding the simulation).
 */
public class RegisterSnapshot {
    public final int    stepIndex;   // 0-based index in the snapshot list
    public final String label;       // human-readable step label, e.g. "call vuln()"
    public final String rip;         // instruction pointer (code address or symbol)
    public final String rsp;         // stack pointer (address of current ESP block)
    public final String rbp;         // base pointer  (address of current EBP block)
    public final String statusType;  // "info" | "success" | "danger" | "warn"
    public final int    codeLine;    // index into level.code for tap-to-scroll

    // Full Simulation State (for time-travel)
    public final float simStep;
    public final List<StackBlock> simStack;
    public final int simEsp;
    public final int simEbp;
    public final String simStatusTitle;
    public final String simStatusDesc;
    public final List<Pair<String, Boolean>> simConsole;

    public RegisterSnapshot(int stepIndex, String label,
                            String rip, String rsp, String rbp,
                            String statusType, int codeLine,
                            float simStep, List<StackBlock> simStack,
                            int simEsp, int simEbp,
                            String simStatusTitle, String simStatusDesc,
                            List<Pair<String, Boolean>> simConsole) {
        this.stepIndex  = stepIndex;
        this.label      = label;
        this.rip        = rip;
        this.rsp        = rsp;
        this.rbp        = rbp;
        this.statusType = statusType;
        this.codeLine   = codeLine;

        this.simStep = simStep;
        this.simStack = simStack;
        this.simEsp = simEsp;
        this.simEbp = simEbp;
        this.simStatusTitle = simStatusTitle;
        this.simStatusDesc = simStatusDesc;
        this.simConsole = simConsole;
    }
}
