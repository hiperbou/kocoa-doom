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


import automap.IAutoMap
import data.Limits
import data.sounds.sfxenum_t
import defines.skill_t
import doom.DoomMain
import doom.SourceCode
import doom.SourceCode.*
import doom.player_t
import hu.IHeadsUp
import i.IDoomSystem
import m.BBox
import p.*
import p.Actions.ActionTrait.*
import p.UnifiedGameMap.Switches
import rr.SceneRenderer
import rr.line_t
import rr.sector_t
import rr.subsector_t
import s.ISoundOrigin
import st.IDoomStatusBar
import utils.C2JUtils
import utils.TraitFactory.ContextKey
import utils.TraitFactory.KeyChain
import utils.TraitFactory.Trait
import java.util.function.Predicate
import java.util.function.Supplier

interface ActionTrait : Trait, ThinkerList {
    fun levelLoader(): AbstractLevelLoader
    fun headsUp(): IHeadsUp
    fun doomSystem(): IDoomSystem
    fun statusBar(): IDoomStatusBar
    fun autoMap(): IAutoMap<*, *>
    fun sceneRenderer(): SceneRenderer<*, *>
    val specials: UnifiedGameMap.Specials
    val switches: Switches
    val thinkers: ActionsThinkers
    val enemies: ActionsEnemies
    val attacks: ActionsAttacks
    fun StopSound(origin: ISoundOrigin?) // DOOM.doomSound.StopSound
    fun StartSound(origin: ISoundOrigin?, s: sfxenum_t?) // DOOM.doomSound.StartSound
    fun StartSound(origin: ISoundOrigin?, s: Int) // DOOM.doomSound.StartSound
    fun getPlayer(number: Int): player_t? //DOOM.players[]

    // DOOM.gameskill
    val gameSkill: skill_t?
    fun createMobj(): mobj_t // mobj_t.from(DOOM);
    fun LevelTime(): Int // DOOM.leveltime
    fun P_Random(): Int
    fun ConsolePlayerNumber(): Int // DOOM.consoleplayer
    fun MapNumber(): Int // DOOM.gamemap
    fun PlayerInGame(number: Int): Boolean // DOOM.palyeringame
    fun IsFastParm(): Boolean // DOOM.fastparm
    fun IsPaused(): Boolean // DOOM.paused
    fun IsNetGame(): Boolean // DOOM.netgame
    fun IsDemoPlayback(): Boolean // DOOM.demoplayback
    fun IsDeathMatch(): Boolean // DOOM.deathmatch
    fun IsAutoMapActive(): Boolean // DOOM.automapactive
    fun IsMenuActive(): Boolean // DOOM.menuactive
    fun CheckThing(m: mobj_t): Boolean
    fun StompThing(m: mobj_t): Boolean
    fun SetThingPosition(mobj: mobj_t) {
        levelLoader().SetThingPosition(mobj)
    }

    /**
     * Try to avoid.
     */
    fun DOOM(): DoomMain<*, *>
    class SlideMove {
        //
        // SLIDE MOVE
        // Allows the player to slide along any angled walls.
        //
        var slidemo: mobj_t? = null

        @SourceCode.fixed_t
        var bestslidefrac = 0

        @SourceCode.fixed_t
        var secondslidefrac = 0
        var bestslideline: line_t? = null
        var secondslideline: line_t? = null

        @SourceCode.fixed_t
        var tmxmove = 0

        @SourceCode.fixed_t
        var tmymove = 0
    }

    class Spechits {
        var spechit = arrayOfNulls<line_t>(Limits.MAXSPECIALCROSS)
        var numspechit = 0

        //
        // USE LINES
        //
        var usething: mobj_t? = null
    }

    ///////////////// MOVEMENT'S ACTIONS ////////////////////////
    class Movement {
        /**
         * If "floatok" true, move would be ok if within "tmfloorz - tmceilingz".
         */
        var floatok = false

        @SourceCode.fixed_t
        var tmfloorz = 0

        @SourceCode.fixed_t
        var tmceilingz = 0

        @SourceCode.fixed_t
        var tmdropoffz = 0

        // keep track of the line that lowers the ceiling,
        // so missiles don't explode against sky hack walls
        var ceilingline: line_t? = null

        @SourceCode.fixed_t
        var tmbbox = IntArray(4)
        var tmthing: mobj_t? = null
        var tmflags = 0

        @SourceCode.fixed_t
        var tmx = 0

        @SourceCode.fixed_t
        var tmy = 0

        ////////////////////// FROM p_maputl.c ////////////////////
        @SourceCode.fixed_t
        var opentop = 0

