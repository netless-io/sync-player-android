package com.agora.netless.syncplayer

import com.herewhite.sdk.AbstractPlayerEventListener
import com.herewhite.sdk.Player
import com.herewhite.sdk.domain.PlayerPhase

class WhiteboardPlayer(
    private val player: Player
) : AbstractAtomPlayer() {

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            player.playbackSpeed = value.toDouble()
        }

    private val interPlayerListener = object : AbstractPlayerEventListener() {
        override fun onPhaseChanged(phase: PlayerPhase) {
            Log.d("[$name] interPlayer onPhaseChanged $phase")
            handler.post { handleInterPlayerPhase(phase) }
        }

        private fun handleInterPlayerPhase(interPhase: PlayerPhase) {
            when (interPhase) {
                PlayerPhase.buffering -> {
                    if (currentPhase == AtomPlayerPhase.Playing) {
                        updatePlayerPhase(AtomPlayerPhase.Buffering)
                    } else if (currentPhase == AtomPlayerPhase.Paused) {
                        pauseInternal()
                    }
                }
                PlayerPhase.pause -> {
                    when (currentPhase) {
                        AtomPlayerPhase.Buffering -> {
                            updatePlayerPhase(AtomPlayerPhase.Paused)
                        }
                        AtomPlayerPhase.Paused -> {
                            // nothing
                        }
                        else -> {
                            Log.w("[$name] onPaused when $currentPhase")
                        }
                    }
                }
                PlayerPhase.playing -> {
                    if (currentPhase == AtomPlayerPhase.Buffering) {
                        updatePlayerPhase(AtomPlayerPhase.Playing)
                    } else {
                        Log.w("[$name] onPlaying when $currentPhase")
                        if (targetPhase == AtomPlayerPhase.Paused) {
                            pauseInternal()
                        }
                    }
                }
                PlayerPhase.stopped, PlayerPhase.ended -> {
                    updatePlayerPhase(AtomPlayerPhase.End)
                }
                else -> {
                }
            }
        }

        override fun onLoadFirstFrame() {
            Log.d("[$name] interPlayer onLoadFirstFrame")
            handler.post {
                Log.d("[$name] onReady when $currentPhase")
                player.playbackSpeed = playbackSpeed.toDouble()
                updatePlayerPhase(AtomPlayerPhase.Ready)
                when (targetPhase) {
                    AtomPlayerPhase.Playing -> {
                        playInternal()
                        updatePlayerPhase(AtomPlayerPhase.Playing)
                    }
                    AtomPlayerPhase.Paused -> {
                        pauseInternal()
                        updatePlayerPhase(AtomPlayerPhase.Paused)
                    }
                    AtomPlayerPhase.Ready -> {
                        pauseInternal()
                    }
                }
            }
        }

        override fun onScheduleTimeChanged(time: Long) {
            // Log.d("[$name] interPlayer onScheduleTimeChanged $time")

            notifyChanged {
                if (isPlaying) {
                    it.onPositionChanged(this@WhiteboardPlayer, time)
                }
            }
        }
    }

    init {
        player.addPlayerListener(interPlayerListener)
    }

    override fun prepareInternal() {
        player.play()
    }

    override fun playInternal() {
        player.play()
    }

    override fun pauseInternal() {
        if (player.playerPhase != PlayerPhase.pause) {
            player.pause()
        }
    }

    override fun release() {
        player.stop()
    }

    override fun seekToInternal(timeMs: Long) {
        player.seekToScheduleTime(timeMs)
        notifyChanged {
            it.onSeekTo(this, timeMs = timeMs)
        }
    }

    override fun currentPosition(): Long {
        if (isInPlaybackState()) {
            return player.playerTimeInfo.scheduleTime
        }
        return 0
    }

    override fun duration(): Long {
        if (isInPlaybackState()) {
            return player.playerTimeInfo.timeDuration
        }
        return -1
    }
}