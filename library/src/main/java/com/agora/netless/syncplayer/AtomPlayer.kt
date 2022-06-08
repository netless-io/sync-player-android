package com.agora.netless.syncplayer

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

    var currentPhase = AtomPlayerPhase.Idle

    internal var targetPhase: AtomPlayerPhase = AtomPlayerPhase.Idle

    open val isPlaying: Boolean
        get() = currentPhase == AtomPlayerPhase.Playing

    internal val isPreparing: Boolean
        get() = currentPhase == AtomPlayerPhase.Idle && targetPhase == AtomPlayerPhase.Ready

    open val isError: Boolean = false

    open var playbackSpeed = 1.0f

    internal abstract fun prepare()

    abstract fun play()

    abstract fun pause()

    open fun stop() {
        seekTo(0)
        pause()
    }

    abstract fun release()

    abstract fun seekTo(timeMs: Long)

    abstract fun currentPosition(): Long

    abstract fun duration(): Long

    open fun setPlayerContainer(container: ViewGroup) {}

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
        Log.d("[$name] updatePlayerPhase to $newPhase, from $currentPhase")
        if (currentPhase != newPhase) {
            currentPhase = newPhase
            notifyChanged {
                it.onPhaseChanged(this, newPhase)
            }
        }
    }
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