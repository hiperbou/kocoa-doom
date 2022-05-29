package data


import w.CacheableDoomObject
import w.IPackableDoomObject
import w.IWritableDoomObject
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** mapthing_t ... same on disk AND in memory, wow?!  */
class mapthing_t : CacheableDoomObject, IPackableDoomObject, IWritableDoomObject, Cloneable {
    var x: Short = 0
    var y: Short = 0
    var angle: Short = 0
    var type: Short = 0
    var options: Short = 0

    constructor() {}
    constructor(source: mapthing_t) {
        copyFrom(source)
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        x = buf.short
        y = buf.short
        angle = buf.short
        type = buf.short
        options = buf.short
    }

    fun copyFrom(source: mapthing_t) {
        x = source.x
        y = source.y
        angle = source.angle
        options = source.options
        type = source.type
    }

    @Throws(IOException::class)
    override fun write(f: DataOutputStream) {

        // More efficient, avoids duplicating code and
        // handles little endian better.
        mapthing_t.iobuffer.position(0)
        mapthing_t.iobuffer.order(ByteOrder.LITTLE_ENDIAN)
        pack(mapthing_t.iobuffer)
        f.write(mapthing_t.iobuffer.array())
    }

    override fun pack(b: ByteBuffer) {
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.putShort(x)
        b.putShort(y)
        b.putShort(angle)
        b.putShort(type)
        b.putShort(options)
    }

    companion object {
        fun sizeOf(): Int {
            return 10
        }

        private val iobuffer = ByteBuffer.allocate(10)
    }
}