package com.example.secviz.data;

import java.util.List;

public class DefensePatch {
    public final String vulnText;
    public final String safeText;

    public DefensePatch(String vulnText, String safeText) {
        this.vulnText = vulnText;
        this.safeText = safeText;
    }
}
