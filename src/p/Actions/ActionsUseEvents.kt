/*
 * Copyright (C) 1993-1996 by id Software, Inc.
 * Copyright (C) 2017 Good Sign
 * Copyright (C) 2022 hiperbou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package p.Actions


import data.Defines
import data.Tables
import data.sounds.sfxenum_t
import doom.SourceCode.P_Map
import doom.player_t
import m.fixed_t.Companion.FRACBITS
import p.*
import p.Actions.ActionTrait.Movement
import p.Actions.ActionTrait.Spechits
import rr.line_t
import utils.C2JUtils
import java.util.function.Predicate

interface ActionsUseEvents : ActionTrait {
    fun VerticalDoor(line: line_t, thing: mobj_t)
    fun LightTurnOn(line: line_t, i: Int)
    fun BuildStairs(line: line_t?, stair_e: stair_e?): Boolean
    fun DoDonut(line: line_t): Boolean
    fun DoFloor(line: line_t, floor_e: floor_e): Boolean
    fun DoDoor(line: line_t, vldoor_e: vldoor_e?): Boolean
    fun DoPlat(line: line_t, plattype_e: plattype_e?, i: Int): Boolean
    fun DoCeiling(line: line_t, ceiling_e: ceiling_e): Boolean
    fun DoLockedDoor(line: line_t, vldoor_e: vldoor_e?, thing: mobj_t): Boolean
    fun PathTraverse(x1: Int, y1: Int, x2: Int, y2: Int, flags: Int, trav: Predicate<intercept_t?>): Boolean

    /**
     * P_UseSpecialLine Called when a thing uses a special line. Only the front sides of lines are usable.
     */
    fun UseSpecialLine(thing: mobj_t, line: line_t, side: Boolean): Boolean {
        // Err...
        // Use the back sides of VERY SPECIAL lines...
        if (side) {
            when (line.special.toInt()) {
                124 -> {}
                else -> return false
            }
        }

        // Switches that other things can activate.
        //_D_: little bug fixed here, see linuxdoom source
        if (thing.player ==  /*!=*/null) {
            // never open secret doors
            if (C2JUtils.eval(line.flags.toInt() and line_t.ML_SECRET)) {
                return false
            }
            when (line.special.toInt()) {
                1, 32, 33, 34 -> {}
                else -> return false
            }
        }
        when (line.special.toInt()) {
            1, 26, 27, 28, 31, 32, 33, 34, 117, 118 -> VerticalDoor(line, thing)
            124 -> {}
            7 ->                 // Build Stairs
                if (BuildStairs(line, stair_e.build8)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            9 ->                 // Change Donut
                if (DoDonut(line)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            11 -> {
                // Exit level
                switches.ChangeSwitchTexture(line, false)
                DOOM().ExitLevel()
            }
            14 ->                 // Raise Floor 32 and change texture
                if (DoPlat(line, plattype_e.raiseAndChange, 32)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            15 ->                 // Raise Floor 24 and change texture
                if (DoPlat(line, plattype_e.raiseAndChange, 24)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            18 ->                 // Raise Floor to next highest floor
                if (DoFloor(line, floor_e.raiseFloorToNearest)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            20 ->                 // Raise Plat next highest floor and change texture
                if (DoPlat(line, plattype_e.raiseToNearestAndChange, 0)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            21 ->                 // PlatDownWaitUpStay
                if (DoPlat(line, plattype_e.downWaitUpStay, 0)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            23 ->                 // Lower Floor to Lowest
                if (DoFloor(line, floor_e.lowerFloorToLowest)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            29 ->                 // Raise Door
                if (DoDoor(line, vldoor_e.normal)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            41 ->                 // Lower Ceiling to Floor
                if (DoCeiling(line, ceiling_e.lowerToFloor)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            71 ->                 // Turbo Lower Floor
                if (DoFloor(line, floor_e.turboLower)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            49 ->                 // Ceiling Crush And Raise
                if (DoCeiling(line, ceiling_e.crushAndRaise)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            50 ->                 // Close Door
                if (DoDoor(line, vldoor_e.close)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            51 -> {
                // Secret EXIT
                switches.ChangeSwitchTexture(line, false)
                DOOM().SecretExitLevel()
            }
            55 ->                 // Raise Floor Crush
                if (DoFloor(line, floor_e.raiseFloorCrush)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            101 ->                 // Raise Floor
                if (DoFloor(line, floor_e.raiseFloor)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            102 ->                 // Lower Floor to Surrounding floor height
                if (DoFloor(line, floor_e.lowerFloor)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            103 ->                 // Open Door
                if (DoDoor(line, vldoor_e.open)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            111 ->                 // Blazing Door Raise (faster than TURBO!)
                if (DoDoor(line, vldoor_e.blazeRaise)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            112 ->                 // Blazing Door Open (faster than TURBO!)
                if (DoDoor(line, vldoor_e.blazeOpen)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            113 ->                 // Blazing Door Close (faster than TURBO!)
                if (DoDoor(line, vldoor_e.blazeClose)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            122 ->                 // Blazing PlatDownWaitUpStay
                if (DoPlat(line, plattype_e.blazeDWUS, 0)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            127 ->                 // Build Stairs Turbo 16
                if (BuildStairs(line, stair_e.turbo16)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            131 ->                 // Raise Floor Turbo
                if (DoFloor(line, floor_e.raiseFloorTurbo)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            133, 135, 137 ->                 // BlzOpenDoor YELLOW
                if (DoLockedDoor(line, vldoor_e.blazeOpen, thing)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            140 ->                 // Raise Floor 512
                if (DoFloor(line, floor_e.raiseFloor512)) {
                    switches.ChangeSwitchTexture(line, false)
                }
            42 ->                 // Close Door
                if (DoDoor(line, vldoor_e.close)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            43 ->                 // Lower Ceiling to Floor
                if (DoCeiling(line, ceiling_e.lowerToFloor)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            45 ->                 // Lower Floor to Surrounding floor height
                if (DoFloor(line, floor_e.lowerFloor)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            60 ->                 // Lower Floor to Lowest
                if (DoFloor(line, floor_e.lowerFloorToLowest)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            61 ->                 // Open Door
                if (DoDoor(line, vldoor_e.open)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            62 ->                 // PlatDownWaitUpStay
                if (DoPlat(line, plattype_e.downWaitUpStay, 1)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            63 ->                 // Raise Door
                if (DoDoor(line, vldoor_e.normal)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            64 ->                 // Raise Floor to ceiling
                if (DoFloor(line, floor_e.raiseFloor)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            66 ->                 // Raise Floor 24 and change texture
                if (DoPlat(line, plattype_e.raiseAndChange, 24)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            67 ->                 // Raise Floor 32 and change texture
                if (DoPlat(line, plattype_e.raiseAndChange, 32)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            65 ->                 // Raise Floor Crush
                if (DoFloor(line, floor_e.raiseFloorCrush)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            68 ->                 // Raise Plat to next highest floor and change texture
                if (DoPlat(line, plattype_e.raiseToNearestAndChange, 0)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            69 ->                 // Raise Floor to next highest floor
                if (DoFloor(line, floor_e.raiseFloorToNearest)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            70 ->                 // Turbo Lower Floor
                if (DoFloor(line, floor_e.turboLower)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            114 ->                 // Blazing Door Raise (faster than TURBO!)
                if (DoDoor(line, vldoor_e.blazeRaise)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            115 ->                 // Blazing Door Open (faster than TURBO!)
                if (DoDoor(line, vldoor_e.blazeOpen)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            116 ->                 // Blazing Door Close (faster than TURBO!)
                if (DoDoor(line, vldoor_e.blazeClose)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            123 ->                 // Blazing PlatDownWaitUpStay
                if (DoPlat(line, plattype_e.blazeDWUS, 0)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            132 ->                 // Raise Floor Turbo
                if (DoFloor(line, floor_e.raiseFloorTurbo)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            99, 134, 136 ->                 // BlzOpenDoor YELLOW
                if (DoLockedDoor(line, vldoor_e.blazeOpen, thing)) {
                    switches.ChangeSwitchTexture(line, true)
                }
            138 -> {
                // Light Turn On
                LightTurnOn(line, 255)
                switches.ChangeSwitchTexture(line, true)
            }
            139 -> {
                // Light Turn Off
                LightTurnOn(line, 35)
                switches.ChangeSwitchTexture(line, true)
            }
        }
        return true
    }

    /**
     * P_UseLines Looks for special lines in front of the player to activate.
     */
    fun UseLines(player: player_t) {
        val sp = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        val angle: Int
        val x1: Int
        val y1: Int
        val x2: Int
        val y2: Int
        //System.out.println("Uselines");
        sp.usething = player.mo

        // Normally this shouldn't cause problems?
        angle = Tables.toBAMIndex(player.mo!!.angle)
        x1 = player.mo!!._x
        y1 = player.mo!!._y
        x2 = x1 + (Defines.USERANGE shr FRACBITS) * Tables.finecosine[angle]
        y2 = y1 + (Defines.USERANGE shr FRACBITS) * Tables.finesine[angle]
        PathTraverse(x1, y1, x2, y2, Defines.PT_ADDLINES) { `in`: intercept_t? -> UseTraverse(`in`) }
    }

    //
    // USE LINES
    //
    @P_Map.C(P_Map.PTR_UseTraverse)
    fun UseTraverse(`in`: intercept_t?): Boolean {
        val mov = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val sp = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        var side: Boolean
        // FIXME: some sanity check here?
        val line = `in`!!.d() as line_t
        if (line.special.toInt() == 0) {
            LineOpening(line)
            if (mov.openrange <= 0) {
                StartSound(sp.usething, sfxenum_t.sfx_noway)

                // can't use through a wall
                return false
            }
            // not a special line, but keep checking
            return true
        }
        side = false
        if (line.PointOnLineSide(sp.usething!!._x, sp.usething!!._y)) {
            side = true
        }

        //  return false;       // don't use back side
        UseSpecialLine(sp.usething!!, line, side)

        // can't use for than one special line in a row
        return false
    }
}