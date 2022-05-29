package boom


import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class mapsubsector_v4_t : CacheableDoomObject {
    var numsegs = 0.toChar()

    /** Index of first one, segs are stored sequentially.  */
    var firstseg = 0
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        numsegs = buf.char
        firstseg = buf.int
    }

    companion object {
        fun sizeOf(): Int {
            return 6
        }
    }
}