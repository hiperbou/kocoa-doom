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
import data.Tables
import data.mobjtype_t
import data.sounds.sfxenum_t
import defines.statenum_t
import doom.SourceCode.angle_t
import doom.items
import doom.player_t
import m.fixed_t
import p.Actions.ActionsSectors
import p.Actions.ActionsSectors.Spawn
import p.Actions.ActiveStatesimportimport.Monsters
import p.mobj_t
import p.pspdef_t
import utils.C2JUtils

interface Attacks : Monsters {
    //
    // A_FirePistol
    //
    fun A_FirePistol(player: player_t, psp: pspdef_t?) {
        StartSound(player.mo, sfxenum_t.sfx_pistol)
        player.mo!!.SetMobjState(statenum_t.S_PLAY_ATK2)
        player.ammo[items.weaponinfo[player.readyweapon.ordinal].ammo.ordinal]--
        player.SetPsprite(
            player_t.ps_flash,
            items.weaponinfo[player.readyweapon.ordinal].flashstate
        )
        attacks.P_BulletSlope(player.mo!!)
        attacks.P_GunShot(player.mo!!, !C2JUtils.eval(player.refire))
    }

    //
    // A_FireShotgun
    //
    fun A_FireShotgun(player: player_t, psp: pspdef_t?) {
        var i: Int
        StartSound(player.mo, sfxenum_t.sfx_shotgn)
        player.mo!!.SetMobjState(statenum_t.S_PLAY_ATK2)
        player.ammo[items.weaponinfo[player.readyweapon.ordinal].ammo.ordinal]--
        player.SetPsprite(
            player_t.ps_flash,
            items.weaponinfo[player.readyweapon.ordinal].flashstate
        )
        attacks.P_BulletSlope(player.mo!!)
        i = 0
        while (i < 7) {
            attacks.P_GunShot(player.mo!!, false)
            i++
        }
    }

    /**
     * A_FireShotgun2
     */
    fun A_FireShotgun2(player: player_t, psp: pspdef_t?) {
        val sp = enemies.contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        var angle: Long
        var damage: Int
        StartSound(player.mo, sfxenum_t.sfx_dshtgn)
        player.mo!!.SetMobjState(statenum_t.S_PLAY_ATK2)
        player.ammo[items.weaponinfo[player.readyweapon.ordinal].ammo.ordinal] -= 2
        player.SetPsprite(
            player_t.ps_flash,
            items.weaponinfo[player.readyweapon.ordinal].flashstate
        )
        attacks.P_BulletSlope(player.mo!!)
        for (i in 0..19) {
            damage = 5 * (P_Random() % 3 + 1)
            angle = player.mo!!.angle
            angle += (P_Random() - P_Random() shl 19).toLong()
            attacks.LineAttack(
                player.mo!!,
                angle,
                Defines.MISSILERANGE,
                sp.bulletslope + (P_Random() - P_Random() shl 5),
                damage
            )
        }
    }

    //
    // A_Punch
    //
    fun A_Punch(player: player_t, psp: pspdef_t?) {
        val sp = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        @angle_t var angle: Long
        var damage: Int
        val slope: Int
        damage = P_Random() % 10 + 1 shl 1
        if (C2JUtils.eval(player.powers[Defines.pw_strength])) {
            damage *= 10
        }
        angle = player.mo!!.angle
        //angle = (angle+(RND.P_Random()-RND.P_Random())<<18)/*&BITS32*/;
        // _D_: for some reason, punch didnt work until I change this
        // I think it's because of "+" VS "<<" prioritys...
        angle += (P_Random() - P_Random() shl 18).toLong()
        slope = attacks.AimLineAttack(player.mo!!, angle, Defines.MELEERANGE)
        attacks.LineAttack(player.mo!!, angle, Defines.MELEERANGE, slope, damage)

        // turn to face target
        if (C2JUtils.eval(sp.linetarget)) {
            StartSound(player.mo!!, sfxenum_t.sfx_punch)
            player.mo!!.angle = sceneRenderer().PointToAngle2(
                player.mo!!._x,
                player.mo!!._y,
                sp.linetarget!!._x,
                sp.linetarget!!._y
            ) and Tables.BITS32
        }
    }

