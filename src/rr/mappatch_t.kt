package rr

import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Texture definition.
 * Each texture is composed of one or more patches,
 * with patches being lumps stored in the WAD.
 * The lumps are referenced by number, and patched
 * into the rectangular texture space using origin
 * and possibly other attributes.
 */
class mappatch_t : CacheableDoomObject {
    var originx: Short = 0
    var originy: Short = 0
    var patch: Short = 0
    var stepdir: Short = 0
    var colormap: Short = 0
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        originx = buf.short
        originy = buf.short
        patch = buf.short
        stepdir = buf.short
        colormap = buf.short
    }

    companion object {
        fun size(): Int {
            return 10
        }
    }
}