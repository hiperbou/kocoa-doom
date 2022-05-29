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
import data.mobjtype_t
import data.sounds.sfxenum_t
import m.fixed_t.Companion.FRACUNIT
import p.Actions.ActionTrait
import p.Actions.ActiveStates.Sounds
import p.ActiveStates
import p.mobj_t
import utils.TraitFactory.ContextKey
import java.util.function.Supplier

import defines.skill_t;
import defines.statenum_t;


interface HorrendousVisages : Sounds, ActionTrait { //TODO: Added ActionTrait because contextRequire?
    class Brain {
        // Brain status
        var braintargets = arrayOfNulls<mobj_t>(Limits.NUMBRAINTARGETS)
        var numbraintargets = 0
        var braintargeton = 0
        var easy = 0
    }

    fun A_BrainAwake(mo: mobj_t?) {
        val brain = contextRequire<Brain>(HorrendousVisages.KEY_BRAIN)
        var m: mobj_t

        // find all the target spots
        brain.numbraintargets = 0
        brain.braintargeton = 0

        //thinker = obs.thinkercap.next;
        var thinker = getThinkerCap().next
        while (thinker !== getThinkerCap()) {
            if (thinker!!.thinkerFunction != ActiveStates.P_MobjThinker) {
                thinker = thinker.next
                continue  // not a mobj
            }
            m = thinker as mobj_t
            if (m.type == mobjtype_t.MT_BOSSTARGET) {
                brain.braintargets[brain.numbraintargets] = m
                brain.numbraintargets++
            }
            thinker = thinker.next
        }
        StartSound(null, sfxenum_t.sfx_bossit)
    }

    fun A_BrainScream(mo: mobj_t) {
        var x: Int
        var y: Int
        var z: Int
        var th: mobj_t
        x = mo._x - 196 * FRACUNIT
        while (x < mo._x + 320 * FRACUNIT) {
            y = mo._y - 320 * FRACUNIT
            z = 128 + P_Random() * 2 * FRACUNIT
            th = enemies.SpawnMobj(x, y, z, mobjtype_t.MT_ROCKET)
            th.momz = P_Random() * 512
            th.SetMobjState(statenum_t.S_BRAINEXPLODE1)
            th.mobj_tics -= (P_Random() and 7).toLong()
            if (th.mobj_tics < 1) {
                th.mobj_tics = 1
            }
            x += FRACUNIT * 8
        }
        StartSound(null, sfxenum_t.sfx_bosdth)
    }

    fun A_BrainExplode(mo: mobj_t) {
        val x: Int
        val y: Int
        val z: Int
        val th: mobj_t
        x = mo._x + (P_Random() - P_Random()) * 2048
        y = mo._y
        z = 128 + P_Random() * 2 * FRACUNIT
        th = enemies.SpawnMobj(x, y, z, mobjtype_t.MT_ROCKET)
        th.momz = P_Random() * 512
        th.SetMobjState(statenum_t.S_BRAINEXPLODE1)
        th.mobj_tics -= (P_Random() and 7).toLong()
        if (th.mobj_tics < 1) {
            th.mobj_tics = 1
        }
    }

    fun A_BrainDie(mo: mobj_t?) {
        DOOM().ExitLevel()
    }

    fun A_BrainSpit(mo: mobj_t) {
        val brain = contextRequire<Brain>(HorrendousVisages.KEY_BRAIN)
        val targ: mobj_t?
        val newmobj: mobj_t
        brain.easy = brain.easy xor 1
        if (gameSkill!!.ordinal <= skill_t.sk_easy.ordinal && brain.easy == 0) {
            return
        }

        // shoot a cube at current target
        targ = brain.braintargets[brain.braintargeton]

        // Load-time fix: awake on zero numbrain targets, if A_BrainSpit is called.
        if (brain.numbraintargets == 0) {
            A_BrainAwake(mo)
            return
        }
        brain.braintargeton = (brain.braintargeton + 1) % brain.numbraintargets

        // spawn brain missile
        newmobj = attacks.SpawnMissile(mo, targ!!, mobjtype_t.MT_SPAWNSHOT)!!
        newmobj.target = targ
        newmobj.reactiontime = (targ!!._y - mo._y) / newmobj.momy / newmobj.mobj_state!!.tics
        StartSound(null, sfxenum_t.sfx_bospit)
    }

    override fun A_SpawnFly(mo: mobj_t) {
        val newmobj: mobj_t
        val fog: mobj_t
        val targ: mobj_t
        val r: Int
        val type: mobjtype_t
        if (--mo.reactiontime != 0) {
            return  // still flying
        }
        targ = mo.target!!

        // First spawn teleport fog.
        fog = enemies.SpawnMobj(targ._x, targ._y, targ._z, mobjtype_t.MT_SPAWNFIRE)
        StartSound(fog, sfxenum_t.sfx_telept)

        // Randomly select monster to spawn.
        r = P_Random()

        // Probability distribution (kind of :),
        // decreasing likelihood.
        type = if (r < 50) {
            mobjtype_t.MT_TROOP
        } else if (r < 90) {
            mobjtype_t.MT_SERGEANT
        } else if (r < 120) {
            mobjtype_t.MT_SHADOWS
        } else if (r < 130) {
            mobjtype_t.MT_PAIN
        } else if (r < 160) {
            mobjtype_t.MT_HEAD
        } else if (r < 162) {
            mobjtype_t.MT_VILE
        } else if (r < 172) {
            mobjtype_t.MT_UNDEAD
        } else if (r < 192) {
            mobjtype_t.MT_BABY
        } else if (r < 222) {
            mobjtype_t.MT_FATSO
        } else if (r < 246) {
            mobjtype_t.MT_KNIGHT
        } else {
            mobjtype_t.MT_BRUISER
        }
        newmobj = enemies.SpawnMobj(targ._x, targ._y, targ._z, type)
        if (enemies.LookForPlayers(newmobj, true)) {
            newmobj.SetMobjState(newmobj.info!!.seestate)
        }

        // telefrag anything in this spot
        attacks.TeleportMove(newmobj, newmobj._x, newmobj._y)

        // remove self (i.e., cube).
        enemies.RemoveMobj(mo)
    }

    companion object {
        val KEY_BRAIN: ContextKey<Brain> =
            ActionTrait.ACTION_KEY_CHAIN.newKey<Brain>(HorrendousVisages::class.java, Supplier { Brain() })
    }
}