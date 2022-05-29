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


import data.Limits
import data.Tables
import data.info
import data.mobjtype_t
import doom.SourceCode
import doom.SourceCode.angle_t
import doom.thinker_t
import m.fixed_t.Companion.FixedMul
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.MAPFRACUNIT
import p.Actions.ActionTrait
import p.ActiveStates
import p.MapUtils
import p.mobj_t

interface PainsSouls : ActionTrait {
    fun A_FaceTarget(actor: mobj_t?)
    fun A_Fall(actor: mobj_t)

    /**
     * SkullAttack
     * Fly at the player like a missile.
     */
    fun A_SkullAttack(actor: mobj_t) {
        val dest: mobj_t
        val an: Int
        var dist: Int
        if (actor.target == null) {
            return
        }
        dest = actor.target!!
        actor.flags = actor.flags or mobj_t.MF_SKULLFLY
        StartSound(actor, actor.info!!.attacksound)
        A_FaceTarget(actor)
        an = Tables.toBAMIndex(actor.angle)
        actor.momx = FixedMul(PainsSouls.SKULLSPEED, Tables.finecosine[an])
        actor.momy = FixedMul(PainsSouls.SKULLSPEED, Tables.finesine[an])
        dist = MapUtils.AproxDistance(dest._x - actor._x, dest._y - actor._y)
        dist /= PainsSouls.SKULLSPEED
        if (dist < 1) {
            dist = 1
        }
        actor.momz = (dest._z + (dest.height shr 1) - actor._z) / dist
    }

    /**
     * A_PainShootSkull
     * Spawn a lost soul and launch it at the target
     * It's not a valid callback like the others, actually.
     * No idea if some DEH patch does use it to cause
     * mayhem though.
     *
     */
    fun A_PainShootSkull(actor: mobj_t, angle: Long?) {
        @SourceCode.fixed_t val x: Int
        @SourceCode.fixed_t val y: Int
        @SourceCode.fixed_t val z: Int
        val newmobj: mobj_t
        @angle_t val an: Int
        val prestep: Int
        var count: Int
        var currentthinker: thinker_t

        // count total number of skull currently on the level
        count = 0
        currentthinker = getThinkerCap().next!!
        while (currentthinker !== getThinkerCap()) {
            if (currentthinker.thinkerFunction == ActiveStates.P_MobjThinker
                && (currentthinker as mobj_t).type == mobjtype_t.MT_SKULL
            ) {
                count++
            }
            currentthinker = currentthinker.next!!
        }

        // if there are allready 20 skulls on the level,
        // don't spit another one
        if (count > Limits.MAXSKULLS) {
            return
        }

        // okay, there's playe for another one
        an = Tables.toBAMIndex(angle!!)
        prestep = (4 * FRACUNIT + 3 * (actor.info!!.radius + info.mobjinfo[mobjtype_t.MT_SKULL.ordinal].radius) / 2)
        x = actor._x + FixedMul(prestep, Tables.finecosine[an])
        y = actor._y + FixedMul(prestep, Tables.finesine[an])
        z = actor._z + 8 * FRACUNIT
        newmobj = attacks.SpawnMobj(x, y, z, mobjtype_t.MT_SKULL)

        // Check for movements.
        if (!attacks.TryMove(newmobj, newmobj._x, newmobj._y)) {
            // kill it immediately
            attacks.DamageMobj(newmobj, actor, actor, 10000)
            return
        }
        newmobj.target = actor.target
        A_SkullAttack(newmobj)
    }

    //
    // A_PainAttack
    // Spawn a lost soul and launch it at the target
    // 
    fun A_PainAttack(actor: mobj_t) {
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        A_PainShootSkull(actor, actor.angle)
    }

    fun A_PainDie(actor: mobj_t) {
        A_Fall(actor)
        A_PainShootSkull(actor, actor.angle + Tables.ANG90)
        A_PainShootSkull(actor, actor.angle + Tables.ANG180)
        A_PainShootSkull(actor, actor.angle + Tables.ANG270)
    }

    companion object {
        val SKULLSPEED: Int = 20 * MAPFRACUNIT
    }
}