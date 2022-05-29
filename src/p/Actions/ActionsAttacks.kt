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


import data.*
import defines.statenum_t
import doom.SourceCode
import doom.SourceCode.*
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedDiv
import m.fixed_t.Companion.FixedMul
import p.Actions.ActionTrait.Movement
import p.Actions.ActionsSectors.Spawn
import p.intercept_t
import p.mobj_t
import rr.line_t
import utils.C2JUtils
import utils.TraitFactory.ContextKey
import java.util.function.Supplier

interface ActionsAttacks : ActionsAim, ActionsMobj, ActionsSight, ActionsShootEvents {
    class Attacks {
        //
        // RADIUS ATTACK
        //
        var bombsource: mobj_t? = null
        var bombspot: mobj_t? = null
        var bombdamage = 0
        ///////////////////// PIT AND PTR FUNCTIONS //////////////////
        /**
         * PIT_VileCheck Detect a corpse that could be raised.
         */
        var vileCorpseHit: mobj_t? = null
        var vileObj: mobj_t? = null
        var vileTryX = 0
        var vileTryY = 0
    }

    //
    // P_GunShot
    //
    fun P_GunShot(mo: mobj_t, accurate: Boolean) {
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        var angle: Long
        val damage: Int
        damage = 5 * (P_Random() % 3 + 1)
        angle = mo.angle
        if (!accurate) {
            angle += (P_Random() - P_Random() shl 18).toLong()
        }
        LineAttack(mo, angle, Defines.MISSILERANGE, targ.bulletslope, damage)
    }

    /**
     * P_LineAttack If damage == 0, it is just a test trace that will leave linetarget set.
     *
     * @param t1
     * @param angle angle_t
     * @param distance fixed_t
     * @param slope fixed_t
     * @param damage
     */
    fun LineAttack(
        t1: mobj_t,
        @angle_t angle: Long,
        @SourceCode.fixed_t distance: Int,
        @SourceCode.fixed_t slope: Int,
        damage: Int
    ) {
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val x2: Int
        val y2: Int
        targ.shootthing = t1
        targ.la_damage = damage
        x2 = t1._x + (distance shr FRACBITS) * Tables.finecosine(angle)
        y2 = t1._y + (distance shr FRACBITS) * Tables.finesine(angle)
        targ.shootz = t1._z + (t1.height shr 1) + 8 * FRACUNIT
        targ.attackrange = distance
        targ.aimslope = slope
        PathTraverse(
            t1._x,
            t1._y,
            x2,
            y2,
            Defines.PT_ADDLINES or Defines.PT_ADDTHINGS
        ) { `in` -> ShootTraverse(`in`!!) }
    }
    //
    // RADIUS ATTACK
    //
    /**
     * P_RadiusAttack Source is the creature that caused the explosion at spot.
     */
    fun RadiusAttack(spot: mobj_t, source: mobj_t?, damage: Int) {
        val ll = levelLoader()
        val att = contextRequire<ActionsAttacks.Attacks>(ActionsAttacks.KEY_ATTACKS)
        var x: Int
        var y: Int
        val xl: Int
        val xh: Int
        val yl: Int
        val yh: Int
        @SourceCode.fixed_t val dist: Int
        dist = damage + Limits.MAXRADIUS shl FRACBITS
        yh = ll.getSafeBlockY(spot._y + dist - ll.bmaporgy)
        yl = ll.getSafeBlockY(spot._y - dist - ll.bmaporgy)
        xh = ll.getSafeBlockX(spot._x + dist - ll.bmaporgx)
        xl = ll.getSafeBlockX(spot._x - dist - ll.bmaporgx)
        att.bombspot = spot
        att.bombsource = source
        att.bombdamage = damage
        y = yl
        while (y <= yh) {
            x = xl
            while (x <= xh) {
                BlockThingsIterator(x, y) { thing: mobj_t -> this.RadiusAttack(thing) }
                x++
            }
            y++
        }
    }
    ///////////////////// PIT AND PTR FUNCTIONS //////////////////
    /**
     * PIT_VileCheck Detect a corpse that could be raised.
     */
    @P_Enemy.C(P_Enemy.PIT_VileCheck)
    fun VileCheck(thing: mobj_t): Boolean {
        val att = contextRequire<ActionsAttacks.Attacks>(ActionsAttacks.KEY_ATTACKS)
        val maxdist: Int
        val check: Boolean
        if (!C2JUtils.eval(thing.flags and mobj_t.MF_CORPSE)) {
            return true // not a monster
        }
        if (thing.mobj_tics != -1L) {
            return true // not lying still yet
        }
        if (thing.info!!.raisestate == statenum_t.S_NULL) {
            return true // monster doesn't have a raise state
        }
        maxdist = thing.info!!.radius + info.mobjinfo[mobjtype_t.MT_VILE.ordinal].radius
        if (Math.abs(thing._x - att.vileTryX) > maxdist
            || Math.abs(thing._y - att.vileTryY) > maxdist
        ) {
            return true // not actually touching
        }
        att.vileCorpseHit = thing
        thing.momy = 0
        thing.momx = thing.momy
        thing.height = thing.height shl 2
        check = CheckPosition(thing, thing._x, thing._y)
        thing.height = thing.height shr 2

        // check it doesn't fit here, or stop checking
        return !check
    }

