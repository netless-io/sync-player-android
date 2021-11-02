package com.agora.netless.syncplayer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class SimpleVideoPlayer @JvmOverloads constructor(
    context: Context,
    videoPath: String,
    appName: String? = null,
) : AtomPlayer(), Player.EventListener {
    private var exoPlayer = SimpleExoPlayer.Builder(context.applicationContext).build()
    private var mediaSource: MediaSource? = null
    private var playerView: PlayerView? = null
    private var dataSourceFactory = DefaultDataSourceFactory(
        context,
        appName?.let { Util.getUserAgent(context, it) }
    )
    private val handler = Handler(Looper.getMainLooper())
    private var currentState = Player.STATE_IDLE

    init {
        exoPlayer.addListener(this)
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, false)
        exoPlayer.playWhenReady = false
        setVideoPath(videoPath)
    }

    /**
     * 设置播放视图
     *
     * @param playerView 视图实例
     */
    override fun setPlayerView(playerView: View) {
        if (playerView !is PlayerView) {
            throw IllegalArgumentException("view must be type of PlayerView")
        }
        this.playerView = playerView
        this.playerView!!.requestFocus()
        this.playerView!!.player = exoPlayer
    }

    private fun setVideoPath(path: String) {
        setVideoURI(Uri.parse(path))
    }

    private fun setVideoURI(uri: Uri) {
        mediaSource = createMediaSource(uri)
        atomPlayerPhase = AtomPlayerPhase.Buffering
        exoPlayer.prepare(mediaSource!!)
    }

    override fun seek(timeMs: Long) {
        exoPlayer.seekTo(timeMs)
    }

    override val isPlaying: Boolean = exoPlayer.isPlaying

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.setPlaybackParameters(PlaybackParameters(value))
        }

    override fun play() {
        handler.post {
            if (isPlaying) {
                return@post
            }
            exoPlayer.playWhenReady = true
            Log.d("play $name isPlaying $isPlaying")
        }
    }

    override fun pause() {
        handler.post {
            if (!isPlaying) {
                return@post
            }
            exoPlayer.playWhenReady = false
            Log.d("pause $name isPlaying $isPlaying")
        }
    }

    override fun release() {
        exoPlayer.release()
        mediaSource = null
    }

    override fun getPhase(): AtomPlayerPhase {
        return atomPlayerPhase
    }

    override fun currentTime(): Long {
        return exoPlayer.currentPosition
    }

    override fun duration(): Long {
        return exoPlayer.duration
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d("$name onPlayerState $isPlaying $playbackState")
        currentState = playbackState
        when (playbackState) {
            Player.STATE_IDLE -> {; }
            Player.STATE_BUFFERING -> {
                updatePlayerPhase(AtomPlayerPhase.Buffering)
            }
            Player.STATE_READY -> {
                updatePlayerPhase(if (isPlaying) AtomPlayerPhase.Playing else AtomPlayerPhase.Pause)
            }
            Player.STATE_ENDED -> {; }
            else -> {; }
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Log.d("$name onPlayerError ${error.message}")
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
        Log.d("$name onIsPlayingChanged $isPlaying")
    }

    override fun onSeekProcessed() {
        val pos = exoPlayer.currentPosition
        atomPlayerListener?.onSeekTo(this, pos)
    }

    private fun createMediaSource(uri: Uri): MediaSource {
        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }
}