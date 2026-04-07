package com.example.secviz.data;

import java.util.Arrays;
import java.util.List;

public class CodeLine {
    public final String text;
    public final List<String> asm;

    public CodeLine(String text, String... asmLines) {
        this.text = text;
        this.asm = Arrays.asList(asmLines);
    }
}
 