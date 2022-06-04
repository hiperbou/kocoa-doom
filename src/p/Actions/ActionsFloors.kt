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


import com.hiperbou.lang.times
import data.Limits
import data.sounds.sfxenum_t
import m.fixed_t
import p.*
import rr.line_t
import rr.sector_t
import rr.side_t
import utils.C2JUtils

interface ActionsFloors : ActionsPlats {
    fun MovePlane(sector: sector_t, speed: Int, floordestheight: Int, crush: Boolean, i: Int, direction: Int): result_e
    fun twoSided(secnum: Int, i: Int): Boolean
    fun getSide(secnum: Int, i: Int, s: Int): side_t
    fun getSector(secnum: Int, i: Int, i0: Int): sector_t

    /**
     * MOVE A FLOOR TO IT'S DESTINATION (UP OR DOWN)
     */
    fun MoveFloor(floor: floormove_t) {
        val res = MovePlane(floor.sector!!, floor.speed, floor.floordestheight, floor.crush, 0, floor.direction)
        if (!C2JUtils.eval(LevelTime() and 7)) {
            StartSound(floor.sector!!.soundorg, sfxenum_t.sfx_stnmov)
        }
        if (res == result_e.pastdest) {
            floor.sector!!.specialdata = null
            if (floor.direction == 1) {
                when (floor.type) {
                    floor_e.donutRaise -> {
                        floor.sector!!.special = floor.newspecial.toShort()
                        floor.sector!!.floorpic = floor.texture
                    }
                    else -> {}
                }
            } else if (floor.direction == -1) {
                when (floor.type) {
                    floor_e.lowerAndChange -> {
                        floor.sector!!.special = floor.newspecial.toShort()
                        floor.sector!!.floorpic = floor.texture
                    }
                    else -> {}
                }
            }
            RemoveThinker(floor)
            StartSound(floor.sector!!.soundorg, sfxenum_t.sfx_pstop)
        }
    }

    //
    // HANDLE FLOOR TYPES
    //
    override fun DoFloor(line: line_t, floortype: floor_e): Boolean {
        var secnum = -1
        var rtn = false
        var sec: sector_t
        var floor: floormove_t
        while (FindSectorFromLineTag(line, secnum).also { secnum = it } >= 0) {
            sec = levelLoader().sectors[secnum]

            // ALREADY MOVING?  IF SO, KEEP GOING...
            if (sec.specialdata != null) {
                continue
            }

            // new floor thinker
            rtn = true
            floor = floormove_t()
            sec.specialdata = floor
            floor.thinkerFunction = ActiveStates.T_MoveFloor
            AddThinker(floor)
            floor.type = floortype
            floor.crush = false
            when (floortype) {
                floor_e.lowerFloor -> {
                    floor.direction = -1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = sec.FindHighestFloorSurrounding()
                }
                floor_e.lowerFloorToLowest -> {
                    floor.direction = -1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = sec.FindLowestFloorSurrounding()
                }
                floor_e.turboLower -> {
                    floor.direction = -1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED * 4
                    floor.floordestheight = sec.FindHighestFloorSurrounding()
                    if (floor.floordestheight != sec.floorheight) {
                        floor.floordestheight += 8 * fixed_t.FRACUNIT
                    }
                }
                floor_e.raiseFloorCrush -> {
                    floor.crush = true
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = sec.FindLowestCeilingSurrounding()
                    if (floor.floordestheight > sec.ceilingheight) {
                        floor.floordestheight = sec.ceilingheight
                    }
                    floor.floordestheight -= (8 * fixed_t.FRACUNIT
                            * C2JUtils.eval(floortype == floor_e.raiseFloorCrush))
                }
                floor_e.raiseFloor -> {
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = sec.FindLowestCeilingSurrounding()
                    if (floor.floordestheight > sec.ceilingheight) {
                        floor.floordestheight = sec.ceilingheight
                    }
                    floor.floordestheight -= (8 * fixed_t.FRACUNIT
                            * C2JUtils.eval(floortype == floor_e.raiseFloorCrush))
                }
                floor_e.raiseFloorTurbo -> {
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED * 4
                    floor.floordestheight = sec.FindNextHighestFloor(sec.floorheight)
                }
                floor_e.raiseFloorToNearest -> {
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = sec.FindNextHighestFloor(sec.floorheight)
                }
                floor_e.raiseFloor24 -> {
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = floor.sector!!.floorheight + 24 * fixed_t.FRACUNIT
                }
                floor_e.raiseFloor512 -> {
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = floor.sector!!.floorheight + 512 * fixed_t.FRACUNIT
                }
                floor_e.raiseFloor24AndChange -> {
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = floor.sector!!.floorheight + 24 * fixed_t.FRACUNIT
                    sec.floorpic = line.frontsector!!.floorpic
                    sec.special = line.frontsector!!.special
                }
                floor_e.raiseToTexture -> {
                    var minsize = Limits.MAXINT
                    var side: side_t
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    sec.linecount.times { i ->
                        if (twoSided(secnum, i)) {

                            2.times { s ->
                                side = getSide(secnum, i, s)
                                if (side.bottomtexture >= 0) {
                                    if (DOOM().textureManager.getTextureheight(side.bottomtexture.toInt()) < minsize) {
                                        minsize = DOOM().textureManager.getTextureheight(side.bottomtexture.toInt())
                                    }
                                }
                            }
                        }
                    }
                    floor.floordestheight = floor.sector!!.floorheight + minsize
                }
                floor_e.lowerAndChange -> {
                    floor.direction = -1
                    floor.sector = sec
                    floor.speed = ActionsFloors.FLOORSPEED
                    floor.floordestheight = sec.FindLowestFloorSurrounding()
                    floor.texture = sec.floorpic
                    var i = 0
                    while (i < sec.linecount) {
                        if (twoSided(secnum, i)) {
                            if (getSide(secnum, i, 0).sector!!.id == secnum) {
                                sec = getSector(secnum, i, 1)
                                if (sec.floorheight == floor.floordestheight) {
                                    floor.texture = sec.floorpic
                                    floor.newspecial = sec.special.toInt()
                                    break
                                }
                            } else {
                                sec = getSector(secnum, i, 0)
                                if (sec.floorheight == floor.floordestheight) {
                                    floor.texture = sec.floorpic
                                    floor.newspecial = sec.special.toInt()
                                    break
                                }
                            }
                        }
                        i++
                    }
                }
                else -> {}
            }
        }
        return rtn
    }

