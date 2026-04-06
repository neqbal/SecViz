package com.example.secviz.ui.dialogs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen objdump viewer.
 * Symbol labels like "<win>:" are rendered in accent colour.
 * Long-pressing an address token copies it to the clipboard.
 */
public class ObjdumpViewerFragment extends DialogFragment {

    private static final String ARG_OBJDUMP = "objdump";

    public static ObjdumpViewerFragment newInstance(String objdump) {
        ObjdumpViewerFragment f = new ObjdumpViewerFragment();
        Bundle b = new Bundle();
        b.putString(ARG_OBJDUMP, objdump);
        f.setArguments(b);
        return f;
    }

    @Override
    public int getTheme() {
        return com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_objdump_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        String objdump = getArguments() != null ? getArguments().getString(ARG_OBJDUMP, "") : "";

        root.findViewById(R.id.btn_objdump_close).setOnClickListener(v -> dismiss());

        RecyclerView rv = root.findViewById(R.id.rv_objdump);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Split objdump into lines
        String[] lines = objdump.split("\n", -1);
        List<String> lineList = new ArrayList<>();
        for (String l : lines) lineList.add(l);

        rv.setAdapter(new ObjdumpAdapter(lineList, address -> {
            // Copy to clipboard
            ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("address", address));
                Snackbar.make(root, "Copied " + address + " to clipboard", 2000).show();
            }
        }));
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    interface OnAddressCopied { void onCopy(String address); }

    static class ObjdumpAdapter extends RecyclerView.Adapter<ObjdumpAdapter.VH> {
        private final List<String> lines;
        private final OnAddressCopied callback;

        ObjdumpAdapter(List<String> lines, OnAddressCopied callback) {
            this.lines = lines;
            this.callback = callback;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(4, 2, 4, 2);
            tv.setTextSize(11f);
            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String line = lines.get(position);
            TextView tv = (TextView) holder.itemView;

            // Color-code by line type
            if (line.isEmpty()) {
                tv.setText("");
                tv.setTextColor(0xFF8B949E);
            } else if (line.contains("<win>:") || line.contains("<vuln>:") ||
                       line.contains("<main>:") || line.contains("<_start>:") ||
                       line.contains("<puts@plt>:") || line.contains("<gets@plt>:") ||
                       line.contains("<_fini>:") || line.contains("<_init>:")) {
                // Symbol label
                tv.setText(line);
                tv.setTextColor(0xFF58A6FF); // accent blue
                tv.setTypeface(android.graphics.Typeface.MONOSPACE,
                        android.graphics.Typeface.BOLD);
            } else if (line.startsWith("Disassembly of")) {
                tv.setText(line);
                tv.setTextColor(0xFF3FB950); // green section header
                tv.setTypeface(android.graphics.Typeface.MONOSPACE,
                        android.graphics.Typeface.BOLD);
            } else {
                tv.setText(line);
                tv.setTextColor(0xFFC9D1D9);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE,
                        android.graphics.Typeface.NORMAL);
            }

            // Long-press to copy the leading address token
            tv.setOnLongClickListener(v -> {
                String trimmed = line.trim();
                int colon = trimmed.indexOf(':');
                if (colon > 0) {
                    String addr = "0x" + trimmed.substring(0, colon).trim();
                    callback.onCopy(addr);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() { return lines.size(); }

        static class VH extends RecyclerView.ViewHolder {
            VH(@NonNull View itemView) { super(itemView); }
        }
    }
}
