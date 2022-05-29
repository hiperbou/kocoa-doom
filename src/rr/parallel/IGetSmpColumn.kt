package rr.parallel


import rr.column_t

/** An interface used to ease the use of the GetCachedColumn by part
 * of parallelized renderers.
 *
 * @author Maes
 */
/**
 * Special version of GetColumn meant to be called concurrently by different
 * seg rendering threads, identfiex by index. This serves to avoid stomping
 * on mutual cached textures and causing crashes.
 *
 */
interface IGetSmpColumn {
    fun GetSmpColumn(tex: Int, col: Int, id: Int): column_t?
}