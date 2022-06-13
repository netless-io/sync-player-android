package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.netless.syncplayer.misc.BaseActivity;
import com.agora.netless.syncplayer.misc.Constant;
import com.agora.netless.syncplayer.misc.PlayerStateLayout;
import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;

public class ClusterPlayerActivity extends BaseActivity implements View.OnClickListener {
    private FrameLayout playerContainer1;
    private FrameLayout playerContainer2;
    private PlayerStateLayout playerStateLayout1;
    private PlayerStateLayout playerStateLayout2;
    private ClusterPlayer finalPlayer;

    private SeekBar seekBar;
    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_player);
        initView();
        initPlayer();
    }

    private void initPlayer() {
        VideoPlayer videoPlayer1 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[0]);
        videoPlayer1.setName("videoPlayer1");
        videoPlayer1.setPlayerContainer(playerContainer1);
        playerStateLayout1.attachPlayer(videoPlayer1);

        VideoPlayer videoPlayer2 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer2.setName("videoPlayer2");
        videoPlayer2.setPlayerContainer(playerContainer2);
        playerStateLayout2.attachPlayer(videoPlayer2);

        finalPlayer = new ClusterPlayer(videoPlayer1, videoPlayer2);
        finalPlayer.addPlayerListener(new AtomPlayerListener() {
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
        });
    }

    private void initView() {
        playerContainer1 = findViewById(R.id.player_container_1);
        playerContainer2 = findViewById(R.id.player_container_2);

        playerStateLayout1 = findViewById(R.id.player_state_layout_1);
        playerStateLayout2 = findViewById(R.id.player_state_layout_2);

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
                    finalPlayer.seekTo(targetProgress);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_play) {
            finalPlayer.play();
        } else if (id == R.id.button_pause) {
            finalPlayer.pause();
        } else if (id == R.id.button_reset) {
            finalPlayer.stop();
            finalPlayer.seekTo(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finalPlayer.release();
    }
}
