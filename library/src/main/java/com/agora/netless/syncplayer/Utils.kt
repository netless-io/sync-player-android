package com.agora.netless.syncplayer

import android.os.Handler
import android.text.TextUtils
import android.util.Log
import com.herewhite.sdk.PlayerListener
import com.herewhite.sdk.domain.PlayerPhase
import com.herewhite.sdk.domain.PlayerState
import com.herewhite.sdk.domain.SDKError

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

    private val positionNotifier = PositionNotifier(eventHandler, this)

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

    override var playbackSpeed = 1.0f

    override fun prepareInternal() {
        updatePlayerPhase(AtomPlayerPhase.Ready)
    }

    override fun playInternal() {
        lastPlay = System.currentTimeMillis()
        positionNotifier.start()
    }

    override fun pauseInternal() {
        startPosition += duringTime()
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
            startPosition + duringTime()
        } else {
            startPosition
        }
    }

    override fun duration(): Long {
        return duration
    }

    private fun duringTime() = ((System.currentTimeMillis() - lastPlay) * playbackSpeed).toLong()
}

internal open class WhitePlayerListenerAdapter : PlayerListener {
    override fun onPhaseChanged(phase: PlayerPhase) {
    }

    override fun onLoadFirstFrame() {
    }

    override fun onSliceChanged(slice: String) {
    }

    override fun onPlayerStateChanged(modifyState: PlayerState) {
    }

    override fun onStoppedWithError(error: SDKError) {
    }

    override fun onScheduleTimeChanged(time: Long) {
    }

    override fun onCatchErrorWhenAppendFrame(error: SDKError) {
    }

    override fun onCatchErrorWhenRender(error: SDKError) {
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