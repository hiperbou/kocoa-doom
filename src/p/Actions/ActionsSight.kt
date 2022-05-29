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
import doom.SourceCode
import m.fixed_t.Companion.FixedDiv
import p.Actions.ActionsSectors.Spawn
import p.MapUtils
import p.divline_t
import p.mobj_t
import rr.line_t
import rr.node_t
import rr.sector_t
import rr.subsector_t
import utils.C2JUtils
import utils.TraitFactory.ContextKey
import java.util.function.Supplier

interface ActionsSight : ActionsSectors {
    class Sight {
        var sightzstart // eye z of looker
                = 0
        var strace = divline_t()

        // from t1 to t2
        var t2x = 0
        var t2y = 0
        var sightcounts = IntArray(2)
    }

    /**
     * P_CheckSight Returns true if a straight line between t1 and t2 is
     * unobstructed. Uses REJECT.
     */
    fun CheckSight(t1: mobj_t, t2: mobj_t): Boolean {
        val ll = levelLoader()
        val sight = contextRequire<Sight>(ActionsSight.KEY_SIGHT)
        val spawn = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val s1: Int
        val s2: Int
        val pnum: Int
        val bytenum: Int
        val bitnum: Int

        // First check for trivial rejection.
        // Determine subsector entries in REJECT table.
        s1 = t1.subsector!!.sector!!.id // (t1.subsector.sector - sectors);
        s2 = t2.subsector!!.sector!!.id // - sectors);
        pnum = s1 * ll.numsectors + s2
        bytenum = pnum shr 3
        bitnum = 1 shl (pnum and 7)

        // Check in REJECT table.
        if (C2JUtils.eval(ll.rejectmatrix[bytenum].toInt() and bitnum)) {
            sight.sightcounts[0]++

            // can't possibly be connected
            return false
        }

        // An unobstructed LOS is possible.
        // Now look from eyes of t1 to any part of t2.
        sight.sightcounts[1]++
        sceneRenderer().increaseValidCount(1)
        sight.sightzstart = t1._z + t1.height - (t1.height shr 2)
        spawn.topslope = t2._z + t2.height - sight.sightzstart
        spawn.bottomslope = t2._z - sight.sightzstart
        sight.strace.x = t1._x
        sight.strace.y = t1._y
        sight.t2x = t2._x
        sight.t2y = t2._y
        sight.strace.dx = t2._x - t1._x
        sight.strace.dy = t2._y - t1._y

        // the head node is the last node output
        return CrossBSPNode(ll.numnodes - 1)
    }

    /**
     * P_CrossSubsector Returns true if strace crosses the given subsector
     * successfully.
     */
    fun CrossSubsector(num: Int): Boolean {
        val sr = sceneRenderer()
        val ll = levelLoader()
        val spawn = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val sight = contextRequire<Sight>(ActionsSight.KEY_SIGHT)
        var seg: Int // pointer inside segs
        var line: line_t
        var s1: Int
        var s2: Int
        var count: Int
        val sub: subsector_t
        var front: sector_t
        var back: sector_t
        @SourceCode.fixed_t var opentop: Int
        var openbottom: Int
        val divl = divline_t()
        //vertex_t v1;
        //vertex_t v2;
        @SourceCode.fixed_t var frac: Int
        var slope: Int
        if (Defines.RANGECHECK) {
            if (num >= ll.numsubsectors) {
                doomSystem().Error("P_CrossSubsector: ss %d with numss = %d", num, ll.numsubsectors)
            }
        }
        sub = ll.subsectors[num]

        // check lines
        count = sub.numlines
        seg = sub.firstline // LL.segs[sub.firstline];
        while (count > 0) {
            line = ll.segs[seg].linedef!!

            // allready checked other side?
            if (line.validcount == sr.getValidCount()) {
                seg++
                count--
                continue
            }
            line.validcount = sr.getValidCount()

            //v1 = line.v1;
            //v2 = line.v2;
            s1 = sight.strace.DivlineSide(line.v1x, line.v1y)
            s2 = sight.strace.DivlineSide(line.v2x, line.v2y)

            // line isn't crossed?
            if (s1 == s2) {
                seg++
                count--
                continue
            }
            divl.x = line.v1x
            divl.y = line.v1y
            divl.dx = line.v2x - line.v1x
            divl.dy = line.v2y - line.v1y
            s1 = divl.DivlineSide(sight.strace.x, sight.strace.y)
            s2 = divl.DivlineSide(sight.t2x, sight.t2y)

            // line isn't crossed?
            if (s1 == s2) {
                seg++
                count--
                continue
            }

            // stop because it is not two sided anyway
            // might do this after updating validcount?
            if (!C2JUtils.flags(line.flags.toInt(), line_t.ML_TWOSIDED)) {
                return false
            }

            // crosses a two sided line
            front = ll.segs[seg].frontsector!!
            back = ll.segs[seg].backsector!!

            // no wall to block sight with?
            if (front.floorheight == back.floorheight
                && front.ceilingheight == back.ceilingheight
            ) {
                seg++
                count--
                continue
            }

            // possible occluder
            // because of ceiling height differences
            opentop = if (front.ceilingheight < back.ceilingheight) {
                front.ceilingheight
            } else {
                back.ceilingheight
            }

            // because of ceiling height differences
            openbottom = if (front.floorheight > back.floorheight) {
                front.floorheight
            } else {
                back.floorheight
            }

            // quick test for totally closed doors
            if (openbottom >= opentop) {
                return false // stop
            }
            frac = MapUtils.P_InterceptVector(sight.strace, divl)
            if (front.floorheight != back.floorheight) {
                slope = FixedDiv(openbottom - sight.sightzstart, frac)
                if (slope > spawn.bottomslope) {
                    spawn.bottomslope = slope
                }
            }
            if (front.ceilingheight != back.ceilingheight) {
                slope = FixedDiv(opentop - sight.sightzstart, frac)
                if (slope < spawn.topslope) {
                    spawn.topslope = slope
                }
            }
            if (spawn.topslope <= spawn.bottomslope) {
                return false // stop
            }
            seg++
            count--
        }
        // passed the subsector ok
        return true
    }

    /**
     * P_CrossBSPNode Returns true if strace crosses the given node
     * successfully.
     */
    fun CrossBSPNode(bspnum: Int): Boolean {
        val ll = levelLoader()
        val sight = contextRequire<Sight>(ActionsSight.KEY_SIGHT)
        val bsp: node_t
        var side: Int
        if (C2JUtils.eval(bspnum and Defines.NF_SUBSECTOR)) {
            return if (bspnum == -1) {
                CrossSubsector(0)
            } else {
                CrossSubsector(bspnum and Defines.NF_SUBSECTOR.inv())
            }
        }
        bsp = ll.nodes[bspnum]

        // decide which side the start point is on
        side = bsp.DivlineSide(sight.strace.x, sight.strace.y)
        if (side == 2) {
            side = 0 // an "on" should cross both sides
        }

        // cross the starting side
        if (!CrossBSPNode(bsp.children[side])) {
            return false
        }

        // the partition plane is crossed here
        return if (side == bsp.DivlineSide(sight.t2x, sight.t2y)) {
            // the line doesn't touch the other side
            true
        } else CrossBSPNode(bsp.children[side xor 1])

        // cross the ending side
    }

    companion object {
        val KEY_SIGHT: ContextKey<Sight> =
            ActionTrait.ACTION_KEY_CHAIN.newKey<Sight>(ActionsSight::class.java, Supplier { Sight() })
    }
}