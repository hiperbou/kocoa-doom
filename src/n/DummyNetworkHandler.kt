package n


import doom.IDoomGameNetworking
import doom.doomcom_t

class DummyNetworkHandler : IDoomGameNetworking {
    override fun NetUpdate() {
        // TODO Auto-generated method stub
    }

    override fun TryRunTics() {
        // TODO Auto-generated method stub
    }

    override fun getDoomCom(): doomcom_t? {
        // TODO Auto-generated method stub
        return null
    }

    override fun setDoomCom(doomcom: doomcom_t?) {
        // TODO Auto-generated method stub
    }

    override fun getTicdup(): Int {
        // TODO Auto-generated method stub
        return 0
    }

    override fun setTicdup(ticdup: Int) {
        // TODO Auto-generated method stub
    }
}