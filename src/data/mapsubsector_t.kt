package data


import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** SubSector, as generated by BSP.  */
class mapsubsector_t : CacheableDoomObject {
    var numsegs = 0.toChar()

    /** Index of first one, segs are stored sequentially.  */
    var firstseg = 0.toChar()
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        numsegs = buf.char
        firstseg = buf.char
    }

    companion object {
        fun sizeOf(): Int {
            return 4
        }
    }
}