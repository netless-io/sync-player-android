package com.agora.netless.syncplayer

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import com.agora.netless.syncplayer.ui.VideoPlayerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class VideoPlayer constructor(
    context: Context,
    private val videoUrl: String,
) : AtomPlayer(), Player.Listener {
    private var exoPlayer = SimpleExoPlayer.Builder(context.applicationContext).build()
    private val containerView: VideoPlayerView by lazy {
        VideoPlayerView(context)
    }
    private var dataSourceFactory = DefaultDataSourceFactory(
        context,
        Util.getUserAgent(context, "SyncPlayer")
    )
    private val positionNotifier = PositionNotifier(handler, this)
    private var playerError: Exception? = null

    init {
        exoPlayer.addListener(this)
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
        val fillParent = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        container.addView(containerView, fillParent)
        containerView.setPlayer(exoPlayer)
    }

    override fun prepare() {
        if (!isPreparing) {
            targetPhase = AtomPlayerPhase.Ready
            playerError = null

            val mediaSource = createMediaSource(Uri.parse(videoUrl))
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
        }
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

    override fun seekTo(timeMs: Long) {
        if (isInPlaybackState() && timeMs < duration()) {
            exoPlayer.seekTo(timeMs)
        } else {
            if (currentPhase != AtomPlayerPhase.End) {
                pauseInternal()
                updatePlayerPhase(AtomPlayerPhase.End)
            }
            notifyChanged {
                it.onSeekTo(this, timeMs)
                it.onPositionChanged(this, timeMs)
            }
        }
    }

    override val isPlaying: Boolean
        get() = exoPlayer.isPlaying

    override val isError: Boolean
        get() = playerError != null

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.playbackParameters = PlaybackParameters(value)
        }

    override fun play() {
        if (currentPhase == AtomPlayerPhase.End) {
            return
        }
        if (currentPhase == AtomPlayerPhase.Idle) {
            prepare()
        } else {
            playInternal()
        }
        targetPhase = AtomPlayerPhase.Playing
    }

    private fun playInternal() {
        exoPlayer.playWhenReady = true
    }

    override fun pause() {
        handler.post {
            pauseInternal()
            targetPhase = AtomPlayerPhase.Paused
        }
    }

    private fun pauseInternal() {
        exoPlayer.playWhenReady = false
    }

    override fun release() {
        exoPlayer.release()
        currentPhase == AtomPlayerPhase.Idle
        targetPhase == AtomPlayerPhase.Idle
    }

    override fun currentPosition(): Long {
        return exoPlayer.currentPosition
    }

    override fun duration(): Long {
        if (isInPlaybackState()) {
            return exoPlayer.duration
        }
        return -1
    }

    private fun isInPlaybackState(): Boolean {
        return currentPhase != AtomPlayerPhase.Idle
    }

    override fun onPlaybackStateChanged(state: Int) {
        Log.d("[$name] onPlaybackStateChanged $state")

        when (state) {
            Player.STATE_IDLE -> {
                updatePlayerPhase(AtomPlayerPhase.Idle)
            }
            Player.STATE_BUFFERING -> {
                if (currentPhase != AtomPlayerPhase.Idle) {
                    updatePlayerPhase(AtomPlayerPhase.Buffering)
                }
            }
            Player.STATE_READY -> {
                if (currentPhase == AtomPlayerPhase.Idle) {
                    updatePlayerPhase(AtomPlayerPhase.Ready)
                }
                if (targetPhase == AtomPlayerPhase.Playing ||
                    targetPhase == AtomPlayerPhase.Paused
                ) {
                    exoPlayer.playWhenReady = targetPhase == AtomPlayerPhase.Playing
                    updatePlayerPhase(targetPhase)
                }
            }
            Player.STATE_ENDED -> {
                updatePlayerPhase(AtomPlayerPhase.End)
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        Log.d("[$name] onPlayWhenReadyChanged $playWhenReady, $reason")
        if (playWhenReady) {
            updatePlayerPhase(AtomPlayerPhase.Playing)
        } else {
            updatePlayerPhase(AtomPlayerPhase.Paused)
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Log.d("[$name] onPlayerError ${error.type} ${error.message}")
        this.playerError = error
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Log.d("[$name] onIsPlayingChanged $isPlaying")
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
            Log.d("[$name] onSeekEnd: ${exoPlayer.currentPosition}")
            val pos = exoPlayer.currentPosition
            notifyChanged {
                it.onSeekTo(this, pos)
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        // Log.d("$name onEvents $events")
    }
}