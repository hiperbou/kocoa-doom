package doom


import java.io.IOException

/** Doom is actually tied to its networking module.
 * Therefore, no matter where and how you implement it, these functions
 * need to be callable from within many modules.
 *
 * This is the so called "game networking" which is internal and game-specific,
 * and not system networking which deals with the low level sockets and packet
 * stuff. You'll need DoomSystemNetworking for that one.
 *
 * @author Velktron
 */
interface IDoomGameNetworking {
    @Throws(IOException::class)
    fun TryRunTics()

    /**
     * NetUpdate
     * Builds ticcmds for console player,
     * sends out a packet
     * @throws IOException
     */
    fun NetUpdate()
    fun getDoomCom(): doomcom_t?
    fun setDoomCom(doomcom: doomcom_t?)
    fun getTicdup(): Int
    fun setTicdup(ticdup: Int)
}