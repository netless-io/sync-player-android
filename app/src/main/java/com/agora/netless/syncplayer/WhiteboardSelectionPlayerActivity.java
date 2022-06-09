package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.netless.syncplayer.misc.BaseActivity;
import com.agora.netless.syncplayer.misc.Constant;
import com.agora.netless.syncplayer.misc.PlayerStateLayout;
import com.agora.netless.syncplayer.misc.SeekBarChangeAdapter;
import com.herewhite.sdk.AbstractPlayerEventListener;
import com.herewhite.sdk.Player;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.WhiteSdkConfiguration;
import com.herewhite.sdk.WhiteboardView;
import com.herewhite.sdk.domain.PlayerConfiguration;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.SDKError;

import java.util.Arrays;

public class WhiteboardSelectionPlayerActivity extends BaseActivity implements View.OnClickListener {
    private WhiteboardView whiteboardView;
    private PlayerStateLayout playerStateLayout;
    private SeekBar seekBar;
    private boolean isSeeking;

    private SelectionPlayer selectionPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whiteboard_player);
        initView();
        initPlayer();
    }

    private void initPlayer() {
        WhiteSdk whiteSdk = new WhiteSdk(whiteboardView, this, new WhiteSdkConfiguration(Constant.SDK_APP_ID, true));

        PlayerConfiguration playerConfiguration = new PlayerConfiguration(Constant.ROOM_UUID, Constant.ROOM_TOKEN);
        playerConfiguration.setDuration(120000L);

        whiteSdk.createPlayer(playerConfiguration, new AbstractPlayerEventListener() {
                },
                new Promise<Player>() {
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
        WhiteboardPlayer whiteboardPlayer = new WhiteboardPlayer(player);
        whiteboardPlayer.setName("whiteboardPlayer");

        selectionPlayer = new SelectionPlayer(whiteboardPlayer, new SelectionOptions(
                Arrays.asList(
                        new Selection(5_000, 10_000),
                        new Selection(15_000, 20_000),
                        new Selection(30_000, 40_000),
                        new Selection(60_000, 100_000)
                )
        ));
        playerStateLayout.attachPlayer(selectionPlayer);

        selectionPlayer.addPlayerListener(new AtomPlayerListener() {
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
        playerStateLayout = findViewById(R.id.player_state_layout);

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
                    selectionPlayer.seekTo(targetProgress);
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
        switch (v.getId()) {
            case R.id.button_play:
                selectionPlayer.play();
                break;
            case R.id.button_pause:
                selectionPlayer.pause();
                break;
            case R.id.button_reset:
                selectionPlayer.stop();
                break;
        }
    }
}
