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


import m.fixed_t
import p.*
import p.Actions.ActionsSectors.Spawn
import rr.line_t

interface ActionsShootEvents : ActionsSpawns {
    /**
     * P_ShootSpecialLine - IMPACT SPECIALS Called when a thing shoots a special line.
     */
    fun ShootSpecialLine(thing: mobj_t, line: line_t) {
        val sw = switches
        var ok: Boolean

        //  Impacts that other things can activate.
        if (thing.player == null) {
            ok = false
            when (line.special.toInt()) {
                46 ->                     // OPEN DOOR IMPACT
                    ok = true
            }
            if (!ok) {
                return
            }
        }
        when (line.special.toInt()) {
            24 -> {
                // RAISE FLOOR
                DoFloor(line, floor_e.raiseFloor)
                sw.ChangeSwitchTexture(line, false)
            }
            46 -> {
                // OPEN DOOR
                DoDoor(line, vldoor_e.open)
                sw.ChangeSwitchTexture(line, true)
            }
            47 -> {
                // RAISE FLOOR NEAR AND CHANGE
                DoPlat(line, plattype_e.raiseToNearestAndChange, 0)
                sw.ChangeSwitchTexture(line, false)
            }
        }
    }

    //_D_: NOTE: this function was added, because replacing a goto by a boolean flag caused a bug if shooting a single sided line
    fun gotoHitLine(`in`: intercept_t, li: line_t): Boolean {
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val x: Int
        val y: Int
        val z: Int
        val frac: Int

        // position a bit closer
        frac = `in`.frac - fixed_t.FixedDiv(4 * fixed_t.FRACUNIT, targ.attackrange)
        x = targ.trace.x + fixed_t.FixedMul(targ.trace.dx, frac)
        y = targ.trace.y + fixed_t.FixedMul(targ.trace.dy, frac)
        z = targ.shootz + fixed_t.FixedMul(targ.aimslope, fixed_t.FixedMul(frac, targ.attackrange))
        if (li.frontsector!!.ceilingpic.toInt() == DOOM().textureManager.getSkyFlatNum()) {
            // don't shoot the sky!
            if (z > li.frontsector!!.ceilingheight) {
                return false
            }

            // it's a sky hack wall
            if (li.backsector != null && li.backsector!!.ceilingpic.toInt() == DOOM().textureManager.getSkyFlatNum()) {
                return false
            }
        }

        // Spawn bullet puffs.
        SpawnPuff(x, y, z)

        // don't go any farther
        return false
    }
}