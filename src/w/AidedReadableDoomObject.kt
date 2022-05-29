package w

import java.io.DataInputStream
import java.io.IOException

/** This is for objects that can be read from disk, but cannot
 * self-determine their own length for some reason.
 *
 * @author Maes
 */
interface AidedReadableDoomObject {
    @Throws(IOException::class)
    fun read(f: DataInputStream?, len: Int)
}