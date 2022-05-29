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

import data.Limits
import data.Tables
import data.mobjtype_t
import data.sounds.sfxenum_t
import doom.SourceCode
import doom.thinker_t
import m.BBox
import p.Actions.ActionTrait.Movement
import p.Actions.ActionTrait.Spechits
import p.ActiveStates
import p.mobj_t
import rr.line_t
import rr.sector_t
import rr.subsector_t

interface ActionsTeleportation : ActionsSectors {
    fun UnsetThingPosition(mobj: mobj_t)

    //
    // TELEPORTATION
    //
    override fun Teleport(line: line_t, side: Int, thing: mobj_t): Int {
        var i: Int
        val tag: Int
        var m: mobj_t
        var fog: mobj_t?
        val an: Int
        var thinker: thinker_t
        var sector: sector_t
        @SourceCode.fixed_t val oldx: Int
        @SourceCode.fixed_t val oldy: Int
        @SourceCode.fixed_t val oldz: Int

        // don't teleport missiles
        if (thing.flags and mobj_t.MF_MISSILE != 0) {
            return 0
        }

        // Don't teleport if hit back of line,
        //  so you can get out of teleporter.
        if (side == 1) {
            return 0
        }
        tag = line.tag.toInt()
        i = 0
        while (i < levelLoader().numsectors) {
            if (levelLoader().sectors[i].tag.toInt() == tag) {
                //thinker = thinkercap.next;
                thinker = getThinkerCap().next!!
                while (thinker !== getThinkerCap()) {

                    // not a mobj
                    if (thinker.thinkerFunction != ActiveStates.P_MobjThinker) {
                        thinker = thinker.next!!
                        continue
                    }
                    m = thinker as mobj_t

                    // not a teleportman
                    if (m.type != mobjtype_t.MT_TELEPORTMAN) {
                        thinker = thinker.next!!
                        continue
                    }
                    sector = m.subsector!!.sector!!
                    // wrong sector
                    if (sector.id != i) {
                        thinker = thinker.next!!
                        continue
                    }
                    oldx = thing._x
                    oldy = thing._y
                    oldz = thing._z
                    if (!TeleportMove(thing, m._x, m._y)) {
                        return 0
                    }
                    thing._z = thing.floorz //fixme: not needed?
                    if (thing.player != null) {
                        thing.player!!.viewz = thing._z + thing.player!!.viewheight
                        thing.player!!.lookdir = 0 // Reset lookdir
                    }

                    // spawn teleport fog at source and destination
                    fog = SpawnMobj(oldx, oldy, oldz, mobjtype_t.MT_TFOG)
                    StartSound(fog, sfxenum_t.sfx_telept)
                    an = Tables.toBAMIndex(m.angle)
                    fog = SpawnMobj(
                        m._x + 20 * Tables.finecosine[an],
                        m._y + 20 * Tables.finesine[an],
                        thing._z,
                        mobjtype_t.MT_TFOG
                    )

                    // emit sound, where?
                    StartSound(fog, sfxenum_t.sfx_telept)

                    // don't move for a bit
                    if (thing.player != null) {
                        thing.reactiontime = 18
                    }
                    thing.angle = m.angle
                    thing.momz = 0
                    thing.momy = thing.momz
                    thing.momx = thing.momy
                    return 1
                    thinker = thinker.next!! //TODO: those loops :(
                }
            }
            i++
        }
        return 0
    }

    //
    // TELEPORT MOVE
    // 
    //
    // P_TeleportMove
    //
    fun TeleportMove(
        thing: mobj_t, x: Int,  /*fixed*/
        y: Int
    ): Boolean {
        val spechits = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        val ll = levelLoader()
        val ma = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val xl: Int
        val xh: Int
        val yl: Int
        val yh: Int
        var bx: Int
        var by: Int
        val newsubsec: subsector_t

        // kill anything occupying the position
        ma.tmthing = thing
        ma.tmflags = thing.flags
        ma.tmx = x
        ma.tmy = y
        ma.tmbbox[BBox.BOXTOP] = y + thing.radius
        ma.tmbbox[BBox.BOXBOTTOM] = y - thing.radius
        ma.tmbbox[BBox.BOXRIGHT] = x + thing.radius
        ma.tmbbox[BBox.BOXLEFT] = x - thing.radius
        newsubsec = ll.PointInSubsector(x, y)
        ma.ceilingline = null

        // The base floor/ceiling is from the subsector
        // that contains the point.
        // Any contacted lines the step closer together
        // will adjust them.
        ma.tmdropoffz = newsubsec.sector!!.floorheight
        ma.tmfloorz = ma.tmdropoffz
        ma.tmceilingz = newsubsec.sector!!.ceilingheight
        sceneRenderer().increaseValidCount(1) // This is r_main's ?
        spechits.numspechit = 0

        // stomp on any things contacted
        xl = ll.getSafeBlockX(ma.tmbbox[BBox.BOXLEFT] - ll.bmaporgx - Limits.MAXRADIUS)
        xh = ll.getSafeBlockX(ma.tmbbox[BBox.BOXRIGHT] - ll.bmaporgx + Limits.MAXRADIUS)
        yl = ll.getSafeBlockY(ma.tmbbox[BBox.BOXBOTTOM] - ll.bmaporgy - Limits.MAXRADIUS)
        yh = ll.getSafeBlockY(ma.tmbbox[BBox.BOXTOP] - ll.bmaporgy + Limits.MAXRADIUS)
        bx = xl
        while (bx <= xh) {
            by = yl
            while (by <= yh) {
                if (!BlockThingsIterator(bx, by) { m: mobj_t? -> StompThing(m!!) }) {
                    return false
                }
                by++
            }
            bx++
        }

        // the move is ok,
        // so link the thing into its new position
        UnsetThingPosition(thing)
        thing.floorz = ma.tmfloorz
        thing.ceilingz = ma.tmceilingz
        thing._x = x
        thing._y = y
        ll.SetThingPosition(thing)
        return true
    }
}