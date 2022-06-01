package com.agora.netless.syncplayer

import android.util.Range

class SegmentPlayer(val atomPlayer: AtomPlayer, val segmentOptions: SegmentOptions) : AtomPlayer() {
    init {

    }

    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun play() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun release() {
        TODO("Not yet implemented")
    }

    override fun seekTo(timeMs: Long) {

    }

    override fun currentPosition(): Long {
        TODO("Not yet implemented")
    }

    override fun duration(): Long {
        TODO("Not yet implemented")
    }
}

class SegmentOptions(val segment: List<Segment>)

class Segment(start: Long, end: Long) {
    private val range = Range(start, end)

    fun contains(value: Long): Boolean {
        return range.contains(value)
    }
}