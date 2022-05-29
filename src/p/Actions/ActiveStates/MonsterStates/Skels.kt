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
package p.Actions.ActiveStates.MonsterStatesimport


import data.Tables
import data.mobjtype_t
import data.sounds.sfxenum_t
import p.Actions.ActionTrait
import p.MapUtils
import p.mobj_t
import utils.C2JUtils
import m.fixed_t.Companion.FixedMul
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.MAPFRACUNIT


interface Skels : ActionTrait {
    //
    // A_SkelMissile
    //
    fun A_SkelMissile(actor: mobj_t) {
        val mo: mobj_t
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        actor._z += 16 * FRACUNIT // so missile spawns higher
        mo = attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_TRACER)!!
        actor._z -= 16 * FRACUNIT // back to normal
        mo._x += mo.momx
        mo._y += mo.momy
        mo.tracer = actor.target
    }

    fun A_SkelWhoosh(actor: mobj_t) {
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        StartSound(actor, sfxenum_t.sfx_skeswg)
    }

    fun A_SkelFist(actor: mobj_t) {
        val damage: Int
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        if (enemies.CheckMeleeRange(actor)) {
            damage = (P_Random() % 10 + 1) * 6
            StartSound(actor, sfxenum_t.sfx_skepch)
            attacks.DamageMobj(actor.target!!, actor, actor, damage)
        }
    }

    fun A_Tracer(actor: mobj_t) {
        val exact: Long //angle_t
        var dist: Int
        val slope: Int // fixed
        val dest: mobj_t?
        val th: mobj_t
        if (C2JUtils.eval(DOOM().gametic and 3)) {
            return
        }
        // spawn a puff of smoke behind the rocket
        attacks.SpawnPuff(actor._x, actor._y, actor._z)
        th = enemies.SpawnMobj(actor._x - actor.momx, actor._y - actor.momy, actor._z, mobjtype_t.MT_SMOKE)
        th.momz = MAPFRACUNIT
        th.mobj_tics -= (P_Random() and 3).toLong()
        if (th.mobj_tics < 1) {
            th.mobj_tics = 1
        }

        // adjust direction
        dest = actor.tracer
        if (dest == null || dest.health <= 0) {
            return
        }

        // change angle
        exact = sceneRenderer().PointToAngle2(actor._x, actor._y, dest._x, dest._y) and Tables.BITS32

        // MAES: let's analyze the logic here...
        // So exact is the angle between the missile and its target.
        if (exact != actor.angle) { // missile is already headed there dead-on.
            if (exact - actor.angle > Tables.ANG180) {
                actor.angle -= Skels.TRACEANGLE.toLong()
                actor.angle = actor.angle and Tables.BITS32
                if (exact - actor.angle and Tables.BITS32 < Tables.ANG180) {
                    actor.angle = exact
                }
            } else {
                actor.angle += Skels.TRACEANGLE.toLong()
                actor.angle = actor.angle and Tables.BITS32
                if (exact - actor.angle and Tables.BITS32 > Tables.ANG180) {
                    actor.angle = exact
                }
            }
        }
        // MAES: fixed and sped up.
        val exact2 = Tables.toBAMIndex(actor.angle)
        actor.momx = FixedMul(actor.info!!.speed, Tables.finecosine[exact2])
        actor.momy = FixedMul(actor.info!!.speed, Tables.finesine[exact2])
        // change slope
        dist = MapUtils.AproxDistance(dest._x - actor._x, dest._y - actor._y)
        dist /= actor.info!!.speed
        if (dist < 1) {
            dist = 1
        }
        slope = (dest._z + 40 * FRACUNIT - actor._z) / dist
        if (slope < actor.momz) {
            actor.momz -= FRACUNIT / 8
        } else {
            actor.momz += FRACUNIT / 8
        }
    }

    fun A_FaceTarget(actor: mobj_t?)

    companion object {
        const val TRACEANGLE = 0xC000000
    }
}