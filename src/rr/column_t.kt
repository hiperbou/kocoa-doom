package rr

import utils.C2JUtils
import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer

/** column_t is a list of 0 or more post_t, (byte)-1 terminated
 * typedef post_t  column_t;
 * For the sake of efficiency, "column_t" will store raw data, however I added
 * some stuff to make my life easier.
 *
 */
class column_t : CacheableDoomObject {
    // MAES: there are useless, since the renderer is using raw byte data anyway, and the per-post
    // data is available in the special arrays.
    // public short        topdelta;   // -1 is the last post in a column (actually 0xFF, since this was unsigned???)
    // public short        length;     // length data bytes follows (actually add +2)
    //public column_t[]      posts;    // This is quite tricky to read.
    /** The RAW data (includes initial header and padding, because no post gets preferential treatment).  */
    lateinit var data: ByteArray

    /** Actual number of posts inside this column. All guesswork is done while loading  */
    var posts = 0

    /** Positions of posts inside the raw data (point at headers)  */
    lateinit var postofs: IntArray

    /** Posts lengths, intended as actual drawable pixels.  Add +4 to get the whole post length  */
    lateinit var postlen: ShortArray

    /** Vertical offset of each post. In theory it should be possible to quickly
     * clip to the next visible post when drawing a column  */
    lateinit var postdeltas: ShortArray
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        // Mark current position.
        buf.mark()
        var skipped = 0
        var postlen: Short = 0
        var colheight = 0
        var len = 0 // How long is the WHOLE column, until the final FF?
        var postno = 0 // Actual number of posts.
        var topdelta = 0
        var prevdelta = -1 // HACK for DeepSea tall patches.

        // Scan every byte until we encounter an 0xFF which definitively marks the end of a column.
        while (C2JUtils.toUnsignedByte(buf.get()).also { topdelta = it } != 0xFF) {

            // From the wiki:
            // A column's topdelta is compared to the previous column's topdelta 
            // (or to -1 if there is no previous column in this row). If the new 
            //  topdelta is lesser than the previous, it is interpreted as a tall
            // patch and the two values are added together, the sum serving as the 
            // current column's actual offset.
            val tmp = topdelta
            if (topdelta < prevdelta) {
                topdelta += prevdelta
            }
            prevdelta = tmp

            // First byte of a post should be its "topdelta"
            column_t.guesspostdeltas[postno] = topdelta.toShort()
            column_t.guesspostofs[postno] = skipped + 3 // 0 for first post

            // Read one more byte...this should be the post length.
            postlen = C2JUtils.toUnsignedByte(buf.get()).toShort()
            column_t.guesspostlens[postno++] = postlen

            // So, we already read 2 bytes (topdelta + length)
            // Two further bytes are padding so we can safely skip 2+2+postlen bytes until the next post
            skipped += 4 + postlen
            buf.position(buf.position() + 2 + postlen)

            // Obviously, this adds to the height of the column, which might not be equal to the patch that
            // contains it.
            colheight += postlen.toInt()
        }

        // Skip final padding byte ?
        skipped++
        len = finalizeStatus(skipped, colheight, postno)

        // Go back...and read the raw data. That's what will actually be used in the renderer.
        buf.reset()
        buf[data, 0, len]
    }

    /** This -almost- completes reading, by filling in the header information
     * before the raw column data is read in.
     *
     * @param skipped
     * @param colheight
     * @param postno
     * @return
     */
    private fun finalizeStatus(skipped: Int, colheight: Int, postno: Int): Int {
        val len: Int
        // That's the TOTAL length including all padding.
        // This means we redundantly read some data
        len = skipped
        data = ByteArray(len)
        postofs = IntArray(postno)
        postlen = ShortArray(postno)
        // this.length=(short) colheight;
        postdeltas = ShortArray(postno)
        System.arraycopy(column_t.guesspostofs, 0, postofs, 0, postno)
        System.arraycopy(column_t.guesspostlens, 0, postlen, 0, postno)
        System.arraycopy(column_t.guesspostdeltas, 0, postdeltas, 0, postno)
        posts = postno
        return len
    }

    /** based on raw data  */
    fun getTopDelta(): Int {
        return C2JUtils.toUnsignedByte(data[0])
    }

    /** based on raw data  */
    fun getLength(): Int {
        return C2JUtils.toUnsignedByte(data[1])
    }

    companion object {
        /** Static buffers used during I/O.
         * There's ABSO-FUCKING-LUTELY no reason to manipulate them externally!!!
         * I'M NOT KIDDING!!!11!!
         */
        private val guesspostofs = IntArray(256)
        private val guesspostlens = ShortArray(256)
        private val guesspostdeltas = ShortArray(256)
    }
}