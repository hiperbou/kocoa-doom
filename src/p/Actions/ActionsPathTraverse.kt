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
import doom.SourceCode
import doom.SourceCode.P_MapUtl
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedDiv
import m.fixed_t.Companion.FixedMul
import p.Actions.ActionsSectors.Spawn
import p.MapUtils
import p.divline_t
import p.intercept_t
import p.mobj_t
import rr.line_t
import utils.C2JUtils
import utils.GenericCopy
import utils.TraitFactory.ContextKey
import java.util.function.Predicate

interface ActionsPathTraverse : ActionsSectors {
    class Traverse {
        //////////////// PIT FUNCTION OBJECTS ///////////////////
        //
        // PIT_AddLineIntercepts.
        // Looks for lines in the given block
        // that intercept the given trace
        // to add to the intercepts list.
        //
        // A line is crossed if its endpoints
        // are on opposite sides of the trace.
        // Returns true if earlyout and a solid line hit.
        //
        var addLineDivLine = divline_t()

        //
        // PIT_AddThingIntercepts
        //
        // maybe make this a shared instance variable?
        var thingInterceptDivLine = divline_t()
        var earlyout = false
        var intercept_p = 0

        //
        // INTERCEPT ROUTINES
        //
        var intercepts = GenericCopy.malloc({ intercept_t() }, Limits.MAXINTERCEPTS)
        fun ResizeIntercepts() {
            intercepts = C2JUtils.resize(intercepts[0], intercepts, intercepts.size * 2)
        }
    }

    /**
     * P_PathTraverse Traces a line from x1,y1 to x2,y2, calling the traverser function for each. Returns true if the
     * traverser function returns true for all lines.
     */
    @P_MapUtl.C(P_MapUtl.P_PathTraverse)
    override fun PathTraverse(x1: Int, y1: Int, x2: Int, y2: Int, flags: Int, trav: Predicate<intercept_t?>): Boolean {
        var x1 = x1
        var y1 = y1
        var x2 = x2
        var y2 = y2
        val ll = levelLoader()
        val sp = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val tr = contextRequire<ActionsPathTraverse.Traverse>(ActionsPathTraverse.KEY_TRAVERSE)

        // System.out.println("Pathtraverse "+x1+" , " +y1+" to "+x2 +" , "
        // +y2);
        val xt1: Int
        val yt1: Int
        val xt2: Int
        val yt2: Int
        val _x1: Long
        val _x2: Long
        val _y1: Long
        val _y2: Long
        val mapx1: Int
        val mapy1: Int
        val xstep: Int
        val ystep: Int
        var partial: Int
        var xintercept: Int
        var yintercept: Int
        var mapx: Int
        var mapy: Int
        val mapxstep: Int
        val mapystep: Int
        var count: Int
        tr.earlyout = C2JUtils.eval(flags and Defines.PT_EARLYOUT)
        sceneRenderer().increaseValidCount(1)
        tr.intercept_p = 0
        if (x1 - ll.bmaporgx and Defines.MAPBLOCKSIZE - 1 == 0) {
            x1 += FRACUNIT // don't side exactly on a line
        }
        if (y1 - ll.bmaporgy and Defines.MAPBLOCKSIZE - 1 == 0) {
            y1 += FRACUNIT // don't side exactly on a line
        }
        sp.trace.x = x1
        sp.trace.y = y1
        sp.trace.dx = x2 - x1
        sp.trace.dy = y2 - y1

        // Code developed in common with entryway
        // for prBoom+
        _x1 = x1.toLong() - ll.bmaporgx
        _y1 = y1.toLong() - ll.bmaporgy
        xt1 = (_x1 shr Defines.MAPBLOCKSHIFT).toInt()
        yt1 = (_y1 shr Defines.MAPBLOCKSHIFT).toInt()
        mapx1 = (_x1 shr Defines.MAPBTOFRAC).toInt()
        mapy1 = (_y1 shr Defines.MAPBTOFRAC).toInt()
        _x2 = x2.toLong() - ll.bmaporgx
        _y2 = y2.toLong() - ll.bmaporgy
        xt2 = (_x2 shr Defines.MAPBLOCKSHIFT).toInt()
        yt2 = (_y2 shr Defines.MAPBLOCKSHIFT).toInt()
        x1 -= ll.bmaporgx
        y1 -= ll.bmaporgy
        x2 -= ll.bmaporgx
        y2 -= ll.bmaporgy
        if (xt2 > xt1) {
            mapxstep = 1
            partial = FRACUNIT - (mapx1 and FRACUNIT - 1)
            ystep = FixedDiv(y2 - y1, Math.abs(x2 - x1))
        } else if (xt2 < xt1) {
            mapxstep = -1
            partial = mapx1 and FRACUNIT - 1
            ystep = FixedDiv(y2 - y1, Math.abs(x2 - x1))
        } else {
            mapxstep = 0
            partial = FRACUNIT
            ystep = 256 * FRACUNIT
        }
        yintercept = mapy1 + FixedMul(partial, ystep)
        if (yt2 > yt1) {
            mapystep = 1
            partial = FRACUNIT - (mapy1 and FRACUNIT - 1)
            xstep = FixedDiv(x2 - x1, Math.abs(y2 - y1))
        } else if (yt2 < yt1) {
            mapystep = -1
            partial = mapy1 and FRACUNIT - 1
            xstep = FixedDiv(x2 - x1, Math.abs(y2 - y1))
        } else {
            mapystep = 0
            partial = FRACUNIT
            xstep = 256 * FRACUNIT
        }
        xintercept = mapx1 + FixedMul(partial, xstep)

        // Step through map blocks.
        // Count is present to prevent a round off error
        // from skipping the break.
        mapx = xt1
        mapy = yt1
        count = 0
        while (count < 64) {
            if (C2JUtils.eval(flags and Defines.PT_ADDLINES)) {
                if (!BlockLinesIterator(mapx, mapy) { ld: line_t -> AddLineIntercepts(ld) }) {
                    return false // early out
                }
            }
            if (C2JUtils.eval(flags and Defines.PT_ADDTHINGS)) {
                if (!BlockThingsIterator(mapx, mapy) { thing: mobj_t -> AddThingIntercepts(thing) }) {
                    return false // early out
                }
            }
            if (mapx == xt2
                && mapy == yt2
            ) {
                break
            }
            val changeX = yintercept shr FRACBITS == mapy
            val changeY = xintercept shr FRACBITS == mapx
            if (changeX) {
                yintercept += ystep
                mapx += mapxstep
            } else  //[MAES]: this fixed sync issues. Lookup linuxdoom
                if (changeY) {
                    xintercept += xstep
                    mapy += mapystep
                }
            count++
        }
        // go through the sorted list
        //System.out.println("Some intercepts found");
        return TraverseIntercept(trav, FRACUNIT)
    } // end method

