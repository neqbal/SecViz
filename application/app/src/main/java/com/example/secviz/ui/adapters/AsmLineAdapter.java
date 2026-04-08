package com.example.secviz.ui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.example.secviz.data.AsmLine;

import java.util.List;

/**
 * RecyclerView adapter for the Assembly tab.
 *
 * Visual conventions:
 *   • Header rows (function labels)  → accent colour, bold, no arrow
 *   • Active instruction row         → left accent bar, ► arrow, bright text
 *   • Normal instruction row         → dimmed monospace text
 *   • Empty / separator rows         → just whitespace
 */
public class AsmLineAdapter extends RecyclerView.Adapter<AsmLineAdapter.VH> {

    private final List<AsmLine> lines;
    private int activeIdx = -1;

    public AsmLineAdapter(List<AsmLine> lines, int activeIdx) {
        this.lines     = lines;
        this.activeIdx = activeIdx;
    }

    /** Call from UI thread; triggers a full rebind (list is small, ~200 lines). */
    public void setActiveIdx(int idx) {
        this.activeIdx = idx;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asm_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Context ctx  = h.itemView.getContext();
        AsmLine line = lines.get(position);

        boolean isActive = (position == activeIdx);

        // ── active bar & arrow ────────────────────────────────────────────────
        h.activeBar.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
        h.tvArrow.setText(isActive ? "►" : "");

        // ── text styling ──────────────────────────────────────────────────────
        h.tvText.setText(line.rawText);

        if (line.isHeader) {
            // Function-label line: accent colour + bold
            h.tvText.setTextColor(ContextCompat.getColor(ctx, R.color.accent));
            h.tvText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            h.tvText.setTextSize(11f);
            h.itemView.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.bg_elevated));
        } else if (isActive) {
            // Highlighted instruction
            h.tvText.setTextColor(
                    ContextCompat.getColor(ctx, R.color.text_primary));
            h.tvText.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            h.tvText.setTextSize(11f);
            h.itemView.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.bg_elevated));
        } else {
            // Normal instruction or blank
            h.tvText.setTextColor(
                    ContextCompat.getColor(ctx, R.color.text_muted));
            h.tvText.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            h.tvText.setTextSize(11f);
            h.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View     activeBar;
        TextView tvArrow;
        TextView tvText;

        VH(@NonNull View v) {
            super(v);
            activeBar = v.findViewById(R.id.view_asm_active_bar);
            tvArrow   = v.findViewById(R.id.tv_asm_arrow);
            tvText    = v.findViewById(R.id.tv_asm_text);
        }
    }
}
