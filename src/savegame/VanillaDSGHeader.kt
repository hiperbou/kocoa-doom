package savegame


import data.Defines
import data.Limits
import defines.skill_t
import utils.C2JUtils
import w.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer

/** The header of a vanilla savegame.
 *
 * It contains a fixed-length, null-terminated string of 24 bytes max, in any case.
 * Then a 16-byte "version string", which normally reads "version 109".
 * Then bytes that record:
 * skill +1
 * episode +1
 * map +1
 * players in game +4
 * gametime +3 (as 24-bit big-endian)
 *
 * So the header has an total size of *drum roll* 50 bytes.
 *
 *
 * @author admin
 */
class VanillaDSGHeader : IDoomSaveGameHeader, IReadableDoomObject, IWritableDoomObject, CacheableDoomObject {
    var _name // max size SAVEGAMENAME
            : String? = null
    var vcheck: String? = null

    // These are for DS
    var _gameskill: skill_t? = null
    var _gameepisode = 0
    var _gamemap = 0
    var _playeringame: BooleanArray

    /** what bullshit, stored as 24-bit integer?!  */
    var _leveltime = 0

    // These help checking shit.
    var _wrongversion = false
    var properend = false

    init {
        _playeringame = BooleanArray(Limits.MAXPLAYERS)
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        _name = DoomBuffer.getNullTerminatedString(buf, Limits.SAVESTRINGSIZE)
        vcheck = DoomBuffer.getNullTerminatedString(buf, Limits.VERSIONSIZE)
        val vcheckb = "version " + Defines.VERSION
        // no more unpacking, and report it.
        if (!vcheckb.equals(vcheck, ignoreCase = true).also { _wrongversion = it }) return
        _gameskill = skill_t.values()[buf.get().toInt()]
        _gameepisode = buf.get().toInt()
        _gamemap = buf.get().toInt()
        for (i in 0 until Limits.MAXPLAYERS) _playeringame[i] = buf.get().toInt() != 0

        // load a base level (this doesn't advance the pointer?) 
        //G_InitNew (gameskill, gameepisode, gamemap); 

        // get the times 
        val a = C2JUtils.toUnsignedByte(buf.get())
        val b = C2JUtils.toUnsignedByte(buf.get())
        val c = C2JUtils.toUnsignedByte(buf.get())
        // Quite anomalous, leveltime is stored as a BIG ENDIAN, 24-bit unsigned integer :-S
        _leveltime = a shl 16 or (b shl 8) or c

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
        DoomIO.writeString(f, _name, Limits.SAVESTRINGSIZE)
        DoomIO.writeString(f, vcheck, Limits.VERSIONSIZE)
        f.writeByte(_gameskill!!.ordinal)
        f.writeByte(_gameepisode)
        f.writeByte(_gamemap)
        for (i in 0 until Limits.MAXPLAYERS) f.writeBoolean(_playeringame[i])

        // load a base level (this doesn't advance the pointer?) 
        //G_InitNew (gameskill, gameepisode, gamemap); 

        // get the times 
        val a = (0x0000FF and (_leveltime shr 16)).toByte()
        val b = (0x00FF and (_leveltime shr 8)).toByte()
        val c = (0x00FF and _leveltime).toByte()
        // Quite anomalous, leveltime is stored as a BIG ENDIAN, 24-bit unsigned integer :-S
        f.writeByte(a.toInt())
        f.writeByte(b.toInt())
        f.writeByte(c.toInt())

        // The end. This is actually just the header, so we don't "end" here just yet.
        // f.writeByte(0x1d);
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        _name = DoomIO.readNullTerminatedString(f, Limits.SAVESTRINGSIZE)
        vcheck = DoomIO.readNullTerminatedString(f, Limits.VERSIONSIZE)
        _gameskill = skill_t.values()[f.readUnsignedByte()]
        _gameepisode = f.readByte().toInt()
        _gamemap = f.readByte().toInt()
        for (i in 0 until Limits.MAXPLAYERS) _playeringame[i] = f.readBoolean()

        // get the times 
        val a = f.readUnsignedByte()
        val b = f.readUnsignedByte()
        val c = f.readUnsignedByte()
        // Quite anomalous, leveltime is stored as a BIG ENDIAN, 24-bit unsigned integer :-S
        _leveltime = a shl 16 or (b shl 8) or c
    }

    ////////////////////////// NASTY GETTERS //////////////////////////////
    override fun getName(): String? {
        return _name
    }

    override fun setName(name: String?) {
        this._name = name
    }

    override fun getVersion(): String? {
        return vcheck
    }

    override fun setVersion(vcheck: String?) {
        this.vcheck = vcheck
    }

    override fun getGameskill(): skill_t? {
        return _gameskill
    }

    override fun setGameskill(gameskill: skill_t?) {
        this._gameskill = gameskill
    }

    override fun getGameepisode(): Int {
        return _gameepisode
    }

    override fun setGameepisode(gameepisode: Int) {
        this._gameepisode = gameepisode
    }

    override fun getGamemap(): Int {
        return _gamemap
    }

    override fun setGamemap(gamemap: Int) {
        this._gamemap = gamemap
    }

    override fun getPlayeringame(): BooleanArray {
        return _playeringame
    }

    override fun setPlayeringame(playeringame: BooleanArray) {
        this._playeringame = playeringame
    }

    override fun getLeveltime(): Int {
        return _leveltime
    }

    override fun setLeveltime(leveltime: Int) {
        this._leveltime = leveltime
    }

    override fun isWrongversion(): Boolean {
        return _wrongversion
    }

    override fun setWrongversion(wrongversion: Boolean) {
        this._wrongversion = wrongversion
    }

    override fun isProperend(): Boolean {
        return properend
    }
}