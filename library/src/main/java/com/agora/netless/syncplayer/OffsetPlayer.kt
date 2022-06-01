package com.agora.netless.syncplayer

class OffsetPlayer constructor(
    private val player: AtomPlayer,
    private val offset: Long,
) : AtomPlayer() {
    override fun setup() {
        player.setup()
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun release() {
        player.release()
    }

    override fun seekTo(timeMs: Long) {
        if (timeMs < offset) {

        }
    }

    override fun currentPosition(): Long {
        return player.currentPosition() + offset
    }

    override fun duration(): Long {
        return player.duration() + offset
    }
}