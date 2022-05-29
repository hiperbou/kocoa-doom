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
import data.Limits
import data.Tables
import data.mobjtype_t
import defines.statenum_t
import doom.SourceCode
import doom.items.weaponinfo
import doom.player_t
import m.fixed_t
import m.fixed_t.Companion.FRACUNIT
import p.Actions.ActionTrait.Movement
import p.MapUtils
import p.MobjFlags
import p.mobj_t
import rr.line_t
import rr.sector_t
import utils.TraitFactory.ContextKey
import java.util.function.Supplier

interface ActionsEnemies : ActionsSight, ActionsSpawns {
    class Enemies {
        var soundtarget: mobj_t? = null

        // Peg to map movement
        var spechitp = arrayOfNulls<line_t>(Limits.MAXSPECIALCROSS)
        var numspechit = 0
    }
    //
    // ENEMY THINKING
    // Enemies are allways spawned
    // with targetplayer = -1, threshold = 0
    // Most monsters are spawned unaware of all players,
    // but some can be made preaware
    //
    /**
     * P_CheckMeleeRange
     */
    fun CheckMeleeRange(actor: mobj_t): Boolean {
        val pl: mobj_t
        @SourceCode.fixed_t val dist: Int
        if (actor.target == null) {
            return false
        }
        pl = actor.target!!
        dist = MapUtils.AproxDistance(pl._x - actor._x, pl._y - actor._y)
        return if (dist >= Defines.MELEERANGE - 20 * fixed_t.FRACUNIT + pl.info!!.radius) {
            false
        } else CheckSight(actor, actor.target!!)
    }

    /**
     * P_CheckMissileRange
     */
    fun CheckMissileRange(actor: mobj_t): Boolean {
        @SourceCode.fixed_t var dist: Int
        if (!CheckSight(actor, actor.target!!)) {
            return false
        }
        if (actor.flags and MobjFlags.MF_JUSTHIT != 0) {
            // the target just hit the enemy,
            // so fight back!
            actor.flags = actor.flags and MobjFlags.MF_JUSTHIT.inv()
            return true
        }
        if (actor.reactiontime != 0) {
            return false // do not attack yet
        }

        // OPTIMIZE: get this from a global checksight
        dist =
            MapUtils.AproxDistance(actor._x - actor.target!!._x, actor._y - actor.target!!._y) - 64 * FRACUNIT

        // [SYNC}: Major desync cause of desyncs.
        // DO NOT compare with null!
        if (actor.info!!.meleestate == statenum_t.S_NULL) {
            dist -= 128 * FRACUNIT // no melee attack, so fire more
        }
        dist = dist shr 16
        if (actor.type == mobjtype_t.MT_VILE) {
            if (dist > 14 * 64) {
                return false // too far away
            }
        }
        if (actor.type == mobjtype_t.MT_UNDEAD) {
            if (dist < 196) {
                return false // close for fist attack
            }
            dist = dist shr 1
        }
        if (actor.type == mobjtype_t.MT_CYBORG || actor.type == mobjtype_t.MT_SPIDER || actor.type == mobjtype_t.MT_SKULL) {
            dist = dist shr 1
        }
        if (dist > 200) {
            dist = 200
        }
        if (actor.type == mobjtype_t.MT_CYBORG && dist > 160) {
            dist = 160
        }
        return P_Random() >= dist
    }

