package pooling


import p.mobj_t
import java.util.*

/** A convenient object pooling class, derived from the stock ObjectPool.
 *
 * It's about 50% faster than calling new, and MUCH faster than ObjectPool
 * because it doesn't do that bullshit object cleanup every so often.
 *
 */
abstract class ObjectQueuePool<K>(expirationTime: Long) {
    protected abstract fun create(): K
    abstract fun validate(obj: K): Boolean
    abstract fun expire(obj: K)
    fun drain() {
        locked.clear()
    }

    fun checkOut(): K {
        val t: K
        if (!locked.isEmpty()) {
            return locked.pop()
        }
        t = create()
        return t
    }

    fun checkIn(t: K) {
        if (ObjectQueuePool.D) if (t is mobj_t) System.out.printf(
            "Object %s returned to the pool\n",
            t.toString()
        )
        locked.push(t)
    }

    protected var locked: Stack<K> // private Hashtable<K,Long> unlocked;

    init {
        locked = Stack()
    }

    companion object {
        private const val D = false
    }
}