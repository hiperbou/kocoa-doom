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

import data.Limits
import data.sounds.sfxenum_t
import doom.thinker_t
import m.Settings
import m.fixed_t.Companion.FRACUNIT
import mochadoom.Engine
import mochadoom.Loggers
import p.ActiveStates
import p.plat_e
import p.plat_t
import p.plattype_e
import rr.line_t
import rr.sector_t
import utils.C2JUtils
import utils.TraitFactory.ContextKey
import java.util.function.Supplier
import java.util.logging.Level


interface ActionsPlats : ActionsMoveEvents, ActionsUseEvents {
    fun FindSectorFromLineTag(line: line_t, secnum: Int): Int
    override fun RemoveThinker(activeplat: thinker_t)
    class Plats {
        // activeplats is just a placeholder. Plat objects aren't
        // actually reused, so we don't need an initialized array.
        // Same rule when resizing.
        var activeplats = arrayOfNulls<plat_t>(Limits.MAXPLATS)

        companion object {
            val LOGGER = Loggers.getLogger(ActionsPlats::class.java.name)
        }
    }

    //
    // Do Platforms
    // "amount" is only used for SOME platforms.
    //
    override fun DoPlat(line: line_t, type: plattype_e?, amount: Int): Boolean {
        val ll = levelLoader()
        var plat: plat_t
        var secnum = -1
        var rtn = false
        var sec: sector_t
        when (type) {
            plattype_e.perpetualRaise -> ActivateInStasis(line.tag.toInt())
            else -> {}
        }
        while (FindSectorFromLineTag(line, secnum).also { secnum = it } >= 0) {
            sec = ll.sectors[secnum]
            if (sec.specialdata != null) {
                continue
            }

            // Find lowest & highest floors around sector
            rtn = true
            plat = plat_t()
            plat.type = type
            plat.sector = sec
            plat.sector!!.specialdata = plat
            plat.thinkerFunction = ActiveStates.T_PlatRaise
            AddThinker(plat)
            plat.crush = false
            plat.tag = line.tag.toInt()
            when (type) {
                plattype_e.raiseToNearestAndChange -> {
                    plat.speed = Limits.PLATSPEED / 2
                    sec.floorpic = ll.sides[line.sidenum[0].code].sector!!.floorpic
                    plat.high = sec.FindNextHighestFloor(sec.floorheight)
                    plat.wait = 0
                    plat.status = plat_e.up
                    // NO MORE DAMAGE, IF APPLICABLE
                    sec.special = 0
                    StartSound(sec.soundorg, sfxenum_t.sfx_stnmov)
                }
                plattype_e.raiseAndChange -> {
                    plat.speed = Limits.PLATSPEED / 2
                    sec.floorpic = ll.sides[line.sidenum[0].code].sector!!.floorpic
                    plat.high = sec.floorheight + amount * FRACUNIT
                    plat.wait = 0
                    plat.status = plat_e.up
                    StartSound(sec.soundorg, sfxenum_t.sfx_stnmov)
                }
                plattype_e.downWaitUpStay -> {
                    plat.speed = Limits.PLATSPEED * 4
                    plat.low = sec.FindLowestFloorSurrounding()
                    if (plat.low > sec.floorheight) {
                        plat.low = sec.floorheight
                    }
                    plat.high = sec.floorheight
                    plat.wait = 35 * Limits.PLATWAIT
                    plat.status = plat_e.down
                    StartSound(sec.soundorg, sfxenum_t.sfx_pstart)
                }
                plattype_e.blazeDWUS -> {
                    plat.speed = Limits.PLATSPEED * 8
                    plat.low = sec.FindLowestFloorSurrounding()
                    if (plat.low > sec.floorheight) {
                        plat.low = sec.floorheight
                    }
                    plat.high = sec.floorheight
                    plat.wait = 35 * Limits.PLATWAIT
                    plat.status = plat_e.down
                    StartSound(sec.soundorg, sfxenum_t.sfx_pstart)
                }
                plattype_e.perpetualRaise -> {
                    plat.speed = Limits.PLATSPEED
                    plat.low = sec.FindLowestFloorSurrounding()
                    if (plat.low > sec.floorheight) {
                        plat.low = sec.floorheight
                    }
                    plat.high = sec.FindHighestFloorSurrounding()
                    if (plat.high < sec.floorheight) {
                        plat.high = sec.floorheight
                    }
                    plat.wait = 35 * Limits.PLATWAIT
                    // Guaranteed to be 0 or 1.
                    plat.status = plat_e.values()[P_Random() and 1]
                    StartSound(sec.soundorg, sfxenum_t.sfx_pstart)
                }
            }
            AddActivePlat(plat)
        }
        return rtn
    }

    fun ActivateInStasis(tag: Int) {
        val plats = contextRequire<Plats>(ActionsPlats.KEY_PLATS)
        for (activeplat in plats.activeplats) {
            if (activeplat != null && activeplat.tag == tag && activeplat.status == plat_e.in_stasis) {
                activeplat.status = activeplat.oldstatus
                activeplat.thinkerFunction = ActiveStates.T_PlatRaise
            }
        }
    }

    override fun StopPlat(line: line_t) {
        val plats = contextRequire<Plats>(ActionsPlats.KEY_PLATS)
        for (activeplat in plats.activeplats) {
            if (activeplat != null && activeplat.status != plat_e.in_stasis && activeplat.tag == line.tag.toInt()) {
                activeplat.oldstatus = activeplat.status
                activeplat.status = plat_e.in_stasis
                activeplat.thinkerFunction = ActiveStates.NOP
            }
        }
    }

    fun AddActivePlat(plat: plat_t?) {
        val plats = contextRequire<Plats>(ActionsPlats.KEY_PLATS)
        for (i in plats.activeplats.indices) {
            if (plats.activeplats[i] == null) {
                plats.activeplats[i] = plat
                return
            }
        }
        /**
         * Added option to turn off the resize
         * - Good Sign 2017/04/26
         */
        // Uhh... lemme guess. Needs to resize?
        // Resize but leave extra items empty.
        if (Engine.getConfig().equals(Settings.extend_plats_limit, java.lang.Boolean.TRUE)) {
            plats.activeplats = C2JUtils.resizeNoAutoInit(plats.activeplats, 2 * plats.activeplats.size)
            AddActivePlat(plat)
        } else {
            Plats.LOGGER.log(Level.SEVERE, "P_AddActivePlat: no more plats!")
            System.exit(1)
        }
    }

    fun RemoveActivePlat(plat: plat_t) {
        val plats = contextRequire<Plats>(ActionsPlats.KEY_PLATS)
        for (i in plats.activeplats.indices) {
            if (plat === plats.activeplats[i]) {
                plats.activeplats[i]!!.sector!!.specialdata = null
                RemoveThinker(plats.activeplats[i]!!)
                plats.activeplats[i] = null
                return
            }
        }
        Plats.LOGGER.log(Level.SEVERE, "P_RemoveActivePlat: can't find plat!")
        System.exit(1)
    }

    fun ClearPlatsBeforeLoading() {
        val plats = contextRequire<Plats>(ActionsPlats.KEY_PLATS)
        for (i in plats.activeplats.indices) {
            plats.activeplats[i] = null
        }
    }

    companion object {
        val KEY_PLATS: ContextKey<Plats> =
            ActionTrait.ACTION_KEY_CHAIN.newKey<Plats>(ActionsPlats::class.java, Supplier { Plats() })
    }
}