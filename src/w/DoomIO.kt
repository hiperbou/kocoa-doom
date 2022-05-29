package w

import m.Swap
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.nio.charset.Charset

/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
//Created on 24.07.2004 by RST.
//$Id: DoomIO.java,v 1.3 2013/06/03 10:30:20 velktron Exp $
/**
 * An extension of RandomAccessFile, which handles readString/WriteString specially
 * and offers several Doom related (and cross-OS) helper functions for reading/writing
 * arrays of multiple objects or fixed-length strings from/to disk.
 *
 * TO DEVELOPERS: this is the preferrered method of I/O for anything implemented.
 * In addition, Doomfiles can be passed to objects implementing the IReadableDoomObject
 * and IWritableDoomObject interfaces, which will "autoread" or "autowrite" themselves
 * to the implied stream.
 *
 * TODO: in the light of greater future portabililty and compatibility in certain
 * environments, PERHAPS this should have been implemented using Streams. Perhaps
 * it's possible to change the underlying implementation without (?) changing too
 * much of the exposed interface, but it's not a priority for me right now.
 *
 */
object DoomIO {
    /** Writes a Vector to a RandomAccessFile.  */
    @Throws(IOException::class)
    fun writeVector(dos: DataOutputStream, v: FloatArray) {
        for (n in 0..2) dos.writeFloat(v[n])
    }

    /** Writes a Vector to a RandomAccessFile.  */
    @Throws(IOException::class)
    fun readVector(dis: DataInputStream): FloatArray {
        val res = floatArrayOf(0f, 0f, 0f)
        for (n in 0..2) res[n] = dis.readFloat()
        return res
    }

    /** Reads a length specified string from a file.  */
    @Throws(IOException::class)
    fun readString(dis: DataInputStream): String? {
        val len = dis.readInt()
        if (len == -1) return null
        if (len == 0) return ""
        val bb = ByteArray(len)
        dis.read(bb, 0, len)
        return String(bb, 0, len, Charset.forName("ISO-8859-1"))
    }

    /** MAES: Reads a specified number of bytes from a file into a new String.
     * With many lengths being implicit, we need to actually take the loader by the hand.
     *
     * @param len
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readString(dis: DataInputStream, len: Int): String? {
        if (len == -1) return null
        if (len == 0) return ""
        val bb = ByteArray(len)
        dis.read(bb, 0, len)
        return String(bb, 0, len)
    }

    @Throws(IOException::class)
    fun readString(f: InputStream, len: Int): String? {
        if (len == -1) return null
        if (len == 0) return ""
        val bb = ByteArray(len)
        f.read(bb, 0, len)
        return String(bb, 0, len, Charset.forName("ISO-8859-1"))
    }

    /** MAES: Reads a specified number of bytes from a file into a new, NULL TERMINATED String.
     * With many lengths being implicit, we need to actually take the loader by the hand.
     *
     * @param len
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readNullTerminatedString(dis: InputStream, len: Int): String? {
        if (len == -1) return null
        if (len == 0) return ""
        val bb = ByteArray(len)
        var terminator = len
        dis.read(bb, 0, len)
        for (i in bb.indices) {
            if (bb[i].toInt() == 0) {
                terminator = i
                break // stop on first null
            }
        }

        // This is the One True Encoding for Doom.
        return String(bb, 0, terminator, Charset.forName("ISO-8859-1"))
    }

    /** MAES: Reads multiple strings with a specified number of bytes from a file.
     * If the array is not large enough, only partial reads will occur.
     *
     * @param len
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readMultipleFixedLengthStrings(
        dis: DataInputStream?,
        dest: Array<String?>,
        num: Int,
        len: Int
    ): Array<String?>? {

        // Some sanity checks...
        if (num <= 0 || len < 0) return null
        if (len == 0) {
            for (i in dest.indices) {
                dest[i] = ""
            }
            return dest
        }
        for (i in 0 until num) {
            dest[i] = DoomIO.readString(dis!!, len)
        }
        return dest
    }

    /** Writes a length specified string (Pascal style) to a file.
     *
     */
    fun writeString(dos: DataOutputStream, s: String?) {
        try {
            if (s == null) {
                dos.writeInt(-1)
                return
            }
            dos.writeInt(s.length)
            if (s.length != 0) dos.writeBytes(s)
        } catch (e: Exception) {
            System.err.println("writeString $s to DoomFile failed!")
        }
    }

    /** Writes a String with a specified len to a file.
     * This is useful for fixed-size String fields in
     * files. Any leftover space will be filled with 0x00s.
     *
     * @param s
     * @param len
     * @throws IOException
     */
    @Throws(IOException::class)
    fun writeString(dos: DataOutputStream, s: String?, len: Int) {
        if (s == null) return
        if (s.length != 0) {
            val dest = s.toByteArray(charset("ISO-8859-1"))
            dos.write(dest, 0, Math.min(len, dest.size))
            // Fill in with 0s if something's left.
            if (dest.size < len) {
                for (i in 0 until len - dest.size) {
                    dos.write(0x00.toByte().toInt())
                }
            }
        }
    }

