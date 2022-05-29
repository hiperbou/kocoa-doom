package w

class name8(name: String) {
    private val s: ByteArray
    var x: IntArray
    var hash: Long

    init {
        s = ByteArray(9)
        x = IntArray(2)
        // in case the name was a full 8 chars
        s[8] = 0
        val tmp = name.toByteArray()
        System.arraycopy(tmp, 0, s, 0, Math.min(8, tmp.size))
        x[0] = name8.byteArrayToInt(s, 0)
        x[1] = name8.byteArrayToInt(s, 4)
        hash = name8.byteArrayToLong(s, 0)
    }

    companion object {
        var ss = ByteArray(9)

        /** Returns a 64-bit number that maps directly to the ASCII
         * 8-bit representation of a fixed-length 8 char string.
         * It's for all effects and purposes a unique 64-bit hash, and can be used to
         * speed up comparisons.
         *
         * @param name
         * @return
         */
        fun getLongHash(name: String): Long {
            // in case the name was a full 8 chars
            for (i in name8.ss.indices) {
                name8.ss[i] = 0
            }
            val tmp = name.toByteArray()
            // We must effectively limit hashes to 31 bits to be able to use them.
            System.arraycopy(tmp, 0, name8.ss, 0, Math.min(8, tmp.size))
            return name8.byteArrayToLong(name8.ss, 0)
        }

        fun getIntName(name: String): Int {
            // in case the name was a full 8 chars
            for (i in name8.ss.indices) {
                name8.ss[i] = 0
            }
            val tmp = name.toByteArray()
            System.arraycopy(tmp, 0, name8.ss, 0, Math.min(4, tmp.size))
            return name8.byteArrayToInt(name8.ss, 0)
        }

        fun byteArrayToInt(src: ByteArray, ofs: Int): Int {
            return src[ofs].toInt() shl 24 or (src[ofs + 1].toInt() shl 16) or (src[ofs + 2].toInt() shl 8) or src[ofs + 3].toInt()
        }

        fun byteArrayToLong(src: ByteArray?, ofs: Int): Long {
            return name8.byteArrayToInt(src!!, 0).toLong() shl 32 or name8.byteArrayToInt(src, 4)
                .toLong()
        }

        /** Probably has horrible performance...
         *
         * @param src
         * @param ofs
         * @return
         */
        fun stringToInt(src: String, ofs: Int): Int {
            val s = ByteArray(9)
            for (i in 0 until src.length) {
                s[i] = src[i].code.toByte()
            }
            return s[ofs].toInt() shl 24 or (s[ofs + 1].toInt() shl 16) or (s[ofs + 2].toInt() shl 8) or s[ofs + 3].toInt()
        }
    }
}