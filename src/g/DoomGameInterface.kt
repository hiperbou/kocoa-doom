package g


import defines.skill_t
import doom.event_t
import doom.gameaction_t

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: DoomGameInterface.java,v 1.4 2010/12/20 17:15:08 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// DESCRIPTION:
//   Duh.
// 
//-----------------------------------------------------------------------------
interface DoomGameInterface {
    //
    // GAME
    //
    fun DeathMatchSpawnPlayer(playernum: Int)
    fun InitNew(skill: skill_t?, episode: Int, map: Int)

    /** Can be called by the startup code or M_Responder.
     * A normal game starts at map 1,
     * but a warp test can start elsewhere  */
    fun DeferedInitNew(skill: skill_t?, episode: Int, map: Int)
    fun DeferedPlayDemo(demo: String?)

    /** Can be called by the startup code or M_Responder,
     * calls P_SetupLevel or W_EnterWorld.  */
    fun LoadGame(name: String?)
    fun DoLoadGame()

    /** Called by M_Responder.  */
    fun SaveGame(slot: Int, description: String?)

    /** Only called by startup code.  */
    fun RecordDemo(name: String?)
    fun BeginRecording()
    fun PlayDemo(name: String?)
    fun TimeDemo(name: String?)
    fun CheckDemoStatus(): Boolean
    fun ExitLevel()
    fun SecretExitLevel()
    fun WorldDone()
    fun Ticker()
    fun Responder(ev: event_t?): Boolean
    fun ScreenShot()
    var gameAction: gameaction_t?
    var paused: Boolean
}