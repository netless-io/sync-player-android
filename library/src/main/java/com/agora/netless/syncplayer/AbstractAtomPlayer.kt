package com.agora.netless.syncplayer

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import java.util.concurrent.CopyOnWriteArraySet

abstract class AbstractAtomPlayer : AtomPlayer {
    override var name: String = this.javaClass.simpleName

    internal var ignorePlayWhenEnd: Boolean = true

    internal val handler = Handler(Looper.getMainLooper())

    override var currentPhase = AtomPlayerPhase.Idle

    internal var targetPhase: AtomPlayerPhase = AtomPlayerPhase.Idle

    internal var playerError: Exception? = null

    override val isPlaying: Boolean
        get() = currentPhase == AtomPlayerPhase.Playing

    internal val isPreparing: Boolean
        get() = currentPhase == AtomPlayerPhase.Idle && targetPhase == AtomPlayerPhase.Ready

    override val isError: Boolean
        get() = playerError != null

    internal fun isInPlaybackState(): Boolean = currentPhase != AtomPlayerPhase.Idle

    override var playbackSpeed = 1.0f

    override fun prepare() {
        if (!isPreparing) {
            targetPhase = AtomPlayerPhase.Ready
            playerError = null

            prepareInternal()
        }
    }

    override fun play() {
        if (currentPhase == AtomPlayerPhase.End && ignorePlayWhenEnd) {
            return
        }
        if (currentPhase == AtomPlayerPhase.Idle) {
            prepare()
        } else {
            playInternal()
        }
        targetPhase = AtomPlayerPhase.Playing
    }

    override fun pause() {
        if (isPlaying) {
            pauseInternal()
        }
        targetPhase = AtomPlayerPhase.Paused
    }

    open fun prepareInternal() {}

    open fun playInternal() {}

    open fun pauseInternal() {}

    override fun stop() {
        seekTo(0)
        pause()
    }

    abstract override fun release()

    abstract override fun seekTo(timeMs: Long)

    abstract override fun currentPosition(): Long

    abstract override fun duration(): Long

    override fun setPlayerContainer(container: ViewGroup) {}

    private val listeners = CopyOnWriteArraySet<AtomPlayerListener>()

    override fun addPlayerListener(listener: AtomPlayerListener) {
        listeners.add(listener)
    }

    override fun removePlayerListener(listener: AtomPlayerListener) {
        listeners.remove(listener)
    }

    internal fun notifyChanged(invoke: (listener: AtomPlayerListener) -> Unit) {
        handler.post {
            listeners.forEach(invoke)
        }
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