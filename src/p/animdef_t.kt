package p


import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Source animation definition. Made readable for compatibility with Boom's
 * SWANTBLS system.
 *
 * @author velktron
 */
class animdef_t : CacheableDoomObject {
    constructor() {}
    constructor(
        istexture: Boolean, endname: String?, startname: String?,
        speed: Int
    ) : super() {
        this.istexture = istexture
        this.endname = endname
        this.startname = startname
        this.speed = speed
    }

    /** if false, it is a flat, and will NOT be used as a texture. Unless you
     * use "flats on walls functionality of course.  */
    var istexture = false

    /** The END name and START name of a texture, given in this order when reading a lump
     * The animation system is agnostic to the actual names of of the "in-between"
     * frames, it's purely pointer based, and only the start/end are constant. It only
     * counts the actual number of existing textures during initialization time.
     *
     */
    var endname: String? = null
    var startname: String? = null
    var speed = 0
    override fun toString(): String {
        return String.format(
            "%s %s %s %d", istexture, startname, endname,
            speed
        )
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        // Like most Doom structs...
        buf.order(ByteOrder.LITTLE_ENDIAN)
        istexture = buf.get().toInt() != 0
        startname = DoomBuffer.getNullTerminatedString(buf, 9)
        endname = DoomBuffer.getNullTerminatedString(buf, 9)
        speed = buf.int
    }

    companion object {
        fun size(): Int {
            return 23
        }
    }
}