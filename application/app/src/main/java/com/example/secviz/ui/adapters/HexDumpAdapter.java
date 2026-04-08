package com.example.secviz.ui.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;

import java.util.ArrayList;
import java.util.List;

public class HexDumpAdapter extends RecyclerView.Adapter<HexDumpAdapter.VH> {

    public static final int BYTES_PER_ROW = 16;

    public enum ByteStatus { EMPTY, FILLED, OVERFLOW }

    public static class HexRow {
        public final String address;
        public final byte[] bytes;
        public final ByteStatus[] status;

        public HexRow(String address, byte[] bytes, ByteStatus[] status) {
            this.address = address;
            this.bytes = bytes;
            this.status = status;
        }
    }

    private List<HexRow> rows = new ArrayList<>();
    private final java.util.Set<Integer> rowsToFlash = new java.util.HashSet<>();

    public void submitRows(List<HexRow> newRows) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return rows.size(); }
            @Override public int getNewListSize() { return newRows.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return rows.get(oldPos).address.equals(newRows.get(newPos).address);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                HexRow oldRow = rows.get(oldPos);
                HexRow newRow = newRows.get(newPos);
                return java.util.Arrays.equals(oldRow.status, newRow.status)
                        && java.util.Arrays.equals(oldRow.bytes,  newRow.bytes);
            }
        });

        // Mark only changed rows for flashing
        rowsToFlash.clear();
        for (int i = 0; i < newRows.size(); i++) {
            boolean hasData = false;
            for (ByteStatus st : newRows.get(i).status) {
                if (st != ByteStatus.EMPTY) { hasData = true; break; }
            }
            if (!hasData) continue;

            // Flash only if this row didn't exist before or its content changed
            if (i >= rows.size()) {
                rowsToFlash.add(i);
            } else {
                if (!java.util.Arrays.equals(rows.get(i).bytes,  newRows.get(i).bytes)
                        || !java.util.Arrays.equals(rows.get(i).status, newRows.get(i).status)) {
                    rowsToFlash.add(i);
                }
            }
        }

        rows = newRows;
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hex_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        bind(holder, rows.get(position), position);
    }

    @Override
    public int getItemCount() { return rows.size(); }

    private void bind(VH holder, HexRow row, int position) {
        Context ctx = holder.itemView.getContext();
        holder.tvAddress.setText(row.address);

        SpannableStringBuilder hexSb   = new SpannableStringBuilder();
        SpannableStringBuilder asciiSb = new SpannableStringBuilder();

        int colorFilled   = ContextCompat.getColor(ctx, R.color.success);
        int colorOverflow = ContextCompat.getColor(ctx, R.color.danger);
        int colorEmpty    = ContextCompat.getColor(ctx, R.color.text_muted);

        for (int i = 0; i < BYTES_PER_ROW; i++) {
            ByteStatus st = row.status[i];
            byte b = row.bytes[i];

            String hexStr  = st == ByteStatus.EMPTY ? "  " : String.format("%02X", b & 0xFF);
            int hexStart   = hexSb.length();
            hexSb.append(hexStr);
            int hexEnd     = hexSb.length();

            int hexColor = st == ByteStatus.OVERFLOW ? colorOverflow
                    : st == ByteStatus.FILLED ? colorFilled
                    : colorEmpty;
            hexSb.setSpan(new ForegroundColorSpan(hexColor),
                    hexStart, hexEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            hexSb.append(i == 7 ? "  " : " ");

            char c = (st == ByteStatus.EMPTY) ? '.'
                    : (b >= 32 && b < 127) ? (char) b : '.';
            int asciiStart = asciiSb.length();
            asciiSb.append(c);
            asciiSb.setSpan(new ForegroundColorSpan(hexColor),
                    asciiStart, asciiSb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.tvHex.setText(hexSb);
        holder.tvAscii.setText(asciiSb);

        // Only flash rows that actually changed since last submitRows()
        if (rowsToFlash.contains(position)) {
            flashRow(holder.itemView, ctx);
            rowsToFlash.remove(position);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void flashRow(View view, Context ctx) {
        int from = Color.argb(60, 88, 166, 255);
        int to   = Color.TRANSPARENT;
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        anim.setDuration(600);
        anim.addUpdateListener(a -> view.setBackgroundColor((int) a.getAnimatedValue()));
        anim.start();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAddress, tvHex, tvAscii;
        VH(@NonNull View v) {
            super(v);
            tvAddress = v.findViewById(R.id.tv_hex_addr);
            tvHex     = v.findViewById(R.id.tv_hex_bytes);
            tvAscii   = v.findViewById(R.id.tv_hex_ascii);
        }
    }
}