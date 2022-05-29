package demo


import doom.ticcmd_t
import w.IWritableDoomObject

/** Demo Tic Commands can be read/written to disk/buffers,
 * and are not necessarily equal to the in-game ticcmd_t.
 * Thus, it's necessary for them to implement some
 * adaptor method (both ways).
 *
 * @author admin
 */
interface IDemoTicCmd : IWritableDoomObject {
    /** Decode this IDemoTicCmd into a standard ticcmd_t.
     *
     * @param source
     */
    fun decode(dest: ticcmd_t)

    /** Encode this IDemoTicCmd from a standard ticcmd_t.
     *
     * @param dest
     */
    fun encode(source: ticcmd_t)
}