package com.agora.netless.syncplayer

import com.herewhite.sdk.AbstractPlayerEventListener
import com.herewhite.sdk.Player
import com.herewhite.sdk.domain.PlayerPhase
import com.herewhite.sdk.domain.PlayerState
import com.herewhite.sdk.domain.SDKError

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
                PlayerPhase.pause, PlayerPhase.playing -> {
                    if (targetPhase == AtomPlayerPhase.Playing ||
                        targetPhase == AtomPlayerPhase.Paused
                    ) {
                        updatePlayerPhase(targetPhase)
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
                updatePlayerPhase(AtomPlayerPhase.Ready)
                if (targetPhase == AtomPlayerPhase.Ready || targetPhase == AtomPlayerPhase.Paused) {
                    player.pause()
                }
                player.playbackSpeed = playbackSpeed.toDouble()
            }
        }

        override fun onPlayerStateChanged(modifyState: PlayerState) {

        }

        override fun onStoppedWithError(error: SDKError) {

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