package com.example.secviz.ui.sheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Bottom sheet that shows the current CPU register state.
 * Opened by the "Registers" button in the title bar.
 * Accepts a snapshot of simRegs as a String[] of "KEY=VALUE" pairs.
 */
public class RegisterViewerSheet extends BottomSheetDialogFragment {

    private static final String ARG_REGS  = "regs";
    private static final String ARG_STEP  = "step_label";

    /**
     * @param regs      Current register map from LevelViewModel.simRegs
     * @param stepLabel Human-readable step label, e.g. "Step 3"
     */
    public static RegisterViewerSheet newInstance(Map<String, String> regs, String stepLabel) {
        RegisterViewerSheet sheet = new RegisterViewerSheet();
        Bundle args = new Bundle();
        // Serialize map as "KEY=VALUE" lines
        String[] entries = new String[regs.size()];
        int i = 0;
        for (Map.Entry<String, String> e : regs.entrySet()) {
            entries[i++] = e.getKey() + "=" + e.getValue();
        }
        args.putStringArray(ARG_REGS, entries);
        args.putString(ARG_STEP, stepLabel != null ? stepLabel : "");
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
        return inflater.inflate(R.layout.sheet_register_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return;

        String[] rawEntries = args.getStringArray(ARG_REGS);
        String stepLabel    = args.getString(ARG_STEP, "");

        TextView tvStep = root.findViewById(R.id.tv_reg_step_label);
        tvStep.setText(stepLabel.isEmpty() ? "" : stepLabel);
        tvStep.setVisibility(stepLabel.isEmpty() ? View.GONE : View.VISIBLE);

        List<RegEntry> entries = new ArrayList<>();
        if (rawEntries != null) {
            for (String raw : rawEntries) {
                int eq = raw.indexOf('=');
                if (eq > 0) {
                    entries.add(new RegEntry(raw.substring(0, eq), raw.substring(eq + 1)));
                }
            }
        }

        RecyclerView rv = root.findViewById(R.id.rv_registers);
        // 2-column grid — each register takes half the width
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(new RegAdapter(entries));
    }

    // ── Data ────────────────────────────────────────────────────────────────

    private static class RegEntry {
        final String name;
        final String value;
        RegEntry(String name, String value) { this.name = name; this.value = value; }
    }

    // ── Adapter ─────────────────────────────────────────────────────────────

    private static class RegAdapter extends RecyclerView.Adapter<RegAdapter.VH> {
        private final List<RegEntry> items;
        RegAdapter(List<RegEntry> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View cell = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new VH(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            RegEntry e = items.get(position);

            // Shorten leading zeros: 0x000000000040046a → 0x40046a
            String val = e.value;
            if (val.startsWith("0x") && val.length() > 4) {
                String stripped = val.replaceFirst("0x0*", "0x");
                if (stripped.equals("0x")) stripped = "0x0";
                val = stripped;
            }

            // Color-code by register category
            int nameColor  = nameColor(e.name);
            int valueColor = valueColor(e.name, e.value);

            // Build a two-part span: name (colored) + " = " + value
            android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
            ssb.append(e.name);
            ssb.setSpan(new android.text.style.ForegroundColorSpan(nameColor),
                    0, e.name.length(), 0);
            ssb.append("  ");
            int valStart = ssb.length();
            ssb.append(val);
            ssb.setSpan(new android.text.style.ForegroundColorSpan(valueColor),
                    valStart, ssb.length(), 0);

            holder.tv.setText(ssb);
            holder.tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            holder.tv.setTextSize(13f);
            holder.tv.setBackgroundColor(0xFF0D1117);
            holder.tv.setPadding(20, 14, 12, 14);
        }

        @Override public int getItemCount() { return items.size(); }

        /** Color for the register name. */
        private int nameColor(String name) {
            switch (name) {
                case "RIP": return 0xFFFF7B72; // red — instruction pointer
                case "RSP": return 0xFF58A6FF; // blue — stack pointer
                case "RBP": return 0xFFA371F7; // purple — base pointer
                case "RAX": return 0xFF3FB950; // green — return/syscall number
                default:    return 0xFF8B949E; // muted for general-purpose regs
            }
        }

        /** Color for the value — zero values are dimmed. */
        private int valueColor(String name, String value) {
            if (value.equals("0x0") || value.equals("0") || value.equals("0x00")) {
                return 0xFF3D444D;
            }
            switch (name) {
                case "RIP": return 0xFFFF7B72;
                case "RSP": return 0xFF79C0FF;
                case "RBP": return 0xFFD2A8FF;
                default:    return 0xFFC9D1D9;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(View v) { super(v); tv = v.findViewById(android.R.id.text1); }
        }
    }
}
