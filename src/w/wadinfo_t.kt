package w

import java.io.DataInputStream
import java.io.IOException

class wadinfo_t : IReadableDoomObject {
    // Should be "IWAD" or "PWAD".
    var identification: String? = null
    var numlumps: Long = 0
    var infotableofs: Long = 0

    /** Reads the wadinfo_t from the file. */
    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        identification = DoomIO.readString(f, 4)
        numlumps = DoomIO.readUnsignedLEInt(f)
        infotableofs = DoomIO.readUnsignedLEInt(f)
    }
}