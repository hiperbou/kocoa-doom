package data

import m.fixed_t

/** Everything that constitutes a removable limit should go here  */
object Limits {
    // Obvious rendering limits
    const val MAXVISPLANES = 128
    const val MAXSEGS = 32
    const val MAXVISSPRITES = 128
    const val MAXDRAWSEGS = 256

    // MAES: Moved MAXOPENINGS to renderer state, it's scale dependant.
    val CEILSPEED: Int = fixed_t.MAPFRACUNIT
    const val CEILWAIT = 150
    const val MAXCEILINGS = 30
    const val MAXANIMS = 32

    /** Animating line specials  */
    const val MAXLINEANIMS = 64

    // These are only used in the renderer, effectively putting
    // a limit to the size of lookup tables for screen buffers.
    const val MAXWIDTH = 1600
    const val MAXHEIGHT = 1200

    // Command line/file limits
    const val MAXWADFILES = 20
    const val MAXARGVS = 100

    // The maximum number of players, multiplayer/networking.
    // Max computers/players in a game. AFFECTS SAVEGAMES.
    const val MAXPLAYERS = 4
    const val MAXNETNODES = 8

    /** Some quirky engine limits  */
    const val MAXEVENTS = 64

    /** max # of wall switch TYPES in a level  */
    const val MAXSWITCHES = 50

    /** 20 adjoining sectors max!  */
    const val MAX_ADJOINING_SECTORS = 20

    // 4 players, 4 buttons each at once, max.
    const val MAXBUTTONS = 16

    // 1 second, in ticks.
    const val BUTTONTIME = 35

    /**
     * keep track of special lines as they are hit, but don't process them until
     * the move is proven valid
     */
    const val MAXSPECIALCROSS = 8
    const val MAXHEALTH = 100

    /**
     * MAXRADIUS is for precalculated sector block boxes the spider demon is
     * larger, but we do not have any moving sectors nearby
     */
    val MAXRADIUS: Int = 32 * fixed_t.FRACUNIT
    const val MAXINTERCEPTS = 128
    val MAXMOVE: Int = 30 * fixed_t.MAPFRACUNIT

    /** Player spawn spots for deathmatch.  */
    const val MAX_DM_STARTS = 10

    // C's "chars" are actually Java signed bytes.
    const val MAXCHAR = 0x7f.toByte()
    const val MINCHAR = 0x80.toByte()

    // 16-bit integers...
    const val MAXSHORT = 0x7fff.toShort()
    const val MINSHORT = 0x8000.toShort()

    // Max pos 32-bit int.
    const val MAXINT = 0x7fffffff
    const val MAXLONG = 0x7fffffffL

    // Max negative 32-bit integer. These are considered to be the same.
    const val MININT = -0x80000000
    const val MINLONG = 0x80000000L

    // Buffering/memory limits.
    const val SAVEGAMESIZE = 0x2c000
    const val SAVESTRINGSIZE = 24
    const val VERSIONSIZE = 16
    const val PLATWAIT = 3
    val PLATSPEED: Int = fixed_t.MAPFRACUNIT
    const val MAXPLATS = 30
    const val MAXSKULLS = 20
    const val NUMBRAINTARGETS = 32
    val NUMMOBJTYPES = mobjtype_t.NUMMOBJTYPES.ordinal
}