package w

import java.io.IOException
import java.nio.ByteBuffer

/** A container allowing for caching of arrays of CacheableDoomObjects
 *
 * It's a massive improvement over the older system, allowing for proper
 * caching and auto-unpacking of arrays of CacheableDoomObjects and much
 * cleaner code throughout.
 *
 * The container itself is a CacheableDoomObject....can you feel the
 * abuse? ;-)
 *
 */
class CacheableDoomObjectContainer<T : CacheableDoomObject?>(private val stuff: Array<T>) : CacheableDoomObject {
    fun getStuff(): Array<T> {
        return stuff
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        for (i in stuff.indices) {
            stuff[i]!!.unpack(buf)
        }
    }

    companion object {
        /** Statically usable method
         *
         * @param buf
         * @param stuff
         * @throws IOException
         */
        @Throws(IOException::class)
        fun unpack(buf: ByteBuffer?, stuff: Array<CacheableDoomObject>) {
            for (i in stuff.indices) {
                stuff[i].unpack(buf!!)
            }
        }
    }
}