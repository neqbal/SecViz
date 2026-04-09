package com.example.secviz.crypto;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

import com.example.secviz.R;

public class CryptoKeyExchange extends AppCompatActivity {

    private int publicColor;
    private int aliceSecret;
    private int bobSecret;

    private int aliceGuessColor = Color.BLACK;
    private int bobGuessColor   = Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_activity_key_exchange);

        Random rnd = new Random();
        publicColor  = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        aliceSecret  = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        bobSecret    = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        findViewById(R.id.viewPublicColor).setBackgroundColor(publicColor);
        findViewById(R.id.viewAliceMixed).setBackgroundColor(blendColors(publicColor, aliceSecret));
        findViewById(R.id.viewBobMixed).setBackgroundColor(blendColors(publicColor, bobSecret));

        View previewAlice = findViewById(R.id.viewAliceGuessPreview);
        View previewBob   = findViewById(R.id.viewBobGuessPreview);

        SeekBar.OnSeekBarChangeListener aliceListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int r = ((SeekBar) findViewById(R.id.seekAliceR)).getProgress();
                int g = ((SeekBar) findViewById(R.id.seekAliceG)).getProgress();
                int b = ((SeekBar) findViewById(R.id.seekAliceB)).getProgress();
                aliceGuessColor = Color.rgb(r, g, b);
                previewAlice.setBackgroundColor(aliceGuessColor);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        ((SeekBar) findViewById(R.id.seekAliceR)).setOnSeekBarChangeListener(aliceListener);
        ((SeekBar) findViewById(R.id.seekAliceG)).setOnSeekBarChangeListener(aliceListener);
        ((SeekBar) findViewById(R.id.seekAliceB)).setOnSeekBarChangeListener(aliceListener);

        SeekBar.OnSeekBarChangeListener bobListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int r = ((SeekBar) findViewById(R.id.seekBobR)).getProgress();
                int g = ((SeekBar) findViewById(R.id.seekBobG)).getProgress();
                int b = ((SeekBar) findViewById(R.id.seekBobB)).getProgress();
                bobGuessColor = Color.rgb(r, g, b);
                previewBob.setBackgroundColor(bobGuessColor);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        ((SeekBar) findViewById(R.id.seekBobR)).setOnSeekBarChangeListener(bobListener);
        ((SeekBar) findViewById(R.id.seekBobG)).setOnSeekBarChangeListener(bobListener);
        ((SeekBar) findViewById(R.id.seekBobB)).setOnSeekBarChangeListener(bobListener);

        Button   btnReveal = findViewById(R.id.btnReveal);
        TextView tvResults = findViewById(R.id.tvResults);

        btnReveal.setOnClickListener(v -> {
            int aliceAcc = calculateAccuracy(aliceSecret, aliceGuessColor);
            int bobAcc   = calculateAccuracy(bobSecret,   bobGuessColor);
            tvResults.setVisibility(View.VISIBLE);
            tvResults.setText(String.format(
                    "Alice Accuracy: %d%%\nBob Accuracy: %d%%\n\nIt is nearly impossible to \"unmix\" the exact colors by eye, just as it is computationally infeasible for computers to reverse discrete logarithms in real key exchange.",
                    aliceAcc, bobAcc));
        });
    }

    private int blendColors(int c1, int c2) {
        return Color.rgb(
                (Color.red(c1)   + Color.red(c2))   / 2,
                (Color.green(c1) + Color.green(c2)) / 2,
                (Color.blue(c1)  + Color.blue(c2))  / 2);
    }

    private int calculateAccuracy(int target, int guess) {
        double rD = Color.red(target)   - Color.red(guess);
        double gD = Color.green(target) - Color.green(guess);
        double bD = Color.blue(target)  - Color.blue(guess);
        return (int) Math.max(0, 100 - ((Math.sqrt(rD*rD + gD*gD + bD*bD) / 441.67) * 100));
    }
}
