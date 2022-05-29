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
import data.mapthing_t
import data.mobjtype_t
import defines.statenum_t
import doom.SourceCode
import doom.SourceCode.P_Map
import m.BBox
import mochadoom.Loggers
import p.*
import p.Actions.ActionTrait.Companion.ACTION_KEY_CHAIN
import rr.line_t
import rr.sector_t
import rr.side_t
import utils.C2JUtils
import utils.TraitFactory.ContextKey

interface ActionsSectors : ActionsLights, ActionsFloors, ActionsDoors, ActionsCeilings, ActionsSlideDoors {
    fun RemoveMobj(thing: mobj_t)
    fun DamageMobj(thing: mobj_t, tmthing: mobj_t?, tmthing0: mobj_t?, damage: Int)
    fun SpawnMobj(
        @SourceCode.fixed_t x: Int,
        @SourceCode.fixed_t y: Int,
        @SourceCode.fixed_t z: Int,
        type: mobjtype_t
    ): mobj_t

    class Crushes {
        var crushchange = false
        var nofit = false
    }

    class RespawnQueue {
        //
        // P_RemoveMobj
        //
        var itemrespawnque = arrayOfNulls<mapthing_t>(Defines.ITEMQUESIZE)
        var itemrespawntime = IntArray(Defines.ITEMQUESIZE)
        var iquehead = 0
        var iquetail = 0
    }

    class Spawn {
        /**
         * who got hit (or NULL)
         */
        var linetarget: mobj_t? = null

        @SourceCode.fixed_t
        var attackrange = 0
        var shootthing: mobj_t? = null

        // Height if not aiming up or down
        // ???: use slope for monsters?
        @SourceCode.fixed_t
        var shootz = 0
        var la_damage = 0

        @SourceCode.fixed_t
        var aimslope = 0
        var trace = divline_t()
        var topslope = 0
        var bottomslope // slopes to top and bottom of target
                = 0

        //
        // P_BulletSlope
        // Sets a slope so a near miss is at aproximately
        // the height of the intended target
        //
        var bulletslope = 0
        val isMeleeRange: Boolean
            get() = attackrange == Defines.MELEERANGE

        companion object {
            val LOGGER = Loggers.getLogger(ActionsSectors::class.java.name)
        }
    }

    //
    // P_ChangeSector
    //
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
    fun ChangeSector(sector: sector_t, crunch: Boolean): Boolean {
        val cr = contextRequire<Crushes>(ActionsSectors.KEY_CRUSHES)
        var x: Int
        var y: Int
        cr.nofit = false
        cr.crushchange = crunch

        // re-check heights for all things near the moving sector
        x = sector.blockbox[BBox.BOXLEFT]
        while (x <= sector.blockbox[BBox.BOXRIGHT]) {
            y = sector.blockbox[BBox.BOXBOTTOM]
            while (y <= sector.blockbox[BBox.BOXTOP]) {
                BlockThingsIterator(x, y) { thing: mobj_t -> this.ChangeSector(thing) }
                y++
            }
            x++
        }
        return cr.nofit
    }

    /**
     * PIT_ChangeSector
     */
    @P_Map.C(P_Map.PIT_ChangeSector)
    fun ChangeSector(thing: mobj_t): Boolean {
        val cr = contextRequire<Crushes>(ActionsSectors.KEY_CRUSHES)
        val mo: mobj_t
        if (ThingHeightClip(thing)) {
            // keep checking
            return true
        }

        // crunch bodies to giblets
        if (thing.health <= 0) {
            thing.SetMobjState(statenum_t.S_GIBS)
            thing.flags = thing.flags and mobj_t.MF_SOLID.inv()
            thing.height = 0
            thing.radius = 0

            // keep checking
            return true
        }

        // crunch dropped items
        if (C2JUtils.eval(thing.flags and mobj_t.MF_DROPPED)) {
            RemoveMobj(thing)

            // keep checking
            return true
        }
        if (!C2JUtils.eval(thing.flags and mobj_t.MF_SHOOTABLE)) {
            // assume it is bloody gibs or something
            return true
        }
        cr.nofit = true
        if (cr.crushchange && !C2JUtils.eval(LevelTime() and 3)) {
            DamageMobj(thing, null, null, 10)

            // spray blood in a random direction
            mo = SpawnMobj(thing._x, thing._y, thing._z + thing.height / 2, mobjtype_t.MT_BLOOD)
            mo.momx = P_Random() - P_Random() shl 12
            mo.momy = P_Random() - P_Random() shl 12
        }

        // keep checking (crush other things)   
        return true
    }

