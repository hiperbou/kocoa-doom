package p


import rr.SectorAction
import w.DoomIO
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer

class floormove_t : SectorAction(), IReadableDoomObject {
    var type: floor_e
    var crush = false
    var direction = 0
    var newspecial = 0
    var texture: Short = 0

    /** fixed_t  */
    var floordestheight = 0

    /** fixed_t  */
    var speed = 0

    init {
        // MAES HACK: floors are implied to be at least of "lowerFloor" type
        // unless set otherwise, due to implicit zero-enum value.
        type = floor_e.lowerFloor
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        super.read(f) // Call thinker reader first            
        type = floor_e.values()[DoomIO.readLEInt(f)]
        crush = DoomIO.readIntBoolean(f)
        super.sectorid = DoomIO.readLEInt(f) // Sector index (or pointer?)
        direction = DoomIO.readLEInt(f)
        newspecial = DoomIO.readLEInt(f)
        texture = DoomIO.readLEShort(f)
        floordestheight = DoomIO.readLEInt(f)
        speed = DoomIO.readLEInt(f)
    }

    @Throws(IOException::class)
    override fun pack(b: ByteBuffer) {
        super.pack(b) //12            
        b.putInt(type.ordinal) // 16
        b.putInt(if (crush) 1 else 0) //20
        b.putInt(super.sectorid) // 24
        b.putInt(direction) // 28
        b.putInt(newspecial) // 32
        b.putShort(texture) // 34
        b.putInt(floordestheight) // 38
        b.putInt(speed) // 42
    }
}