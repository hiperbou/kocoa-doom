package boom


import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class mapnode_znod_t : CacheableDoomObject {
    var x // Partition line from (x,y) to x+dx,y+dy)
            : Short = 0
    var y: Short = 0
    var dx: Short = 0
    var dy: Short = 0

    // Bounding box for each child, clip against view frustum.
    var bbox: Array<ShortArray>

    // If NF_SUBSECTOR its a subsector, else it's a node of another subtree.
    var children: IntArray

    init {
        bbox = Array(2) { ShortArray(4) }
        children = IntArray(2)
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        x = buf.short
        y = buf.short
        dx = buf.short
        dy = buf.short
        DoomBuffer.readShortArray(buf, bbox[0], 4)
        DoomBuffer.readShortArray(buf, bbox[1], 4)
        DoomBuffer.readIntArray(buf, children, 2)
    }

    companion object {
        fun sizeOf(): Int {
            return 8 + 16 + 8
        }
    }
}