package rr

import m.fixed_t.Companion.FRACBITS
import p.Resettable
import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** This is the vertex structure used IN MEMORY with fixed-point arithmetic.
 * It's DIFFERENT than the one used on disk, which has 16-bit signed shorts.
 * However, it must be parsed.
 *
 */
open class vertex_t : CacheableDoomObject, Resettable {
    /** treat as (fixed_t)  */
    var x = 0
    var y = 0

    /** Notice how we auto-expand to fixed_t  */
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        x = buf.short.toInt() shl FRACBITS
        y = buf.short.toInt() shl FRACBITS
    }

    override fun reset() {
        x = 0
        y = 0
    }

    companion object {
        fun sizeOf(): Int {
            return 4
        }
    }
}