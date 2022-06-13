package com.agora.netless.syncplayer.misc;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.agora.netless.syncplayer.AtomPlayer;
import com.agora.netless.syncplayer.AtomPlayerListener;
import com.agora.netless.syncplayer.AtomPlayerPhase;
import com.agora.netless.syncplayer.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * I'm not sure if this improves the understanding of the samples
 * so only replace {@link com.agora.netless.syncplayer.ClusterPlayerActivity}
 */
public final class PlayerControllerLayout extends LinearLayoutCompat implements View.OnClickListener {
    private SeekBar seekBar;

    private AtomPlayer atomPlayer;
    private boolean isSeeking;

    private AtomPlayerListener playerListener = new AtomPlayerListener() {
        @Override
        public void onPositionChanged(@NonNull AtomPlayer atomPlayer, long position) {
            if (!isSeeking) {
                seekBar.setProgress((int) position);
            }
        }

        @Override
        public void onPhaseChanged(@NonNull AtomPlayer atomPlayer, @NonNull AtomPlayerPhase phase) {
            if (phase == AtomPlayerPhase.Playing) {
                seekBar.setMax((int) atomPlayer.duration());
            }
        }

        @Override
        public void onSeekTo(@NonNull AtomPlayer atomPlayer, long timeMs) {

        }
    };

    public PlayerControllerLayout(Context context) {
        this(context, null, 0);
    }

    public PlayerControllerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerControllerLayout(@NotNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_player_controller, this);

        findViewById(R.id.button_play).setOnClickListener(this);
        findViewById(R.id.button_pause).setOnClickListener(this);
        findViewById(R.id.button_reset).setOnClickListener(this);

        seekBar = findViewById(R.id.player_seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBarChangeAdapter() {
            private long targetProgress = -1;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    targetProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
                targetProgress = -1;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                if (targetProgress != -1) {
                    atomPlayer.seekTo(targetProgress);
                }
            }
        });

        setOrientation(LinearLayoutCompat.VERTICAL);
    }

    public void attachPlayer(@NotNull AtomPlayer player) {
        this.atomPlayer = player;
        player.addPlayerListener(playerListener);
    }

    public void detachPlayer() {
        if (atomPlayer != null) {
            atomPlayer.removePlayerListener(playerListener);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_play) {
            atomPlayer.play();
        } else if (id == R.id.button_pause) {
            atomPlayer.pause();
        } else if (id == R.id.button_reset) {
            atomPlayer.stop();
            atomPlayer.seekTo(0);
        }
    }
}
