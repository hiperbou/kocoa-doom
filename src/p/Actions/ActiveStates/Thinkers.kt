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
package p.Actions.ActiveStatesimport

import doom.SourceCode
import doom.SourceCode.P_Lights
import doom.thinker_t
import p.*
import p.Actions.ActionTrait
import p.Actions.ActionsLights.*
import p.DoorDefines.GLOWSPEED

interface Thinkers : ActionTrait {
    //
    // T_FireFlicker
    //
    @SourceCode.Exact
    @P_Lights.C(P_Lights.T_FireFlicker)
    fun T_FireFlicker(f: thinker_t) {
        val flick = f as fireflicker_t
        val amount: Int
        if (--flick.count != 0) {
            return
        }
        amount = (P_Random() and 3) * 16
        if (flick.sector!!.lightlevel - amount < flick.minlight) {
            flick.sector!!.lightlevel = flick.minlight.toShort()
        } else {
            flick.sector!!.lightlevel = (flick.maxlight - amount).toShort()
        }
        flick.count = 4
    }

    /**
     * T_LightFlash
     * Do flashing lights.
     */
    @SourceCode.Exact
    @P_Lights.C(P_Lights.T_LightFlash)
    fun T_LightFlash(l: thinker_t) {
        val flash = l as lightflash_t
        if (--flash.count != 0) {
            return
        }
        if (flash.sector!!.lightlevel.toInt() == flash.maxlight) {
            flash.sector!!.lightlevel = flash.minlight.toShort()
            flash.count = (P_Random() and flash.mintime) + 1
        } else {
            flash.sector!!.lightlevel = flash.maxlight.toShort()
            flash.count = (P_Random() and flash.maxtime) + 1
        }
    }

    fun T_StrobeFlash(s: thinker_t) {
        (s as strobe_t).StrobeFlash()
    }

    //
    // Spawn glowing light
    //
    @SourceCode.Exact
    @P_Lights.C(P_Lights.T_Glow)
    fun T_Glow(t: thinker_t) {
        val g = t as glow_t
        when (g.direction) {
            -1 -> {
                // DOWN
                g.sector!!.lightlevel = (g.sector!!.lightlevel - GLOWSPEED.toShort()).toShort()
                if (g.sector!!.lightlevel <= g.minlight) {
                    g.sector!!.lightlevel = (g.sector!!.lightlevel + GLOWSPEED.toShort()).toShort()
                    g.direction = 1
                }
            }
            1 -> {
                // UP
                g.sector!!.lightlevel = (g.sector!!.lightlevel + GLOWSPEED.toShort()).toShort()
                if (g.sector!!.lightlevel >= g.maxlight) {
                    g.sector!!.lightlevel = (g.sector!!.lightlevel - GLOWSPEED.toShort()).toShort()
                    g.direction = -1
                }
            }
            else -> {}
        }
    }

    fun T_MoveCeiling(c: thinker_t?) {
        thinkers.MoveCeiling(c as ceiling_t)
    }

    fun T_MoveFloor(f: thinker_t?) {
        thinkers.MoveFloor(f as floormove_t)
    }

    fun T_VerticalDoor(v: thinker_t?) {
        thinkers.VerticalDoor(v as vldoor_t)
    }

    fun T_SlidingDoor(door: thinker_t?) {
        thinkers.SlidingDoor(door as slidedoor_t)
    }

    fun T_PlatRaise(p: thinker_t?) {
        thinkers.PlatRaise(p as plat_t)
    }

    fun nop(vararg o: Any) {}
}