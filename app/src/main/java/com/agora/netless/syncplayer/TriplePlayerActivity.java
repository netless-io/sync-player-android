package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.agora.netless.syncplayer.misc.Constant;
import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;
import com.herewhite.sdk.AbstractPlayerEventListener;
import com.herewhite.sdk.Player;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.WhiteSdkConfiguration;
import com.herewhite.sdk.WhiteboardView;
import com.herewhite.sdk.domain.PlayerConfiguration;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.SDKError;

public class TriplePlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private WhiteboardView whiteboardView;
    private FrameLayout playerContainer1;
    private FrameLayout playerContainer2;
    private ClusterPlayer clusterPlayer;

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
        playerConfiguration.setDuration(600_000L);

        whiteSdk.createPlayer(playerConfiguration, new AbstractPlayerEventListener() {
                },
                new Promise<Player>() {
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
        videoPlayer1.setPlayerName("videoPlayer1");
        videoPlayer1.setPlayerContainer(playerContainer1);

        VideoPlayer videoPlayer2 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer2.setPlayerName("videoPlayer2");
        videoPlayer2.setPlayerContainer(playerContainer2);

        ClusterPlayer combinePlayer = new ClusterPlayer(videoPlayer1, videoPlayer2);
        combinePlayer.setPlayerName("combinePlayer");

        WhiteboardPlayer whiteboardPlayer = new WhiteboardPlayer(player);
        whiteboardPlayer.setPlayerName("whiteboardPlayer");

        clusterPlayer = new ClusterPlayer(whiteboardPlayer, combinePlayer);
        clusterPlayer.addPlayerListener(new AtomPlayerListener() {
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

    private void enableBtn() {
        findViewById(R.id.button_play).setEnabled(true);
        findViewById(R.id.button_pause).setEnabled(true);
        findViewById(R.id.button_reset).setEnabled(true);
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
