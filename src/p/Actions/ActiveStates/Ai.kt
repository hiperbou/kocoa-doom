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
package p.Actions.ActiveStates

import data.Tables
import data.mobjtype_t
import data.sounds.sfxenum_t
import defines.skill_t
import defines.statenum_t
import p.Actions.ActiveStatesimportimport.Monsters
import p.ActiveStates
import p.mobj_t
import utils.C2JUtils

interface Ai : Monsters, Sounds {
    //
    // A_Look
    // Stay in state until a player is sighted.
    //
    fun A_Look(actor: mobj_t) {
        val targ: mobj_t?
        var seeyou = false // to avoid the fugly goto
        actor.threshold = 0 // any shot will wake up
        targ = actor.subsector!!.sector!!.soundtarget
        if (targ != null
            && C2JUtils.eval(targ.flags and mobj_t.MF_SHOOTABLE)
        ) {
            actor.target = targ
            seeyou = if (C2JUtils.eval(actor.flags and mobj_t.MF_AMBUSH)) {
                enemies.CheckSight(actor, actor.target!!)
            } else {
                true
            }
        }
        if (!seeyou) {
            if (!enemies.LookForPlayers(actor, false)) {
                return
            }
        }

        // go into chase state
        seeyou@ if (actor.info!!.seesound != null && actor.info!!.seesound != sfxenum_t.sfx_None) {
            val sound: Int
            sound = when (actor.info!!.seesound) {
                sfxenum_t.sfx_posit1, sfxenum_t.sfx_posit2, sfxenum_t.sfx_posit3 -> sfxenum_t.sfx_posit1.ordinal + P_Random() % 3
                sfxenum_t.sfx_bgsit1, sfxenum_t.sfx_bgsit2 -> sfxenum_t.sfx_bgsit1.ordinal + P_Random() % 2
                else -> actor.info!!.seesound.ordinal
            }
            if (actor.type == mobjtype_t.MT_SPIDER || actor.type == mobjtype_t.MT_CYBORG) {
                // full volume
                StartSound(null, sound)
            } else {
                StartSound(actor, sound)
            }
        }
        actor.SetMobjState(actor.info!!.seestate)
    }

    /**
     * A_Chase
     * Actor has a melee attack,
     * so it tries to close as fast as possible
     */
    override fun A_Chase(actor: mobj_t) {
        val delta: Int
        var nomissile = false // for the fugly goto
        if (actor.reactiontime != 0) {
            actor.reactiontime--
        }

        // modify target threshold
        if (actor.threshold != 0) {
            if (actor.target == null || actor.target!!.health <= 0) {
                actor.threshold = 0
            } else {
                actor.threshold--
            }
        }

        // turn towards movement direction if not there yet
        if (actor.movedir < 8) {
            actor.angle = actor.angle and (7 shl 29).toLong()
            actor.angle = actor.angle and Tables.BITS32
            // Nice problem, here!
            delta = (actor.angle - (actor.movedir shl 29)).toInt()
            if (delta > 0) {
                actor.angle -= Tables.ANG45
            } else if (delta < 0) {
                actor.angle += Tables.ANG45
            }
            actor.angle = actor.angle and Tables.BITS32
        }
        if (actor.target == null || !C2JUtils.eval(actor.target!!.flags and mobj_t.MF_SHOOTABLE)) {
            // look for a new target
            if (enemies.LookForPlayers(actor, true)) {
                return  // got a new target
            }
            actor.SetMobjState(actor.info!!.spawnstate)
            return
        }

        // do not attack twice in a row
        if (C2JUtils.eval(actor.flags and mobj_t.MF_JUSTATTACKED)) {
            actor.flags = actor.flags and mobj_t.MF_JUSTATTACKED.inv()
            if (gameSkill != skill_t.sk_nightmare && !IsFastParm()) {
                attacks.NewChaseDir(actor)
            }
            return
        }

        // check for melee attack
        if (actor.info!!.meleestate != statenum_t.S_NULL && enemies.CheckMeleeRange(actor)) {
            if (actor.info!!.attacksound != null) {
                StartSound(actor, actor.info!!.attacksound)
            }
            actor.SetMobjState(actor.info!!.meleestate)
            return
        }

        // check for missile attack
        if (actor.info!!.missilestate != statenum_t.S_NULL) { //_D_: this caused a bug where Demon for example were disappearing
            // Assume that a missile attack is possible
            if (gameSkill!!.ordinal < skill_t.sk_nightmare.ordinal && !IsFastParm() && actor.movecount != 0) {
                // Uhm....no.
                nomissile = true
            } else if (!enemies.CheckMissileRange(actor)) {
                nomissile = true // Out of range
            }
            if (!nomissile) {
                // Perform the attack
                actor.SetMobjState(actor.info!!.missilestate)
                actor.flags = actor.flags or mobj_t.MF_JUSTATTACKED
                return
            }
        }

        // This should be executed always, if not averted by returns.
        // possibly choose another target
        if (IsNetGame() && actor.threshold == 0 && !enemies.CheckSight(actor, actor.target!!)) {
            if (enemies.LookForPlayers(actor, true)) {
                return  // got a new target
            }
        }

        // chase towards player
        if (--actor.movecount < 0 || !attacks.Move(actor)) {
            attacks.NewChaseDir(actor)
        }

        // make active sound
        if (actor.info!!.activesound != null && P_Random() < 3) {
            StartSound(actor, actor.info!!.activesound)
        }
    }

