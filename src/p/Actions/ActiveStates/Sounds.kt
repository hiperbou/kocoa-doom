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
package p.Actions.ActiveStates

import data.mobjtype_t;
import data.sounds.sfxenum_t;
import doom.player_t;
import p.Actions.ActionTrait;
import p.mobj_t;
import p.pspdef_t;

interface Sounds : ActionTrait {
    fun A_Chase(mo: mobj_t)
    fun A_ReFire(player: player_t, psp: pspdef_t?)
    fun A_SpawnFly(mo: mobj_t)
    fun A_Scream(actor: mobj_t) {
        val sound = when (actor.info!!.deathsound) {
            sfxenum_t.sfx_None -> return
            sfxenum_t.sfx_podth1, sfxenum_t.sfx_podth2, sfxenum_t.sfx_podth3 -> sfxenum_t.sfx_podth1.ordinal + P_Random() % 3
            sfxenum_t.sfx_bgdth1, sfxenum_t.sfx_bgdth2 -> sfxenum_t.sfx_bgdth1.ordinal + P_Random() % 2
            else -> actor.info!!.deathsound.ordinal
        }

        // Check for bosses.
        if (actor.type == mobjtype_t.MT_SPIDER
            || actor.type == mobjtype_t.MT_CYBORG
        ) {
            // full volume
            StartSound(null, sound)
        } else {
            StartSound(actor, sound)
        }
    }

    fun A_Hoof(mo: mobj_t) {
        StartSound(mo, sfxenum_t.sfx_hoof)
        A_Chase(mo)
    }

    //
    // A_BFGsound
    //
    fun A_BFGsound(player: player_t, psp: pspdef_t?) {
        StartSound(player.mo, sfxenum_t.sfx_bfg)
    }

    fun A_OpenShotgun2(player: player_t, psp: pspdef_t?) {
        StartSound(player.mo, sfxenum_t.sfx_dbopn)
    }

    fun A_LoadShotgun2(player: player_t, psp: pspdef_t?) {
        StartSound(player.mo, sfxenum_t.sfx_dbload)
    }

    fun A_CloseShotgun2(player: player_t, psp: pspdef_t?) {
        StartSound(player.mo, sfxenum_t.sfx_dbcls)
        A_ReFire(player, psp)
    }

    fun A_BrainPain(mo: mobj_t?) {
        StartSound(null, sfxenum_t.sfx_bospn)
    }

    fun A_Metal(mo: mobj_t) {
        StartSound(mo, sfxenum_t.sfx_metal)
        A_Chase(mo)
    }

    fun A_BabyMetal(mo: mobj_t) {
        StartSound(mo, sfxenum_t.sfx_bspwlk)
        A_Chase(mo)
    }

    // travelling cube sound
    fun A_SpawnSound(mo: mobj_t) {
        StartSound(mo, sfxenum_t.sfx_boscub)
        A_SpawnFly(mo)
    }

    fun A_PlayerScream(actor: mobj_t) {
        // Default death sound.
        var sound = sfxenum_t.sfx_pldeth
        if (DOOM().isCommercial() && actor.health < -50) {
            // IF THE PLAYER DIES
            // LESS THAN -50% WITHOUT GIBBING
            sound = sfxenum_t.sfx_pdiehi
        }
        StartSound(actor, sound)
    }
}