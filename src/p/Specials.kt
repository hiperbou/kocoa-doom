package p


import doom.player_t
import m.fixed_t
import p.Actions.ActionsLights.glow_t
import p.Actions.ActionsLights.lightflash_t
import p.mobj_t
import rr.line_t
import rr.sector_t
import rr.side_t

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Specials.java,v 1.7 2011/06/01 00:09:08 velktron Exp $
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
// DESCRIPTION:  none
//	Implements special effects:
//	Texture animation, height or lighting changes
//	 according to adjacent sectors, respective
//	 utility functions, etc.
//
//-----------------------------------------------------------------------------
interface Specials {
    // at game start
    fun P_InitPicAnims()

    // at map load
    fun P_SpawnSpecials()

    // every tic
    fun P_UpdateSpecials()

    // when needed
    fun P_UseSpecialLine(
        thing: mobj_t?,
        line: line_t?,
        side: Int
    ): Boolean

    fun P_ShootSpecialLine(
        thing: mobj_t?,
        line: line_t?
    )

    fun P_CrossSpecialLine(
        linenum: Int,
        side: Int,
        thing: mobj_t?
    )

    fun P_PlayerInSpecialSector(player: player_t?)
    fun twoSided(
        sector: Int,
        line: Int
    ): Int

    fun getSector(
        currentSector: Int,
        line: Int,
        side: Int
    ): sector_t?

    fun getSide(
        currentSector: Int,
        line: Int,
        side: Int
    ): side_t?

    fun P_FindLowestFloorSurrounding(sec: sector_t?): fixed_t?
    fun P_FindHighestFloorSurrounding(sec: sector_t?): fixed_t?
    fun P_FindNextHighestFloor(
        sec: sector_t?,
        currentheight: Int
    ): fixed_t?

    fun P_FindLowestCeilingSurrounding(sec: sector_t?): fixed_t?
    fun P_FindHighestCeilingSurrounding(sec: sector_t?): fixed_t?
    fun P_FindSectorFromLineTag(
        line: line_t?,
        start: Int
    ): Int

    fun P_FindMinSurroundingLight(
        sector: sector_t?,
        max: Int
    ): Int

    fun getNextSector(
        line: line_t?,
        sec: sector_t?
    ): sector_t?

    //
    // SPECIAL
    //
    fun EV_DoDonut(line: line_t?): Int
    fun P_SpawnFireFlicker(sector: sector_t?)
    fun T_LightFlash(flash: lightflash_t?)
    fun P_SpawnLightFlash(sector: sector_t?)
    fun T_StrobeFlash(flash: strobe_t?)
    fun P_SpawnStrobeFlash(
        sector: sector_t?,
        fastOrSlow: Int,
        inSync: Int
    )

    fun EV_StartLightStrobing(line: line_t?)
    fun EV_TurnTagLightsOff(line: line_t?)
    fun EV_LightTurnOn(
        line: line_t?,
        bright: Int
    )

    fun T_Glow(g: glow_t?)
    fun P_SpawnGlowingLight(sector: sector_t?)

    //extern button_t	buttonlist[MAXBUTTONS]; 
    fun P_ChangeSwitchTexture(
        line: line_t?,
        useAgain: Int
    )

    fun P_InitSwitchList()

    //extern plat_t*	activeplats[MAXPLATS];
    fun T_PlatRaise(plat: plat_t?)
    fun EV_DoPlat(
        line: line_t?,
        type: plattype_e?,
        amount: Int
    ): Int

    fun P_AddActivePlat(plat: plat_t?)
    fun P_RemoveActivePlat(plat: plat_t?)
    fun EV_StopPlat(line: line_t?)
    fun P_ActivateInStasis(tag: Int)
    fun EV_VerticalDoor(
        line: line_t?,
        thing: mobj_t?
    )

    fun EV_DoDoor(
        line: line_t?,
        type: vldoor_e?
    ): Int

    fun EV_DoLockedDoor(
        line: line_t?,
        type: vldoor_e?,
        thing: mobj_t?
    ): Int

    fun T_VerticalDoor(door: vldoor_t?)
    fun P_SpawnDoorCloseIn30(sec: sector_t?)
    fun P_SpawnDoorRaiseIn5Mins(
        sec: sector_t?,
        secnum: Int
    )

    companion object {
        //
        // End-level timer (-TIMER option)
        //
        //extern	boolean levelTimer;
        //extern	int	levelTimeCount;
        //      Define values for map objects
        const val MO_TELEPORTMAN = 14
        const val GLOWSPEED = 8
        const val STROBEBRIGHT = 5
        const val FASTDARK = 15
        const val SLOWDARK = 35

        // max # of wall switches in a level
        const val MAXSWITCHES = 50

        // 4 players, 4 buttons each at once, max.
        const val MAXBUTTONS = 16

        // 1 second, in ticks. 
        const val BUTTONTIME = 35
        const val PLATWAIT = 3
        val PLATSPEED: Int = fixed_t.FRACUNIT
        const val MAXPLATS = 30
        val VDOORSPEED: Int = fixed_t.FRACUNIT * 2
        const val VDOORWAIT = 150
    }
}