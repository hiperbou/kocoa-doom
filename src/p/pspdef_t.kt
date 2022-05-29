package p


import data.state_t
import w.DoomIO
import w.IPackableDoomObject
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer

class pspdef_t : IReadableDoomObject, IPackableDoomObject {
    /** a NULL state means not active  */
    var state: state_t?
    var tics = 0

    /** fixed_t  */
    var sx = 0
    var sy = 0

    // When read from disk.
    var readstate = 0

    init {
        state = state_t()
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        //state=data.info.states[f.readLEInt()];
        readstate = DoomIO.readLEInt(f)
        tics = DoomIO.readLEInt(f)
        sx = DoomIO.readLEInt(f)
        sy = DoomIO.readLEInt(f)
    }

    @Throws(IOException::class)
    override fun pack(f: ByteBuffer) {
        if (state == null) f.putInt(0) else f.putInt(state!!.id)
        f.putInt(tics)
        f.putInt(sx)
        f.putInt(sy)
    }
}