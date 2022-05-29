package doom


import data.Defines
import data.Limits
import data.mapthing_t
import defines.*
import demo.IDoomDemo
import f.Finale
import g.Signals.ScanCode
import m.Settings
import mochadoom.Engine
import p.mobj_t
import java.io.OutputStreamWriter
import java.util.*
import java.util.stream.Stream

/**
 * We need globally shared data structures, for defining the global state
 * variables. MAES: in pure OO style, this should be a global "Doom state"
 * object to be passed along various modules. No ugly globals here!!! Now, some
 * of the variables that appear here were actually defined in separate modules.
 * Pretty much, whatever needs to be shared with other modules was placed here,
 * either as a local definition, or as an extern share. The very least, I'll
 * document where everything is supposed to come from/reside.
 */
abstract class DoomStatus<T, V> {
    var wadfiles = arrayOfNulls<String>(Limits.MAXWADFILES)
    var drone = false

    /** Command line parametersm, actually defined in d_main.c  */
    var nomonsters // checkparm of -nomonsters
            = false
    var respawnparm // checkparm of -respawn
            = false
    var fastparm // checkparm of -fast
            = false
    var devparm // DEBUG: launched with -devparm
            = false

    // MAES: declared as "extern", shared with Menu.java
    var inhelpscreens = false
    var advancedemo = false
    /////////// Local to doomstat.c ////////////
    // TODO: hide those behind getters
    /** Game Mode - identify IWAD as shareware, retail etc.
     * This is now hidden behind getters so some cases like plutonia
     * etc. can be handled more cleanly.
     */
    private var gamemode: GameMode? = null
    fun setGameMode(mode: GameMode?) {
        gamemode = mode
    }

    fun getGameMode(): GameMode? {
        return gamemode
    }

    fun isShareware(): Boolean {
        return gamemode == GameMode.shareware
    }

    /** Commercial means Doom 2, Plutonia, TNT, and possibly others like XBLA.
     *
     * @return
     */
    fun isCommercial(): Boolean {
        return gamemode == GameMode.commercial || gamemode == GameMode.pack_plut || gamemode == GameMode.pack_tnt || gamemode == GameMode.pack_xbla || gamemode == GameMode.freedoom2 || gamemode == GameMode.freedm
    }

    /** Retail means Ultimate.
     *
     * @return
     */
    fun isRetail(): Boolean {
        return gamemode == GameMode.retail || gamemode == GameMode.freedoom1
    }

    /** Registered is a subset of Ultimate
     *
     * @return
     */
    fun isRegistered(): Boolean {
        return gamemode == GameMode.registered || gamemode == GameMode.retail || gamemode == GameMode.freedoom1
    }

    var gamemission: GameMission_t? = null

    /** Language.  */
    var language: Language_t? = null
    // /////////// Normally found in d_main.c ///////////////
    // Selected skill type, map etc.
    /** Defaults for menu, methinks.  */
    var startskill: skill_t? = null
    var startepisode = 0
    var startmap = 0
    var autostart = false

    /** Selected by user  */
    var gameskill: skill_t? = null
    var gameepisode = 0
    var gamemap = 0

    /** Nightmare mode flag, single player.  */
    var respawnmonsters = false

    /** Netgame? Only true if >1 player.  */
    var netgame = false

    /**
     * Flag: true only if started as net deathmatch. An enum might handle
     * altdeath/cooperative better. Use altdeath for the "2" value
     */
    var deathmatch = false

    /** Use this instead of "deathmatch=2" which is bullshit.  */
    var altdeath = false

    //////////// STUFF SHARED WITH THE RENDERER ///////////////
    // -------------------------
    // Status flags for refresh.
    //
    var nodrawers = false
    var noblit = false
    var viewactive = false

    // Player taking events, and displaying.
    var consoleplayer = 0
    var displayplayer = 0

