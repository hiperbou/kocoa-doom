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
import data.Tables
import data.info
import data.mobjtype_t
import defines.skill_t
import defines.statenum_t
import doom.SourceCode
import doom.SourceCode.P_MapUtl
import doom.SourceCode.P_Mobj
import doom.player_t
import doom.weapontype_t
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedMul
import m.fixed_t.Companion.MAPFRACUNIT
import p.Actions.ActionsSectors.RespawnQueue
import p.MobjFlags
import p.mobj_t
import utils.C2JUtils

interface ActionsMobj : ActionsThings, ActionsMovement, ActionsTeleportation {
    //
    // P_DamageMobj
    // Damages both enemies and players
    // "inflictor" is the thing that caused the damage
    //  creature or missile, can be NULL (slime, etc)
    // "source" is the thing to target after taking damage
    //  creature or NULL
    // Source and inflictor are the same for melee attacks.
    // Source can be NULL for slime, barrel explosions
    // and other environmental stuff.
    //
    override fun DamageMobj(target: mobj_t, inflictor: mobj_t?, source: mobj_t?, damage: Int) {
        var damage = damage
        var ang: Long // unsigned
        var saved: Int
        val player: player_t?
        @SourceCode.fixed_t var thrust: Int
        val temp: Int
        if (!C2JUtils.eval(target.flags and mobj_t.MF_SHOOTABLE)) {
            return  // shouldn't happen...
        }
        if (target.health <= 0) {
            return
        }
        if (C2JUtils.eval(target.flags and mobj_t.MF_SKULLFLY)) {
            target.momz = 0
            target.momy = target.momz
            target.momx = target.momy
        }
        player = target.player
        if (player != null && gameSkill == skill_t.sk_baby) {
            damage = damage shr 1 // take half damage in trainer mode
        }

        // Some close combat weapons should not
        // inflict thrust and push the victim out of reach,
        // thus kick away unless using the chainsaw.
        if ((inflictor != null
                    && !C2JUtils.eval(target.flags and mobj_t.MF_NOCLIP)) &&
            (source == null || source.player == null || source.player!!.readyweapon != weapontype_t.wp_chainsaw)
        ) {
            ang = sceneRenderer().PointToAngle2(
                inflictor._x,
                inflictor._y,
                target._x,
                target._y
            ) and Tables.BITS32
            thrust = damage * (MAPFRACUNIT shr 3) * 100 / target.info!!.mass

            // make fall forwards sometimes
            if (damage < 40 && damage > target.health && target._z - inflictor._z > 64 * FRACUNIT
                && C2JUtils.eval(P_Random() and 1)
            ) {
                ang += Tables.ANG180
                thrust *= 4
            }

            //ang >>= ANGLETOFINESHIFT;
            target.momx += FixedMul(thrust, Tables.finecosine(ang))
            target.momy += FixedMul(thrust, Tables.finesine(ang))
        }

        // player specific
        if (player != null) {
            // end of game hell hack
            if (target.subsector!!.sector!!.special.toInt() == 11
                && damage >= target.health
            ) {
                damage = target.health - 1
            }

            // Below certain threshold,
            // ignore damage in GOD mode, or with INVUL power.
            if (damage < 1000
                && C2JUtils.eval(player.cheats and player_t.CF_GODMODE)
                || player.powers[Defines.pw_invulnerability] != 0
            ) {
                return
            }
            if (player.armortype != 0) {
                saved = if (player.armortype == 1) {
                    damage / 3
                } else {
                    damage / 2
                }
                if (player.armorpoints[0] <= saved) {
                    // armor is used up
                    saved = player.armorpoints[0]
                    player.armortype = 0
                }
                player.armorpoints[0] -= saved
                damage -= saved
            }
            player.health[0] -= damage // mirror mobj health here for Dave
            if (player.health[0] < 0) {
                player.health[0] = 0
            }
            player.attacker = source
            player.damagecount += damage // add damage after armor / invuln
            if (player.damagecount > 100) {
                player.damagecount = 100 // teleport stomp does 10k points...
            }
            temp = if (damage < 100) damage else 100
            if (player === getPlayer(ConsolePlayerNumber())) {
                doomSystem().Tactile(40, 10, 40 + temp * 2)
            }
        }

        // do the damage    
        target.health -= damage
        if (target.health <= 0) {
            KillMobj(source, target)
            return
        }
        if (P_Random() < target.info!!.painchance
            && !C2JUtils.eval(target.flags and mobj_t.MF_SKULLFLY)
        ) {
            target.flags = target.flags or mobj_t.MF_JUSTHIT // fight back!
            target.SetMobjState(target.info!!.painstate)
        }
        target.reactiontime = 0 // we're awake now...   
        if (target.threshold == 0 || target.type == mobjtype_t.MT_VILE && source != null && source !== target && source.type != mobjtype_t.MT_VILE) {
            // if not intent on another player,
            // chase after this one
            target.target = source
            target.threshold = Defines.BASETHRESHOLD
            if (target.mobj_state === info.states[target.info!!.spawnstate.ordinal]
                && target.info!!.seestate != statenum_t.S_NULL
            ) {
                target.SetMobjState(target.info!!.seestate)
            }
        }
    }

