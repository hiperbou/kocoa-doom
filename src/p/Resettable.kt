package p


/** For objects that needed to be memset to 0 in C,
 * rather than being reallocated.  */
interface Resettable {
    fun reset()
}