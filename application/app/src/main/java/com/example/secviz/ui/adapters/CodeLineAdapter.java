package com.example.secviz.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.example.secviz.data.CodeLine;
import com.example.secviz.data.DefensePatch;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CodeLineAdapter extends RecyclerView.Adapter<CodeLineAdapter.VH> {

    public interface OnPatchClickListener {
        void onPatchClick();
    }

    private final List<CodeLine> lines;
    private int activeLine;
    private boolean isPatched;
    private final DefensePatch defensePatch;
    private final OnPatchClickListener patchListener;

    public CodeLineAdapter(List<CodeLine> lines, int activeLine, boolean isPatched,
                           DefensePatch defensePatch, OnPatchClickListener patchListener) {
        this.lines = lines;
        this.activeLine = activeLine;
        this.isPatched = isPatched;
        this.defensePatch = defensePatch;
        this.patchListener = patchListener;
    }

    public void setActiveLine(int idx) {
        this.activeLine = idx;
        notifyDataSetChanged();
    }

    public void setPatched(boolean patched) {
        this.isPatched = patched;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_code_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Context ctx = holder.itemView.getContext();
        CodeLine line = lines.get(position);

        // Determine displayed text (hot-swapped if this is the patch line)
        String rawText = line.text;
        boolean isPatchLine = defensePatch != null &&
                (rawText.trim().equals(defensePatch.vulnText.trim()) ||
                 rawText.trim().equals(defensePatch.safeText.trim()));
        if (isPatchLine) {
            rawText = isPatched ? defensePatch.safeText : defensePatch.vulnText;
        }

        // Active line highlight
        int bgColor = position == activeLine
                ? ContextCompat.getColor(ctx, R.color.bg_elevated)
                : Color.TRANSPARENT;
        holder.itemView.setBackgroundColor(bgColor);

        holder.tvLineNumber.setText(String.valueOf(position + 1));
        holder.tvCodeText.setText(syntaxHighlight(rawText, ctx));

        // Patch button
        if (isPatchLine && defensePatch != null) {
            holder.btnPatch.setVisibility(View.VISIBLE);
            holder.btnPatch.setText(isPatched
                    ? ctx.getString(R.string.patched)
                    : ctx.getString(R.string.fix));
            int tint = isPatched
                    ? ContextCompat.getColor(ctx, R.color.accent)
                    : ContextCompat.getColor(ctx, R.color.success);
            holder.btnPatch.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(tint));
            holder.btnPatch.setOnClickListener(v -> {
                if (patchListener != null) patchListener.onPatchClick();
            });
        } else {
            holder.btnPatch.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return lines.size(); }

    // ── Syntax Highlighting ───────────────────────────────────────────────────

    private SpannableStringBuilder syntaxHighlight(String text, Context ctx) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);

        int keyword = ContextCompat.getColor(ctx, R.color.syntax_keyword);
        int function = ContextCompat.getColor(ctx, R.color.syntax_function);
        int string = ContextCompat.getColor(ctx, R.color.syntax_string);
        int number = ContextCompat.getColor(ctx, R.color.syntax_number);
        int comment = ContextCompat.getColor(ctx, R.color.syntax_comment);

        applyPattern(sb, "\\b(void|char|int|long|if|return|include)\\b", keyword);
        applyPattern(sb, "\\b(vuln|main|read|printf|puts|system|__stack_chk_fail)\\b", function);
        applyPattern(sb, "\"[^\"]*\"", string);
        applyPattern(sb, "\\b[0-9]+\\b", number);
        applyPattern(sb, "//.*", comment);
        applyPattern(sb, "#[a-zA-Z]+", keyword);

        return sb;
    }

    private void applyPattern(SpannableStringBuilder sb, String pattern, int color) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(sb.toString());
        while (m.find()) {
            sb.setSpan(new ForegroundColorSpan(color),
                    m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLineNumber, tvCodeText;
        MaterialButton btnPatch;

        VH(@NonNull View v) {
            super(v);
            tvLineNumber = v.findViewById(R.id.tv_line_number);
            tvCodeText = v.findViewById(R.id.tv_code_text);
            btnPatch = v.findViewById(R.id.btn_patch);
        }
    }
}
