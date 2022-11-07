package com.agora.netless.syncplayer

/**
 * A factory class to create AtomPlayer
 *
 * @Experimental(level = Experimental.Level.WARNING)
 */
class SyncPlayer {
    companion object {
        const val VERSION = "1.0.2"

        @JvmStatic
        fun offset(atomPlayer: AtomPlayer, offset: Long): AtomPlayer {
            return OffsetPlayer(atomPlayer, offset)
        }

        @JvmStatic
        fun selection(atomPlayer: AtomPlayer, selectionOptions: SelectionOptions): AtomPlayer {
            return SelectionPlayer(atomPlayer, selectionOptions)
        }

        @JvmStatic
        fun combine(vararg atomPlayers: AtomPlayer): AtomPlayer {
            if (atomPlayers.isEmpty()) {
                throw RuntimeException("atomPlayers should not be empty!")
            }
            val size = atomPlayers.size
            if (size == 1) {
                return atomPlayers[0]
            } else if (size == 2) {
                return ClusterPlayer(atomPlayers[0], atomPlayers[1])
            }
            val middle = size / 2
            val partOne = combine(*atomPlayers.slice(0 until middle).toTypedArray())
            val partTwo = combine(*atomPlayers.slice(middle until size).toTypedArray())
            return combine(partOne, partTwo)
        }
    }
}