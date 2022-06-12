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

    init {
        val interPlayerListener = object : AbstractPlayerEventListener() {
            override fun onPhaseChanged(phase: PlayerPhase) {
                Log.d("[$name] interPlayer onPhaseChanged $phase")
                when (phase) {
                    PlayerPhase.buffering -> {
                        val msg = eventHandler.obtainMessage(INTERNAL_BUFFERING)
                        msg.sendToTarget()
                    }
                    PlayerPhase.pause -> {
                        val msg = eventHandler.obtainMessage(INTERNAL_PAUSED)
                        msg.sendToTarget()
                    }
                    PlayerPhase.playing -> {
                        val msg = eventHandler.obtainMessage(INTERNAL_PLAYING)
                        msg.sendToTarget()
                    }
                    PlayerPhase.stopped, PlayerPhase.ended -> {
                        val msg = eventHandler.obtainMessage(INTERNAL_PLAYER_END)
                        msg.sendToTarget()
                    }
                    else -> {
                    }
                }
            }

            override fun onLoadFirstFrame() {
                Log.d("[$name] interPlayer onLoadFirstFrame")
                // player.playbackSpeed = playbackSpeed.toDouble()
                val msg = eventHandler.obtainMessage(INTERNAL_PLAYER_READY)
                msg.sendToTarget()
            }

            override fun onScheduleTimeChanged(time: Long) {
                // Log.d("[$name] interPlayer onScheduleTimeChanged $time")
                notifyChanged {
                    it.onPositionChanged(this@WhiteboardPlayer, time)
                }
            }
        }
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

    override fun seekToInternal(timeMs: Long) {
        player.seekToScheduleTime(timeMs)
        notifyChanged {
            it.onSeekTo(this, timeMs = timeMs)
        }
    }

    override fun release() {
        player.stop()
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