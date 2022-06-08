package com.agora.netless.syncplayer

class OffsetPlayer constructor(
    private val player: AtomPlayer,
    private val offset: Long,
) : AtomPlayer() {
    private val fakePlayer = FakePlayer(offset)

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
            }
        })
    }

    private fun checkReady() {
        if (player.currentPhase == AtomPlayerPhase.Ready
            && fakePlayer.currentPhase == AtomPlayerPhase.Ready
        ) {
            updatePlayerPhase(AtomPlayerPhase.Ready)
            if (targetPhase == AtomPlayerPhase.Playing) {
                playInternal()
            }
            if (targetPhase == AtomPlayerPhase.Paused) {
                pauseInternal()
            }
        }
    }

    override fun prepare() {
        if (!isPreparing) {
            player.prepare()
            fakePlayer.prepare()
            targetPhase = AtomPlayerPhase.Ready
        }
    }

    override fun play() {
        if (currentPhase == AtomPlayerPhase.Idle) {
            prepare()
        } else {
            playInternal()
        }
        targetPhase = AtomPlayerPhase.Playing
    }

    private fun playInternal() {
        if (fakePlayer.currentPosition() < offset) {
            fakePlayer.play()
        } else {
            player.play()
        }
    }

    override fun pause() {
        if (isPlaying) {
            pauseInternal()
        }
        targetPhase = AtomPlayerPhase.Paused
    }

    private fun pauseInternal() {
        fakePlayer.pause()
        player.pause()
    }

    override fun release() {
        player.release()
        currentPhase = AtomPlayerPhase.End
        targetPhase = AtomPlayerPhase.End
    }

    override fun seekTo(timeMs: Long) {
        if (timeMs < offset) {
            fakePlayer.seekTo(timeMs)
        } else {
            player.seekTo(timeMs - offset)
        }
        adjustPlayer(timeMs)
    }

    private fun adjustPlayer(position: Long) {
        if (!isPlaying) {
            return
        }
        if (position > offset) {
            fakePlayer.pause()
            player.play()
        } else {
            fakePlayer.play()
            player.pause()
        }
    }

    private fun playNext() {
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
}