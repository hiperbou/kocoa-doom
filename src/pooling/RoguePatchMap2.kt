package pooling


import java.util.*

class RoguePatchMap2 {
    fun containsKey(lump: Int): Boolean {
        return indexOf(lump) >= 0
    }

    operator fun get(lump: Int): Array<ByteArray>? {
        val index = indexOf(lump)
        return if (index >= 0) {
            patches[index]
        } else {
            null
        }
    }

    fun put(lump: Int, patch: Array<ByteArray>) {
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

    private fun ensureCapacity(cap: Int) {
        while (lumps.size <= cap) {
            lumps = Arrays.copyOf(lumps, Math.max(lumps.size * 2, RoguePatchMap2.DEFAULT_CAPACITY))
        }
        while (patches.size <= cap) {
            patches = Arrays.copyOf(patches, Math.max(patches.size * 2, RoguePatchMap2.DEFAULT_CAPACITY))
        }
    }

    private fun indexOf(lump: Int): Int {
        return Arrays.binarySearch(lumps, 0, numEntries, lump)
    }

    private var lumps: IntArray
    private var numEntries = 0
    private var patches: Array<Array<ByteArray>?>

    init {
        lumps = IntArray(DEFAULT_CAPACITY)
        patches = arrayOfNulls(DEFAULT_CAPACITY)
    }

    companion object {
        private const val DEFAULT_CAPACITY = 16
    }
}