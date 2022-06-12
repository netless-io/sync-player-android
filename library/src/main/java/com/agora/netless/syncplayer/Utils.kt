package com.agora.netless.syncplayer

import android.os.Handler
import android.text.TextUtils
import android.util.Log

/**
 * used for notify position.
 */
internal class PositionNotifier constructor(
    val handler: Handler,
    val atomPlayer: AbstractAtomPlayer,
    val intervalTime: Long = 500,
) {
    private val ticker = object : Runnable {
        override fun run() {
            if (atomPlayer.isPlaying) {
                atomPlayer.notifyChanged {
                    it.onPositionChanged(
                        atomPlayer,
                        atomPlayer.currentPosition()
                    )
                }
                handler.postDelayed(this, intervalTime)
            }
        }
    }

    fun start() {
        handler.post(ticker)
    }

    fun stop() {
        handler.removeCallbacks(ticker)
    }
}

/**
 * 模拟空白播放，实现AtomPlayer
 */
internal class FakePlayer(private val duration: Long) : AbstractAtomPlayer() {
    private var startPosition = 0L
    private var lastPlay = 0L

    private val positionNotifier = PositionNotifier(handler, this)

    init {
        addPlayerListener(object : AtomPlayerListener {
            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                if (position >= duration) {
                    pauseInternal()
                    updatePlayerPhase(AtomPlayerPhase.End)
                }
            }
        })
    }

    override fun prepareInternal() {
        updatePlayerPhase(AtomPlayerPhase.Ready)
    }

    override fun playInternal() {
        lastPlay = System.currentTimeMillis()
        positionNotifier.start()
    }

    override fun pauseInternal() {
        startPosition += System.currentTimeMillis() - lastPlay
        positionNotifier.stop()
    }

    override fun release() {
        positionNotifier.stop()
    }

    override fun seekToInternal(timeMs: Long) {
        startPosition = timeMs
        lastPlay = System.currentTimeMillis()
        notifyChanged {
            it.onSeekTo(this, timeMs)
        }
    }

    override fun currentPosition(): Long {
        return if (isPlaying) {
            startPosition + System.currentTimeMillis() - lastPlay
        } else {
            startPosition
        }
    }

    override fun duration(): Long {
        return duration
    }
}

/**
 * Wrapper around android.util.Log.
 */
internal class Log {
    companion object {
        private const val TAG = "SyncPlayer"

        const val LOG_LEVEL_ALL = 0
        const val LOG_LEVEL_INFO = 1
        const val LOG_LEVEL_WARNING = 2
        const val LOG_LEVEL_ERROR = 3
        const val LOG_LEVEL_OFF = Int.MAX_VALUE

        @JvmStatic
        var logLevel = LOG_LEVEL_ALL

        @JvmStatic
        var logStackTraces = true

        @JvmStatic
        fun d(message: String) {
            if (logLevel == LOG_LEVEL_ALL) {
                Log.d(TAG, message)
            }
        }

        @JvmStatic
        fun i(message: String) {
            if (logLevel <= LOG_LEVEL_INFO) {
                Log.i(TAG, message)
            }
        }

        @JvmStatic
        fun e(message: String, throwable: Throwable?) {
            e(appendThrowableString(message, throwable))
        }

        @JvmStatic
        fun e(message: String) {
            if (logLevel <= LOG_LEVEL_ERROR) {
                Log.e(TAG, message)
            }
        }

        @JvmStatic
        fun w(message: String, throwable: Throwable?) {
            w(appendThrowableString(message, throwable))
        }

        @JvmStatic
        fun w(message: String) {
            if (logLevel <= LOG_LEVEL_WARNING) {
                Log.w(TAG, message)
            }
        }

        @JvmStatic
        private fun getThrowableString(throwable: Throwable?): String? {
            return if (throwable == null) {
                null
            } else if (!logStackTraces) {
                throwable.message
            } else {
                Log.getStackTraceString(throwable).trim { it <= ' ' }.replace("\t", "    ")
            }
        }

        @JvmStatic
        private fun appendThrowableString(message: String, throwable: Throwable?): String {
            var msg: String = message
            val throwableString = getThrowableString(throwable)
            if (!TextUtils.isEmpty(throwableString)) {
                msg += """
                ｜${throwableString!!.replace("\n", "\n  ")}
                """
            }
            return msg
        }
    }
}