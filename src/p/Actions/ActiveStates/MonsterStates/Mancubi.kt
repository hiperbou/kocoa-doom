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
import data.info
import data.mobjtype_t
import data.sounds.sfxenum_t
import m.fixed_t
import p.Actions.ActionTrait
import p.mobj_t

interface Mancubi : ActionTrait {
    fun A_FaceTarget(actor: mobj_t?)

    //
    // Mancubus attack,
    // firing three missiles (bruisers)
    // in three different directions?
    // Doesn't look like it. 
    //
    fun A_FatRaise(actor: mobj_t?) {
        A_FaceTarget(actor)
        StartSound(actor, sfxenum_t.sfx_manatk)
    }

    fun A_FatAttack1(actor: mobj_t) {
        val mo: mobj_t
        val an: Int
        A_FaceTarget(actor)
        // Change direction  to ...
        actor.angle += Mancubi.FATSPREAD
        attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_FATSHOT)
        mo = attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_FATSHOT)!!
        mo.angle += Mancubi.FATSPREAD
        an = Tables.toBAMIndex(mo.angle)
        mo.momx = fixed_t.FixedMul(mo.info!!.speed, Tables.finecosine[an])
        mo.momy = fixed_t.FixedMul(mo.info!!.speed, Tables.finesine[an])
    }

    fun A_FatAttack2(actor: mobj_t) {
        val mo: mobj_t
        val an: Int
        A_FaceTarget(actor)
        // Now here choose opposite deviation.
        actor.angle -= Mancubi.FATSPREAD
        attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_FATSHOT)
        mo = attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_FATSHOT)!!
        mo.angle -= Mancubi.FATSPREAD * 2
        an = Tables.toBAMIndex(mo.angle)
        mo.momx = fixed_t.FixedMul(mo.info!!.speed, Tables.finecosine[an])
        mo.momy = fixed_t.FixedMul(mo.info!!.speed, Tables.finesine[an])
    }

    fun A_FatAttack3(actor: mobj_t) {
        var mo: mobj_t
        var an: Int
        A_FaceTarget(actor)
        mo = attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_FATSHOT)!!
        mo.angle -= Mancubi.FATSPREAD / 2
        an = Tables.toBAMIndex(mo.angle)
        mo.momx = fixed_t.FixedMul(mo.info!!.speed, Tables.finecosine[an])
        mo.momy = fixed_t.FixedMul(mo.info!!.speed, Tables.finesine[an])
        mo = attacks.SpawnMissile(actor, actor.target!!, mobjtype_t.MT_FATSHOT)!!
        mo.angle += Mancubi.FATSPREAD / 2
        an = Tables.toBAMIndex(mo.angle)
        mo.momx = fixed_t.FixedMul(mo.info!!.speed, Tables.finecosine[an])
        mo.momy = fixed_t.FixedMul(mo.info!!.speed, Tables.finesine[an])
    }

    companion object {
        const val FATSPREAD = Tables.ANG90 / 8
    }
}