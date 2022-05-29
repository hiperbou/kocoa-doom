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


import data.sounds.sfxenum_t
import defines.card_t
import doom.SourceCode
import doom.SourceCode.P_Doors
import doom.englsh
import doom.player_t
import doom.thinker_t
import m.fixed_t.Companion.FRACUNIT
import p.*
import rr.line_t
import rr.sector_t
import utils.C2JUtils

interface ActionsDoors : ActionsMoveEvents, ActionsUseEvents {
    fun MovePlane(sector: sector_t, speed: Int, floorheight: Int, b: Boolean, i: Int, direction: Int): result_e
    override fun RemoveThinker(door: thinker_t)
    fun FindSectorFromLineTag(line: line_t, secnum: Int): Int
    //
    // VERTICAL DOORS
    //
    /**
     * T_VerticalDoor
     */
    fun VerticalDoor(door: vldoor_t) {
        val sector = door.sector!!
        when (door.direction) {
            0 ->                 // WAITING
                if (!C2JUtils.eval(--door.topcountdown)) {
                    when (door.type) {
                        vldoor_e.blazeRaise -> {
                            door.direction = -1 // time to go back down
                            StartSound(sector.soundorg, sfxenum_t.sfx_bdcls)
                        }
                        vldoor_e.normal -> {
                            door.direction = -1 // time to go back down
                            StartSound(sector.soundorg, sfxenum_t.sfx_dorcls)
                        }
                        vldoor_e.close30ThenOpen -> {
                            door.direction = 1
                            StartSound(sector.soundorg, sfxenum_t.sfx_doropn)
                        }
                        else -> {}
                    }
                }
            2 ->                 //  INITIAL WAIT
                if (!C2JUtils.eval(--door.topcountdown)) {
                    when (door.type) {
                        vldoor_e.raiseIn5Mins -> {
                            door.direction = 1
                            door.type = vldoor_e.normal
                            StartSound(sector.soundorg, sfxenum_t.sfx_doropn)
                        }
                        else -> {}
                    }
                }
            -1 -> {

                // DOWN
                val res = MovePlane(sector, door.speed, sector.floorheight, false, 1, door.direction)
                if (res == result_e.pastdest) {
                    when (door.type) {
                        vldoor_e.blazeRaise, vldoor_e.blazeClose -> {
                            sector.specialdata = null
                            RemoveThinker(door) // unlink and free
                            StartSound(sector.soundorg, sfxenum_t.sfx_bdcls)
                        }
                        vldoor_e.normal, vldoor_e.close -> {
                            sector.specialdata = null
                            RemoveThinker(door) // unlink and free
                        }
                        vldoor_e.close30ThenOpen -> {
                            door.direction = 0
                            door.topcountdown = 35 * 30
                        }
                        else -> {}
                    }
                } else if (res == result_e.crushed) {
                    when (door.type) {
                        vldoor_e.blazeClose, vldoor_e.close -> {}
                        else -> {
                            door.direction = 1
                            StartSound(sector.soundorg, sfxenum_t.sfx_doropn)
                        }
                    }
                }
            }
            1 -> {

                // UP
                val res = MovePlane(sector, door.speed, door.topheight, false, 1, door.direction)
                if (res == result_e.pastdest) {
                    when (door.type) {
                        vldoor_e.blazeRaise, vldoor_e.normal -> {
                            door.direction = 0 // wait at top
                            door.topcountdown = door.topwait
                        }
                        vldoor_e.close30ThenOpen, vldoor_e.blazeOpen, vldoor_e.open -> {
                            sector.specialdata = null
                            RemoveThinker(door) // unlink and free
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * EV_DoLockedDoor Move a locked door up/down
     */
    override fun DoLockedDoor(line: line_t, type: vldoor_e?, thing: mobj_t): Boolean {
        val p: player_t?
        p = thing.player
        if (p == null) {
            return false
        }
        when (line.special.toInt()) {
            99, 133 ->                 /*         if ( p==null )
             return false; */if (!p.cards[card_t.it_bluecard.ordinal] && !p.cards[card_t.it_blueskull.ordinal]) {
                p.message = englsh.PD_BLUEO
                StartSound(null, sfxenum_t.sfx_oof)
                return false
            }
            134, 135 ->                 /*        if ( p==null )
             return false; */if (!p.cards[card_t.it_redcard.ordinal] && !p.cards[card_t.it_redskull.ordinal]) {
                p.message = englsh.PD_REDO
                StartSound(null, sfxenum_t.sfx_oof)
                return false
            }
            136, 137 ->                 /*        if ( p==null )
             return false; */if (!p.cards[card_t.it_yellowcard.ordinal]
                && !p.cards[card_t.it_yellowskull.ordinal]
            ) {
                p.message = englsh.PD_YELLOWO
                StartSound(null, sfxenum_t.sfx_oof)
                return false
            }
        }
        return DoDoor(line, type)
    }

    override fun DoDoor(line: line_t, type: vldoor_e?): Boolean {
        var secnum: Int
        var rtn = false
        var sec: sector_t
        var door: vldoor_t
        secnum = -1
        while (FindSectorFromLineTag(line, secnum).also { secnum = it } >= 0) {
            sec = levelLoader().sectors[secnum]
            if (sec.specialdata != null) {
                continue
            }

            // new door thinker
            rtn = true
            door = vldoor_t()
            sec.specialdata = door
            door.thinkerFunction = ActiveStates.T_VerticalDoor
            AddThinker(door)
            door.sector = sec
            door.type = type
            door.topwait = DoorDefines.VDOORWAIT
            door.speed = DoorDefines.VDOORSPEED
            when (type) {
                vldoor_e.blazeClose -> {
                    door.topheight = sec.FindLowestCeilingSurrounding()
                    door.topheight -= 4 * FRACUNIT
                    door.direction = -1
                    door.speed = DoorDefines.VDOORSPEED * 4
                    StartSound(sec.soundorg, sfxenum_t.sfx_bdcls)
                }
                vldoor_e.close -> {
                    door.topheight = sec.FindLowestCeilingSurrounding()
                    door.topheight -= 4 * FRACUNIT
                    door.direction = -1
                    StartSound(sec.soundorg, sfxenum_t.sfx_dorcls)
                }
                vldoor_e.close30ThenOpen -> {
                    door.topheight = sec.ceilingheight
                    door.direction = -1
                    StartSound(sec.soundorg, sfxenum_t.sfx_dorcls)
                }
                vldoor_e.blazeRaise, vldoor_e.blazeOpen -> {
                    door.direction = 1
                    door.topheight = sec.FindLowestCeilingSurrounding()
                    door.topheight -= 4 * FRACUNIT
                    door.speed = DoorDefines.VDOORSPEED * 4
                    if (door.topheight != sec.ceilingheight) {
                        StartSound(sec.soundorg, sfxenum_t.sfx_bdopn)
                    }
                }
                vldoor_e.normal, vldoor_e.open -> {
                    door.direction = 1
                    door.topheight = sec.FindLowestCeilingSurrounding()
                    door.topheight -= 4 * FRACUNIT
                    if (door.topheight != sec.ceilingheight) {
                        StartSound(sec.soundorg, sfxenum_t.sfx_doropn)
                    }
                }
                else -> {}
            }
        }
        return rtn
    }

    /**
     * EV_VerticalDoor : open a door manually, no tag value
     */
    override fun VerticalDoor(line: line_t, thing: mobj_t) {
        val player: player_t?
        //int      secnum;
        val sec: sector_t
        var door: vldoor_t
        val side: Int
        side = 0 // only front sides can be used

        // Check for locks
        player = thing.player
        when (line.special.toInt()) {
            26, 32 -> {
                if (player == null) {
                    return
                }
                if (!player.cards[card_t.it_bluecard.ordinal] && !player.cards[card_t.it_blueskull.ordinal]) {
                    player.message = englsh.PD_BLUEK
                    StartSound(null, sfxenum_t.sfx_oof)
                    return
                }
            }
            27, 34 -> {
                if (player == null) {
                    return
                }
                if (!player.cards[card_t.it_yellowcard.ordinal] && !player.cards[card_t.it_yellowskull.ordinal]) {
                    player.message = englsh.PD_YELLOWK
                    StartSound(null, sfxenum_t.sfx_oof)
                    return
                }
            }
            28, 33 -> {
                if (player == null) {
                    return
                }
                if (!player.cards[card_t.it_redcard.ordinal] && !player.cards[card_t.it_redskull.ordinal]) {
                    player.message = englsh.PD_REDK
                    StartSound(null, sfxenum_t.sfx_oof)
                    return
                }
            }
        }

        // if the sector has an active thinker, use it
        sec = levelLoader().sides[line.sidenum[side xor 1].code].sector!!
        // secnum = sec.id;
        if (sec.specialdata != null) {
            door = if (sec.specialdata is plat_t) {
                /**
                 * [MAES]: demo sync for e1nm0646: emulates active plat_t interpreted
                 * as door. TODO: add our own overflow handling class.
                 */
                (sec.specialdata as plat_t).asVlDoor(levelLoader().sectors)
            } else {
                sec.specialdata as vldoor_t
            }
            when (line.special.toInt()) {
                1, 26, 27, 28, 117 -> {
                    if (door.direction == -1) {
                        door.direction = 1 // go back up
                    } else {
                        if (thing.player == null) {
                            return  // JDC: bad guys never close doors
                        }
                        door.direction = -1 // start going down immediately
                    }
                    return
                }
            }
        }
        when (line.special.toInt()) {
            117, 118 -> StartSound(sec.soundorg, sfxenum_t.sfx_bdopn)
            1, 31 -> StartSound(sec.soundorg, sfxenum_t.sfx_doropn)
            else -> StartSound(sec.soundorg, sfxenum_t.sfx_doropn)
        }

        // new door thinker
        door = vldoor_t()
        sec.specialdata = door
        door.thinkerFunction = ActiveStates.T_VerticalDoor
        AddThinker(door)
        door.sector = sec
        door.direction = 1
        door.speed = DoorDefines.VDOORSPEED
        door.topwait = DoorDefines.VDOORWAIT
        when (line.special.toInt()) {
            1, 26, 27, 28 -> door.type = vldoor_e.normal
            31, 32, 33, 34 -> {
                door.type = vldoor_e.open
                line.special = 0
            }
            117 -> {
                door.type = vldoor_e.blazeRaise
                door.speed = DoorDefines.VDOORSPEED * 4
            }
            118 -> {
                door.type = vldoor_e.blazeOpen
                line.special = 0
                door.speed = DoorDefines.VDOORSPEED * 4
            }
        }

        // find the top and bottom of the movement range
        door.topheight = sec.FindLowestCeilingSurrounding()
        door.topheight -= 4 * FRACUNIT
    }

    //
    // Spawn a door that closes after 30 seconds
    //
    @SourceCode.Exact
    @P_Doors.C(P_Doors.P_SpawnDoorCloseIn30)
    fun SpawnDoorCloseIn30(sector: sector_t) {
        var door: vldoor_t
        //Z_Malloc@ run {
            door = vldoor_t()
        //}
        //P_AddThinker@ run {
            AddThinker(door)
        //}
        sector.specialdata = door
        sector.special = 0
        door.thinkerFunction = ActiveStates.T_VerticalDoor
        door.sector = sector
        door.direction = 0
        door.type = vldoor_e.normal
        door.speed = DoorDefines.VDOORSPEED
        door.topcountdown = 30 * 35
    }

    /**
     * Spawn a door that opens after 5 minutes
     */
    @SourceCode.Exact
    @P_Doors.C(P_Doors.P_SpawnDoorRaiseIn5Mins)
    fun SpawnDoorRaiseIn5Mins(sector: sector_t, secnum: Int) {
        var door: vldoor_t
        //Z_Malloc@ run {
            door = vldoor_t()
        //}
        //P_AddThinker@ run {
            AddThinker(door)
        //}
        sector.specialdata = door
        sector.special = 0
        door.thinkerFunction = ActiveStates.T_VerticalDoor
        door.sector = sector
        door.direction = 2
        door.type = vldoor_e.raiseIn5Mins
        door.speed = DoorDefines.VDOORSPEED
        P_FindLowestCeilingSurrounding@ run {
            door.topheight = sector.FindLowestCeilingSurrounding()
        }
        door.topheight -= 4 * FRACUNIT
        door.topwait = DoorDefines.VDOORWAIT
        door.topcountdown = 5 * 60 * 35
    }
}