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
import data.sounds.sfxenum_t
import doom.CommandVariable
import doom.SourceCode.*
import doom.thinker_t
import m.fixed_t.Companion.FRACBITS
import p.*
import p.Actions.ActionsSectors.RespawnQueue
import rr.sector_t
import rr.subsector_t
import utils.C2JUtils

interface ActionsThinkers : ActionsSectors, ThinkerList {
    //
    // P_RemoveThinker
    // Deallocation is lazy -- it will not actually be freed
    // until its thinking turn comes up.
    //
    //
    // killough 4/25/98:
    //
    // Instead of marking the function with -1 value cast to a function pointer,
    // set the function to P_RemoveThinkerDelayed(), so that later, it will be
    // removed automatically as part of the thinker process.
    //
    @Compatible("thinker->function.acv = (actionf_v)(-1)")
    @P_Tick.C(P_Tick.P_RemoveThinker)
    override fun RemoveThinker(thinker: thinker_t) {
        thinker.thinkerFunction = RemoveState.REMOVE
    }

    /**
     * P_SpawnSpecials After the map has been loaded, scan for specials that spawn thinkers
     */
    @Suspicious(CauseOfDesyncProbability.LOW)
    @P_Spec.C(P_Spec.P_SpawnSpecials)
    fun SpawnSpecials() {
        val D = DOOM()
        val ll = levelLoader()
        val sp = specials
        var sector: sector_t

        /*int     episode;

        episode = 1;
        if (W.CheckNumForName("texture2") >= 0)
        episode = 2;
         */
        // See if -TIMER needs to be used.
        sp.levelTimer = false
        if (D.cVarManager.bool(CommandVariable.AVG) && IsDeathMatch()) {
            sp.levelTimer = true
            sp.levelTimeCount = 20 * 60 * 35
        }
        if (IsDeathMatch()) {
            D.cVarManager.with(CommandVariable.TIMER, 0) { i: Int? ->
                sp.levelTimer = true
                sp.levelTimeCount = i!! * 60 * 35
            }
        }

        //  Init special SECTORs.
        //sector = LL.sectors;
        for (i in 0 until ll.numsectors) {
            sector = ll.sectors[i]
            if (!C2JUtils.eval(sector.special.toInt())) {
                continue
            }
            when (sector.special.toInt()) {
                1 ->                     // FLICKERING LIGHTS
                    //P_SpawnLightFlash@ {
                        SpawnLightFlash(sector)
                    //}
                2 ->                     // STROBE FAST
                    //P_SpawnStrobeFlash@ {
                        SpawnStrobeFlash(sector, DoorDefines.FASTDARK, 0)
                    //}
                3 ->                     // STROBE SLOW
                    //P_SpawnStrobeFlash@ {
                        SpawnStrobeFlash(sector, DoorDefines.SLOWDARK, 0)
                    //}
                4 -> {
                    // STROBE FAST/DEATH SLIME
                    //P_SpawnStrobeFlash@ run {
                        SpawnStrobeFlash(sector, DoorDefines.FASTDARK, 0)
                    //}
                    sector.special = 4
                }
                8 ->                     // GLOWING LIGHT
                    //P_SpawnGlowingLight@ {
                        SpawnGlowingLight(sector)
                    //}
                9 ->                     // SECRET SECTOR
                    D.totalsecret++
                10 ->                     // DOOR CLOSE IN 30 SECONDS
                    //SpawnDoorCloseIn30@ {
                        SpawnDoorCloseIn30(sector)
                    //}
                12 ->                     // SYNC STROBE SLOW
                    //P_SpawnStrobeFlash@ {
                        SpawnStrobeFlash(sector, DoorDefines.SLOWDARK, 1)
                    //}
                13 ->                     // SYNC STROBE FAST
                    //P_SpawnStrobeFlash@ {
                        SpawnStrobeFlash(sector, DoorDefines.FASTDARK, 1)
                    //}
                14 ->                     // DOOR RAISE IN 5 MINUTES
                    //P_SpawnDoorRaiseIn5Mins@ {
                        SpawnDoorRaiseIn5Mins(sector, i)
                    //}
                17 -> //P_SpawnFireFlicker@ {
                    SpawnFireFlicker(sector)
                //}
            }
        }

        //  Init line EFFECTs
        sp.numlinespecials = 0
        for (i in 0 until ll.numlines) {
            when (ll.lines[i].special.toInt()) {
                48 -> {
                    // EFFECT FIRSTCOL SCROLL+
                    // Maes 6/4/2012: removed length limit.
                    if (sp.numlinespecials.toInt() == sp.linespeciallist.size) {
                        sp.resizeLinesSpecialList()
                    }
                    sp.linespeciallist[sp.numlinespecials.toInt()] = ll.lines[i]
                    sp.numlinespecials++
                }
            }
        }

        //  Init other misc stuff
        for (i in 0 until this.getMaxCeilings()) {
            this.getActiveCeilings()[i] = null
        }
        switches.initButtonList()

        // UNUSED: no horizonal sliders.
        // if (SL!=null) {
        // SL.updateStatus(DM);
        //  SL.P_InitSlidingDoorFrames();
        //}
    }

