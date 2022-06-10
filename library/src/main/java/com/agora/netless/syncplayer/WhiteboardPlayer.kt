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
                    if (currentPhase != AtomPlayerPhase.Idle) {
                        updatePlayerPhase(AtomPlayerPhase.Buffering)
                    }
                }
                PlayerPhase.pause -> {
                    if (targetPhase == AtomPlayerPhase.Paused) {
                        updatePlayerPhase(AtomPlayerPhase.Paused)
                    }
                }
                PlayerPhase.playing -> {
                    if (targetPhase == AtomPlayerPhase.Playing) {
                        updatePlayerPhase(AtomPlayerPhase.Playing)
                    }
                }
                PlayerPhase.stopped, PlayerPhase.ended -> {
                    updatePlayerPhase(AtomPlayerPhase.End)
                }
                else -> {}
            }
        }

        override fun onLoadFirstFrame() {
            Log.d("[$name] interPlayer onLoadFirstFrame")

            handler.post {
                player.playbackSpeed = playbackSpeed.toDouble()
                updatePlayerPhase(AtomPlayerPhase.Ready)
                if (targetPhase == AtomPlayerPhase.Ready || targetPhase == AtomPlayerPhase.Paused) {
                    player.pause()
                }
            }
        }

        override fun onScheduleTimeChanged(time: Long) {
            // Log.d("[$name] interPlayer onScheduleTimeChanged $time")

            notifyChanged {
                it.onPositionChanged(this@WhiteboardPlayer, time)
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
        player.pause()
    }

    override fun release() {
        player.stop()
    }

    override fun seekTo(timeMs: Long) {
        player.seekToScheduleTime(timeMs)
        notifyChanged {
            it.onSeekTo(this, timeMs = timeMs)
        }
    }

    override fun currentPosition(): Long {
        return player.playerTimeInfo.scheduleTime
    }

    override fun duration(): Long {
        return player.playerTimeInfo.timeDuration
    }
}