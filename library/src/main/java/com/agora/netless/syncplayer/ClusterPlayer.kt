package com.agora.netless.syncplayer

class ClusterPlayer constructor(
    private val aPlayer: AtomPlayer,
    private val bPlayer: AtomPlayer,
) : AbstractAtomPlayer() {
    private var players: Array<AtomPlayer> = arrayOf(aPlayer, bPlayer)
    private var pauseReason: Array<Boolean> = arrayOf(false, false)

    private var seeking = 0
    private var position: Long = 0
    private var targetPosition: Long = 0

    init {
        val atomPlayerListener = LocalAtomPlayerListener()
        players.forEach {
            it.addPlayerListener(atomPlayerListener)
        }
    }

    private fun other(player: AtomPlayer) = if (players[0] == player) players[1] else players[0]

    private fun index(player: AtomPlayer) = if (players[0] == player) 0 else 1

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            players.forEach {
                it.playbackSpeed = value
            }
        }

    override fun prepareInternal() {
        players.forEach {
            it.prepare()
        }
    }

    override fun playInternal() {
        players.forEach {
            it.play()
        }
    }

    override fun pauseInternal() {
        players.forEach {
            it.pause()
        }
    }

    override fun release() {
        players.forEach {
            it.release()
        }
    }

    /**
     * 协同播放器需要将 seek 信息传递给内部播放器
     * 内部播放器 seekTo 需要保证回调 onSeekTo，如果到达尾部，变更为End
     */
    override fun seekToInternal(timeMs: Long) {
        seeking = 2
        players.forEach {
            it.seekTo(timeMs)
        }
        targetPosition = timeMs
    }

    private fun isSeeking(): Boolean {
        return seeking != 0
    }

    override fun currentPosition(): Long {
        return position
    }

    override fun duration(): Long {
        return aPlayer.duration().coerceAtLeast(bPlayer.duration())
    }

    private fun pauseWhenBuffering(atomPlayer: AtomPlayer) {
        pauseReason[index(atomPlayer)] = true
    }

    private fun isPauseWhenBuffering(atomPlayer: AtomPlayer): Boolean {
        return pauseReason[index(atomPlayer)]
    }

    private fun clearPauseWhenBuffering(atomPlayer: AtomPlayer) {
        pauseReason[index(atomPlayer)] = false
    }

    inner class LocalAtomPlayerListener : AtomPlayerListener {
        override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
            if (!isSeeking()) {
                if (this@ClusterPlayer.position < position) {
                    this@ClusterPlayer.position = position
                    notifyChanged {
                        it.onPositionChanged(this@ClusterPlayer, this@ClusterPlayer.position)
                    }
                }
                if (this@ClusterPlayer.position > position + 500) {
                    atomPlayer.seekTo(this@ClusterPlayer.position)
                }
            }
        }

        override fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
            Log.d("[$name] onPhaseChanged ${atomPlayer.name} $phaseChange")

            when (phaseChange) {
                AtomPlayerPhase.Idle -> {; }
                AtomPlayerPhase.Ready -> {
                    if (other(atomPlayer).currentPhase == AtomPlayerPhase.Ready) {
                        eventHandler.obtainMessage(INTERNAL_READY).sendToTarget()
                    }
                }
                AtomPlayerPhase.Paused -> {
                    if (isPauseWhenBuffering(atomPlayer)) {
                        return
                    }
                    if (other(atomPlayer).isPlaying) {
                        other(atomPlayer).pause()
                    }
                    updatePlayerPhase(AtomPlayerPhase.Paused)
                }
                AtomPlayerPhase.Playing -> {
                    if (!other(atomPlayer).isPlaying) {
                        clearPauseWhenBuffering(other(atomPlayer))
                        other(atomPlayer).play()
                    }
                    updatePlayerPhase(AtomPlayerPhase.Playing)
                }
                AtomPlayerPhase.Buffering -> {
                    if (other(atomPlayer).isPlaying) {
                        pauseWhenBuffering(other(atomPlayer))
                        other(atomPlayer).pause()
                    }
                    updatePlayerPhase(AtomPlayerPhase.Buffering)
                }
                AtomPlayerPhase.End -> {
                    if (other(atomPlayer).currentPhase == AtomPlayerPhase.End) {
                        eventHandler.obtainMessage(INTERNAL_END).sendToTarget()
                    }
                }
            }
        }

        override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
            Log.d("[$name] onSeekTo ${atomPlayer.name} $timeMs")

            if (seeking > 0) seeking--
            if (seeking == 0) {
                position = targetPosition
                notifyChanged {
                    it.onSeekTo(this@ClusterPlayer, timeMs = position)
                }
            }
        }
    }
}
