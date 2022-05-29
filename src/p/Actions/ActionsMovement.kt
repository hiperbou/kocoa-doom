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
import defines.slopetype_t
import defines.statenum_t
import doom.SourceCode
import doom.SourceCode.P_Map
import doom.player_t
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedMul
import p.Actions.ActionTrait.*
import p.ChaseDirections
import p.MapUtils
import p.intercept_t
import p.mobj_t
import p.mobj_t.Companion.MF_DROPOFF
import p.mobj_t.Companion.MF_NOCLIP
import p.mobj_t.Companion.MF_TELEPORT
import rr.line_t
import utils.C2JUtils
import utils.TraitFactory.ContextKey
import java.util.function.Supplier

interface ActionsMovement : ActionsPathTraverse {
    fun UnsetThingPosition(thing: mobj_t)
    fun ExplodeMissile(mo: mobj_t)
    class DirType {
        //dirtype
        var d1 = 0
        var d2 = 0
    }
    ///////////////// MOVEMENT'S ACTIONS ////////////////////////
    /**
     * If "floatok" true, move would be ok if within "tmfloorz - tmceilingz".
     */
    //
    // P_Move
    // Move in the current direction,
    // returns false if the move is blocked.
    //
    fun Move(actor: mobj_t): Boolean {
        val mov = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val sp = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        @SourceCode.fixed_t val tryx: Int
        @SourceCode.fixed_t val tryy: Int
        var ld: line_t?

        // warning: 'catch', 'throw', and 'try'
        // are all C++ reserved words
        val try_ok: Boolean
        var good: Boolean
        if (actor.movedir == ChaseDirections.DI_NODIR) {
            return false
        }
        if (actor.movedir >= 8) {
            doomSystem().Error("Weird actor.movedir!")
        }

        val info = actor.info!!

        tryx = actor._x + info.speed * ChaseDirections.xspeed[actor.movedir]
        tryy = actor._y + info.speed * ChaseDirections.yspeed[actor.movedir]
        try_ok = TryMove(actor, tryx, tryy)
        if (!try_ok) {
            // open any specials
            if (C2JUtils.eval(actor.flags and mobj_t.MF_FLOAT) && mov.floatok) {
                // must adjust height
                if (actor._z < mov.tmfloorz) {
                    actor._z += Defines.FLOATSPEED
                } else {
                    actor._z -= Defines.FLOATSPEED
                }
                actor.flags = actor.flags or mobj_t.MF_INFLOAT
                return true
            }
            if (sp.numspechit == 0) {
                return false
            }
            actor.movedir = ChaseDirections.DI_NODIR
            good = false
            while (sp.numspechit-- > 0) {
                ld = sp.spechit[sp.numspechit]
                // if the special is not a door
                // that can be opened,
                // return false
                if (UseSpecialLine(actor, ld!!, false)) {
                    good = true
                }
            }
            return good
        } else {
            actor.flags = actor.flags and mobj_t.MF_INFLOAT.inv()
        }
        if (!C2JUtils.eval(actor.flags and mobj_t.MF_FLOAT)) {
            actor._z = actor.floorz
        }
        return true
    }

    /**
     * // P_TryMove // Attempt to move to a new position, // crossing special lines unless MF_TELEPORT is set.
     *
     * @param x fixed_t
     * @param y fixed_t
     */
    fun TryMove(thing: mobj_t, @SourceCode.fixed_t x: Int, @SourceCode.fixed_t y: Int): Boolean {
        val mov = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val sp = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        @SourceCode.fixed_t val oldx: Int
        @SourceCode.fixed_t val oldy: Int
        var side: Boolean
        var oldside: Boolean // both were int
        var ld: line_t
        mov.floatok = false
        if (!CheckPosition(thing, x, y)) {
            return false // solid wall or thing
        }
        if (!C2JUtils.eval(thing.flags and MF_NOCLIP)) {
            if (mov.tmceilingz - mov.tmfloorz < thing.height) {
                return false // doesn't fit
            }
            mov.floatok = true
            if (!C2JUtils.eval(thing.flags and MF_TELEPORT) && mov.tmceilingz - thing._z < thing.height) {
                return false // mobj must lower itself to fit
            }
            if (!C2JUtils.eval(thing.flags and MF_TELEPORT) && mov.tmfloorz - thing._z > 24 * FRACUNIT) {
                return false // too big a step up
            }
            if (!C2JUtils.eval(thing.flags and (MF_DROPOFF or mobj_t.MF_FLOAT)) && mov.tmfloorz - mov.tmdropoffz > 24 * FRACUNIT) {
                return false // don't stand over a dropoff
            }
        }

        // the move is ok,
        // so link the thing into its new position
        UnsetThingPosition(thing)
        oldx = thing._x
        oldy = thing._y
        thing.floorz = mov.tmfloorz
        thing.ceilingz = mov.tmceilingz
        thing._x = x
        thing._y = y
        levelLoader().SetThingPosition(thing)

        // if any special lines were hit, do the effect
        if (!C2JUtils.eval(thing.flags and (MF_TELEPORT or MF_NOCLIP))) {
            while (sp.numspechit-- > 0) {
                // see if the line was crossed
                ld = sp.spechit[sp.numspechit]!!
                side = ld.PointOnLineSide(thing._x, thing._y)
                oldside = ld.PointOnLineSide(oldx, oldy)
                if (side != oldside) {
                    if (ld.special.toInt() != 0) {
                        CrossSpecialLine(ld, if (oldside) 1 else 0, thing)
                    }
                }
            }
        }
        return true
    }

