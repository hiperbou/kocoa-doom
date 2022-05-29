package p


import defines.skill_t
import doom.SourceCode.P_Setup

import rr.subsector_t
import java.io.IOException

interface ILevelLoader {
    /** P_SetupLevel
     *
     * @param episode
     * @param map
     * @param playermask
     * @param skill
     * @throws IOException
     */
    @P_Setup.C(P_Setup.P_SetupLevel)
    @Throws(IOException::class)
    fun SetupLevel(episode: Int, map: Int, playermask: Int, skill: skill_t?)

    /**
     * P_SetThingPosition Links a thing into both a block and a subsector based
     * on it's x y. Sets thing.subsector properly
     *
     *
     * @param thing
     */
    fun SetThingPosition(thing: mobj_t)

    /**
     * R_PointInSubsector
     *
     * MAES: it makes more sense to have this here.
     *
     * @param x fixed
     * @param y fixed
     */
    fun PointInSubsector(x: Int, y: Int): subsector_t

    companion object {
        // Lump order in a map WAD: each map needs a couple of lumps
        // to provide a complete scene geometry description.
        const val ML_LABEL = 0

        /** A separator, name, ExMx or MAPxx  */
        const val ML_THINGS = 1

        /** Monsters, items..  */
        const val ML_LINEDEFS = 2

        /** LineDefs, from editing  */
        const val ML_SIDEDEFS = 3

        /** SideDefs, from editing  */
        const val ML_VERTEXES = 4

        /** Vertices, edited and BSP splits generated  */
        const val ML_SEGS = 5

        /** LineSegs, from LineDefs split by BSP  */
        const val ML_SSECTORS = 6

        /** SubSectors, list of LineSegs  */
        const val ML_NODES = 7

        /** BSP nodes  */
        const val ML_SECTORS = 8

        /** Sectors, from editing  */
        const val ML_REJECT = 9

        /** LUT, sector-sector visibility  */
        const val ML_BLOCKMAP = 10

        // Maintain single and multi player starting spots.
        const val MAX_DEATHMATCH_STARTS = 10

        /** Expected lump names for verification  */
        val LABELS = arrayOf(
            "MAPNAME", "THINGS", "LINEDEFS", "SIDEDEFS",
            "VERTEXES", "SEGS", "SSECTORS", "NODES",
            "SECTORS", "REJECT", "BLOCKMAP"
        )
    }
}