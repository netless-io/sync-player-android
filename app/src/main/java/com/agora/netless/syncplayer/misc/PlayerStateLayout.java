package com.agora.netless.syncplayer.misc;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.agora.netless.syncplayer.AtomPlayer;
import com.agora.netless.syncplayer.AtomPlayerListener;
import com.agora.netless.syncplayer.AtomPlayerPhase;
import com.agora.netless.syncplayer.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerStateLayout extends FrameLayout {
    private TextView positionLabel;
    private ProgressBar loadingBar;
    private AtomPlayer player;

    public PlayerStateLayout(Context context) {
        this(context, null, 0);
    }

    public PlayerStateLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerStateLayout(@NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View root = LayoutInflater.from(getContext()).inflate(R.layout.layout_player_state, this);
        positionLabel = root.findViewById(R.id.position_label);
        loadingBar = root.findViewById(R.id.loading_bar);
    }

    public void attachPlayer(@NotNull AtomPlayer player) {
        this.player = player;
        player.addPlayerListener(playerListener);
    }

    public void detachPlayer() {
        if (player != null) {
            player.removePlayerListener(playerListener);
        }
    }

    public void showBuffering(boolean loading) {
        this.loadingBar.setVisibility(loading ? VISIBLE : GONE);
    }

    public void showPosition(long position) {
        long second = position / (long) 1000;
        this.positionLabel.setText((CharSequence) (second / (long) 60 + ":" + second % (long) 60));
    }

    AtomPlayerListener playerListener = new AtomPlayerListener() {
        @Override
        public void onSeekTo(@NonNull AtomPlayer atomPlayer, long timeMs) {

        }

        public void onPhaseChanged(@NotNull AtomPlayer atomPlayer, @NotNull AtomPlayerPhase phaseChange) {
            showBuffering(phaseChange == AtomPlayerPhase.Buffering);
        }

        public void onPositionChanged(@NotNull AtomPlayer atomPlayer, long position) {
            showPosition(position);
        }
    };
}
