package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agora.netless.syncplayer.misc.Constant;
import com.google.gson.Gson;
import com.herewhite.sdk.Player;
import com.herewhite.sdk.PlayerListener;
import com.herewhite.sdk.WhiteSdk;
import com.herewhite.sdk.WhiteSdkConfiguration;
import com.herewhite.sdk.WhiteboardView;
import com.herewhite.sdk.domain.PlayerConfiguration;
import com.herewhite.sdk.domain.PlayerPhase;
import com.herewhite.sdk.domain.PlayerState;
import com.herewhite.sdk.domain.PlayerTimeInfo;
import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.SDKError;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author fenglibin
 */
public class MultipleActivity extends AppCompatActivity implements PlayerListener {
    public static final String TAG = MultipleActivity.class.getSimpleName();
    private static final float PLAYBACK_SPEED = 1.0F;
    ArrayList<VideoItem> records = new ArrayList<VideoItem>() {
        {
            add(new VideoItem(905,
                    5665,
                    "https://white-pan.oss-cn-shanghai.aliyuncs.com/101/oceans.mp4"));
            add(new VideoItem(10454,
                    15333,
                    "https://flat-storage.oss-cn-hangzhou.aliyuncs.com/agoraCloudRecording/1c4e35d4b11649f6bf071dc3c71fe0b2/f0beb2541f45757e00d6de9c48ff2992_1c4e35d4-b116-49f6-bf07-1dc3c71fe0b2.m3u8"));
            add(new VideoItem(20333,
                    60665,
                    "https://white-pan.oss-cn-shanghai.aliyuncs.com/101/oceans.mp4"));
        }
    };
    private SeekBar seekBar;
    private View playerView;
    private View playerView2;
    private WhiteboardView whiteboardView;
    private final Gson gson = new Gson();
    private Player playbackPlayer;
    private AtomPlayer whiteboardPlayer;
    private AtomPlayer videoPlayer;
    private AtomPlayer videoPlayer2;
    private AtomPlayer clusterVideoPlayer;
    private AtomPlayer clusterPlayer;
    private final Handler mSeekBarUpdateHandler = new Handler(Looper.getMainLooper());
    private final boolean mUserIsSeeking = false;
    private final Runnable mUpdateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mUserIsSeeking) {
                return;
            }
            int progress = playerProgress();
            Log.v(TAG, "progress" + progress);
            seekBar.setProgress(progress);
            mSeekBarUpdateHandler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple);

        seekBar = findViewById(R.id.player_seek_bar);
        playerView = findViewById(R.id.player_view);
        playerView2 = findViewById(R.id.exo_video_view_2);

        whiteboardView = findViewById(R.id.white);
        WebView.setWebContentsDebuggingEnabled(true);

        setupPlayer();
        setupSeekBar();

        findViewById(R.id.button_play).setOnClickListener(view -> play());
        findViewById(R.id.button_pause).setOnClickListener(view -> pause());
        findViewById(R.id.button_reset).setOnClickListener(view -> {
            pause();
            seek(0f);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSeekBarUpdateHandler.removeCallbacks(mUpdateSeekBar);
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userSelectedPosition = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    userSelectedPosition = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.e("Aderan", "onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.e("Aderan", "onStartTrackingTouch");
            }
        });
        mSeekBarUpdateHandler.post(mUpdateSeekBar);
    }

    private void setupPlayer() {
        try {
            videoPlayer = new RtcVideoExoPlayer(this, records);
            // videoPlayer.setPlayerContainer(playerView);
            videoPlayer.setPlayerName("videoPlayer");

            videoPlayer2 = new VideoPlayer(this, "https://white-pan.oss-cn-shanghai.aliyuncs.com/101/oceans.mp4");
            // videoPlayer2.setPlayerContainer(playerView2);
            videoPlayer2.setPlayerName("videoPlayer2");

            clusterVideoPlayer = new ClusterPlayer(videoPlayer, videoPlayer2);
            clusterVideoPlayer.setPlayerName("clusterVideoPlayer");

            Log.d(TAG, "create success");
        } catch (Exception e) {
            Log.e(TAG, "create fail");
        }
        initPlayer(Constant.ROOM_UUID, Constant.ROOM_TOKEN);
    }

    private void initPlayer(String roomUUID, String roomToken) {
        WhiteSdk whiteSdk = new WhiteSdk(whiteboardView, this, new WhiteSdkConfiguration(Constant.SDK_APP_ID, true));

        PlayerConfiguration playerConf = new PlayerConfiguration(roomUUID, roomToken);
        playerConf.setDuration(120000L);

        whiteSdk.createPlayer(playerConf, this, new Promise<Player>() {
            @Override
            public void then(Player player) {
                enableBtn();

                playbackPlayer = player;

                whiteboardPlayer = new WhiteboardPlayer(player);
                whiteboardPlayer.setPlayerName("whiteboardPlayer");

                clusterPlayer = new ClusterPlayer(whiteboardPlayer, clusterVideoPlayer);
                clusterPlayer.setPlayerName("clusterPlayer");
            }

            @Override
            public void catchEx(SDKError t) {
                Log.e(TAG, "create player error, " + t.getJsStack());
            }
        });
    }

    private void enableBtn() {
        findViewById(R.id.button_play).setEnabled(true);
        findViewById(R.id.button_pause).setEnabled(true);
        findViewById(R.id.button_reset).setEnabled(true);
    }

    private int playerProgress() {
        if (playbackPlayer == null || playbackPlayer.getPlayerPhase() == PlayerPhase.waitingFirstFrame) {
            return 0;
        }
        PlayerTimeInfo timeInfo = playbackPlayer.getPlayerTimeInfo();
        return (int) (timeInfo.getScheduleTime() * 1f / timeInfo.getTimeDuration() * 100f);
    }

    private Boolean isPlayable() {
        return clusterPlayer != null && playbackPlayer != null && videoPlayer != null;
    }

    private void play() {
        if (isPlayable()) {
            clusterPlayer.play();
            clusterPlayer.setPlaybackSpeed(PLAYBACK_SPEED);
        }
    }

    private void pause() {
        if (isPlayable()) {
            clusterPlayer.pause();
        }
    }

    private void seek(Long time, TimeUnit timeUnit) {
        if (isPlayable()) {
            clusterPlayer.seekTo(TimeUnit.MILLISECONDS.convert(time, timeUnit));
        }
    }

    private void seek(Float progress) {
        if (isPlayable()) {
            long time = (long) (progress * clusterPlayer.duration());
            seek(time, TimeUnit.MILLISECONDS);
            seekBar.setProgress(playerProgress());
        }
    }


    @Override
    public void onPhaseChanged(PlayerPhase phase) {
//        ((WhiteboardPlayer) whiteboardPlayer).updateWhitePlayerPhase(phase);
    }

    @Override
    public void onLoadFirstFrame() {
        Log.i(TAG, "onLoadFirstFrame");
        showToast("onLoadFirstFrame");
    }

    @Override
    public void onSliceChanged(String slice) {

    }

    @Override
    public void onPlayerStateChanged(PlayerState modifyState) {
        Log.i(TAG, "onPlayerStateChanged: " + gson.toJson(modifyState));
    }

    @Override
    public void onStoppedWithError(SDKError error) {
        Log.d(TAG, "onStoppedWithError: " + error.getJsStack());
    }

    @Override
    public void onScheduleTimeChanged(long time) {
        if (isPlayable()) {
        }
    }

    @Override
    public void onCatchErrorWhenAppendFrame(SDKError error) {
        showToast(error.getJsStack());
    }

    @Override
    public void onCatchErrorWhenRender(SDKError error) {
        showToast(error.getJsStack());
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}