package com.agora.netless.syncplayer

import android.view.View
import java.util.concurrent.CopyOnWriteArraySet

abstract class AtomPlayer {
    private var listeners: CopyOnWriteArraySet<AtomPlayerListener> = CopyOnWriteArraySet()

    var name: String? = null
        get() {
            return field ?: "${this.javaClass}"
        }

    fun addPlayerListener(listener: AtomPlayerListener) {
        listeners.add(listener)
    }

    fun removePlayerListener(listener: AtomPlayerListener) {
        listeners.remove(listener)
    }

    fun notifyChanged(block: (listener: AtomPlayerListener) -> Unit) {
        listeners.forEach(block)
    }

    var playerPhase = AtomPlayerPhase.Idle

    open val isPlaying: Boolean = false

    open var playbackSpeed = 1.0f

    abstract fun play()

    abstract fun pause()

    open fun stop() {
        pause()
        seekTo(0)
    }

    abstract fun release()

    abstract fun seekTo(timeMs: Long)

    abstract fun getPhase(): AtomPlayerPhase

    abstract fun currentPosition(): Long

    abstract fun duration(): Long

    open fun setPlayerView(view: View) {

    }

    /**
     * mostly，it's used for debug
     */
    fun setPlayerName(name: String) {
        this.name = name
    }

    internal fun updatePlayerPhase(newPhase: AtomPlayerPhase) {
        if (newPhase != playerPhase) {
            playerPhase = newPhase
            notifyChanged {
                it.onPhaseChanged(this, playerPhase)
            }
        }
    }

    internal val debugInfo: String
        get() = "{" +
                "isPlaying: $isPlaying," +
                "playerPhase: $playerPhase" +
                "}";
}

interface AtomPlayerListener {
    fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {}

    fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {}

    fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {}
}

enum class AtomPlayerPhase {
    /**
     * 视频播放尚未开始或已经结束。
     */
    Idle,

    /**
     * 视频播放已暂停。
     */
    Pause,

    /**
     * 正在播放视频。
     */
    Playing,

    /**
     * 视频正在缓冲。
     */
    Buffering,

    /**
     * 播放结束。
     */
    End,
}