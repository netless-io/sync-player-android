package com.agora.netless.syncplayer

/**
 * 选取一个播放器的多段
 */
class SelectionPlayer(
    private val atomPlayer: AtomPlayer,
    selectionOptions: SelectionOptions,
) : AtomPlayer() {
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
                updatePlayerPhase(phaseChange)
            }

            override fun onPositionChanged(atomPlayer: AtomPlayer, inPosition: Long) {
                notifyChanged {
                    it.onPositionChanged(this@SelectionPlayer, getOutFromIn(inPosition))
                }
                checkAndSeek(inPosition)
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

    private fun checkAndSeek(inPosition: Long) {
        if (inPosition > segInter.last().end) {
            atomPlayer.pause()
            updatePlayerPhase(AtomPlayerPhase.End)
            return
        }
        val first = segInter.indexOfFirst { inPosition < it.end }
        if (first == currentSelection + 1) {
            atomPlayer.seekTo(segInter[first].start)
        }
    }

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