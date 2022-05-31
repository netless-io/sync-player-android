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
    private var positionLabel: TextView
    private var loadingBar: ProgressBar

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.video_player_layout, this)
        playerView = root.findViewById(R.id.player_view)
        positionLabel = root.findViewById(R.id.position_label)
        loadingBar = root.findViewById(R.id.loading_bar)
    }

    fun showBuffering(loading: Boolean) {
        loadingBar.visibility = if (loading) VISIBLE else GONE
    }

    fun setPosition(position: Long) {
        val second = position / 1000
        positionLabel.text = (second / 60).toString() + ":" + second % 60
    }

    fun setPlayer(player: Player) {
        playerView.requestFocus()
        playerView.player = player
    }

    fun getPlayer(): Player? {
        return playerView.player
    }
}