package data


import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * LineSeg, generated by splitting LineDefs
 * using partition lines selected by BSP builder.
 * MAES: this is the ON-DISK structure. The corresponding memory structure,
 * segs_t, has fixed_t members.
 */
class mapseg_t : CacheableDoomObject {
    var v1 = 0.toChar()
    var v2 = 0.toChar()
    var angle = 0.toChar()
    var linedef = 0.toChar()
    var side = 0.toChar()
    var offset = 0.toChar()
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        v1 = buf.char
        v2 = buf.char
        angle = buf.char
        linedef = buf.char
        side = buf.char
        offset = buf.char
    }

    override fun toString(): String {
        return String.format(
            "mapseg_t v1,2: %d %d ang: %d ld: %d sd: %d off: %d",
            v1.code,
            v2.code,
            angle.code,
            linedef.code,
            side.code,
            offset.code
        )
    }

    companion object {
        fun sizeOf(): Int {
            return 12
        }
    }
}