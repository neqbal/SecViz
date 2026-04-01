package com.example.secviz.ui.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.example.secviz.data.RegisterSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RegisterTimelineAdapter
        extends RecyclerView.Adapter<RegisterTimelineAdapter.VH> {

    public interface OnSnapshotClickListener {
        void onSnapshotClick(RegisterSnapshot snapshot);
    }

    private final List<RegisterSnapshot> snapshots = new ArrayList<>();
    private int activeIndex = -1;
    private OnSnapshotClickListener listener;

    public void setListener(OnSnapshotClickListener l) { this.listener = l; }

    public void addSnapshot(RegisterSnapshot snap) {
        snapshots.add(snap);
        notifyItemInserted(snapshots.size() - 1);
    }

    public void truncateSnapshots(int newSize) {
        if (newSize >= 0 && newSize < snapshots.size()) {
            int removedCount = snapshots.size() - newSize;
            snapshots.subList(newSize, snapshots.size()).clear();
            notifyItemRangeRemoved(newSize, removedCount);
        }
    }

    public void clearSnapshots() {
        snapshots.clear();
        activeIndex = -1;
        notifyDataSetChanged();
    }

    public void setActiveIndex(int newIndex) {
        if (newIndex >= 0 && newIndex < snapshots.size() && newIndex != activeIndex) {
            int oldIndex = activeIndex;
            activeIndex = newIndex;
            if (oldIndex >= 0) notifyItemChanged(oldIndex);
            notifyItemChanged(activeIndex);
        }
    }

    public int getActiveIndex() { return activeIndex; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_register_snapshot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RegisterSnapshot snap = snapshots.get(position);
        boolean isActive  = position == activeIndex;

        // Index badge
        holder.tvIndex.setText("#" + (snap.stepIndex + 1));

        // Label
        holder.tvLabel.setText(snap.label);

        // Registers
        holder.tvRip.setText(snap.rip.isEmpty() ? "—" : snap.rip);
        holder.tvRsp.setText(snap.rsp.isEmpty() ? "—" : snap.rsp);
        holder.tvRbp.setText(snap.rbp.isEmpty() ? "—" : snap.rbp);

        // Dynamic border color based on status + active state
        int borderColor = accentForStatus(snap.statusType, holder.itemView.getContext());
        if (!isActive) borderColor = Color.argb(80,
                Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isActive ? Color.argb(60,
                        Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor))
                : Color.parseColor("#161B22"));
        bg.setCornerRadius(24f);
        bg.setStroke(isActive ? 4 : 1, borderColor);
        holder.root.setBackground(bg);

        // Elevation / scale for active card
        holder.itemView.animate()
                .scaleX(isActive ? 1.03f : 1f)
                .scaleY(isActive ? 1.03f : 1f)
                .setDuration(200)
                .start();

        // Cancel any running animator on this view before rebinding
        Object tag = holder.root.getTag(R.id.snapshot_root);
        if (tag instanceof ValueAnimator) ((ValueAnimator) tag).cancel();

        // Click listener on root (opaque) — the CardView is transparent so clicks fall through it
        holder.root.setClickable(true);
        holder.root.setFocusable(true);
        holder.root.setOnClickListener(v -> {
            if (listener != null) listener.onSnapshotClick(snap);
        });

        // Only animate the newest (active) card
        if (isActive) flashCard(holder.root, borderColor);
    }

    @Override
    public int getItemCount() { return snapshots.size(); }

    private void flashCard(View view, int accentColor) {
        int from = Color.argb(120,
                Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
        int to = Color.argb(40,
                Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        anim.setDuration(500);
        anim.addUpdateListener(a -> {
            GradientDrawable bg = (GradientDrawable) view.getBackground();
            if (bg != null) bg.setColor((int) a.getAnimatedValue());
        });
        view.setTag(R.id.snapshot_root, anim); // store so we can cancel on rebind
        anim.start();
    }

    private int accentForStatus(String status, android.content.Context ctx) {
        switch (status) {
            case "danger":  return ContextCompat.getColor(ctx, R.color.danger);
            case "success": return ContextCompat.getColor(ctx, R.color.success);
            case "warn":    return ContextCompat.getColor(ctx, R.color.warning);
            default:        return ContextCompat.getColor(ctx, R.color.accent);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView tvIndex, tvLabel, tvRip, tvRsp, tvRbp;

        VH(@NonNull View v) {
            super(v);
            root    = v.findViewById(R.id.snapshot_root);
            tvIndex = v.findViewById(R.id.tv_snap_index);
            tvLabel = v.findViewById(R.id.tv_snap_label);
            tvRip   = v.findViewById(R.id.tv_snap_rip);
            tvRsp   = v.findViewById(R.id.tv_snap_rsp);
            tvRbp   = v.findViewById(R.id.tv_snap_rbp);
        }
    }
}
