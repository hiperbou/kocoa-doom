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
package p.Actions.ActiveStates.MonsterStatesimportimport


import data.Defines
import data.info
import data.sounds.sfxenum_t
import p.Actions.ActionTrait
import p.mobj_t

interface Zombies : ActionTrait {
    fun A_FaceTarget(actor: mobj_t?)

    //
    // A_PosAttack
    //
    fun A_PosAttack(actor: mobj_t) {
        var angle: Int
        val damage: Int
        val slope: Int
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        angle = actor.angle.toInt()
        slope = attacks.AimLineAttack(actor, angle.toLong(), Defines.MISSILERANGE)
        StartSound(actor, sfxenum_t.sfx_pistol)
        angle += P_Random() - P_Random() shl 20
        damage = (P_Random() % 5 + 1) * 3
        attacks.LineAttack(actor, angle.toLong(), Defines.MISSILERANGE, slope, damage)
    }

    fun A_SPosAttack(actor: mobj_t) {
        var i: Int
        var angle: Long
        val bangle: Long
        var damage: Int
        val slope: Int
        if (actor.target == null) {
            return
        }
        StartSound(actor, sfxenum_t.sfx_shotgn)
        A_FaceTarget(actor)
        bangle = actor.angle
        slope = attacks.AimLineAttack(actor, bangle, Defines.MISSILERANGE)
        i = 0
        while (i < 3) {
            angle = bangle + (P_Random() - P_Random() shl 20)
            damage = (P_Random() % 5 + 1) * 3
            attacks.LineAttack(actor, angle, Defines.MISSILERANGE, slope, damage)
            i++
        }
    }

    fun A_CPosAttack(actor: mobj_t) {
        val angle: Long
        val bangle: Long
        val damage: Int
        val slope: Int
        if (actor.target == null) {
            return
        }
        StartSound(actor, sfxenum_t.sfx_shotgn)
        A_FaceTarget(actor)
        bangle = actor.angle
        slope = attacks.AimLineAttack(actor, bangle, Defines.MISSILERANGE)
        angle = bangle + (P_Random() - P_Random() shl 20)
        damage = (P_Random() % 5 + 1) * 3
        attacks.LineAttack(actor, angle, Defines.MISSILERANGE, slope, damage)
    }

    fun A_CPosRefire(actor: mobj_t) {
        // keep firing unless target got out of sight
        A_FaceTarget(actor)
        if (P_Random() < 40) {
            return
        }
        if (actor.target == null || actor.target!!.health <= 0 || !enemies.CheckSight(actor, actor.target!!)) {
            actor.SetMobjState(actor.info!!.seestate)
        }
    }
}