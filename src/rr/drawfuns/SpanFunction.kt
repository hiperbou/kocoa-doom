package rr.drawfuns


/** Either draws a column or a span
 *
 * @author velktron
 */
interface SpanFunction<T, V> {
    operator fun invoke()
    operator fun invoke(dsvars: SpanVars<T, V>)
}