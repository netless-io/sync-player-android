package com.agora.netless.syncplayer

import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.concurrent.CopyOnWriteArraySet

abstract class AtomPlayer {
    private var listeners = CopyOnWriteArraySet<AtomPlayerListener>()
    internal val handler = Handler(Looper.getMainLooper())

    fun addPlayerListener(listener: AtomPlayerListener) {
        listeners.add(listener)
    }

    fun removePlayerListener(listener: AtomPlayerListener) {
        listeners.remove(listener)
    }

    internal fun notifyChanged(block: (listener: AtomPlayerListener) -> Unit) {
        handler.post {
            listeners.forEach(block)
        }
    }

    var playerPhase = AtomPlayerPhase.Idle

    internal var targetPhase: AtomPlayerPhase = AtomPlayerPhase.Idle

    open val isPlaying: Boolean = false

    internal val isPreparing: Boolean
        get() = playerPhase == AtomPlayerPhase.Idle && targetPhase == AtomPlayerPhase.Ready

    open val isError: Boolean = false

    open var playbackSpeed = 1.0f

    internal abstract fun setup()

    abstract fun play()

    abstract fun pause()

    open fun stop() {
        pause()
        seekTo(0)
    }

    abstract fun release()

    abstract fun seekTo(timeMs: Long)

    abstract fun currentPosition(): Long

    abstract fun duration(): Long

    open fun setPlayerView(view: View) {}

    var name: String? = null
        get() {
            return field ?: "${this.javaClass}"
        }

    /**
     * mostly，it's used for debug
     */
    fun setPlayerName(name: String) {
        this.name = name
    }

    internal fun updatePlayerPhase(newPhase: AtomPlayerPhase) {
        Log.d("[$name] updatePlayerPhase to $newPhase, from $playerPhase")
        if (playerPhase != newPhase) {
            playerPhase = newPhase
            notifyChanged {
                it.onPhaseChanged(this, newPhase)
            }
        }
    }

    internal val debugInfo: String
        get() = "{" +
                "isPlaying: $isPlaying," +
                "playerPhase: $playerPhase" +
                "}"
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
     * 视频准备就绪
     */
    Ready,

    /**
     * 视频播放已暂停。
     */
    Paused,

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