    /**
     * Move a plane (floor or ceiling) and check for crushing
     *
     * @param sector
     * @param speed fixed
     * @param dest fixed
     * @param crush
     * @param floorOrCeiling
     * @param direction
     */
    override fun MovePlane(
        sector: sector_t,
        speed: Int,
        dest: Int,
        crush: Boolean,
        floorOrCeiling: Int,
        direction: Int
    ): result_e {
        val flag: Boolean
        @SourceCode.fixed_t val lastpos: Int
        when (floorOrCeiling) {
            0 -> when (direction) {
                -1 ->                         // DOWN
                    if (sector.floorheight - speed < dest) {
                        lastpos = sector.floorheight
                        sector.floorheight = dest
                        flag = ChangeSector(sector, crush)
                        if (flag == true) {
                            sector.floorheight = lastpos
                            ChangeSector(sector, crush)
                            //return crushed;
                        }
                        return result_e.pastdest
                    } else {
                        lastpos = sector.floorheight
                        sector.floorheight -= speed
                        flag = ChangeSector(sector, crush)
                        if (flag == true) {
                            sector.floorheight = lastpos
                            ChangeSector(sector, crush)
                            return result_e.crushed
                        }
                    }
                1 ->                         // UP
                    if (sector.floorheight + speed > dest) {
                        lastpos = sector.floorheight
                        sector.floorheight = dest
                        flag = ChangeSector(sector, crush)
                        if (flag == true) {
                            sector.floorheight = lastpos
                            ChangeSector(sector, crush)
                            //return crushed;
                        }
                        return result_e.pastdest
                    } else {
                        // COULD GET CRUSHED
                        lastpos = sector.floorheight
                        sector.floorheight += speed
                        flag = ChangeSector(sector, crush)
                        if (flag == true) {
                            if (crush == true) {
                                return result_e.crushed
                            }
                            sector.floorheight = lastpos
                            ChangeSector(sector, crush)
                            return result_e.crushed
                        }
                    }
            }
            1 -> when (direction) {
                -1 ->                         // DOWN
                    if (sector.ceilingheight - speed < dest) {
                        lastpos = sector.ceilingheight
                        sector.ceilingheight = dest
                        flag = ChangeSector(sector, crush)
                        if (flag == true) {
                            sector.ceilingheight = lastpos
                            ChangeSector(sector, crush)
                            //return crushed;
                        }
                        return result_e.pastdest
                    } else {
                        // COULD GET CRUSHED
                        lastpos = sector.ceilingheight
                        sector.ceilingheight -= speed
                        flag = ChangeSector(sector, crush)
                        if (flag == true) {
                            if (crush == true) {
                                return result_e.crushed
                            }
                            sector.ceilingheight = lastpos
                            ChangeSector(sector, crush)
                            return result_e.crushed
                        }
                    }
                1 ->                         // UP
                    if (sector.ceilingheight + speed > dest) {
                        lastpos = sector.ceilingheight
                        sector.ceilingheight = dest
                        flag = ChangeSector(sector, crush)
                        if (flag == true) {
                            sector.ceilingheight = lastpos
                            ChangeSector(sector, crush)
                            //return crushed;
                        }
                        return result_e.pastdest
                    } else {
                        lastpos = sector.ceilingheight
                        sector.ceilingheight += speed
                        flag = ChangeSector(sector, crush)
                        // UNUSED
                        /*
                            if (flag == true)
                            {
                                sector.ceilingheight = lastpos;
                                P_ChangeSector(sector,crush);
                                return crushed;
                            }
                             */
                    }
            }
        }
        return result_e.ok
    }

