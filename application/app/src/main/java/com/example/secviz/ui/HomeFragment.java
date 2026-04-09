package com.example.secviz.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.secviz.R;
import com.example.secviz.crypto.CryptoMainActivity;

public class HomeFragment extends Fragment {

    /** Callback interface for the binary exploitation module. */
    public interface HomeListener {
        void onBinaryExploitationSelected();
    }

    private HomeListener listener;

    public static HomeFragment newInstance(HomeListener listener) {
        HomeFragment fragment = new HomeFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Binary Exploitation card → start the module
        view.findViewById(R.id.card_binary_exploitation).setOnClickListener(v -> {
            if (listener != null) listener.onBinaryExploitationSelected();
        });

        // Cryptography card → launch crypto module
        view.findViewById(R.id.card_cryptography).setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), CryptoMainActivity.class));
        });

        return view;
    }
}

