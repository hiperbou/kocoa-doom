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
import doom.SourceCode.P_Ceiling
import doom.thinker_t
import m.fixed_t.Companion.FRACUNIT
import p.ActiveStates
import p.ceiling_e
import p.ceiling_t
import p.result_e
import rr.line_t
import rr.sector_t
import utils.C2JUtils
import utils.TraitFactory.ContextKey
import java.util.function.Supplier

interface ActionsCeilings : ActionsMoveEvents, ActionsUseEvents {
    override fun RemoveThinker(activeCeiling: thinker_t)
    fun MovePlane(sector: sector_t, speed: Int, bottomheight: Int, crush: Boolean, i: Int, direction: Int): result_e
    fun FindSectorFromLineTag(line: line_t, secnum: Int): Int
    class Ceilings {
        var activeCeilings = arrayOfNulls<ceiling_t>(Limits.MAXCEILINGS)
            //get() = contextRequire<Ceilings>(ActionsCeilings.KEY_CEILINGS).field //TODO: wrong code generated
    }

    /**
     * This needs to be called before loading, otherwise crushers won't be able to be restarted.
     */
    fun ClearCeilingsBeforeLoading() {
        contextRequire<Ceilings>(ActionsCeilings.KEY_CEILINGS).activeCeilings =
            arrayOfNulls(Limits.MAXCEILINGS)
    }

