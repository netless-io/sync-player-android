package com.agora.netless.syncplayer

import com.herewhite.sdk.Player
import com.herewhite.sdk.domain.PlayerPhase

class WhiteboardPlayer constructor(private val player: Player) : AtomPlayer() {
    override val isPlaying: Boolean = playerPhase == AtomPlayerPhase.Playing

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            player.playbackSpeed = value.toDouble()
        }

    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
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

    override fun getPhase(): AtomPlayerPhase {
        return playerPhase
    }

    override fun currentPosition(): Long {
        return player.playerTimeInfo.scheduleTime
    }

    override fun duration(): Long {
        return player.playerTimeInfo.timeDuration
    }

    fun updateWhitePlayerPhase(phase: PlayerPhase) {
        when (phase) {
            PlayerPhase.buffering, PlayerPhase.waitingFirstFrame -> {
                updatePlayerPhase(AtomPlayerPhase.Buffering)
            }
            PlayerPhase.pause, PlayerPhase.playing -> {
                player.playbackSpeed = playbackSpeed.toDouble()
                updatePlayerPhase(if (phase == PlayerPhase.pause) AtomPlayerPhase.Paused else AtomPlayerPhase.Playing)
            }
            else -> {
                updatePlayerPhase(AtomPlayerPhase.End)
            }
        }
    }
}