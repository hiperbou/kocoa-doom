package boom


import utils.GenericCopy
import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class ZNodeSegs : CacheableDoomObject {
    lateinit var header: ByteArray
    lateinit var nodes: Array<mapseg_znod_t>
    var numnodes = 0
    fun formatOK(): Boolean {
        return Arrays.equals(header, ZNodeSegs.DeepBSPHeader)
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
        nodes = GenericCopy.malloc({ mapseg_znod_t() }, length)
        for (i in 0 until length) {
            nodes[i].unpack(buf)
        }
    }

    companion object {
        private val DeepBSPHeader = byteArrayOf(
            'x'.code.toByte(), 'N'.code.toByte(), 'd'.code.toByte(), '4'.code.toByte(), 0, 0, 0, 0
        )
    }
}