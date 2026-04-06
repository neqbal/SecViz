package com.example.secviz.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.secviz.R;
import com.example.secviz.data.StackBlock;
import com.example.secviz.ui.views.StackCanvasView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 3-step payload builder wizard.
 *
 * Step 1 (JUNK): tap first block, tap last block → junk range selected.
 * Step 2 (ADDRESS): tap the block immediately above (lower index) the junk range → enter address.
 * Step 3 (PREVIEW): shows generated payload bytes, Exploit button sends it.
 */
public class PayloadBuilderFragment extends DialogFragment
        implements StackCanvasView.BlockTapListener {

    public interface OnPayloadReady {
        void onPayload(int junkStartIndex, int junkEndIndex,
                       int targetBlockIndex, String targetAddress);
    }

    public interface OnNormalInputRequested {
        void onNormalInput();
    }

    // ── Wizard state ──────────────────────────────────────────────────────────
    private enum WizardStep { JUNK, ADDRESS, PREVIEW }

    private WizardStep wizardStep = WizardStep.JUNK;
    private int junkFirstTap  = -1; // first block the user tapped
    private int junkStart     = -1; // lower index (higher mem addr)
    private int junkEnd       = -1; // higher index (lower mem addr, = towards buf bottom)
    private int targetBlockIdx = -1;
    private String targetAddress = "";

    // Stack passed from LevelFragment
    private List<StackBlock> stackBlocks;
    /** Index 2 in our level = "vuln Return Addr" — the correct target */
    private static final int CORRECT_TARGET_INDEX = 2;
    private static final String WIN_ADDRESS = "0x400476";

    private OnPayloadReady payloadCallback;
    private OnNormalInputRequested normalInputCallback;
    private String objdump;

    // Views
    private StackCanvasView stackCanvas;
    private TextView tvInstruction, tvStepDots, tvPayloadPreview;
    private View scrollPayloadPreview;
    private MaterialButton btnPrev, btnNext;

    // ─────────────────────────────────────────────────────────────────────────

    public static PayloadBuilderFragment newInstance(List<StackBlock> stack, String objdump) {
        PayloadBuilderFragment f = new PayloadBuilderFragment();
        f.stackBlocks = deepCopy(stack);
        f.objdump = objdump;
        return f;
    }

    public void setOnPayloadReady(OnPayloadReady cb) { this.payloadCallback = cb; }
    public void setOnNormalInputRequested(OnNormalInputRequested cb) { this.normalInputCallback = cb; }

    @Override
    public int getTheme() {
        return com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payload_builder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        stackCanvas      = root.findViewById(R.id.stack_canvas_pb);
        tvInstruction    = root.findViewById(R.id.tv_pb_instruction);
        tvStepDots       = root.findViewById(R.id.tv_pb_step_dots);
        tvPayloadPreview = root.findViewById(R.id.tv_payload_preview);
        scrollPayloadPreview = root.findViewById(R.id.scroll_payload_preview);
        btnPrev          = root.findViewById(R.id.btn_pb_prev);
        btnNext          = root.findViewById(R.id.btn_pb_next);

        // Init stack canvas
        stackCanvas.setStack(new ArrayList<>(stackBlocks));
        stackCanvas.setSelectionMode(StackCanvasView.SelectionMode.JUNK_SELECT);
        stackCanvas.setBlockTapListener(this);

        // Close / back
        root.findViewById(R.id.btn_pb_back).setOnClickListener(v -> dismiss());

        // Objdump viewer
        root.findViewById(R.id.btn_pb_objdump).setOnClickListener(v -> {
            ObjdumpViewerFragment viewer = ObjdumpViewerFragment.newInstance(objdump);
            viewer.show(getChildFragmentManager(), "objdump");
        });

        btnPrev.setOnClickListener(v -> goBack());
        btnNext.setOnClickListener(v -> goNext());

        renderStep();
    }

    // ── Block tap dispatch ────────────────────────────────────────────────────

    @Override
    public void onBlockTapped(int blockIndex) {
        switch (wizardStep) {
            case JUNK:    handleJunkTap(blockIndex);    break;
            case ADDRESS: handleAddressTap(blockIndex); break;
        }
    }

    private void handleJunkTap(int blockIndex) {
        if (junkFirstTap < 0) {
            // First tap
            junkFirstTap = blockIndex;
            stackCanvas.setJunkRange(blockIndex, blockIndex);
            tvInstruction.setText("Step 1 / 3 — Now tap the LAST block of the junk range.");
        } else {
            // Second tap — resolve order (lower index = closer to top/high address)
            junkStart = Math.min(junkFirstTap, blockIndex);
            junkEnd   = Math.max(junkFirstTap, blockIndex);
            stackCanvas.setJunkRange(junkStart, junkEnd);

            int junkBytes = (junkEnd - junkStart + 1) * 8;
            tvInstruction.setText("Step 1 / 3 — Junk range: blocks " + junkStart + "–" + junkEnd
                    + " (" + junkBytes + " bytes). Tap again to reset, or tap Next.");
            btnNext.setEnabled(true);
        }
    }

    private void handleAddressTap(int blockIndex) {
        // Valid target = block immediately above (lower index) junk range
        int validTarget = junkStart - 1;
        if (blockIndex != validTarget) {
            stackCanvas.shakeBlock(blockIndex);
            // Brief toast-like feedback via instruction text
            tvInstruction.setText("Step 2 / 3 — ✗ The address must go right after the junk range (block "
                    + validTarget + "). Try again.");
            return;
        }
        // Correct block — show address input dialog
        showAddressInputDialog(blockIndex);
    }

    private void showAddressInputDialog(int blockIndex) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_address_input, null, false);
        TextInputEditText etAddr = dialogView.findViewById(R.id.et_address);
        TextView tvPreview       = dialogView.findViewById(R.id.tv_le_preview);
        TextInputLayout til      = dialogView.findViewById(R.id.til_address);

        // Live LE preview
        etAddr.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String hex = s.toString().trim().replace("0x", "").replace("0X", "");
                try {
                    long val = Long.parseUnsignedLong(hex, 16);
                    StringBuilder le = new StringBuilder();
                    for (int i = 0; i < 8; i++) {
                        le.append(String.format(Locale.US, "%02x", val & 0xFF));
                        val >>= 8;
                        if (i < 7) le.append(' ');
                    }
                    tvPreview.setText("Little-endian: " + le);
                    tvPreview.setTextColor(0xFF58A6FF);
                    til.setError(null);
                } catch (NumberFormatException e) {
                    tvPreview.setText("Little-endian: —");
                    tvPreview.setTextColor(0xFF8B949E);
                }
            }
        });

        new AlertDialog.Builder(requireContext(), R.style.Theme_SecViz_Dialog)
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dlg, which) -> {
                    String input = etAddr.getText() != null
                            ? etAddr.getText().toString().trim() : "";
                    String normInput = input.startsWith("0x") || input.startsWith("0X")
                            ? input : "0x" + input;
                    if (!isValidHex(normInput)) {
                        til.setError("Enter a valid hex address");
                        return;
                    }
                    targetAddress  = normInput.toLowerCase(Locale.US);
                    targetBlockIdx = blockIndex;

                    // Update stack block to show the address
                    List<StackBlock> updated = deepCopy(stackBlocks);
                    updated.get(blockIndex).type  = StackBlock.TYPE_TARGET;
                    updated.get(blockIndex).value = targetAddress;
                    stackCanvas.setStack(updated);
                    stackCanvas.setTargetBlock(blockIndex);

                    tvInstruction.setText("Step 2 / 3 — Address " + targetAddress
                            + " placed at block " + blockIndex + ". Tap Next to generate payload.");
                    btnNext.setText("Generate →");
                    btnNext.setEnabled(true);
                    btnPrev.setEnabled(true);
                })
                .show();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void goNext() {
        if (wizardStep == WizardStep.JUNK) {
            if (junkStart < 0) return;
            wizardStep = WizardStep.ADDRESS;
            renderStep();

        } else if (wizardStep == WizardStep.ADDRESS) {
            if (targetBlockIdx < 0) return;
            wizardStep = WizardStep.PREVIEW;
            renderStep();

        } else if (wizardStep == WizardStep.PREVIEW) {
            // Exploit!
            if (payloadCallback != null) {
                payloadCallback.onPayload(junkStart, junkEnd, targetBlockIdx, targetAddress);
            }
            dismiss();
        }
    }

    private void goBack() {
        if (wizardStep == WizardStep.ADDRESS) {
            wizardStep = WizardStep.JUNK;
            // Reset junk selection
            junkStart = junkEnd = junkFirstTap = -1;
            stackCanvas.clearJunkRange();
            stackCanvas.setSelectionMode(StackCanvasView.SelectionMode.JUNK_SELECT);
            renderStep();

        } else if (wizardStep == WizardStep.PREVIEW) {
            wizardStep = WizardStep.ADDRESS;
            targetBlockIdx = -1;
            targetAddress = "";
            stackCanvas.clearTargetBlock();
            // Restore stack appearance
            stackCanvas.setStack(deepCopy(stackBlocks));
            stackCanvas.setJunkRange(junkStart, junkEnd);
            renderStep();
        }
    }

    // ── Render current step ───────────────────────────────────────────────────

    private void renderStep() {
        scrollPayloadPreview.setVisibility(View.GONE);

        switch (wizardStep) {
            case JUNK:
                tvStepDots.setText("① ─── ② ─── ③");
                tvStepDots.setTextColor(0xFF58A6FF); // step 1 active
                tvInstruction.setText("Step 1 / 3 — Tap the FIRST stack block you want to fill with junk data, " +
                        "then tap the LAST block. This sets your junk fill range.");
                stackCanvas.setSelectionMode(StackCanvasView.SelectionMode.JUNK_SELECT);
                btnPrev.setEnabled(false);
                btnNext.setText("Next →");
                btnNext.setEnabled(false);
                break;

            case ADDRESS:
                tvStepDots.setText("① ─── ② ─── ③");
                tvStepDots.setTextColor(0xFF58A6FF);
                tvInstruction.setText("Step 2 / 3 — Open the Objdump viewer to find win()'s address, " +
                        "then tap the stack block where you want to write it.\n\n" +
                        "Hint: the address block must be directly above your junk range.");
                stackCanvas.setSelectionMode(StackCanvasView.SelectionMode.ADDRESS_SELECT);
                stackCanvas.setJunkRange(junkStart, junkEnd);
                btnPrev.setEnabled(true);
                btnNext.setText("Generate →");
                btnNext.setEnabled(false);
                break;

            case PREVIEW:
                tvStepDots.setText("① ─── ② ─── ③");
                tvStepDots.setTextColor(0xFF3FB950); // all done, green
                tvInstruction.setText("Step 3 / 3 — Review your payload. Tap Exploit to send it and hijack execution.");
                stackCanvas.setSelectionMode(StackCanvasView.SelectionMode.NONE);
                scrollPayloadPreview.setVisibility(View.VISIBLE);
                tvPayloadPreview.setText(buildPayloadString());
                btnPrev.setEnabled(true);
                btnNext.setText("Exploit!");
                btnNext.setEnabled(true);
                break;
        }
    }

    // ── Payload string builder ────────────────────────────────────────────────

    private String buildPayloadString() {
        StringBuilder sb = new StringBuilder();
        int junkBytes = (junkEnd - junkStart + 1) * 8;
        sb.append("Payload (").append(junkBytes + 8).append(" bytes total):\n\n");

        // Junk blocks from bottom up (junkEnd has lowest address in buf)
        for (int i = junkEnd; i >= junkStart; i--) {
            sb.append("\\x41\\x41\\x41\\x41\\x41\\x41\\x41\\x41");
            sb.append("   ← ").append(stackBlocks.get(i).label).append("\n");
        }

        // Target address (little-endian)
        sb.append(buildLeString(targetAddress));
        sb.append("   ← ").append(stackBlocks.get(targetBlockIdx).label).append(" → win()\n");

        return sb.toString();
    }

    private String buildLeString(String hexAddr) {
        String hex = hexAddr.replace("0x", "").replace("0X", "");
        try {
            long val = Long.parseUnsignedLong(hex, 16);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format(Locale.US, "\\x%02x", val & 0xFF));
                val >>= 8;
            }
            return sb.toString();
        } catch (NumberFormatException e) {
            return "\\x??\\x??\\x??\\x??\\x??\\x??\\x??\\x??";
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isValidHex(String s) {
        String h = s.replace("0x", "").replace("0X", "");
        if (h.isEmpty() || h.length() > 16) return false;
        try { Long.parseUnsignedLong(h, 16); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private static List<StackBlock> deepCopy(List<StackBlock> src) {
        List<StackBlock> copy = new ArrayList<>();
        if (src != null) for (StackBlock b : src) copy.add(b.copy());
        return copy;
    }
}
