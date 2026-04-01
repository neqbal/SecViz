package com.example.secviz.ui.adapters;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;

import java.util.List;

public class ConsoleAdapter extends RecyclerView.Adapter<ConsoleAdapter.VH> {

    private final List<Pair<String, Boolean>> items;

    public ConsoleAdapter(List<Pair<String, Boolean>> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_console_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Context ctx = holder.itemView.getContext();
        Pair<String, Boolean> item = items.get(position);
        String text = item.first;
        boolean isGarbage = Boolean.TRUE.equals(item.second);

        if (isGarbage) {
            SpannableString ss = new SpannableString(text);
            ss.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(ctx, R.color.danger)),
                    0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.tvText.setText(ss);
        } else {
            holder.tvText.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
            holder.tvText.setText(text);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvText;
        VH(@NonNull View v) {
            super(v);
            tvText = v.findViewById(R.id.tv_console_text);
        }
    }
}
