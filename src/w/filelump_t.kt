package w

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

/** filelumps are on-disk structures. lumpinfos are almost the same, but are memory only.
 *
 * @author Maes
 */
class filelump_t : IReadableDoomObject, IWritableDoomObject {
    var filepos: Long = 0
    var size // Is INT 32-bit in file!
            : Long = 0
    var name // Whatever appears inside the wadfile
            : String? = null
    var actualname // Sanitized name, e.g. after compression markers
            : String? = null
    var big_endian = false // E.g. Jaguar
    var compressed = false // Compressed lump
    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        // MAES: Byte Buffers actually make it convenient changing byte order on-the-fly.
        // But RandomAccessFiles (and inputsteams) don't :-S
        if (!big_endian) {
            filepos = DoomIO.readUnsignedLEInt(f)
            size = DoomIO.readUnsignedLEInt(f)
        } else {
            filepos = f.readInt().toLong()
            size = f.readInt().toLong()
        }

        // Names used in the reading subsystem should be upper case,
        // but check for compressed status first
        name = DoomIO.readNullTerminatedString(f, 8)
        val stuff = name!!.toCharArray()

        // It's a compressed lump
        if (stuff[0].code > 0x7F) {
            compressed = true
            stuff[0] = (stuff[0].code and 0x7F).toChar()
        }
        actualname = String(stuff).uppercase(Locale.getDefault())
    }

    @Throws(IOException::class)
    override fun write(dos: DataOutputStream) {
        if (!big_endian) {
            DoomIO.writeLEInt(dos, filepos.toInt())
            DoomIO.writeLEInt(dos, size.toInt())
        } else {
            dos.writeInt(filepos.toInt())
            dos.writeInt(size.toInt())
        }
        DoomIO.writeString(dos, name, 8)
    }

    companion object {
        fun sizeof(): Int {
            return 4 + 4 + 8
        }
    }
}