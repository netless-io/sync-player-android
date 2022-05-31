package com.agora.netless.syncplayer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
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
    private val handler = Handler(Looper.getMainLooper())
    private val positionNotifier = PositionNotifier(handler, this)
    private var targetPhase = AtomPlayerPhase.Idle
    private var error: Exception? = null;

    init {
        exoPlayer.addListener(this)
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, false)
        exoPlayer.playWhenReady = false
    }

    /**
     * 设置播放视图
     *
     * @param view 视图实例
     */
    override fun setPlayerView(view: View) {
        if (view !is FrameLayout) {
            throw IllegalArgumentException("view must be type of PlayerView")
        }
        val fillParent = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        view.addView(containerView, fillParent)
        containerView.setPlayer(exoPlayer)
        addPlayerListener(object : AtomPlayerListener {
            override fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
                containerView.showBuffering(playerPhase == AtomPlayerPhase.Buffering)
            }

            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                containerView.setPosition(position)
            }
        })
    }

    override fun setup() {
        targetPhase = AtomPlayerPhase.Ready
        error = null
        updatePlayerPhase(AtomPlayerPhase.Buffering)
        setVideoURI(Uri.parse(videoUrl))
    }

    private fun setVideoURI(uri: Uri) {
        val mediaSource = createMediaSource(uri)
        exoPlayer.setMediaSources(listOf(mediaSource), true)
        exoPlayer.prepare()
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
        Log.d("[$name] seekTo call: $timeMs, $debugInfo")
        exoPlayer.seekTo(timeMs)
    }

    override val isPlaying: Boolean
        get() = exoPlayer.isPlaying

    override val isError: Boolean
        get() = error != null

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.playbackParameters = PlaybackParameters(value)
        }

    override fun play() {
        handler.post {
            Log.d("[$name] play called, $debugInfo")
            if (playerPhase == AtomPlayerPhase.Idle) {
                setup()
            } else {
                // real play
                exoPlayer.playWhenReady = true
            }
            targetPhase = AtomPlayerPhase.Playing
        }
    }

    override fun pause() {
        handler.post {
            Log.d("[$name] pause called: $debugInfo")
            exoPlayer.playWhenReady = false
            targetPhase = AtomPlayerPhase.Paused
        }
    }

    override fun release() {
        exoPlayer.release()
    }

    override fun getPhase(): AtomPlayerPhase {
        return playerPhase
    }

    override fun currentPosition(): Long {
        return exoPlayer.currentPosition
    }

    override fun duration(): Long {
        return exoPlayer.duration
    }

    override fun onPlaybackStateChanged(state: Int) {
        Log.d("[$name] onPlaybackStateChanged $state, $debugInfo")

        when (state) {
            Player.STATE_IDLE -> {
                updatePlayerPhase(AtomPlayerPhase.Idle)
            }
            Player.STATE_BUFFERING -> {
                updatePlayerPhase(AtomPlayerPhase.Buffering)
            }
            Player.STATE_READY -> {
                updatePlayerPhase(AtomPlayerPhase.Ready)
                when (targetPhase) {
                    AtomPlayerPhase.Paused, AtomPlayerPhase.Playing -> {
                        exoPlayer.playWhenReady = targetPhase == AtomPlayerPhase.Playing
                        updatePlayerPhase(if (targetPhase == AtomPlayerPhase.Playing) AtomPlayerPhase.Playing else AtomPlayerPhase.Paused)
                    }
                    AtomPlayerPhase.Ready -> {; }
                    AtomPlayerPhase.Buffering, AtomPlayerPhase.Idle, AtomPlayerPhase.End -> {; }
                }
            }
            Player.STATE_ENDED -> {
                updatePlayerPhase(AtomPlayerPhase.End)
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        Log.d("[$name] onReadyChanged $playWhenReady, $reason, $debugInfo")
        // TODO 内部出发暂停
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Log.d("[$name] onPlayerError ${error.message}, $debugInfo")
        this.error = error;
        when (error.type) {
            ExoPlaybackException.TYPE_SOURCE -> {
            }
            ExoPlaybackException.TYPE_RENDERER -> {
            }
            ExoPlaybackException.TYPE_UNEXPECTED -> {
            }
            ExoPlaybackException.TYPE_REMOTE -> {
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Log.d("[$name] onIsPlayingChanged $isPlaying, $debugInfo")
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