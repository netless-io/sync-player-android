package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.agora.netless.syncplayer.misc.Constant;
import com.agora.netless.syncplayer.misc.PlayerStateLayout;
import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;

import java.util.Arrays;

public class SelectionPlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private FrameLayout playerContainer;
    private PlayerStateLayout playerStateLayout;
    private SeekBar seekBar;

    private SelectionPlayer selectionPlayer;
    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_player);
        initView();
        initData();
    }

    private void initData() {
        VideoPlayer videoPlayer = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer.setPlayerName("videoPlayer");
        videoPlayer.setPlayerContainer(playerContainer);
        playerStateLayout.attachPlayer(videoPlayer);

        selectionPlayer = new SelectionPlayer(videoPlayer, new SelectionOptions(
                Arrays.asList(
                        new Selection(0, 5_000),
                        new Selection(10_000, 20_000),
                        new Selection(60_000, 100_000)
                )
        ));
        selectionPlayer.addPlayerListener(new AtomPlayerListener() {
            @Override
            public void onPositionChanged(@NonNull AtomPlayer atomPlayer, long position) {
                if (!isSeeking) {
                    seekBar.setProgress((int) position);
                }
            }

            @Override
            public void onPhaseChanged(@NonNull AtomPlayer atomPlayer, @NonNull AtomPlayerPhase phase) {
                if (phase == AtomPlayerPhase.Ready) {
                    seekBar.setMax((int) atomPlayer.duration());
                }
            }

            @Override
            public void onSeekTo(@NonNull AtomPlayer atomPlayer, long timeMs) {

            }
        });
    }

    private void initView() {
        playerContainer = findViewById(R.id.player_container);
        playerStateLayout = findViewById(R.id.player_state_layout);

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
                    selectionPlayer.seekTo(targetProgress);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                selectionPlayer.play();
                break;
            case R.id.button_pause:
                selectionPlayer.pause();
                break;
            case R.id.button_reset:
                selectionPlayer.stop();
                break;
        }
    }
}
