package pooling


import java.util.*

abstract class GenericIntMap<K> internal constructor() {
    fun containsKey(lump: Int): Boolean {
        return indexOf(lump) >= 0
    }

    operator fun get(lump: Int): K? {
        val index = indexOf(lump)
        return if (index >= 0) {
            patches[index]
        } else {
            null
        }
    }

    fun put(lump: Int, patch: K) {
        val index = indexOf(lump)
        if (index >= 0) {
            patches[index] = patch
        } else {
            ensureCapacity(numEntries + 1)
            val newIndex = index.inv()
            val moveCount = numEntries - newIndex
            if (moveCount > 0) {
                System.arraycopy(lumps, newIndex, lumps, newIndex + 1, moveCount)
                System.arraycopy(patches, newIndex, patches, newIndex + 1, moveCount)
            }
            lumps[newIndex] = lump
            patches[newIndex] = patch
            ++numEntries
        }
    }

    protected fun ensureCapacity(cap: Int) {
        while (lumps.size <= cap) {
            lumps = Arrays.copyOf(lumps, Math.max(lumps.size * 2, GenericIntMap.DEFAULT_CAPACITY))
        }
        while (patches.size <= cap) {
            patches = Arrays.copyOf(patches, Math.max(patches.size * 2, GenericIntMap.DEFAULT_CAPACITY))
        }
    }

    protected fun indexOf(lump: Int): Int {
        return Arrays.binarySearch(lumps, 0, numEntries, lump)
        //for (int i=0;i<numEntries;i++)
        //	if (lumps[i]==lump) return i;
        //
        //return -1;
    }

    protected var lumps: IntArray
    protected var numEntries = 0
    protected lateinit var patches: Array<K>

    /** Concrete implementations must allocate patches
     *
     */
    init {
        lumps = IntArray(GenericIntMap.DEFAULT_CAPACITY)
        // patches = new K[DEFAULT_CAPACITY];
    }

    companion object {
        const val DEFAULT_CAPACITY = 16
    }
}