package p


/** YEAH, I'M USING THE CONSTANTS INTERFACE PATTERN. DEAL WITH IT  */
interface MobjFlags {
    companion object {
        // // MF_ flags for mobjs.
        // Call P_SpecialThing when touched.
        const val MF_SPECIAL = 1

        // Blocks.
        const val MF_SOLID = 2

        // Can be hit.
        const val MF_SHOOTABLE = 4

        // Don't use the sector links (invisible but touchable).
        const val MF_NOSECTOR = 8

        // Don't use the blocklinks (inert but displayable)
        const val MF_NOBLOCKMAP = 16

        // Not to be activated by sound, deaf monster.
        const val MF_AMBUSH = 32

        // Will try to attack right back.
        const val MF_JUSTHIT = 64

        // Will take at least one step before attacking.
        const val MF_JUSTATTACKED = 128

        // On level spawning (initial position),
        // hang from ceiling instead of stand on floor.
        const val MF_SPAWNCEILING = 256

        // Don't apply gravity (every tic),
        // that is, object will float, keeping current height
        // or changing it actively.
        const val MF_NOGRAVITY = 512

        // Movement flags.
        // This allows jumps from high places.
        const val MF_DROPOFF = 0x400

        // For players, will pick up items.
        const val MF_PICKUP = 0x800

        // Player cheat. ???
        const val MF_NOCLIP = 0x1000

        // Player: keep info about sliding along walls.
        const val MF_SLIDE = 0x2000

        // Allow moves to any height, no gravity.
        // For active floaters, e.g. cacodemons, pain elementals.
        const val MF_FLOAT = 0x4000

        // Don't cross lines
        // ??? or look at heights on teleport.
        const val MF_TELEPORT = 0x8000

        // Don't hit same species, explode on block.
        // Player missiles as well as fireballs of various kinds.
        const val MF_MISSILE = 0x10000

        // Dropped by a demon, not level spawned.
        // E.g. ammo clips dropped by dying former humans.
        const val MF_DROPPED = 0x20000

        // Use fuzzy draw (shadow demons or spectres),
        // temporary player invisibility powerup.
        const val MF_SHADOW = 0x40000

        // Flag: don't bleed when shot (use puff),
        // barrels and shootable furniture shall not bleed.
        const val MF_NOBLOOD = 0x80000

        // Don't stop moving halfway off a step,
        // that is, have dead bodies slide down all the way.
        const val MF_CORPSE = 0x100000

        // Floating to a height for a move, ???
        // don't auto float to target's height.
        const val MF_INFLOAT = 0x200000

        // On kill, count this enemy object
        // towards intermission kill total.
        // Happy gathering.
        const val MF_COUNTKILL = 0x400000

        // On picking up, count this item object
        // towards intermission item total.
        const val MF_COUNTITEM = 0x800000

        // Special handling: skull in flight.
        // Neither a cacodemon nor a missile.
        const val MF_SKULLFLY = 0x1000000

        // Don't spawn this object
        // in death match mode (e.g. key cards).
        const val MF_NOTDMATCH = 0x2000000

        // Player sprites in multiplayer modes are modified
        // using an internal color lookup table for re-indexing.
        // If 0x4 0x8 or 0xc,
        // use a translation table for player colormaps
        const val MF_TRANSLATION = 0xc000000

        // Hmm ???.
        const val MF_TRANSSHIFT = 26
        const val MF_UNUSED2 = 0x0000000010000000.toLong()
        const val MF_UNUSED3 = 0x0000000020000000.toLong()

        // Translucent sprite?                                          // phares
        const val MF_TRANSLUCENT = 0x0000000040000000.toLong()

        // this is free            LONGLONG(0x0000000100000000)
        // these are greater than an int. That's why the flags below are now uint_64_t
        const val MF_TOUCHY = 0x0000000100000000L
        const val MF_BOUNCES = 0x0000000200000000L
        const val MF_FRIEND = 0x0000000400000000L
        const val MF_RESSURECTED = 0x0000001000000000L
        const val MF_NO_DEPTH_TEST = 0x0000002000000000L
        const val MF_FOREGROUND = 0x0000004000000000L
    }
}