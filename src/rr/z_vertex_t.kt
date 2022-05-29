package rr

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class z_vertex_t : vertex_t() {
    /** Notice how we auto-expand to fixed_t  */
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