    /**
     * T_MoveCeiling
     */
    fun MoveCeiling(ceiling: ceiling_t) {
        val res: result_e
        val sector = ceiling.sector!!
        when (ceiling.direction) {
            0 -> {}
            1 -> {
                // UP
                res = MovePlane(sector, ceiling.speed, ceiling.topheight, false, 1, ceiling.direction)
                if (!C2JUtils.eval(LevelTime() and 7)) {
                    when (ceiling.type) {
                        ceiling_e.silentCrushAndRaise -> {}
                        else -> StartSound(sector.soundorg, sfxenum_t.sfx_stnmov)
                    }
                }
                if (res == result_e.pastdest) {
                    when (ceiling.type) {
                        ceiling_e.raiseToHighest -> RemoveActiveCeiling(ceiling)
                        ceiling_e.silentCrushAndRaise -> {
                            StartSound(sector.soundorg, sfxenum_t.sfx_pstop)
                            ceiling.direction = -1
                        }
                        ceiling_e.fastCrushAndRaise, ceiling_e.crushAndRaise -> ceiling.direction = -1
                        else -> {}
                    }
                }
            }
            -1 -> {
                // DOWN
                res =
                    MovePlane(sector, ceiling.speed, ceiling.bottomheight, ceiling.crush, 1, ceiling.direction)
                if (!C2JUtils.eval(LevelTime() and 7)) {
                    when (ceiling.type) {
                        ceiling_e.silentCrushAndRaise -> {}
                        else -> StartSound(sector.soundorg, sfxenum_t.sfx_stnmov)
                    }
                }
                if (res == result_e.pastdest) {
                    when (ceiling.type) {
                        ceiling_e.silentCrushAndRaise -> {
                            StartSound(sector.soundorg, sfxenum_t.sfx_pstop)
                            ceiling.speed = Limits.CEILSPEED
                            ceiling.direction = 1
                        }
                        ceiling_e.crushAndRaise -> {
                            ceiling.speed = Limits.CEILSPEED
                            ceiling.direction = 1
                        }
                        ceiling_e.fastCrushAndRaise -> ceiling.direction = 1
                        ceiling_e.lowerAndCrush, ceiling_e.lowerToFloor -> RemoveActiveCeiling(ceiling)
                        else -> {}
                    }
                } else { // ( res != result_e.pastdest )
                    if (res == result_e.crushed) {
                        when (ceiling.type) {
                            ceiling_e.silentCrushAndRaise, ceiling_e.crushAndRaise, ceiling_e.lowerAndCrush -> ceiling.speed =
                                Limits.CEILSPEED / 8
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    //
    // EV.DoCeiling
    // Move a ceiling up/down and all around!
    //
    @P_Ceiling.C(P_Ceiling.EV_DoCeiling)
    override fun DoCeiling(line: line_t, type: ceiling_e): Boolean {
        var secnum = -1
        var rtn = false
        var sec: sector_t
        var ceiling: ceiling_t
        when (type) {
            ceiling_e.fastCrushAndRaise, ceiling_e.silentCrushAndRaise, ceiling_e.crushAndRaise -> ActivateInStasisCeiling(
                line
            )
            else -> {}
        }
        while (FindSectorFromLineTag(line, secnum).also { secnum = it } >= 0) {
            sec = levelLoader().sectors[secnum]
            if (sec.specialdata != null) {
                continue
            }

            // new door thinker
            rtn = true
            ceiling = ceiling_t()
            sec.specialdata = ceiling
            ceiling.thinkerFunction = ActiveStates.T_MoveCeiling
            AddThinker(ceiling)
            ceiling.sector = sec
            ceiling.crush = false
            when (type) {
                ceiling_e.fastCrushAndRaise -> {
                    ceiling.crush = true
                    ceiling.topheight = sec.ceilingheight
                    ceiling.bottomheight = sec.floorheight + 8 * FRACUNIT
                    ceiling.direction = -1
                    ceiling.speed = Limits.CEILSPEED * 2
                }
                ceiling_e.silentCrushAndRaise, ceiling_e.crushAndRaise -> {
                    ceiling.crush = true
                    ceiling.topheight = sec.ceilingheight
                    ceiling.bottomheight = sec.floorheight
                    if (type != ceiling_e.lowerToFloor) {
                        ceiling.bottomheight += 8 * FRACUNIT
                    }
                    ceiling.direction = -1
                    ceiling.speed = Limits.CEILSPEED
                }
                ceiling_e.lowerAndCrush, ceiling_e.lowerToFloor -> {
                    ceiling.bottomheight = sec.floorheight
                    if (type != ceiling_e.lowerToFloor) {
                        ceiling.bottomheight += 8 * FRACUNIT
                    }
                    ceiling.direction = -1
                    ceiling.speed = Limits.CEILSPEED
                }
                ceiling_e.raiseToHighest -> {
                    ceiling.topheight = sec.FindHighestCeilingSurrounding()
                    ceiling.direction = 1
                    ceiling.speed = Limits.CEILSPEED
                }
            }
            ceiling.tag = sec.tag.toInt()
            ceiling.type = type
            AddActiveCeiling(ceiling)
        }
        return rtn
    }

    //
    // Add an active ceiling
    //
    fun AddActiveCeiling(c: ceiling_t?) {
        val activeCeilings: Array<ceiling_t?> = this.getActiveCeilings()
        for (i in activeCeilings.indices) {
            if (activeCeilings[i] == null) {
                activeCeilings[i] = c
                return
            }
        }
        // Needs rezising
        setActiveceilings(C2JUtils.resize(c, activeCeilings, 2 * activeCeilings.size))
    }

    //
    // Remove a ceiling's thinker
    //
    fun RemoveActiveCeiling(c: ceiling_t) {
        val activeCeilings: Array<ceiling_t?> = this.getActiveCeilings()
        for (i in activeCeilings.indices) {
            if (activeCeilings[i] === c) {
                activeCeilings[i]!!.sector!!.specialdata = null
                RemoveThinker(activeCeilings[i]!!)
                activeCeilings[i] = null
                break
            }
        }
    }

    //
    // Restart a ceiling that's in-stasis
    //
    fun ActivateInStasisCeiling(line: line_t) {
        val activeCeilings: Array<ceiling_t?> = this.getActiveCeilings()
        for (i in activeCeilings.indices) {
            if (activeCeilings[i] != null && activeCeilings[i]!!.tag == line.tag.toInt() && activeCeilings[i]!!.direction == 0) {
                activeCeilings[i]!!.direction = activeCeilings[i]!!.olddirection
                activeCeilings[i]!!.thinkerFunction = ActiveStates.T_MoveCeiling
            }
        }
    }

    //
    // EV_CeilingCrushStop
    // Stop a ceiling from crushing!
    //
    override fun CeilingCrushStop(line: line_t): Int {
        var rtn = 0
        getActiveCeilings().forEach { ceiling ->
            if (ceiling != null && ceiling.tag == line.tag.toInt() && ceiling.direction != 0) {
                ceiling.olddirection = ceiling.direction
                ceiling.thinkerFunction = ActiveStates.NOP
                ceiling.direction = 0 // in-stasis
                rtn = 1
            }
        }
        return rtn
    }

    fun setActiveceilings(activeceilings: Array<ceiling_t?>) {
        contextRequire<Ceilings>(ActionsCeilings.KEY_CEILINGS).activeCeilings = activeceilings
    }

    fun getMaxCeilings(): Int {
        return contextRequire<Ceilings>(ActionsCeilings.KEY_CEILINGS).activeCeilings.size
    }

    fun getActiveCeilings(): Array<ceiling_t?> {
        return contextRequire(KEY_CEILINGS).activeCeilings
    }

    companion object {
        val KEY_CEILINGS: ContextKey<Ceilings> = ActionTrait.ACTION_KEY_CHAIN.newKey<Ceilings>(
            ActionsCeilings::class.java
        ) { Ceilings() }
    }
}