package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.netless.syncplayer.misc.BaseActivity;
import com.agora.netless.syncplayer.misc.Constant;
import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;

public class ClusterPlayerActivity extends BaseActivity implements View.OnClickListener {
    private FrameLayout playerContainer1;
    private FrameLayout playerContainer2;
    private ClusterPlayer clusterPlayer;

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

        VideoPlayer videoPlayer2 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer2.setName("videoPlayer2");
        videoPlayer2.setPlayerContainer(playerContainer2);

        clusterPlayer = new ClusterPlayer(videoPlayer1, videoPlayer2);
        clusterPlayer.addPlayerListener(new AtomPlayerListener() {
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
                    clusterPlayer.seekTo(targetProgress);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                clusterPlayer.play();
                break;
            case R.id.button_pause:
                clusterPlayer.pause();
                break;
            case R.id.button_reset:
                clusterPlayer.stop();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clusterPlayer.release();
    }
}
