package m


import doom.SourceCode.M_Menu
import doom.event_t


// -----------------------------------------------------------------------------
//
// $Id: IDoomMenu.java,v 1.5 2011/09/29 15:16:23 velktron Exp $
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// DESCRIPTION:
// Menu widget stuff, episode selection and such.
//    
// -----------------------------------------------------------------------------
/**
 *
 */
interface IDoomMenu {
    //
    // MENUS
    //
    /**
     * Called by main loop, saves config file and calls I_Quit when user exits.
     * Even when the menu is not displayed, this can resize the view and change
     * game parameters. Does all the real work of the menu interaction.
     */
    @M_Menu.C(M_Menu.M_Responder)
    fun Responder(ev: event_t): Boolean

    /**
     * Called by main loop, only used for menu (skull cursor) animation.
     */
    @M_Menu.C(M_Menu.M_Ticker)
    fun Ticker()

    /**
     * Called by main loop, draws the menus directly into the screen buffer.
     */
    @M_Menu.C(M_Menu.M_Drawer)
    fun Drawer()

    /**
     * Called by D_DoomMain, loads the config file.
     */
    @M_Menu.C(M_Menu.M_Init)
    fun Init()

    /**
     * Called by intro code to force menu up upon a keypress, does nothing if
     * menu is already up.
     */
    @M_Menu.C(M_Menu.M_StartControlPanel)
    fun StartControlPanel()
    var showMessages: Boolean
    var screenBlocks: Int
    val detailLevel: Int
    fun ClearMenus()
}