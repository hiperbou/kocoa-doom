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
package p.Actions.ActiveStates.MonsterStatesimport


import data.Limits
import data.mobjtype_t
import doom.thinker_t
import p.Actions.ActionTrait
import p.ActiveStates
import p.floor_e
import p.mobj_t
import p.vldoor_e
import rr.line_t

interface Bosses : ActionTrait {
    fun A_Fall(mo: mobj_t)

    /**
     * A_BossDeath
     * Possibly trigger special effects
     * if on first boss level
     *
     * TODO: find out how Plutonia/TNT does cope with this.
     * Special clauses?
     *
     */
    fun A_BossDeath(mo: mobj_t) {
        val D = DOOM()
        var th: thinker_t
        var mo2: mobj_t
        val junk = line_t()
        var i: Int
        if (D.isCommercial()) {
            if (D.gamemap != 7) {
                return
            }
            if (mo.type != mobjtype_t.MT_FATSO && mo.type != mobjtype_t.MT_BABY) {
                return
            }
        } else {
            when (D.gameepisode) {
                1 -> {
                    if (D.gamemap != 8) {
                        return
                    }
                    if (mo.type != mobjtype_t.MT_BRUISER) {
                        return
                    }
                }
                2 -> {
                    if (D.gamemap != 8) {
                        return
                    }
                    if (mo.type != mobjtype_t.MT_CYBORG) {
                        return
                    }
                }
                3 -> {
                    if (D.gamemap != 8) {
                        return
                    }
                    if (mo.type != mobjtype_t.MT_SPIDER) {
                        return
                    }
                }
                4 -> when (D.gamemap) {
                    6 -> if (mo.type != mobjtype_t.MT_CYBORG) {
                        return
                    }
                    8 -> if (mo.type != mobjtype_t.MT_SPIDER) {
                        return
                    }
                    else -> return
                }
                else -> if (D.gamemap != 8) {
                    return
                }
            }
        }

        // make sure there is a player alive for victory
        i = 0
        while (i < Limits.MAXPLAYERS) {
            if (D.playeringame[i] && D.players[i].health[0] > 0) {
                break
            }
            i++
        }
        if (i == Limits.MAXPLAYERS) {
            return  // no one left alive, so do not end game
        }
        // scan the remaining thinkers to see
        // if all bosses are dead
        th = getThinkerCap().next!!
        while (th !== getThinkerCap()) {
            if (th.thinkerFunction != ActiveStates.P_MobjThinker) {
                th = th.next!!
                continue
            }
            mo2 = th as mobj_t
            if (mo2 !== mo && mo2.type == mo.type && mo2.health > 0) {
                // other boss not dead
                return
            }
            th = th.next!!
        }

        // victory!
        if (D.isCommercial()) {
            if (D.gamemap == 7) {
                if (mo.type == mobjtype_t.MT_FATSO) {
                    junk.tag = 666
                    thinkers.DoFloor(junk, floor_e.lowerFloorToLowest)
                    return
                }
                if (mo.type == mobjtype_t.MT_BABY) {
                    junk.tag = 667
                    thinkers.DoFloor(junk, floor_e.raiseToTexture)
                    return
                }
            }
        } else {
            when (D.gameepisode) {
                1 -> {
                    junk.tag = 666
                    thinkers.DoFloor(junk, floor_e.lowerFloorToLowest)
                    return
                }
                4 -> when (D.gamemap) {
                    6 -> {
                        junk.tag = 666
                        thinkers.DoDoor(junk, vldoor_e.blazeOpen)
                        return
                    }
                    8 -> {
                        junk.tag = 666
                        thinkers.DoFloor(junk, floor_e.lowerFloorToLowest)
                        return
                    }
                }
            }
        }
        D.ExitLevel()
    }

    fun A_KeenDie(mo: mobj_t) {
        var th: thinker_t
        var mo2: mobj_t
        val junk = line_t() // MAES: fixed null 21/5/2011
        A_Fall(mo)

        // scan the remaining thinkers
        // to see if all Keens are dead
        th = getThinkerCap().next!!
        while (th !== getThinkerCap()) {
            if (th.thinkerFunction != ActiveStates.P_MobjThinker) {
                th = th.next!!
                continue
            }
            mo2 = th as mobj_t
            if (mo2 !== mo && mo2.type == mo.type && mo2.health > 0) {
                // other Keen not dead
                return
            }
            th = th.next!!
        }
        junk.tag = 666
        thinkers.DoDoor(junk, vldoor_e.open)
    }
}