    fun AddLineIntercepts(ld: line_t): Boolean {
        val sp = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val tr = contextRequire<ActionsPathTraverse.Traverse>(ActionsPathTraverse.KEY_TRAVERSE)
        val s1: Boolean
        val s2: Boolean
        @SourceCode.fixed_t val frac: Int

        // avoid precision problems with two routines
        if (sp.trace.dx > FRACUNIT * 16 || sp.trace.dy > FRACUNIT * 16 || sp.trace.dx < -FRACUNIT * 16 || sp.trace.dy < -FRACUNIT * 16) {
            s1 = sp.trace.PointOnDivlineSide(ld.v1x, ld.v1y)
            s2 = sp.trace.PointOnDivlineSide(ld.v2x, ld.v2y)
            //s1 = obs.trace.DivlineSide(ld.v1x, ld.v1.y);
            //s2 = obs.trace.DivlineSide(ld.v2x, ld.v2y);
        } else {
            s1 = ld.PointOnLineSide(sp.trace.x, sp.trace.y)
            s2 = ld.PointOnLineSide(sp.trace.x + sp.trace.dx, sp.trace.y + sp.trace.dy)
            //s1 = new divline_t(ld).DivlineSide(obs.trace.x, obs.trace.y);
            //s2 = new divline_t(ld).DivlineSide(obs.trace.x + obs.trace.dx, obs.trace.y + obs.trace.dy);
        }
        if (s1 == s2) {
            return true // line isn't crossed
        }
        // hit the line
        tr.addLineDivLine.MakeDivline(ld)
        frac = MapUtils.InterceptVector(sp.trace, tr.addLineDivLine)
        if (frac < 0) {
            return true // behind source
        }
        // try to early out the check
        if (tr.earlyout && frac < FRACUNIT && ld.backsector == null) {
            return false // stop checking
        }

        // "create" a new intercept in the static intercept pool.
        if (tr.intercept_p >= tr.intercepts.size) {
            tr.ResizeIntercepts()
        }
        tr.intercepts[tr.intercept_p].frac = frac
        tr.intercepts[tr.intercept_p].isaline = true
        tr.intercepts[tr.intercept_p].line = ld
        tr.intercept_p++
        return true // continue
    }

