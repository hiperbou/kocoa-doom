package rr.drawfuns


/** Either draws a column or a span
 *
 * @author velktron
 */
interface ColumnFunction<T, V> {
    operator fun invoke()
    operator fun invoke(dcvars: ColVars<T, V>)

    /** A set of flags that help identifying the type of function  */
    fun getFlags(): Int
}