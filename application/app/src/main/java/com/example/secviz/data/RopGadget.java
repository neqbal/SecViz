package com.example.secviz.data;

/**
 * Represents a single ROP gadget found in a binary.
 * address     — hex string e.g. "0x401234"
 * asm         — assembly text e.g. "pop rdi ; ret"
 * description — human-readable explanation shown in the inspector
 * alias       — short keyword the chain editor accepts, e.g. "pop_rdi"
 */
public class RopGadget {
    public final String address;
    public final String asm;
    public final String description;
    public final String alias;

    public RopGadget(String address, String asm, String description, String alias) {
        this.address     = address;
        this.asm         = asm;
        this.description = description;
        this.alias       = alias;
    }
}
