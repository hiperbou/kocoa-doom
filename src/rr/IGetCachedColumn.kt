package rr


/** An interface used to ease the use of the GetCachedColumn by part
 * of parallelized renderers.
 *
 * @author Maes
 */
interface IGetCachedColumn<T> {
    fun GetCachedColumn(tex: Int, col: Int): T
}