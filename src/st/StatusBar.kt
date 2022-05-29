package st

import data.Defines
import data.Limits
import data.Tables
import data.sounds.musicenum_t
import defines.ammotype_t
import doom.*
import doom.SourceCode.*
import g.Signals
import m.Settings
import m.cheatseq_t
import rr.patch_t
import v.DoomGraphicSystem
import v.renderers.DoomScreen
import java.awt.Rectangle


// -----------------------------------------------------------------------------
//
// $Id: StatusBar.java,v 1.47 2011/11/01 23:46:37 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// DESCRIPTION:
// Status bar code.
// Does the face/direction indicator animatin.
// Does palette indicators as well (red pain/berserk, bright pickup)
//
// -----------------------------------------------------------------------------
class StatusBar(DOOM: DoomMain<*, *>) : AbstractStatusBar(DOOM) {
    // Location of status bar
    private val ST_X = 0
    private val ST_X2: Int
    private val ST_FX: Int
    private val ST_FY: Int
    private val ST_RECT: Rectangle
    private val ST_FACESX: Int
    private val ST_FACESY: Int

    // Location and size of statistics,
    // justified according to widget type.
    // Problem is, within which space? STbar? Screen?
    // Note: this could be read in by a lump.
    // Problem is, is the stuff rendered
    // into a buffer,
    // or into the frame buffer?
    // AMMO number pos.
    private val ST_AMMOWIDTH: Int
    private val ST_AMMOX: Int
    private val ST_AMMOY: Int

    // HEALTH number pos.
    private var ST_HEALTHWIDTH = 3
    private val ST_HEALTHX: Int
    private val ST_HEALTHY: Int

    // Weapon pos.
    private val ST_ARMSX: Int
    private val ST_ARMSY: Int
    private val ST_ARMSBGX: Int
    private val ST_ARMSBGY: Int
    private val ST_ARMSXSPACE: Int
    private val ST_ARMSYSPACE: Int

    // Frags pos.
    private val ST_FRAGSX: Int
    private val ST_FRAGSY: Int
    private val ST_FRAGSWIDTH: Int

    // ARMOR number pos.
    private val ST_ARMORWIDTH = 3
    private val ST_ARMORX: Int
    private val ST_ARMORY: Int

    // Key icon positions.
    private val ST_KEY0WIDTH: Int
    private val ST_KEY0HEIGHT: Int
    private val ST_KEY0X: Int
    private val ST_KEY0Y: Int
    private val ST_KEY1WIDTH: Int
    private val ST_KEY1X: Int
    private val ST_KEY1Y: Int
    private val ST_KEY2WIDTH: Int
    private val ST_KEY2X: Int
    private val ST_KEY2Y: Int

    // Ammunition counter.
    private val ST_AMMO0WIDTH: Int
    private val ST_AMMO0HEIGHT: Int
    private val ST_AMMO0X: Int
    private val ST_AMMO0Y: Int
    private val ST_AMMO1WIDTH: Int
    private val ST_AMMO1X: Int
    private val ST_AMMO1Y: Int
    private val ST_AMMO2WIDTH: Int
    private val ST_AMMO2X: Int
    private val ST_AMMO2Y: Int
    private val ST_AMMO3WIDTH: Int
    private val ST_AMMO3X: Int
    private val ST_AMMO3Y: Int

    // Indicate maximum ammunition.
    // Only needed because backpack exists.
    private val ST_MAXAMMO0WIDTH: Int
    private val ST_MAXAMMO0HEIGHT: Int
    private val ST_MAXAMMO0X: Int
    private val ST_MAXAMMO0Y: Int
    private val ST_MAXAMMO1WIDTH: Int
    private val ST_MAXAMMO1X: Int
    private val ST_MAXAMMO1Y: Int
    private val ST_MAXAMMO2WIDTH: Int
    private val ST_MAXAMMO2X: Int
    private val ST_MAXAMMO2Y: Int
    private val ST_MAXAMMO3WIDTH: Int
    private val ST_MAXAMMO3X: Int
    private val ST_MAXAMMO3Y: Int

    // pistol
    private val ST_WEAPON0X: Int
    private val ST_WEAPON0Y: Int

    // shotgun
    private val ST_WEAPON1X: Int
    private val ST_WEAPON1Y: Int

    // chain gun
    private val ST_WEAPON2X: Int
    private val ST_WEAPON2Y: Int

    // missile launcher
    private val ST_WEAPON3X: Int
    private val ST_WEAPON3Y: Int

    // plasma gun
    private val ST_WEAPON4X: Int
    private val ST_WEAPON4Y: Int

    // bfg
    private val ST_WEAPON5X: Int
    private val ST_WEAPON5Y: Int

    // WPNS title
    private val ST_WPNSX: Int
    private val ST_WPNSY: Int

    // DETH title
    private val ST_DETHX: Int
    private val ST_DETHY: Int

    // main player in game
    private var plyr: player_t? = null

    // ST_Start() has just been called, OR we want to force an redraw anyway.
    private var st_firsttime = false
    override fun forceRefresh() {
        st_firsttime = true
    }

    // used to execute ST_Init() only once
    private var veryfirsttime = 1

    // lump number for PLAYPAL
    private var lu_palette = 0

    // used for timing (unsigned int .. maybe long !)
    private var st_clock: Long = 0

    // used for making messages go away
    var st_msgcounter = 0

    // used when in chat
    private var st_chatstate: st_chatstateenum_t? = null

    // whether in automap or first-person
    private var st_gamestate: st_stateenum_t? = null

    /** whether left-side main status bar is active. This fugly hax
     * (and others like it) are necessary in order to have something
     * close to a pointer.
     */
    private val st_statusbaron = booleanArrayOf(false)

    // whether status bar chat is active
    private var st_chat = false

    // value of st_chat before message popped up
    private var st_oldchat = false

    // whether chat window has the cursor on
    private val st_cursoron = booleanArrayOf(false)

    /** !deathmatch  */
    private val st_notdeathmatch = booleanArrayOf(true)

    /** !deathmatch && st_statusbaron  */
    private val st_armson = booleanArrayOf(true)

    /** !deathmatch  */
    private val st_fragson = booleanArrayOf(false)

    // main bar left
    private var sbar: patch_t? = null

    // 0-9, tall numbers
    private val tallnum = arrayOfNulls<patch_t>(10)

    // tall % sign
    private var tallpercent: patch_t? = null

    // 0-9, short, yellow (,different!) numbers
    private val shortnum = arrayOfNulls<patch_t>(10)

    // 3 key-cards, 3 skulls
    private val keys = arrayOfNulls<patch_t>(Defines.NUMCARDS)

    // face status patches
    private val faces = arrayOfNulls<patch_t>(ST_NUMFACES)

    // face background
    private var faceback: patch_t? = null

    // main bar right
    private var armsbg: patch_t? = null

    // weapon ownership patches
    private val arms = Array(6) { arrayOfNulls<patch_t>(2) }

    // // WIDGETS /////
    // ready-weapon widget
    private var w_ready: st_number_t? = null

    // in deathmatch only, summary of frags stats
    private var w_frags: st_number_t? = null

    // health widget
    private var w_health: st_percent_t? = null

    // arms background
    private var w_armsbg: st_binicon_t? = null

    // weapon ownership widgets
    private val w_arms = arrayOfNulls<st_multicon_t>(6)

    // face status widget
    private var w_faces: st_multicon_t? = null

    // keycard widgets
    private val w_keyboxes = arrayOfNulls<st_multicon_t>(3)

    // armor widget
    private var w_armor: st_percent_t? = null

    // ammo widgets
    private val w_ammo = arrayOfNulls<st_number_t>(4)

    // max ammo widgets
    private val w_maxammo = arrayOfNulls<st_number_t>(4)

    // / END WIDGETS ////
    // number of frags so far in deathmatch
    private val st_fragscount = intArrayOf(0)

    // used to use appopriately pained face
    private var st_oldhealth = -1

    // used for evil grin
    private val oldweaponsowned = BooleanArray(Defines.NUMWEAPONS)

    // count until face changes
    private var st_facecount = 0

