package data


import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A LineDef, as used for editing, and as input to the BSP builder.
 */
class maplinedef_t : CacheableDoomObject {
    var v1 = 0.toChar()
    var v2 = 0.toChar()
    var flags: Short = 0
    var special: Short = 0
    var tag: Short = 0

    /** sidenum[1] will be 0xFFFF if one sided  */
    var sidenum: CharArray

    init {
        sidenum = CharArray(2)
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        v1 = buf.char
        v2 = buf.char
        flags = buf.short
        special = buf.short
        tag = buf.short
        DoomBuffer.readCharArray(buf, sidenum, 2)
    }

    companion object {
        fun sizeOf(): Int {
            return 14
        }
    }
}