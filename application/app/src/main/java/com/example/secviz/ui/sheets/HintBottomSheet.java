package com.example.secviz.ui.sheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.secviz.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

/**
 * Bottom sheet that shows a contextual hint for the current level.
 * The hint text is hidden behind a lock until the user explicitly taps "Show Hint".
 */
public class HintBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_HINT = "hint";

    public static HintBottomSheet newInstance(String hint) {
        HintBottomSheet sheet = new HintBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_HINT, hint);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public int getTheme() {
        return com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hint_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        String hint = getArguments() != null ? getArguments().getString(ARG_HINT, "") : "";

        TextView tvHint          = root.findViewById(R.id.tv_hint_text);
        View    layoutLocked     = root.findViewById(R.id.layout_hint_locked);
        MaterialButton btnShow   = root.findViewById(R.id.btn_show_hint);

        tvHint.setText(hint);

        btnShow.setOnClickListener(v -> {
            if (tvHint.getVisibility() == View.VISIBLE) {
                // Hide again
                tvHint.setVisibility(View.GONE);
                layoutLocked.setVisibility(View.VISIBLE);
                btnShow.setText("Show Hint");
            } else {
                // Reveal
                layoutLocked.setVisibility(View.GONE);
                tvHint.setVisibility(View.VISIBLE);
                btnShow.setText("Hide Hint");
            }
        });
    }
}
