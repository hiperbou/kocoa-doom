package n

import doom.CommandVariable
import doom.DoomMain
import doom.NetConsts
import doom.doomcom_t
import mochadoom.Engine


/** Does nothing.
 * Allows running single-player games without an actual network.
 * Hopefully, it will be replaced by a real UDP-based driver one day.
 *
 * @author Velktron
 */
class DummyNetworkDriver<T, V>(  ////////////// STATUS ///////////
    private val DOOM: DoomMain<T, V>
) : NetConsts, DoomSystemNetworking {
    override fun InitNetwork() {
        val doomcom = doomcom_t()
        doomcom.id = NetConsts.DOOMCOM_ID
        doomcom.ticdup = 1

        // single player game
        DOOM.netgame = Engine.getCVM().present(CommandVariable.NET)
        doomcom.id = NetConsts.DOOMCOM_ID
        doomcom.numnodes = 1
        doomcom.numplayers = doomcom.numnodes
        doomcom.deathmatch = 0
        doomcom.consoleplayer = 0
        DOOM.gameNetworking.setDoomCom(doomcom)
    }

    override fun NetCmd() {
        // TODO Auto-generated method stub
    }
}