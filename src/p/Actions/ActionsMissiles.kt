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

import data.Tables
import data.info
import data.mobjtype_t
import doom.SourceCode.angle_t
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedMul
import p.Actions.ActionsSectors.Spawn
import p.MapUtils
import p.mobj_t
import utils.C2JUtils

interface ActionsMissiles : ActionsMobj {
    fun AimLineAttack(source: mobj_t, an: Long, i: Int): Int

    /**
     * P_CheckMissileSpawn Moves the missile forward a bit and possibly explodes it right there.
     *
     * @param th
     */
    fun CheckMissileSpawn(th: mobj_t) {
        th.mobj_tics -= (P_Random() and 3).toLong()
        if (th.mobj_tics < 1) {
            th.mobj_tics = 1
        }

        // move a little forward so an angle can
        // be computed if it immediately explodes
        th._x += th.momx shr 1
        th._y += th.momy shr 1
        th._z += th.momz shr 1

        if (!TryMove(th, th._x, th._y)) {
            ExplodeMissile(th)
        }
    }

    /**
     * P_SpawnMissile
     */
    fun SpawnMissile(source: mobj_t, dest: mobj_t, type: mobjtype_t?): mobj_t? {
        val th: mobj_t
        @angle_t var an: Long
        var dist: Int
        th = SpawnMobj(source._x, source._y, source._z + 4 * 8 * FRACUNIT, type!!)
        if (th.info!!.seesound != null) {
            StartSound(th, th.info!!.seesound)
        }
        th.target = source // where it came from
        an = sceneRenderer().PointToAngle2(source._x, source._y, dest._x, dest._y) and Tables.BITS32

        // fuzzy player
        if (C2JUtils.eval(dest.flags and mobj_t.MF_SHADOW)) {
            an += (P_Random() - P_Random() shl 20).toLong()
        }
        th.angle = an and Tables.BITS32
        //an >>= ANGLETOFINESHIFT;
        th.momx = FixedMul(th.info!!.speed, Tables.finecosine(an))
        th.momy = FixedMul(th.info!!.speed, Tables.finesine(an))
        dist = MapUtils.AproxDistance(dest._x - source._x, dest._y - source._y)
        dist /= th.info!!.speed
        if (dist < 1) {
            dist = 1
        }
        th.momz = (dest._z - source._z) / dist
        CheckMissileSpawn(th)
        return th
    }

    /**
     * P_SpawnPlayerMissile Tries to aim at a nearby monster
     */
    fun SpawnPlayerMissile(source: mobj_t, type: mobjtype_t?) {
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val th: mobj_t
        @angle_t var an: Long
        val x: Int
        val y: Int
        val z: Int
        var slope: Int // ActionFunction

        // see which target is to be aimed at
        an = source.angle
        slope = AimLineAttack(source, an, 16 * 64 * FRACUNIT)
        if (targ.linetarget == null) {
            an += (1 shl 26).toLong()
            an = an and Tables.BITS32
            slope = AimLineAttack(source, an, 16 * 64 * FRACUNIT)
            if (targ.linetarget == null) {
                an -= (2 shl 26).toLong()
                an = an and Tables.BITS32
                slope = AimLineAttack(source, an, 16 * 64 * FRACUNIT)
            }
            if (targ.linetarget == null) {
                an = source.angle and Tables.BITS32
                // angle should be "sane"..right?
                // Just this line allows freelook.
                slope = (source.player!!.lookdir shl FRACBITS) / 173
            }
        }
        x = source._x
        y = source._y
        z = source._z + 4 * 8 * FRACUNIT + slope
        th = SpawnMobj(x, y, z, type!!)
        if (th.info!!.seesound != null) {
            StartSound(th, th.info!!.seesound)
        }
        th.target = source
        th.angle = an
        th.momx = FixedMul(th.info!!.speed, Tables.finecosine(an))
        th.momy = FixedMul(th.info!!.speed, Tables.finesine(an))
        th.momz = FixedMul(th.info!!.speed, slope)
        CheckMissileSpawn(th)
    }

    /**
     * P_ExplodeMissile
     */
    override fun ExplodeMissile(mo: mobj_t) {
        mo.momz = 0
        mo.momy = mo.momz
        mo.momx = mo.momy

        // MAES 9/5/2011: using mobj code for that.
        mo.SetMobjState(info.mobjinfo[mo.type!!.ordinal].deathstate)
        mo.mobj_tics -= (P_Random() and 3).toLong()
        if (mo.mobj_tics < 1) {
            mo.mobj_tics = 1
        }
        mo.flags = mo.flags and mobj_t.MF_MISSILE.inv()
        if (mo.info!!.deathsound != null) {
            StartSound(mo, mo.info!!.deathsound)
        }
    }
}