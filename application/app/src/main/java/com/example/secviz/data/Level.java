package com.example.secviz.data;

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

    public Level(String id, String title, String subtitle, String goal,
                 String successTitle, String successDesc, int startCodeLine,
                 List<CodeLine> code, List<StackBlock> initialStack,
                 List<String[]> payloadPresets, DefensePatch defensePatch) {
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
    }
}
