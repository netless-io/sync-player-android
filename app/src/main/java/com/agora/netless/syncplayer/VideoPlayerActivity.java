package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.netless.syncplayer.misc.BaseActivity;
import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;

public class VideoPlayerActivity extends BaseActivity implements View.OnClickListener {
    private FrameLayout playerContainer;
    private SeekBar seekBar;

    private VideoPlayer finalPlayer;
    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        initView();
        initData();
    }

    private void initData() {
        finalPlayer = new VideoPlayer(this, "https://flat-storage.oss-cn-hangzhou.aliyuncs.com/agoraCloudRecording/b8bff14ed3df45969c0643f263c94a65/a82a52bdf34120c29a0ac4a8e8e65044_b8bff14e-d3df-4596-9c06-43f263c94a65__uid_s_24721__uid_e_av.m3u8");
        finalPlayer.setPlayerContainer(playerContainer);

        finalPlayer.addPlayerListener(new AtomPlayerListener() {
            @Override
            public void onPositionChanged(@NonNull AtomPlayer atomPlayer, long position) {
                if (!isSeeking) {
                    seekBar.setProgress((int) position);
                    Log.e("Aderan", "duration " + atomPlayer.duration() + "position " + position);
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
