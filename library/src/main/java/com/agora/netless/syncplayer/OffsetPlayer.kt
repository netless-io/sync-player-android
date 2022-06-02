package com.agora.netless.syncplayer

class OffsetPlayer constructor(
    private val player: AtomPlayer,
    private val offset: Long,
) : AtomPlayer() {
    private var startPosition = 0L
    private var startTime = 0L
    private val fakePlayer = FakePlayer()

    init {
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
                        updatePlayerPhase(AtomPlayerPhase.Ready)
                        if (targetPhase == AtomPlayerPhase.Playing) {
                            playInternal()
                        }
                        if (targetPhase == AtomPlayerPhase.Paused) {
                            pauseInternal()
                        }
                    }
                    AtomPlayerPhase.Paused -> {
                        updatePlayerPhase(AtomPlayerPhase.Paused)
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

    override fun prepare() {
        if (!isPreparing) {
            player.prepare()
            targetPhase == AtomPlayerPhase.Ready
        }
    }

    override fun play() {
        if (currentPhase == AtomPlayerPhase.Idle) {
            prepare()
        } else {
            playInternal()
        }
        targetPhase == AtomPlayerPhase.Playing
    }

    private fun playInternal() {
        startPosition = 0
        startTime = System.currentTimeMillis()
    }

    override fun pause() {
        // Check if isPlaying is OK?
        if (isPlaying) {
            pauseInternal()
            // Check Pause When End
            targetPhase == AtomPlayerPhase.Paused
        }
    }

    private fun pauseInternal() {
        player.pause()
    }

    override fun release() {
        player.release()
        currentPhase == AtomPlayerPhase.End
        targetPhase == AtomPlayerPhase.End
    }

    override fun seekTo(timeMs: Long) {
        if (timeMs < offset) {
            startPosition = timeMs;
            startTime = System.currentTimeMillis()
        } else {
            player.seekTo(timeMs - offset)
        }
    }

    override fun currentPosition(): Long {
        val fakeTime = startPosition + (System.currentTimeMillis() - startTime)
        if (fakeTime < offset) {
            return fakeTime;
        }
        return player.currentPosition() + offset
    }

    override fun duration(): Long {
        return player.duration() + offset
    }
}