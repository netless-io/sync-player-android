package com.agora.netless.syncplayer

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.agora.netless.syncplayer.ui.VideoPlayerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

open class VideoPlayer constructor(
    private val context: Context,
    private val videoUrl: String,
) : AbstractAtomPlayer() {
    private var exoPlayer = SimpleExoPlayer.Builder(context.applicationContext).build()

    private var dataSourceFactory = DefaultDataSourceFactory(
        context,
        Util.getUserAgent(context, "SyncPlayer")
    )

    private val positionNotifier = PositionNotifier(eventHandler, this)

    private val interPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            Log.d("[$name] interPlayer onPlaybackStateChanged $state")

            when (state) {
                Player.STATE_IDLE -> {
                    updatePlayerPhase(AtomPlayerPhase.Idle)
                }
                Player.STATE_BUFFERING -> {
                    eventHandler.obtainMessage(INTERNAL_BUFFERING).sendToTarget()
                }
                Player.STATE_READY -> {
                    if (currentPhase == AtomPlayerPhase.Idle) {
                        eventHandler.obtainMessage(INTERNAL_READY).sendToTarget()
                    } else {
                        if (exoPlayer.playWhenReady) {
                            eventHandler.obtainMessage(INTERNAL_PLAYING).sendToTarget()
                        } else {
                            eventHandler.obtainMessage(INTERNAL_PAUSED).sendToTarget()
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    eventHandler.obtainMessage(INTERNAL_END).sendToTarget()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            eventHandler.obtainMessage(INTERNAL_ERROR, error).sendToTarget()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d("[$name] exoPlayer onIsPlayingChanged $isPlaying")
            if (isPlaying) {
                positionNotifier.start()
            } else {
                positionNotifier.stop()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                Log.d("[$name] interPlayer onSeekEnd: ${exoPlayer.currentPosition}")
                val pos = exoPlayer.currentPosition
                notifyChanged {
                    it.onSeekTo(this@VideoPlayer, pos)
                    it.onPositionChanged(this@VideoPlayer, pos)
                }
            }
        }
    }

    init {
        exoPlayer.addListener(interPlayerListener)
        // disable handleAudioFocus to support multiple players
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, false)
        exoPlayer.playWhenReady = false
    }

    /**
     * 设置播放视图
     *
     * @param container 视图实例
     */
    override fun setPlayerContainer(container: ViewGroup) {
        if (container !is FrameLayout) {
            throw IllegalArgumentException("videoPlayer container must be type of FrameLayout!")
        }
        container.addView(bindPlayer(exoPlayer))
    }

    open fun bindPlayer(player: Player): View {
        val playerView = VideoPlayerView(context).apply {
            setPlayer(exoPlayer)
        }
        return playerView
    }

    private fun createMediaSource(uri: Uri): MediaSource {
        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_HLS -> HlsMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            C.TYPE_OTHER -> ProgressiveMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    override fun seekToInternal(timeMs: Long) {
        exoPlayer.seekTo(timeMs)
    }

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.playbackParameters = PlaybackParameters(value)
        }

    override fun prepareInternal() {
        val mediaSource = createMediaSource(Uri.parse(videoUrl))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    override fun playInternal() {
        exoPlayer.playWhenReady = true
    }

    override fun pauseInternal() {
        exoPlayer.playWhenReady = false
    }

    override fun release() {
        exoPlayer.release()
        positionNotifier.stop()
    }

    override fun currentPosition(): Long {
        if (isInPlaybackState()) {
            return exoPlayer.currentPosition
        }
        return 0
    }

    override fun duration(): Long {
        if (isInPlaybackState()) {
            return exoPlayer.duration
        }
        return -1
    }
}