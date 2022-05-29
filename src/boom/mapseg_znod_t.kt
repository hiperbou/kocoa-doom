package boom


import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** ZDoom node?
 *
 */
class mapseg_znod_t : CacheableDoomObject {
    var v1 = 0
    var v2 // Those are unsigned :-/
            = 0
    var linedef = 0.toChar()
    var side: Byte = 0
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        v1 = buf.int
        v2 = buf.int
        linedef = buf.char
        side = buf.get()
    }

    companion object {
        fun sizeOf(): Int {
            return 11
        }
    }
}