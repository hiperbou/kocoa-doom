package w

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class wadheader_t : IReadableDoomObject, IWritableDoomObject {
    var type: String? = null
    var numentries = 0
    var tablepos = 0
    var big_endian = false
    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        type = DoomIO.readNullTerminatedString(f, 4)
        if (!big_endian) {
            numentries = DoomIO.readUnsignedLEInt(f).toInt()
            tablepos = DoomIO.readUnsignedLEInt(f).toInt()
        } else {
            numentries = f.readInt()
            tablepos = f.readInt()
        }
    }

    @Throws(IOException::class)
    override fun write(dos: DataOutputStream) {
        DoomIO.writeString(dos, type, 4)
        if (!big_endian) {
            DoomIO.writeLEInt(dos, numentries)
            DoomIO.writeLEInt(dos, tablepos)
        } else {
            dos.writeInt(numentries)
            dos.writeInt(tablepos)
        }
    }

    companion object {
        fun sizeof(): Int {
            return 16
        }
    }
}