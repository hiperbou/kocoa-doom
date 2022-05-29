package rr


/** An interface used to ease the use of the GetCachedColumn by part
 * of parallelized renderers.
 *
 * @author Maes
 */
interface IGetColumn<T> {
    fun GetColumn(tex: Int, col: Int): T
}