    //
    // KillMobj
    //
    fun KillMobj(source: mobj_t?, target: mobj_t) {
        val item: mobjtype_t
        val mo: mobj_t

        // Maes: this seems necessary in order for barrel damage
        // to propagate inflictors.
        target.target = source
        target.flags =
            target.flags and (mobj_t.MF_SHOOTABLE or mobj_t.MF_FLOAT or mobj_t.MF_SKULLFLY)
                .inv()
        if (target.type != mobjtype_t.MT_SKULL) {
            target.flags = target.flags and mobj_t.MF_NOGRAVITY.inv()
        }
        target.flags = target.flags or (mobj_t.MF_CORPSE or mobj_t.MF_DROPOFF)
        target.height = target.height shr 2
        if (source != null && source.player != null) {
            // count for intermission
            if (target.flags and mobj_t.MF_COUNTKILL != 0) {
                source.player!!.killcount++
            }
            if (target.player != null) //; <-- _D_: that semicolon caused a bug!
            {
                source.player!!.frags[target.player!!.identify()]++
            }
            // It's probably intended to increment the frags of source player vs target player. Lookup? 
        } else if (!IsNetGame() && target.flags and mobj_t.MF_COUNTKILL != 0) {
            // count all monster deaths,
            // even those caused by other monsters
            getPlayer(0)!!.killcount++
        }
        if (target.player != null) {
            // count environment kills against you
            if (source == null) // TODO: some way to indentify which one of the 
            // four possiblelayers is the current player
            {
                target.player!!.frags[target.player!!.identify()]++
            }
            target.flags = target.flags and mobj_t.MF_SOLID.inv()
            target.player!!.playerstate = Defines.PST_DEAD
            target.player!!.DropWeapon() // in PSPR
            if (target.player === getPlayer(ConsolePlayerNumber()) && IsAutoMapActive()) {
                // don't die in auto map,
                // switch view prior to dying
                autoMap().Stop()
            }
        }
        if (target.health < -target.info!!.spawnhealth && target.info!!.xdeathstate != statenum_t.S_NULL) {
            target.SetMobjState(target.info!!.xdeathstate)
        } else {
            target.SetMobjState(target.info!!.deathstate)
        }
        target.mobj_tics -= (P_Random() and 3).toLong()
        if (target.mobj_tics < 1) {
            target.mobj_tics = 1
        }
        item = when (target.type) {
            mobjtype_t.MT_WOLFSS, mobjtype_t.MT_POSSESSED -> mobjtype_t.MT_CLIP
            mobjtype_t.MT_SHOTGUY -> mobjtype_t.MT_SHOTGUN
            mobjtype_t.MT_CHAINGUY -> mobjtype_t.MT_CHAINGUN
            else -> return
        }
        mo = SpawnMobj(target._x, target._y, Defines.ONFLOORZ, item)
        mo.flags = mo.flags or MobjFlags.MF_DROPPED // special versions of items
    }

    @SourceCode.Exact
    @P_Mobj.C(P_Mobj.P_RemoveMobj)
    override fun RemoveMobj(mobj: mobj_t) {
        if ((C2JUtils.eval(mobj.flags and MobjFlags.MF_SPECIAL)
                    && !C2JUtils.eval(mobj.flags and MobjFlags.MF_DROPPED)) && mobj.type != mobjtype_t.MT_INV && mobj.type != mobjtype_t.MT_INS
        ) {
            val resp = contextRequire<RespawnQueue>(ActionsSectors.KEY_RESP_QUEUE)
            resp.itemrespawnque[resp.iquehead] = mobj.spawnpoint
            resp.itemrespawntime[resp.iquehead] = LevelTime()
            resp.iquehead = resp.iquehead + 1 and Defines.ITEMQUESIZE - 1

            // lose one off the end?
            if (resp.iquehead == resp.iquetail) {
                resp.iquetail = resp.iquetail + 1 and Defines.ITEMQUESIZE - 1
            }
        }

        // unlink from sector and block lists
        P_UnsetThingPosition@ run {
            UnsetThingPosition(mobj)
        }

        // stop any playing sound
        S_StopSound@ run {
            StopSound(mobj)
        }

        // free block
        P_RemoveThinker@ run {
            RemoveThinker(mobj)
        }
    }

    /**
     * P_UnsetThingPosition Unlinks a thing from block map and sectors. On each
     * position change, BLOCKMAP and other lookups maintaining lists ot things
     * inside these structures need to be updated.
     */
    @SourceCode.Exact
    @P_MapUtl.C(P_MapUtl.P_UnsetThingPosition)
    override fun UnsetThingPosition(thing: mobj_t) {
        val ll = levelLoader()
        val blockx: Int
        val blocky: Int
        if (!C2JUtils.eval(thing.flags and MobjFlags.MF_NOSECTOR)) {
            // inert things don't need to be in blockmap?
            // unlink from subsector
            if (thing.snext != null) {
                (thing.snext as mobj_t).sprev = thing.sprev
            }
            if (thing.sprev != null) {
                (thing.sprev as mobj_t).snext = thing.snext
            } else {
                thing.subsector!!.sector!!.thinglist = thing.snext as mobj_t?
            }
        }
        if (!C2JUtils.eval(thing.flags and MobjFlags.MF_NOBLOCKMAP)) {
            // inert things don't need to be in blockmap
            // unlink from block map
            if (thing.bnext != null) {
                (thing.bnext as mobj_t).bprev = thing.bprev
            }
            if (thing.bprev != null) {
                (thing.bprev as mobj_t).bnext = thing.bnext
            } else {
                blockx = ll.getSafeBlockX(thing._x - ll.bmaporgx)
                blocky = ll.getSafeBlockY(thing._y - ll.bmaporgy)
                if (blockx >= 0 && blockx < ll.bmapwidth && blocky >= 0 && blocky < ll.bmapheight) {
                    ll.blocklinks!![blocky * ll.bmapwidth + blockx] = thing.bnext as mobj_t?
                }
            }
        }
    }
}