    /**
     * BUILD A STAIRCASE!
     */
    override fun BuildStairs(line: line_t?, type: stair_e?): Boolean {
        var secnum: Int
        var height: Int
        var i: Int
        var newsecnum: Int
        var texture: Int
        var ok: Boolean
        var rtn: Boolean
        var sec: sector_t
        var tsec: sector_t
        var floor: floormove_t
        var stairsize = 0
        var speed = 0 // shut up compiler
        secnum = -1
        rtn = false
        while (FindSectorFromLineTag(line!!, secnum).also { secnum = it } >= 0) {
            sec = levelLoader().sectors[secnum]

            // ALREADY MOVING?  IF SO, KEEP GOING...
            if (sec.specialdata != null) {
                continue
            }

            // new floor thinker
            rtn = true
            floor = floormove_t()
            sec.specialdata = floor
            floor.thinkerFunction = ActiveStates.T_MoveFloor
            AddThinker(floor)
            floor.direction = 1
            floor.sector = sec
            when (type) {
                stair_e.build8 -> {
                    speed = ActionsFloors.FLOORSPEED / 4
                    stairsize = 8 * fixed_t.FRACUNIT
                }
                stair_e.turbo16 -> {
                    speed = ActionsFloors.FLOORSPEED * 4
                    stairsize = 16 * fixed_t.FRACUNIT
                }
            }
            floor.speed = speed
            height = sec.floorheight + stairsize
            floor.floordestheight = height
            texture = sec.floorpic.toInt()

            // Find next sector to raise
            // 1.   Find 2-sided line with same sector side[0]
            // 2.   Other side is the next sector to raise
            do {
                ok = false
                i = 0
                while (i < sec.linecount) {
                    if (!C2JUtils.eval(sec.lines!![i]!!.flags.toInt() and line_t.ML_TWOSIDED)) {
                        i++
                        continue
                    }
                    tsec = sec.lines!![i]!!.frontsector!!
                    newsecnum = tsec.id
                    if (secnum != newsecnum) {
                        i++
                        continue
                    }
                    tsec = sec.lines!![i]!!.backsector!!
                    newsecnum = tsec.id
                    if (tsec.floorpic.toInt() != texture) {
                        i++
                        continue
                    }
                    height += stairsize
                    if (tsec.specialdata != null) {
                        i++
                        continue
                    }
                    sec = tsec
                    secnum = newsecnum
                    floor = floormove_t()
                    sec.specialdata = floor
                    floor.thinkerFunction = ActiveStates.T_MoveFloor
                    AddThinker(floor)
                    floor.direction = 1
                    floor.sector = sec
                    floor.speed = speed
                    floor.floordestheight = height
                    ok = true
                    break
                    i++
                }
            } while (ok)
        }
        return rtn
    }

    /**
     * Move a plat up and down
     */
    fun PlatRaise(plat: plat_t) {
        val res: result_e
        when (plat.status) {
            plat_e.up -> {
                res = MovePlane(plat.sector!!, plat.speed, plat.high, plat.crush, 0, 1)
                if (plat.type == plattype_e.raiseAndChange
                    || plat.type == plattype_e.raiseToNearestAndChange
                ) {
                    if (!C2JUtils.eval(LevelTime() and 7)) {
                        StartSound(plat.sector!!.soundorg, sfxenum_t.sfx_stnmov)
                    }
                }
                if (res == result_e.crushed && !plat.crush) {
                    plat.count = plat.wait
                    plat.status = plat_e.down
                    StartSound(plat.sector!!.soundorg, sfxenum_t.sfx_pstart)
                } else {
                    if (res == result_e.pastdest) {
                        plat.count = plat.wait
                        plat.status = plat_e.waiting
                        StartSound(plat.sector!!.soundorg, sfxenum_t.sfx_pstop)
                        when (plat.type) {
                            plattype_e.blazeDWUS, plattype_e.downWaitUpStay -> RemoveActivePlat(plat)
                            plattype_e.raiseAndChange, plattype_e.raiseToNearestAndChange -> RemoveActivePlat(plat)
                            else -> {}
                        }
                    }
                }
            }
            plat_e.down -> {
                res = MovePlane(plat.sector!!, plat.speed, plat.low, false, 0, -1)
                if (res == result_e.pastdest) {
                    plat.count = plat.wait
                    plat.status = plat_e.waiting
                    StartSound(plat.sector!!.soundorg, sfxenum_t.sfx_pstop)
                }
            }
            plat_e.waiting -> if (--plat.count == 0) {
                if (plat.sector!!.floorheight == plat.low) {
                    plat.status = plat_e.up
                } else {
                    plat.status = plat_e.down
                }
                StartSound(plat.sector!!.soundorg, sfxenum_t.sfx_pstart)
            }
            plat_e.in_stasis -> {}
        }
    }

    companion object {
        //
        // FLOORS
        //
        val FLOORSPEED: Int = fixed_t.MAPFRACUNIT
    }
}