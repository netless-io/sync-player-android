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

public class OffsetPlayerActivity extends BaseActivity implements View.OnClickListener {
    private FrameLayout playerContainer;
    private SeekBar seekBar;

    private OffsetPlayer offsetPlayer;
    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offset_player);
        initView();
        initData();
    }

    private void initData() {
        VideoPlayer videoPlayer = new VideoPlayer(this, Constant.ALL_VIDEO_URL[0]);
        videoPlayer.setPlayerName("videoPlayer");
        videoPlayer.setPlayerContainer(playerContainer);

        offsetPlayer = new OffsetPlayer(videoPlayer, 5000L);
        offsetPlayer.addPlayerListener(new AtomPlayerListener() {
            @Override
            public void onPositionChanged(@NonNull AtomPlayer atomPlayer, long position) {
                if (!isSeeking) {
                    seekBar.setProgress((int) position);
                }
            }

            @Override
            public void onPhaseChanged(@NonNull AtomPlayer atomPlayer, @NonNull AtomPlayerPhase phase) {
                if (phase == AtomPlayerPhase.Ready) {
                    seekBar.setMax((int) atomPlayer.duration() * 2);
                }
            }

            @Override
            public void onSeekTo(@NonNull AtomPlayer atomPlayer, long timeMs) {

            }
        });
    }

    private void initView() {
        playerContainer = findViewById(R.id.player_container);

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
                    offsetPlayer.seekTo(targetProgress);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                offsetPlayer.play();
                break;
            case R.id.button_pause:
                offsetPlayer.pause();
                break;
            case R.id.button_reset:
                offsetPlayer.stop();
                break;
        }
    }
}
