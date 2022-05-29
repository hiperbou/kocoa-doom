package p


import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

//
// P_SWITCH
//
class switchlist_t : CacheableDoomObject {
    constructor() {}

    // Were char[9]
    var name1: String? = null
    var name2: String? = null
    var episode: Short = 0

    constructor(name1: String?, name2: String?, episode: Int) : super() {
        this.name1 = name1
        this.name2 = name2
        this.episode = episode.toShort()
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        // Like most Doom structs...
        buf.order(ByteOrder.LITTLE_ENDIAN)
        name1 = DoomBuffer.getNullTerminatedString(buf, 9)
        name2 = DoomBuffer.getNullTerminatedString(buf, 9)
        episode = buf.short
    }

    override fun toString(): String {
        return String.format("%s %s %d", name1, name2, episode)
    }

    companion object {
        fun size(): Int {
            return 20
        }
    }
}