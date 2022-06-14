package com.agora.netless.syncplayer

import android.content.Context
import android.view.ViewGroup
import com.herewhite.sdk.Player

class AtomPlayerBuilder {
    private var context: Context? = null
    private var videoUrl: String? = null
    private var player: Player? = null
    private var offset: Long? = null
    private var selectionOptions: SelectionOptions? = null
    private var container: ViewGroup? = null

    fun video(context: Context, videoUrl: String): AtomPlayerBuilder = apply {
        this.context = context
        this.videoUrl = videoUrl;
    }

    fun whiteboard(player: Player): AtomPlayerBuilder = apply {
        this.player = player
    }

    fun offset(offset: Long): AtomPlayerBuilder = apply {
        this.offset = offset
    }

    fun selection(selectionOptions: SelectionOptions): AtomPlayerBuilder = apply {
        this.selectionOptions = selectionOptions
    }

    fun setPlayerContainer(container: ViewGroup): AtomPlayerBuilder = apply {
        this.container = container
    }

    fun create(): AtomPlayer {
        if (player == null && videoUrl == null) {
            throw RuntimeException("videoUrl or player should be set!")
        }
        if (player != null && videoUrl != null) {
            throw RuntimeException("can't set both videoUrl and player!")
        }

        var atomPlayer: AtomPlayer = if (player != null) {
            WhiteboardPlayer(player!!)
        } else {
            VideoPlayer(context!!, videoUrl!!)
        }
        selectionOptions?.let { atomPlayer = SelectionPlayer(atomPlayer, it) }
        offset?.let { atomPlayer = OffsetPlayer(atomPlayer, it) }
        container?.let { atomPlayer.setPlayerContainer(it) }
        return atomPlayer
    }
}