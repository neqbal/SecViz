package com.example.secviz.ui.sheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.example.secviz.data.CodeLine;
import com.example.secviz.data.StackBlock;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AssemblyBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_LINE_TEXT = "lineText";
    private static final String ARG_ASM = "asm";
    private static final String ARG_RSP = "rsp";
    private static final String ARG_RBP = "rbp";

    public static AssemblyBottomSheet newInstance(CodeLine line, String rsp, String rbp) {
        AssemblyBottomSheet sheet = new AssemblyBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_LINE_TEXT, line != null ? line.text : "");
        if (line != null && line.asm != null) {
            args.putStringArray(ARG_ASM, line.asm.toArray(new String[0]));
        } else {
            args.putStringArray(ARG_ASM, new String[0]);
        }
        args.putString(ARG_RSP, rsp);
        args.putString(ARG_RBP, rbp);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_assembly, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) return;

        String lineText = args.getString(ARG_LINE_TEXT, "");
        String[] asmLines = args.getStringArray(ARG_ASM);
        String rsp = args.getString(ARG_RSP, "—");
        String rbp = args.getString(ARG_RBP, "—");

        ((TextView) view.findViewById(R.id.tv_asm_code_line)).setText(lineText.trim());
        ((TextView) view.findViewById(R.id.tv_rsp)).setText("RSP: " + rsp);
        ((TextView) view.findViewById(R.id.tv_rbp)).setText("RBP: " + rbp);

        RecyclerView rv = view.findViewById(R.id.rv_asm_lines);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(buildAsmAdapter(asmLines != null ? asmLines : new String[0]));
    }

    private RecyclerView.Adapter<AsmVH> buildAsmAdapter(String[] lines) {
        return new RecyclerView.Adapter<AsmVH>() {
            @NonNull
            @Override
            public AsmVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView tv = new TextView(parent.getContext());
                tv.setTextSize(12f);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setPadding(16, 6, 16, 6);
                tv.setTextColor(0xFF79C0FF);
                return new AsmVH(tv);
            }

            @Override
            public void onBindViewHolder(@NonNull AsmVH holder, int position) {
                holder.tv.setText(lines[position]);
            }

            @Override
            public int getItemCount() { return lines.length; }
        };
    }

    static class AsmVH extends RecyclerView.ViewHolder {
        TextView tv;
        AsmVH(TextView v) { super(v); tv = v; }
    }
}
