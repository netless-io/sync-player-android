package com.agora.netless.syncplayer

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
    private var currentSelection = -1

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
                if (phaseChange == AtomPlayerPhase.Ready) {
                    currentSelection = 0
                    atomPlayer.seekTo(segInter[0].start)
                }
                updatePlayerPhase(phaseChange)
            }

            override fun onPositionChanged(atomPlayer: AtomPlayer, position: Long) {
                if (checkInternalEnd(position)) {
                    atomPlayer.pause()
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

    override fun prepare() {
        atomPlayer.prepare()
    }

    override fun play() {
        atomPlayer.play()
    }

    override fun pause() {
        atomPlayer.pause()
    }

    override fun release() {
        atomPlayer.release()
    }

    override fun seekTo(timeMs: Long) {
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
        val first = segInter.indexOfFirst { inPosition < it.end }
        if (first != -1) {
            return segOuter[first].start + (inPosition - segInter[first].start)
        }
        return inPosition
    }

    private fun checkInternalEnd(inPosition: Long) = inPosition > segInter.last().end

    override fun currentPosition(): Long {
        val inPosition = atomPlayer.currentPosition()
        return getOutFromIn(inPosition)
    }

    override fun duration(): Long {
        return segOuter.last().end
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