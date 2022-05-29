package p


import rr.SectorAction
import w.DoomIO
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer

class strobe_t : SectorAction() {
    var count = 0
    var minlight = 0
    var maxlight = 0
    var darktime = 0
    var brighttime = 0

    //
    // T_StrobeFlash
    //
    fun StrobeFlash() {
        if (--count != 0) {
            return
        }
        val sector = sector!!
        if (sector.lightlevel.toInt() == minlight) {
            sector.lightlevel = maxlight.toShort()
            count = brighttime
        } else {
            sector.lightlevel = minlight.toShort()
            count = darktime
        }
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        super.read(f) // Call thinker reader first            
        super.sectorid = DoomIO.readLEInt(f) // Sector index
        count = DoomIO.readLEInt(f)
        maxlight = DoomIO.readLEInt(f)
        minlight = DoomIO.readLEInt(f)
        darktime = DoomIO.readLEInt(f)
        brighttime = DoomIO.readLEInt(f)
    }

    @Throws(IOException::class)
    override fun pack(b: ByteBuffer) {
        super.pack(b) //12            
        b.putInt(super.sectorid) // 16
        b.putInt(count) //20
        b.putInt(maxlight) //24
        b.putInt(minlight) //28
        b.putInt(darktime) //32
        b.putInt(brighttime) //36
    }
}