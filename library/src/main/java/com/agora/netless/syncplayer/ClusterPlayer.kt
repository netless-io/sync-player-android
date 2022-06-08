package com.agora.netless.syncplayer

import android.os.Handler

class ClusterPlayer constructor(
    private val one: AtomPlayer,
    private val two: AtomPlayer,
) : AtomPlayer() {
    private var players: Array<AtomPlayer> = arrayOf(one, two)
    private var pauseReason: Array<Boolean> = arrayOf(false, false)

    private var seeking = 0
    private var position: Long = 0
    private var targetPosition: Long = 0

    init {
        val atomPlayerListener = LocalAtomPlayerListener(handler)
        players[0].addPlayerListener(atomPlayerListener)
        players[1].addPlayerListener(atomPlayerListener)
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

    override fun prepare() {
        if (!isPreparing) {
            targetPhase = AtomPlayerPhase.Ready
            players.forEach {
                it.prepare()
            }
        }
    }

    override fun play() {
        if (currentPhase == AtomPlayerPhase.Idle) {
            prepare()
        } else {
            players.forEach {
                it.play()
            }
        }
        targetPhase = AtomPlayerPhase.Playing
    }

    override fun pause() {
        players.forEach {
            it.pause()
        }
        targetPhase = AtomPlayerPhase.Paused
    }

    override fun release() {
        players.forEach {
            it.release()
        }
    }

    override fun seekTo(timeMs: Long) {
        seeking = 2
        players[0].seekTo(timeMs)
        players[1].seekTo(timeMs)
        targetPosition = timeMs
    }

    private fun isSeeking(): Boolean {
        return seeking != 0
    }

    override fun currentPosition(): Long {
        return position
    }

    override fun duration(): Long {
        return one.duration().coerceAtLeast(two.duration())
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

    inner class LocalAtomPlayerListener(handler: Handler) : AtomPlayerListener {
        override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
            if (!isSeeking()) {
                if (this@ClusterPlayer.position < position) {
                    this@ClusterPlayer.position = position
                    notifyChanged {
                        it.onPositionChanged(this@ClusterPlayer, this@ClusterPlayer.position)
                    }
                }
                if (this@ClusterPlayer.position > position + 1000) {
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
                        updatePlayerPhase(AtomPlayerPhase.Ready)
                        players.forEach {
                            if (targetPhase == AtomPlayerPhase.Playing) {
                                it.play()
                            }
                            if (targetPhase == AtomPlayerPhase.Paused) {
                                it.pause()
                            }
                        }
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
                        updatePlayerPhase(AtomPlayerPhase.End)
                    }
                }
            }
        }

        override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
            if (seeking > 0) seeking--
            if (seeking == 0) {
                Log.d("[$name] onSeekTo ${atomPlayer.name} $timeMs")
                position = targetPosition
                notifyChanged {
                    it.onSeekTo(this@ClusterPlayer, timeMs = timeMs)
                }
            }
        }
    }
}
