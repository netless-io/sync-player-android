package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.netless.syncplayer.misc.BaseActivity;
import com.agora.netless.syncplayer.misc.Constant;
import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;

import java.util.Arrays;
import java.util.List;

public class MultiVideoPlayerActivity extends BaseActivity implements View.OnClickListener {
    private FrameLayout playerContainer;
    private SeekBar seekBar;

    private AtomPlayer finalPlayer;
    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_video_player);
        initView();
        initData();
    }

    private void initData() {
        List<VideoItem> items = Arrays.asList(
                new VideoItem(5000, 60000, Constant.ALL_VIDEO_URL[0])
                // new VideoItem(15000, 25000, Constant.ALL_VIDEO_URL[1]),
                // new VideoItem(30000, 40000, Constant.ALL_VIDEO_URL[0])
        );
        finalPlayer = new MultiVideoPlayer(this, items);
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
                if (phase == AtomPlayerPhase.End) {
                    finalPlayer.pause();
                    finalPlayer.seekTo(0);
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
