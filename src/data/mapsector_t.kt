package data


import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/** Sector definition, from editing.  */
class mapsector_t : CacheableDoomObject {
    var floorheight: Short = 0
    var ceilingheight: Short = 0
    var floorpic: String? = null
    var ceilingpic: String? = null
    var lightlevel: Short = 0
    var special: Short = 0
    var tag: Short = 0
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        floorheight = buf.short
        ceilingheight = buf.short
        floorpic = DoomBuffer.getNullTerminatedString(buf, 8)!!.uppercase(Locale.getDefault())
        ceilingpic = DoomBuffer.getNullTerminatedString(buf, 8)!!.uppercase(Locale.getDefault())
        lightlevel = buf.short
        special = buf.short
        tag = buf.short
    }

    companion object {
        fun sizeOf(): Int {
            return 26
        }
    }
}