package com.example.secviz.data;

/**
 * One parsed line from the level's objdump string.
 *
 * Examples:
 *   header:      "0000000000400487 <vuln>:"
 *   instruction: "  400487:  55  push   rbp"
 *   separator:   ""
 */
public class AsmLine {

    /** Index in the flat list built from the full objdump. */
    public final int flatIdx;

    /**
     * Hex address string WITHOUT "0x" prefix, lower-case.
     * e.g. "40049c"
     * Empty string for header/separator lines.
     */
    public final String address;

    /** Full raw text of this line, displayed verbatim in the Assembly tab. */
    public final String rawText;

    /** True if this is a function-label header line (e.g. "000…487 <vuln>:"). */
    public final boolean isHeader;

    /**
     * If this instruction is a CALL, the hex address of the call target
     * (without "0x").  Null otherwise.
     * Used by handleNextInstruction() to step inside the called function.
     *
     * Example: for "  4004a3:  call  400370 <gets@plt>"
     *   callTarget = "400370"
     */
    public final String callTarget;

    public AsmLine(int flatIdx, String address, String rawText,
                   boolean isHeader, String callTarget) {
        this.flatIdx    = flatIdx;
        this.address    = address;
        this.rawText    = rawText;
        this.isHeader   = isHeader;
        this.callTarget = callTarget;
    }
}
