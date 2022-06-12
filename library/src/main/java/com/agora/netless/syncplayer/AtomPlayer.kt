package com.agora.netless.syncplayer

import android.view.ViewGroup

/**
 * 接口说明
 * play 为组合接口，用户可以用过调用 play 准备并启动播放，在 Ready｜Paused状态下，AtomPLayer调用 play 立刻切换 为 Playing状态，同时出发 Playing 状态回调
 * pause
 */
interface AtomPlayer {
    /**
     * mostly，it's used for debug
     */
    var name: String

    val currentPhase: AtomPlayerPhase

    val isPlaying: Boolean

    val isError: Boolean

    var playbackSpeed: Float

    fun prepare()

    fun play()

    fun pause()

    fun stop()

    fun release()

    fun seekTo(timeMs: Long)

    fun currentPosition(): Long

    fun duration(): Long

    fun setPlayerContainer(container: ViewGroup)

    fun addPlayerListener(listener: AtomPlayerListener);

    fun removePlayerListener(listener: AtomPlayerListener);
}

interface AtomPlayerListener {
    fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {}

    fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {}

    fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {}
}

enum class AtomPlayerPhase {
    /**
     * 视频播放尚未开始或播放过程中出现错误。
     */
    Idle,

    /**
     * 视频准备就绪
     */
    Ready,

    /**
     * 视频播放已暂停。
     */
    Paused,

    /**
     * 正在播放视频。
     */
    Playing,

    /**
     * 视频正在缓冲。
     */
    Buffering,

    /**
     * 播放结束。
     *
     */
    End,
}