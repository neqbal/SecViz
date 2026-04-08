package com.example.secviz;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.secviz.ui.HomeFragment;
import com.example.secviz.ui.LevelFragment;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Level> levels;
    private int currentLevelIdx = 0;
    private DrawerLayout drawerLayout;
    private LevelsAdapter drawerAdapter;
    private ActionBarDrawerToggle toggle;
    private boolean isOnHome = true;

    private MenuItem timerMenuItem;
    private android.os.Handler timerHandler;
    private Runnable timerRunnable;
    private long timerStartMs = 0;
    private boolean timerRunning = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.main_drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        levels = LevelsRepository.getLevels();

        RecyclerView rvLevels = findViewById(R.id.rv_levels);
        rvLevels.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new LevelsAdapter();
        rvLevels.setAdapter(drawerAdapter);

        showHome();
    }

    private void showHome() {
        isOnHome = true;
        stopTimer();
        invalidateOptionsMenu();
        // Lock the drawer and hide the hamburger icon on the home screen
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        toggle.setDrawerIndicatorEnabled(false);
        toggle.syncState();

        HomeFragment homeFragment = HomeFragment.newInstance(this::startBinaryExploitation);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit();
        invalidateOptionsMenu();
    }

    private void startBinaryExploitation() {
        loadLevel(0);
    }

    private void loadLevel(int idx) {
        if (idx >= levels.size()) idx = levels.size() - 1;
        currentLevelIdx = idx;
        isOnHome = false;

        resetTimer(); // resets and restarts for each level

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
        invalidateOptionsMenu();

        Level level = levels.get(currentLevelIdx);
        if (drawerAdapter != null) drawerAdapter.notifyDataSetChanged();

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
        } else if (!isOnHome) {
            // Navigate back to home instead of exiting the app
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_level, menu);

        timerMenuItem = menu.findItem(R.id.action_timer);
        TextView timerView = (TextView) timerMenuItem.getActionView();
        timerView.setTextColor(0xFFC9D1D9);
        timerView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        timerView.setTypeface(android.graphics.Typeface.MONOSPACE);
        timerView.setPadding(0, 0, 16, 0);
        timerView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        timerView.setText("00:00");

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Show the home icon only when inside a level, not on the home screen
        MenuItem homeItem = menu.findItem(R.id.action_home);
        MenuItem timerItem = menu.findItem(R.id.action_timer);
        if (homeItem != null) {
            homeItem.setVisible(!isOnHome);
        }
        if (timerItem != null) timerItem.setVisible(!isOnHome);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_home) {
            showHome();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void startTimer() {
        if (timerHandler == null) timerHandler = new android.os.Handler(getMainLooper());
        timerStartMs = System.currentTimeMillis();
        timerRunning = true;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!timerRunning || timerMenuItem == null) return;
                long elapsed = System.currentTimeMillis() - timerStartMs;
                long minutes = (elapsed / 60000) % 60;
                long seconds = (elapsed / 1000)  % 60;
                TextView tv = (TextView) timerMenuItem.getActionView();
                if (tv != null) tv.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerRunning = false;
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void resetTimer() {
        stopTimer();
        if (timerMenuItem != null) {
            TextView tv = (TextView) timerMenuItem.getActionView();
            if (tv != null) tv.setText("00:00");
        }
        startTimer();
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

