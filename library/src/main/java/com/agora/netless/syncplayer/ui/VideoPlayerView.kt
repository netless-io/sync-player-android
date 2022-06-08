package com.agora.netless.syncplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.agora.syncplayer.library.R
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView

class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    var playerView: PlayerView

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.video_player_layout, this)
        playerView = root.findViewById(R.id.player_view)
    }

    fun setPlayer(player: Player) {
        playerView.requestFocus()
        playerView.player = player
    }

    fun getPlayer(): Player? {
        return playerView.player
    }
}