    // Depending on view size - no status bar?
    // Note that there is no way to disable the
    // status bar explicitely.
    var statusbaractive = false
    var automapactive // In AutoMap mode?
            = false
    var menuactive // Menu overlayed?
            = false
    var _paused // Game Pause?
            = false
    // -------------------------
    // Internal parameters for sound rendering.
    // These have been taken from the DOS version,
    // but are not (yet) supported with Linux
    // (e.g. no sound volume adjustment with menu.
    // These are not used, but should be (menu).
    // From m_menu.c:
    // Sound FX volume has default, 0 - 15
    // Music volume has default, 0 - 15
    // These are multiplied by 8.
    /** maximum volume for sound  */
    var snd_SfxVolume = 0

    /** maximum volume for music  */
    var snd_MusicVolume = 0

    /** Maximum number of sound channels  */
    var numChannels = 0

    // Current music/sfx card - index useless
    // w/o a reference LUT in a sound module.
    // Ideally, this would use indices found
    // in: /usr/include/linux/soundcard.h
    var snd_MusicDevice = 0
    var snd_SfxDevice = 0

    // Config file? Same disclaimer as above.
    var snd_DesiredMusicDevice = 0
    var snd_DesiredSfxDevice = 0

    // -------------------------------------
    // Scores, rating.
    // Statistics on a given map, for intermission.
    //
    var totalkills = 0
    var totalitems = 0
    var totalsecret = 0

    /** TNTHOM "cheat" for flashing HOM-detecting BG  */
    var flashing_hom = false

    // Added for prBoom+ code
    var totallive = 0

    // Timer, for scores.
    var levelstarttic // gametic at level start
            = 0
    var leveltime // tics in game play for par
            = 0

    // --------------------------------------
    // DEMO playback/recording related stuff.
    // No demo, there is a human player in charge?
    // Disable save/end game?
    var usergame = false

    // ?
    var demoplayback = false
    var demorecording = false

    // Quit after playing a demo from cmdline.
    var singledemo = false
    var mapstrobe = false

    /**
     * Set this to GS_DEMOSCREEN upon init, else it will be null
     * Good Sign at 2017/03/21: I hope it is no longer true info, since I've checked its assignment by NetBeans
     */
    var gamestate = gamestate_t.GS_DEMOSCREEN

    // -----------------------------
    // Internal parameters, fixed.
    // These are set by the engine, and not changed
    // according to user inputs. Partly load from
    // WAD, partly set at startup time.
    var gametic = 0

    // Alive? Disconnected?
    var playeringame = BooleanArray(Limits.MAXPLAYERS)
    var deathmatchstarts = arrayOfNulls<mapthing_t>(Limits.MAX_DM_STARTS)

    /** pointer into deathmatchstarts  */
    var deathmatch_p = 0

    /** Player spawn spots.  */
    var playerstarts = arrayOfNulls<mapthing_t>(Limits.MAXPLAYERS)

    /** Intermission stats.
     * Parameters for world map / intermission.  */
    var wminfo: wbstartstruct_t

    // -----------------------------------------
    // Internal parameters, used for engine.
    //
    // File handling stuff.
    var debugfile: OutputStreamWriter? = null

    // if true, load all graphics at level load
    var precache = false

    // wipegamestate can be set to -1
    // to force a wipe on the next draw
    // wipegamestate can be set to -1 to force a wipe on the next draw
    var wipegamestate = gamestate_t.GS_DEMOSCREEN
    var mouseSensitivity = 5 // AX: Fix wrong defaut mouseSensitivity

    /** Set if homebrew PWAD stuff has been added.  */
    var modifiedgame = false

    /** debug flag to cancel adaptiveness set to true during timedemos.  */
    var singletics = false

    /* A "fastdemo" is a demo with a clock that tics as
     * fast as possible, yet it maintains adaptiveness and doesn't
     * try to render everything at all costs.
     */
    protected var fastdemo = false
    protected var normaldemo = false
    protected var loaddemo: String? = null
    var bodyqueslot = 0