    //
    // Called by P_NoiseAlert.
    // Recursively traverse adjacent sectors,
    // sound blocking lines cut off traversal.
    //
    fun RecursiveSound(sec: sector_t, soundblocks: Int) {
        val sr = sceneRenderer()
        val en = contextRequire<Enemies>(ActionsEnemies.KEY_ENEMIES)
        val mov = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        var i: Int
        var check: line_t
        var other: sector_t

        // wake up all monsters in this sector
        if (sec.validcount == sr.getValidCount() && sec.soundtraversed <= soundblocks + 1) {
            return  // already flooded
        }
        sec.validcount = sr.getValidCount()
        sec.soundtraversed = soundblocks + 1
        sec.soundtarget = en.soundtarget

        // "peg" to the level loader for syntactic sugar
        val sides = levelLoader().sides
        i = 0
        while (i < sec.linecount) {
            check = sec.lines!![i]!!
            if (check.flags.toInt() and line_t.ML_TWOSIDED == 0) {
                i++
                continue
            }
            LineOpening(check)
            if (mov.openrange <= 0) {
                i++
                continue  // closed door
            }
            other = if (sides[check.sidenum[0].code].sector === sec) {
                sides[check.sidenum[1].code].sector!!
            } else {
                sides[check.sidenum[0].code].sector!!
            }
            if (check.flags.toInt() and line_t.ML_SOUNDBLOCK != 0) {
                if (soundblocks == 0) {
                    RecursiveSound(other, 1)
                }
            } else {
                RecursiveSound(other, soundblocks)
            }
            i++
        }
    }

    /**
     * P_NoiseAlert
     * If a monster yells at a player,
     * it will alert other monsters to the player.
     */
    fun NoiseAlert(target: mobj_t?, emmiter: mobj_t) {
        val en = contextRequire<Enemies>(ActionsEnemies.KEY_ENEMIES)
        en.soundtarget = target
        sceneRenderer().increaseValidCount(1)
        RecursiveSound(emmiter.subsector!!.sector!!, 0)
    }

    /**
     * P_FireWeapon. Originally in pspr
     */
    fun FireWeapon(player: player_t) {
        val newstate: statenum_t
        if (!player.CheckAmmo()) {
            return
        }
        player.mo!!.SetMobjState(statenum_t.S_PLAY_ATK1)
        newstate = weaponinfo[player.readyweapon.ordinal].atkstate
        player.SetPsprite(player_t.ps_weapon, newstate)
        NoiseAlert(player.mo, player.mo!!)
    }

    /**
     * P_LookForPlayers If allaround is false, only look 180 degrees in
     * front. Returns true if a player is targeted.
     */
    fun LookForPlayers(actor: mobj_t, allaround: Boolean): Boolean {
        val sr = sceneRenderer()
        var c: Int
        val stop: Int
        var player: player_t
        // sector_t sector;
        var an: Long // angle
        var dist: Int // fixed

        // sector = actor.subsector.sector;
        c = 0
        stop = actor.lastlook - 1 and 3
        while (true) {
            if (!PlayerInGame(actor.lastlook)) {
                actor.lastlook = actor.lastlook + 1 and 3
                continue
            }
            if (c++ == 2 || actor.lastlook == stop) {
                // done looking
                return false
            }
            player = getPlayer(actor.lastlook)!!
            if (player.health[0] <= 0) {
                actor.lastlook = actor.lastlook + 1 and 3
                continue  // dead
            }
            val player_mo = player.mo!!
            if (!CheckSight(actor, player_mo)) {
                actor.lastlook = actor.lastlook + 1 and 3
                continue  // out of sight
            }
            if (!allaround) {
                an = sr.PointToAngle2(actor._x, actor._y, player_mo._x, player_mo._y) - actor.angle and Tables.BITS32
                if (an > Tables.ANG90 && an < Tables.ANG270) {
                    dist = MapUtils.AproxDistance(player_mo._x - actor._x, player_mo._y - actor._y)

                    // if real close, react anyway
                    if (dist > Defines.MELEERANGE) {
                        actor.lastlook = actor.lastlook + 1 and 3
                        continue  // behind back
                    }
                }
            }
            actor.target = player.mo
            return true
            actor.lastlook = actor.lastlook + 1 and 3 //TODO: unreachable code XD
        }
        // The compiler complains that this is unreachable
        // return false;
    }

    companion object {
        val KEY_ENEMIES: ContextKey<Enemies> =
            ActionTrait.ACTION_KEY_CHAIN.newKey<Enemies>(ActionsEnemies::class.java, Supplier { Enemies() })
    }
}