    fun NewChaseDir(actor: mobj_t) {
        val dirtype = contextRequire<DirType>(ActionsMovement.KEY_DIRTYPE)
        @SourceCode.fixed_t val deltax: Int
        @SourceCode.fixed_t val deltay: Int
        var tdir: Int
        val olddir: Int
        // dirtypes
        val turnaround: Int
        if (actor.target == null) {
            doomSystem().Error("P_NewChaseDir: called with no target")
        }
        olddir = actor.movedir
        turnaround = ChaseDirections.opposite[olddir]
        deltax = actor.target!!._x - actor._x
        deltay = actor.target!!._y - actor._y
        if (deltax > 10 * FRACUNIT) {
            dirtype.d1 = ChaseDirections.DI_EAST
        } else if (deltax < -10 * FRACUNIT) {
            dirtype.d1 = ChaseDirections.DI_WEST
        } else {
            dirtype.d1 = ChaseDirections.DI_NODIR
        }
        if (deltay < -10 * FRACUNIT) {
            dirtype.d2 = ChaseDirections.DI_SOUTH
        } else if (deltay > 10 * FRACUNIT) {
            dirtype.d2 = ChaseDirections.DI_NORTH
        } else {
            dirtype.d2 = ChaseDirections.DI_NODIR
        }

        // try direct route
        if (dirtype.d1 != ChaseDirections.DI_NODIR && dirtype.d2 != ChaseDirections.DI_NODIR) {
            actor.movedir = ChaseDirections.diags[(C2JUtils.eval(deltay < 0) shl 1) + C2JUtils.eval(deltax > 0)]
            if (actor.movedir != turnaround && TryWalk(actor)) {
                return
            }
        }

        // try other directions
        if (P_Random() > 200 || Math.abs(deltay) > Math.abs(deltax)) {
            tdir = dirtype.d1
            dirtype.d1 = dirtype.d2
            dirtype.d2 = tdir
        }
        if (dirtype.d1 == turnaround) {
            dirtype.d1 = ChaseDirections.DI_NODIR
        }
        if (dirtype.d2 == turnaround) {
            dirtype.d2 = ChaseDirections.DI_NODIR
        }
        if (dirtype.d1 != ChaseDirections.DI_NODIR) {
            actor.movedir = dirtype.d1
            if (TryWalk(actor)) {
                // either moved forward or attacked
                return
            }
        }
        if (dirtype.d2 != ChaseDirections.DI_NODIR) {
            actor.movedir = dirtype.d2
            if (TryWalk(actor)) {
                return
            }
        }

        // there is no direct path to the player,
        // so pick another direction.
        if (olddir != ChaseDirections.DI_NODIR) {
            actor.movedir = olddir
            if (TryWalk(actor)) {
                return
            }
        }

        // randomly determine direction of search
        if (C2JUtils.eval(P_Random() and 1)) {
            tdir = ChaseDirections.DI_EAST
            while (tdir <= ChaseDirections.DI_SOUTHEAST) {
                if (tdir != turnaround) {
                    actor.movedir = tdir
                    if (TryWalk(actor)) {
                        return
                    }
                }
                tdir++
            }
        } else {
            tdir = ChaseDirections.DI_SOUTHEAST
            while (tdir != ChaseDirections.DI_EAST - 1) {
                if (tdir != turnaround) {
                    actor.movedir = tdir
                    if (TryWalk(actor)) {
                        return
                    }
                }
                tdir--
            }
        }
        if (turnaround != ChaseDirections.DI_NODIR) {
            actor.movedir = turnaround
            if (TryWalk(actor)) {
                return
            }
        }
        actor.movedir = ChaseDirections.DI_NODIR // can not move
    }

