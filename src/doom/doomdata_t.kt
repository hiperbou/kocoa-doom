package doom


import data.Defines
import utils.GenericCopy
import w.DoomBuffer
import java.nio.ByteBuffer

class doomdata_t : IDatagramSerializable {
    // High bit is retransmit request.
    /** MAES: was "unsigned"  */
    var checksum = 0
    /*
	 * CAREFUL!!! Those "bytes" are actually unsigned
	 */
    /** Only valid if NCMD_RETRANSMIT.  */
    var retransmitfrom: Byte = 0
    var starttic: Byte = 0
    var player: Byte = 0
    var numtics: Byte = 0
    var cmds: Array<ticcmd_t>

    // Used for datagram serialization.
    private val buffer: ByteArray
    private val bbuf: ByteBuffer
    override fun pack(): ByteArray {
        bbuf.rewind()

        // Why making it harder?
        bbuf.putInt(checksum)
        bbuf.put(retransmitfrom)
        bbuf.put(starttic)
        bbuf.put(player)
        bbuf.put(numtics)

        // FIXME: it's probably more efficient to use System.arraycopy ? 
        // Or are the packets too small anyway? At most we'll be sending "doomdata_t's"
        for (i in cmds.indices) {
            bbuf.put(cmds[i].pack())
        }
        return bbuf.array()
    }

    override fun pack(buf: ByteArray, offset: Int) {
        // No need to make it harder...just pack it and slap it in.
        val tmp = this.pack()
        System.arraycopy(tmp, 0, buf, offset, tmp.size)
    }

    override fun unpack(buf: ByteArray) {
        unpack(buf, 0)
    }

    override fun unpack(buf: ByteArray, offset: Int) {
        var offset = offset
        checksum = DoomBuffer.getBEInt(buf)
        offset = +4
        retransmitfrom = buf[offset++]
        starttic = buf[offset++]
        player = buf[offset++]
        numtics = buf[offset++]
        for (i in cmds.indices) {
            cmds[i].unpack(buf, offset)
            offset += ticcmd_t.TICCMDLEN
        }
    }

    fun selfUnpack() {
        unpack(buffer)
    }

    fun copyFrom(source: doomdata_t) {
        checksum = source.checksum
        numtics = source.numtics
        player = source.player
        retransmitfrom = source.retransmitfrom
        starttic = source.starttic

        // MAES: this was buggy as hell, and didn't work at all, which
        // in turn prevented other subsystems such as speed throttling and
        // networking to work.
        //
        // This should be enough to alter the ByteBuffer too.
        // System.arraycopy(source.cached(), 0, this.buffer, 0, DOOMDATALEN);
        // This should set all fields
        // selfUnpack();
    }

    override fun cached(): ByteArray {
        return buffer
    }

    var sb = StringBuilder()

    init {
        cmds = GenericCopy.malloc({ ticcmd_t() }, Defines.BACKUPTICS)
        // Enough space for its own header + the ticcmds;
        buffer = ByteArray(doomdata_t.DOOMDATALEN)
        // This "pegs" the ByteBuffer to this particular array.
        // Separate updates are not necessary.
        bbuf = ByteBuffer.wrap(buffer)
    }

    override fun toString(): String {
        sb.setLength(0)
        sb.append("doomdata_t ")
        sb.append(retransmitfrom.toInt())
        sb.append(" starttic ")
        sb.append(starttic.toInt())
        sb.append(" player ")
        sb.append(player.toInt())
        sb.append(" numtics ")
        sb.append(numtics.toInt())
        return sb.toString()
    }

    companion object {
        val DOOMDATALEN: Int = 8 + Defines.BACKUPTICS * ticcmd_t.TICCMDLEN
    }
}