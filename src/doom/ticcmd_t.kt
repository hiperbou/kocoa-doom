package doom


import w.CacheableDoomObject
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ticcmd_t : IDatagramSerializable, IReadableDoomObject, CacheableDoomObject {
    /** *2048 for move  */
    var forwardmove: Byte = 0

    /** *2048 for move  */
    var sidemove: Byte = 0

    /** <<16 for angle delta  */
    var angleturn: Short = 0

    /** checks for net game  */
    var consistancy: Short = 0

    /** MAES: these are unsigned bytes :-(
     * However over networks, if we wish for vanilla compatibility,
     * these must be reduced to 8-bit "chars"
     */
    var chatchar = 0.toChar()
    var buttons = 0.toChar()

    /** HERETIC look/fly up/down/centering  */
    var lookfly = 0.toChar()
    // TODO: will be ignored during vanilla demos. Consider using specialized
    // per-demotype readers instead of Cacheable/Unpackage.
    /** replaces G_CmdChecksum (ticcmd_t cmd)  */ /////////////////////////////////////////////
    // For datagram serialization
    private val buffer: ByteArray
    fun getChecksum(cmd: ticcmd_t?): Int {
        var sum = 0
        sum += forwardmove.toInt()
        sum += sidemove.toInt()
        sum += angleturn.toInt()
        sum += consistancy.toInt()
        sum += chatchar.code
        sum += buttons.code
        return sum
    }

    /** because Cloneable is bullshit  */
    fun copyTo(dest: ticcmd_t) {
        dest.forwardmove = forwardmove
        dest.sidemove = sidemove
        dest.angleturn = angleturn
        dest.consistancy = consistancy
        dest.chatchar = chatchar
        dest.buttons = buttons
        dest.lookfly = lookfly
    }

    override fun toString(): String {
        ticcmd_t.sb.setLength(0)
        ticcmd_t.sb.append(" forwardmove ")
        ticcmd_t.sb.append(Integer.toHexString(forwardmove.toInt()))
        ticcmd_t.sb.append(" sidemove ")
        ticcmd_t.sb.append(Integer.toHexString(sidemove.toInt()))
        ticcmd_t.sb.append(" angleturn ")
        ticcmd_t.sb.append(Integer.toHexString(angleturn.toInt()))
        ticcmd_t.sb.append(" consistancy ")
        ticcmd_t.sb.append(Integer.toHexString(consistancy.toInt()))
        ticcmd_t.sb.append(" chatchar ")
        ticcmd_t.sb.append(chatchar)
        ticcmd_t.sb.append(" buttons ")
        ticcmd_t.sb.append(Integer.toHexString(buttons.code))
        return ticcmd_t.sb.toString()
    }

    override fun pack(): ByteArray {
        buffer[0] = forwardmove
        buffer[1] = sidemove
        buffer[2] = (angleturn.toInt() ushr 8).toByte()
        buffer[3] = (angleturn.toInt() and 0x00FF).toByte()
        buffer[4] = (consistancy.toInt() ushr 8).toByte()
        buffer[5] = (consistancy.toInt() and 0x00FF).toByte()

        // We only send 8 bytes because the original length was 8 bytes.
        buffer[6] = (chatchar.code and 0x00FF).toByte()
        buffer[7] = (buttons.code and 0x00FF).toByte()
        return buffer
    }

    override fun pack(buf: ByteArray, offset: Int) {
        buf[0 + offset] = forwardmove
        buf[1 + offset] = sidemove
        buf[2 + offset] = (angleturn.toInt() ushr 8).toByte()
        buf[3 + offset] = (angleturn.toInt() and 0x00FF).toByte()
        buf[4 + offset] = (consistancy.toInt() ushr 8).toByte()
        buf[5 + offset] = (consistancy.toInt() and 0x00FF).toByte()

        // We only send 8 bytes because the original length was 8 bytes.
        buf[6 + offset] = (chatchar.code and 0x00FF).toByte()
        buf[7 + offset] = (buttons.code and 0x00FF).toByte()
    }

    override fun unpack(buf: ByteArray) {
        unpack(buf, 0)
    }

    override fun unpack(buf: ByteArray, offset: Int) {
        forwardmove = buf[0 + offset]
        sidemove = buf[1 + offset]
        angleturn = (buf[2 + offset].toInt() shl 8 or buf[3 + offset].toInt()).toShort()
        consistancy = (buf[4 + offset].toInt() shl 8 or buf[5 + offset].toInt()).toShort()
        // We blow these up to full chars.
        chatchar = (0x00FF and buf[6 + offset].toInt()).toChar()
        buttons = (0x00FF and buf[7 + offset].toInt()).toChar()
    }

    override fun cached(): ByteArray {
        return buffer
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        ticcmd_t.iobuffer.position(0)
        ticcmd_t.iobuffer.order(ByteOrder.LITTLE_ENDIAN)
        f.read(ticcmd_t.iobuffer.array())
        unpack(ticcmd_t.iobuffer)
    }

    /** This is useful only when loading/saving players from savegames.
     * It's NOT interchangeable with datagram methods, because it
     * does not use the network byte order.
     */
    @Throws(IOException::class)
    override fun unpack(f: ByteBuffer) {
        f.order(ByteOrder.LITTLE_ENDIAN)
        forwardmove = f.get()
        sidemove = f.get()
        // Even if they use the "unsigned char" syntax, angleturn is signed.
        angleturn = f.short
        consistancy = f.short
        // We blow these up to full chars.
        chatchar = Char(f.get().toUShort())
        buttons = Char(f.get().toUShort())
    }

    /** Ditto, we only pack some of the fields.
     *
     * @param f
     * @throws IOException
     */
    @Throws(IOException::class)
    fun pack(f: ByteBuffer) {
        f.order(ByteOrder.LITTLE_ENDIAN)
        f.put(forwardmove)
        f.put(sidemove)
        // LE order on disk for vanilla compatibility.
        f.putShort(angleturn)
        f.putShort(consistancy)
        // We crimp these to bytes :-(
        f.put(chatchar.code.toByte())
        f.put(buttons.code.toByte())
    }

    // Initializes ticcmd buffer, too.
    init {
        buffer = ByteArray(ticcmd_t.TICCMDLEN)
    }

    companion object {
        // The length datagrams are supposed to have, for full compatibility.
        const val TICCMDLEN = 8
        private val sb = StringBuilder()
        private val iobuffer = ByteBuffer.allocate(8)
    }
}