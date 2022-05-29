package g

import data.Defines
import data.Limits
import defines.skill_t
import doom.DoomStatus
import utils.C2JUtils
import w.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/** represents the header of Doom savegame, so that basic info can be checked quickly.
 *
 * To load the whole game and check if there are final mistakes, you must go through it all.
 * Savegames need to be aware of ALL status and context, so maybe they should be inner classes?
 *
 */
class DoomSaveGame : CacheableDoomObject, IReadableDoomObject, IWritableDoomObject {
    var name // max size SAVEGAMENAME
            : String? = null
    var vcheck: String? = null

    // These are for DS
    var gameskill = 0
    var gameepisode = 0
    var gamemap = 0
    var playeringame: BooleanArray

    /** what bullshit, stored as 24-bit integer?!  */
    var leveltime = 0

    // These help checking shit.
    var wrongversion = false
    var properend = false

    init {
        playeringame = BooleanArray(Limits.MAXPLAYERS)
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        name = DoomBuffer.getNullTerminatedString(buf, Limits.SAVESTRINGSIZE)
        vcheck = DoomBuffer.getNullTerminatedString(buf, Limits.VERSIONSIZE)
        val vcheckb = "version " + Defines.VERSION
        // no more unpacking, and report it.
        if (!vcheckb.equals(vcheck, ignoreCase = true).also { wrongversion = it }) return
        gameskill = buf.get().toInt()
        gameepisode = buf.get().toInt()
        gamemap = buf.get().toInt()
        for (i in 0 until Limits.MAXPLAYERS) playeringame[i] = buf.get().toInt() != 0

        // load a base level (this doesn't advance the pointer?) 
        //G_InitNew (gameskill, gameepisode, gamemap); 

        // get the times 
        val a = C2JUtils.toUnsignedByte(buf.get())
        val b = C2JUtils.toUnsignedByte(buf.get())
        val c = C2JUtils.toUnsignedByte(buf.get())
        // Quite anomalous, leveltime is stored as a BIG ENDIAN, 24-bit unsigned integer :-S
        leveltime = (a shl 16) + (b shl 8) + c

        // Mark this position...
        buf.mark()
        buf.position(buf.limit() - 1)
        properend = if (buf.get().toInt() != 0x1d) false else true
        buf.reset()

        // We've loaded whatever consistutes "header" info, the rest must be unpacked by proper
        // methods in the game engine itself.
    }

    @Throws(IOException::class)
    override fun write(f: DataOutputStream) {
        DoomIO.writeString(f, name, Limits.SAVESTRINGSIZE)
        DoomIO.writeString(f, vcheck, Limits.VERSIONSIZE)
        f.writeByte(gameskill)
        f.writeByte(gameepisode)
        f.writeByte(gamemap)
        for (i in 0 until Limits.MAXPLAYERS) f.writeBoolean(playeringame[i])

        // load a base level (this doesn't advance the pointer?) 
        //G_InitNew (gameskill, gameepisode, gamemap); 

        // get the times 
        val a = (0x0000FF and (leveltime ushr 16)).toByte()
        val b = (0x00FF and (leveltime ushr 8)).toByte()
        val c = (0x00FF and leveltime).toByte()
        // Quite anomalous, leveltime is stored as a BIG ENDIAN, 24-bit unsigned integer :-S
        f.writeByte(a.toInt())
        f.writeByte(b.toInt())
        f.writeByte(c.toInt())

        // TODO: after this point, we should probably save some packed buffers representing raw state...
        // needs further study.

        // The end.
        f.writeByte(0x1d)
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        name = DoomIO.readNullTerminatedString(f, Limits.SAVESTRINGSIZE)
        vcheck = DoomIO.readNullTerminatedString(f, Limits.VERSIONSIZE)
        val vcheckb = "version " + Defines.VERSION
        // no more unpacking, and report it.
        if (!vcheckb.equals(vcheck, ignoreCase = true).also { wrongversion = it }) return
        gameskill = f.readByte().toInt()
        gameepisode = f.readByte().toInt()
        gamemap = f.readByte().toInt()
        playeringame = BooleanArray(Limits.MAXPLAYERS)
        for (i in 0 until Limits.MAXPLAYERS) playeringame[i] = f.readBoolean()

        // load a base level (this doesn't advance the pointer?) 
        //G_InitNew (gameskill, gameepisode, gamemap); 

        // get the times 
        val a = f.readUnsignedByte()
        val b = f.readUnsignedByte()
        val c = f.readUnsignedByte()
        // Quite anomalous, leveltime is stored as a BIG ENDIAN, 24-bit unsigned integer :-S
        leveltime = (a shl 16) + (b shl 8) + c

        // Mark this position...
        //long mark=f.getFilePointer();
        //f.seek(f.length()-1);
        //if (f.readByte() != 0x1d) properend=false; else
        //    properend=true;
        //f.seek(mark);
        val available = f.available().toLong()
        f.skip(available - 1)
        properend = if (f.readByte().toInt() != 0x1d) false else true

        // We've loaded whatever consistutes "header" info, the rest must be unpacked by proper
        // methods in the game engine itself.
    }

    fun toStat(DS: DoomStatus<*, *>) {
        System.arraycopy(playeringame, 0, DS.playeringame, 0, playeringame.size)
        DS.gameskill = skill_t.values()[gameskill]
        DS.gameepisode = gameepisode
        DS.gamemap = gamemap
        DS.leveltime = leveltime
    }

    fun fromStat(DS: DoomStatus<*, *>) {
        System.arraycopy(DS.playeringame, 0, playeringame, 0, DS.playeringame.size)
        gameskill = DS.gameskill!!.ordinal
        gameepisode = DS.gameepisode
        gamemap = DS.gamemap
        leveltime = DS.leveltime
    }
}