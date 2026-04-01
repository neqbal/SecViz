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

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.example.secviz.data.Level;
import com.example.secviz.data.StackBlock;
import com.example.secviz.ui.adapters.CodeLineAdapter;
import com.example.secviz.ui.adapters.ConsoleAdapter;
import com.example.secviz.ui.adapters.HexDumpAdapter;
import com.example.secviz.ui.adapters.RegisterTimelineAdapter;
import com.example.secviz.ui.sheets.AssemblyBottomSheet;
import com.example.secviz.ui.views.StackCanvasView;
import com.example.secviz.viewmodel.LevelViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

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
    private MaterialButton btnNextStep, btnReset, btnNextStage, btnSend;
    private View snackbarAnchor;
    private DrawerLayout drawerLayout;
    private boolean hexDataReceived = false;
    private CodeLineAdapter codeAdapter;
    private ConsoleAdapter consoleAdapter;
    private HexDumpAdapter hexDumpAdapter;
    private RegisterTimelineAdapter timelineAdapter;
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
        RecyclerView rvHexDump = root.findViewById(R.id.rv_hex_dump);
        MaterialButton btnHexToggle = root.findViewById(R.id.btn_hex_toggle);
        MaterialButton btnHexClose  = root.findViewById(R.id.btn_hex_close);

        // Header
        tvLevelTitle.setText(level.title);
        tvLevelSubtitle.setText(level.subtitle);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(LevelViewModel.class);
        viewModel.init(level);

        // Code RecyclerView
        codeAdapter = new CodeLineAdapter(
                level.code,
                viewModel.getLevel().startCodeLine,
                false,
                level.defensePatch,
                () -> viewModel.togglePatch());
        rvCode.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCode.setAdapter(codeAdapter);

        // Console RecyclerView
        consoleAdapter = new ConsoleAdapter(consoleItems);
        rvConsole.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConsole.setAdapter(consoleAdapter);

        // Hex dump RecyclerView (inside right drawer)
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

        // Burger Menu
        MaterialButton btnMenu = root.findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), v);
            android.view.Menu menu = popup.getMenu();
            
            menu.add(0, 1, 0, "View Hex Dump");
            
            String captureText = Boolean.TRUE.equals(viewModel.captureEnabled.getValue()) 
                    ? "Stop Capturing Snapshots" : "Start Capturing Snapshots";
            menu.add(0, 2, 0, captureText);
            
            String viewText = Boolean.TRUE.equals(viewModel.timelineVisible.getValue()) 
                    ? "Hide Timeline Strip" : "Show Timeline Strip";
            menu.add(0, 3, 0, viewText);

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        drawerLayout.openDrawer(GravityCompat.END);
                        return true;
                    case 2:
                        viewModel.toggleCaptureEnabled();
                        return true;
                    case 3:
                        viewModel.toggleTimelineVisible();
                        return true;
                }
                return false;
            });
            popup.show();
        });
        btnHexClose.setOnClickListener(v ->
                drawerLayout.closeDrawer(GravityCompat.END));

        // Stack canvas
        stackCanvas.setLevelId(level.id);
        stackCanvas.setStack(new ArrayList<>(level.initialStack));

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

        viewModel.step.observe(getViewLifecycleOwner(), s -> {
            int stepInt = (int)(s * 10);
            stackCanvas.setStep(s.intValue());
            boolean waitingForInput = stepInt == 40;
            layoutInput.setVisibility(waitingForInput ? View.VISIBLE : View.GONE);
            btnNextStep.setEnabled(!waitingForInput && stepInt != 1000);
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
                drawerLayout.openDrawer(GravityCompat.END);
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

        btnReset.setOnClickListener(v -> viewModel.reset());

        btnNextStage.setOnClickListener(v -> {
            if (onNextLevel != null) onNextLevel.run();
        });

        btnSend.setOnClickListener(v -> submitInput());
        etInput.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { submitInput(); return true; }
            return false;
        });

        // Assembly sheet
        root.findViewById(R.id.btn_peek_assembly).setOnClickListener(v -> {
            Integer lineIdx = viewModel.activeLineIndex.getValue();
            if (lineIdx == null) return;
            List<StackBlock> stk = viewModel.stack.getValue();
            String rsp = (stk != null && viewModel.espIndex.getValue() != null
                    && viewModel.espIndex.getValue() >= 0
                    && stk.size() > viewModel.espIndex.getValue())
                    ? stk.get(viewModel.espIndex.getValue()).value : "—";
            String rbp = (stk != null && viewModel.ebpIndex.getValue() != null
                    && viewModel.ebpIndex.getValue() >= 0
                    && stk.size() > viewModel.ebpIndex.getValue())
                    ? stk.get(viewModel.ebpIndex.getValue()).value : "—";
            AssemblyBottomSheet.newInstance(level.code.get(lineIdx), rsp, rbp)
                    .show(getChildFragmentManager(), "asm");
        });
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
}
