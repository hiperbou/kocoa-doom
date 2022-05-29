package data


import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * A SideDef, defining the visual appearance of a wall, by setting textures and
 * offsets. ON-DISK.
 */
class mapsidedef_t : CacheableDoomObject {
    var textureoffset: Short = 0
    var rowoffset: Short = 0

    // 8-char strings.
    var toptexture: String? = null
    var bottomtexture: String? = null
    var midtexture: String? = null

    /** Front sector, towards viewer.  */
    var sector: Short = 0
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        textureoffset = buf.short
        rowoffset = buf.short
        toptexture = DoomBuffer.getNullTerminatedString(buf, 8)!!.uppercase(Locale.getDefault())
        bottomtexture = DoomBuffer.getNullTerminatedString(buf, 8)!!.uppercase(Locale.getDefault())
        midtexture = DoomBuffer.getNullTerminatedString(buf, 8)!!.uppercase(Locale.getDefault())
        sector = buf.short
    }

    companion object {
        fun sizeOf(): Int {
            return 30
        }
    }
}