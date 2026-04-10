package com.example.secviz.data;

import java.util.ArrayList;
import java.util.List;

public class Level {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String goal;
    public final String successTitle;
    public final String successDesc;
    public final int startCodeLine;
    public final List<CodeLine> code;
    public final List<StackBlock> initialStack;
    public final List<String[]> payloadPresets; // {label, value}
    public final DefensePatch defensePatch;
    /** Raw objdump text, shown in the Objdump Viewer for levels that need it. Null for others. */
    public final String objdump;
    /** ROP gadgets for Level 3 */
    public final List<RopGadget> ropGadgets;
    /** Contextual hint shown in the hint bottom sheet. May be null if no hint defined. */
    public final String hint;

    // Constructor without objdump, gadgets, or hint (backward-compat)
    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch) {
        this(id, title, subtitle, goal, successTitle, successDesc, startCodeLine,
                code, initialStack, payloadPresets, defensePatch, null, new ArrayList<>(), null);
    }

    // Constructor with objdump but no gadgets or hint (backward-compat for level 2b)
    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch,
                 String objdump) {
        this(id, title, subtitle, goal, successTitle, successDesc, startCodeLine,
                code, initialStack, payloadPresets, defensePatch, objdump, new ArrayList<>(), null);
    }

    // Constructor with objdump and gadgets but no hint (backward-compat)
    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch,
                 String objdump, List<RopGadget> ropGadgets) {
        this(id, title, subtitle, goal, successTitle, successDesc, startCodeLine,
                code, initialStack, payloadPresets, defensePatch, objdump, ropGadgets, null);
    }

    // Full constructor including hint
    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch,
                 String objdump, List<RopGadget> ropGadgets, String hint) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.goal = goal;
        this.successTitle = successTitle;
        this.successDesc = successDesc;
        this.startCodeLine = startCodeLine;
        this.code = code;
        this.initialStack = initialStack;
        this.payloadPresets = payloadPresets;
        this.defensePatch = defensePatch;
        this.objdump = objdump;
        this.ropGadgets = ropGadgets;
        this.hint = hint;
    }
}
