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

public class TriplePlayerActivity extends BaseActivity implements View.OnClickListener {
    private WhiteboardView whiteboardView;
    private FrameLayout playerContainer1;
    private FrameLayout playerContainer2;

    private PlayerStateLayout playerStateLayout1;
    private PlayerStateLayout playerStateLayout2;
    private PlayerStateLayout playerStateLayout3;

    private AtomPlayer finalPlayer;

    private SeekBar seekBar;
    private boolean isSeeking;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triple_player);
        initView();
        initData();
    }

    private void initData() {
        WhiteSdk whiteSdk = new WhiteSdk(whiteboardView, this, new WhiteSdkConfiguration(Constant.SDK_APP_ID, true));

        PlayerConfiguration playerConfiguration = new PlayerConfiguration(Constant.ROOM_UUID, Constant.ROOM_TOKEN);
        playerConfiguration.setRegion(Constant.REGION);
        playerConfiguration.setDuration(120_000L);

        whiteSdk.createPlayer(playerConfiguration, new Promise<Player>() {
            @Override
            public void then(Player player) {
                initPlayer(player);
                enableBtn();
            }

            @Override
            public void catchEx(SDKError t) {

            }
        });
    }

    private void initPlayer(Player player) {
        VideoPlayer videoPlayer1 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[0]);
        videoPlayer1.setName("videoPlayer1");
        videoPlayer1.setPlayerContainer(playerContainer1);
        playerStateLayout1.attachPlayer(videoPlayer1);

        VideoPlayer videoPlayer2 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer2.setName("videoPlayer2");

        AtomPlayer selectionPlayer = SyncPlayer.selection(videoPlayer2,
                new SelectionOptions(Arrays.asList(
                        new Selection(5_000, 20_000),
                        new Selection(20_000, 120_000))
                ));
        selectionPlayer.setPlayerContainer(playerContainer2);
        playerStateLayout2.attachPlayer(selectionPlayer);

        ClusterPlayer combinePlayer = new ClusterPlayer(videoPlayer1, selectionPlayer);
        combinePlayer.setName("combinePlayer1");

        WhiteboardPlayer whiteboardPlayer = new WhiteboardPlayer(player);
        whiteboardPlayer.setName("whiteboardPlayer");
        playerStateLayout3.attachPlayer(whiteboardPlayer);

        finalPlayer = SyncPlayer.combine(whiteboardPlayer, combinePlayer);
        finalPlayer.addPlayerListener(new AtomPlayerListener() {
            @Override
            public void onPositionChanged(@NonNull AtomPlayer atomPlayer, long position) {
                if (!isSeeking) {
                    seekBar.setProgress((int) position);
                }
            }

            @Override
            public void onPhaseChanged(@NonNull AtomPlayer atomPlayer, @NonNull AtomPlayerPhase phase) {
                if (phase == AtomPlayerPhase.Ready) {
                    Log.d("SyncPlayerSimple", "duration:" + atomPlayer.duration());
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

        playerContainer1 = findViewById(R.id.player_container_1);
        playerContainer2 = findViewById(R.id.player_container_2);

        playerStateLayout1 = findViewById(R.id.player_state_layout_1);
        playerStateLayout2 = findViewById(R.id.player_state_layout_2);
        playerStateLayout3 = findViewById(R.id.player_state_layout_3);

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
        disableBtn();
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
        finalPlayer.release();
    }
}
