package pooling


import p.mobj_t
import java.util.*

/** A convenient object pooling class. Currently used for AudioChunks, but
 * could be reused for UI events and other such things. Perhaps reusing it
 * for mobj_t's is possible, but risky.
 *
 */
abstract class ObjectPool<K>(private val expirationTime: Long) {
    protected abstract fun create(): K
    abstract fun validate(obj: K?): Boolean
    abstract fun expire(obj: K?)
    @Synchronized
    fun checkOut(): K? {
        val now = System.currentTimeMillis()
        var t: K?
        if (unlocked.size > 0) {
            val e = unlocked.keys()
            // System.out.println((new StringBuilder("Pool size ")).append(unlocked.size()).toString());
            while (e.hasMoreElements()) {
                t = e.nextElement()
                if (now - unlocked[t]!!.toLong() > expirationTime) {
                    // object has expired
                    if (t is mobj_t) if (ObjectPool.D) System.out.printf("Object %s expired\n", t.toString())
                    unlocked.remove(t)
                    expire(t)
                    t = null
                } else {
                    if (validate(t)) {
                        unlocked.remove(t)
                        locked[t] = java.lang.Long.valueOf(now)
                        if (ObjectPool.D) if (t is mobj_t) System.out.printf(
                            "Object %s reused\n",
                            t.toString()
                        )
                        return t
                    }

                    // object failed validation
                    unlocked.remove(t)
                    expire(t)
                    t = null
                }
            }
        }
        t = create()
        locked[t] = java.lang.Long.valueOf(now)
        return t
    }

    @Synchronized
    fun checkIn(t: K) {
        if (ObjectPool.D) if (t is mobj_t) System.out.printf("Object %s returned to the pool\n", t.toString())
        locked.remove(t)
        unlocked[t] = java.lang.Long.valueOf(System.currentTimeMillis())
    }

    protected var locked: Hashtable<K?, Long>
    private val unlocked: Hashtable<K?, Long>

    init {
        locked = Hashtable()
        unlocked = Hashtable()
    }

    companion object {
        private const val D = false
    }
}