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

import com.hiperbou.lang.apply
import com.hiperbou.lang.multiply
import data.Defines
import data.mobjtype_t
import data.sounds.sfxenum_t
import data.spritenum_t
import defines.ammotype_t
import defines.card_t
import doom.SourceCode
import doom.SourceCode.P_Map
import doom.englsh
import doom.player_t
import doom.weapontype_t
import m.Settings
import m.fixed_t.Companion.FRACUNIT
import p.Actions.ActionTrait.Movement
import p.mobj_t
import utils.C2JUtils

interface ActionsThings : ActionTrait {
    fun DamageMobj(thing: mobj_t, tmthing: mobj_t?, tmthing0: mobj_t?, damage: Int)
    fun RemoveMobj(special: mobj_t)

    /**
     * PIT_CheckThing
     */
    @P_Map.C(P_Map.PIT_CheckThing)
    override fun CheckThing(thing: mobj_t): Boolean {
        val movm = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        @SourceCode.fixed_t val blockdist: Int
        val solid: Boolean
        val damage: Int
        if (thing.flags and (mobj_t.MF_SOLID or mobj_t.MF_SPECIAL or mobj_t.MF_SHOOTABLE) == 0) {
            return true
        }
        blockdist = thing.radius + movm.tmthing!!.radius
        if (Math.abs(thing._x - movm.tmx) >= blockdist
            || Math.abs(thing._y - movm.tmy) >= blockdist
        ) {
            // didn't hit it
            return true
        }

        // don't clip against self
        if (thing == movm.tmthing) {
            return true
        }
        val tmthing = movm.tmthing!!
        // check for skulls slamming into things
        if (tmthing.flags and mobj_t.MF_SKULLFLY != 0) {
            damage = (P_Random() % 8 + 1) * tmthing.info!!.damage
            DamageMobj(thing, tmthing, tmthing, damage)
            tmthing.flags = tmthing.flags and mobj_t.MF_SKULLFLY.inv()
            tmthing.momz = 0
            tmthing.momy = tmthing.momz
            tmthing.momx = tmthing.momy
            tmthing.SetMobjState(tmthing.info!!.spawnstate)
            return false // stop moving
        }

        // missiles can hit other things
        if (C2JUtils.eval(tmthing.flags and mobj_t.MF_MISSILE)) {
            // see if it went over / under
            if (tmthing._z > thing._z + thing.height) {
                return true // overhead
            }
            if (tmthing._z + tmthing.height < thing._z) {
                return true // underneath
            }
            if (tmthing.target != null && (tmthing.target!!.type == thing.type
                        || (tmthing.target!!.type == mobjtype_t.MT_KNIGHT && thing.type == mobjtype_t.MT_BRUISER)
                        || (tmthing.target!!.type == mobjtype_t.MT_BRUISER && thing.type == mobjtype_t.MT_KNIGHT))) {
                // Don't hit same species as originator.
                if (thing === tmthing.target) {
                    return true
                }
                if (thing.type != mobjtype_t.MT_PLAYER) {
                    // Explode, but do no damage.
                    // Let players missile other players.
                    return false
                }
            }
            if (!C2JUtils.eval(thing.flags and mobj_t.MF_SHOOTABLE)) {
                // didn't do any damage
                return !C2JUtils.eval(thing.flags and mobj_t.MF_SOLID)
            }

            // damage / explode
            damage = (P_Random() % 8 + 1) * tmthing.info!!.damage
            DamageMobj(thing, tmthing, tmthing.target, damage)

            // don't traverse any more
            return false
        }

        // check for special pickup
        if (C2JUtils.eval(thing.flags and mobj_t.MF_SPECIAL)) {
            solid = C2JUtils.eval(thing.flags and mobj_t.MF_SOLID)
            if (C2JUtils.eval(movm.tmflags and mobj_t.MF_PICKUP)) {
                // can remove thing
                TouchSpecialThing(thing, tmthing)
            }
            return !solid
        }
        return !C2JUtils.eval(thing.flags and mobj_t.MF_SOLID)
    }