    // Needed to store the number of the dummy sky flat.
    // Used for rendering,
    // as well as tracking projectiles etc.
    //public int skyflatnum;
    // TODO: Netgame stuff (buffers and pointers, i.e. indices).
    // TODO: This is ???
    var doomcom: doomcom_t? = null

    // TODO: This points inside doomcom.
    var netbuffer: doomdata_t? = null
    var localcmds = arrayOfNulls<ticcmd_t>(Defines.BACKUPTICS)
    var rndindex = 0
    lateinit var netcmds // [MAXPLAYERS][BACKUPTICS];
            : Array<Array<ticcmd_t?>>

    /** MAES: this WAS NOT in the original.
     * Remember to call it!
     */
    protected fun initNetGameStuff() {
        //this.netbuffer = new doomdata_t();
        doomcom = doomcom_t()
        netcmds = Array(Limits.MAXPLAYERS) { arrayOfNulls(Defines.BACKUPTICS) }
        Arrays.setAll(localcmds) { i: Int -> ticcmd_t() }
        for (i in 0 until Limits.MAXPLAYERS) {
            Arrays.setAll(netcmds[i]) { j: Int -> ticcmd_t() }
        }
    }

    // Fields used for selecting variable BPP implementations.
    protected abstract fun selectFinale(): Finale<T>

    // MAES: Fields specific to DoomGame. A lot of them were
    // duplicated/externalized
    // in d_game.c and d_game.h, so it makes sense adopting a more unified
    // approach.
    protected var gameaction = gameaction_t.ga_nothing
    var sendpause // send a pause event next tic
            = false
    protected var sendsave // send a save event next tic
            = false
    protected var starttime = 0
    protected var timingdemo // if true, exit with report on completion
            = false

    fun getPaused(): Boolean {
        return _paused
    }

    fun setPaused(paused: Boolean) {
        this._paused = paused
    }

    // ////////// DEMO SPECIFIC STUFF/////////////
    protected var demoname: String? = null
    protected var netdemo = false

    //protected IDemoTicCmd[] demobuffer;
    protected var demobuffer: IDoomDemo? = null

    /** pointers  */ // USELESS protected int demo_p;
    // USELESS protected int demoend;
    protected var consistancy = Array(Limits.MAXPLAYERS) { ShortArray(Defines.BACKUPTICS) }
    protected lateinit var savebuffer: ByteArray

    /* TODO Proper reconfigurable controls. Defaults hardcoded for now. T3h h4x, d00d. */
    var key_right = ScanCode.SC_NUMKEY6.ordinal
    var key_left = ScanCode.SC_NUMKEY4.ordinal
    var key_up = ScanCode.SC_W.ordinal
    var key_down = ScanCode.SC_S.ordinal
    var key_strafeleft = ScanCode.SC_A.ordinal
    var key_straferight = ScanCode.SC_D.ordinal
    var key_fire = ScanCode.SC_LCTRL.ordinal
    var key_use = ScanCode.SC_SPACE.ordinal
    var key_strafe = ScanCode.SC_LALT.ordinal
    var key_speed = ScanCode.SC_RSHIFT.ordinal
    var vanillaKeyBehavior = false
    var key_recordstop = ScanCode.SC_Q.ordinal
    var key_numbers = Stream.of(
        ScanCode.SC_1,
        ScanCode.SC_2,
        ScanCode.SC_3,
        ScanCode.SC_4,
        ScanCode.SC_5,
        ScanCode.SC_6,
        ScanCode.SC_7,
        ScanCode.SC_8,
        ScanCode.SC_9,
        ScanCode.SC_0
    )
        .mapToInt { obj: ScanCode -> obj.ordinal }.toArray()

