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


import data.mobjtype_t
import data.sounds.sfxenum_t
import p.Actions.ActionTrait
import p.mobj_t

interface Demonspawns : ActionTrait {
    fun A_FaceTarget(actor: mobj_t?)

    //
    // A_TroopAttack
    //
    fun A_TroopAttack(actor: mobj_t) {
        val damage: Int
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        if (enemies.CheckMeleeRange(actor)) {
            StartSound(actor, sfxenum_t.sfx_claw)
            damage = (P_Random() % 8 + 1) * 3
            attacks.DamageMobj(actor.target!!, actor, actor, damage)
            return
        }

        // launch a missile
        attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_TROOPSHOT)
    }

    fun A_SargAttack(actor: mobj_t) {
        val damage: Int
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        if (enemies.CheckMeleeRange(actor)) {
            damage = (P_Random() % 10 + 1) * 4
            attacks.DamageMobj(actor.target!!, actor, actor, damage)
        }
    }

    fun A_HeadAttack(actor: mobj_t) {
        val damage: Int
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        if (enemies.CheckMeleeRange(actor)) {
            damage = (P_Random() % 6 + 1) * 10
            attacks.DamageMobj(actor.target!!, actor, actor, damage)
            return
        }

        // launch a missile
        attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_HEADSHOT)
    }

    fun A_CyberAttack(actor: mobj_t) {
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_ROCKET)
    }

    fun A_BruisAttack(actor: mobj_t) {
        val damage: Int
        if (actor.target == null) {
            return
        }
        if (enemies.CheckMeleeRange(actor)) {
            StartSound(actor, sfxenum_t.sfx_claw)
            damage = (P_Random() % 8 + 1) * 10
            attacks.DamageMobj(actor.target!!, actor, actor, damage)
            return
        }

        // launch a missile
        attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_BRUISERSHOT)
    }
}