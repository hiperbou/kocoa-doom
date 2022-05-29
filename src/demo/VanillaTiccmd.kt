package demo


import doom.ticcmd_t
import utils.C2JUtils
import w.CacheableDoomObject
import w.IWritableDoomObject
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/** A more lightweight version of ticcmd_t, which contains
 * too much crap and redundant data. In order to keep demo
 * loading and recording easier, this class contains only the
 * necessary stuff to read/write from/to disk during VANILLA
 * demos. It can be converted from/to ticcmd_t, if needed.
 *
 * @author admin
 */
class VanillaTiccmd : CacheableDoomObject, IDemoTicCmd, IWritableDoomObject {
    /** *2048 for move  */
    var forwardmove: Byte = 0

    /** *2048 for move  */
    var sidemove: Byte = 0

    /** <<16 for angle delta  */
    var angleturn: Byte = 0
    var buttons: Byte = 0

    /** Special note: the only occasion where we'd ever be interested
     * in reading ticcmd_t's from a lump is when playing back demos.
     * Therefore, we use this specialized reading method which does NOT,
     * I repeat, DOES NOT set all fields and some are read differently.
     * NOT 1:1 intercangeable with the Datagram methods!
     *
     */
    @Throws(IOException::class)
    override fun unpack(f: ByteBuffer) {

        // MAES: the original ID code for reference.
        // demo_p++ is a pointer inside a raw byte buffer.

        //cmd->forwardmove = ((signed char)*demo_p++); 
        //cmd->sidemove = ((signed char)*demo_p++); 
        //cmd->angleturn = ((unsigned char)*demo_p++)<<8; 
        //cmd->buttons = (unsigned char)*demo_p++; 
        forwardmove = f.get()
        sidemove = f.get()
        // Even if they use the "unsigned char" syntax, angleturn is signed.
        angleturn = f.get()
        buttons = f.get()
    }

    /** Ditto, we only pack some of the fields.
     *
     * @param f
     * @throws IOException
     */
    @Throws(IOException::class)
    fun pack(f: ByteBuffer) {
        f.put(forwardmove)
        f.put(sidemove)
        f.put(angleturn)
        f.put(buttons)
    }

    override fun toString(): String {
        VanillaTiccmd.sb.setLength(0)
        VanillaTiccmd.sb.append(" forwardmove ")
        VanillaTiccmd.sb.append(forwardmove.toInt())
        VanillaTiccmd.sb.append(" sidemove ")
        VanillaTiccmd.sb.append(sidemove.toInt())
        VanillaTiccmd.sb.append(" angleturn ")
        VanillaTiccmd.sb.append(angleturn.toInt())
        VanillaTiccmd.sb.append(" buttons ")
        VanillaTiccmd.sb.append(Integer.toHexString(buttons.toInt()))
        return VanillaTiccmd.sb.toString()
    }

    override fun decode(dest: ticcmd_t) {
        // Decode
        dest.forwardmove = forwardmove
        dest.sidemove = sidemove
        dest.angleturn = (angleturn.toInt() shl 8).toShort()
        dest.buttons = C2JUtils.toUnsignedByte(buttons).toChar()
    }

    override fun encode(source: ticcmd_t) {
        // Note: we can get away with a simple copy because
        // Demoticcmds have already been "decoded".
        forwardmove = source.forwardmove
        sidemove = source.sidemove
        // OUCH!!! NASTY PRECISION REDUCTION... but it's the
        // One True Vanilla way.
        angleturn = (source.angleturn.toInt() ushr 8).toByte()
        buttons = (source.buttons.code and 0x00FF).toByte()
    }

    @Throws(IOException::class)
    override fun write(f: DataOutputStream) {
        f.writeByte(forwardmove.toInt())
        f.writeByte(sidemove.toInt())
        f.writeByte(angleturn.toInt())
        f.writeByte(buttons.toInt())
    }

    companion object {
        private val sb = StringBuilder()
    }
}