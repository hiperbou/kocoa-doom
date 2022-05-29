package p


import doom.*
import rr.SectorAction
import w.CacheableDoomObject
import w.IPackableDoomObject
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ceiling_t : SectorAction(), CacheableDoomObject, IReadableDoomObject, IPackableDoomObject {
    var type: ceiling_e

    @SourceCode.fixed_t
    var bottomheight = 0

    @SourceCode.fixed_t
    var topheight = 0

    @SourceCode.fixed_t
    var speed = 0
    var crush = false

    // 1 = up, 0 = waiting, -1 = down
    var direction = 0

    // ID
    var tag = 0
    var olddirection = 0
    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        // Read 48 bytes.
        ceiling_t.readbuffer.position(0)
        ceiling_t.readbuffer.order(ByteOrder.LITTLE_ENDIAN)
        f.read(ceiling_t.readbuffer.array(), 0, 48)
        unpack(ceiling_t.readbuffer)
    }

    @Throws(IOException::class)
    override fun pack(b: ByteBuffer) {
        b.order(ByteOrder.LITTLE_ENDIAN)
        super.pack(b) //12            
        b.putInt(type.ordinal) // 16            
        b.putInt(super.sectorid) // 20
        b.putInt(bottomheight)
        b.putInt(topheight) // 28
        b.putInt(speed)
        b.putInt(if (crush) 1 else 0)
        b.putInt(direction) // 40
        b.putInt(tag)
        b.putInt(olddirection) //48
    }

    @Throws(IOException::class)
    override fun unpack(b: ByteBuffer) {
        b.order(ByteOrder.LITTLE_ENDIAN)
        super.unpack(b) // Call thinker reader first
        type = ceiling_t.values.get(b.int)
        super.sectorid = b.int // sector pointer.
        bottomheight = b.int
        topheight = b.int
        speed = b.int
        crush = b.int != 0
        direction = b.int
        tag = b.int
        olddirection = b.int
    }

    init {
        // Set to the smallest ordinal type.
        type = ceiling_e.lowerToFloor
    }

    companion object {
        // HACK for speed.
        val values = ceiling_e.values()
        private val readbuffer = ByteBuffer.allocate(48)
    }
}