    //
    // A_Saw
    //
    fun A_Saw(player: player_t, psp: pspdef_t?) {
        val sp = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        @angle_t var angle: Long
        val damage: Int
        val slope: Int
        damage = 2 * (P_Random() % 10 + 1)
        angle = player.mo!!.angle
        angle += (P_Random() - P_Random() shl 18).toLong()
        angle = angle and Tables.BITS32

        // use meleerange + 1 se the puff doesn't skip the flash
        slope = attacks.AimLineAttack(player.mo!!, angle, Defines.MELEERANGE + 1)
        attacks.LineAttack(player.mo!!, angle, Defines.MELEERANGE + 1, slope, damage)
        if (!C2JUtils.eval(sp.linetarget)) {
            StartSound(player.mo, sfxenum_t.sfx_sawful)
            return
        }
        StartSound(player.mo, sfxenum_t.sfx_sawhit)

        // turn to face target
        angle = sceneRenderer().PointToAngle2(
            player.mo!!._x, player.mo!!._y,
            sp.linetarget!!._x, sp.linetarget!!._y
        ) and Tables.BITS32
        /* FIXME: this comparison is going to fail.... or not?
            If e.g. angle = 359 degrees (which will be mapped to a small negative number),
            and player.mo.angle = 160 degrees (a large, positive value), the result will be a
            large negative value, which will still be "greater" than ANG180.
            
            It seems that *differences* between angles will always compare correctly, but
            not direct inequalities.
            
         */

        // Yet another screwy place where unsigned BAM angles are used as SIGNED comparisons.
        var dangle = angle - player.mo!!.angle
        dangle = dangle and Tables.BITS32
        if (dangle > Tables.ANG180) {
            if (dangle.toInt() < -Tables.ANG90 / 20) {
                player.mo!!.angle = angle + Tables.ANG90 / 21
            } else {
                player.mo!!.angle -= Tables.ANG90 / 20
            }
        } else {
            if (dangle > Tables.ANG90 / 20) {
                player.mo!!.angle = angle - Tables.ANG90 / 21
            } else {
                player.mo!!.angle += Tables.ANG90 / 20
            }
        }
        player.mo!!.angle = player.mo!!.angle and Tables.BITS32
        player.mo!!.flags = player.mo!!.flags or mobj_t.MF_JUSTATTACKED
    }

    //
    // A_FireMissile
    //
    fun A_FireMissile(player: player_t, psp: pspdef_t?) {
        player.ammo[items.weaponinfo[player.readyweapon.ordinal].ammo.ordinal]--
        attacks.SpawnPlayerMissile(player.mo!!, mobjtype_t.MT_ROCKET)
    }

    //
    // A_FireBFG
    //
    fun A_FireBFG(player: player_t, psp: pspdef_t?) {
        player.ammo[items.weaponinfo[player.readyweapon.ordinal].ammo.ordinal] -= BFGCELLS
        attacks.SpawnPlayerMissile(player.mo!!, mobjtype_t.MT_BFG)
    }

    //
    // A_FireCGun
    //
    fun A_FireCGun(player: player_t, psp: pspdef_t) {
        // For convenience.
        val readyweap = player.readyweapon.ordinal
        val flashstate = items.weaponinfo[readyweap].flashstate.ordinal
        val current_state = psp.state!!.id
        StartSound(player.mo, sfxenum_t.sfx_pistol)
        if (!C2JUtils.eval(player.ammo[items.weaponinfo[readyweap].ammo.ordinal])) {
            return
        }
        player.mo!!.SetMobjState(statenum_t.S_PLAY_ATK2)
        player.ammo[items.weaponinfo[readyweap].ammo.ordinal]--

        // MAES: Code to alternate between two different gun flashes
        // needed a clear rewrite, as it was way too messy.
        // We know that the flash states are a certain amount away from
        // the firing states. This amount is two frames.
        player.SetPsprite(
            player_t.ps_flash, statenum_t.values()[flashstate + current_state - statenum_t.S_CHAIN1.ordinal]
        )
        attacks.P_BulletSlope(player.mo!!)
        attacks.P_GunShot(player.mo!!, !C2JUtils.eval(player.refire))
    }

    //
    // A_FirePlasma
    //
    fun A_FirePlasma(player: player_t, psp: pspdef_t?) {
        player.ammo[items.weaponinfo[player.readyweapon.ordinal].ammo.ordinal]--
        player.SetPsprite(
            player_t.ps_flash,
            items.weaponinfo[player.readyweapon.ordinal].flashstate
        )
        attacks.SpawnPlayerMissile(player.mo!!, mobjtype_t.MT_PLASMA)
    }

    fun A_XScream(actor: mobj_t?) {
        StartSound(actor, sfxenum_t.sfx_slop)
    }

    fun A_Pain(actor: mobj_t) {
        if (actor.info!!.painsound != null) {
            StartSound(actor, actor.info!!.painsound)
        }
    }

    //
    // A_Explode
    //
    fun A_Explode(thingy: mobj_t) {
        attacks.RadiusAttack(thingy, thingy.target, 128)
    }

    //
    // A_BFGSpray
    // Spawn a BFG explosion on every monster in view
    //
    fun A_BFGSpray(mo: mobj_t) {
        val sp = contextRequire<Spawn>(ActionsSectors.KEY_SPAWN)
        var damage: Int
        var an: Long // angle_t

        // offset angles from its attack angle
        for (i in 0..39) {
            an = mo.angle - Tables.ANG90 / 2 + Tables.ANG90 / 40 * i and Tables.BITS32

            // mo.target is the originator (player)
            //  of the missile
            attacks.AimLineAttack(mo.target!!, an, 16 * 64 * fixed_t.FRACUNIT)
            if (!C2JUtils.eval(sp.linetarget)) {
                continue
            }
            enemies.SpawnMobj(
                sp.linetarget!!._x,
                sp.linetarget!!._y,
                sp.linetarget!!._z + (sp.linetarget!!.height shr 2),
                mobjtype_t.MT_EXTRABFG
            )
            damage = 0
            for (j in 0..14) {
                damage += (P_Random() and 7) + 1
            }
            enemies.DamageMobj(sp.linetarget!!, mo.target, mo.target, damage)
        }
    }

    companion object {
        // plasma cells for a bfg attack
        // IDEA: make action functions partially parametrizable?
        const val BFGCELLS = 40
    }
}