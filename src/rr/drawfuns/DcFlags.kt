package rr.drawfuns


/** Flags used to mark column functions according to type,
 * for quick type identification.
 *
 * @author velktron
 */
object DcFlags {
    const val FUZZY = 0x1
    const val TRANSLATED = 0x2
    const val TRANSPARENT = 0x4
    const val LOW_DETAIL = 0x8
}