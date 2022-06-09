package com.agora.netless.syncplayer

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import java.util.concurrent.CopyOnWriteArraySet

abstract class AbstractAtomPlayer : AtomPlayer {
    override var name: String = this.javaClass.simpleName

    internal val handler = Handler(Looper.getMainLooper())

    override var currentPhase = AtomPlayerPhase.Idle

    internal var targetPhase: AtomPlayerPhase = AtomPlayerPhase.Idle

    override val isPlaying: Boolean
        get() = currentPhase == AtomPlayerPhase.Playing

    internal val isPreparing: Boolean
        get() = currentPhase == AtomPlayerPhase.Idle && targetPhase == AtomPlayerPhase.Ready

    override val isError: Boolean = false

    override var playbackSpeed = 1.0f

    abstract override fun prepare()

    abstract override fun play()

    abstract override fun pause()

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
        Log.d("[$name] try updatePlayerPhase to $newPhase, from $currentPhase")
        if (currentPhase != newPhase) {
            currentPhase = newPhase
            notifyChanged {
                it.onPhaseChanged(this, newPhase)
            }
        }
    }
}