    override fun A_Fall(actor: mobj_t) {
        // actor is on ground, it can be walked over
        actor!!.flags = actor.flags and mobj_t.MF_SOLID.inv()

        // So change this if corpse objects
        // are meant to be obstacles.
    }

    /**
     * Causes object to move and perform obs.
     * Can only be called through the Actions dispatcher.
     *
     * @param mobj
     */
    //
    //P_MobjThinker
    //
    fun P_MobjThinker(mobj: mobj_t) {
        // momentum movement
        if (mobj.momx != 0 || mobj.momy != 0 || C2JUtils.eval(mobj.flags and mobj_t.MF_SKULLFLY)) {
            attacks.XYMovement(mobj)

            // FIXME: decent NOP/NULL/Nil function pointer please.
            if (mobj.thinkerFunction == ActiveStates.NOP) {
                return  // mobj was removed
            }
        }
        if (mobj._z != mobj.floorz || mobj.momz != 0) {
            mobj.ZMovement()

            // FIXME: decent NOP/NULL/Nil function pointer please.
            if (mobj.thinkerFunction == ActiveStates.NOP) {
                return  // mobj was removed
            }
        }

        // cycle through states,
        // calling action functions at transitions
        if (mobj.mobj_tics != -1L) {
            mobj.mobj_tics--

            // you can cycle through multiple states in a tic
            if (!C2JUtils.eval(mobj.mobj_tics)) {
                if (!mobj.SetMobjState(mobj.mobj_state!!.nextstate)) {
                    // freed itself
                }
            }
        } else {
            // check for nightmare respawn
            if (!C2JUtils.eval(mobj.flags and mobj_t.MF_COUNTKILL)) {
                return
            }
            if (!DOOM().respawnmonsters) {
                return
            }
            mobj.movecount++
            if (mobj.movecount < 12 * 35) {
                return
            }
            if (C2JUtils.eval(LevelTime() and 31)) {
                return
            }
            if (P_Random() > 4) {
                return
            }
            enemies.NightmareRespawn(mobj)
        }
    }

    //
    // A_FaceTarget
    //
    override fun A_FaceTarget(actor: mobj_t?) {
        if (actor!!.target == null) {
            return
        }
        actor.flags = actor.flags and mobj_t.MF_AMBUSH.inv()
        actor.angle = sceneRenderer().PointToAngle2(
            actor._x,
            actor._y,
            actor.target!!._x,
            actor.target!!._y
        ) and Tables.BITS32
        if (C2JUtils.eval(actor.target!!.flags and mobj_t.MF_SHADOW)) {
            actor.angle += (P_Random() - P_Random() shl 21)
        }
        actor.angle = actor.angle and Tables.BITS32
    }
}