        @SourceCode.fixed_t
        var openbottom = 0

        @SourceCode.fixed_t
        var openrange = 0

        @SourceCode.fixed_t
        var lowfloor = 0
    }

    /**
     * P_LineOpening Sets opentop and openbottom to the window through a two
     * sided line. OPTIMIZE: keep this precalculated
     */
    fun LineOpening(linedef: line_t) {
        val ma = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val front: sector_t
        val back: sector_t
        if (linedef.sidenum[1] == line_t.NO_INDEX) {
            // single sided line
            ma.openrange = 0
            return
        }
        front = linedef.frontsector!!
        back = linedef.backsector!!
        if (front.ceilingheight < back.ceilingheight) {
            ma.opentop = front.ceilingheight
        } else {
            ma.opentop = back.ceilingheight
        }
        if (front.floorheight > back.floorheight) {
            ma.openbottom = front.floorheight
            ma.lowfloor = back.floorheight
        } else {
            ma.openbottom = back.floorheight
            ma.lowfloor = front.floorheight
        }
        ma.openrange = ma.opentop - ma.openbottom
    }

    //
    //P_BlockThingsIterator
    //
    @SourceCode.Exact
    @P_MapUtl.C(P_MapUtl.P_BlockThingsIterator)
    fun BlockThingsIterator(x: Int, y: Int, func: Predicate<mobj_t>): Boolean {
        val ll = levelLoader()
        var mobj: mobj_t?
        if (x < 0 || y < 0 || x >= ll.bmapwidth || y >= ll.bmapheight) {
            return true
        }
        mobj = ll.blocklinks!![y * ll.bmapwidth + x]
        while (mobj != null) {
            if (!func.test(mobj)) {
                return false
            }
            mobj = mobj.bnext as mobj_t?
        }
        return true
    }
    //
    // SECTOR HEIGHT CHANGING
    // After modifying a sectors floor or ceiling height,
    // call this routine to adjust the positions
    // of all things that touch the sector.
    //
    // If anything doesn't fit anymore, true will be returned.
    // If crunch is true, they will take damage
    //  as they are being crushed.
    // If Crunch is false, you should set the sector height back
    //  the way it was and call P_ChangeSector again
    //  to undo the changes.
    //
    /**
     * P_BlockLinesIterator The validcount flags are used to avoid checking lines that are marked in multiple mapblocks,
     * so increment validcount before the first call to P_BlockLinesIterator, then make one or more calls to it.
     */
    @P_MapUtl.C(P_MapUtl.P_BlockLinesIterator)
    fun BlockLinesIterator(x: Int, y: Int, func: Predicate<line_t>): Boolean {
        val ll = levelLoader()
        val sr = sceneRenderer()
        var offset: Int
        var lineinblock: Int
        var ld: line_t
        if (x < 0 || y < 0 || x >= ll.bmapwidth || y >= ll.bmapheight) {
            return true
        }

        // This gives us the index to look up (in blockmap)
        offset = y * ll.bmapwidth + x

        // The index contains yet another offset, but this time 
        offset = ll.blockmap[offset]

        // MAES: blockmap terminating marker is always -1
        @Compatible("validcount") val validcount = sr.getValidCount()

        // [SYNC ISSUE]: don't skip offset+1 :-/
        @Compatible("list = blockmaplump+offset ; *list != -1 ; list++") var list = offset
        while (ll.blockmap[list].also { lineinblock = it } != -1) {
            ld = ll.lines[lineinblock]
            //System.out.println(ld);
            if (ld.validcount == validcount) {
                list++
                continue  // line has already been checked
            }
            ld.validcount = validcount
            if (!func.test(ld)) {
                return false
            }
            list++
        }
        return true // everything was checked
    }

    // keep track of the line that lowers the ceiling,
    // so missiles don't explode against sky hack walls
    fun ResizeSpechits() {
        val spechits = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        spechits.spechit = C2JUtils.resize(spechits.spechit[0], spechits.spechit, spechits.spechit.size * 2)
    }

