package p


import data.Defines
import m.fixed_t.Companion.MAPFRACUNIT

object ChaseDirections {
    const val DI_EAST = 0
    const val DI_NORTHEAST = 1
    const val DI_NORTH = 2
    const val DI_NORTHWEST = 3
    const val DI_WEST = 4
    const val DI_SOUTHWEST = 5
    const val DI_SOUTH = 6
    const val DI_SOUTHEAST = 7
    const val DI_NODIR = 8
    const val NUMDIR = 9

    //
    // P_NewChaseDir related LUT.
    //
    val opposite = intArrayOf(
        ChaseDirections.DI_WEST,
        ChaseDirections.DI_SOUTHWEST,
        ChaseDirections.DI_SOUTH,
        ChaseDirections.DI_SOUTHEAST,
        ChaseDirections.DI_EAST,
        ChaseDirections.DI_NORTHEAST,
        ChaseDirections.DI_NORTH,
        ChaseDirections.DI_NORTHWEST,
        ChaseDirections.DI_NODIR
    )
    val diags = intArrayOf(
        ChaseDirections.DI_NORTHWEST,
        ChaseDirections.DI_NORTHEAST,
        ChaseDirections.DI_SOUTHWEST,
        ChaseDirections.DI_SOUTHEAST
    )
    val xspeed = intArrayOf(
        MAPFRACUNIT,
        47000 / Defines.TIC_MUL,
        0,
        -47000 / Defines.TIC_MUL,
        -MAPFRACUNIT,
        -47000 / Defines.TIC_MUL,
        0,
        47000 / Defines.TIC_MUL
    ) // all

    // fixed
    val yspeed = intArrayOf(
        0,
        47000 / Defines.TIC_MUL,
        MAPFRACUNIT,
        47000 / Defines.TIC_MUL,
        0,
        -47000 / Defines.TIC_MUL,
        -MAPFRACUNIT,
        -47000 / Defines.TIC_MUL
    ) // all
    // fixed
}