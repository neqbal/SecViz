package com.example.secviz.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.example.secviz.data.Level;
import com.example.secviz.data.StackBlock;
import com.example.secviz.data.AsmLine;
import com.example.secviz.ui.adapters.AsmLineAdapter;
import com.example.secviz.ui.adapters.CodeLineAdapter;
import com.example.secviz.ui.adapters.ConsoleAdapter;
import com.example.secviz.ui.adapters.HexDumpAdapter;
import com.example.secviz.ui.adapters.RegisterTimelineAdapter;
import com.example.secviz.ui.dialogs.ObjdumpViewerFragment;
import com.example.secviz.ui.dialogs.PayloadBuilderFragment;
import com.example.secviz.ui.dialogs.RopEditorFragment;
import com.example.secviz.ui.dialogs.RopScannerFragment;
import com.example.secviz.ui.sheets.RegisterViewerSheet;
import com.example.secviz.ui.sheets.StackInspectSheet;
import com.example.secviz.ui.views.StackCanvasView;
import com.example.secviz.viewmodel.LevelViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LevelFragment extends Fragment {

    private static final String ARG_LEVEL_INDEX = "levelIndex";

    private Level level;
    private LevelViewModel viewModel;

    private RecyclerView rvCode;
    private RecyclerView rvConsole;
    private StackCanvasView stackCanvas;
    private TextView tvStatusTitle, tvStatusDesc, tvLevelTitle, tvLevelSubtitle;
    private LinearLayout layoutInput;
    private TextInputEditText etInput;
    private LinearLayout layoutPresets;
    private MaterialButton btnNextStep, btnNextInstr, btnReset, btnNextStage, btnSend;
    private View snackbarAnchor;
    private DrawerLayout drawerLayout;
    private boolean hexDataReceived = false;
    private CodeLineAdapter codeAdapter;
    private AsmLineAdapter asmAdapter;
    private ConsoleAdapter consoleAdapter;
    private HexDumpAdapter hexDumpAdapter;
    private RegisterTimelineAdapter timelineAdapter;
    // Assembly tab UI references
    private RecyclerView rvAsm;
    private TextView tabSource, tabAssembly;
    private boolean isAsmTabActive = false;
    private int lastSnapshotCount = 0;
    private List<Pair<String, Boolean>> consoleItems = new ArrayList<>();

    private Runnable onNextLevel;

    public static LevelFragment newInstance(Level level, Runnable onNextLevel) {
        LevelFragment f = new LevelFragment();
        f.level = level;
        f.onNextLevel = onNextLevel;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_level, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        // Views
        rvCode = root.findViewById(R.id.rv_code);
        rvConsole = root.findViewById(R.id.rv_console);
        stackCanvas = root.findViewById(R.id.stack_canvas);
        tvStatusTitle = root.findViewById(R.id.tv_status_title);
        tvStatusDesc = root.findViewById(R.id.tv_status_desc);
        tvLevelTitle = root.findViewById(R.id.tv_level_title);
        tvLevelSubtitle = root.findViewById(R.id.tv_level_subtitle);
        layoutInput = root.findViewById(R.id.layout_input);
        etInput = root.findViewById(R.id.et_input);
        layoutPresets = root.findViewById(R.id.layout_presets);
        btnNextStep = root.findViewById(R.id.btn_next_step);
        btnReset = root.findViewById(R.id.btn_reset);
        btnNextStage = root.findViewById(R.id.btn_next_stage);
        btnSend = root.findViewById(R.id.btn_send);
        snackbarAnchor = root.findViewById(R.id.snackbar_anchor);
        drawerLayout = root.findViewById(R.id.drawer_layout);
        rvAsm        = root.findViewById(R.id.rv_asm);
        tabSource    = root.findViewById(R.id.tab_source);
        tabAssembly  = root.findViewById(R.id.tab_assembly);
        btnNextInstr = root.findViewById(R.id.btn_next_instr);
        MaterialButton btnHexClose  = root.findViewById(R.id.btn_hex_close);

        // Header
        tvLevelTitle.setText(level.title);
        tvLevelSubtitle.setText(level.subtitle);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        viewModel.init(level);
        loadLevelRegisters(level.id); // load pre-calculated register snapshots from assets

        // Code RecyclerView
        codeAdapter = new CodeLineAdapter(
                level.code,
                viewModel.getLevel().startCodeLine,
                false,
                level.defensePatch,
                () -> viewModel.togglePatch());
        rvCode.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCode.setAdapter(codeAdapter);

        // Assembly RecyclerView
        List<AsmLine> asmFlat = viewModel.getAsmFlat();
        asmAdapter = new AsmLineAdapter(asmFlat, -1);
        rvAsm.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAsm.setAdapter(asmAdapter);

        // Tab switching
        tabSource.setOnClickListener(v -> showTab(false));
        tabAssembly.setOnClickListener(v -> showTab(true));
        // Assembly tab hidden for levels with no objdump
        if (asmFlat.isEmpty()) {
            tabAssembly.setVisibility(View.GONE);
            tabAssembly.setEnabled(false);
        }

        // Console RecyclerView
        consoleAdapter = new ConsoleAdapter(consoleItems);
        rvConsole.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConsole.setAdapter(consoleAdapter);

        // Hex dump RecyclerView (inside right drawer)
        RecyclerView rvHexDump = root.findViewById(R.id.rv_hex_dump);
        hexDumpAdapter = new HexDumpAdapter();
        rvHexDump.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHexDump.setAdapter(hexDumpAdapter);


        // Register timeline (horizontal strip)
        RecyclerView rvTimeline = root.findViewById(R.id.rv_register_timeline);
        timelineAdapter = new RegisterTimelineAdapter();
        LinearLayoutManager tlm = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvTimeline.setLayoutManager(tlm);
        rvTimeline.setAdapter(timelineAdapter);
        // Tap: time-travel rewind to this snapshot
        timelineAdapter.setListener(snap -> {
            viewModel.restoreSnapshot(snap);
            rvCode.smoothScrollToPosition(
                    Math.max(0, Math.min(snap.codeLine, codeAdapter.getItemCount() - 1)));
        });
        
        // Timeline visibility
        View cardTimeline = root.findViewById(R.id.card_register_timeline);
        viewModel.timelineVisible.observe(getViewLifecycleOwner(), visible -> {
            cardTimeline.setVisibility(Boolean.TRUE.equals(visible) ? View.VISIBLE : View.GONE);
        });

        // Burger Menu — custom dark-themed popup
        MaterialButton btnMenu = root.findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> showCustomMenu(v));
        btnHexClose.setOnClickListener(v ->
                drawerLayout.closeDrawer(GravityCompat.END));

        // Stack canvas
        stackCanvas.setLevelId(level.id);
        stackCanvas.setStack(new ArrayList<>(level.initialStack));
        // Long-press on any slot → show inspection sheet
        stackCanvas.setBlockLongPressListener((idx, block) -> {
            if (!isAdded()) return;
            StackInspectSheet sheet = StackInspectSheet.newInstance(block);
            sheet.show(getChildFragmentManager(), "stack_inspect");
        });

        // Payload preset buttons
        for (String[] preset : level.payloadPresets) {
            MaterialButton btn = new MaterialButton(requireContext(),
                    null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(preset[0]);
            btn.setTextSize(11f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            btn.setLayoutParams(lp);
            String val = preset[1];
            btn.setOnClickListener(v -> {
                if (etInput != null) etInput.setText(val);
            });
            layoutPresets.addView(btn);
        }

        // ── LiveData observers ────────────────────────────────────────────────

        viewModel.activeLineIndex.observe(getViewLifecycleOwner(), idx -> {
            codeAdapter.setActiveLine(idx);
            rvCode.scrollToPosition(idx);
        });

        viewModel.isPatched.observe(getViewLifecycleOwner(), patched -> {
            codeAdapter.setPatched(patched);
            stackCanvas.setIsPatched(patched);
        });

        viewModel.stack.observe(getViewLifecycleOwner(), stack -> {
            stackCanvas.setStack(stack);
        });

        viewModel.espIndex.observe(getViewLifecycleOwner(), idx -> {
            if (idx < 0) return;
            stackCanvas.setEspIndex(idx);
        });

        viewModel.ebpIndex.observe(getViewLifecycleOwner(), idx -> {
            stackCanvas.setEbpIndex(idx);
        });

        viewModel.statusTitle.observe(getViewLifecycleOwner(), t -> tvStatusTitle.setText(t));
        viewModel.statusDesc.observe(getViewLifecycleOwner(), d -> tvStatusDesc.setText(d));

        viewModel.statusType.observe(getViewLifecycleOwner(), type -> {
            int color;
            switch (type) {
                case "danger":  color = ContextCompat.getColor(requireContext(), R.color.danger); break;
                case "success": color = ContextCompat.getColor(requireContext(), R.color.success); break;
                case "warn":    color = ContextCompat.getColor(requireContext(), R.color.warning); break;
                default:        color = ContextCompat.getColor(requireContext(), R.color.text_muted); break;
            }
            tvStatusTitle.setTextColor(color);
        });

        // step still drives StackCanvasView visual phase
        viewModel.step.observe(getViewLifecycleOwner(), s -> stackCanvas.setStep(s.intValue()));

        // waitingForInput drives button gating and input panel visibility
        viewModel.waitingForInput.observe(getViewLifecycleOwner(), waiting -> {
            boolean done = Boolean.TRUE.equals(viewModel.step.getValue())
                    && (int)(viewModel.step.getValue() * 10) == 1000;
            layoutInput.setVisibility(Boolean.TRUE.equals(waiting) ? View.VISIBLE : View.GONE);
            btnNextStep.setEnabled(!Boolean.TRUE.equals(waiting) && !done);
            if (btnNextInstr != null) btnNextInstr.setEnabled(!Boolean.TRUE.equals(waiting) && !done);
        });

        // Register values auto-update simRegs LiveData — no inline panel needed
        // (accessible via the Registers button → RegisterViewerSheet)


        // Assembly cursor observer
        viewModel.activeAsmInstrIdx.observe(getViewLifecycleOwner(), idx -> {
            if (idx == null || idx < 0) return;
            asmAdapter.setActiveIdx(idx);
            if (isAsmTabActive) {
                rvAsm.scrollToPosition(Math.max(0, idx - 2));
            }
        });

        viewModel.consoleOut.observe(getViewLifecycleOwner(), items -> {
            consoleItems.clear();
            consoleItems.addAll(items);
            consoleAdapter.notifyDataSetChanged();
            if (!consoleItems.isEmpty())
                rvConsole.scrollToPosition(consoleItems.size() - 1);
        });

        // Register timeline observer
        viewModel.snapshots.observe(getViewLifecycleOwner(), snaps -> {
            if (snaps == null) return;
            int newCount = snaps.size();
            if (newCount > lastSnapshotCount) {
                for (int i = lastSnapshotCount; i < newCount; i++) {
                    timelineAdapter.addSnapshot(snaps.get(i));
                }
                lastSnapshotCount = newCount;
            } else if (newCount < lastSnapshotCount && newCount > 0) {
                // Time-travel truncation (rewinding + taking new action)
                timelineAdapter.truncateSnapshots(newCount);
                lastSnapshotCount = newCount;
            } else if (newCount == 0) {
                // Reset
                timelineAdapter.clearSnapshots();
                lastSnapshotCount = 0;
            }
        });

        viewModel.activeSnapshotIndex.observe(getViewLifecycleOwner(), idx -> {
            if (idx != null && idx >= 0) {
                timelineAdapter.setActiveIndex(idx);
                rvTimeline.smoothScrollToPosition(idx);
            }
        });

        viewModel.hexRows.observe(getViewLifecycleOwner(), rawRows -> {
            if (rawRows == null || rawRows.isEmpty()) return;
            // Auto-open the drawer the first time payload data arrives
            if (!hexDataReceived) {
                hexDataReceived = true;
            //    drawerLayout.openDrawer(GravityCompat.END);
            }
            // Convert Object[] rows → HexDumpAdapter.HexRow
            List<HexDumpAdapter.HexRow> hexRows = new ArrayList<>();
            for (Object[] raw : rawRows) {
                String addr   = (String) raw[0];
                byte[] bytes  = (byte[]) raw[1];
                int[]  status = (int[])  raw[2];
                HexDumpAdapter.ByteStatus[] statArr =
                        new HexDumpAdapter.ByteStatus[HexDumpAdapter.BYTES_PER_ROW];
                for (int i = 0; i < HexDumpAdapter.BYTES_PER_ROW; i++) {
                    int s = (i < status.length) ? status[i] : 0;
                    statArr[i] = s == 2 ? HexDumpAdapter.ByteStatus.OVERFLOW
                               : s == 1 ? HexDumpAdapter.ByteStatus.FILLED
                               : HexDumpAdapter.ByteStatus.EMPTY;
                }
                hexRows.add(new HexDumpAdapter.HexRow(addr, bytes, statArr));
            }
            hexDumpAdapter.submitRows(hexRows);
        });

        viewModel.toast.observe(getViewLifecycleOwner(), t -> {
            if (t == null) return;
            boolean isSuccess = Boolean.TRUE.equals(t.second);
            int bg = isSuccess
                    ? ContextCompat.getColor(requireContext(), R.color.success)
                    : ContextCompat.getColor(requireContext(), R.color.danger);
            Snackbar sb = Snackbar.make(snackbarAnchor, t.first, 4000);
            sb.getView().setBackgroundColor(bg);
            sb.setTextColor(0xFFFFFFFF);
            sb.show();
            vibrate(isSuccess ? new long[]{50} : new long[]{200, 100, 200});
        });

        // ── Button listeners ──────────────────────────────────────────────────

        btnNextStep.setOnClickListener(v -> {
            vibrate(new long[]{20});
            viewModel.handleNextStep();
        });

        btnNextInstr.setOnClickListener(v -> {
            vibrate(new long[]{20});
            viewModel.handleNextInstruction();
        });

        btnReset.setOnClickListener(v -> viewModel.reset());


        btnNextStage.setOnClickListener(v -> {
            if (onNextLevel != null) onNextLevel.run();
        });

        btnSend.setOnClickListener(v -> submitInput());
        etInput.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { submitInput(); return true; }
            return false;
        });

        // Registers button → open RegisterViewerSheet with current snapshot
        root.findViewById(R.id.btn_peek_assembly).setOnClickListener(v -> {
            Map<String, String> regs = viewModel.simRegs.getValue();
            if (regs == null || regs.isEmpty()) {
                com.google.android.material.snackbar.Snackbar
                        .make(snackbarAnchor, "No register data yet — step through the code first", 2000)
                        .show();
                return;
            }
            // Build a human-readable step label
            Float stepVal = viewModel.step.getValue();
            String stepLabel = stepVal != null ? "Step " + (int)(stepVal * 10) : "";
            RegisterViewerSheet sheet = RegisterViewerSheet.newInstance(regs, stepLabel);
            sheet.show(getChildFragmentManager(), "registers");
        });



        viewModel.getsReached.observe(getViewLifecycleOwner(), reached -> {
            if (!Boolean.TRUE.equals(reached)) return;
            showGetsTriggerDialog();
        });
    }

    /**
     * Shows the 'Generate Payload / Enter Normal Value' dialog when gets() is reached.
     * For Level 3 the positive button opens the ROP Editor.
     * For Level 2B it opens the Payload Builder wizard.
     */
    private void showGetsTriggerDialog() {
        boolean isLevel3 = "3".equals(level.id);

        String positiveLabel = isLevel3 ? "Open ROP Editor" : "Generate Payload";
        String message = isLevel3
                ? "gets() is blocking. You can:\n• Send a normal string (it returns safely)\n" +
                  "• Use the NX Demo preset (shellcode → SIGSEGV)\n" +
                  "• Open the ROP Editor to build your chain"
                : "The program is waiting on stdin. How do you want to respond?";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_SecViz_Dialog)
                .setTitle("🔴  gets() reached")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Normal Value", (dlg, w) -> {
                    viewModel.consumeGetsReached();
                    layoutInput.setVisibility(View.VISIBLE);
                    btnNextStep.setEnabled(false);
                })
                .setPositiveButton(positiveLabel, (dlg, w) -> {
                    if (isLevel3) {
                        RopEditorFragment editor = RopEditorFragment.newInstance(level.ropGadgets);
                        editor.setOnRopPayloadReady(payload -> viewModel.submitRopPayload(payload));
                        editor.show(getChildFragmentManager(), "rop_editor");
                    } else {
                        List<StackBlock> currentStack = viewModel.getStack();
                        if (currentStack == null) return;
                        PayloadBuilderFragment builder = PayloadBuilderFragment.newInstance(
                                currentStack, level.objdump != null ? level.objdump : "");
                        builder.setOnPayloadReady((junkStart, junkEnd, targetIdx, address) ->
                                viewModel.submitPayload(junkStart, junkEnd, targetIdx, address));
                        builder.setOnNormalInputRequested(() ->
                                layoutInput.setVisibility(View.VISIBLE));
                        builder.show(getChildFragmentManager(), "payload_builder");
                    }
                })
                .show();
    }

    /**
     * Switch between Source and Assembly tabs.
     * @param showAsm true → show Assembly, false → show Source
     */
    private void showTab(boolean showAsm) {
        isAsmTabActive = showAsm;

        // Toggle RecyclerView visibility
        rvCode.setVisibility(showAsm ? View.GONE  : View.VISIBLE);
        rvAsm .setVisibility(showAsm ? View.VISIBLE : View.GONE);

        // Active tab: accent text + bottom border drawable; inactive: muted + plain
        int activeColor   = ContextCompat.getColor(requireContext(), R.color.accent);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        tabSource  .setTextColor(showAsm ? inactiveColor : activeColor);
        tabAssembly.setTextColor(showAsm ? activeColor   : inactiveColor);
        tabSource  .setBackgroundResource(showAsm ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);
        tabAssembly.setBackgroundResource(showAsm ? R.drawable.bg_tab_active   : R.drawable.bg_tab_inactive);

        // If switching to Assembly, scroll to current instruction immediately
        if (showAsm) {
            Integer idx = viewModel.activeAsmInstrIdx.getValue();
            if (idx != null && idx >= 0) {
                rvAsm.scrollToPosition(Math.max(0, idx - 2));
            }
        }
    }

    private void submitInput() {

        if (etInput == null) return;
        String input = etInput.getText() != null ? etInput.getText().toString() : "";
        if (input.isEmpty()) return;
        viewModel.setUserInput(input);
        viewModel.submitInput(input);
        stackCanvas.setUserInput(input);
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
    }

    private void vibrate(long[] pattern) {
        Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
    }

    private void showCustomMenu(View anchor) {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(requireContext());
        View panel = inflater.inflate(R.layout.popup_menu_panel, null, false);

        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        boolean capturing = Boolean.TRUE.equals(viewModel.captureEnabled.getValue());
        boolean visible   = Boolean.TRUE.equals(viewModel.timelineVisible.getValue());

        // Capture badge
        TextView captureBadge = panel.findViewById(R.id.mi_capture_badge);
        TextView captureIcon  = panel.findViewById(R.id.mi_capture_icon);
        if (capturing) {
            captureBadge.setText("ON");
            captureBadge.setTextColor(0xFF3FB950);
            captureIcon.setText("◉");
            captureIcon.setTextColor(0xFF3FB950);
        } else {
            captureBadge.setText("OFF");
            captureBadge.setTextColor(0xFF8B949E);
            captureIcon.setText("◯");
            captureIcon.setTextColor(0xFF8B949E);
        }

        // Timeline row
        View miTimeline = panel.findViewById(R.id.mi_timeline);
        TextView timelineBadge = panel.findViewById(R.id.mi_timeline_badge);
        if (capturing || visible) {
            miTimeline.setVisibility(View.VISIBLE);
            if (visible) {
                timelineBadge.setText("VISIBLE");
                timelineBadge.setTextColor(0xFF58A6FF);
            } else {
                timelineBadge.setText("HIDDEN");
                timelineBadge.setTextColor(0xFF8B949E);
            }
        } else {
            miTimeline.setVisibility(View.GONE);
        }

        // Objdump row
        View miObjdump = panel.findViewById(R.id.mi_objdump);

        android.widget.PopupWindow popup = new android.widget.PopupWindow(
                panel,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setElevation(24f);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0x00000000));
        popup.setOutsideTouchable(true);

        // Click listeners
        panel.findViewById(R.id.mi_hex).setOnClickListener(v2 -> {
            popup.dismiss();
            drawerLayout.openDrawer(GravityCompat.END);
        });
        panel.findViewById(R.id.mi_capture).setOnClickListener(v2 -> {
            popup.dismiss();
            viewModel.toggleCaptureEnabled();
        });
        miTimeline.setOnClickListener(v2 -> {
            popup.dismiss();
            viewModel.toggleTimelineVisible();
        });
        miObjdump.setOnClickListener(v2 -> {
            popup.dismiss();
            ObjdumpViewerFragment.newInstance(level.objdump)
                    .show(getChildFragmentManager(), "objdump");
        });

        int[] loc = new int[2];
        anchor.getLocationInWindow(loc);
        popup.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY,
                loc[0] + anchor.getWidth() - panel.getMeasuredWidth(),
                loc[1] + anchor.getHeight() + 8);
    }

    /** Read assets/registers/<levelId>.json and pass its content to the ViewModel. */
    private void loadLevelRegisters(String levelId) {
        String filename = "registers/" + levelId + ".json";
        try (java.io.InputStream is = requireContext().getAssets().open(filename)) {
            byte[] buf = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(buf);
            viewModel.loadRegisterJson(new String(buf, java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.FileNotFoundException ignored) {
            // No register file for this level — silently skip
        } catch (Exception e) {
            android.util.Log.w("LevelFragment", "loadLevelRegisters error: " + e.getMessage());
        }
    }
}