    // current face index, used by w_faces
    private val st_faceindex = IntArray(1)

    // holds key-type for each key box on bar
    private val keyboxes = IntArray(3)

    // a random number per tick
    private var st_randomnumber = 0

    // idmypos toggle mode
    private var st_idmypos = false

    // Massive bunches of cheat shit
    // to keep it from being easy to figure them out.
    // Yeah, right...
    private val cheat_mus_seq = charArrayOf(
        0xb2.toChar(),
        0x26.toChar(),
        0xb6.toChar(),
        0xae.toChar(),
        0xea.toChar(),
        1.toChar(),
        0.toChar(),
        0.toChar(),
        0xff.toChar()
    )
    private val cheat_choppers_seq = charArrayOf(
        0xb2.toChar(),
        0x26.toChar(),
        0xe2.toChar(),
        0x32.toChar(),
        0xf6.toChar(),
        0x2a.toChar(),
        0x2a.toChar(),
        0xa6.toChar(),
        0x6a.toChar(),
        0xea.toChar(),
        0xff // id...
            .toChar()
    )
    private val cheat_god_seq = charArrayOf(
        0xb2.toChar(), 0x26.toChar(), 0x26.toChar(), 0xaa.toChar(), 0x26.toChar(), 0xff // iddqd
            .toChar()
    )
    private val cheat_ammo_seq = charArrayOf(
        0xb2.toChar(), 0x26.toChar(), 0xf2.toChar(), 0x66.toChar(), 0xa2.toChar(), 0xff // idkfa
            .toChar()
    )
    private val cheat_ammonokey_seq = charArrayOf(
        0xb2.toChar(), 0x26.toChar(), 0x66.toChar(), 0xa2.toChar(), 0xff // idfa
            .toChar()
    )

    // Smashing Pumpkins Into Samml Piles Of Putried Debris.
    private val cheat_noclip_seq = charArrayOf(
        0xb2.toChar(), 0x26.toChar(), 0xea.toChar(), 0x2a.toChar(), 0xb2.toChar(),  // idspispopd
        0xea.toChar(), 0x2a.toChar(), 0xf6.toChar(), 0x2a.toChar(), 0x26.toChar(), 0xff.toChar()
    )

    //
    private val cheat_commercial_noclip_seq = charArrayOf(
        0xb2.toChar(), 0x26.toChar(), 0xe2.toChar(), 0x36.toChar(), 0xb2.toChar(), 0x2a.toChar(), 0xff // idclip
            .toChar()
    )
    private val cheat_powerup_seq = arrayOf(
        charArrayOf(
            0xb2.toChar(),
            0x26.toChar(),
            0x62.toChar(),
            0xa6.toChar(),
            0x32.toChar(),
            0xf6.toChar(),
            0x36.toChar(),
            0x26.toChar(),
            0x6e.toChar(),
            0xff.toChar()
        ),
        charArrayOf(
            0xb2.toChar(),
            0x26.toChar(),
            0x62.toChar(),
            0xa6.toChar(),
            0x32.toChar(),
            0xf6.toChar(),
            0x36.toChar(),
            0x26.toChar(),
            0xea.toChar(),
            0xff.toChar()
        ),
        charArrayOf(
            0xb2.toChar(),
            0x26.toChar(),
            0x62.toChar(),
            0xa6.toChar(),
            0x32.toChar(),
            0xf6.toChar(),
            0x36.toChar(),
            0x26.toChar(),
            0xb2.toChar(),
            0xff.toChar()
        ),
        charArrayOf(
            0xb2.toChar(),
            0x26.toChar(),
            0x62.toChar(),
            0xa6.toChar(),
            0x32.toChar(),
            0xf6.toChar(),
            0x36.toChar(),
            0x26.toChar(),
            0x6a.toChar(),
            0xff.toChar()
        ),
        charArrayOf(
            0xb2.toChar(),
            0x26.toChar(),
            0x62.toChar(),
            0xa6.toChar(),
            0x32.toChar(),
            0xf6.toChar(),
            0x36.toChar(),
            0x26.toChar(),
            0xa2.toChar(),
            0xff.toChar()
        ),
        charArrayOf(
            0xb2.toChar(),
            0x26.toChar(),
            0x62.toChar(),
            0xa6.toChar(),
            0x32.toChar(),
            0xf6.toChar(),
            0x36.toChar(),
            0x26.toChar(),
            0x36.toChar(),
            0xff.toChar()
        ),
        charArrayOf(
            0xb2.toChar(),
            0x26.toChar(),
            0x62.toChar(),
            0xa6.toChar(),
            0x32.toChar(),
            0xf6.toChar(),
            0x36.toChar(),
            0x26.toChar(),
            0xff.toChar()
        )
    )
    private val cheat_clev_seq = charArrayOf(
        0xb2.toChar(),
        0x26.toChar(),
        0xe2.toChar(),
        0x36.toChar(),
        0xa6.toChar(),
        0x6e.toChar(),
        1.toChar(),
        0.toChar(),
        0.toChar(),
        0xff // idclev
            .toChar()
    )

    // my position cheat
    private val cheat_mypos_seq = charArrayOf(
        0xb2.toChar(),
        0x26.toChar(),
        0xb6.toChar(),
        0xba.toChar(),
        0x2a.toChar(),
        0xf6.toChar(),
        0xea.toChar(),
        0xff // idmypos
            .toChar()
    )

    // Now what?
    var cheat_mus = cheatseq_t(cheat_mus_seq, 0)
    var cheat_god = cheatseq_t(cheat_god_seq, 0)
    var cheat_ammo = cheatseq_t(cheat_ammo_seq, 0)
    var cheat_ammonokey = cheatseq_t(cheat_ammonokey_seq, 0)
    var cheat_noclip = cheatseq_t(cheat_noclip_seq, 0)
    var cheat_commercial_noclip = cheatseq_t(cheat_commercial_noclip_seq, 0)
    var cheat_powerup = arrayOf(
        cheatseq_t(cheat_powerup_seq[0], 0),
        cheatseq_t(cheat_powerup_seq[1], 0),
        cheatseq_t(cheat_powerup_seq[2], 0),
        cheatseq_t(cheat_powerup_seq[3], 0),
        cheatseq_t(cheat_powerup_seq[4], 0),
        cheatseq_t(cheat_powerup_seq[5], 0),
        cheatseq_t(cheat_powerup_seq[6], 0)
    )
    var cheat_choppers = cheatseq_t(cheat_choppers_seq, 0)
    var cheat_clev = cheatseq_t(cheat_clev_seq, 0)
    var cheat_mypos = cheatseq_t(cheat_mypos_seq, 0)
    var cheat_tnthom = cheatseq_t("tnthom", false)

