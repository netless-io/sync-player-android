package com.agora.netless.syncplayer;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.agora.netless.syncplayer.misc.BaseActivity;
import com.agora.netless.syncplayer.misc.Constant;
import com.agora.netless.syncplayer.misc.PlayerControllerLayout;
import com.agora.netless.syncplayer.misc.PlayerStateLayout;

public class ClusterPlayerActivity extends BaseActivity {
    private FrameLayout playerContainer1;
    private FrameLayout playerContainer2;
    private PlayerStateLayout playerStateLayout1;
    private PlayerStateLayout playerStateLayout2;
    private PlayerControllerLayout playerControllerLayout;

    private ClusterPlayer finalPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_player);
        initView();
        initPlayer();
    }

    private void initPlayer() {
        VideoPlayer videoPlayer1 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[0]);
        videoPlayer1.setName("videoPlayer1");
        videoPlayer1.setPlayerContainer(playerContainer1);
        playerStateLayout1.attachPlayer(videoPlayer1);

        VideoPlayer videoPlayer2 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer2.setName("videoPlayer2");
        videoPlayer2.setPlayerContainer(playerContainer2);
        playerStateLayout2.attachPlayer(videoPlayer2);

        finalPlayer = new ClusterPlayer(videoPlayer1, videoPlayer2);
        playerControllerLayout.attachPlayer(finalPlayer);
    }

    private void initView() {
        playerContainer1 = findViewById(R.id.player_container_1);
        playerContainer2 = findViewById(R.id.player_container_2);

        playerStateLayout1 = findViewById(R.id.player_state_layout_1);
        playerStateLayout2 = findViewById(R.id.player_state_layout_2);

        playerControllerLayout = findViewById(R.id.player_controller_layout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finalPlayer.release();
    }
}
