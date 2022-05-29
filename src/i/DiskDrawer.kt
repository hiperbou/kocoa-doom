package i


import doom.DoomMain
import rr.patch_t
import v.renderers.DoomScreen

class DiskDrawer(private val DOOM: DoomMain<*, *>, private val diskname: String) : IDiskDrawer {
    private var disk: patch_t? = null
    private var timer = 0
    override fun Init() {
        disk = DOOM.wadLoader.CachePatchName(diskname)
    }

    override fun Drawer() {
        if (timer > 0) {
            if (timer % 2 == 0) DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, disk!!, DOOM.vs, 304, 184)
        }
        if (timer >= 0) timer--
    }

    override fun setReading(reading: Int) {
        timer = reading
    }

    override fun isReading(): Boolean {
        return timer > 0
    }

    override fun justDoneReading(): Boolean {
        return timer == 0
    }

    companion object {
        const val STDISK = "STDISK"
        const val STCDROM = "STCDROM"
    }
}