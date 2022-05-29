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

import data.Defines
import data.Defines.BT_ATTACK;
import data.Defines.PST_DEAD;
import data.Tables
import data.Tables.FINEANGLES;
import data.Tables.FINEMASK;
import data.Tables.finecosine;
import data.Tables.finesine;
import data.info
import data.info.states;
import data.sounds.sfxenum_t;
import defines.statenum_t;
import doom.items
import doom.items.weaponinfo;
import doom.player_t
import doom.player_t.Companion.LOWERSPEED;
import doom.player_t.Companion.RAISESPEED;
import doom.player_t.Companion.WEAPONBOTTOM;
import doom.player_t.Companion.WEAPONTOP;
import doom.player_t.Companion.ps_flash;
import doom.player_t.Companion.ps_weapon;
import doom.weapontype_t;
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedMul
import p.pspdef_t;
import utils.C2JUtils
import utils.C2JUtils.eval;

interface Weapons : Sounds {
    /**
     * A_WeaponReady
     * The player can fire the weapon
     * or change to another weapon at this time.
     * Follows after getting weapon up,
     * or after previous attack/fire sequence.
     */
    fun A_WeaponReady(player: player_t, psp: pspdef_t) {
        val newstate: statenum_t
        var angle: Int

        val mo = player.mo!!
        // get out of attack state
        if (mo.mobj_state === info.states[statenum_t.S_PLAY_ATK1.ordinal]
            || mo.mobj_state === info.states[statenum_t.S_PLAY_ATK2.ordinal]
        ) {
            mo.SetMobjState(statenum_t.S_PLAY)
        }
        if (player.readyweapon == weapontype_t.wp_chainsaw
            && psp.state === info.states[statenum_t.S_SAW.ordinal]
        ) {
            StartSound(player.mo, sfxenum_t.sfx_sawidl)
        }

        // check for change
        //  if player is dead, put the weapon away
        if (player.pendingweapon != weapontype_t.wp_nochange || !C2JUtils.eval(player.health[0])) {
            // change weapon
            //  (pending weapon should allready be validated)
            newstate = items.weaponinfo[player.readyweapon.ordinal].downstate
            player.SetPsprite(player_t.ps_weapon, newstate)
            return
        }

        // check for fire
        //  the missile launcher and bfg do not auto fire
        if (C2JUtils.eval(player.cmd.buttons.code and Defines.BT_ATTACK)) {
            if (!player.attackdown
                || (player.readyweapon != weapontype_t.wp_missile
                        && player.readyweapon != weapontype_t.wp_bfg)
            ) {
                player.attackdown = true
                enemies.FireWeapon(player)
                return
            }
        } else {
            player.attackdown = false
        }

        // bob the weapon based on movement speed
        angle = 128 * LevelTime() and Tables.FINEMASK
        psp.sx = FRACUNIT + FixedMul(player.bob, Tables.finecosine[angle])
        angle = angle and Tables.FINEANGLES / 2 - 1
        psp.sy = player_t.WEAPONTOP + FixedMul(player.bob, Tables.finesine[angle])
    }

    //
    // A_Raise
    //
    fun A_Raise(player: player_t, psp: pspdef_t) {
        val newstate: statenum_t

        //System.out.println("Trying to raise weapon");
        //System.out.println(player.readyweapon + " height: "+psp.sy);
        psp.sy -= player_t.RAISESPEED
        if (psp.sy > player_t.WEAPONTOP) {
            //System.out.println("Not on top yet, exit and repeat.");
            return
        }
        psp.sy = player_t.WEAPONTOP

        // The weapon has been raised all the way,
        //  so change to the ready state.
        newstate = items.weaponinfo[player.readyweapon.ordinal].readystate
        //System.out.println("Weapon raised, setting new state.");
        player.SetPsprite(player_t.ps_weapon, newstate)
    }

    //
    // A_ReFire
    // The player can re-fire the weapon
    // without lowering it entirely.
    //
    override fun A_ReFire(player: player_t, psp: pspdef_t?) {
        // check for fire
        //  (if a weaponchange is pending, let it go through instead)
        if (C2JUtils.eval(player.cmd.buttons.code and Defines.BT_ATTACK) && player.pendingweapon == weapontype_t.wp_nochange && C2JUtils.eval(
                player.health[0]
            )
        ) {
            player.refire++
            enemies.FireWeapon(player)
        } else {
            player.refire = 0
            player.CheckAmmo()
        }
    }

    //
    // A_GunFlash
    //
    fun A_GunFlash(player: player_t, psp: pspdef_t?) {
        player.mo!!.SetMobjState(statenum_t.S_PLAY_ATK2)
        player.SetPsprite(player_t.ps_flash, items.weaponinfo[player.readyweapon.ordinal].flashstate)
    }

    //
    // ?
    //
    fun A_Light0(player: player_t, psp: pspdef_t?) {
        player.extralight = 0
    }

    fun A_Light1(player: player_t, psp: pspdef_t?) {
        player.extralight = 1
    }

    fun A_Light2(player: player_t, psp: pspdef_t?) {
        player.extralight = 2
    }

    //
    // A_Lower
    // Lowers current weapon,
    //  and changes weapon at bottom.
    //
    fun A_Lower(player: player_t, psp: pspdef_t) {
        psp.sy += player_t.LOWERSPEED

        // Is already down.
        if (psp.sy < player_t.WEAPONBOTTOM) {
            return
        }

        // Player is dead.
        if (player.playerstate == Defines.PST_DEAD) {
            psp.sy = player_t.WEAPONBOTTOM

            // don't bring weapon back up
            return
        }

        // The old weapon has been lowered off the screen,
        // so change the weapon and start raising it
        if (!C2JUtils.eval(player.health[0])) {
            // Player is dead, so keep the weapon off screen.
            player.SetPsprite(player_t.ps_weapon, statenum_t.S_NULL)
            return
        }
        player.readyweapon = player.pendingweapon!!
        player.BringUpWeapon()
    }

    fun A_CheckReload(player: player_t, psp: pspdef_t?) {
        player.CheckAmmo()
        /*
        if (player.ammo[am_shell]<2)
        P_SetPsprite (player, ps_weapon, S_DSNR1);
         */
    }
}