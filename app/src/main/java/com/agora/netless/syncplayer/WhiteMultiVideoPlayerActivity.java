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
import com.herewhite.sdk.Player;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.WhiteSdkConfiguration;
import com.herewhite.sdk.WhiteboardView;
import com.herewhite.sdk.domain.PlayerConfiguration;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.SDKError;

import java.util.Arrays;
import java.util.List;

public class WhiteMultiVideoPlayerActivity extends BaseActivity implements View.OnClickListener {
    private WhiteboardView whiteboardView;
    private FrameLayout playerContainer;
    private PlayerStateLayout playerStateLayout1;
    private PlayerStateLayout playerStateLayout2;
    private SeekBar seekBar;
    private boolean isSeeking;

    private AtomPlayer finalPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_multi_video_player);
        initView();
        initPlayer();
    }

    private void initPlayer() {
        WhiteSdk whiteSdk = new WhiteSdk(whiteboardView, this, new WhiteSdkConfiguration(Constant.SDK_APP_ID, true));

        PlayerConfiguration playerConfiguration = new PlayerConfiguration(Constant.ROOM_UUID, Constant.ROOM_TOKEN);
        playerConfiguration.setRegion(Constant.REGION);
        playerConfiguration.setDuration(50_000L);

        whiteSdk.createPlayer(playerConfiguration, new Promise<Player>() {
            @Override
            public void then(Player player) {
                enableBtn();
                initPlayer(player);
            }

            @Override
            public void catchEx(SDKError t) {

            }
        });
    }

    private void initPlayer(Player player) {
        List<VideoItem> items = Arrays.asList(
                new VideoItem(5000, 10000, Constant.ALL_VIDEO_URL[0]),
                new VideoItem(15000, 25000, Constant.ALL_VIDEO_URL[1]),
                new VideoItem(30000, 40000, Constant.ALL_VIDEO_URL[0])
        );
        AtomPlayer videoPlayer = new MultiVideoPlayer(this, items);
        videoPlayer.setPlayerContainer(playerContainer);
        playerStateLayout1.attachPlayer(videoPlayer);

        AtomPlayer whiteboardPlayer = new WhiteboardPlayer(player);
        playerStateLayout2.attachPlayer(whiteboardPlayer);

        finalPlayer = SyncPlayer.combine(whiteboardPlayer, videoPlayer);
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
        whiteboardView = findViewById(R.id.whiteboard_view);
        playerContainer = findViewById(R.id.player_container);
        playerStateLayout1 = findViewById(R.id.player_state_layout_1);
        playerStateLayout2 = findViewById(R.id.player_state_layout_2);

        findViewById(R.id.button_play).setOnClickListener(this);
        findViewById(R.id.button_pause).setOnClickListener(this);
        findViewById(R.id.button_reset).setOnClickListener(this);
        disableBtn();

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

    private void enableBtn() {
        findViewById(R.id.button_play).setEnabled(true);
        findViewById(R.id.button_pause).setEnabled(true);
        findViewById(R.id.button_reset).setEnabled(true);
    }

    private void disableBtn() {
        findViewById(R.id.button_play).setEnabled(false);
        findViewById(R.id.button_pause).setEnabled(false);
        findViewById(R.id.button_reset).setEnabled(false);
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
        if (finalPlayer != null) {
            finalPlayer.release();
        }
    }
}
