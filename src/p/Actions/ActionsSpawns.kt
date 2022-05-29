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
import data.info.mobjinfo
import data.info.states
import data.sounds.sfxenum_t
import defines.skill_t
import defines.statenum_t
import doom.SourceCode
import doom.SourceCode.P_Mobj
import doom.player_t
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import p.Actions.ActionsSectors.Spawn
import p.ActiveStates
import p.mobj_t
import rr.subsector_t
import utils.C2JUtils
import v.graphics.Lights
import java.util.logging.Level

interface ActionsSpawns : ActionsSectors {
    /**
     * P_NightmareRespawn
     */
    fun NightmareRespawn(mobj: mobj_t) {
        val x: Int
        val y: Int
        val z: Int // fixed 
        val ss: subsector_t
        var mo: mobj_t
        val mthing: mapthing_t
        x = mobj.spawnpoint.x.toInt() shl FRACBITS
        y = mobj.spawnpoint.y.toInt() shl FRACBITS

        // somthing is occupying it's position?
        if (!CheckPosition(mobj, x, y)) {
            return  // no respwan
        }
        // spawn a teleport fog at old spot
        // because of removal of the body?
        mo = SpawnMobj(mobj._x, mobj._y, mobj.subsector!!.sector!!.floorheight, mobjtype_t.MT_TFOG)

        // initiate teleport sound
        StartSound(mo, sfxenum_t.sfx_telept)

        // spawn a teleport fog at the new spot
        ss = levelLoader().PointInSubsector(x, y)
        mo = SpawnMobj(x, y, ss.sector!!.floorheight, mobjtype_t.MT_TFOG)
        StartSound(mo, sfxenum_t.sfx_telept)

        // spawn the new monster
        mthing = mobj.spawnpoint

        // spawn it
        z = if (C2JUtils.eval(mobj.info!!.flags and mobj_t.MF_SPAWNCEILING)) {
            Defines.ONCEILINGZ
        } else {
            Defines.ONFLOORZ
        }

        // inherit attributes from deceased one
        mo = SpawnMobj(x, y, z, mobj.type!!)
        mo.spawnpoint = mobj.spawnpoint
        mo.angle = Tables.ANG45 * (mthing.angle / 45)
        if (C2JUtils.eval(mthing.options.toInt() and Defines.MTF_AMBUSH)) {
            mo.flags = mo.flags or mobj_t.MF_AMBUSH
        }
        mo.reactiontime = 18

        // remove the old monster,
        RemoveMobj(mobj)
    }

    /**
     * P_SpawnMobj
     *
     * @param x fixed
     * @param y fixed
     * @param z fixed
     * @param type
     * @return
     */
    @SourceCode.Exact
    @P_Mobj.C(P_Mobj.P_SpawnMobj)
    override fun SpawnMobj(
        @SourceCode.fixed_t x: Int,
        @SourceCode.fixed_t y: Int,
        @SourceCode.fixed_t z: Int,
        type: mobjtype_t
    ): mobj_t {
        var mobj: mobj_t
        val st: state_t
        val info: mobjinfo_t
        Z_Malloc@ run {
            mobj = createMobj()
        }
        info = mobjinfo.get(type.ordinal)
        mobj.type = type
        mobj.info = info
        mobj._x = x
        mobj._y = y
        mobj.radius = info.radius
        mobj.height = info.height
        mobj.flags = info.flags
        mobj.health = info.spawnhealth
        if (gameSkill != skill_t.sk_nightmare) {
            mobj.reactiontime = info.reactiontime
        }
        P_Random@ run {
            mobj.lastlook = P_Random() % Limits.MAXPLAYERS
        }
        // do not set the state with P_SetMobjState,
        // because action routines can not be called yet
        st = states.get(info.spawnstate.ordinal)
        mobj.mobj_state = st
        mobj.mobj_tics = st.tics.toLong()
        mobj.mobj_sprite = st.sprite
        mobj.mobj_frame = st.frame

        // set subsector and/or block links
        P_SetThingPosition@ run {
            SetThingPosition(mobj)
        }
        mobj.floorz = mobj.subsector!!.sector!!.floorheight
        mobj.ceilingz = mobj.subsector!!.sector!!.ceilingheight
        if (z == Defines.ONFLOORZ) {
            mobj._z = mobj.floorz
        } else if (z == Defines.ONCEILINGZ) {
            mobj._z = mobj.ceilingz - mobj.info!!.height
        } else {
            mobj._z = z
        }
        mobj.thinkerFunction = ActiveStates.P_MobjThinker
        P_AddThinker@ run {
            AddThinker(mobj)
        }
        return mobj
    }

