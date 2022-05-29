package demo

import data.Limits
import defines.skill_t
import utils.GenericCopy
import w.CacheableDoomObject
import w.DoomBuffer
import w.DoomIO
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer


class VanillaDoomDemo : IDoomDemo, CacheableDoomObject {
    // This stuff is in the demo header, in the order it appears
    // However everything is byte-sized when read from disk or to memory.
    var _version = 0
    var _skill: skill_t? = null
    var _episode = 0
    var _map = 0
    var _deathmatch = false
    var _respawnparm = false
    var _fastparm = false
    var _nomonsters = false
    var _consoleplayer = 0
    lateinit var _playeringame // normally MAXPLAYERS (4) for vanilla.
            : BooleanArray
    protected var p_demo = 0

    //  After that, demos contain a sequence of ticcmd_t's to build dynamically at
    // load time or when recording. This abstraction allows arbitrary demo sizes
    // and easy per-step handling, and even changes/extensions. Just make sure
    // that ticcmd_t's are serializable!
    // Also, the format used in demo lumps is NOT the same as in datagrams/network
    // (e.g. there is no consistency) and their handling is modified.
    var commands: Array<VanillaTiccmd>? = null
    var demorecorder: MutableList<IDemoTicCmd>

    init {
        demorecorder = ArrayList()
    }

    override fun unpack(b: ByteBuffer) {
        // Just the Header info for vanilla should be 13 bytes.
        // 1 byte at the end is the end-demo marker
        // So valid vanilla demos should have sizes that
        // fit the formula 14+4n, since each vanilla 
        // demo ticcmd_t is 4 bytes.
        val lens = (b.limit() - 13) / 4
        val vanilla = b.limit() == 14 + 4 * lens

        // Minimum valid vanilla demo should be 14 bytes...in theory.
        if (b.limit() < 14) {
            // Use skill==null as an indicator that loading didn't go well.
            _skill = null
            return
        }
        _version = b.get().toInt()
        _skill = try {
            skill_t.values()[b.get().toInt()]
        } catch (e: Exception) {
            null
        }
        _episode = b.get().toInt()
        _map = b.get().toInt()
        _deathmatch = b.get().toInt() != 0
        _respawnparm = b.get().toInt() != 0
        _fastparm = b.get().toInt() != 0
        _nomonsters = b.get().toInt() != 0
        _consoleplayer = b.get().toInt()
        _playeringame = BooleanArray(Limits.MAXPLAYERS)
        for (i in 0 until Limits.MAXPLAYERS) {
            _playeringame[i] = b.get().toInt() != 0
        }
        commands = GenericCopy.malloc({ VanillaTiccmd() }, lens)
        try {
            DoomBuffer.readObjectArray(b, commands as Array<CacheableDoomObject>, lens)
        } catch (e: IOException) {
            _skill = null
        }
    }

    override fun getNextTic(): IDemoTicCmd? {
        return if (commands != null && p_demo < commands!!.size) {
            commands!![p_demo++]
        } else null
    }

    override fun putTic(tic: IDemoTicCmd) {
        demorecorder.add(tic)
    }

    override fun getVersion(): Int {
        return _version
    }

    override fun setVersion(version: Int) {
        this._version = version
    }

    override fun getSkill(): skill_t? {
        return _skill
    }

    override fun setSkill(skill: skill_t?) {
        this._skill = skill
    }

    override fun getEpisode(): Int {
        return _episode
    }

    override fun setEpisode(episode: Int) {
        this._episode = episode
    }

    override fun getMap(): Int {
        return _map
    }

    override fun setMap(map: Int) {
        this._map = map
    }

    override fun isDeathmatch(): Boolean {
        return _deathmatch
    }

    override fun setDeathmatch(deathmatch: Boolean) {
        this._deathmatch = deathmatch
    }

    override fun isRespawnparm(): Boolean {
        return _respawnparm
    }

    override fun setRespawnparm(respawnparm: Boolean) {
        this._respawnparm = respawnparm
    }

    override fun isFastparm(): Boolean {
        return _fastparm
    }

    override fun setFastparm(fastparm: Boolean) {
        this._fastparm = fastparm
    }

    override fun isNomonsters(): Boolean {
        return _nomonsters
    }

    override fun setNomonsters(nomonsters: Boolean) {
        this._nomonsters = nomonsters
    }

    override fun getConsoleplayer(): Int {
        return _consoleplayer
    }

    override fun setConsoleplayer(consoleplayer: Int) {
        this._consoleplayer = consoleplayer
    }

    override fun getPlayeringame(): BooleanArray {
        return _playeringame
    }

    override fun setPlayeringame(playeringame: BooleanArray) {
        this._playeringame = playeringame
    }

    @Throws(IOException::class)
    override fun write(f: DataOutputStream) {
        f.writeByte(_version)
        f.writeByte(_skill!!.ordinal)
        f.writeByte(_episode)
        f.writeByte(_map)
        f.writeBoolean(_deathmatch)
        f.writeBoolean(_respawnparm)
        f.writeBoolean(_fastparm)
        f.writeBoolean(_nomonsters)
        f.writeByte(_consoleplayer)
        DoomIO.writeBoolean(f, _playeringame, Limits.MAXPLAYERS)
        for (i in demorecorder) {
            i.write(f)
        }
        f.writeByte(IDoomDemo.DEMOMARKER)

        // TODO Auto-generated method stub
    }

    override fun resetDemo() {
        p_demo = 0
    } /////////////////////// VARIOUS BORING GETTERS /////////////////////
}