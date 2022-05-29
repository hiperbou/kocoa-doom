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
package p

import automap.IAutoMap
import data.sounds.sfxenum_t
import defines.skill_t
import doom.DoomMain
import doom.player_t
import hu.IHeadsUp
import i.IDoomSystem
import p.Actions.ActionTrait
import p.Actions.ActionsAttacks
import p.Actions.ActionsEnemies
import p.Actions.ActionsThinkers
import p.Actions.ActiveStates.Ai
import p.Actions.ActiveStates.Weapons
import p.Actions.ActiveStates.Attacks
import p.Actions.ActiveStatesimport.Thinkers
import p.mobj_t
import rr.SceneRenderer
import s.ISoundOrigin
import st.IDoomStatusBar
import utils.TraitFactory
import utils.TraitFactory.SharedContext
import java.util.logging.Level
import java.util.logging.Logger

class ActionFunctions(DOOM: DoomMain<*, *>) : UnifiedGameMap(DOOM), ActionsThinkers, ActionsEnemies, ActionsAttacks,
    Ai, Attacks, Thinkers, Weapons {
    private val traitsSharedContext: SharedContext = buildContext()

    private fun buildContext(): SharedContext {
        return try {
            TraitFactory.build<ActionFunctions>(this, ActionTrait.ACTION_KEY_CHAIN)
        } catch (ex: IllegalArgumentException) {
            Logger.getLogger(ActionFunctions::class.java.name).log(Level.SEVERE, null, ex)
            throw RuntimeException(ex)
        } catch (ex: IllegalAccessException) {
            Logger.getLogger(ActionFunctions::class.java.name).log(Level.SEVERE, null, ex)
            throw RuntimeException(ex)
        }
    }

    override fun levelLoader(): AbstractLevelLoader {
        return DOOM.levelLoader
    }

    override fun headsUp(): IHeadsUp {
        return DOOM.headsUp
    }

    override fun doomSystem(): IDoomSystem {
        return DOOM.doomSystem
    }

    override fun statusBar(): IDoomStatusBar {
        return DOOM.statusBar
    }

    override fun autoMap(): IAutoMap<*, *> {
        return DOOM.autoMap
    }

    override fun sceneRenderer(): SceneRenderer<*, *> {
        return DOOM.sceneRenderer
    }

    override val specials: Specials
        get() = SPECS

    override val switches: Switches
        get() = SW

    override fun StopSound(origin: ISoundOrigin?) {
        DOOM.doomSound.StopSound(origin)
    }

    override fun StartSound(origin: ISoundOrigin?, s: sfxenum_t?) {
        DOOM.doomSound.StartSound(origin, s)
    }

    override fun StartSound(origin: ISoundOrigin?, s: Int) {
        DOOM.doomSound.StartSound(origin, s)
    }

    override fun getPlayer(number: Int): player_t? {
        return DOOM.players[number]
    }

    override val gameSkill: skill_t?
        get() = DOOM.gameskill

    override fun createMobj(): mobj_t {
        return mobj_t.createOn(DOOM)
    }

    override fun LevelTime(): Int {
        return DOOM.leveltime
    }

    override fun P_Random(): Int {
        return DOOM.random.P_Random()
    }

    override fun ConsolePlayerNumber(): Int {
        return DOOM.consoleplayer
    }

    override fun MapNumber(): Int {
        return DOOM.gamemap
    }

    override fun PlayerInGame(number: Int): Boolean {
        return DOOM.playeringame[number]
    }

    override fun IsFastParm(): Boolean {
        return DOOM.fastparm
    }

    override fun IsPaused(): Boolean {
        return DOOM.getPaused()
    }

    override fun IsNetGame(): Boolean {
        return DOOM.netgame
    }

    override fun IsDemoPlayback(): Boolean {
        return DOOM.demoplayback
    }

    override fun IsDeathMatch(): Boolean {
        return DOOM.deathmatch
    }

    override fun IsAutoMapActive(): Boolean {
        return DOOM.automapactive
    }

    override fun IsMenuActive(): Boolean {
        return DOOM.menuactive
    }

    /**
     * TODO: avoid, deprecate
     */
    override fun DOOM(): DoomMain<*, *> {
        return DOOM
    }

    override fun getContext(): SharedContext {
        return traitsSharedContext
    }

    override val thinkers: ActionsThinkers
        get() = this

    override val enemies: ActionsEnemies
        get() = this

    override val attacks: ActionsAttacks
        get() = this
}