    /**
     * TryWalk Attempts to move actor on in its current (ob.moveangle) direction. If blocked by either a wall or an
     * actor returns FALSE If move is either clear or blocked only by a door, returns TRUE and sets... If a door is in
     * the way, an OpenDoor call is made to start it opening.
     */
    fun TryWalk(actor: mobj_t): Boolean {
        if (!Move(actor)) {
            return false
        }
        actor.movecount = P_Random() and 15
        return true
    }

    //
    // P_HitSlideLine
    // Adjusts the xmove / ymove
    // so that the next move will slide along the wall.
    //
    fun HitSlideLine(ld: line_t) {
        val sr = sceneRenderer()
        val slideMove = contextRequire<SlideMove>(ActionTrait.KEY_SLIDEMOVE)
        val side: Boolean

        // all angles
        var lineangle: Long
        val moveangle: Long
        var deltaangle: Long
        @SourceCode.fixed_t val movelen: Int
        @SourceCode.fixed_t val newlen: Int
        if (ld.slopetype == slopetype_t.ST_HORIZONTAL) {
            slideMove.tmymove = 0
            return
        }
        if (ld.slopetype == slopetype_t.ST_VERTICAL) {
            slideMove.tmxmove = 0
            return
        }
        side = ld.PointOnLineSide(slideMove.slidemo!!._x, slideMove.slidemo!!._y)
        lineangle = sr.PointToAngle2(0, 0, ld.dx, ld.dy)
        if (side == true) {
            lineangle += Tables.ANG180
        }
        moveangle = sr.PointToAngle2(0, 0, slideMove.tmxmove, slideMove.tmymove)
        deltaangle = moveangle - lineangle and Tables.BITS32
        if (deltaangle > Tables.ANG180) {
            deltaangle += Tables.ANG180
        }
        //  system.Error ("SlideLine: ang>ANG180");

        //lineangle >>>= ANGLETOFINESHIFT;
        //deltaangle >>>= ANGLETOFINESHIFT;
        movelen = MapUtils.AproxDistance(slideMove.tmxmove, slideMove.tmymove)
        newlen = FixedMul(movelen, Tables.finecosine(deltaangle))
        slideMove.tmxmove = FixedMul(newlen, Tables.finecosine(lineangle))
        slideMove.tmymove = FixedMul(newlen, Tables.finesine(lineangle))
    }

