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

/**
 * 用于多段视频播放
 */
class MultiVideoPlayer constructor(
    private val context: Context,
    private val videos: List<VideoItem>,
) : AbstractAtomPlayer() {
    private val selections = videos.mapIndexed { index, videoItem ->
        Selection((if (index != 0) videos[index - 1].endTime else 0), videoItem.endTime)
    }

    private val positionNotifier = PositionNotifier(eventHandler, this)
    private var exoPlayer = SimpleExoPlayer.Builder(context.applicationContext).build()
    private var dataSourceFactory = DefaultDataSourceFactory(context, "SyncPlayer")

    private var fakePlayer = FakePlayer(0)

    private var videoPlaying = videos[0].beginTime == 0L
    private var currentSelection = 0

    private val interPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            Log.d("[$name] exoPlayer onPlaybackStateChanged $state")

            when (state) {
                Player.STATE_IDLE -> {
                    updatePlayerPhase(AtomPlayerPhase.Idle)
                }
                Player.STATE_BUFFERING -> {
                    eventHandler.obtainMessage(INTERNAL_BUFFERING).sendToTarget()
                }
                Player.STATE_READY -> {
                    when (currentPhase) {
                        AtomPlayerPhase.Idle -> {
                            eventHandler.obtainMessage(INTERNAL_READY).sendToTarget()
                        }
                        AtomPlayerPhase.Buffering -> {
                            if (exoPlayer.playWhenReady) {
                                eventHandler.obtainMessage(INTERNAL_PLAYING).sendToTarget()
                            } else {
                                eventHandler.obtainMessage(INTERNAL_PAUSED).sendToTarget()
                            }
                        }
                        else -> {}
                    }
                }
                Player.STATE_ENDED -> {
                    playNext()
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
                Log.d("[$name] exoPlayer position changed: ${exoPlayer.currentPosition}")
                val pos = currentPosition()
                notifyChanged {
                    it.onSeekTo(this@MultiVideoPlayer, pos)
                    it.onPositionChanged(this@MultiVideoPlayer, pos)
                }
            }
        }
    }

    init {
        addPlayerListener(object : AtomPlayerListener {
            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                if (videoPlaying && validSelection()) {
                    if (position > selections[currentSelection].end) {
                        playNext()
                    }
                }
            }
        })
        exoPlayer.addListener(interPlayerListener)
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, false)
        exoPlayer.playWhenReady = false

        fakePlayer.addPlayerListener(object : AtomPlayerListener {
            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                if (validSelection()) {
                    val selection = selections[currentSelection]
                    if (position < selection.duration()) {
                        Log.d("[$name] fakePlayer onPositionChanged: $position, notify ${selection.start + position}")
                        notifyChanged {
                            it.onPositionChanged(this@MultiVideoPlayer, selection.start + position)
                        }
                    }
                }
            }

            override fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
                when (phaseChange) {
                    AtomPlayerPhase.End -> {
                        playNext()
                    }
                    else -> {}
                }
            }

            override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
                notifyChanged {
                    it.onSeekTo(this@MultiVideoPlayer, timeMs)
                }
            }
        })
        fakePlayer.updateDuration(currentFakeDuration())
    }

    override fun setPlayerContainer(container: ViewGroup) {
        if (container !is FrameLayout) {
            throw IllegalArgumentException("videoPlayer container must be type of FrameLayout!")
        }
        container.addView(bindPlayer(exoPlayer))
    }

    private fun bindPlayer(player: Player): View {
        val playerView = VideoPlayerView(context).apply {
            setPlayer(player)
        }
        return playerView
    }

    override fun seekToInternal(timeMs: Long) {
        val index = selections.indexOfFirst { timeMs < it.end }
        if (index != -1) {
            if (index != currentSelection) {
                currentSelection = index
                fakePlayer.updateDuration(currentFakeDuration())
                setVideoPath(videos[currentSelection].videoURL)
            }
            seekCurrentSelection(timeMs)
        }
    }

    private fun seekCurrentSelection(timeMs: Long) {
        videoPlaying = videos[currentSelection].beginTime < timeMs
        if (videoPlaying) {
            fakePlayer.pause()
            exoPlayer.playWhenReady = true
            exoPlayer.seekTo(currentPlayerPosition(timeMs))
        } else {
            exoPlayer.playWhenReady = false
            fakePlayer.seekTo(currentFakePosition(timeMs))
            fakePlayer.play()
        }
    }

    private fun currentFakeDuration() =
        videos[currentSelection].beginTime - selections[currentSelection].start

    private fun currentFakePosition(timeMs: Long) = timeMs - selections[currentSelection].start

    private fun currentPlayerPosition(timeMs: Long) = timeMs - videos[currentSelection].beginTime

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.playbackParameters = PlaybackParameters(value)
            fakePlayer.playbackSpeed = value
        }

    override fun prepareInternal() {
        setVideoPath(videos[0].videoURL)
        exoPlayer.prepare()
        fakePlayer.prepare()
    }

    override fun playInternal() {
        if (videoPlaying) {
            exoPlayer.playWhenReady = true
        } else {
            fakePlayer.play()
        }
    }

    override fun pauseInternal() {
        exoPlayer.playWhenReady = false
        fakePlayer.pause()
    }

    override fun release() {
        fakePlayer.release()
        exoPlayer.release()
        positionNotifier.stop()
    }

    override fun currentPosition(): Long {
        if (!isInPlaybackState()) {
            return 0
        }
        return if (validSelection()) {
            if (isPlaying) {
                videos[currentSelection].beginTime + exoPlayer.currentPosition
            } else {
                selections[currentSelection].start + fakePlayer.currentPosition()
            }
        } else {
            duration()
        }
    }

    private fun validSelection() = currentSelection >= 0 && currentSelection < videos.size

    override fun duration(): Long {
        if (isInPlaybackState()) {
            return videos.last().endTime
        }
        return -1
    }

    private fun playNext() {
        Log.d("[$name] play next called: $videoPlaying")
        if (videoPlaying) {
            // play next fake
            currentSelection += 1
            if (currentSelection < selections.size) {
                exoPlayer.playWhenReady = false
                // setVideoPath(videos[currentSelection].videoURL)

                fakePlayer.updateDuration(currentFakeDuration())
                fakePlayer.seekTo(0)
                fakePlayer.play()
            } else {
                eventHandler.obtainMessage(INTERNAL_END).sendToTarget()
            }
        } else {
            // play current video
            setVideoPath(videos[currentSelection].videoURL)
            exoPlayer.playWhenReady = true
        }
        videoPlaying = !videoPlaying
    }

    private fun setVideoPath(path: String) {
        val mediaSource = createMediaSource(Uri.parse(path))
        exoPlayer.setMediaSource(mediaSource)
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
}

data class VideoItem constructor(
    val beginTime: Long,
    val endTime: Long,
    val videoURL: String,
)