    // Heretic stuff
    var key_lookup = ScanCode.SC_PGUP.ordinal
    var key_lookdown = ScanCode.SC_PGDOWN.ordinal
    var key_lookcenter = ScanCode.SC_END.ordinal
    var mousebfire = 0
    var mousebstrafe = 2 // AX: Fixed - Now we use the right mouse buttons
    var mousebforward = 1 // AX: Fixed - Now we use the right mouse buttons
    var joybfire = 0
    var joybstrafe = 0
    var joybuse = 0
    var joybspeed = 0

    /** Cancel vertical mouse movement by default  */
    protected var novert = false // AX: The good default
    protected fun MAXPLMOVE(): Int {
        return forwardmove[1]
    }

    /** fixed_t  */
    protected val forwardmove = intArrayOf(0x19, 0x32) // + slow turn
    protected val sidemove = intArrayOf(0x18, 0x28)
    protected val angleturn = intArrayOf(640, 1280, 320)
    protected var gamekeydown = BooleanArray(DoomStatus.NUMKEYS)
    protected var keysCleared = false
    var alwaysrun = false
    protected var turnheld // for accelerative turning
            = 0
    protected var lookheld // for accelerative looking?
            = 0
    protected var mousearray = BooleanArray(4)

    /** This is an alias for mousearray [1+i]  */
    protected fun mousebuttons(i: Int): Boolean {
        return mousearray[1 + i] // allow [-1]
    }

    protected fun mousebuttons(i: Int, value: Boolean) {
        mousearray[1 + i] = value // allow [-1]
    }

    protected fun mousebuttons(i: Int, value: Int) {
        mousearray[1 + i] = value != 0 // allow [-1]
    }

    /** mouse values are used once  */
    protected var mousex = 0
    protected var mousey = 0
    protected var dclicktime = 0
    protected var dclickstate = 0
    protected var dclicks = 0
    protected var dclicktime2 = 0
    protected var dclickstate2 = 0
    protected var dclicks2 = 0

    /** joystick values are repeated  */
    protected var joyxmove = 0
    protected var joyymove = 0
    protected var joyarray = BooleanArray(5)
    protected fun joybuttons(i: Int): Boolean {
        return joyarray[1 + i] // allow [-1]
    }

    protected fun joybuttons(i: Int, value: Boolean) {
        joyarray[1 + i] = value // allow [-1]
    }

    protected fun joybuttons(i: Int, value: Int) {
        joyarray[1 + i] = value != 0 // allow [-1]
    }

    protected var savegameslot = 0
    protected var savedescription: String? = null
    protected var bodyque = arrayOfNulls<mobj_t>(DoomStatus.BODYQUESIZE)
    var statcopy // for statistics driver
            : String? = null

    /** Not documented/used in linuxdoom. I supposed it could be used to
     * ignore mouse input?
     */
    var use_mouse = false
    var use_joystick = false
    val CM: ConfigManager = Engine.getConfig()

    init {
        wminfo = wbstartstruct_t()
        initNetGameStuff()
    }

