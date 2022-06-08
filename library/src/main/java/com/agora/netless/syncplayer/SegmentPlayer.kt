package com.agora.netless.syncplayer

/**
 * 选取一个播放器的多段
 */
class SegmentPlayer(
    val atomPlayer: AtomPlayer,
    segmentOptions: SegmentOptions,
) : AtomPlayer() {
    private var segIn: List<Segment> = segmentOptions.segment
    private var segOut: MutableList<Segment> = ArrayList(segIn.size)

    init {
        var start = 0L
        segIn.forEachIndexed { index, segment ->
            segOut[index] = Segment(start, start + segment.duration())
            start += segment.duration()
        }
    }

    override fun prepare() {
        TODO("Not yet implemented")
    }

    override fun play() {

    }

    override fun pause() {
        atomPlayer.pause()
    }

    override fun release() {
        atomPlayer.release()
    }

    override fun seekTo(timeMs: Long) {
        val time = getInFromOut(timeMs)
    }

    private fun getInFromOut(timeMs: Long): Long {
        val first = segOut.indexOfFirst { timeMs < it.end }
        if (first != -1) {
            return segIn[first].start + (timeMs - segOut[first].start)
        }
        return timeMs
    }

    private fun getOutFromIn(inPosition: Long) {

    }

    override fun currentPosition(): Long {
        return 0
    }

    override fun duration(): Long {
        return segOut.last().end
    }
}

class SegmentOptions(val segment: List<Segment>)

data class Segment(val start: Long, val end: Long) {
    fun duration(): Long {
        return end - start
    }

    fun contains(position: Long): Boolean {
        return position in start..end
    }
}