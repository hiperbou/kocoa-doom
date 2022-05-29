package rr

import utils.GenericCopy
import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Texture definition.
 * A DOOM wall texture is a list of patches which are to be combined in a predefined order.
 * This is the ON-DISK structure, to be read from the TEXTURES1 and TEXTURES2 lumps.
 * In memory, this becomes texture_t.
 *
 * @author MAES
 */
class maptexture_t : CacheableDoomObject {
    var name: String? = null
    var masked = false
    var width // was signed byte
            : Short = 0
    var height // was 
            : Short = 0

    //void**t        columndirectory;  // OBSOLETE (yeah, but we must read a dummy integer here)
    var patchcount: Short = 0
    lateinit var patches: Array<mappatch_t>
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        name = DoomBuffer.getNullTerminatedString(buf, 8)
        masked = buf.int != 0
        width = buf.short
        height = buf.short
        buf.int // read a dummy integer for obsolete columndirectory.
        patchcount = buf.short

        // Simple sanity check. Do not attempt reading more patches than there
        // are left in the TEXTURE lump.
        patchcount =
            Math.min(patchcount.toInt(), (buf.capacity() - buf.position()) / mappatch_t.size()).toShort()
        patches = GenericCopy.malloc({ mappatch_t() }, patchcount.toInt())
        DoomBuffer.readObjectArray(buf, patches as Array<CacheableDoomObject>, patchcount.toInt())
    }
}