    open fun update() {
        snd_SfxVolume = CM.getValue(Settings.sfx_volume, Int::class.java)
        snd_MusicVolume = CM.getValue(Settings.music_volume, Int::class.java)
        alwaysrun = CM.equals(Settings.alwaysrun, java.lang.Boolean.TRUE)

        // Keys...
        key_right = CM.getValue(Settings.key_right, Int::class.java)
        key_left = CM.getValue(Settings.key_left, Int::class.java)
        key_up = CM.getValue(Settings.key_up, Int::class.java)
        key_down = CM.getValue(Settings.key_down, Int::class.java)
        key_strafeleft = CM.getValue(Settings.key_strafeleft, Int::class.java)
        key_straferight = CM.getValue(Settings.key_straferight, Int::class.java)
        key_fire = CM.getValue(Settings.key_fire, Int::class.java)
        key_use = CM.getValue(Settings.key_use, Int::class.java)
        key_strafe = CM.getValue(Settings.key_strafe, Int::class.java)
        key_speed = CM.getValue(Settings.key_speed, Int::class.java)

        // Mouse buttons
        use_mouse = CM.equals(Settings.use_mouse, 1)
        mousebfire = CM.getValue(Settings.mouseb_fire, Int::class.java)
        mousebstrafe = CM.getValue(Settings.mouseb_strafe, Int::class.java)
        mousebforward = CM.getValue(Settings.mouseb_forward, Int::class.java)

        // Joystick
        use_joystick = CM.equals(Settings.use_joystick, 1)
        joybfire = CM.getValue(Settings.joyb_fire, Int::class.java)
        joybstrafe = CM.getValue(Settings.joyb_strafe, Int::class.java)
        joybuse = CM.getValue(Settings.joyb_use, Int::class.java)
        joybspeed = CM.getValue(Settings.joyb_speed, Int::class.java)

        // Sound
        numChannels = CM.getValue(Settings.snd_channels, Int::class.java)

        // Map strobe
        mapstrobe = CM.equals(Settings.vestrobe, java.lang.Boolean.TRUE)

        // Mouse sensitivity
        mouseSensitivity = CM.getValue(Settings.mouse_sensitivity, Int::class.java)

        // This should indicate keyboard behavior should be as close as possible to vanilla
        vanillaKeyBehavior = CM.equals(Settings.vanilla_key_behavior, java.lang.Boolean.TRUE)
    }

    open fun commit() {
        CM.update(Settings.sfx_volume, snd_SfxVolume)
        CM.update(Settings.music_volume, snd_MusicVolume)
        CM.update(Settings.alwaysrun, alwaysrun)

        // Keys...
        CM.update(Settings.key_right, key_right)
        CM.update(Settings.key_left, key_left)
        CM.update(Settings.key_up, key_up)
        CM.update(Settings.key_down, key_down)
        CM.update(Settings.key_strafeleft, key_strafeleft)
        CM.update(Settings.key_straferight, key_straferight)
        CM.update(Settings.key_fire, key_fire)
        CM.update(Settings.key_use, key_use)
        CM.update(Settings.key_strafe, key_strafe)
        CM.update(Settings.key_speed, key_speed)

        // Mouse buttons
        CM.update(Settings.use_mouse, if (use_mouse) 1 else 0)
        CM.update(Settings.mouseb_fire, mousebfire)
        CM.update(Settings.mouseb_strafe, mousebstrafe)
        CM.update(Settings.mouseb_forward, mousebforward)

        // Joystick
        CM.update(Settings.use_joystick, if (use_joystick) 1 else 0)
        CM.update(Settings.joyb_fire, joybfire)
        CM.update(Settings.joyb_strafe, joybstrafe)
        CM.update(Settings.joyb_use, joybuse)
        CM.update(Settings.joyb_speed, joybspeed)

        // Sound
        CM.update(Settings.snd_channels, numChannels)

        // Map strobe
        CM.update(Settings.vestrobe, mapstrobe)

        // Mouse sensitivity
        CM.update(Settings.mouse_sensitivity, mouseSensitivity)
    }

    companion object {
        const val BGCOLOR = 7
        const val FGCOLOR = 8
        var RESENDCOUNT = 10
        var PL_DRONE = 0x80 // bit flag in doomdata->player

        /** LUT of ammunition limits for each kind.
         * This doubles with BackPack powerup item.
         * NOTE: this "maxammo" is treated like a global.
         */
        val maxammo = intArrayOf(200, 50, 300, 50)
        @JvmStatic
        protected val TURBOTHRESHOLD = 0x32
        @JvmStatic
        protected val SLOWTURNTICS = 6
        @JvmStatic
        protected val NUMKEYS = 256
        @JvmStatic
        protected val BODYQUESIZE = 32

        /** More prBoom+ stuff. Used mostly for code uhm..reuse, rather
         * than to actually change the way stuff works.
         *
         */
        var compatibility_level = 0
    }
}