    /**
     * P_RespawnSpecials
     */
    fun RespawnSpecials() {
        val resp = contextRequire<RespawnQueue>(ActionsSectors.KEY_RESP_QUEUE)
        val x: Int
        val y: Int
        val z: Int // fixed
        val ss: subsector_t
        var mo: mobj_t
        val mthing: mapthing_t
        var i: Int

        // only respawn items in deathmatch (deathmatch!=2)
        if (!DOOM().altdeath) {
            return  // 
        }
        // nothing left to respawn?
        if (resp.iquehead == resp.iquetail) {
            return
        }

        // wait at least 30 seconds
        if (LevelTime() - resp.itemrespawntime[resp.iquetail] < 30 * 35) {
            return
        }
        mthing = resp.itemrespawnque[resp.iquetail]!!
        x = mthing.x.toInt() shl FRACBITS
        y = mthing.y.toInt() shl FRACBITS

        // spawn a teleport fog at the new spot
        ss = levelLoader().PointInSubsector(x, y)
        mo = SpawnMobj(x, y, ss.sector!!.floorheight, mobjtype_t.MT_IFOG)
        StartSound(mo, sfxenum_t.sfx_itmbk)

        // find which type to spawn
        i = 0
        while (i < mobjtype_t.NUMMOBJTYPES.ordinal) {
            if (mthing.type.toInt() == info.mobjinfo[i].doomednum) {
                break
            }
            i++
        }

        // spawn it
        z = if (C2JUtils.eval(info.mobjinfo[i].flags and mobj_t.MF_SPAWNCEILING)) {
            Defines.ONCEILINGZ
        } else {
            Defines.ONFLOORZ
        }
        mo = SpawnMobj(x, y, z, mobjtype_t.values()[i])
        mo.spawnpoint = mthing
        mo.angle = Tables.ANG45 * (mthing.angle / 45)

        // pull it from the que
        resp.iquetail = resp.iquetail + 1 and Defines.ITEMQUESIZE - 1
    }

    //
    // P_AllocateThinker
    // Allocates memory and adds a new thinker at the end of the list.
    //
    //public void AllocateThinker(thinker_t thinker) {;
    // UNUSED
    //}
    //
    // P_RunThinkers
    //
    fun RunThinkers() {
        var thinker = getThinkerCap().next
        while (thinker !== getThinkerCap()) {
            if (thinker!!.thinkerFunction == RemoveState.REMOVE) {
                // time to remove it
                thinker.next!!.prev = thinker.prev
                thinker.prev!!.next = thinker.next
                // Z_Free (currentthinker);
            } else {
                val thinkerFunction = thinker.thinkerFunction
                if (thinkerFunction.activeState is MobjActiveStates) {
                    (thinkerFunction.activeState as MobjActiveStates).accept(DOOM().actions, MobjConsumer(thinker as mobj_t))
                } else if (thinkerFunction.activeState is ThinkerActiveStates) {
                    (thinkerFunction.activeState as ThinkerActiveStates).accept(DOOM().actions, ThinkerConsumer(thinker))
                }
            }
            thinker = thinker.next
        }
    }

    //
    //P_Ticker
    //
    fun Ticker() {
        // run the tic
        if (IsPaused()) {
            return
        }

        // pause if in menu and at least one tic has been run
        if (!IsNetGame() && IsMenuActive() && !IsDemoPlayback() && getPlayer(ConsolePlayerNumber())!!.viewz != 1) {
            return
        }
        for (i in 0 until Limits.MAXPLAYERS) {
            if (PlayerInGame(i)) {
                getPlayer(i)!!.PlayerThink()
            }
        }
        RunThinkers()
        specials.UpdateSpecials() // In specials. Merge?
        RespawnSpecials()

        // for par times
        DOOM().leveltime++
    }
}