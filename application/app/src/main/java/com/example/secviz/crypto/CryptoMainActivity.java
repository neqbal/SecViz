package com.example.secviz.crypto;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.secviz.R;

public class CryptoMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_activity_main);

        Button btnGame1 = findViewById(R.id.btnGame1);
        Button btnGame2 = findViewById(R.id.btnGame2);
        Button btnGame3 = findViewById(R.id.btnGame3);

        // Open Game 1: Key Exchange
        btnGame1.setOnClickListener(v -> {
            startActivity(new Intent(this, CryptoKeyExchange.class));
        });

        // Open Game 2: Hash Collision
        btnGame2.setOnClickListener(v -> {
            startActivity(new Intent(this, CryptoHashCollision.class));
        });

        // Open Game 3: Avalanche Effect
        btnGame3.setOnClickListener(v -> {
            startActivity(new Intent(this, CryptoAvalanche.class));
        });
    }
}
