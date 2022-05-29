package data


import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This is the structure of a map vertex ON DISK: in memory it gets shifted and
 * expanded to fixed_t. Also, on disk it only exists as part of the VERTEXES
 * lump: it is not individually cacheable, even though it implements
 * CacheableDoomObject.
 */
class mapvertex_t /*@JvmOverloads*/ constructor(var x: Short = 0.toShort(), var y: Short = 0.toShort()) :
    CacheableDoomObject {
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        x = buf.short
        y = buf.short
    }

    companion object {
        fun sizeOf(): Int {
            return 4
        }
    }
}