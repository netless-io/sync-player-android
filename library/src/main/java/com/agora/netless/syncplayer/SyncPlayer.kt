package com.agora.netless.syncplayer

/**
 * A factory class to create AtomPlayer
 *
 * @Experimental(level = Experimental.Level.WARNING)
 */
class SyncPlayer {
    companion object {
        const val VERSION = "1.0.0-beta.1"

        @JvmStatic
        fun offset(atomPlayer: AtomPlayer, offset: Long): AtomPlayer {
            return OffsetPlayer(atomPlayer, offset)
        }

        @JvmStatic
        fun selection(atomPlayer: AtomPlayer, selectionOptions: SelectionOptions): AtomPlayer {
            return SelectionPlayer(atomPlayer, selectionOptions)
        }

        @JvmStatic
        fun combine(aPlayer: AtomPlayer, bPlayer: AtomPlayer): AtomPlayer {
            return ClusterPlayer(aPlayer, bPlayer)
        }
    }
}