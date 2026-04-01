package com.example.secviz;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.secviz.data.Level;
import com.example.secviz.data.LevelsRepository;
import com.example.secviz.ui.LevelFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Level> levels;
    private int currentLevelIdx = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        levels = LevelsRepository.getLevels();
        loadLevel(0);
    }

    private void loadLevel(int idx) {
        if (idx >= levels.size()) idx = levels.size() - 1;
        currentLevelIdx = idx;
        Level level = levels.get(currentLevelIdx);

        final int nextIdx = currentLevelIdx + 1;
        LevelFragment fragment = LevelFragment.newInstance(level, () -> {
            if (nextIdx < levels.size()) loadLevel(nextIdx);
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
