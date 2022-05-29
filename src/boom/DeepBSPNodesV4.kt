package boom


import utils.GenericCopy
import utils.GenericCopy.malloc
import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class DeepBSPNodesV4 : CacheableDoomObject {
    var header = ByteArray(8)
    lateinit var nodes: Array<mapnode_v4_t>
    var numnodes = 0
    fun formatOK(): Boolean {
        return Arrays.equals(header, DeepBSPNodesV4.DeepBSPHeader)
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        val length = buf.capacity()

        // Too short, not even header.
        if (length < 8) {
            return
        }
        numnodes = (length - 8) / mapnode_v4_t.sizeOf()
        if (length < 1) {
            return
        }
        buf[header] // read header
        nodes = malloc({ mapnode_v4_t() }, length)
        for (i in 0 until length) {
            nodes[i].unpack(buf)
        }
    }

    companion object {
        val DeepBSPHeader = byteArrayOf(
            'x'.code.toByte(), 'N'.code.toByte(), 'd'.code.toByte(), '4'.code.toByte(), 0, 0, 0, 0
        )
    }
}