package f

import data.*
import data.sounds.sfxenum_t
import data.sounds.musicenum_t
import defines.GameMode
import defines.gamestate_t
import defines.statenum_t
import doom.*
import doom.SourceCode.F_Finale
import m.Settings
import mochadoom.Engine
import rr.flat_t
import rr.patch_t
import utils.C2JUtils
import v.DoomGraphicSystem
import v.graphics.Blocks
import v.renderers.DoomScreen
import java.awt.Rectangle


//-----------------------------------------------------------------------------
//
// $Id: Finale.java,v 1.28 2012/09/24 17:16:23 velktron Exp $
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// DESCRIPTION:
//  Game completion, final screen animation.
//
//-----------------------------------------------------------------------------
class Finale<T>(val DOOM: DoomMain<T, *>) {
    var finalestage = 0
    var finalecount = 0
    var finaletext: String? = null
    var finaleflat: String? = null

    /**
     * F_StartFinale
     */
    fun StartFinale() {
        DOOM.setGameAction( gameaction_t.ga_nothing )
        DOOM.gamestate = gamestate_t.GS_FINALE
        DOOM.viewactive = false
        DOOM.automapactive = false

        val texts: Array<String?> = when (DOOM.getGameMode()) {
            GameMode.commercial, GameMode.pack_xbla, GameMode.freedoom2, GameMode.freedm -> doom2_text
            GameMode.pack_tnt -> tnt_text
            GameMode.pack_plut -> plut_text
            GameMode.shareware, GameMode.registered, GameMode.retail, GameMode.freedoom1 -> doom_text
            else -> arrayOf()
        }
        when (DOOM.getGameMode()) {
            GameMode.freedoom1, GameMode.shareware, GameMode.registered, GameMode.retail -> {
                DOOM.doomSound.ChangeMusic(sounds.musicenum_t.mus_victor, true)
                when (DOOM.gameepisode) {
                    1 -> {
                        finaleflat = "FLOOR4_8"
                        finaletext = texts[0]
                    }
                    2 -> {
                        finaleflat = "SFLR6_1"
                        finaletext = texts[1]
                    }
                    3 -> {
                        finaleflat = "MFLR8_4"
                        finaletext = texts[2]
                    }
                    4 -> {
                        finaleflat = "MFLR8_3"
                        finaletext = texts[3]
                    }
                    else -> {}
                }
            }
            GameMode.freedm, GameMode.freedoom2, GameMode.commercial, GameMode.pack_xbla, GameMode.pack_tnt, GameMode.pack_plut -> {
                DOOM.doomSound.ChangeMusic(sounds.musicenum_t.mus_read_m, true)
                when (DOOM.gamemap) {
                    6 -> {
                        finaleflat = "SLIME16"
                        finaletext = texts[0]
                    }
                    11 -> {
                        finaleflat = "RROCK14"
                        finaletext = texts[1]
                    }
                    20 -> {
                        finaleflat = "RROCK07"
                        finaletext = texts[2]
                    }
                    30 -> {
                        finaleflat = "RROCK17"
                        finaletext = texts[3]
                    }
                    15 -> {
                        finaleflat = "RROCK13"
                        finaletext = texts[4]
                    }
                    31 -> {
                        finaleflat = "RROCK19"
                        finaletext = texts[5]
                    }
                    else -> {}
                }
            }
            else -> {
                DOOM.doomSound.ChangeMusic(sounds.musicenum_t.mus_read_m, true)
                finaleflat = "F_SKY1" // Not used anywhere else.
                finaletext = doom2_text[1]
            }
        }
        finalestage = 0
        finalecount = 0
    }

    @F_Finale.C(F_Finale.F_Responder)
    fun Responder(event: event_t): Boolean {
        return if (finalestage == 2) CastResponder(event) else false
    }

    /**
     * F_Ticker
     */
    fun Ticker() {

        // check for skipping
        if (DOOM.isCommercial() && finalecount > 50) {
            var i: Int
            // go on to the next level
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (DOOM.players[i].cmd.buttons.code != 0) {
                    break
                }
                i++
            }
            if (i < Limits.MAXPLAYERS) {
                if (DOOM.gamemap == 30) {
                    StartCast()
                } else {
                    DOOM.setGameAction(gameaction_t.ga_worlddone)
                }
            }
        }

