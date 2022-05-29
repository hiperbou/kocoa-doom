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
import data.mobjinfo_t
import data.mobjtype_t
import data.sounds.sfxenum_t
import defines.statenum_t
import p.Actions.ActionTrait
import p.Actions.ActionsAttacks
import p.ChaseDirections
import p.mobj_t
import m.fixed_t.Companion.FixedMul
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.MAPFRACUNIT

interface Viles : ActionTrait {
    fun A_FaceTarget(actor: mobj_t?)
    fun A_Chase(actor: mobj_t)

    //
    // A_VileChase
    // Check for ressurecting a body
    //
    fun A_VileChase(actor: mobj_t) {
        val ll = levelLoader()
        val actionsAttacks = attacks
        val att = actionsAttacks.contextRequire<ActionsAttacks.Attacks>(ActionsAttacks.KEY_ATTACKS)
        val xl: Int
        val xh: Int
        val yl: Int
        val yh: Int
        var bx: Int
        var by: Int
        val info: mobjinfo_t
        val temp: mobj_t
        if (actor.movedir != ChaseDirections.DI_NODIR) {
            // check for corpses to raise
            att.vileTryX = actor._x + actor.info!!.speed * ChaseDirections.xspeed[actor.movedir]
            att.vileTryY = actor._y + actor.info!!.speed * ChaseDirections.yspeed[actor.movedir]
            xl = ll.getSafeBlockX(att.vileTryX - ll.bmaporgx - Limits.MAXRADIUS * 2)
            xh = ll.getSafeBlockX(att.vileTryX - ll.bmaporgx + Limits.MAXRADIUS * 2)
            yl = ll.getSafeBlockY(att.vileTryY - ll.bmaporgy - Limits.MAXRADIUS * 2)
            yh = ll.getSafeBlockY(att.vileTryY - ll.bmaporgy + Limits.MAXRADIUS * 2)
            att.vileObj = actor
            bx = xl
            while (bx <= xh) {
                by = yl
                while (by <= yh) {

                    // Call PIT_VileCheck to check
                    // whether object is a corpse
                    // that can be raised.
                    if (!BlockThingsIterator(bx, by) { thing: mobj_t? -> actionsAttacks.VileCheck(thing!!) }) {
                        // got one!
                        temp = actor.target!!
                        actor.target = att.vileCorpseHit
                        A_FaceTarget(actor)
                        actor.target = temp
                        actor.SetMobjState(statenum_t.S_VILE_HEAL1)
                        StartSound(att.vileCorpseHit, sfxenum_t.sfx_slop)
                        info = att.vileCorpseHit!!.info!!
                        att.vileCorpseHit!!.SetMobjState(info.raisestate)
                        att.vileCorpseHit!!.height = att.vileCorpseHit!!.height shl 2
                        att.vileCorpseHit!!.flags = info.flags
                        att.vileCorpseHit!!.health = info.spawnhealth
                        att.vileCorpseHit!!.target = null
                        return
                    }
                    by++
                }
                bx++
            }
        }

        // Return to normal attack.
        A_Chase(actor)
    }

    //
    // A_VileStart
    //
    fun A_VileStart(actor: mobj_t?) {
        StartSound(actor, sfxenum_t.sfx_vilatk)
    }

    //
    // A_Fire
    // Keep fire in front of player unless out of sight
    //
    fun A_StartFire(actor: mobj_t) {
        StartSound(actor, sfxenum_t.sfx_flamst)
        A_Fire(actor)
    }

    fun A_FireCrackle(actor: mobj_t) {
        StartSound(actor, sfxenum_t.sfx_flame)
        A_Fire(actor)
    }

    fun A_Fire(actor: mobj_t) {
        val dest: mobj_t?
        //long    an;
        dest = actor.tracer
        if (dest == null) {
            return
        }

        // don't move it if the vile lost sight
        if (!enemies.CheckSight(actor.target!!, dest)) {
            return
        }

        // an = dest.angle >>> ANGLETOFINESHIFT;
        attacks.UnsetThingPosition(actor)
        actor._x = dest._x + FixedMul(24 * FRACUNIT, Tables.finecosine(dest.angle))
        actor._y = dest._y + FixedMul(24 * FRACUNIT, Tables.finesine(dest.angle))
        actor._z = dest._z
        SetThingPosition(actor)
    }

    //
    // A_VileTarget
    // Spawn the hellfire
    //
    fun A_VileTarget(actor: mobj_t) {
        val fog: mobj_t
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        fog = enemies.SpawnMobj(actor.target!!._x, actor.target!!._y, actor.target!!._z, mobjtype_t.MT_FIRE)
        actor.tracer = fog
        fog.target = actor
        fog.tracer = actor.target
        A_Fire(fog)
    }

    //
    // A_VileAttack
    //
    fun A_VileAttack(actor: mobj_t) {
        val fire: mobj_t?
        //int     an;
        if (actor.target == null) {
            return
        }
        A_FaceTarget(actor)
        if (!enemies.CheckSight(actor, actor.target!!)) {
            return
        }
        StartSound(actor, sfxenum_t.sfx_barexp)
        attacks.DamageMobj(actor.target!!, actor, actor, 20)
        actor.target!!.momz = 1000 * MAPFRACUNIT / actor.target!!.info!!.mass

        // an = actor.angle >> ANGLETOFINESHIFT;
        fire = actor.tracer
        if (fire == null) {
            return
        }

        // move the fire between the vile and the player
        fire._x =
            actor.target!!._x - FixedMul(24 * FRACUNIT, Tables.finecosine(actor.angle))
        fire._y =
            actor.target!!._y - FixedMul(24 * FRACUNIT, Tables.finesine(actor.angle))
        attacks.RadiusAttack(fire, actor, 70)
    }
}