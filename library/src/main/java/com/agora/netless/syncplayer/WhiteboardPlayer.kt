package com.agora.netless.syncplayer

import com.herewhite.sdk.Player
import com.herewhite.sdk.PlayerListener
import com.herewhite.sdk.domain.PlayerPhase
import com.herewhite.sdk.domain.PlayerState
import com.herewhite.sdk.domain.SDKError

class WhiteboardPlayer constructor(private val player: Player) : AtomPlayer() {

    init {
        player.addPlayerListener(object : PlayerListener {
            override fun onPhaseChanged(phase: PlayerPhase) {
                Log.d("[$name] interPlayer onPhaseChanged $phase")

                handler.post { updateWhitePlayerPhase(phase) }
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

            override fun onSliceChanged(slice: String?) {

            }

            override fun onPlayerStateChanged(modifyState: PlayerState) {

            }

            override fun onStoppedWithError(error: SDKError) {

            }

            override fun onScheduleTimeChanged(time: Long) {
                // Log.d("[$name] onScheduleTimeChanged $time")

                notifyChanged {
                    it.onPositionChanged(this@WhiteboardPlayer, time)
                }
            }

            override fun onCatchErrorWhenAppendFrame(error: SDKError) {

            }

            override fun onCatchErrorWhenRender(error: SDKError) {

            }
        })
    }

    override val isPlaying: Boolean
        get() = playerPhase == AtomPlayerPhase.Playing

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            player.playbackSpeed = value.toDouble()
        }

    override fun setup() {
        if (!isPreparing) {
            player.play()
            targetPhase = AtomPlayerPhase.Ready
        }
    }

    override fun play() {
        if (playerPhase == AtomPlayerPhase.Idle) {
            setup()
        } else {
            player.play()
        }
        targetPhase = AtomPlayerPhase.Playing
    }

    override fun pause() {
        player.pause()
        targetPhase = AtomPlayerPhase.Paused
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

    private fun updateWhitePlayerPhase(phase: PlayerPhase) {
        when (phase) {
            PlayerPhase.buffering -> {
                if (playerPhase != AtomPlayerPhase.Idle) {
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
        }
    }
}