        // advance animation
        finalecount++
        if (finalestage == 2) {
            CastTicker()
            return
        }
        if (DOOM.isCommercial()) {
            return
        }

        // MAES: this is when we can transition to bunny.
        if (finalestage == 0 && finalecount > finaletext!!.length * TEXTSPEED + TEXTWAIT) {
            finalecount = 0
            finalestage = 1
            DOOM.wipegamestate = gamestate_t.GS_MINUS_ONE // force a wipe
            if (DOOM.gameepisode == 3) {
                DOOM.doomSound.StartMusic(sounds.musicenum_t.mus_bunny)
            }
        }
    }

    //
    // F_TextWrite
    //
    // #include "hu_stuff.h"
    var hu_font: Array<patch_t>
    fun TextWrite() {
        // erase the entire screen to a tiled background
        val src = DOOM.wadLoader.CacheLumpName(finaleflat!!, Defines.PU_CACHE, flat_t::class.java).data
        if (Engine.getConfig().equals(Settings.scale_screen_tiles, java.lang.Boolean.TRUE)) {
            val scaled = (DOOM.graphicSystem as Blocks<Any, DoomScreen>)
                .ScaleBlock(
                    DOOM.graphicSystem.convertPalettedBlock(*src), 64, 64,
                    DOOM.graphicSystem.getScalingX(), DOOM.graphicSystem.getScalingY()
                )
            (DOOM.graphicSystem as Blocks<Any, DoomScreen>)
                .TileScreen(
                    DoomScreen.FG, scaled, Rectangle(
                        0, 0,
                        64 * DOOM.graphicSystem.getScalingX(), 64 * DOOM.graphicSystem.getScalingY()
                    )
                )
        } else {
            (DOOM.graphicSystem as Blocks<Any, DoomScreen>)
                .TileScreen(
                    DoomScreen.FG, DOOM.graphicSystem.convertPalettedBlock(*src),
                    Rectangle(0, 0, 64, 64)
                )
        }

        // draw some of the text onto the screen
        var cx = 10
        var cy = 10
        val ch = finaletext!!.toCharArray()
        var count = (finalecount - 10) / TEXTSPEED
        if (count < 0) {
            count = 0
        }

        // _D_: added min between count and ch.length, so that the text is not
        // written all at once
        for (i in 0 until Math.min(ch.size, count)) {
            var c = ch[i].code
            if (c == 0) break
            if (c == '\n'.code) {
                cx = 10
                cy += 11
                continue
            }
            c = c.uppercaseChar() - Defines.HU_FONTSTART
            if (c < 0 || c > Defines.HU_FONTSIZE) {
                cx += 4
                continue
            }
            if (cx + hu_font[c].width > DOOM.vs.getScreenWidth()) {
                break
            }
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, hu_font[c], DOOM.vs, cx, cy)
            cx += hu_font[c].width.toInt()
        }
    }

    private val castorder: Array<castinfo_t>
    var castnum = 0
    var casttics = 0
    var caststate: state_t? = null
    var castdeath = false
    var castframes = 0
    var castonmelee = 0
    var castattacking = false

    //
    // F_StartCast
    //
    // extern gamestate_t wipegamestate;
    fun StartCast() {
        DOOM.wipegamestate = gamestate_t.GS_MINUS_ONE // force a screen wipe
        castnum = 0
        caststate = info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].seestate.ordinal]
        casttics = caststate!!.tics
        castdeath = false
        finalestage = 2
        castframes = 0
        castonmelee = 0
        castattacking = false
        DOOM.doomSound.ChangeMusic(sounds.musicenum_t.mus_evil, true)
    }

    //
    // F_CastTicker
    //
    fun CastTicker() {
        if (--casttics > 0) return  // not time to change state yet
        if (caststate!!.tics == -1 || caststate!!.nextstate == statenum_t.S_NULL || caststate!!.nextstate == null) {
            // switch from deathstate to next monster
            castnum++
            castdeath = false
            if (castorder[castnum].name == null) {
                castnum = 0
            }
            if (info.mobjinfo[castorder[castnum].type!!.ordinal].seesound.ordinal != 0) {
                DOOM.doomSound.StartSound(null, info.mobjinfo[castorder[castnum].type!!.ordinal].seesound)
            }
            caststate = info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].seestate.ordinal]
            castframes = 0
        } else {
            val sfx: sounds.sfxenum_t?

            // just advance to next state in animation
            if (caststate === info.states[statenum_t.S_PLAY_ATK1.ordinal]) {
                stopattack() // Oh, gross hack!
                afterstopattack()
                return  // bye ...
            }
            val st = caststate!!.nextstate!!
            caststate = info.states[st.ordinal]
            castframes++
            sfx = when (st) {
                statenum_t.S_PLAY_ATK1 -> sfxenum_t.sfx_dshtgn
                statenum_t.S_POSS_ATK2 -> sfxenum_t.sfx_pistol
                statenum_t.S_SPOS_ATK2 -> sfxenum_t.sfx_shotgn
                statenum_t.S_VILE_ATK2 -> sfxenum_t.sfx_vilatk
                statenum_t.S_SKEL_FIST2 -> sfxenum_t.sfx_skeswg
                statenum_t.S_SKEL_FIST4 -> sfxenum_t.sfx_skepch
                statenum_t.S_SKEL_MISS2 -> sfxenum_t.sfx_skeatk
                statenum_t.S_FATT_ATK8, statenum_t.S_FATT_ATK5, statenum_t.S_FATT_ATK2 -> sfxenum_t.sfx_firsht
                statenum_t.S_CPOS_ATK2, statenum_t.S_CPOS_ATK3, statenum_t.S_CPOS_ATK4 -> sfxenum_t.sfx_shotgn
                statenum_t.S_TROO_ATK3 -> sfxenum_t.sfx_claw
                statenum_t.S_SARG_ATK2 -> sfxenum_t.sfx_sgtatk
                statenum_t.S_BOSS_ATK2, statenum_t.S_BOS2_ATK2, statenum_t.S_HEAD_ATK2 -> sfxenum_t.sfx_firsht
                statenum_t.S_SKULL_ATK2 -> sfxenum_t.sfx_sklatk
                statenum_t.S_SPID_ATK2, statenum_t.S_SPID_ATK3 -> sfxenum_t.sfx_shotgn
                statenum_t.S_BSPI_ATK2 -> sfxenum_t.sfx_plasma
                statenum_t.S_CYBER_ATK2, statenum_t.S_CYBER_ATK4, statenum_t.S_CYBER_ATK6 -> sfxenum_t.sfx_rlaunc
                statenum_t.S_PAIN_ATK3 -> sfxenum_t.sfx_sklatk
                else -> null
            }
            if (sfx != null) { // Fixed mute thanks to _D_ 8/6/2011
                DOOM.doomSound.StartSound(null, sfx)
            }
        }
        if (castframes == 12) {
            // go into attack frame
            castattacking = true
            caststate = if (castonmelee != 0) {
                info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].meleestate.ordinal]
            } else {
                info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].missilestate.ordinal]
            }
            castonmelee = castonmelee xor 1
            if (caststate === info.states[statenum_t.S_NULL.ordinal]) {
                caststate = if (castonmelee != 0) {
                    info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].meleestate.ordinal]
                } else {
                    info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].missilestate.ordinal]
                }
            }
        }
        if (castattacking) {
            if (castframes == 24 || caststate === info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].seestate.ordinal]) {
                stopattack()
            }
        }
        afterstopattack()
    }

    protected fun stopattack() {
        castattacking = false
        castframes = 0
        caststate = info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].seestate.ordinal]
    }

    protected fun afterstopattack() {
        casttics = caststate!!.tics
        if (casttics == -1) {
            casttics = 15
        }
    }

    /**
     * CastResponder
     */
    fun CastResponder(ev: event_t): Boolean {
        if (!ev.isType(evtype_t.ev_keydown)) {
            return false
        }
        if (castdeath) {
            return true // already in dying frames
        }

        // go into death frame
        castdeath = true
        caststate = info.states[info.mobjinfo[castorder[castnum].type!!.ordinal].deathstate.ordinal]
        casttics = caststate!!.tics
        castframes = 0
        castattacking = false
        if (info.mobjinfo[castorder[castnum].type!!.ordinal].deathsound != null) {
            DOOM.doomSound.StartSound(null, info.mobjinfo[castorder[castnum].type!!.ordinal].deathsound)
        }
        return true
    }

    fun CastPrint(text: String) {
        var c: Int
        var width = 0

        // find width
        val ch = text.toCharArray()
        for (i in ch.indices) {
            c = ch[i].code
            if (c == 0) break
            c = c.uppercaseChar() - Defines.HU_FONTSTART
            if (c < 0 || c > Defines.HU_FONTSIZE) {
                width += 4
                continue
            }
            width += hu_font[c].width.toInt()
        }

        // draw it
        var cx = 160 - width / 2
        // ch = text;
        for (i in ch.indices) {
            c = ch[i].code
            if (c == 0) break
            c = c.uppercaseChar() - Defines.HU_FONTSTART
            if (c < 0 || c > Defines.HU_FONTSIZE) {
                cx += 4
                continue
            }
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, hu_font[c], DOOM.vs, cx, 180)
            cx += hu_font[c].width.toInt()
        }
    }

    /**
     * F_CastDrawer
     *
     * @throws IOException
     */
    // public void V_DrawPatchFlipped (int x, int y, int scrn, patch_t patch);
    fun CastDrawer() {
        // erase the entire screen to a background
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            DOOM.wadLoader.CachePatchName("BOSSBACK", Defines.PU_CACHE),
            DOOM.vs,
            0,
            0
        )
        CastPrint(castorder[castnum].name!!)

        // draw the current frame in the middle of the screen
        val sprdef = DOOM.spriteManager.getSprite(caststate!!.sprite!!.ordinal)
        val sprframe = sprdef.spriteframes[caststate!!.frame and Defines.FF_FRAMEMASK]!!
        val lump = sprframe.lump[0]
        val flip = C2JUtils.eval(sprframe.flip[0].toInt())
        // flip=false;
        // lump=0;
        val patch = DOOM.wadLoader.CachePatchNum(lump + DOOM.spriteManager.getFirstSpriteLump())
        if (flip) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                patch,
                DOOM.vs,
                160,
                170,
                DoomGraphicSystem.V_FLIPPEDPATCH
            )
        } else {
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, patch, DOOM.vs, 160, 170)
        }
    }

    protected var laststage = 0

    /**
     * F_BunnyScroll
     */
    fun BunnyScroll() {
        val p1 = DOOM.wadLoader.CachePatchName("PFUB2", Defines.PU_LEVEL)
        val p2 = DOOM.wadLoader.CachePatchName("PFUB1", Defines.PU_LEVEL)

        //V.MarkRect(0, 0, DOOM.vs.getScreenWidth(), DOOM.vs.getScreenHeight());
        var scrolled = 320 - (finalecount - 230) / 2
        if (scrolled > 320) {
            scrolled = 320
        }
        if (scrolled < 0) {
            scrolled = 0
        }
        for (x in 0..319) {
            if (x + scrolled < 320) {
                DOOM.graphicSystem.DrawPatchColScaled(DoomScreen.FG, p1, DOOM.vs, x, x + scrolled)
            } else {
                DOOM.graphicSystem.DrawPatchColScaled(DoomScreen.FG, p2, DOOM.vs, x, x + scrolled - 320)
            }
        }
        if (finalecount < 1130) {
            return
        } else if (finalecount < 1180) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                DOOM.wadLoader.CachePatchName("END0", Defines.PU_CACHE),
                DOOM.vs,
                (320 - 13 * 8) / 2,
                (200 - 8 * 8) / 2
            )
            laststage = 0
            return
        }
        var stage = (finalecount - 1180) / 5
        if (stage > 6) {
            stage = 6
        }
        if (stage > laststage) {
            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_pistol)
            laststage = stage
        }
        val name = "END$stage"
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            DOOM.wadLoader.CachePatchName(name, Defines.PU_CACHE),
            DOOM.vs,
            (320 - 13 * 8) / 2,
            (200 - 8 * 8) / 2
        )
    }

    //
    // F_Drawer
    //
    fun Drawer() {
        if (finalestage == 2) {
            CastDrawer()
            return
        }
        if (finalestage == 0) {
            TextWrite()
        } else {
            when (DOOM.gameepisode) {
                1 -> if (DOOM.isCommercial() || DOOM.isRegistered()) DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG,
                    DOOM.wadLoader.CachePatchName("CREDIT", Defines.PU_CACHE),
                    DOOM.vs,
                    0,
                    0
                ) else  // Fun fact: Registered/Ultimate Doom has no "HELP2" lump.
                    DOOM.graphicSystem.DrawPatchScaled(
                        DoomScreen.FG,
                        DOOM.wadLoader.CachePatchName("HELP2", Defines.PU_CACHE),
                        DOOM.vs,
                        0,
                        0
                    )
                2 -> DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG,
                    DOOM.wadLoader.CachePatchName("VICTORY2", Defines.PU_CACHE),
                    DOOM.vs,
                    0,
                    0
                )
                3 -> BunnyScroll()
                4 -> DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG,
                    DOOM.wadLoader.CachePatchName("ENDPIC", Defines.PU_CACHE),
                    DOOM.vs,
                    0,
                    0
                )
            }
        }
    }

    init {
        hu_font = DOOM.headsUp.getHUFonts()

        //castinfo_t shit = new castinfo_t(CC_ZOMBIE, mobjtype_t.MT_POSSESSED);
        castorder = arrayOf(
            castinfo_t(englsh.CC_ZOMBIE, mobjtype_t.MT_POSSESSED),
            castinfo_t(englsh.CC_SHOTGUN, mobjtype_t.MT_SHOTGUY),
            castinfo_t(englsh.CC_HEAVY, mobjtype_t.MT_CHAINGUY),
            castinfo_t(englsh.CC_IMP, mobjtype_t.MT_TROOP),
            castinfo_t(englsh.CC_DEMON, mobjtype_t.MT_SERGEANT),
            castinfo_t(englsh.CC_LOST, mobjtype_t.MT_SKULL),
            castinfo_t(englsh.CC_CACO, mobjtype_t.MT_HEAD),
            castinfo_t(englsh.CC_HELL, mobjtype_t.MT_KNIGHT),
            castinfo_t(englsh.CC_BARON, mobjtype_t.MT_BRUISER),
            castinfo_t(englsh.CC_ARACH, mobjtype_t.MT_BABY),
            castinfo_t(englsh.CC_PAIN, mobjtype_t.MT_PAIN),
            castinfo_t(englsh.CC_REVEN, mobjtype_t.MT_UNDEAD),
            castinfo_t(englsh.CC_MANCU, mobjtype_t.MT_FATSO),
            castinfo_t(englsh.CC_ARCH, mobjtype_t.MT_VILE),
            castinfo_t(englsh.CC_SPIDER, mobjtype_t.MT_SPIDER),
            castinfo_t(englsh.CC_CYBER, mobjtype_t.MT_CYBORG),
            castinfo_t(englsh.CC_HERO, mobjtype_t.MT_PLAYER),
            castinfo_t(null, null)
        )
    }

    companion object {
        private const val TEXTSPEED = 3
        private const val TEXTWAIT = 250
        val doom_text = arrayOf<String?>(englsh.E1TEXT, englsh.E2TEXT, englsh.E3TEXT, englsh.E4TEXT)
        val doom2_text =
            arrayOf<String?>(englsh.C1TEXT, englsh.C2TEXT, englsh.C3TEXT, englsh.C4TEXT, englsh.C5TEXT, englsh.C6TEXT)
        val plut_text =
            arrayOf<String?>(englsh.P1TEXT, englsh.P2TEXT, englsh.P3TEXT, englsh.P4TEXT, englsh.P5TEXT, englsh.P6TEXT)
        val tnt_text =
            arrayOf<String?>(englsh.T1TEXT, englsh.T2TEXT, englsh.T3TEXT, englsh.T4TEXT, englsh.T5TEXT, englsh.T6TEXT)
    }
}

private fun Int.uppercaseChar(): Int {
    return Character.toUpperCase(this)
}