    @Throws(IOException::class)
    fun readObjectArray(dis: DataInputStream?, s: Array<IReadableDoomObject>?, len: Int) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i].read(dis!!)
        }
    }

    @Throws(Exception::class)
    fun readObjectArrayWithReflection(dis: DataInputStream?, s: Array<IReadableDoomObject?>, len: Int) {
        if (len == 0) return
        val c = s.javaClass.componentType
        for (i in 0 until Math.min(len, s.size)) {
            if (s[i] == null) s[i] = c.newInstance() as IReadableDoomObject
            s[i]!!.read(dis!!)
        }
    }

    @Throws(Exception::class)
    fun readObjectArray(dis: DataInputStream?, s: Array<IReadableDoomObject?>?, len: Int, c: Class<*>) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            if (s[i] == null) {
                s[i] = c.newInstance() as IReadableDoomObject
            }
            s[i]!!.read(dis!!)
        }
    }

    @Throws(IOException::class)
    fun readIntArray(dis: DataInputStream, s: IntArray?, len: Int, bo: ByteOrder) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i] = dis.readInt()
            if (bo == ByteOrder.LITTLE_ENDIAN) {
                s[i] = Swap.LONG(s[i])
            }
        }
    }

    @Throws(IOException::class)
    fun readShortArray(dis: DataInputStream, s: ShortArray?, len: Int, bo: ByteOrder) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i] = dis.readShort()
            if (bo == ByteOrder.LITTLE_ENDIAN) {
                s[i] = Swap.SHORT(s[i])
            }
        }
    }

    @Throws(IOException::class)
    fun readIntArray(dis: DataInputStream?, s: IntArray, bo: ByteOrder?) {
        DoomIO.readIntArray(dis!!, s, s.size, bo!!)
    }

    @Throws(IOException::class)
    fun readShortArray(dis: DataInputStream?, s: ShortArray, bo: ByteOrder?) {
        DoomIO.readShortArray(dis!!, s, s.size, bo!!)
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun readBooleanArray(dis: DataInputStream, s: BooleanArray?, len: Int = s!!.size) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i] = dis.readBoolean()
        }
    }

    /** Reads an array of "int booleans" into an array or
     * proper booleans. 4 bytes per boolean are used!
     *
     * @param s
     * @param len
     * @throws IOException
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun readBooleanIntArray(dis: DataInputStream?, s: BooleanArray?, len: Int = s!!.size) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i] = DoomIO.readIntBoolean(dis!!)
        }
    }

    @Throws(IOException::class)
    fun writeBoolean(dos: DataOutputStream, s: BooleanArray?, len: Int) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            dos.writeBoolean(s[i])
        }
    }

    @Throws(IOException::class)
    fun writeObjectArray(dos: DataOutputStream?, s: Array<IWritableDoomObject>?, len: Int) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i].write(dos!!)
        }
    }

    @Throws(IOException::class)
    fun writeListOfObjects(dos: DataOutputStream?, s: List<IWritableDoomObject>?, len: Int) {
        if (s == null || len == 0) return
        for (i in 0 until Math.min(len, s.size)) {
            s[i].write(dos!!)
        }
    }

    @Throws(IOException::class)
    fun readIntBooleanArray(dis: DataInputStream?, s: BooleanArray) {
        DoomIO.readBooleanIntArray(dis, s, s.size)
    }

    @Throws(IOException::class)
    fun writeCharArray(dos: DataOutputStream, charr: CharArray?, len: Int) {
        if (charr == null || len == 0) return
        for (i in 0 until Math.min(len, charr.size)) {
            dos.writeChar(charr[i].code)
        }
    }

    /** Will read an array of proper Unicode chars.
     *
     * @param charr
     * @param len
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readCharArray(dis: DataInputStream, charr: CharArray?, len: Int) {
        if (charr == null || len == 0) return
        for (i in 0 until Math.min(len, charr.size)) {
            charr[i] = dis.readChar()
        }
    }

    /** Will read a bunch of non-unicode chars into a char array.
     * Useful when dealing with legacy text files.
     *
     * @param charr
     * @param len
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readNonUnicodeCharArray(dis: DataInputStream, charr: CharArray?, len: Int) {
        if (charr == null || len == 0) return
        for (i in 0 until Math.min(len, charr.size)) {
            charr[i] = dis.readUnsignedByte().toChar()
        }
    }
    /** Writes an item reference.
     * public void writeItem(gitem_t item) throws IOException {
     * if (item == null)
     * writeInt(-1);
     * else
     * writeInt(item.index);
     * }
     */
    /** Reads the item index and returns the game item.
     * public gitem_t readItem() throws IOException {
     * int ndx = readInt();
     * if (ndx == -1)
     * return null;
     * else
     * return GameItemList.itemlist[ndx];
     * }
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readUnsignedLEInt(dis: DataInputStream): Long {
        val tmp = dis.readInt()
        return 0xFFFFFFFFL and Swap.LONG(tmp).toLong()
    }

    @Throws(IOException::class)
    fun readLEInt(dis: DataInputStream): Int {
        val tmp = dis.readInt()
        return Swap.LONG(tmp)
    }

    @Throws(IOException::class)
    fun readLEInt(dis: InputStream): Int {
        val tmp = DataInputStream(dis).readInt()
        return Swap.LONG(tmp)
    }

    @Throws(IOException::class)
    fun writeLEInt(dos: DataOutputStream, value: Int) {
        dos.writeInt(Swap.LONG(value))
    }

    // 2-byte number
    fun SHORT_little_endian_TO_big_endian(i: Int): Int {
        return (i shr 8 and 0xff) + (i shl 8 and 0xff00)
    }

    // 4-byte number
    fun INT_little_endian_TO_big_endian(i: Int): Int {
        return (i and 0xff shl 24) + (i and 0xff00 shl 8) + (i and 0xff0000 shr 8) + (i shr 24 and 0xff)
    }

    @Throws(IOException::class)
    fun readLEShort(dis: DataInputStream): Short {
        val tmp = dis.readShort()
        return Swap.SHORT(tmp)
    }

    /** Reads a "big boolean" using 4 bytes.
     *
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readIntBoolean(dis: DataInputStream): Boolean {
        return dis.readInt() != 0
    }
}