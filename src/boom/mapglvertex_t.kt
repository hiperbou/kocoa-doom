package boom


import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** fixed 32 bit gl_vert format v2.0+ (glBsp 1.91)  */
class mapglvertex_t : CacheableDoomObject {
    var x = 0
    var y // fixed_t
            = 0

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        x = buf.int
        y = buf.int
    }

    companion object {
        fun sizeOf(): Int {
            return 8
        }
    }
}