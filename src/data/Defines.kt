package data


import defines.ammotype_t
import defines.card_t
import doom.weapontype_t
import g.Signals.ScanCode
import m.fixed_t

//import m.define;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Defines.java,v 1.48 2012/09/24 17:16:22 velktron Exp $
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
//  Internally used data structures for virtually everything,
//   key definitions, lots of other stuff.
//
//-----------------------------------------------------------------------------
//#ifndef __DOOMDEF__
//#define __DOOMDEF__
//#include <stdio.h>
//#include <string.h>
//
// Global parameters/defines.
//
// DOOM version
object Defines {
    /** Seems to be 109 for shareware 1.9, wtf is this */
    const val VERSION = 109
    const val JAVARANDOM_MASK = 0x80

    /** Some parts of the code may actually be better used as if in a UNIX environment  */
    const val NORMALUNIX = false

    /** If rangecheck is undefined,  ost parameter validation debugging code will not be compiled  */
    const val RANGECHECK = false

    // Do or do not use external soundserver.
    // The sndserver binary to be run separately
    //  has been introduced by Dave Taylor.
    // The integrated sound support is experimental,
    //  and unfinished. Default is synchronous.
    // Experimental asynchronous timer based is
    //  handled by SNDINTR. 
    //#define SNDSERV  1
    //#define SNDINTR  1
    // Defines suck. C sucks.
    // C++ might sucks for OOP, but it sure is a better C.
    // So there.
    // MAES: moved static defines out of here and into VideoScaleInfo.
    // State updates, number of tics / second.
    const val BASETICRATE = 35
    const val TIC_MUL = 1
    const val TICRATE = Defines.BASETICRATE * Defines.TIC_MUL

    //
    // Difficulty/skill settings/filters.
    //
    // Skill flags.
    var MTF_EASY = 1
    var MTF_NORMAL = 2
    var MTF_HARD = 4

    // Deaf monsters/do not react to sound.
    var MTF_AMBUSH = 8

    //Maes: this makes it a bit less retarded.
    val NUMCARDS = card_t.NUMCARDS.ordinal

    //Maes: this makes it a bit less retarded.
    val NUMWEAPONS = weapontype_t.NUMWEAPONS.ordinal

    //Maes: this makes it a bit less retarded.
    val NUMAMMO = ammotype_t.NUMAMMO.ordinal

    // Power up artifacts.
    const val pw_invulnerability = 0
    const val pw_strength = 1
    const val pw_invisibility = 2
    const val pw_ironfeet = 3
    const val pw_allmap = 4
    const val pw_infrared = 5
    const val NUMPOWERS = 6

    /** Power up durations,
     * how many seconds till expiration,
     * assuming TICRATE is 35 ticks/second.
     */
    const val INVULNTICS = 30 * Defines.TICRATE
    const val INVISTICS = 60 * Defines.TICRATE
    const val INFRATICS = 120 * Defines.TICRATE
    const val IRONTICS = 60 * Defines.TICRATE

    // Center command from Heretic
    const val TOCENTER = -8

    // from r_defs.h:
    //Silhouette, needed for clipping Segs (mainly)
    //and sprites representing things.
    const val SIL_NONE = 0
    const val SIL_BOTTOM = 1
    const val SIL_TOP = 2
    const val SIL_BOTH = 3

    //SKY, store the number for name.
    const val SKYFLATNAME = "F_SKY1"

    // The sky map is 256*128*4 maps.
    const val ANGLETOSKYSHIFT = 22

    // From r_draw.c
    // status bar height at bottom of screen
    const val SBARHEIGHT = 32

    //
    //Different vetween registered DOOM (1994) and
    //Ultimate DOOM - Final edition (retail, 1995?).
    //This is supposedly ignored for commercial
    //release (aka DOOM II), which had 34 maps
    //in one episode. So there.
    const val NUMEPISODES = 4
    const val NUMMAPS = 9

    //in tics
    //U #define PAUSELEN        (TICRATE*2) 
    //U #define SCORESTEP       100
    //U #define ANIMPERIOD      32
    //pixel distance from "(YOU)" to "PLAYER N"
    //U #define STARDIST        10 
    //U #define WK 1
    // MAES 23/5/2011: moved SP_... stuff to EndLevel
    const val BACKUPTICS = 12

    // From Zone:
    //
    //ZONE MEMORY
    //PU - purge tags.
    //Tags < 100 are not overwritten until freed.
    const val PU_STATIC = 1 // static entire execution time
    const val PU_SOUND = 2 // static while playing
    const val PU_MUSIC = 3 // static while playing
    const val PU_DAVE = 4 // anything else Dave wants static
    const val PU_LEVEL = 50 // static until level exited
    const val PU_LEVSPEC = 51 // a special thinker in a level

