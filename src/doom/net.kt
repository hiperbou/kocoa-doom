package doom


//-----------------------------------------------------------------------------
//
// $Id: net.java,v 1.5 2011/02/11 00:11:13 velktron Exp $
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
// $Log: net.java,v $
// Revision 1.5  2011/02/11 00:11:13  velktron
// A MUCH needed update to v1.3.
//
// Revision 1.1  2010/06/30 08:58:50  velktron
// Let's see if this stuff will finally commit....
//
//
// Most stuff is still  being worked on. For a good place to start and get an idea of what is being done, I suggest checking out the "testers" package.
//
// Revision 1.1  2010/06/29 11:07:34  velktron
// Release often, release early they say...
//
// Commiting ALL stuff done so far. A lot of stuff is still broken/incomplete, and there's still mixed C code in there. I suggest you load everything up in Eclpise and see what gives from there.
//
// A good place to start is the testers/ directory, where you  can get an idea of how a few of the implemented stuff works.
//
//
// DESCRIPTION:
//	DOOM Network game communication and protocol,
//	all OS independent parts.
//
//-----------------------------------------------------------------------------
//static const char rcsid[] = "$Id: net.java,v 1.5 2011/02/11 00:11:13 velktron Exp $";
//#include "m_menu.h"
//#include "i_system.h"
//#include "i_video.h"
//#include "i_net.h"
//#include "g_game.h"
//
//Network play related stuff.
//There is a data struct that stores network
//communication related stuff, and another
//one that defines the actual packets to
//be transmitted.
//
class net {
    var doomcom: doomcom_t? = null
    var netbuffer // points inside doomcom
            : doomdata_t? = null
    var localcmds = arrayOfNulls<ticcmd_t>(net.BACKUPTICS)
    val MAXPLAYERS = 4
    var netcmds = Array(MAXPLAYERS) { arrayOfNulls<ticcmd_t>(net.BACKUPTICS) }
    var nettics = IntArray(net.MAXNETNODES)
    var nodeingame = BooleanArray(net.MAXNETNODES) // set false as nodes leave game
    var remoteresend = BooleanArray(net.MAXNETNODES) // set when local needs tics
    var resendto = IntArray(net.MAXNETNODES) // set when remote needs tics
    var resendcount = IntArray(net.MAXNETNODES)
    var nodeforplayer = IntArray(MAXPLAYERS)
    var maketic = 0
    var lastnettic = 0
    var skiptics = 0
    var ticdup = 0
    var maxsend // BACKUPTICS/(2*ticdup)-1
            = 0

    //void D_ProcessEvents (void); 
    //void G_BuildTiccmd (ticcmd_t *cmd); 
    //void D_DoAdvanceDemo (void);
    var reboundpacket = false
    var reboundstore: doomdata_t? = null
    // 
    //
    //123
    /** MAES: interesting. After testing it was found to return the following size:
     *
     */
    fun NetbufferSize(): Int {
//    return (int)(((doomdata_t)0).cmds[netbuffer.numtics]);
        return 8 * (netbuffer!!.numtics + 1)
    }

    companion object {
        protected var NCMD_EXIT = -0x80000000
        protected var NCMD_RETRANSMIT = 0x40000000
        protected var NCMD_SETUP = 0x20000000
        protected var NCMD_KILL = 0x10000000 // kill game
        protected var NCMD_CHECKSUM = 0x0fffffff
        protected var DOOMCOM_ID = 0x12345678

        //Max computers/players in a game.
        protected var MAXNETNODES = 8

        //Networking and tick handling related.
        protected var BACKUPTICS = 12

        // commant_t
        protected var CMD_SEND = 1
        protected var CMD_GET = 2

        //
        // NETWORKING
        //
        // gametic is the tic about to (or currently being) run
        // maketic is the tick that hasn't had control made for it yet
        // nettics[] has the maketics for all players 
        //
        // a gametic cannot be run until nettics[] > gametic for all players
        //
        var RESENDCOUNT = 10
        var PL_DRONE = 0x80 // bit flag in doomdata->player
    }
}