    fun AddThingIntercepts(thing: mobj_t): Boolean {
        val sp = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val tr = contextRequire<ActionsPathTraverse.Traverse>(ActionsPathTraverse.KEY_TRAVERSE)
        @SourceCode.fixed_t val x1: Int
        @SourceCode.fixed_t val y1: Int
        @SourceCode.fixed_t val x2: Int
        @SourceCode.fixed_t val y2: Int
        val s1: Boolean
        val s2: Boolean
        val tracepositive: Boolean
        @SourceCode.fixed_t val frac: Int
        tracepositive = sp.trace.dx xor sp.trace.dy > 0

        // check a corner to corner crossection for hit
        if (tracepositive) {
            x1 = thing._x - thing.radius
            y1 = thing._y + thing.radius
            x2 = thing._x + thing.radius
            y2 = thing._y - thing.radius
        } else {
            x1 = thing._x - thing.radius
            y1 = thing._y - thing.radius
            x2 = thing._x + thing.radius
            y2 = thing._y + thing.radius
        }
        s1 = sp.trace.PointOnDivlineSide(x1, y1)
        s2 = sp.trace.PointOnDivlineSide(x2, y2)
        if (s1 == s2) {
            return true // line isn't crossed
        }
        tr.thingInterceptDivLine.x = x1
        tr.thingInterceptDivLine.y = y1
        tr.thingInterceptDivLine.dx = x2 - x1
        tr.thingInterceptDivLine.dy = y2 - y1
        frac = MapUtils.InterceptVector(sp.trace, tr.thingInterceptDivLine)
        if (frac < 0) {
            return true // behind source
        }

        // "create" a new intercept in the static intercept pool.
        if (tr.intercept_p >= tr.intercepts.size) {
            tr.ResizeIntercepts()
        }
        tr.intercepts[tr.intercept_p].frac = frac
        tr.intercepts[tr.intercept_p].isaline = false
        tr.intercepts[tr.intercept_p].thing = thing
        tr.intercept_p++
        return true // keep going
    }

    //
    //P_TraverseIntercepts
    //Returns true if the traverser function returns true
    //for all lines.
    //
    fun TraverseIntercept(func: Predicate<intercept_t?>, maxfrac: Int): Boolean {
        val tr = contextRequire<ActionsPathTraverse.Traverse>(ActionsPathTraverse.KEY_TRAVERSE)
        var count: Int
        @SourceCode.fixed_t var dist: Int
        var `in`: intercept_t? = null // shut up compiler warning
        count = tr.intercept_p
        while (count-- > 0) {
            dist = Limits.MAXINT
            for (scan in 0 until tr.intercept_p) {
                if (tr.intercepts[scan].frac < dist) {
                    dist = tr.intercepts[scan].frac
                    `in` = tr.intercepts[scan]
                }
            }
            if (dist > maxfrac) {
                return true // checked everything in range      
            }
            /*  // UNUSED
            {
            // don't check these yet, there may be others inserted
            in = scan = intercepts;
            for ( scan = intercepts ; scan<intercept_p ; scan++)
                if (scan.frac > maxfrac)
                *in++ = *scan;
            intercept_p = in;
            return false;
            }
             */if (!func.test(`in`)) {
                return false // don't bother going farther
            }
            `in`!!.frac = Limits.MAXINT
        }
        return true // everything was traversed
    }

    companion object {
        val KEY_TRAVERSE: ContextKey<ActionsPathTraverse.Traverse> =
            ActionTrait.ACTION_KEY_CHAIN.newKey<ActionsPathTraverse.Traverse>(
                ActionsPathTraverse::class.java, { ActionsPathTraverse.Traverse() })
    }
}