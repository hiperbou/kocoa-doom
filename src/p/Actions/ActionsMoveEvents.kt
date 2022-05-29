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

import data.mobjtype_t
import p.*
import rr.line_t

interface ActionsMoveEvents : ActionTrait {
    fun DoDoor(line: line_t, type: vldoor_e?): Boolean
    fun DoFloor(line: line_t, floor_e: floor_e): Boolean
    fun DoPlat(line: line_t, plattype_e: plattype_e?, i: Int): Boolean
    fun BuildStairs(line: line_t?, stair_e: stair_e?): Boolean
    fun DoCeiling(line: line_t, ceiling_e: ceiling_e): Boolean
    fun StopPlat(line: line_t)
    fun LightTurnOn(line: line_t, i: Int)
    fun StartLightStrobing(line: line_t)
    fun TurnTagLightsOff(line: line_t)
    fun Teleport(line: line_t, side: Int, thing: mobj_t): Int
    fun CeilingCrushStop(line: line_t): Int
    //
    //EVENTS
    //Events are operations triggered by using, crossing,
    //or shooting special lines, or by timed thinkers.
    //
    /**
     * P_CrossSpecialLine - TRIGGER Called every time a thing origin is about to cross a line with a non 0 special.
     */
    fun CrossSpecialLine(line: line_t, side: Int, thing: mobj_t) {
        //line_t line;
        var ok: Boolean

        //line = LL.lines[linenum];
        //  Triggers that other things can activate
        if (thing.player == null) {
            // Things that should NOT trigger specials...
            when (thing.type) {
                mobjtype_t.MT_ROCKET, mobjtype_t.MT_PLASMA, mobjtype_t.MT_BFG, mobjtype_t.MT_TROOPSHOT, mobjtype_t.MT_HEADSHOT, mobjtype_t.MT_BRUISERSHOT -> return
                else -> {}
            }
            ok = false
            when (line.special.toInt()) {
                39, 97, 125, 126, 4, 10, 88 -> ok = true
            }
            if (!ok) {
                return
            }
        }
        when (line.special.toInt()) {
            2 -> {
                // Open Door
                DoDoor(line, vldoor_e.open)
                line.special = 0
            }
            3 -> {
                // Close Door
                DoDoor(line, vldoor_e.close)
                line.special = 0
            }
            4 -> {
                // Raise Door
                DoDoor(line, vldoor_e.normal)
                line.special = 0
            }
            5 -> {
                // Raise Floor
                DoFloor(line, floor_e.raiseFloor)
                line.special = 0
            }
            6 -> {
                // Fast Ceiling Crush & Raise
                DoCeiling(line, ceiling_e.fastCrushAndRaise)
                line.special = 0
            }
            8 -> {
                // Build Stairs
                BuildStairs(line, stair_e.build8)
                line.special = 0
            }
            10 -> {
                // PlatDownWaitUp
                DoPlat(line, plattype_e.downWaitUpStay, 0)
                line.special = 0
            }
            12 -> {
                // Light Turn On - brightest near
                LightTurnOn(line, 0)
                line.special = 0
            }
            13 -> {
                // Light Turn On 255
                LightTurnOn(line, 255)
                line.special = 0
            }
            16 -> {
                // Close Door 30
                DoDoor(line, vldoor_e.close30ThenOpen)
                line.special = 0
            }
            17 -> {
                // Start Light Strobing
                StartLightStrobing(line)
                line.special = 0
            }
            19 -> {
                // Lower Floor
                DoFloor(line, floor_e.lowerFloor)
                line.special = 0
            }
            22 -> {
                // Raise floor to nearest height and change texture
                DoPlat(line, plattype_e.raiseToNearestAndChange, 0)
                line.special = 0
            }
            25 -> {
                // Ceiling Crush and Raise
                DoCeiling(line, ceiling_e.crushAndRaise)
                line.special = 0
            }
            30 -> {
                // Raise floor to shortest texture height
                //  on either side of lines.
                DoFloor(line, floor_e.raiseToTexture)
                line.special = 0
            }
            35 -> {
                // Lights Very Dark
                LightTurnOn(line, 35)
                line.special = 0
            }
            36 -> {
                // Lower Floor (TURBO)
                DoFloor(line, floor_e.turboLower)
                line.special = 0
            }
            37 -> {
                // LowerAndChange
                DoFloor(line, floor_e.lowerAndChange)
                line.special = 0
            }
            38 -> {
                // Lower Floor To Lowest
                DoFloor(line, floor_e.lowerFloorToLowest)
                line.special = 0
            }
            39 -> {
                // TELEPORT!
                Teleport(line, side, thing)
                line.special = 0
            }
            40 -> {
                // RaiseCeilingLowerFloor
                DoCeiling(line, ceiling_e.raiseToHighest)
                DoFloor(line, floor_e.lowerFloorToLowest)
                line.special = 0
            }
            44 -> {
                // Ceiling Crush
                DoCeiling(line, ceiling_e.lowerAndCrush)
                line.special = 0
            }
            52 ->                 // EXIT!
                DOOM().ExitLevel()
            53 -> {
                // Perpetual Platform Raise
                DoPlat(line, plattype_e.perpetualRaise, 0)
                line.special = 0
            }
            54 -> {
                // Platform Stop
                StopPlat(line)
                line.special = 0
            }
            56 -> {
                // Raise Floor Crush
                DoFloor(line, floor_e.raiseFloorCrush)
                line.special = 0
            }
            57 -> {
                // Ceiling Crush Stop
                CeilingCrushStop(line)
                line.special = 0
            }
            58 -> {
                // Raise Floor 24
                DoFloor(line, floor_e.raiseFloor24)
                line.special = 0
            }
            59 -> {
                // Raise Floor 24 And Change
                DoFloor(line, floor_e.raiseFloor24AndChange)
                line.special = 0
            }
            104 -> {
                // Turn lights off in sector(tag)
                TurnTagLightsOff(line)
                line.special = 0
            }
            108 -> {
                // Blazing Door Raise (faster than TURBO!)
                DoDoor(line, vldoor_e.blazeRaise)
                line.special = 0
            }
            109 -> {
                // Blazing Door Open (faster than TURBO!)
                DoDoor(line, vldoor_e.blazeOpen)
                line.special = 0
            }
            100 -> {
                // Build Stairs Turbo 16
                BuildStairs(line, stair_e.turbo16)
                line.special = 0
            }
            110 -> {
                // Blazing Door Close (faster than TURBO!)
                DoDoor(line, vldoor_e.blazeClose)
                line.special = 0
            }
            119 -> {
                // Raise floor to nearest surr. floor
                DoFloor(line, floor_e.raiseFloorToNearest)
                line.special = 0
            }
            121 -> {
                // Blazing PlatDownWaitUpStay
                DoPlat(line, plattype_e.blazeDWUS, 0)
                line.special = 0
            }
            124 ->                 // Secret EXIT
                DOOM().SecretExitLevel()
            125 ->                 // TELEPORT MonsterONLY
                if (thing.player == null) {
                    Teleport(line, side, thing)
                    line.special = 0
                }
            130 -> {
                // Raise Floor Turbo
                DoFloor(line, floor_e.raiseFloorTurbo)
                line.special = 0
            }
            141 -> {
                // Silent Ceiling Crush & Raise
                DoCeiling(line, ceiling_e.silentCrushAndRaise)
                line.special = 0
            }
            72 ->                 // Ceiling Crush
                DoCeiling(line, ceiling_e.lowerAndCrush)
            73 ->                 // Ceiling Crush and Raise
                DoCeiling(line, ceiling_e.crushAndRaise)
            74 ->                 // Ceiling Crush Stop
                CeilingCrushStop(line)
            75 ->                 // Close Door
                DoDoor(line, vldoor_e.close)
            76 ->                 // Close Door 30
                DoDoor(line, vldoor_e.close30ThenOpen)
            77 ->                 // Fast Ceiling Crush & Raise
                DoCeiling(line, ceiling_e.fastCrushAndRaise)
            79 ->                 // Lights Very Dark
                LightTurnOn(line, 35)
            80 ->                 // Light Turn On - brightest near
                LightTurnOn(line, 0)
            81 ->                 // Light Turn On 255
                LightTurnOn(line, 255)
            82 ->                 // Lower Floor To Lowest
                DoFloor(line, floor_e.lowerFloorToLowest)
            83 ->                 // Lower Floor
                DoFloor(line, floor_e.lowerFloor)
            84 ->                 // LowerAndChange
                DoFloor(line, floor_e.lowerAndChange)
            86 ->                 // Open Door
                DoDoor(line, vldoor_e.open)
            87 ->                 // Perpetual Platform Raise
                DoPlat(line, plattype_e.perpetualRaise, 0)
            88 ->                 // PlatDownWaitUp
                DoPlat(line, plattype_e.downWaitUpStay, 0)
            89 ->                 // Platform Stop
                StopPlat(line)
            90 ->                 // Raise Door
                DoDoor(line, vldoor_e.normal)
            91 ->                 // Raise Floor
                DoFloor(line, floor_e.raiseFloor)
            92 ->                 // Raise Floor 24
                DoFloor(line, floor_e.raiseFloor24)
            93 ->                 // Raise Floor 24 And Change
                DoFloor(line, floor_e.raiseFloor24AndChange)
            94 ->                 // Raise Floor Crush
                DoFloor(line, floor_e.raiseFloorCrush)
            95 ->                 // Raise floor to nearest height
                // and change texture.
                DoPlat(line, plattype_e.raiseToNearestAndChange, 0)
            96 ->                 // Raise floor to shortest texture height
                // on either side of lines.
                DoFloor(line, floor_e.raiseToTexture)
            97 ->                 // TELEPORT!
                Teleport(line, side, thing)
            98 ->                 // Lower Floor (TURBO)
                DoFloor(line, floor_e.turboLower)
            105 ->                 // Blazing Door Raise (faster than TURBO!)
                DoDoor(line, vldoor_e.blazeRaise)
            106 ->                 // Blazing Door Open (faster than TURBO!)
                DoDoor(line, vldoor_e.blazeOpen)
            107 ->                 // Blazing Door Close (faster than TURBO!)
                DoDoor(line, vldoor_e.blazeClose)
            120 ->                 // Blazing PlatDownWaitUpStay.
                DoPlat(line, plattype_e.blazeDWUS, 0)
            126 ->                 // TELEPORT MonsterONLY.
                if (thing.player == null) {
                    Teleport(line, side, thing)
                }
            128 ->                 // Raise To Nearest Floor
                DoFloor(line, floor_e.raiseFloorToNearest)
            129 ->                 // Raise Floor Turbo
                DoFloor(line, floor_e.raiseFloorTurbo)
        }
    }
}