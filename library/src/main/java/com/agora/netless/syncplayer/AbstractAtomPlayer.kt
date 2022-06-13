package com.agora.netless.syncplayer

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.ViewGroup
import java.util.concurrent.CopyOnWriteArraySet

abstract class AbstractAtomPlayer : AtomPlayer {
    override var name: String = this.javaClass.simpleName

    internal val eventHandler = EventHandler(Looper.getMainLooper())

    override var currentPhase = AtomPlayerPhase.Idle

    internal var targetPhase: AtomPlayerPhase = AtomPlayerPhase.Idle

    internal var playerError: Exception? = null

    override val isPlaying: Boolean
        get() = currentPhase == AtomPlayerPhase.Playing

    private val isPreparing: Boolean
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

    /**
     * play 方法为方便用户调用，设计成非原子操作，依据不同状态执行相应请求。
     *
     * Idle 组合类型下 Case（如 init，preparing，error）由 [prepare] 处理
     *
     */
    override fun play() {
        when (currentPhase) {
            AtomPlayerPhase.Idle -> {
                prepare()
            }
            AtomPlayerPhase.Ready, AtomPlayerPhase.Buffering, AtomPlayerPhase.Paused -> {
                playInternal()
                updatePlayerPhase(AtomPlayerPhase.Playing)
            }
            AtomPlayerPhase.Playing -> {
                playInternal()
            }
            AtomPlayerPhase.End -> {
                /**
                 * 依据外部播放器 seek 变更
                 * 具体由协作播放器处理，协作播放器需要判断当前End状态，[seekTo] 至指定位置，后调用 player 方法
                 * [seekTo] 方法传入的参数为 position 需要被记录
                 */
                if (currentPosition() < duration()) {
                    playInternal()
                    updatePlayerPhase(AtomPlayerPhase.Playing)
                }
            }
        }
        targetPhase = AtomPlayerPhase.Playing
    }

    override fun pause() {
        if (isInPlaybackState()) {
            pauseInternal()
            updatePlayerPhase(AtomPlayerPhase.Paused)
        } else {
            Log.w("$name pause when $currentPhase")
        }
        targetPhase = AtomPlayerPhase.Paused
    }

    override fun seekTo(timeMs: Long) {
        if (isInPlaybackState() && timeMs <= duration()) {
            seekToInternal(timeMs)
        } else {
            if (currentPhase != AtomPlayerPhase.End) {
                seekToInternal(duration())
                pauseInternal()
                updatePlayerPhase(AtomPlayerPhase.End)
            }
            notifyChanged {
                it.onSeekTo(this, timeMs)
            }
        }
    }

    override fun stop() {
        seekTo(duration())
        pause()
    }

    open fun prepareInternal() {}

    open fun playInternal() {}

    open fun pauseInternal() {}

    open fun seekToInternal(timeMs: Long) {}

    abstract override fun release()

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
        eventHandler.post {
            listeners.forEach(invoke)
        }
    }

    /**
     * 更新并发送异步通知
     */
    internal fun updatePlayerPhase(newPhase: AtomPlayerPhase) {
        Log.d("[$name] updatePlayerPhase to $newPhase, from $currentPhase")

        if (currentPhase != newPhase) {
            currentPhase = newPhase
            notifyChanged {
                it.onPhaseChanged(this, newPhase)
            }
        }
    }

    companion object {
        internal const val INTERNAL_READY = 1
        internal const val INTERNAL_PLAYING = 2
        internal const val INTERNAL_BUFFERING = 3
        internal const val INTERNAL_PAUSED = 4
        internal const val INTERNAL_END = 5
        internal const val INTERNAL_ERROR = 6
        internal const val INTERNAL_SEEK_COMPLETE = 7
    }

    inner class EventHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            Log.d("[$name] event ${message(msg)} when $currentPhase, targetPhase $targetPhase")

            when (msg.what) {
                INTERNAL_READY -> {
                    updatePlayerPhase(AtomPlayerPhase.Ready)
                    when (targetPhase) {
                        AtomPlayerPhase.Playing -> {
                            playInternal()
                            updatePlayerPhase(AtomPlayerPhase.Playing)
                        }
                        AtomPlayerPhase.Paused -> {
                            pauseInternal()
                            updatePlayerPhase(AtomPlayerPhase.Paused)
                        }
                        else -> {; }
                    }
                }
                INTERNAL_PLAYING -> {
                    if (currentPhase == AtomPlayerPhase.Buffering) {
                        updatePlayerPhase(AtomPlayerPhase.Playing)
                    } else if (currentPhase == AtomPlayerPhase.Paused) {
                        // nothing
                    } else {
                        if (targetPhase == AtomPlayerPhase.Paused) {
                            pauseInternal()
                        } else {
                            Log.w("[$name] onPlaying when $currentPhase")
                        }
                    }
                }
                INTERNAL_PAUSED -> {
                    when (currentPhase) {
                        AtomPlayerPhase.Buffering -> {
                            updatePlayerPhase(AtomPlayerPhase.Paused)
                        }
                        AtomPlayerPhase.Paused -> {
                            // nothing
                        }
                        else -> {
                            Log.w("[$name] onPaused when $currentPhase")
                        }
                    }
                }
                INTERNAL_BUFFERING -> {
                    if (currentPhase == AtomPlayerPhase.Playing) {
                        updatePlayerPhase(AtomPlayerPhase.Buffering)
                    } else if (currentPhase == AtomPlayerPhase.Paused) {
                        pauseInternal()
                    }
                }
                INTERNAL_END -> {
                    updatePlayerPhase(AtomPlayerPhase.End)
                }
                INTERNAL_ERROR -> {
                    playerError = msg.obj as Exception
                    updatePlayerPhase(AtomPlayerPhase.Idle)
                }
            }
            super.handleMessage(msg)
        }

        private fun message(msg: Message): String {
            val msgType = when (msg.what) {
                INTERNAL_READY -> "InternalReady"
                INTERNAL_PLAYING -> "InternalPlaying"
                INTERNAL_BUFFERING -> "InternalBuffering"
                INTERNAL_PAUSED -> "InternalPaused"
                INTERNAL_END -> "InternalEnd"
                INTERNAL_ERROR -> "InternalError"
                INTERNAL_SEEK_COMPLETE -> "InternalSeekEnd"
                else -> msg.what.toString()
            }
            return msgType
        }
    }
}

