package com.example.secviz;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.secviz.data.Level;
import com.example.secviz.data.LevelsRepository;
import com.example.secviz.ui.LevelFragment;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Level> levels;
    private int currentLevelIdx = 0;
    private DrawerLayout drawerLayout;
    private LevelsAdapter drawerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.main_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        levels = LevelsRepository.getLevels();
        
        RecyclerView rvLevels = findViewById(R.id.rv_levels);
        rvLevels.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new LevelsAdapter();
        rvLevels.setAdapter(drawerAdapter);

        loadLevel(0);
    }

    private void loadLevel(int idx) {
        if (idx >= levels.size()) idx = levels.size() - 1;
        currentLevelIdx = idx;
        Level level = levels.get(currentLevelIdx);

        if (drawerAdapter != null) {
            drawerAdapter.notifyDataSetChanged();
        }

        final int nextIdx = currentLevelIdx + 1;
        LevelFragment fragment = LevelFragment.newInstance(level, () -> {
            if (nextIdx < levels.size()) loadLevel(nextIdx);
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private class LevelsAdapter extends RecyclerView.Adapter<LevelsAdapter.LevelViewHolder> {

        @NonNull
        @Override
        public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_level_drawer, parent, false);
            return new LevelViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
            Level level = levels.get(position);
            holder.tvNumber.setText(String.valueOf(position + 1));
            holder.tvTitle.setText(level.title);

            if (position == currentLevelIdx) {
                holder.itemView.setBackgroundColor(Color.parseColor("#1A58A6FF"));
                holder.tvTitle.setTextColor(Color.parseColor("#58A6FF"));
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_menu_item_ripple);
                holder.tvTitle.setTextColor(Color.parseColor("#C9D1D9")); // text_primary
            }

            // Hide connecting line for the last item
            if (position == levels.size() - 1) {
                holder.viewLine.setVisibility(View.INVISIBLE);
            } else {
                holder.viewLine.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (currentLevelIdx != position) {
                    loadLevel(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return levels == null ? 0 : levels.size();
        }

        class LevelViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber;
            TextView tvTitle;
            View viewLine;

            public LevelViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNumber = itemView.findViewById(R.id.tv_level_number);
                tvTitle = itemView.findViewById(R.id.tv_level_title);
                viewLine = itemView.findViewById(R.id.view_line);
            }
        }
    }
}