    /**
     * P_SpawnPlayer
     * Called when a player is spawned on the level.
     * Most of the player structure stays unchanged
     * between levels.
     */
    @SourceCode.Exact
    @P_Mobj.C(P_Mobj.P_SpawnPlayer)
    fun SpawnPlayer(mthing: mapthing_t) {
        val p: player_t
        @SourceCode.fixed_t val x: Int
        @SourceCode.fixed_t val y: Int
        @SourceCode.fixed_t val z: Int
        var mobj: mobj_t

        // not playing?
        if (!PlayerInGame(mthing.type - 1)) {
            return
        }
        p = getPlayer(mthing.type - 1)!!
        if (p.playerstate == Defines.PST_REBORN) {
            G_PlayerReborn@ run {
                p.PlayerReborn()
            }
        }
        //DM.PlayerReborn (mthing.type-1);
        x = mthing.x.toInt() shl FRACBITS
        y = mthing.y.toInt() shl FRACBITS
        z = Defines.ONFLOORZ
        //P_SpawnMobj@ run {
            mobj = SpawnMobj(x, y, z, mobjtype_t.MT_PLAYER)
        //}

        // set color translations for player sprites
        if (mthing.type > 1) {
            mobj.flags = mobj.flags or (mthing.type - 1 shl mobj_t.MF_TRANSSHIFT)
        }
        mobj.angle = Tables.ANG45 * (mthing.angle / 45)
        mobj.player = p
        mobj.health = p.health[0]
        p.mo = mobj
        p.playerstate = Defines.PST_LIVE
        p.refire = 0
        p.message = null
        p.damagecount = 0
        p.bonuscount = 0
        p.extralight = 0
        p.fixedcolormap = Lights.COLORMAP_FIXED
        p.viewheight = Defines.VIEWHEIGHT

        // setup gun psprite
        //P_SetupPsprites@ run {
            p.SetupPsprites()
        //}

        // give all cards in death match mode
        if (IsDeathMatch()) {
            for (i in 0 until Defines.NUMCARDS) {
                p.cards[i] = true
            }
        }
        if (mthing.type - 1 == ConsolePlayerNumber()) {
            // wake up the status bar
            //ST_Start@ run {
                statusBar().Start()
            //}
            // wake up the heads up text
            //HU_Start@ run {
                headsUp().Start()
            //}
        }
    }