    /**
     * PIT_CheckLine Adjusts tmfloorz and tmceilingz as lines are contacted
     *
     */
    @P_Map.C(P_Map.PIT_CheckLine)
    fun CheckLine(ld: line_t): Boolean {
        val spechits = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        val ma = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        if (ma.tmbbox[BBox.BOXRIGHT] <= ld.bbox[BBox.BOXLEFT] || ma.tmbbox[BBox.BOXLEFT] >= ld.bbox[BBox.BOXRIGHT] || ma.tmbbox[BBox.BOXTOP] <= ld.bbox[BBox.BOXBOTTOM] || ma.tmbbox[BBox.BOXBOTTOM] >= ld.bbox[BBox.BOXTOP]) {
            return true
        }
        if (ld.BoxOnLineSide(ma.tmbbox) != -1) {
            return true
        }

        // A line has been hit
        // The moving thing's destination position will cross
        // the given line.
        // If this should not be allowed, return false.
        // If the line is special, keep track of it
        // to process later if the move is proven ok.
        // NOTE: specials are NOT sorted by order,
        // so two special lines that are only 8 pixels apart
        // could be crossed in either order.
        if (ld.backsector == null) {
            return false // one sided line
        }
        if (!C2JUtils.eval(ma.tmthing!!.flags and mobj_t.MF_MISSILE)) {
            if (C2JUtils.eval(ld.flags.toInt() and line_t.ML_BLOCKING)) {
                return false // explicitly blocking everything
            }
            if (ma.tmthing!!.player == null && C2JUtils.eval(ld.flags.toInt() and line_t.ML_BLOCKMONSTERS)) {
                return false // block monsters only
            }
        }

        // set openrange, opentop, openbottom
        LineOpening(ld)

        // adjust floor / ceiling heights
        if (ma.opentop < ma.tmceilingz) {
            ma.tmceilingz = ma.opentop
            ma.ceilingline = ld
        }
        if (ma.openbottom > ma.tmfloorz) {
            ma.tmfloorz = ma.openbottom
        }
        if (ma.lowfloor < ma.tmdropoffz) {
            ma.tmdropoffz = ma.lowfloor
        }

        // if contacted a special line, add it to the list
        if (ld.special.toInt() != 0) {
            spechits.spechit[spechits.numspechit] = ld
            spechits.numspechit++
            // Let's be proactive about this.
            if (spechits.numspechit >= spechits.spechit.size) {
                ResizeSpechits()
            }
        }
        return true
    }
    //
    // MOVEMENT CLIPPING
    //
    /**
     * P_CheckPosition This is purely informative, nothing is modified (except things picked up).
     *
     * in: a mobj_t (can be valid or invalid) a position to be checked (doesn't need to be related to the mobj_t.x,y)
     *
     * during: special things are touched if MF_PICKUP early out on solid lines?
     *
     * out: newsubsec floorz ceilingz tmdropoffz the lowest point contacted (monsters won't move to a dropoff)
     * speciallines[] numspeciallines
     *
     * @param thing
     * @param x fixed_t
     * @param y fixed_t
     */
    @Compatible
    @P_Map.C(P_Map.P_CheckPosition)
    fun CheckPosition(thing: mobj_t, @SourceCode.fixed_t x: Int, @SourceCode.fixed_t y: Int): Boolean {
        val ll = levelLoader()
        val spechits = contextRequire<Spechits>(ActionTrait.KEY_SPECHITS)
        val ma = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        var xl: Int
        var xh: Int
        var yl: Int
        var yh: Int
        var bx: Int
        var by: Int
        var newsubsec: subsector_t
        ma.tmthing = thing
        ma.tmflags = thing.flags
        ma.tmx = x
        ma.tmy = y
        ma.tmbbox[BBox.BOXTOP] = y + ma.tmthing!!.radius
        ma.tmbbox[BBox.BOXBOTTOM] = y - ma.tmthing!!.radius
        ma.tmbbox[BBox.BOXRIGHT] = x + ma.tmthing!!.radius
        ma.tmbbox[BBox.BOXLEFT] = x - ma.tmthing!!.radius
        //R_PointInSubsector@ run { //TODO: check all "@ run {"
            newsubsec = levelLoader().PointInSubsector(x, y)
        //}
        ma.ceilingline = null

        // The base floor / ceiling is from the subsector
        // that contains the point.
        // Any contacted lines the step closer together
        // will adjust them.
        ma.tmdropoffz = newsubsec.sector!!.floorheight
        ma.tmfloorz = ma.tmdropoffz
        ma.tmceilingz = newsubsec.sector!!.ceilingheight
        sceneRenderer().increaseValidCount(1)
        spechits.numspechit = 0
        if (C2JUtils.eval(ma.tmflags and mobj_t.MF_NOCLIP)) {
            return true
        }

        // Check things first, possibly picking things up.
        // The bounding box is extended by MAXRADIUS
        // because mobj_ts are grouped into mapblocks
        // based on their origin point, and can overlap
        // into adjacent blocks by up to MAXRADIUS units.
        xl = ll.getSafeBlockX(ma.tmbbox[BBox.BOXLEFT] - ll.bmaporgx - Limits.MAXRADIUS)
        xh = ll.getSafeBlockX(ma.tmbbox[BBox.BOXRIGHT] - ll.bmaporgx + Limits.MAXRADIUS)
        yl = ll.getSafeBlockY(ma.tmbbox[BBox.BOXBOTTOM] - ll.bmaporgy - Limits.MAXRADIUS)
        yh = ll.getSafeBlockY(ma.tmbbox[BBox.BOXTOP] - ll.bmaporgy + Limits.MAXRADIUS)
        bx = xl
        while (bx <= xh) {
            by = yl
            while (by <= yh) {
                //P_BlockThingsIterator@ run {
                    if (!BlockThingsIterator(bx, by) { m: mobj_t -> CheckThing(m) }) {
                        return false
                    }
                //}
                by++
            }
            bx++
        }

        // check lines
        xl = ll.getSafeBlockX(ma.tmbbox[BBox.BOXLEFT] - ll.bmaporgx)
        xh = ll.getSafeBlockX(ma.tmbbox[BBox.BOXRIGHT] - ll.bmaporgx)
        yl = ll.getSafeBlockY(ma.tmbbox[BBox.BOXBOTTOM] - ll.bmaporgy)
        yh = ll.getSafeBlockY(ma.tmbbox[BBox.BOXTOP] - ll.bmaporgy)
        if (AbstractLevelLoader.FIX_BLOCKMAP_512) {
            // Maes's quick and dirty blockmap extension hack
            // E.g. for an extension of 511 blocks, max negative is -1.
            // A full 512x512 blockmap doesn't have negative indexes.
            if (xl <= ll.blockmapxneg) {
                xl = 0x1FF and xl // Broke width boundary
            }
            if (xh <= ll.blockmapxneg) {
                xh = 0x1FF and xh // Broke width boundary
            }
            if (yl <= ll.blockmapyneg) {
                yl = 0x1FF and yl // Broke height boundary
            }
            if (yh <= ll.blockmapyneg) {
                yh = 0x1FF and yh // Broke height boundary     
            }
        }
        bx = xl
        while (bx <= xh) {
            by = yl
            while (by <= yh) {
                //P_BlockLinesIterator@ run {
                    if (!BlockLinesIterator(bx, by) { ld: line_t -> CheckLine(ld) }) {
                        return false
                    }
                //}
                by++
            }
            bx++
        }
        return true
    }

