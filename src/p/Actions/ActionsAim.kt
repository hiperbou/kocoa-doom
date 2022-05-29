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
import doom.SourceCode.P_Map
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedDiv
import m.fixed_t.Companion.FixedMul
import p.Actions.ActionTrait.Movement
import p.Actions.ActionsSectors.Spawn
import p.intercept_t
import p.mobj_t
import rr.line_t
import utils.C2JUtils

interface ActionsAim : ActionsMissiles {
    /**
     * P_AimLineAttack
     *
     * @param t1
     * @param angle long
     * @param distance int
     */
    override fun AimLineAttack(t1: mobj_t, angle: Long, distance: Int): Int {
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val x2: Int
        val y2: Int
        targ.shootthing = t1
        x2 = t1._x + (distance shr FRACBITS) * Tables.finecosine(angle)
        y2 = t1._y + (distance shr  FRACBITS) * Tables.finesine(angle)
        targ.shootz = t1._z + (t1.height shr 1) + 8 *  FRACUNIT

        // can't shoot outside view angles
        targ.topslope = 100 *  FRACUNIT / 160
        targ.bottomslope = -100 *  FRACUNIT / 160
        targ.attackrange = distance
        targ.linetarget = null
        PathTraverse(
            t1._x,
            t1._y,
            x2,
            y2,
            Defines.PT_ADDLINES or Defines.PT_ADDTHINGS
        ) { `in` -> AimTraverse(`in`!!) }
        return if (targ.linetarget != null) {
            targ.aimslope
        } else 0
    }

    //
    // P_BulletSlope
    // Sets a slope so a near miss is at aproximately
    // the height of the intended target
    //
    fun P_BulletSlope(mo: mobj_t) {
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        var an: Long

        // see which target is to be aimed at
        // FIXME: angle can already be negative here.
        // Not a problem if it's just moving about (accumulation will work)
        // but it needs to be sanitized before being used in any function.
        an = mo.angle
        //_D_: &BITS32 will be used later in this function, by fine(co)sine()
        targ.bulletslope = AimLineAttack(mo, an /*&BITS32*/, 16 * 64 *  FRACUNIT)
        if (!C2JUtils.eval(targ.linetarget)) {
            an += (1 shl 26).toLong()
            targ.bulletslope = AimLineAttack(mo, an /*&BITS32*/, 16 * 64 *  FRACUNIT)
            if (!C2JUtils.eval(targ.linetarget)) {
                an -= (2 shl 26).toLong()
                targ.bulletslope = AimLineAttack(mo, an /*&BITS32*/, 16 * 64 *  FRACUNIT)
            }

            // Give it one more try, with freelook
            if (mo.player!!.lookdir != 0 && !C2JUtils.eval(targ.linetarget)) {
                an += (2 shl 26).toLong()
                an = an and Tables.BITS32
                targ.bulletslope = (mo.player!!.lookdir shl  FRACBITS) / 173
            }
        }
    }

    ////////////////// PTR Traverse Interception Functions ///////////////////////
    // Height if not aiming up or down
    // ???: use slope for monsters?
    @P_Map.C(P_Map.PTR_AimTraverse)
    fun AimTraverse(`in`: intercept_t): Boolean {
        val mov = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val li: line_t
        val th: mobj_t
        var slope: Int
        var thingtopslope: Int
        var thingbottomslope: Int
        val dist: Int
        if (`in`.isaline) {
            li = `in`.d() as line_t
            if (!C2JUtils.eval(li.flags.toInt() and line_t.ML_TWOSIDED)) {
                return false // stop
            }
            // Crosses a two sided line.
            // A two sided line will restrict
            // the possible target ranges.
            LineOpening(li)
            if (mov.openbottom >= mov.opentop) {
                return false // stop
            }
            dist = FixedMul(targ.attackrange, `in`.frac)
            if (li.frontsector!!.floorheight != li.backsector!!.floorheight) {
                slope = FixedDiv(mov.openbottom - targ.shootz, dist)
                if (slope > targ.bottomslope) {
                    targ.bottomslope = slope
                }
            }
            if (li.frontsector!!.ceilingheight != li.backsector!!.ceilingheight) {
                slope = FixedDiv(mov.opentop - targ.shootz, dist)
                if (slope < targ.topslope) {
                    targ.topslope = slope
                }
            }

            // determine whether shot continues
            return targ.topslope > targ.bottomslope
        }

        // shoot a thing
        th = `in`.d() as mobj_t
        if (th === targ.shootthing) {
            return true // can't shoot self
        }
        if (!C2JUtils.eval(th.flags and mobj_t.MF_SHOOTABLE)) {
            return true // corpse or something
        }
        // check angles to see if the thing can be aimed at
        dist = FixedMul(targ.attackrange, `in`.frac)
        thingtopslope = FixedDiv(th._z + th.height - targ.shootz, dist)
        if (thingtopslope < targ.bottomslope) {
            return true // shot over the thing
        }
        thingbottomslope = FixedDiv(th._z - targ.shootz, dist)
        if (thingbottomslope > targ.topslope) {
            return true // shot under the thing
        }
        // this thing can be hit!
        if (thingtopslope > targ.topslope) {
            thingtopslope = targ.topslope
        }
        if (thingbottomslope < targ.bottomslope) {
            thingbottomslope = targ.bottomslope
        }
        targ.aimslope = (thingtopslope + thingbottomslope) / 2
        targ.linetarget = th
        return false // don't go any farther
    }
}