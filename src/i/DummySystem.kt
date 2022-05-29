package i


import doom.ticcmd_t

class DummySystem : IDoomSystem {
    override fun AllocLow(length: Int) {
        // TODO Auto-generated method stub
    }

    override fun BeginRead() {
        // TODO Auto-generated method stub
    }

    override fun EndRead() {
        // TODO Auto-generated method stub
    }

    override fun WaitVBL(count: Int) {
        // TODO Auto-generated method stub
    }

    override fun ZoneBase(size: Int): ByteArray? {
        // TODO Auto-generated method stub
        return null
    }

    override fun GetHeapSize(): Int {
        // TODO Auto-generated method stub
        return 0
    }

    override fun Tactile(on: Int, off: Int, total: Int) {
        // TODO Auto-generated method stub
    }

    override fun Quit() {
        // TODO Auto-generated method stub
    }

    override fun BaseTiccmd(): ticcmd_t? {
        // TODO Auto-generated method stub
        return null
    }

    override fun Error(error: String?, vararg args: Any?) {
        // TODO Auto-generated method stub
    }

    override fun Error(error: String?) {
        // TODO Auto-generated method stub
    }

    override fun Init() {
        // TODO Auto-generated method stub
    }

    override fun GenerateAlert(title: String?, cause: String?): Boolean {
        // TODO Auto-generated method stub
        return false
    }
}