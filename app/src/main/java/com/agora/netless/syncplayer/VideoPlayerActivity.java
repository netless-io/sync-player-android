package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;

public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private SeekBar seekBar;
    private FrameLayout playerContainer;
    private View playButton;
    private View pauseButton;
    private View resetButton;

    private VideoPlayer videoPlayer;
    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        initView();
        initData();
    }

    private void initData() {
        videoPlayer = new VideoPlayer(this, "https://white-pan.oss-cn-shanghai.aliyuncs.com/101/oceans.mp4");
        // videoPlayer = new SimpleVideoPlayer(this, "https://flat-storage.oss-cn-hangzhou.aliyuncs.com/temp/BigBuckBunny.mp4");
        videoPlayer.setPlayerName("videoPlayer");
        videoPlayer.setPlayerView(playerContainer);

        videoPlayer.addPlayerListener(new AtomPlayerListener() {
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
        playerContainer = findViewById(R.id.player_container);
        seekBar = findViewById(R.id.player_seek_bar);
        playButton = findViewById(R.id.button_play);
        pauseButton = findViewById(R.id.button_pause);
        resetButton = findViewById(R.id.button_reset);

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
                    videoPlayer.seekTo(targetProgress);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                videoPlayer.play();
                break;
            case R.id.button_pause:
                videoPlayer.pause();
                break;
            case R.id.button_reset:
                videoPlayer.stop();
                break;
        }
    }
}
