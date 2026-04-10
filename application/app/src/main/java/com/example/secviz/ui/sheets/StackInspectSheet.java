package com.example.secviz.ui.sheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.secviz.R;
import com.example.secviz.data.StackBlock;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

/**
 * Bottom sheet shown when the user long-presses a stack slot.
 * Displays:
 *   • The raw stored value in hexadecimal
 *   • The numeric value in decimal (signed 64-bit)
 *   • The ASCII string representation (if all bytes are printable)
 *   • The last assembly instruction that wrote to this slot
 */
public class StackInspectSheet extends BottomSheetDialogFragment {

    private static final String ARG_LABEL   = "label";
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_VALUE   = "value";
    private static final String ARG_LAST    = "last_instr";

    public static StackInspectSheet newInstance(StackBlock block) {
        StackInspectSheet sheet = new StackInspectSheet();
        Bundle args = new Bundle();
        args.putString(ARG_LABEL,   block.label);
        args.putString(ARG_ADDRESS, block.address);
        args.putString(ARG_VALUE,   block.value);
        args.putString(ARG_LAST,    block.lastInstruction);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public int getTheme() {
        return com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stack_inspect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return;

        String label   = args.getString(ARG_LABEL,   "");
        String address = args.getString(ARG_ADDRESS, "");
        String value   = args.getString(ARG_VALUE,   "0x0");
        String lastInstr = args.getString(ARG_LAST,  null);

        ((TextView) root.findViewById(R.id.tv_slot_title)).setText(label);
        ((TextView) root.findViewById(R.id.tv_slot_address)).setText("@ " + address);

        // ── Parse value ──────────────────────────────────────────────────────
        String hexStr  = toHex(value);
        String decStr  = toDec(value);
        String asciiStr = toAscii(value);

        ((TextView) root.findViewById(R.id.tv_val_hex)).setText(hexStr);
        ((TextView) root.findViewById(R.id.tv_val_dec)).setText(decStr);

        View rowStr = root.findViewById(R.id.row_string);
        TextView tvStr = root.findViewById(R.id.tv_val_str);
        if (asciiStr != null) {
            tvStr.setText("\"" + asciiStr + "\"");
            rowStr.setVisibility(View.VISIBLE);
        } else {
            rowStr.setVisibility(View.GONE);
        }

        // ── Last instruction ─────────────────────────────────────────────────
        TextView tvLast = root.findViewById(R.id.tv_last_instr);
        TextView tvLastLabel = root.findViewById(R.id.tv_last_instr_label);
        if (lastInstr != null && !lastInstr.isEmpty()) {
            tvLast.setText(lastInstr);
            tvLast.setVisibility(View.VISIBLE);
            tvLastLabel.setVisibility(View.VISIBLE);
        } else {
            tvLast.setText("— never written during this session —");
            tvLast.setTextColor(0xFF8B949E);
            tvLast.setVisibility(View.VISIBLE);
            tvLastLabel.setVisibility(View.VISIBLE);
        }
    }

    // ── Value parsing helpers ────────────────────────────────────────────────

    /**
     * Convert the stored value string to clean hex.
     * Accepts "0x..." hex strings or plain ASCII labels like "SUPER_SE".
     */
    private String toHex(String raw) {
        if (raw == null || raw.isEmpty()) return "0x0";
        Long parsed = parseLong(raw);
        if (parsed != null) {
            return "0x" + Long.toHexString(parsed).toUpperCase(Locale.US);
        }
        // It's a plain string — show each byte as hex
        StringBuilder sb = new StringBuilder();
        for (byte b : raw.getBytes()) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    /**
     * Convert to signed decimal.
     */
    private String toDec(String raw) {
        if (raw == null || raw.isEmpty()) return "0";
        Long parsed = parseLong(raw);
        if (parsed != null) return String.valueOf(parsed);
        // For plain strings, show byte count instead
        return raw.length() + " bytes (ASCII string)";
    }

    /**
     * Attempt to decode the value as ASCII. Returns null if not representable.
     * Works for:
     *  - "SUPER_SE" directly as ASCII text
     *  - hex values like 0x45535f5245505553 (little-endian encoded string)
     */
    @Nullable
    private String toAscii(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        Long parsed = parseLong(raw);
        if (parsed != null) {
            // Try to decode as little-endian 8-byte string
            StringBuilder sb = new StringBuilder(8);
            long v = parsed;
            for (int i = 0; i < 8; i++) {
                byte b = (byte)(v & 0xFF);
                v >>= 8;
                if (b == 0) continue; // null terminator
                if (b < 0x20 || b > 0x7E) return null; // not printable
                sb.append((char) b);
            }
            String s = sb.toString();
            return s.isEmpty() ? null : s;
        }

        // Already an ASCII string (e.g. "SUPER_SE", "AAAAAAAA")
        boolean allPrintable = true;
        for (char c : raw.toCharArray()) {
            if (c < 0x20 || c > 0x7E) { allPrintable = false; break; }
        }
        return allPrintable && !raw.startsWith("0x") ? raw : null;
    }

    /** Parse "0x..." or decimal numeric string to Long. Returns null if not numeric. */
    @Nullable
    private Long parseLong(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return Long.parseUnsignedLong(s.substring(2), 16);
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
