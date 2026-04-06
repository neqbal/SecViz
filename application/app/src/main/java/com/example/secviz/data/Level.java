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

    // Constructor without objdump or gadgets (backward-compat for levels 1, 2a)
    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch) {
        this(id, title, subtitle, goal, successTitle, successDesc, startCodeLine,
                code, initialStack, payloadPresets, defensePatch, null, new ArrayList<>());
    }

    // Constructor with objdump but no gadgets (backward-compat for level 2b)
    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch,
                 String objdump) {
        this(id, title, subtitle, goal, successTitle, successDesc, startCodeLine,
                code, initialStack, payloadPresets, defensePatch, objdump, new ArrayList<>());
    }

    // Full constructor including objdump and gadgets
    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch,
                 String objdump, List<RopGadget> ropGadgets) {
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
    }
}