    //
    // P_ThingHeightClip
    // Takes a valid thing and adjusts the thing.floorz,
    // thing.ceilingz, and possibly thing.z.
    // This is called for all nearby monsters
    // whenever a sector changes height.
    // If the thing doesn't fit,
    // the z will be set to the lowest value
    // and false will be returned.
    //
    fun ThingHeightClip(thing: mobj_t): Boolean {
        val ma = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        val onfloor: Boolean
        onfloor = thing._z == thing.floorz
        CheckPosition(thing, thing._x, thing._y)
        // what about stranding a monster partially off an edge?
        thing.floorz = ma.tmfloorz
        thing.ceilingz = ma.tmceilingz
        if (onfloor) {
            // walking monsters rise and fall with the floor
            thing._z = thing.floorz
        } else {
            // don't adjust a floating monster unless forced to
            if (thing._z + thing.height > thing.ceilingz) {
                thing._z = thing.ceilingz - thing.height
            }
        }
        return thing.ceilingz - thing.floorz >= thing.height
    }

    fun isblocking(`in`: intercept_t, li: line_t?): Boolean {
        val slideMove = contextRequire<SlideMove>(ActionTrait.KEY_SLIDEMOVE)
        // the line does block movement,
        // see if it is closer than best so far
        if (`in`.frac < slideMove.bestslidefrac) {
            slideMove.secondslidefrac = slideMove.bestslidefrac
            slideMove.secondslideline = slideMove.bestslideline
            slideMove.bestslidefrac = `in`.frac
            slideMove.bestslideline = li
        }
        return false // stop
    }

    companion object {
        var ACTION_KEY_CHAIN = KeyChain()

        var KEY_SLIDEMOVE: ContextKey<SlideMove> =
            ACTION_KEY_CHAIN.newKey(ActionTrait::class.java) { SlideMove() }
        var KEY_SPECHITS: ContextKey<Spechits> =
            ACTION_KEY_CHAIN.newKey(ActionTrait::class.java) { Spechits() }
        var KEY_MOVEMENT: ContextKey<Movement> =
            ACTION_KEY_CHAIN.newKey(ActionTrait::class.java) { Movement() }
    }
}