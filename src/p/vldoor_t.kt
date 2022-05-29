package p


import rr.SectorAction
import w.DoomIO
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer

class vldoor_t : SectorAction(), IReadableDoomObject {
    var type: vldoor_e? = null

    /** fixed_t  */
    var topheight = 0
    var speed = 0

    /** 1 = up, 0 = waiting at top, -1 = down  */
    var direction = 0

    /** tics to wait at the top  */
    var topwait = 0

    /**(keep in case a door going down is reset)
     * when it reaches 0, start going down  */
    var topcountdown = 0
    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        super.read(f) // Call thinker reader first            
        type = vldoor_e.values()[DoomIO.readLEInt(f)]
        super.sectorid = DoomIO.readLEInt(f) // Sector index (or pointer?)
        topheight = DoomIO.readLEInt(f)
        speed = DoomIO.readLEInt(f)
        direction = DoomIO.readLEInt(f)
        topwait = DoomIO.readLEInt(f)
        topcountdown = DoomIO.readLEInt(f)
    }

    @Throws(IOException::class)
    override fun pack(b: ByteBuffer) {
        super.pack(b) //12            
        b.putInt(type!!.ordinal) // 16
        b.putInt(super.sectorid) // 20
        b.putInt(topheight) // 24
        b.putInt(speed) //28
        b.putInt(direction) // 32
        b.putInt(topwait) //36
        b.putInt(topcountdown) //40
    }
}