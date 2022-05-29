package boom


import utils.C2JUtils
import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class mapsubsector_znod_t : CacheableDoomObject {
    var numsegs: Long = 0
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        numsegs = C2JUtils.unsigned(buf.int)
    }

    companion object {
        fun sizeOf(): Int {
            return 4
        }
    }
}