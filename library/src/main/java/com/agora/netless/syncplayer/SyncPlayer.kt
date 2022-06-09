package com.agora.netless.syncplayer

/**
 *
 * @Experimental(level = Experimental.Level.WARNING)
 */
class SyncPlayer {
    companion object {
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