    /**
     * PIT_RadiusAttack "bombsource" is the creature that caused the explosion at "bombspot".
     */
    @P_Map.C(P_Map.PIT_RadiusAttack)
    fun RadiusAttack(thing: mobj_t): Boolean {
        val att = contextRequire<ActionsAttacks.Attacks>(ActionsAttacks.KEY_ATTACKS)
        @SourceCode.fixed_t val dx: Int
        @SourceCode.fixed_t val dy: Int
        @SourceCode.fixed_t var dist: Int
        if (!C2JUtils.eval(thing.flags and mobj_t.MF_SHOOTABLE)) {
            return true
        }

        // Boss spider and cyborg
        // take no damage from concussion.
        if (thing.type == mobjtype_t.MT_CYBORG || thing.type == mobjtype_t.MT_SPIDER) {
            return true
        }
        dx = Math.abs(thing._x - att.bombspot!!._x)
        dy = Math.abs(thing._y - att.bombspot!!._y)
        dist = if (dx > dy) dx else dy
        dist = dist - thing.radius shr FRACBITS
        if (dist < 0) {
            dist = 0
        }
        if (dist >= att.bombdamage) {
            return true // out of range
        }
        if (CheckSight(thing, att.bombspot!!)) {
            // must be in direct path
            DamageMobj(thing, att.bombspot, att.bombsource, att.bombdamage - dist)
        }
        return true
    }

    /**
     * PTR_ShootTraverse
     *
     * 9/5/2011: Accepted _D_'s fix
     */
    @P_Map.C(P_Map.PTR_ShootTraverse)
    fun ShootTraverse(`in`: intercept_t): Boolean {
        val targ = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        val mov = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        @SourceCode.fixed_t val x: Int
        @SourceCode.fixed_t val y: Int
        @SourceCode.fixed_t val z: Int
        @SourceCode.fixed_t val frac: Int
        val li: line_t
        val th: mobj_t
        @SourceCode.fixed_t var slope: Int
        @SourceCode.fixed_t val dist: Int
        @SourceCode.fixed_t val thingtopslope: Int
        @SourceCode.fixed_t val thingbottomslope: Int
        if (`in`.isaline) {
            li = `in`.d() as line_t
            if (li.special.toInt() != 0) {
                ShootSpecialLine(targ.shootthing!!, li)
            }
            if (!C2JUtils.eval(li.flags.toInt() and line_t.ML_TWOSIDED)) {
                return gotoHitLine(`in`, li)
            }

            // crosses a two sided line
            LineOpening(li)
            dist = FixedMul(targ.attackrange, `in`.frac)
            if (li.frontsector!!.floorheight != li.backsector!!.floorheight) {
                slope = FixedDiv(mov.openbottom - targ.shootz, dist)
                if (slope > targ.aimslope) {
                    return gotoHitLine(`in`, li)
                }
            }
            if (li.frontsector!!.ceilingheight != li.backsector!!.ceilingheight) {
                slope = FixedDiv(mov.opentop - targ.shootz, dist)
                if (slope < targ.aimslope) {
                    return gotoHitLine(`in`, li)
                }
            }

            // shot continues
            return true
        }

        // shoot a thing
        th = `in`.d() as mobj_t
        if (th === targ.shootthing) {
            return true // can't shoot self
        }
        if (!C2JUtils.eval(th.flags and mobj_t.MF_SHOOTABLE)) {
            return true // corpse or something
        }
        // check angles to see if the thing can be aimed at
        dist = FixedMul(targ.attackrange, `in`.frac)
        thingtopslope = FixedDiv(th._z + th.height - targ.shootz, dist)
        if (thingtopslope < targ.aimslope) {
            return true // shot over the thing
        }
        thingbottomslope = FixedDiv(th._z - targ.shootz, dist)
        if (thingbottomslope > targ.aimslope) {
            return true // shot under the thing
        }

        // hit thing
        // position a bit closer
        frac = `in`.frac - FixedDiv(10 * FRACUNIT, targ.attackrange)
        x = targ.trace.x + FixedMul(targ.trace.dx, frac)
        y = targ.trace.y + FixedMul(targ.trace.dy, frac)
        z = targ.shootz + FixedMul(targ.aimslope, FixedMul(frac, targ.attackrange))

        // Spawn bullet puffs or blod spots,
        // depending on target type.
        if (C2JUtils.eval((`in`.d() as mobj_t).flags and mobj_t.MF_NOBLOOD)) {
            SpawnPuff(x, y, z)
        } else {
            SpawnBlood(x, y, z, targ.la_damage)
        }
        if (targ.la_damage != 0) {
            DamageMobj(th, targ.shootthing, targ.shootthing, targ.la_damage)
        }

        // don't go any farther
        return false
    }

    companion object {
        val KEY_ATTACKS: ContextKey<ActionsAttacks.Attacks> =
            ActionTrait.ACTION_KEY_CHAIN.newKey<ActionsAttacks.Attacks>(
                ActionsAttacks::class.java, Supplier { ActionsAttacks.Attacks() })
    }
}