package com.example.secviz.ui.dialogs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.example.secviz.data.RopGadget;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom-sheet that simulates scanning the binary's .text section for gadgets,
 * then shows them in a list. Tapping a row reveals an inspector panel with the
 * assembly, address, description, and input alias.
 */
public class RopScannerFragment extends BottomSheetDialogFragment {

    private List<RopGadget> gadgets = new ArrayList<>();

    private TextView tvStatus;
    private LinearProgressIndicator progressScanner;
    private RecyclerView rvGadgets;
    private View panelInspector;
    private TextView tvInspectorAsm, tvInspectorAddr, tvInspectorDesc, tvInspectorAlias;

    // ── Factory ──────────────────────────────────────────────────────────────

    public static RopScannerFragment newInstance(List<RopGadget> gadgets) {
        RopScannerFragment f = new RopScannerFragment();
        f.gadgets = gadgets;
        return f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rop_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar_scanner);
        toolbar.setNavigationOnClickListener(v -> dismiss());

        tvStatus         = root.findViewById(R.id.tv_scanner_status);
        progressScanner  = root.findViewById(R.id.progress_scanner);
        rvGadgets        = root.findViewById(R.id.rv_gadgets);
        panelInspector   = root.findViewById(R.id.panel_inspector);
        tvInspectorAsm   = root.findViewById(R.id.tv_inspector_asm);
        tvInspectorAddr  = root.findViewById(R.id.tv_inspector_addr);
        tvInspectorDesc  = root.findViewById(R.id.tv_inspector_desc);
        tvInspectorAlias = root.findViewById(R.id.tv_inspector_alias);

        rvGadgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        GadgetAdapter adapter = new GadgetAdapter(gadgets, this::onGadgetSelected);
        rvGadgets.setAdapter(adapter);

        // Simulate a 1.2 s scan animation then reveal the list
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressScanner.setVisibility(View.GONE);
            tvStatus.setText("Found " + gadgets.size() + " usable gadgets.");
            tvStatus.setTextColor(0xFF3FB950);
            rvGadgets.setVisibility(View.VISIBLE);
        }, 1200);
    }

    // ── Gadget Selection ──────────────────────────────────────────────────────

    private void onGadgetSelected(RopGadget gadget) {
        panelInspector.setVisibility(View.VISIBLE);
        tvInspectorAsm.setText(gadget.asm);
        tvInspectorAddr.setText(gadget.address);
        tvInspectorDesc.setText(gadget.description);
        tvInspectorAlias.setText("alias: " + gadget.alias);
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────────────

    private static class GadgetAdapter extends RecyclerView.Adapter<GadgetAdapter.VH> {
        interface Listener { void onClick(RopGadget g); }
        private final List<RopGadget> items;
        private final Listener listener;

        GadgetAdapter(List<RopGadget> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rop_gadget, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            RopGadget g = items.get(pos);
            h.tvAddr.setText(g.address);
            h.tvAsm.setText(g.asm);
            h.tvAlias.setText(g.alias);
            h.itemView.setOnClickListener(v -> listener.onClick(g));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAddr, tvAsm, tvAlias;
            VH(View v) {
                super(v);
                tvAddr  = v.findViewById(R.id.tv_gadget_addr);
                tvAsm   = v.findViewById(R.id.tv_gadget_asm);
                tvAlias = v.findViewById(R.id.tv_gadget_alias);
            }
        }
    }
}
