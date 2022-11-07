package com.agora.netless.syncplayer

import android.content.Context
import android.net.Uri
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

/**
 * 用于多段视频播放
 */
class MultiVideoPlayer constructor(
    private val context: Context,
    private val videos: List<VideoItem>,
) : AbstractAtomPlayer() {
    private var exoPlayer: SimpleExoPlayer =
        SimpleExoPlayer.Builder(context.applicationContext).build()
    private val videoPlayerView: VideoPlayerView by lazy {
        VideoPlayerView(context)
    }

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
                    it.onSeekTo(this@MultiVideoPlayer, pos)
                    it.onPositionChanged(this@MultiVideoPlayer, pos)
                }
            }
        }
    }

    private var mediaSource: MediaSource? = null
    private var currentPlaying: VideoItem? = null

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
        container.addView(videoPlayerView)
        videoPlayerView.setPlayer(exoPlayer)
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
        checkAndPlayTime(timeMs, true)
    }

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.playbackParameters = PlaybackParameters(value)
        }

    override fun prepareInternal() {
        mediaSource = createMediaSource(Uri.parse(videos[0].videoURL))
        exoPlayer.setMediaSource(mediaSource!!)
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
            return (currentPlaying?.endTime ?: 0) + exoPlayer.currentPosition
        }
        return 0
    }

    override fun duration(): Long {
        if (isInPlaybackState()) {
            return videos.last().endTime
        }
        return -1
    }

    private fun checkAndPlayTime(timeMs: Long, seek: Boolean = false) {
        val recordItem = getRecordItem(timeMs)
        if (recordItem != null) {
            if (recordItem == currentPlaying) {
                if (seek) {
                    exoPlayer.seekTo(timeMs - recordItem.beginTime)
                }
            } else {
                currentPlaying = recordItem
                setVideoPath(recordItem.videoURL)
                exoPlayer.playWhenReady = true
            }
        } else {
            currentPlaying = null
            exoPlayer.playWhenReady = false
        }
    }

    private fun getRecordItem(timeMs: Long): VideoItem? {
        return videos.find { timeMs >= it.beginTime && timeMs < it.endTime }
    }

    /**
     * 设置播放链接
     *
     * @param path 播放链接
     */
    private fun setVideoPath(path: String) {
        mediaSource = createMediaSource(Uri.parse(path))
        exoPlayer.setMediaSource(mediaSource!!)
    }
}

data class VideoItem constructor(
    val beginTime: Long,
    val endTime: Long,
    val videoURL: String,
)