    /**
     * P_TouchSpecialThing LIKE ROMERO's ASS!!!
     */
    fun TouchSpecialThing(special: mobj_t, toucher: mobj_t) {
        val DOOM = DOOM()!!
        val player: player_t
        var i: Int
        @SourceCode.fixed_t val delta: Int
        var sound: sfxenum_t
        delta = special._z - toucher._z
        if (delta > toucher.height || delta < -8 * FRACUNIT) {
            // out of reach
            return
        }
        sound = sfxenum_t.sfx_itemup
        player = toucher.player!!

        // Dead thing touching.
        // Can happen with a sliding player corpse.
        if (toucher.health <= 0) {
            return
        }
        when (special.mobj_sprite) {
            spritenum_t.SPR_ARM1 -> {
                if (!player.GiveArmor(1)) {
                    return
                }
                player.message = englsh.GOTARMOR
            }
            spritenum_t.SPR_ARM2 -> {
                if (!player.GiveArmor(2)) {
                    return
                }
                player.message = englsh.GOTMEGA
            }
            spritenum_t.SPR_BON1 -> {
                player.health[0]++ // can go over 100%
                if (player.health[0] > 200) {
                    player.health[0] = 200
                }
                player.mo!!.health = player.health[0]
                player.message = englsh.GOTHTHBONUS
            }
            spritenum_t.SPR_BON2 -> {
                player.armorpoints[0]++ // can go over 100%
                if (player.armorpoints[0] > 200) {
                    player.armorpoints[0] = 200
                }
                if (player.armortype == 0) {
                    player.armortype = 1
                }
                player.message = englsh.GOTARMBONUS
            }
            spritenum_t.SPR_SOUL -> {
                player.health[0] += 100
                if (player.health[0] > 200) {
                    player.health[0] = 200
                }
                player.mo!!.health = player.health[0]
                player.message = englsh.GOTSUPER
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_MEGA -> {
                if (!DOOM.isCommercial()) {
                    return
                }
                player.health[0] = 200
                player.mo!!.health = player.health[0]
                player.GiveArmor(2)
                player.message = englsh.GOTMSPHERE
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_BKEY -> {
                if (!player.cards[card_t.it_bluecard.ordinal]) {
                    player.message = englsh.GOTBLUECARD
                }
                player.GiveCard(card_t.it_bluecard)
                if (DOOM.netgame) {
                    return
                }
            }
            spritenum_t.SPR_YKEY -> {
                if (!player.cards[card_t.it_yellowcard.ordinal]) {
                    player.message = englsh.GOTYELWCARD
                }
                player.GiveCard(card_t.it_yellowcard)
                if (DOOM.netgame) {
                    return
                }

            }
            spritenum_t.SPR_RKEY -> {
                if (!player.cards[card_t.it_redcard.ordinal]) {
                    player.message = englsh.GOTREDCARD
                }
                player.GiveCard(card_t.it_redcard)
                if (DOOM.netgame) {
                    return
                }

            }
            spritenum_t.SPR_BSKU -> {
                if (!player.cards[card_t.it_blueskull.ordinal]) {
                    player.message = englsh.GOTBLUESKUL
                }
                player.GiveCard(card_t.it_blueskull)
                if (DOOM.netgame) {
                    return
                }

            }
            spritenum_t.SPR_YSKU -> {
                if (!player.cards[card_t.it_yellowskull.ordinal]) {
                    player.message = englsh.GOTYELWSKUL
                }
                player.GiveCard(card_t.it_yellowskull)
                if (DOOM.netgame) {
                    return
                }

            }
            spritenum_t.SPR_RSKU -> {
                if (!player.cards[card_t.it_redskull.ordinal]) {
                    player.message = englsh.GOTREDSKULL
                }
                player.GiveCard(card_t.it_redskull)
                if (!DOOM.netgame) {
                    return
                }

            }
            spritenum_t.SPR_STIM -> {
                if (!player.GiveBody(10)) {
                    return
                }
                player.message = englsh.GOTSTIM
            }
            spritenum_t.SPR_MEDI -> {
                /**
                 * Another fix with switchable option to enable
                 * - Good Sign 2017/04/03
                 */
                val need = player.health[0] < 25
                if (!player.GiveBody(25)) {
                    return
                }
                if (DOOM.CM.equals(Settings.fix_medi_need, java.lang.Boolean.FALSE)) // default behavior - with bug
                {
                    player.message = if (player.health[0] < 25) englsh.GOTMEDINEED else englsh.GOTMEDIKIT
                } else  //proper behavior
                {
                    player.message = if (need) englsh.GOTMEDINEED else englsh.GOTMEDIKIT
                }
            }
            spritenum_t.SPR_PINV -> {
                if (!player.GivePower(Defines.pw_invulnerability)) {
                    return
                }
                player.message = englsh.GOTINVUL
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_PSTR -> {
                if (!player.GivePower(Defines.pw_strength)) {
                    return
                }
                player.message = englsh.GOTBERSERK
                if (player.readyweapon != weapontype_t.wp_fist) {
                    player.pendingweapon = weapontype_t.wp_fist
                }
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_PINS -> {
                if (!player.GivePower(Defines.pw_invisibility)) {
                    return
                }
                player.message = englsh.GOTINVIS
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_SUIT -> {
                if (!player.GivePower(Defines.pw_ironfeet)) {
                    return
                }
                player.message = englsh.GOTSUIT
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_PMAP -> {
                if (!player.GivePower(Defines.pw_allmap)) {
                    return
                }
                player.message = englsh.GOTMAP
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_PVIS -> {
                if (!player.GivePower(Defines.pw_infrared)) {
                    return
                }
                player.message = englsh.GOTVISOR
                sound = sfxenum_t.sfx_getpow
            }
            spritenum_t.SPR_CLIP -> {
                if (special.flags and mobj_t.MF_DROPPED != 0) {
                    if (!player.GiveAmmo(ammotype_t.am_clip, 0)) {
                        return
                    }
                } else {
                    if (!player.GiveAmmo(ammotype_t.am_clip, 1)) {
                        return
                    }
                }
                player.message = englsh.GOTCLIP
            }
            spritenum_t.SPR_AMMO -> {
                if (!player.GiveAmmo(ammotype_t.am_clip, 5)) {
                    return
                }
                player.message = englsh.GOTCLIPBOX
            }
            spritenum_t.SPR_ROCK -> {
                if (!player.GiveAmmo(ammotype_t.am_misl, 1)) {
                    return
                }
                player.message = englsh.GOTROCKET
            }
            spritenum_t.SPR_BROK -> {
                if (!player.GiveAmmo(ammotype_t.am_misl, 5)) {
                    return
                }
                player.message = englsh.GOTROCKBOX
            }
            spritenum_t.SPR_CELL -> {
                if (!player.GiveAmmo(ammotype_t.am_cell, 1)) {
                    return
                }
                player.message = englsh.GOTCELL
            }
            spritenum_t.SPR_CELP -> {
                if (!player.GiveAmmo(ammotype_t.am_cell, 5)) {
                    return
                }
                player.message = englsh.GOTCELLBOX
            }
            spritenum_t.SPR_SHEL -> {
                if (!player.GiveAmmo(ammotype_t.am_shell, 1)) {
                    return
                }
                player.message = englsh.GOTSHELLS
            }
            spritenum_t.SPR_SBOX -> {
                if (!player.GiveAmmo(ammotype_t.am_shell, 5)) {
                    return
                }
                player.message = englsh.GOTSHELLBOX
            }
            spritenum_t.SPR_BPAK -> {
                if (!player.backpack) {
                    player.maxammo.multiply(2, Defines.NUMAMMO)
                    player.backpack = true
                }

                ammotype_t.values().apply({ player.GiveAmmo(it, 1) }, Defines.NUMAMMO)
                player.message = englsh.GOTBACKPACK
            }
            spritenum_t.SPR_BFUG -> {
                if (!player.GiveWeapon(weapontype_t.wp_bfg, false)) {
                    return
                }
                player.message = englsh.GOTBFG9000
                sound = sfxenum_t.sfx_wpnup
            }
            spritenum_t.SPR_MGUN -> {
                if (!player.GiveWeapon(
                        weapontype_t.wp_chaingun,
                        special.flags and mobj_t.MF_DROPPED != 0
                    )
                ) {
                    return
                }
                player.message = englsh.GOTCHAINGUN
                sound = sfxenum_t.sfx_wpnup
            }
            spritenum_t.SPR_CSAW -> {
                if (!player.GiveWeapon(weapontype_t.wp_chainsaw, false)) {
                    return
                }
                player.message = englsh.GOTCHAINSAW
                sound = sfxenum_t.sfx_wpnup
            }
            spritenum_t.SPR_LAUN -> {
                if (!player.GiveWeapon(weapontype_t.wp_missile, false)) {
                    return
                }
                player.message = englsh.GOTLAUNCHER
                sound = sfxenum_t.sfx_wpnup
            }
            spritenum_t.SPR_PLAS -> {
                if (!player.GiveWeapon(weapontype_t.wp_plasma, false)) {
                    return
                }
                player.message = englsh.GOTPLASMA
                sound = sfxenum_t.sfx_wpnup
            }
            spritenum_t.SPR_SHOT -> {
                if (!player.GiveWeapon(
                        weapontype_t.wp_shotgun,
                        special.flags and mobj_t.MF_DROPPED != 0
                    )
                ) {
                    return
                }
                player.message = englsh.GOTSHOTGUN
                sound = sfxenum_t.sfx_wpnup
            }
            spritenum_t.SPR_SGN2 -> {
                if (!player.GiveWeapon(
                        weapontype_t.wp_supershotgun,
                        special.flags and mobj_t.MF_DROPPED != 0
                    )
                ) {
                    return
                }
                player.message = englsh.GOTSHOTGUN2
                sound = sfxenum_t.sfx_wpnup
            }
            else -> DOOM.doomSystem.Error("P_SpecialThing: Unknown gettable thing")
        }
        if (special.flags and mobj_t.MF_COUNTITEM != 0) {
            player.itemcount++
        }
        RemoveMobj(special)
        player.bonuscount += player_t.BONUSADD
        if (player === DOOM.players[DOOM.consoleplayer]) {
            DOOM.doomSound.StartSound(null, sound)
        }
    }

    /**
     * PIT_StompThing
     */
    @P_Map.C(P_Map.PIT_StompThing)
    override fun StompThing(thing: mobj_t): Boolean {
        val mov = contextRequire<Movement>(ActionTrait.KEY_MOVEMENT)
        @SourceCode.fixed_t val blockdist: Int
        if (thing.flags and mobj_t.MF_SHOOTABLE == 0) {
            return true
        }
        blockdist = thing.radius + mov.tmthing!!.radius
        if (Math.abs(thing._x - mov.tmx) >= blockdist || Math.abs(thing._y - mov.tmy) >= blockdist) {
            // didn't hit it
            return true
        }

        // don't clip against self
        if (thing === mov.tmthing) {
            return true
        }

        // monsters don't stomp things except on boss level
        if (mov.tmthing!!.player == null && MapNumber() != 30) {
            return false
        }
        DamageMobj(thing, mov.tmthing, mov.tmthing, 10000) // in interaction
        return true
    }
}