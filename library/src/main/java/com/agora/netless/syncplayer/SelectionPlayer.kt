package com.agora.netless.syncplayer

import android.view.ViewGroup

/**
 * 选取一个播放器的多段
 */
class SelectionPlayer(
    private val atomPlayer: AtomPlayer,
    selectionOptions: SelectionOptions,
) : AbstractAtomPlayer() {
    // 内部播放器使用参数
    private val segInter: List<Selection> = selectionOptions.selections

    // 外部表现参数
    private val segOuter: List<Selection>

    // 正在播放的段
    private var currentSelection = 0

    init {
        var start = 0L
        val segOutTmp = ArrayList<Selection>(segInter.size)
        for (selection in segInter) {
            segOutTmp.add(Selection(start, start + selection.duration()))
            start += selection.duration()
        }
        segOuter = segOutTmp

        atomPlayer.addPlayerListener(object : AtomPlayerListener {
            override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
                currentSelection = segInter.indexOfFirst { timeMs < it.end }
                notifyChanged {
                    it.onSeekTo(this@SelectionPlayer, getOutFromIn(timeMs))
                }
            }

            override fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
                when (phaseChange) {
                    AtomPlayerPhase.Idle -> {

                    }
                    AtomPlayerPhase.Ready -> {
                        currentSelection = 0
                        atomPlayer.seekTo(segInter[0].start)
                        eventHandler.obtainMessage(INTERNAL_READY).sendToTarget()
                    }
                    AtomPlayerPhase.Paused -> {
                        eventHandler.obtainMessage(INTERNAL_PAUSED).sendToTarget()
                    }
                    AtomPlayerPhase.Playing -> {
                        eventHandler.obtainMessage(INTERNAL_PLAYING).sendToTarget()
                    }
                    AtomPlayerPhase.Buffering -> {
                        eventHandler.obtainMessage(INTERNAL_BUFFERING).sendToTarget()
                    }
                    AtomPlayerPhase.End -> {
                        eventHandler.obtainMessage(INTERNAL_END).sendToTarget()
                    }
                }
            }

            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                if (checkEnd(position)) {
                    pauseInternal()
                    updatePlayerPhase(AtomPlayerPhase.End)
                    return
                }
                val index = segInter.indexOfFirst { position < it.end }
                if (index == currentSelection + 1) {
                    atomPlayer.seekTo(segInter[index].start)
                } else {
                    // continue playing
                    notifyChanged {
                        it.onPositionChanged(this@SelectionPlayer, getOutFromIn(position))
                    }
                }
            }
        })
    }

    override fun prepareInternal() {
        atomPlayer.prepare()
    }

    override fun playInternal() {
        atomPlayer.play()
    }

    override fun pauseInternal() {
        atomPlayer.pause()
    }

    override fun release() {
        atomPlayer.release()
    }

    override fun seekToInternal(timeMs: Long) {
        val time = getInFromOut(timeMs)
        atomPlayer.seekTo(time)
    }

    private fun getInFromOut(timeMs: Long): Long {
        val first = segOuter.indexOfFirst { timeMs < it.end }
        if (first != -1) {
            return segInter[first].start + (timeMs - segOuter[first].start)
        }
        return timeMs
    }

    private fun getOutFromIn(inPosition: Long): Long {
        if (inPosition < segInter[0].start) {
            return 0
        }
        val first = segInter.indexOfFirst { inPosition < it.end }
        if (first != -1) {
            return segOuter[first].start + (inPosition - segInter[first].start)
        }
        return inPosition
    }

    private fun checkEnd(inPosition: Long) = inPosition > segInter.last().end

    override fun currentPosition(): Long {
        if (isInPlaybackState()) {
            val inPosition = atomPlayer.currentPosition()
            return getOutFromIn(inPosition)
        }
        return 0
    }

    override fun duration(): Long {
        if (isInPlaybackState()) {
            return segOuter.last().end
        }
        return -1
    }

    override fun setPlayerContainer(container: ViewGroup) {
        atomPlayer.setPlayerContainer(container)
    }
}

class SelectionOptions(val selections: List<Selection>)

data class Selection(val start: Long, val end: Long) {
    fun duration(): Long {
        return end - start
    }

    fun contains(position: Long): Boolean {
        return position in start..end
    }
}