    // 
    //var mapnames: Array<String>
    fun refreshBackground() {
        if (st_statusbaron[0]) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.SB,
                sbar!!,
                DOOM.vs,
                ST_X,
                0,
                DoomGraphicSystem.V_SAFESCALE or DoomGraphicSystem.V_NOSCALESTART
            )
            //V.DrawPatch(ST_X, 0, BG, sbar);
            if (DOOM.netgame) {
                DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.SB,
                    faceback!!,
                    DOOM.vs,
                    ST_FX,
                    ST_Y,
                    DoomGraphicSystem.V_SAFESCALE or DoomGraphicSystem.V_NOSCALESTART
                )
                //V.DrawPatch(ST_FX, 0, BG, faceback);
            }

            // Buffers the background.
            DOOM.graphicSystem.CopyRect(DoomScreen.SB, ST_RECT, DoomScreen.FG, DOOM.graphicSystem.point(ST_X, ST_Y))
            //V.CopyRect(ST_X, 0, SCREEN_SB, ST_WIDTH, ST_HEIGHT, ST_X, ST_Y, SCREEN_FG);
        }
    }

    override fun Init() {
        veryfirsttime = 0
        loadData()
    }

    protected var st_stopped = true
    @Suspicious(CauseOfDesyncProbability.LOW)
    override fun Start() {
        if (!st_stopped) {
            Stop()
        }
        initData()
        createWidgets()
        st_stopped = false
    }

    fun Stop() {
        if (st_stopped) return
        // Reset palette.
        DOOM.graphicSystem.setPalette(0)
        st_stopped = true
    }

    fun loadData() {
        lu_palette = DOOM.wadLoader.GetNumForName("PLAYPAL")
        loadGraphics()
    }

    // Filter automap on/off.
    override fun NotifyAMEnter() {
        st_gamestate = st_stateenum_t.AutomapState
        st_firsttime = true
    }

    override fun NotifyAMExit() {
        // fprintf(stderr, "AM exited\n");
        st_gamestate = st_stateenum_t.FirstPersonState
    }

    // Respond to keyboard input events,
    // intercept cheats.
    @ST_Stuff.C(ST_Stuff.ST_Responder)
    override fun Responder(ev: event_t): Boolean {
        if (ev.isType(evtype_t.ev_keydown)) {
            if (!DOOM.netgame) {
                // b. - enabled for more debug fun.
                // if (gameskill != sk_nightmare) {

                // 'dqd' cheat for toggleable god mode
                if (ev.ifKeyAsciiChar { key: Int -> cheat_god.CheckCheat(key) }) {
                    plyr!!.cheats = plyr!!.cheats xor player_t.CF_GODMODE
                    if (plyr!!.cheats and player_t.CF_GODMODE != 0) {
                        if (plyr!!.mo != null) plyr!!.mo!!.health = 100
                        plyr!!.health[0] = 100
                        plyr!!.message = englsh.STSTR_DQDON
                    } else plyr!!.message = englsh.STSTR_DQDOFF
                } else if (ev.ifKeyAsciiChar { key: Int -> cheat_ammonokey.CheckCheat(key) }) {
                    plyr!!.armorpoints[0] = 200
                    plyr!!.armortype = 2
                    for (i in 0 until Defines.NUMWEAPONS) plyr!!.weaponowned[i] = true // true
                    System.arraycopy(plyr!!.maxammo, 0, plyr!!.ammo, 0, Defines.NUMAMMO)
                    plyr!!.message = englsh.STSTR_FAADDED
                } else if (ev.ifKeyAsciiChar { key: Int -> cheat_ammo.CheckCheat(key) }) {
                    plyr!!.armorpoints[0] = 200
                    plyr!!.armortype = 2
                    for (i in 0 until Defines.NUMWEAPONS) plyr!!.weaponowned[i] = true // true
                    System.arraycopy(plyr!!.maxammo, 0, plyr!!.ammo, 0, Defines.NUMAMMO)
                    for (i in 0 until Defines.NUMCARDS) plyr!!.cards[i] = true
                    plyr!!.message = englsh.STSTR_KFAADDED
                } else if (ev.ifKeyAsciiChar { key: Int -> cheat_mus.CheckCheat(key) }) {
                    val buf = CharArray(3)
                    val musnum: Int
                    plyr!!.message = englsh.STSTR_MUS
                    cheat_mus.GetParam(buf)
                    if (DOOM.isCommercial()) {
                        musnum = musicenum_t.mus_runnin.ordinal + ((buf[0].code - '0'.code)
                                * 10) + buf[1].code - '0'.code - 1
                        if ((buf[0].code - '0'.code) * 10 + buf[1].code - '0'.code > 35) plyr!!.message =
                            englsh.STSTR_NOMUS else DOOM.doomSound.ChangeMusic(musnum, true)
                    } else {
                        musnum = musicenum_t.mus_e1m1.ordinal + (buf[0].code - '1'.code) * 9 + (buf[1].code - '1'.code)
                        if ((buf[0].code - '1'.code) * 9 + buf[1].code - '1'.code > 31) plyr!!.message =
                            englsh.STSTR_NOMUS else DOOM.doomSound.ChangeMusic(musnum, true)
                    }
                } else if (ev.ifKeyAsciiChar { key: Int -> cheat_noclip.CheckCheat(key) } || ev.ifKeyAsciiChar { key: Int ->
                        cheat_commercial_noclip.CheckCheat(
                            key
                        )
                    }) {
                    plyr!!.cheats = plyr!!.cheats xor player_t.CF_NOCLIP
                    if (plyr!!.cheats and player_t.CF_NOCLIP != 0) plyr!!.message =
                        englsh.STSTR_NCON else plyr!!.message = englsh.STSTR_NCOFF
                }
                // 'behold?' power-up cheats
                for (i in 0..5) {
                    if (ev.ifKeyAsciiChar { key: Int -> cheat_powerup[i].CheckCheat(key) }) {
                        if (plyr!!.powers[i] == 0) plyr!!.GivePower(i) else if (i != Defines.pw_strength) plyr!!.powers[i] =
                            1 else plyr!!.powers[i] = 0
                        plyr!!.message = englsh.STSTR_BEHOLDX
                    }
                }

                // 'behold' power-up menu
                if (ev.ifKeyAsciiChar { key: Int -> cheat_powerup[6].CheckCheat(key) }) {
                    plyr!!.message = englsh.STSTR_BEHOLD
                } else if (ev.ifKeyAsciiChar { key: Int -> cheat_choppers.CheckCheat(key) }) {
                    plyr!!.weaponowned[weapontype_t.wp_chainsaw.ordinal] = true
                    plyr!!.powers[Defines.pw_invulnerability] = 1 // true
                    plyr!!.message = englsh.STSTR_CHOPPERS
                } else if (ev.ifKeyAsciiChar { key: Int -> cheat_mypos.CheckCheat(key) }) {
                    // MAES: made into a toggleable cheat.
                    st_idmypos = !st_idmypos
                } else if (ev.ifKeyAsciiChar { key: Int -> cheat_tnthom.CheckCheat(key) }) {
                    // MAES: made into a toggleable cheat.
                    plyr!!.message = if (!DOOM.flashing_hom.also {
                            DOOM.flashing_hom = it
                        }) "HOM Detection On" else "HOM Detection Off"
                }
            }

            // 'clev' change-level cheat
            if (ev.ifKeyAsciiChar { key: Int -> cheat_clev.CheckCheat(key) }) {
                val buf = CharArray(3)
                val epsd: Int
                val map: Int
                cheat_clev.GetParam(buf)

                // This applies to Doom II, Plutonia and TNT.
                if (DOOM.isCommercial()) {
                    epsd = 0
                    map = (buf[0].code - '0'.code) * 10 + buf[1].code - '0'.code
                } else {
                    epsd = buf[0].code - '0'.code
                    map = buf[1].code - '0'.code
                }

                // Catch invalid maps.
                if (epsd < 1 && !DOOM.isCommercial()) return false
                if (map < 1) return false
                
                // Ohmygod - this is not going to work.
                if (DOOM.isRetail() && (epsd > 4 || map > 9)) return false

                // MAES: If it's doom.wad but not ultimate
                if (DOOM.isRegistered() && !DOOM.isRetail() && (epsd > 3 || map > 9)) return false
                if (DOOM.isShareware() && (epsd > 1 || map > 9)) return false
                if (DOOM.isCommercial() && (epsd > 1 || map > 34)) return false

                // So be it.
                plyr!!.message = englsh.STSTR_CLEV
                DOOM.DeferedInitNew(DOOM.gameskill, epsd, map)
            }
        }
        return false
    }

    protected var lastcalc = 0
    protected var oldhealth = -1
    fun calcPainOffset(): Int {
        var health = 0
        health = if (plyr!!.health[0] > 100) 100 else plyr!!.health[0]
        if (health != oldhealth) {
            lastcalc = ST_FACESTRIDE * ((100 - health) * ST_NUMPAINFACES / 101)
            oldhealth = health
        }
        return lastcalc
    }

    protected var lastattackdown = -1
    protected var priority = 0

    /**
     * This is a not-very-pretty routine which handles the face states and their
     * timing. the precedence of expressions is: dead > evil grin > turned head
     * > straight ahead
     */
    fun updateFaceWidget() {
        val badguyangle: Long // angle_t
        val diffang: Long
        var doevilgrin: Boolean
        if (priority < 10) {
            // dead
            if (plyr!!.health[0] == 0) {
                priority = 9
                st_faceindex[0] = ST_DEADFACE
                st_facecount = 1
            }
        }
        if (priority < 9) {
            if (plyr!!.bonuscount != 0) {
                // picking up bonus
                doevilgrin = false
                for (i in 0 until Defines.NUMWEAPONS) {
                    if (oldweaponsowned[i] != plyr!!.weaponowned[i]) {
                        doevilgrin = true
                        oldweaponsowned[i] = plyr!!.weaponowned[i]
                    }
                }
                if (doevilgrin) {
                    // evil grin if just picked up weapon
                    priority = 8
                    st_facecount = ST_EVILGRINCOUNT
                    st_faceindex[0] = calcPainOffset() + ST_EVILGRINOFFSET
                }
            }
        }
        if (priority < 8) {
            if (plyr!!.damagecount != 0 && plyr!!.attacker != null && plyr!!.attacker !== plyr!!.mo) {
                // being attacked
                priority = 7
                /**
                 * Another switchable fix of mine
                 * - Good Sign 2017/04/02
                 */
                if ((if (DOOM.CM.equals(
                            Settings.fix_ouch_face,
                            java.lang.Boolean.TRUE
                        )
                    ) st_oldhealth - plyr!!.health[0] else plyr!!.health[0] - st_oldhealth) > ST_MUCHPAIN
                ) {
                    st_facecount = ST_TURNCOUNT
                    st_faceindex[0] = calcPainOffset() + ST_OUCHOFFSET
                } else {
                    badguyangle = DOOM.sceneRenderer.PointToAngle2(
                        plyr!!.mo!!._x, plyr!!.mo!!._y, plyr!!.attacker!!._x,
                        plyr!!.attacker!!._y
                    )
                    val obtuse: Boolean // that's another "i"
                    if (badguyangle > plyr!!.mo!!.angle) {
                        // whether right or left
                        diffang = badguyangle - plyr!!.mo!!.angle
                        obtuse = diffang > Tables.ANG180
                    } else {
                        // whether left or right
                        diffang = plyr!!.mo!!.angle - badguyangle
                        obtuse = diffang <= Tables.ANG180
                    } // confusing, aint it?
                    st_facecount = ST_TURNCOUNT
                    st_faceindex[0] = calcPainOffset()
                    if (diffang < Tables.ANG45) {
                        // head-on
                        st_faceindex[0] += ST_RAMPAGEOFFSET
                    } else if (obtuse) {
                        // turn face right
                        st_faceindex[0] += ST_TURNOFFSET
                    } else {
                        // turn face left
                        st_faceindex[0] += ST_TURNOFFSET + 1
                    }
                }
            }
        }
        if (priority < 7) {
            // getting hurt because of your own damn stupidity
            if (plyr!!.damagecount != 0) {
                /**
                 * Another switchable fix of mine
                 * - Good Sign 2017/04/02
                 */
                if ((if (DOOM.CM.equals(
                            Settings.fix_ouch_face,
                            java.lang.Boolean.TRUE
                        )
                    ) st_oldhealth - plyr!!.health[0] else plyr!!.health[0] - st_oldhealth) > ST_MUCHPAIN
                ) {
                    priority = 7
                    st_facecount = ST_TURNCOUNT
                    st_faceindex[0] = calcPainOffset() + ST_OUCHOFFSET
                } else {
                    priority = 6
                    st_facecount = ST_TURNCOUNT
                    st_faceindex[0] = calcPainOffset() + ST_RAMPAGEOFFSET
                }
            }
        }
        if (priority < 6) {
            // rapid firing
            if (plyr!!.attackdown) {
                if (lastattackdown == -1) lastattackdown = ST_RAMPAGEDELAY else if (--lastattackdown == 0) {
                    priority = 5
                    st_faceindex[0] = calcPainOffset() + ST_RAMPAGEOFFSET
                    st_facecount = 1
                    lastattackdown = 1
                }
            } else lastattackdown = -1
        }
        if (priority < 5) {
            // invulnerability
            if (plyr!!.cheats and player_t.CF_GODMODE != 0 || plyr!!.powers[Defines.pw_invulnerability] != 0) {
                priority = 4
                st_faceindex[0] = ST_GODFACE
                st_facecount = 1
            }
        }

        // look left or look right if the facecount has timed out
        if (st_facecount == 0) {
            st_faceindex[0] = calcPainOffset() + st_randomnumber % 3
            st_facecount = ST_STRAIGHTFACECOUNT
            priority = 0
        }
        st_facecount--
    }

    protected var largeammo = 1994 // means "n/a"

    /**
     * MAES: this code updated the widgets. Now, due to the way they are
     * constructed, they originally were "hooked" to actual variables using
     * pointers so that they could tap into them directly and self-update.
     * Clearly we can't do that in Java unless said variables are inside an
     * array and we provide both the array AND an index. For other cases, we
     * must simply build ad-hoc hacks.
     *
     * In any case, only "status" updates are performed here. Actual visual
     * updates are performed by the Drawer.
     *
     */
    fun updateWidgets() {
        var i: Int

        // MAES: sticky idmypos cheat that is actually useful
        // TODO: this spams the player message queue at every tic.
        // A direct overlay with a widget would be more useful.
        if (st_idmypos) {
            val mo = DOOM.players[DOOM.consoleplayer]!!.mo
            plyr!!.message = String.format("ang= 0x%x; x,y= (%x, %x)", mo!!.angle.toInt(), mo!!._x, mo!!._y)
        }


        // must redirect the pointer if the ready weapon has changed.
        // if (w_ready.data != plyr.readyweapon)
        // {
        if (items.weaponinfo[plyr!!.readyweapon.ordinal].ammo == ammotype_t.am_noammo) w_ready!!.numindex =
            largeammo else w_ready!!.numindex = items.weaponinfo[plyr!!.readyweapon.ordinal].ammo.ordinal
        w_ready!!.data = plyr!!.readyweapon.ordinal

        // if (*w_ready.on)
        // STlib_updateNum(&w_ready, true);
        // refresh weapon change
        // }

        // update keycard multiple widgets
        i = 0
        while (i < 3) {
            keyboxes[i] = if (plyr!!.cards[i]) i else -1
            if (plyr!!.cards[i + 3]) keyboxes[i] = i + 3
            i++
        }

        // refresh everything if this is him coming back to life
        updateFaceWidget()

        // used by the w_armsbg widget
        st_notdeathmatch[0] = !DOOM.deathmatch

        // used by w_arms[] widgets
        st_armson[0] = st_statusbaron[0] && !(DOOM.altdeath || DOOM.deathmatch)

        // used by w_frags widget
        st_fragson[0] = (DOOM.altdeath || DOOM.deathmatch) && st_statusbaron[0]
        st_fragscount[0] = 0
        i = 0
        while (i < Limits.MAXPLAYERS) {
            if (i != DOOM.consoleplayer) st_fragscount[0] += plyr!!.frags[i] else st_fragscount[0] -= plyr!!.frags[i]
            i++
        }

        // get rid of chat window if up because of message
        if (--st_msgcounter == 0) st_chat = st_oldchat
    }

    override fun Ticker() {
        st_clock++
        st_randomnumber = DOOM.random.M_Random()
        updateWidgets()
        st_oldhealth = plyr!!.health[0]
    }

    fun doPaletteStuff() {
        var palette: Int
        //byte[] pal;
        var cnt: Int
        val bzc: Int
        cnt = plyr!!.damagecount
        if (plyr!!.powers[Defines.pw_strength] != 0) {
            // slowly fade the berzerk out
            bzc = 12 - (plyr!!.powers[Defines.pw_strength] shr 6)
            if (bzc > cnt) cnt = bzc
        }
        if (cnt != 0) {
            palette = cnt + 7 shr 3
            if (palette >= NUMREDPALS) palette = NUMREDPALS - 1
            palette += STARTREDPALS
        } else if (plyr!!.bonuscount != 0) {
            palette = plyr!!.bonuscount + 7 shr 3
            if (palette >= NUMBONUSPALS) palette = NUMBONUSPALS - 1
            palette += STARTBONUSPALS
        } else if (plyr!!.powers[Defines.pw_ironfeet] > 4 * 32
            || plyr!!.powers[Defines.pw_ironfeet] and 8 != 0
        ) palette = RADIATIONPAL else palette = 0
        if (palette != st_palette) {
            st_palette = palette
            DOOM.graphicSystem.setPalette(palette)
        }
    }

    fun drawWidgets(refresh: Boolean) {
        var i: Int

        // used by w_arms[] widgets
        st_armson[0] = st_statusbaron[0] && !(DOOM.altdeath || DOOM.deathmatch)

        // used by w_frags widget
        st_fragson[0] = DOOM.deathmatch && st_statusbaron[0]
        w_ready!!.update(refresh)
        i = 0
        while (i < 4) {
            w_ammo[i]!!.update(refresh)
            w_maxammo[i]!!.update(refresh)
            i++
        }
        w_armor!!.update(refresh)
        w_armsbg!!.update(refresh)
        i = 0
        while (i < 6) {
            w_arms[i]!!.update(refresh)
            i++
        }
        w_faces!!.update(refresh)
        i = 0
        while (i < 3) {
            w_keyboxes[i]!!.update(refresh)
            i++
        }
        w_frags!!.update(refresh)
        w_health!!.update(refresh)
    }

    fun doRefresh() {
        st_firsttime = false

        // draw status bar background to off-screen buff
        refreshBackground()

        // and refresh all widgets
        drawWidgets(true)
    }

    fun diffDraw() {
        // update all widgets
        drawWidgets(false)
    }

    override fun Drawer(fullscreen: Boolean, refresh: Boolean) {
        st_statusbaron[0] = !fullscreen || DOOM.automapactive
        st_firsttime = st_firsttime || refresh

        // Do red-/gold-shifts from damage/items
        doPaletteStuff()

        // If just after ST_Start(), refresh all
        if (st_firsttime) doRefresh() else diffDraw()
    }

    fun loadGraphics() {
        var i: Int
        var j: Int
        var facenum: Int
        var namebuf: String

        // Load the numbers, tall and short
        i = 0
        while (i < 10) {
            namebuf = "STTNUM$i"
            tallnum[i] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            namebuf = "STYSNUM$i"
            shortnum[i] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            i++
        }

        // Load percent key.
        // Note: why not load STMINUS here, too?
        tallpercent = DOOM.wadLoader.CachePatchName("STTPRCNT", Defines.PU_STATIC)
        // MAES: in fact, I do this for sanity. Fuck them. Seriously.
        sttminus = DOOM.wadLoader.CachePatchName("STTMINUS")

        // key cards
        i = 0
        while (i < Defines.NUMCARDS) {
            namebuf = "STKEYS$i"
            keys[i] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            i++
        }

        // arms background
        armsbg = DOOM.wadLoader.CachePatchName("STARMS", Defines.PU_STATIC)

        // arms ownership widgets
        i = 0
        while (i < 6) {
            namebuf = "STGNUM" + (i + 2)

            // gray #
            arms[i][0] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)

            // yellow #
            arms[i][1] = shortnum[i + 2]
            i++
        }

        // face backgrounds for different color players
        namebuf = "STFB" + DOOM.consoleplayer
        faceback = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)

        // status bar background bits
        sbar = DOOM.wadLoader.CachePatchName("STBAR", Defines.PU_STATIC)

        // face states
        facenum = 0
        i = 0
        while (i < ST_NUMPAINFACES) {
            j = 0
            while (j < ST_NUMSTRAIGHTFACES) {
                namebuf = "STFST$i$j"
                faces[facenum++] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
                j++
            }
            namebuf = "STFTR" + i + "0" // turn right
            faces[facenum++] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            namebuf = "STFTL" + i + "0" // turn left
            faces[facenum++] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            namebuf = "STFOUCH$i" // ouch!
            faces[facenum++] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            namebuf = "STFEVL$i" // evil grin ;)
            faces[facenum++] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            namebuf = "STFKILL$i" // pissed off
            faces[facenum++] = DOOM.wadLoader.CachePatchName(namebuf, Defines.PU_STATIC)
            i++
        }
        faces[facenum++] = DOOM.wadLoader.CachePatchName("STFGOD0", Defines.PU_STATIC)
        faces[facenum++] = DOOM.wadLoader.CachePatchName("STFDEAD0", Defines.PU_STATIC)
    }

    fun unloadGraphics() {
        var i: Int // unload the numbers, tall and short 
        i = 0
        while (i < 10) {
            DOOM.wadLoader.UnlockLumpNum(tallnum[i])
            tallnum[i] = null
            DOOM.wadLoader.UnlockLumpNum(shortnum[i])
            shortnum[i] = null
            i++
        }

        // unload tall percent
        DOOM.wadLoader.UnlockLumpNum(tallpercent)
        tallpercent = null


        // unload arms background          
        DOOM.wadLoader.UnlockLumpNum(armsbg)
        armsbg = null
        // unload gray #'s          
        i = 0
        while (i < 6) {
            DOOM.wadLoader.UnlockLumpNum(arms[i][0])
            arms[i][0] = null
            DOOM.wadLoader.UnlockLumpNum(arms[i][1])
            arms[i][1] = null
            i++
        }

        // unload the key cards for (i=0;i<NUMCARDS;i++)
        i = 0
        while (i < 6) {
            DOOM.wadLoader.UnlockLumpNum(keys[i])
            keys[i] = null
            i++
        }
        DOOM.wadLoader.UnlockLumpNum(sbar)
        sbar = null
        DOOM.wadLoader.UnlockLumpNum(faceback)
        faceback = null
        i = 0
        while (i < ST_NUMFACES) {
            DOOM.wadLoader.UnlockLumpNum(faces[i])
            faces[i] = null
            i++
        }


        // Note: nobody ain't seen no unloading
        // of stminus yet. Dude.
    }

    fun unloadData() {
        unloadGraphics()
    }

    fun initData() {
        var i: Int
        st_firsttime = true
        plyr = DOOM.players[DOOM.consoleplayer]
        st_clock = 0
        st_chatstate = st_chatstateenum_t.StartChatState
        st_gamestate = st_stateenum_t.FirstPersonState
        st_statusbaron[0] = true
        st_chat = false
        st_oldchat = st_chat
        st_cursoron[0] = false
        st_faceindex[0] = 0
        st_palette = -1
        st_oldhealth = -1
        i = 0
        while (i < Defines.NUMWEAPONS) {
            oldweaponsowned[i] = plyr!!.weaponowned[i]
            i++
        }
        i = 0
        while (i < 3) {
            keyboxes[i] = -1
            i++
        }
        Init()
    }

    /**
     * Widgets are created here. Be careful, because their "constructors" used
     * reference to boolean or int variables so that they could be auto-updated
     * by the global refresh functions. We can only do this with some
     * limitations in Java (e.g. passing an array AND an index).
     */
    fun createWidgets() {
        var i: Int

        // ready weapon ammo
        w_ready = st_number_t(
            ST_AMMOX, ST_AMMOY, tallnum, plyr!!.ammo,
            items.weaponinfo[plyr!!.readyweapon.ordinal].ammo.ordinal,
            st_statusbaron, 0, ST_AMMOWIDTH
        )

        // the last weapon type
        w_ready!!.data = plyr!!.readyweapon.ordinal

        // health percentage
        w_health = st_percent_t(
            ST_HEALTHX, ST_HEALTHY, tallnum, plyr!!.health,
            0, st_statusbaron, 0, tallpercent
        )

        // arms background
        w_armsbg = st_binicon_t(
            ST_ARMSBGX, ST_ARMSBGY, armsbg, st_notdeathmatch, 0,
            st_statusbaron, 0
        )

        // weapons owned
        i = 0
        while (i < 6) {
            w_arms[i] = st_multicon_t(
                ST_ARMSX + i % 3 * ST_ARMSXSPACE, ST_ARMSY
                        + i / 3 * ST_ARMSYSPACE, arms[i], plyr!!.weaponowned,
                i + 1, st_armson, 0
            )
            i++
        }

        // frags sum
        w_frags = st_number_t(
            ST_FRAGSX, ST_FRAGSY, tallnum, st_fragscount, 0,  // dummy,
            // we're
            // passing
            // an
            // integer.
            st_fragson, 0, ST_FRAGSWIDTH
        )

        // faces
        w_faces = st_multicon_t(
            ST_FACESX, ST_FACESY, faces, st_faceindex, 0,
            st_statusbaron, 0
        )

        // armor percentage - should be colored later
        w_armor = st_percent_t(
            ST_ARMORX, ST_ARMORY, tallnum, plyr!!.armorpoints,
            0, st_statusbaron, 0, tallpercent
        )

        // keyboxes 0-2
        w_keyboxes[0] = st_multicon_t(
            ST_KEY0X, ST_KEY0Y, keys, keyboxes, 0,
            st_statusbaron, 0
        )
        w_keyboxes[1] = st_multicon_t(
            ST_KEY1X, ST_KEY1Y, keys, keyboxes, 1,
            st_statusbaron, 0
        )
        w_keyboxes[2] = st_multicon_t(
            ST_KEY2X, ST_KEY2Y, keys, keyboxes, 2,
            st_statusbaron, 0
        )

        // ammo count (all four kinds)
        w_ammo[0] = st_number_t(
            ST_AMMO0X, ST_AMMO0Y, shortnum, plyr!!.ammo, 0,
            st_statusbaron, 0, ST_AMMO0WIDTH
        )
        w_ammo[1] = st_number_t(
            ST_AMMO1X, ST_AMMO1Y, shortnum, plyr!!.ammo, 1,
            st_statusbaron, 0, ST_AMMO1WIDTH
        )
        w_ammo[2] = st_number_t(
            ST_AMMO2X, ST_AMMO2Y, shortnum, plyr!!.ammo, 2,
            st_statusbaron, 0, ST_AMMO2WIDTH
        )
        w_ammo[3] = st_number_t(
            ST_AMMO3X, ST_AMMO3Y, shortnum, plyr!!.ammo, 3,
            st_statusbaron, 0, ST_AMMO3WIDTH
        )

        // max ammo count (all four kinds)
        w_maxammo[0] = st_number_t(
            ST_MAXAMMO0X, ST_MAXAMMO0Y, shortnum, plyr!!.maxammo,
            0, st_statusbaron, 0, ST_MAXAMMO0WIDTH
        )
        w_maxammo[1] = st_number_t(
            ST_MAXAMMO1X, ST_MAXAMMO1Y, shortnum, plyr!!.maxammo,
            1, st_statusbaron, 0, ST_MAXAMMO1WIDTH
        )
        w_maxammo[2] = st_number_t(
            ST_MAXAMMO2X, ST_MAXAMMO2Y, shortnum, plyr!!.maxammo,
            2, st_statusbaron, 0, ST_MAXAMMO2WIDTH
        )
        w_maxammo[3] = st_number_t(
            ST_MAXAMMO3X, ST_MAXAMMO3Y, shortnum, plyr!!.maxammo,
            3, st_statusbaron, 0, ST_MAXAMMO3WIDTH
        )
    }

    /** Binary Icon widget
     * This is used for stuff such as keys or weapons, which you either have
     * or you don't.
     *
     */
    internal inner class st_binicon_t(// center-justified location of icon
        var x: Int, var y: Int, // icon
        var p: patch_t?,
        /** pointer to current icon status  */
        var `val`: BooleanArray, var valindex: Int,
        /** pointer to boolean
         * stating whether to update icon  */
        var on: BooleanArray, var onindex: Int
    ) : StatusBarWidget {
        // last icon value
        var oldval = false
        var data // user data
                = 0

        // Binary Icon widget routines
        init {
            `val`[valindex] = false
        }

        override fun update(refresh: Boolean) {
            val bi = this
            val x: Int
            val y: Int
            val w: Int
            val h: Int
            if (bi.on[onindex] && (bi.oldval != bi.`val`[valindex] || refresh)) {
                x = bi.x - bi.p!!.leftoffset
                y = bi.y - bi.p!!.topoffset
                w = bi.p!!.width.toInt()
                h = bi.p!!.height.toInt()
                if (y - ST_Y < 0) DOOM.doomSystem.Error("updateBinIcon: y - ST_Y < 0")
                if (bi.`val`[valindex]) {
                    val rect = Rectangle(x, ST_Y, w * DOOM.vs.getScalingX(), h * DOOM.vs.getScalingY())
                    DOOM.graphicSystem.CopyRect(
                        DoomScreen.FG,
                        rect,
                        DoomScreen.BG,
                        DOOM.graphicSystem.point(rect.x, rect.y)
                    )
                    DOOM.graphicSystem.DrawPatchScaled(
                        DoomScreen.FG,
                        bi.p!!,
                        DOOM.vs,
                        bi.x,
                        bi.y,
                        DoomGraphicSystem.V_PREDIVIDE
                    )
                } else {
                    val rect = Rectangle(x, ST_Y, w * DOOM.vs.getScalingX(), h * DOOM.vs.getScalingY())
                    DOOM.graphicSystem.CopyRect(
                        DoomScreen.FG,
                        rect,
                        DoomScreen.BG,
                        DOOM.graphicSystem.point(rect.x, rect.y)
                    )
                }
                bi.oldval = bi.`val`[valindex]
            }
        }
    }

    /** Icon widget  */
    internal inner class st_multicon_t(// center-justified location of icons
        var x: Int, var y: Int, il: Array<patch_t?>, iarray: Any?,
        inum: Int, on: BooleanArray, onindex: Int
    ) : StatusBarWidget {
        // last icon number
        var oldinum: Int

        /** pointer to current icon, if not an array type.  */
        //var iarray: IntArray
        var inum: Int

        // pointer to boolean stating
        // whether to update icon
        var on: BooleanArray
        var onindex = 0

        // list of icons
        var p: Array<patch_t?>

        // user data
        var data = 0

        /** special status 0=boolean[] 1=integer[] -1= unspecified  */
        var status = -1
        protected lateinit var asboolean: BooleanArray
        protected lateinit var asint: IntArray

        init {
            oldinum = -1
            this.inum = inum
            this.on = on
            p = il
            if (iarray is BooleanArray) {
                status = 0
                asboolean = iarray
            } else if (iarray is IntArray) {
                status = 1
                asint = iarray
            }
        }

        override fun update(refresh: Boolean) {
            val w: Int
            val h: Int
            val x: Int
            val y: Int

            // Actual value to be considered. Poor man's generics!
            var thevalue = -1
            when (status) {
                0 -> thevalue = if (asboolean[inum]) 1 else 0
                1 -> thevalue = asint[inum]
            }

            // Unified treatment of boolean and integer references
            // So the widget will update iff:
            // a) It's on AND
            // b) The new value is different than the old one
            // c) Neither of them is -1
            // d) We actually asked for a refresh.
            if (on[onindex] && (oldinum != thevalue || refresh) && thevalue != -1) {
                // Previous value must not have been -1.
                if (oldinum != -1) {
                    x = this.x - p[oldinum]!!.leftoffset * DOOM.vs.getScalingX()
                    y = this.y - p[oldinum]!!.topoffset * DOOM.vs.getScalingY()
                    w = p[oldinum]!!.width * DOOM.vs.getScalingX()
                    h = p[oldinum]!!.height * DOOM.vs.getScalingY()
                    val rect = Rectangle(x, y - ST_Y, w, h)
                    if (y - ST_Y < 0) DOOM.doomSystem.Error("updateMultIcon: y - ST_Y < 0")
                    //System.out.printf("Restoring at x y %d %d w h %d %d\n",x, y - ST_Y,w,h);
                    DOOM.graphicSystem.CopyRect(DoomScreen.SB, rect, DoomScreen.FG, DOOM.graphicSystem.point(x, y))
                    //V.CopyRect(x, y - ST_Y, SCREEN_SB, w, h, x, y, SCREEN_FG);
                    //V.FillRect(x, y - ST_Y, w, h, FG);
                }

                //System.out.printf("Drawing at x y %d %d w h %d %d\n",this.x,this.y,p[thevalue].width,p[thevalue].height);
                DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG,
                    p[thevalue]!!,
                    DOOM.vs,
                    this.x,
                    this.y,
                    DoomGraphicSystem.V_SCALEOFFSET or DoomGraphicSystem.V_NOSCALESTART
                )
                oldinum = thevalue
            }
        }
    }

    protected var sttminus: patch_t? = null

    /** Number widget  */
    internal inner class st_number_t(
        x: Int, y: Int, pl: Array<patch_t?>, numarray: IntArray,
        numindex: Int, on: BooleanArray, onindex: Int, width: Int
    ) : StatusBarWidget {
        /** upper right-hand corner of the number (right-justified)  */
        var x = 0
        var y = 0

        /** max # of digits in number  */
        var width = 0

        /** last number value  */
        var oldnum = 0

        /**
         * Array in which to point with num.
         *
         * Fun fact: initially I tried to use Integer and Boolean, but those are
         * immutable -_-. Fuck that, Java.
         *
         */
        lateinit var numarray: IntArray

        /** pointer to current value. Of course makes sense only for arrays.  */
        var numindex = 0

        /** pointer to boolean stating whether to update number  */
        lateinit var on: BooleanArray
        var onindex = 0

        /** list of patches for 0-9  */
        lateinit var p: Array<patch_t?>

        /** user data  */
        var data = 0

        // Number widget routines
        init {
            init(x, y, pl, numarray, numindex, on, onindex, width)
        }

        fun init(
            x: Int, y: Int, pl: Array<patch_t?>, numarray: IntArray, numindex: Int,
            on: BooleanArray, onindex: Int, width: Int
        ) {
            this.x = x
            this.y = y
            oldnum = 0
            this.width = width
            this.numarray = numarray
            this.on = on
            this.onindex = onindex
            p = pl
            this.numindex = numindex // _D_ fixed this bug
        }

        // 
        // A fairly efficient way to draw a number
        // based on differences from the old number.
        // Note: worth the trouble?
        //
        fun drawNum(refresh: Boolean) {

            //st_number_t n = this;
            var numdigits = width // HELL NO. This only worked while the width happened
            // to be 3.
            val w = p[0]!!.width * DOOM.vs.getScalingX()
            val h = p[0]!!.height * DOOM.vs.getScalingY()
            var x = x
            val neg: Boolean

            // clear the area
            x = this.x - numdigits * w
            if (y - ST_Y < 0) {
                DOOM.doomSystem.Error("drawNum: n.y - ST_Y < 0")
            }

            // Restore BG from buffer
            //V.FillRect(x+(numdigits-3) * w, y, w*3 , h, FG);
            val rect = Rectangle(x + (numdigits - 3) * w, y - ST_Y, w * 3, h)
            DOOM.graphicSystem.CopyRect(
                DoomScreen.SB,
                rect,
                DoomScreen.FG,
                DOOM.graphicSystem.point(x + (numdigits - 3) * w, y)
            )
            //V.CopyRect(x+(numdigits-3)*w, y- ST_Y, SCREEN_SB, w * 3, h, x+(numdigits-3)*w, y, SCREEN_FG);

            // if non-number, do not draw it
            if (numindex == largeammo) return
            var num = numarray[numindex]

            // In this way, num and oldnum are exactly the same. Maybe this
            // should go in the end?
            oldnum = num
            neg = num < 0
            if (neg) {
                if (numdigits == 2 && num < -9) num = -9 else if (numdigits == 3 && num < -99) num = -99
                num = -num
            }
            x = this.x

            // in the special case of 0, you draw 0
            if (num == 0) //V.DrawPatch(x - w, n.y, FG, n.p[0]);
                DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG,
                    p[0]!!,
                    DOOM.vs,
                    x - w,
                    y,
                    DoomGraphicSystem.V_NOSCALESTART or DoomGraphicSystem.V_TRANSLUCENTPATCH
                )


            // draw the new number
            while (num != 0 && numdigits-- != 0) {
                x -= w
                //V.DrawPatch(x, n.y, FG, n.p[num % 10]);
                DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG,
                    p[num % 10]!!,
                    DOOM.vs,
                    x,
                    y,
                    DoomGraphicSystem.V_NOSCALESTART or DoomGraphicSystem.V_TRANSLUCENTPATCH
                )
                num /= 10
            }

            // draw a minus sign if necessary
            if (neg) DOOM.graphicSystem.DrawPatchScaled( /*DrawPatch*/DoomScreen.FG,
                sttminus!!,
                DOOM.vs,
                x - 8 * DOOM.vs.getScalingX(),
                y,
                DoomGraphicSystem.V_NOSCALESTART or DoomGraphicSystem.V_TRANSLUCENTPATCH
            )
            //V.DrawPatch(x - sttminus.width*vs.getScalingX(), n.y, FG, sttminus);
        }

        override fun update(refresh: Boolean) {
            if (on[onindex]) drawNum(refresh)
        }
    }

    internal inner class st_percent_t(
        x: Int, y: Int, pl: Array<patch_t?>, numarray: IntArray,
        numindex: Int, on: BooleanArray, onindex: Int, percent: patch_t?
    ) : StatusBarWidget {
        // Percent widget ("child" of number widget,
        // or, more precisely, contains a number widget.)
        // number information
        var n: st_number_t

        // percent sign graphic
        var p: patch_t?

        init {
            n = st_number_t(x, y, pl, numarray, numindex, on, onindex, 3)
            p = percent
        }

        override fun update(refresh: Boolean) {
            if (n.on[0]) DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                p!!,
                DOOM.vs,
                n.x,
                n.y,
                DoomGraphicSystem.V_NOSCALESTART
            )
            n.update(refresh)
        }
    }

    internal interface StatusBarWidget {
        fun update(refresh: Boolean)
    }

    // Size of statusbar.
    // Now sensitive for scaling.
    var ST_HEIGHT: Int = 32 * DOOM.vs.getSafeScaling()
    var ST_WIDTH: Int
    var ST_Y: Int

    //
    // STATUS BAR CODE
    //
    init {

        ST_WIDTH = DOOM.vs.getScreenWidth()
        ST_Y = DOOM.vs.getScreenHeight() - ST_HEIGHT
        ST_X2 = (104 * DOOM.vs.getSafeScaling())
        ST_FX = (143 * DOOM.vs.getSafeScaling())
        ST_FY = (169 * DOOM.vs.getSafeScaling())
        ST_FACESX = (143 * DOOM.vs.getSafeScaling())
        ST_FACESY = (168 * DOOM.vs.getSafeScaling())

        // AMMO number pos.
        ST_AMMOWIDTH = 3
        ST_AMMOX = (44 * DOOM.vs.getSafeScaling())
        ST_AMMOY = (171 * DOOM.vs.getSafeScaling())

        // HEALTH number pos
        ST_HEALTHWIDTH = 3
        ST_HEALTHX = (90 * DOOM.vs.getSafeScaling())
        ST_HEALTHY = (171 * DOOM.vs.getSafeScaling())

        // Weapon pos.
        ST_ARMSX = (111 * DOOM.vs.getSafeScaling())
        ST_ARMSY = (172 * DOOM.vs.getSafeScaling())
        ST_ARMSBGX = (104 * DOOM.vs.getSafeScaling())
        ST_ARMSBGY = (168 * DOOM.vs.getSafeScaling())
        ST_ARMSXSPACE = 12 * DOOM.vs.getSafeScaling()
        ST_ARMSYSPACE = 10 * DOOM.vs.getSafeScaling()

        // Frags pos.
        ST_FRAGSX = (138 * DOOM.vs.getSafeScaling())
        ST_FRAGSY = (171 * DOOM.vs.getSafeScaling())
        ST_FRAGSWIDTH = 2

        //
        ST_ARMORX = (221 * DOOM.vs.getSafeScaling())
        ST_ARMORY = (171 * DOOM.vs.getSafeScaling())

        // Key icon positions.
        ST_KEY0WIDTH = 8 * DOOM.vs.getSafeScaling()
        ST_KEY0HEIGHT = 5 * DOOM.vs.getSafeScaling()
        ST_KEY0X = (239 * DOOM.vs.getSafeScaling())
        ST_KEY0Y = (171 * DOOM.vs.getSafeScaling())
        ST_KEY1WIDTH = ST_KEY0WIDTH
        ST_KEY1X = (239 * DOOM.vs.getSafeScaling())
        ST_KEY1Y = (181 * DOOM.vs.getSafeScaling())
        ST_KEY2WIDTH = ST_KEY0WIDTH
        ST_KEY2X = (239 * DOOM.vs.getSafeScaling())
        ST_KEY2Y = (191 * DOOM.vs.getSafeScaling())

        // Ammunition counter.
        ST_AMMO0WIDTH = 3 * DOOM.vs.getSafeScaling()
        ST_AMMO0HEIGHT = 6 * DOOM.vs.getSafeScaling()
        ST_AMMO0X = (288 * DOOM.vs.getSafeScaling())
        ST_AMMO0Y = (173 * DOOM.vs.getSafeScaling())
        ST_AMMO1WIDTH = ST_AMMO0WIDTH
        ST_AMMO1X = (288 * DOOM.vs.getSafeScaling())
        ST_AMMO1Y = (179 * DOOM.vs.getSafeScaling())
        ST_AMMO2WIDTH = ST_AMMO0WIDTH
        ST_AMMO2X = (288 * DOOM.vs.getSafeScaling())
        ST_AMMO2Y = (191 * DOOM.vs.getSafeScaling())
        ST_AMMO3WIDTH = ST_AMMO0WIDTH
        ST_AMMO3X = (288 * DOOM.vs.getSafeScaling())
        ST_AMMO3Y = (185 * DOOM.vs.getSafeScaling())

        // Indicate maximum ammunition.
        // Only needed because backpack exists.
        ST_MAXAMMO0WIDTH = 3 * DOOM.vs.getSafeScaling()
        ST_MAXAMMO0HEIGHT = 5 * DOOM.vs.getSafeScaling()
        ST_MAXAMMO0X = (314 * DOOM.vs.getSafeScaling())
        ST_MAXAMMO0Y = (173 * DOOM.vs.getSafeScaling())
        ST_MAXAMMO1WIDTH = ST_MAXAMMO0WIDTH
        ST_MAXAMMO1X = 314 * DOOM.vs.getSafeScaling()
        ST_MAXAMMO1Y = (179 * DOOM.vs.getSafeScaling())
        ST_MAXAMMO2WIDTH = ST_MAXAMMO0WIDTH
        ST_MAXAMMO2X = (314 * DOOM.vs.getSafeScaling())
        ST_MAXAMMO2Y = (191 * DOOM.vs.getSafeScaling())
        ST_MAXAMMO3WIDTH = ST_MAXAMMO0WIDTH
        ST_MAXAMMO3X = (314 * DOOM.vs.getSafeScaling())
        ST_MAXAMMO3Y = (185 * DOOM.vs.getSafeScaling())

        // pistol
        ST_WEAPON0X = (110 * DOOM.vs.getSafeScaling())
        ST_WEAPON0Y = (172 * DOOM.vs.getSafeScaling())

        // shotgun
        ST_WEAPON1X = (122 * DOOM.vs.getSafeScaling())
        ST_WEAPON1Y = (172 * DOOM.vs.getSafeScaling())

        // chain gun
        ST_WEAPON2X = (134 * DOOM.vs.getSafeScaling())
        ST_WEAPON2Y = (172 * DOOM.vs.getSafeScaling())

        // missile launcher
        ST_WEAPON3X = (110 * DOOM.vs.getSafeScaling())
        ST_WEAPON3Y = (181 * DOOM.vs.getSafeScaling())

        // plasma gun
        ST_WEAPON4X = (122 * DOOM.vs.getSafeScaling())
        ST_WEAPON4Y = (181 * DOOM.vs.getSafeScaling())

        // bfg
        ST_WEAPON5X = (134 * DOOM.vs.getSafeScaling())
        ST_WEAPON5Y = (181 * DOOM.vs.getSafeScaling())

        // WPNS title
        ST_WPNSX = (109 * DOOM.vs.getSafeScaling())
        ST_WPNSY = (191 * DOOM.vs.getSafeScaling())

        // DETH title
        ST_DETHX = (109 * DOOM.vs.getSafeScaling())
        ST_DETHY = (191 * DOOM.vs.getSafeScaling())
        ST_RECT = Rectangle(ST_X, 0, ST_WIDTH, ST_HEIGHT)
        //this.plyr=DM.players[DM.]
    }

	//TODO: CHeck this    
	override fun getHeight(): Int {
        return ST_HEIGHT//_height
    }

    companion object {
        const val rcsid = "\$Id: StatusBar.java,v 1.47 2011/11/01 23:46:37 velktron Exp $"

        // Size of statusbar.
        // Now sensitive for scaling.
        //
        // STATUS BAR DATA
        //
        // Palette indices.
        // For damage/bonus red-/gold-shifts
        private const val STARTREDPALS = 1
        private const val STARTBONUSPALS = 9
        private const val NUMREDPALS = 8
        private const val NUMBONUSPALS = 4

        // Radiation suit, green shift.
        private const val RADIATIONPAL = 13

        // N/256*100% probability
        // that the normal face state will change
        private const val ST_FACEPROBABILITY = 96

        // For Responder
        private val ST_TOGGLECHAT = Signals.ScanCode.SC_ENTER.c.code

        // Should be set to patch width
        // for tall numbers later on
        // TODO: private static int ST_TALLNUMWIDTH = (tallnum[0].width);
        // Number of status faces.
        private const val ST_NUMPAINFACES = 5
        private const val ST_NUMSTRAIGHTFACES = 3
        private const val ST_NUMTURNFACES = 2
        private const val ST_NUMSPECIALFACES = 3
        private const val ST_FACESTRIDE = ST_NUMSTRAIGHTFACES + ST_NUMTURNFACES + ST_NUMSPECIALFACES
        private const val ST_NUMEXTRAFACES = 2
        private const val ST_NUMFACES = ST_FACESTRIDE * ST_NUMPAINFACES + ST_NUMEXTRAFACES
        private const val ST_TURNOFFSET = ST_NUMSTRAIGHTFACES
        private const val ST_OUCHOFFSET = ST_TURNOFFSET + ST_NUMTURNFACES
        private const val ST_EVILGRINOFFSET = ST_OUCHOFFSET + 1
        private const val ST_RAMPAGEOFFSET = ST_EVILGRINOFFSET + 1
        private const val ST_GODFACE = ST_NUMPAINFACES * ST_FACESTRIDE
        private const val ST_DEADFACE = ST_GODFACE + 1
        private const val ST_EVILGRINCOUNT = 2 * Defines.TICRATE
        private const val ST_STRAIGHTFACECOUNT = Defines.TICRATE / 2
        private const val ST_TURNCOUNT = 1 * Defines.TICRATE
        private const val ST_OUCHCOUNT = 1 * Defines.TICRATE
        private const val ST_RAMPAGEDELAY = 2 * Defines.TICRATE
        private const val ST_MUCHPAIN = 20

        // Incoming messages window location
        // UNUSED
        // #define ST_MSGTEXTX (viewwindowx)
        // #define ST_MSGTEXTY (viewwindowy+viewheight-18)
        private const val ST_MSGTEXTX = 0
        private const val ST_MSGTEXTY = 0

        // Dimensions given in characters.
        private const val ST_MSGWIDTH = 52

        // Or shall I say, in lines?
        private const val ST_MSGHEIGHT = 1
        private const val ST_OUTTEXTX = 0
        private const val ST_OUTTEXTY = 6

        // Width, in characters again.
        private const val ST_OUTWIDTH = 52

        // Height, in lines.
        private const val ST_OUTHEIGHT = 1

        // TODO private static int ST_MAPWIDTH =
        // (mapnames[(gameepisode-1)*9+(gamemap-1)].length));
        // TODO private static int ST_MAPTITLEX = (SCREENWIDTH - ST_MAPWIDTH *
        // ST_CHATFONTWIDTH);
        private const val ST_MAPTITLEY = 0
        private const val ST_MAPHEIGHT = 1
        var st_palette = 0
    }
}