    /**
     * Special Stuff that can not be categorized
     *
     * (I'm sure it has something to do with John Romero's obsession with fucking stuff and making them his bitches).
     *
     * @param line
     */
    override fun DoDonut(line: line_t): Boolean {
        var s1: sector_t
        var s2: sector_t
        var s3: sector_t
        var secnum: Int
        var rtn: Boolean
        var i: Int
        var floor: floormove_t
        secnum = -1
        rtn = false
        while (FindSectorFromLineTag(line, secnum).also { secnum = it } >= 0) {
            s1 = levelLoader().sectors[secnum]

            // ALREADY MOVING?  IF SO, KEEP GOING...
            if (s1.specialdata != null) {
                continue
            }
            rtn = true
            s2 = s1.lines!![0]!!.getNextSector(s1)!!
            i = 0
            while (i < s2.linecount) {
                if (!C2JUtils.eval(s2.lines!![i]!!.flags.toInt() and line_t.ML_TWOSIDED) || s2.lines!![i]!!.backsector === s1) {
                    i++
                    continue
                }
                s3 = s2.lines!![i]!!.backsector!!

                //  Spawn rising slime
                floor = floormove_t()
                s2.specialdata = floor
                floor.thinkerFunction = ActiveStates.T_MoveFloor
                AddThinker(floor)
                floor.type = floor_e.donutRaise
                floor.crush = false
                floor.direction = 1
                floor.sector = s2
                floor.speed = ActionsFloors.FLOORSPEED / 2
                floor.texture = s3.floorpic
                floor.newspecial = 0
                floor.floordestheight = s3.floorheight

                //  Spawn lowering donut-hole
                floor = floormove_t()
                s1.specialdata = floor
                floor.thinkerFunction = ActiveStates.T_MoveFloor
                AddThinker(floor)
                floor.type = floor_e.lowerFloor
                floor.crush = false
                floor.direction = -1
                floor.sector = s1
                floor.speed = ActionsFloors.FLOORSPEED / 2
                floor.floordestheight = s3.floorheight
                break
                i++
            }
        }
        return rtn
    }

    /**
     * RETURN NEXT SECTOR # THAT LINE TAG REFERS TO
     */
    override fun FindSectorFromLineTag(line: line_t, start: Int): Int {
        val ll = levelLoader()
        for (i in start + 1 until ll.numsectors) {
            if (ll.sectors[i].tag == line.tag) {
                return i
            }
        }
        return -1
    }

    //
    // UTILITIES
    //
    //
    // getSide()
    // Will return a side_t*
    // given the number of the current sector,
    // the line number, and the side (0/1) that you want.
    //
    override fun getSide(currentSector: Int, line: Int, side: Int): side_t {
        val ll = levelLoader()
        return ll.sides[ll.sectors[currentSector].lines!![line]!!.sidenum[side].code]
    }

    /**
     * getSector()
     * Will return a sector_t
     * given the number of the current sector,
     * the line number and the side (0/1) that you want.
     */
    override fun getSector(currentSector: Int, line: Int, side: Int): sector_t {
        val ll = levelLoader()
        return ll.sides[ll.sectors[currentSector].lines!![line]!!.sidenum[side].code].sector!!
    }

    /**
     * twoSided()
     * Given the sector number and the line number,
     * it will tell you whether the line is two-sided or not.
     */
    override fun twoSided(sector: Int, line: Int): Boolean {
        return C2JUtils.eval(levelLoader().sectors[sector].lines!![line]!!.flags.toInt() and line_t.ML_TWOSIDED)
    }

    fun ClearRespawnQueue() {
        // clear special respawning que
        val rq = contextRequire(KEY_RESP_QUEUE)
        rq.iquehead = 0
        rq.iquetail = 0
    }

    companion object {
        var KEY_RESP_QUEUE: ContextKey<RespawnQueue> =
            ACTION_KEY_CHAIN.newKey(ActionsSectors::class.java) { RespawnQueue() }
        var KEY_SPAWN: ContextKey<Spawn> = ACTION_KEY_CHAIN.newKey(ActionsSectors::class.java) { Spawn() }
        var KEY_CRUSHES: ContextKey<Crushes> = ACTION_KEY_CHAIN.newKey(ActionsSectors::class.java) { Crushes() }
    }
}