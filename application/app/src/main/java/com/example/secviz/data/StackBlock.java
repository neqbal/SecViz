package com.example.secviz.data;

public class StackBlock {
    public static final String TYPE_MAIN_FRAME = "main-frame";
    public static final String TYPE_SAFE = "safe";
    public static final String TYPE_WARN = "warn";
    public static final String TYPE_DANGER = "danger";
    public static final String TYPE_NEUTRAL = "neutral";
    public static final String TYPE_FILLED = "filled";

    public String label;
    public String address;
    public String value;
    public String type;
    public int size;

    public StackBlock(String address, String label, String value, int size, String type) {
        this.address = address;
        this.label = label;
        this.value = value;
        this.size = size;
        this.type = type;
    }

    // Deep-clone for immutable state updates
    public StackBlock copy() {
        return new StackBlock(address, label, value, size, type);
    }
}
