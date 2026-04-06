package com.example.secviz.ui.dialogs;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.secviz.R;
import com.example.secviz.data.RopGadget;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen dialog that contains the ROP chain text editor.
 * Left column: raw text entry (aliases / hex / integers).
 * Right column: live resolved hex preview per line.
 */
public class RopEditorFragment extends DialogFragment {

    public interface OnRopPayloadReady {
        void onRopPayload(List<String> resolvedAddresses);
    }

    private List<RopGadget> gadgets = new ArrayList<>();
    private OnRopPayloadReady callback;

    // Views
    private EditText etEditor;
    private LinearLayout llPreviewLines;
    private LinearLayout llAliasChips;
    private TextView tvValidation;
    private MaterialButton btnExecute;

    // State
    private List<String> currentValidPayload = new ArrayList<>();

    // ── Factory ──────────────────────────────────────────────────────────────

    public static RopEditorFragment newInstance(List<RopGadget> gadgets) {
        RopEditorFragment f = new RopEditorFragment();
        f.gadgets = gadgets;
        return f;
    }

    public void setOnRopPayloadReady(OnRopPayloadReady cb) {
        this.callback = cb;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public int getTheme() {
        return com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rop_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar_editor);
        toolbar.setNavigationOnClickListener(v -> dismiss());

        etEditor      = root.findViewById(R.id.et_rop_editor);
        llPreviewLines = root.findViewById(R.id.ll_preview_lines);
        llAliasChips  = root.findViewById(R.id.ll_alias_chips);
        tvValidation  = root.findViewById(R.id.tv_validation);
        btnExecute    = root.findViewById(R.id.btn_execute_payload);

        // Build alias chips from gadget list
        buildAliasChips();

        btnExecute.setOnClickListener(v -> {
            if (callback != null && !currentValidPayload.isEmpty()) {
                callback.onRopPayload(currentValidPayload);
            }
            dismiss();
        });

        etEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                parseAndRender(s.toString());
            }
        });

        // Start empty — player builds the chain themselves
        parseAndRender("");
    }

    // ── Alias Chips ───────────────────────────────────────────────────────────

    private void buildAliasChips() {
        if (llAliasChips == null) return;
        llAliasChips.removeAllViews();

        // Header label
        TextView label = new TextView(requireContext());
        label.setText("aliases: ");
        label.setTextColor(0xFF8B949E);
        label.setTextSize(11f);
        label.setTypeface(Typeface.MONOSPACE);
        llAliasChips.addView(label);

        for (RopGadget gadget : gadgets) {
            Chip chip = new Chip(requireContext());
            chip.setText(gadget.alias);
            chip.setTextSize(11f);
            chip.setTypeface(Typeface.MONOSPACE);
            chip.setChipBackgroundColorResource(R.color.bg_elevated);
            chip.setTextColor(0xFF58A6FF);
            chip.setChipStrokeColorResource(R.color.border);
            chip.setChipStrokeWidth(1f);
            chip.setClickable(true);
            chip.setOnClickListener(v -> insertAlias(gadget.alias));
            llAliasChips.addView(chip);
        }
    }

    /** Insert an alias at the current cursor position in the editor */
    private void insertAlias(String alias) {
        if (etEditor == null) return;
        int start = etEditor.getSelectionStart();
        int end = etEditor.getSelectionEnd();
        Editable text = etEditor.getText();
        if (text == null) return;

        // If in the middle of a line, go to end of line first
        String raw = text.toString();
        int lineEnd = raw.indexOf('\n', start);
        int insertPos = (lineEnd == -1) ? raw.length() : lineEnd;

        text.replace(insertPos, insertPos, (insertPos == raw.length() ? "\n" : "") + alias + "\n");
        etEditor.setSelection(insertPos + alias.length() + (insertPos == raw.length() ? 2 : 1));
    }

    // ── Live Preview ──────────────────────────────────────────────────────────

    private void parseAndRender(String text) {
        llPreviewLines.removeAllViews();
        currentValidPayload.clear();

        String[] lines = text.split("\n", -1);
        boolean hasError = false;
        int entryCount = 0;

        for (String rawLine : lines) {
            // Strip comment
            String clean = rawLine;
            int hash = clean.indexOf('#');
            if (hash >= 0) clean = clean.substring(0, hash);
            clean = clean.trim();

            // Build a preview row TextView
            TextView tv = new TextView(requireContext());
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextSize(11f);
            tv.setPadding(0, 1, 0, 1);

            if (clean.isEmpty()) {
                // blank / comment-only line — dim hint
                tv.setText(" ");
                tv.setTextColor(0xFF3C4048);
                llPreviewLines.addView(tv);
                continue;
            }

            String resolved = resolveToken(clean);
            if (resolved == null) {
                tv.setText("???  ← unknown alias");
                tv.setTextColor(0xFFF85149); // red
                hasError = true;
            } else {
                tv.setText(resolved);
                tv.setTextColor(chooseColor(clean));
                currentValidPayload.add(resolved);
                entryCount++;
            }
            llPreviewLines.addView(tv);
        }

        // Update footer
        if (hasError) {
            tvValidation.setText("⚠  Syntax error — fix red lines before sending");
            tvValidation.setTextColor(0xFFF85149);
            btnExecute.setEnabled(false);
            currentValidPayload.clear();
        } else if (entryCount == 0) {
            tvValidation.setText("Type gadget aliases or hex values above");
            tvValidation.setTextColor(0xFF8B949E);
            btnExecute.setEnabled(false);
        } else {
            int bytes = entryCount * 8;
            tvValidation.setText(entryCount + " entries · " + bytes + " bytes · ready");
            tvValidation.setTextColor(0xFF3FB950); // green
            btnExecute.setEnabled(true);
        }
    }

    /** Pick a colour based on whether the token is a gadget address, data, or raw number */
    private int chooseColor(String token) {
        // Check if it's a recognized gadget
        for (RopGadget g : gadgets) {
            if (g.alias.equalsIgnoreCase(token)) {
                // Gadget addresses are blue; data pointers are purple
                if (g.alias.equals("/bin/sh")) return 0xFFA371F7; // purple
                return 0xFF58A6FF; // blue
            }
        }
        // raw numeric → yellow-ish
        return 0xFFD2A8FF;
    }

    // ── Token Resolution ──────────────────────────────────────────────────────

    /**
     * Resolves a single token to a canonical 0x-prefixed 16-digit hex string,
     * or null if unrecognised.
     */
    @Nullable
    private String resolveToken(String token) {
        // 1. Check alias table
        for (RopGadget g : gadgets) {
            if (g.alias.equalsIgnoreCase(token)) {
                return formatAddr(g.address);
            }
        }

        // 2. Try hex literal (0x...)
        if (token.toLowerCase(Locale.US).startsWith("0x")) {
            try {
                long v = Long.parseUnsignedLong(token.substring(2), 16);
                return String.format(Locale.US, "0x%016x", v);
            } catch (NumberFormatException ignored) {}
        }

        // 3. Try decimal integer
        try {
            long v = Long.parseLong(token);
            return String.format(Locale.US, "0x%016x", v);
        } catch (NumberFormatException ignored) {}

        return null;
    }

    private String formatAddr(String addr) {
        try {
            String hex = addr.toLowerCase(Locale.US).replace("0x", "");
            long v = Long.parseUnsignedLong(hex, 16);
            return String.format(Locale.US, "0x%016x", v);
        } catch (Exception e) {
            return addr;
        }
    }

    public List<String> getCurrentPayload() {
        return new ArrayList<>(currentValidPayload);
    }
}
