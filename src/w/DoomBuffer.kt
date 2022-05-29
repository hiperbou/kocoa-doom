package w

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Very similar to the concept of ReadableDoomObjects
 * but made to work with byte buffers instead.
 *
 * This is normally NOT used to pass data around: I am
 * using it as a workaround to store raw byte buffers
 * into a "CacheableDoomObject" array, as Java
 * doesn't seem to like storing both ByteBuffers and
 * CacheableDoomObjects in the same array. WTF...
 *
 * @author admin
 */
class DoomBuffer : CacheableDoomObject {
    constructor() {}
    constructor(b: ByteBuffer?) {
        _buffer = b
    }

    var _buffer: ByteBuffer? = null
    @Throws(IOException::class)
    fun readShortArray(s: ShortArray?, len: Int) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i] = _buffer!!.short
        }
    }

    @Throws(IOException::class)
    fun readCharArray(s: CharArray?, len: Int) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i] = _buffer!!.char
        }
    }

    @Throws(IOException::class)
    fun readCharArray(s: IntArray?, len: Int) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i] = _buffer!!.char.code
        }
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        _buffer = buf
    }

    fun getBuffer(): ByteBuffer? {
        return _buffer
    }

    fun setOrder(bo: ByteOrder?) {
        _buffer!!.order(bo)
    }

    fun rewind() {
        _buffer!!.rewind()
    }

    companion object {
        @Throws(IOException::class)
        fun readObjectArray(buf: ByteBuffer?, s: Array<CacheableDoomObject>?, len: Int) {
            if (s == null || len == 0) {
                return
            }
            for (i in 0 until Math.min(len, s.size)) {
                s[i].unpack(buf!!)
            }
        }

        @Throws(IOException::class)
        fun readIntArray(buf: ByteBuffer, s: IntArray?, len: Int) {
            if (s == null || len == 0) {
                return
            }
            for (i in 0 until Math.min(len, s.size)) {
                s[i] = buf.int
            }
        }

        @Throws(IOException::class)
        fun putIntArray(buf: ByteBuffer, s: IntArray?, len: Int, bo: ByteOrder?) {
            buf.order(bo)
            if (s == null || len == 0) return
            for (i in 0 until Math.min(len, s.size)) {
                buf.putInt(s[i])
            }
        }

        @Throws(IOException::class)
        fun putBooleanIntArray(buf: ByteBuffer, s: BooleanArray?, len: Int, bo: ByteOrder?) {
            buf.order(bo)
            if (s == null || len == 0) return
            for (i in 0 until Math.min(len, s.size)) {
                buf.putInt(if (s[i]) 1 else 0)
            }
        }

        @Throws(IOException::class)
        fun putBooleanInt(buf: ByteBuffer, s: Boolean, bo: ByteOrder?) {
            buf.order(bo)
            buf.putInt(if (s) 1 else 0)
        }

        @Throws(IOException::class)
        fun readCharArray(buf: ByteBuffer, s: CharArray?, len: Int) {
            if (s == null || len == 0) return
            for (i in 0 until Math.min(len, s.size)) {
                s[i] = buf.char
            }
        }

        @Throws(IOException::class)
        fun readShortArray(buf: ByteBuffer, s: ShortArray?, len: Int) {
            if (s == null || len == 0) return
            for (i in 0 until Math.min(len, s.size)) {
                s[i] = buf.short
            }
        }

        /** Reads a length specified string from a buffer.  */
        @Throws(IOException::class)
        fun readString(buf: ByteBuffer): String? {
            val len = buf.int
            if (len == -1) return null
            if (len == 0) return ""
            val bb = ByteArray(len)
            buf[bb, 0, len]
            return String(bb, 0, len)
        }

        /** MAES: Reads a specified number of bytes from a buffer into a new String.
         * With many lengths being implicit, we need to actually take the loader by the hand.
         *
         * @param buf
         * @param len
         * @return
         * @throws IOException
         */
        @Throws(IOException::class)
        fun getString(buf: ByteBuffer, len: Int): String? {
            if (len == -1) return null
            if (len == 0) return ""
            val bb = ByteArray(len)
            buf[bb, 0, len]
            return String(bb, 0, len)
        }

        /** MAES: Reads a maximum specified number of bytes from a buffer into a new String,
         * considering the bytes as representing a null-terminated, C-style string.
         *
         * @param buf
         * @param len
         * @return
         * @throws IOException
         */
        @Throws(IOException::class)
        fun getNullTerminatedString(buf: ByteBuffer, len: Int): String? {
            var len = len
            if (len == -1) return null
            if (len == 0) return ""
            val bb = ByteArray(len)
            buf[bb, 0, len]
            // Detect null-termination.
            for (i in 0 until len) {
                if (bb[i].toInt() == 0x00) {
                    len = i
                    break
                }
            }
            return String(bb, 0, len)
        }

        /** MAES: Reads a specified number of bytes from a buffer into a new String.
         * With many lengths being implicit, we need to actually take the loader by the hand.
         *
         * @param buf
         * @param len
         * @return
         * @throws IOException
         */
        @Throws(IOException::class)
        fun getCharSeq(buf: ByteBuffer?, len: Int): CharArray {
            return DoomBuffer.getString(buf!!, len)!!.toCharArray()
        }

        fun getBEInt(b3: Byte, b2: Byte, b1: Byte, b0: Byte): Int {
            return b3.toInt() shl 24 or (b2.toInt() shl 16) or (b1.toInt() shl 8) or b0.toInt()
        }

        fun getBEInt(buf: ByteArray, offset: Int): Int {
            return buf[offset].toInt() shl 24 or (buf[offset + 1].toInt() shl 16) or (buf[offset + 2].toInt() shl 8) or buf[offset + 3].toInt()
        }

        fun getBEInt(buf: ByteArray): Int {
            return buf[0].toInt() shl 24 or (buf[1].toInt() shl 16) or (buf[2].toInt() shl 8) or buf[3].toInt()
        }

        fun getLEInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
            return b3.toInt() shl 24 or (b2.toInt() shl 16) or (b1.toInt() shl 8) or b0.toInt()
        }

        fun getLEInt(buf: ByteArray): Int {
            return buf[3].toInt() shl 24 or (buf[2].toInt() shl 16) or (buf[1].toInt() shl 24) or buf[0].toInt()
        }

        fun getBEShort(buf: ByteArray): Short {
            return (buf[0].toInt() shl 8 or buf[1].toInt()).toShort()
        }

        fun getLEShort(buf: ByteArray): Short {
            return (buf[0].toInt() shl 8 or buf[1].toInt()).toShort()
        }
    }
}