    ///(FRACUNIT/MAPFRACUNIT);
    //
    // P_SlideMove
    // The momx / momy move is bad, so try to slide
    // along a wall.
    // Find the first line hit, move flush to it,
    // and slide along it
    //
    // This is a kludgy mess.
    //
    fun SlideMove(mo: mobj_t) {
        val slideMove = contextRequire<SlideMove>(ActionTrait.KEY_SLIDEMOVE)
        @SourceCode.fixed_t var leadx: Int
        @SourceCode.fixed_t var leady: Int
        @SourceCode.fixed_t var trailx: Int
        @SourceCode.fixed_t var traily: Int
        @SourceCode.fixed_t var newx: Int
        @SourceCode.fixed_t var newy: Int
        var hitcount: Int
        slideMove.slidemo = mo
        hitcount = 0
        do {
            if (++hitcount == 3) {
                // goto stairstep
                stairstep(mo)
                return
            } // don't loop forever

            // trace along the three leading corners
            if (mo.momx > 0) {
                leadx = mo._x + mo.radius
                trailx = mo._x - mo.radius
            } else {
                leadx = mo._x - mo.radius
                trailx = mo._x + mo.radius
            }
            if (mo.momy > 0) {
                leady = mo._y + mo.radius
                traily = mo._y - mo.radius
            } else {
                leady = mo._y - mo.radius
                traily = mo._y + mo.radius
            }
            slideMove.bestslidefrac = FRACUNIT + 1
            PathTraverse(
                leadx,
                leady,
                leadx + mo.momx,
                leady + mo.momy,
                Defines.PT_ADDLINES
            ) { SlideTraverse(it!!) }
            PathTraverse(
                trailx,
                leady,
                trailx + mo.momx,
                leady + mo.momy,
                Defines.PT_ADDLINES
            ) { SlideTraverse(it!!) }
            PathTraverse(
                leadx,
                traily,
                leadx + mo.momx,
                traily + mo.momy,
                Defines.PT_ADDLINES
            ) { SlideTraverse(it!!) }

            // move up to the wall
            if (slideMove.bestslidefrac == FRACUNIT + 1) {
                // the move most have hit the middle, so stairstep
                stairstep(mo)
                return
            } // don't loop forever

            // fudge a bit to make sure it doesn't hit
            slideMove.bestslidefrac -= ActionsMovement.FUDGE
            if (slideMove.bestslidefrac > 0) {
                newx = FixedMul(mo.momx, slideMove.bestslidefrac)
                newy = FixedMul(mo.momy, slideMove.bestslidefrac)
                if (!TryMove(mo, mo._x + newx, mo._y + newy)) {
                    // goto stairstep
                    stairstep(mo)
                    return
                } // don't loop forever
            }

            // Now continue along the wall.
            // First calculate remainder.
            slideMove.bestslidefrac =
                FRACUNIT - (slideMove.bestslidefrac + ActionsMovement.FUDGE)
            if (slideMove.bestslidefrac > FRACUNIT) {
                slideMove.bestslidefrac = FRACUNIT
            }
            if (slideMove.bestslidefrac <= 0) {
                return
            }
            slideMove.tmxmove = FixedMul(mo.momx, slideMove.bestslidefrac)
            slideMove.tmymove = FixedMul(mo.momy, slideMove.bestslidefrac)
            HitSlideLine(slideMove.bestslideline!!) // clip the moves
            mo.momx = slideMove.tmxmove
            mo.momy = slideMove.tmymove
        } // goto retry
        while (!TryMove(mo, mo._x + slideMove.tmxmove, mo._y + slideMove.tmymove))
    }

    /**
     * Fugly "goto stairstep" simulation
     *
     * @param mo
     */
    fun stairstep(mo: mobj_t) {
        if (!TryMove(mo, mo._x, mo._y + mo.momy)) {
            TryMove(mo, mo._x + mo.momx, mo._y)
        }
    }