    /**
     * P_SpawnMapThing The fields of the mapthing should already be in host byte order.
     */
    fun SpawnMapThing(mthing: mapthing_t): mobj_t? {
        val D = DOOM()
        var i: Int
        val bit: Int
        val mobj: mobj_t
        val x: Int
        val y: Int
        val z: Int

        // count deathmatch start positions
        if (mthing.type.toInt() == 11) {
            if (D.deathmatch_p < 10 /*DM.deathmatchstarts[10]*/) {
                // memcpy (deathmatch_p, mthing, sizeof(*mthing));
                D.deathmatchstarts[D.deathmatch_p] = mapthing_t(mthing)
                D.deathmatch_p++
            }
            return null
        }
        if (mthing.type <= 0) {
            // Ripped from Chocolate Doom :-p
            // Thing type 0 is actually "player -1 start".  
            // For some reason, Vanilla Doom accepts/ignores this.
            // MAES: no kidding.
            return null
        }

        // check for players specially
        if (mthing.type <= 4 && mthing.type > 0) // killough 2/26/98 -- fix crashes
        {
            // save spots for respawning in network games
            D.playerstarts[mthing.type - 1] = mapthing_t(mthing)
            if (!IsDeathMatch()) {
                SpawnPlayer(mthing)
            }
            return null
        }

        // check for apropriate skill level
        if (!IsNetGame() && C2JUtils.eval(mthing.options.toInt() and 16)) {
            return null
        }
        bit = when (gameSkill) {
            skill_t.sk_baby -> 1
            skill_t.sk_nightmare -> 4
            else -> 1 shl gameSkill!!.ordinal - 1
        }
        if (!C2JUtils.eval(mthing.options.toInt() and bit)) {
            return null
        }

        // find which type to spawn
        i = 0
        while (i < Limits.NUMMOBJTYPES) {
            if (mthing.type.toInt() == info.mobjinfo[i].doomednum) {
                break
            }
            i++
        }

        // phares 5/16/98:
        // Do not abort because of an unknown thing. Ignore it, but post a
        // warning message for the player.
        if (i == Limits.NUMMOBJTYPES) {
            Spawn.LOGGER.log(
                Level.WARNING,
                String.format("P_SpawnMapThing: Unknown type %d at (%d, %d)", mthing.type, mthing.x, mthing.y)
            )
            return null
        }

        // don't spawn keycards and players in deathmatch
        if (IsDeathMatch() && C2JUtils.eval(info.mobjinfo[i].flags and mobj_t.MF_NOTDMATCH)) {
            return null
        }

        // don't spawn any monsters if -nomonsters
        if (D.nomonsters && (i == mobjtype_t.MT_SKULL.ordinal || C2JUtils.eval(info.mobjinfo[i].flags and mobj_t.MF_COUNTKILL))) {
            return null
        }

        // spawn it
        x = mthing.x.toInt() shl FRACBITS
        y = mthing.y.toInt() shl FRACBITS
        z = if (C2JUtils.eval(info.mobjinfo[i].flags and mobj_t.MF_SPAWNCEILING)) {
            Defines.ONCEILINGZ
        } else {
            Defines.ONFLOORZ
        }
        mobj = SpawnMobj(x, y, z, mobjtype_t.values()[i])
        mobj.spawnpoint.copyFrom(mthing)
        if (mobj.mobj_tics > 0) {
            mobj.mobj_tics = 1 + P_Random() % mobj.mobj_tics
        }
        if (C2JUtils.eval(mobj.flags and mobj_t.MF_COUNTKILL)) {
            D.totalkills++
        }
        if (C2JUtils.eval(mobj.flags and mobj_t.MF_COUNTITEM)) {
            D.totalitems++
        }
        mobj.angle = Tables.ANG45 * (mthing.angle / 45)
        if (C2JUtils.eval(mthing.options.toInt() and Defines.MTF_AMBUSH)) {
            mobj.flags = mobj.flags or mobj_t.MF_AMBUSH
        }
        return mobj
    }

    /**
     * P_SpawnBlood
     *
     * @param x fixed
     * @param y fixed
     * @param z fixed
     * @param damage
     */
    fun SpawnBlood(x: Int, y: Int, z: Int, damage: Int) {
        var z = z
        val th: mobj_t
        z += P_Random() - P_Random() shl 10
        th = SpawnMobj(x, y, z, mobjtype_t.MT_BLOOD)
        th.momz = FRACUNIT * 2
        th.mobj_tics -= (P_Random() and 3).toLong()
        if (th.mobj_tics < 1) {
            th.mobj_tics = 1
        }
        if (damage <= 12 && damage >= 9) {
            th.SetMobjState(statenum_t.S_BLOOD2)
        } else if (damage < 9) {
            th.SetMobjState(statenum_t.S_BLOOD3)
        }
    }

    /**
     * P_SpawnPuff
     *
     * @param x fixed
     * @param y fixed
     * @param z fixed
     */
    fun SpawnPuff(x: Int, y: Int, z: Int) {
        var z = z
        val th: mobj_t
        z += P_Random() - P_Random() shl 10
        th = SpawnMobj(x, y, z, mobjtype_t.MT_PUFF)
        th.momz = FRACUNIT
        th.mobj_tics -= (P_Random() and 3).toLong()
        if (th.mobj_tics < 1) {
            th.mobj_tics = 1
        }

        // don't make punches spark on the wall
        if (contextTest(ActionsSectors.KEY_SPAWN) { it.isMeleeRange }) {
            th.SetMobjState(statenum_t.S_PUFF3)
        }
    }
}