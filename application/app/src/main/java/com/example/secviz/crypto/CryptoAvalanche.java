package com.example.secviz.crypto;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.secviz.R;

public class CryptoAvalanche extends AppCompatActivity {

    // Simple 4-bit S-Box (Non-linear substitution for Confusion)
    private final int[] S_BOX = {0xC, 0x5, 0x6, 0xB, 0x9, 0x0, 0xA, 0xD, 0x3, 0xE, 0xF, 0x8, 0x4, 0x7, 0x1, 0x2};

    // Simple 8-bit P-Box (Wiring permutation for Diffusion)
    private final int[] P_BOX = {3, 7, 0, 4, 1, 5, 2, 6};

    private ToggleButton[] ptButtons = new ToggleButton[8];
    private TextView[] ctViews = new TextView[8];

    private int basePlaintext = 0b00000000;
    private int baseCiphertext;

    private SwitchCompat switchSBox, switchPBox;
    private SeekBar seekRounds;
    private TextView tvScore, tvRounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_activity_avalanche);

        switchSBox = findViewById(R.id.switchSBox);
        switchPBox = findViewById(R.id.switchPBox);
        seekRounds = findViewById(R.id.seekRounds);
        tvScore    = findViewById(R.id.tvScore);
        tvRounds   = findViewById(R.id.tvRounds);

        LinearLayout layoutPlaintext  = findViewById(R.id.layoutPlaintext);
        LinearLayout layoutCiphertext = findViewById(R.id.layoutCiphertext);

        for (int i = 7; i >= 0; i--) {
            final int bitIndex = i;

            ToggleButton tb = new ToggleButton(this);
            tb.setTextOff("0");
            tb.setTextOn("1");
            tb.setChecked(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(4, 4, 4, 4);
            tb.setLayoutParams(params);
            tb.setOnCheckedChangeListener((buttonView, isChecked) -> updateCipher());
            ptButtons[7 - i] = tb;
            layoutPlaintext.addView(tb);

            TextView tv = new TextView(this);
            tv.setText("0");
            tv.setTextSize(20);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setLayoutParams(params);
            tv.setBackgroundColor(Color.LTGRAY);
            tv.setPadding(0, 16, 0, 16);
            ctViews[7 - i] = tv;
            layoutCiphertext.addView(tv);
        }

        switchSBox.setOnCheckedChangeListener((btn, b) -> updateBaseAndCipher());
        switchPBox.setOnCheckedChangeListener((btn, b) -> updateBaseAndCipher());
        seekRounds.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRounds.setText("Encryption Rounds: " + (progress + 1));
                updateBaseAndCipher();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateBaseAndCipher();
    }

    private void updateBaseAndCipher() {
        baseCiphertext = encrypt(basePlaintext);
        updateCipher();
    }

    private void updateCipher() {
        int currentPlaintext = 0;
        for (int i = 0; i < 8; i++) {
            if (ptButtons[i].isChecked()) currentPlaintext |= (1 << (7 - i));
        }
        int currentCiphertext = encrypt(currentPlaintext);
        int bitsFlipped = 0;
        for (int i = 0; i < 8; i++) {
            int bitMask    = 1 << (7 - i);
            boolean curBit = (currentCiphertext & bitMask) != 0;
            boolean base   = (baseCiphertext    & bitMask) != 0;
            ctViews[i].setText(curBit ? "1" : "0");
            if (curBit != base) { ctViews[i].setBackgroundColor(Color.rgb(255, 100, 100)); bitsFlipped++; }
            else                { ctViews[i].setBackgroundColor(Color.LTGRAY); }
        }
        float pct = (bitsFlipped / 8.0f) * 100;
        tvScore.setText(String.format("Avalanche Score: %.0f%%", pct));
        tvScore.setTextColor(pct >= 50 ? Color.rgb(0, 150, 0) : Color.rgb(13, 110, 253));
    }

    private int encrypt(int input) {
        int state = input;
        boolean useS = switchSBox.isChecked();
        boolean useP = switchPBox.isChecked();
        int rounds   = seekRounds.getProgress() + 1;
        for (int r = 0; r < rounds; r++) {
            if (useS) {
                int l = (state >> 4) & 0x0F, right = state & 0x0F;
                state = (S_BOX[l] << 4) | S_BOX[right];
            }
            if (useP) {
                int pState = 0;
                for (int i = 0; i < 8; i++) if (((state >> i) & 1) == 1) pState |= (1 << P_BOX[i]);
                state = pState;
            }
        }
        return state;
    }
}