    //Tags >= 100 are purgable whenever needed.
    const val PU_PURGELEVEL = 100
    const val PU_CACHE = 101

    // From hu_lib.h:
    //font stuff
    val HU_CHARERASE = ScanCode.SC_BACKSPACE
    const val HU_MAXLINES = 4
    const val HU_MAXLINELENGTH = 80

    // From hu_stuff.h
    //
    //Globally visible constants.
    //
    const val HU_FONTSTART = '!' // the first font characters
        .code.toByte()
    const val HU_FONTEND = '_' // the last font characters
        .code.toByte()

    //Calculate # of glyphs in font.
    const val HU_FONTSIZE = HU_FONTEND - HU_FONTSTART + 1
    const val HU_BROADCAST = 5.toChar()
    val HU_MSGREFRESH = ScanCode.SC_ENTER
    const val HU_MSGX = 0.toChar()
    const val HU_MSGY = 0.toChar()
    const val HU_MSGWIDTH = 64 // in characters
        .toChar()
    const val HU_MSGHEIGHT = 1 // in lines
        .toChar()
    const val HU_MSGTIMEOUT = 4 * Defines.TICRATE
    const val SAVESTRINGSIZE = 24

    //
    // Button/action code definitions.
    // From d_event.h
    // Press "Fire".
    const val BT_ATTACK = 1

    // Use button, to open doors, activate switches.
    const val BT_USE = 2

    // Flag: game events, not really buttons.
    const val BT_SPECIAL = 128
    const val BT_SPECIALMASK = 3

    // Flag, weapon change pending.
    // If true, the next 3 bits hold weapon num.
    const val BT_CHANGE = 4

    // The 3bit weapon mask and shift, convenience.
    const val BT_WEAPONMASK = 8 + 16 + 32
    const val BT_WEAPONSHIFT = 3

    // Pause the game.
    const val BTS_PAUSE = 1

    // Save the game at each console.
    const val BTS_SAVEGAME = 2

    // Savegame slot numbers
    //  occupy the second byte of buttons.    
    const val BTS_SAVEMASK = 4 + 8 + 16
    const val BTS_SAVESHIFT = 2

    //==================== Stuff from r_local.c =========================================
    val FLOATSPEED: Int = fixed_t.FRACUNIT * 4
    val VIEWHEIGHT: Int = 41 * fixed_t.FRACUNIT

    // mapblocks are used to check movement
    // against lines and things
    const val MAPBLOCKUNITS = 128
    val MAPBLOCKSIZE: Int = MAPBLOCKUNITS * fixed_t.FRACUNIT
    val MAPBLOCKSHIFT: Int = fixed_t.FRACBITS + 7
    val MAPBMASK = MAPBLOCKSIZE - 1
    val MAPBTOFRAC: Int = MAPBLOCKSHIFT - fixed_t.FRACBITS
    val BLOCKMAPPADDING: Int = 8 * fixed_t.FRACUNIT

    // player radius for movement checking
    val PLAYERRADIUS: Int = 16 * fixed_t.FRACUNIT
    val GRAVITY: Int = fixed_t.MAPFRACUNIT
    var USERANGE: Int = 64 * fixed_t.FRACUNIT
    var MELEERANGE: Int = 64 * fixed_t.FRACUNIT
    var MISSILERANGE: Int = 32 * 64 * fixed_t.FRACUNIT

    // follow a player exlusively for 3 seconds
    var BASETHRESHOLD = 100
    var PT_ADDLINES = 1
    var PT_ADDTHINGS = 2
    var PT_EARLYOUT = 4

    //
    // P_MOBJ
    //
    var ONFLOORZ = Limits.MININT
    var ONCEILINGZ = Limits.MAXINT

    // Time interval for item respawning.
    var ITEMQUESIZE = 128

    /** Indicate a leaf. e6y: support for extended nodes  */
    const val NF_SUBSECTOR = -0x80000000

    /** This is the regular leaf indicator. Use for reference/conversions  */
    const val NF_SUBSECTOR_CLASSIC = 0x8000

    /** Player states.  */
    const val PST_LIVE = 0

    // Playing or camping.    
    const val PST_DEAD = 1

    // Dead on the ground, view follows killer.
    const val PST_REBORN = 2 // Ready to restart/respawn???
    const val FF_FULLBRIGHT = 0x8000 // flag in thing->frame
    const val FF_FRAMEMASK = 0x7fff
    const val rcsid = "\$Id: Defines.java,v 1.48 2012/09/24 17:16:22 velktron Exp $"
}