    //
    // P_XYMovement  
    //
    fun XYMovement(mo: mobj_t) {
        val mv = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        @SourceCode.fixed_t var ptryx: Int
        @SourceCode.fixed_t var ptryy: Int // pointers to fixed_t ???
        @SourceCode.fixed_t var xmove: Int
        @SourceCode.fixed_t var ymove: Int
        val player: player_t?
        if (mo.momx == 0 && mo.momy == 0) {
            if (mo.flags and mobj_t.MF_SKULLFLY != 0) {
                // the skull slammed into something
                mo.flags = mo.flags and mobj_t.MF_SKULLFLY.inv()
                mo.momz = 0
                mo.momy = mo.momz
                mo.momx = mo.momy
                mo.SetMobjState(mo.info!!.spawnstate)
            }
            return
        }
        player = mo.player
        if (mo.momx > Limits.MAXMOVE) {
            mo.momx = Limits.MAXMOVE
        } else if (mo.momx < -Limits.MAXMOVE) {
            mo.momx = -Limits.MAXMOVE
        }
        if (mo.momy > Limits.MAXMOVE) {
            mo.momy = Limits.MAXMOVE
        } else if (mo.momy < -Limits.MAXMOVE) {
            mo.momy = -Limits.MAXMOVE
        }
        xmove = mo.momx
        ymove = mo.momy
        do {
            if (xmove > Limits.MAXMOVE / 2 || ymove > Limits.MAXMOVE / 2) {
                ptryx = mo._x + xmove / 2
                ptryy = mo._y + ymove / 2
                xmove = xmove shr 1
                ymove = ymove shr 1
            } else {
                ptryx = mo._x + xmove
                ptryy = mo._y + ymove
                ymove = 0
                xmove = ymove
            }
            if (!TryMove(mo, ptryx, ptryy)) {
                // blocked move
                if (mo.player != null) {   // try to slide along it
                    SlideMove(mo)
                } else if (C2JUtils.eval(mo.flags and mobj_t.MF_MISSILE)) {
                    // explode a missile
                    if (mv.ceilingline != null && mv.ceilingline!!.backsector != null && mv.ceilingline!!.backsector!!.ceilingpic.toInt() == DOOM().textureManager.getSkyFlatNum()) {
                        // Hack to prevent missiles exploding
                        // against the sky.
                        // Does not handle sky floors.
                        RemoveMobj(mo)
                        return
                    }
                    ExplodeMissile(mo)
                } else {
                    mo.momy = 0
                    mo.momx = mo.momy
                }
            }
        } while (xmove or ymove != 0)

        // slow down
        if (player != null && C2JUtils.eval(player.cheats and player_t.CF_NOMOMENTUM)) {
            // debug option for no sliding at all
            mo.momy = 0
            mo.momx = mo.momy
            return
        }
        if (C2JUtils.eval(mo.flags and (mobj_t.MF_MISSILE or mobj_t.MF_SKULLFLY))) {
            return  // no friction for missiles ever
        }
        if (mo._z > mo.floorz) {
            return  // no friction when airborne
        }
        if (C2JUtils.eval(mo.flags and mobj_t.MF_CORPSE)) {
            // do not stop sliding
            //  if halfway off a step with some momentum
            if (mo.momx > FRACUNIT / 4 || mo.momx < -FRACUNIT / 4 || mo.momy > FRACUNIT / 4 || mo.momy < -FRACUNIT / 4) {
                if (mo.floorz != mo.subsector!!.sector!!.floorheight) {
                    return
                }
            }
        }
        if (mo.momx > -STOPSPEED && mo.momx < STOPSPEED && mo.momy > -STOPSPEED && mo.momy < STOPSPEED && (player == null || player.cmd.forwardmove.toInt() == 0 && player.cmd.sidemove.toInt() == 0)) {
            // if in a walking frame, stop moving
            // TODO: we need a way to get state indexed inside of states[], to sim pointer arithmetic.
            // FIX: added an "id" field.
            if (player != null && player.mo!!.mobj_state!!.id - statenum_t.S_PLAY_RUN1.ordinal < 4) {
                player.mo!!.SetMobjState(statenum_t.S_PLAY)
            }
            mo.momx = 0
            mo.momy = 0
        } else {
            mo.momx = FixedMul(mo.momx, ActionsMovement.FRICTION)
            mo.momy = FixedMul(mo.momy, ActionsMovement.FRICTION)
        }
    }

    //
    // SLIDE MOVE
    // Allows the player to slide along any angled walls.
    //
    // fixed
    //
    // PTR_SlideTraverse
    //   
    @P_Map.C(P_Map.PTR_SlideTraverse)
    fun SlideTraverse(`in`: intercept_t): Boolean {
        val slideMove = contextRequire<SlideMove>(ActionTrait.KEY_SLIDEMOVE)
        val slidemo = slideMove.slidemo!!
        val ma = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val li: line_t
        if (!`in`.isaline) {
            doomSystem().Error("PTR_SlideTraverse: not a line?")
        }
        li = `in`.d() as line_t
        if (!C2JUtils.eval(li.flags.toInt() and line_t.ML_TWOSIDED)) {
            return if (li.PointOnLineSide(slidemo._x, slidemo._y)) {
                // don't hit the back side
                true
            } else isblocking(`in`, li)
        }

        // set openrange, opentop, openbottom
        LineOpening(li)
        return if (ma.openrange < slidemo.height || ma.opentop - slidemo._z < slidemo.height || ma.openbottom - slidemo._z > 24 * FRACUNIT) // too big a step up
        {
            if (`in`.frac < slideMove.bestslidefrac) {
                slideMove.secondslidefrac = slideMove.bestslidefrac
                slideMove.secondslideline = slideMove.bestslideline
                slideMove.bestslidefrac = `in`.frac
                slideMove.bestslideline = li
            }
            false // stop
        } else { // this line doesn't block movement
            true
        }
    }

    companion object {
        val KEY_DIRTYPE: ContextKey<DirType> =
            ActionTrait.ACTION_KEY_CHAIN.newKey<DirType>(ActionsMovement::class.java, Supplier { DirType() })

        //
        // P_XYMovement
        //
        const val STOPSPEED = 4096
        const val FRICTION = 59392
        const val FUDGE = 2048 ///(FRACUNIT/MAPFRACUNIT);
    }
}