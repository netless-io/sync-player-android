package com.agora.netless.syncplayer

import android.view.View
import android.view.ViewGroup

class OffsetPlayer constructor(
    private val player: AtomPlayer,
    private val offset: Long,
) : AbstractAtomPlayer() {
    private var container: ViewGroup? = null
    private val fakePlayer = FakePlayer(offset)
    private var nextPlaying = false

    init {
        fakePlayer.addPlayerListener(object : AtomPlayerListener {
            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                if (position < offset) {
                    notifyChanged {
                        it.onPositionChanged(this@OffsetPlayer, position)
                    }
                }
            }

            override fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
                when (phaseChange) {
                    AtomPlayerPhase.Idle -> {

                    }
                    AtomPlayerPhase.Ready -> {
                        checkReady()
                    }
                    AtomPlayerPhase.Paused -> {
                        if (targetPhase == AtomPlayerPhase.Paused) {
                            updatePlayerPhase(AtomPlayerPhase.Paused)
                        }
                    }
                    AtomPlayerPhase.Playing -> {
                        updatePlayerPhase(AtomPlayerPhase.Playing)
                    }
                    AtomPlayerPhase.Buffering -> {
                        updatePlayerPhase(AtomPlayerPhase.Buffering)
                    }
                    AtomPlayerPhase.End -> {
                        playNext()
                    }
                }
            }

            override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
                notifyChanged {
                    it.onSeekTo(this@OffsetPlayer, timeMs)
                }
                adjustPlayer(timeMs)
            }
        })

        player.addPlayerListener(object : AtomPlayerListener {
            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                notifyChanged {
                    it.onPositionChanged(this@OffsetPlayer, position + offset)
                }
            }

            override fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
                when (phaseChange) {
                    AtomPlayerPhase.Idle -> {

                    }
                    AtomPlayerPhase.Ready -> {
                        checkReady()
                    }
                    AtomPlayerPhase.Paused -> {
                        if (targetPhase == AtomPlayerPhase.Paused) {
                            updatePlayerPhase(AtomPlayerPhase.Paused)
                        }
                    }
                    AtomPlayerPhase.Playing -> {
                        updatePlayerPhase(AtomPlayerPhase.Playing)
                    }
                    AtomPlayerPhase.Buffering -> {
                        updatePlayerPhase(AtomPlayerPhase.Buffering)
                    }
                    AtomPlayerPhase.End -> {
                        updatePlayerPhase(AtomPlayerPhase.End)
                    }
                }
            }

            override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
                notifyChanged {
                    it.onSeekTo(this@OffsetPlayer, timeMs + offset)
                }
                adjustPlayer(timeMs + offset)
            }
        })
    }

    private fun checkReady() {
        if (player.currentPhase == AtomPlayerPhase.Ready
            && fakePlayer.currentPhase == AtomPlayerPhase.Ready
        ) {
            eventHandler.obtainMessage(INTERNAL_READY).sendToTarget()
        }
    }

    override fun prepareInternal() {
        player.prepare()
        fakePlayer.prepare()
    }

    override fun playInternal() {
        if (nextPlaying) {
            player.play()
        } else {
            fakePlayer.play()
        }
    }

    override fun pauseInternal() {
        fakePlayer.pause()
        player.pause()
    }

    override fun release() {
        player.release()
    }

    override fun seekToInternal(timeMs: Long) {
        if (timeMs < offset) {
            fakePlayer.seekTo(timeMs)
        } else {
            player.seekTo(timeMs - offset)
        }
    }

    private fun adjustPlayer(position: Long) {
        if (!isPlaying) {
            return
        }
        updateNextPlaying(position >= offset)
        if (position >= offset) {
            fakePlayer.pause()
            player.play()
        } else {
            player.pause()
            fakePlayer.play()
        }
    }

    private fun updateNextPlaying(nextPlaying: Boolean) {
        this.nextPlaying = nextPlaying
        container?.visibility = if (nextPlaying) View.VISIBLE else View.INVISIBLE
    }

    private fun playNext() {
        updateNextPlaying(true)
        player.seekTo(0)
        player.play()
    }

    override fun currentPosition(): Long {
        if (fakePlayer.isPlaying) {
            return fakePlayer.currentPosition()
        }
        if (player.isPlaying) {
            return player.currentPosition() + offset
        }
        return 0
    }

    override fun duration(): Long {
        return player.duration() + fakePlayer.duration()
    }

    override fun setPlayerContainer(container: ViewGroup) {
        player.setPlayerContainer(container)
        this.container = container
        this.container?.visibility = if (nextPlaying) View.VISIBLE else View.INVISIBLE
    }
}