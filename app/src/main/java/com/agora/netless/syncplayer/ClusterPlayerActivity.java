package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;

public class ClusterPlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private View playButton;
    private View pauseButton;
    private View resetButton;
    private SeekBar seekBar;

    private FrameLayout playerContainer1;
    private FrameLayout playerContainer2;

    private VideoPlayer videoPlayer1;
    private VideoPlayer videoPlayer2;

    private ClusterPlayer clusterPlayer;

    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_player);
        initView();
        initData();
    }

    private void initData() {
        videoPlayer1 = new VideoPlayer(this, "https://white-pan.oss-cn-shanghai.aliyuncs.com/101/oceans.mp4");
        videoPlayer1.setPlayerName("videoPlayer1");
        videoPlayer1.setPlayerView(playerContainer1);

        videoPlayer2 = new VideoPlayer(this, "https://flat-storage.oss-cn-hangzhou.aliyuncs.com/temp/BigBuckBunny.mp4");
        videoPlayer2.setPlayerName("videoPlayer2");
        videoPlayer2.setPlayerView(playerContainer2);

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
        seekBar = findViewById(R.id.player_seek_bar);
        playButton = findViewById(R.id.button_play);
        pauseButton = findViewById(R.id.button_pause);
        resetButton = findViewById(R.id.button_reset);
        playerContainer1 = findViewById(R.id.player_container_1);
        playerContainer2 = findViewById(R.id.player_container_2);

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
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
}
