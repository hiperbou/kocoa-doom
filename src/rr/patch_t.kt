package rr

import rr.column_t
import utils.C2JUtils
import w.CacheableDoomObject
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

//Patches.
//A patch holds one or more columns.
//Patches are used for sprites and all masked pictures,
//and we compose textures from the TEXTURE1/2 lists
//of patches.
class patch_t : CacheableDoomObject {
    /** bounding box size  */
    var width: Short = 0
    var height: Short = 0

    /** pixels to the left of origin  */
    var leftoffset: Short = 0

    /** pixels below the origin  */
    var topoffset: Short = 0

    /** This used to be an implicit array pointing to raw posts of data.
     * TODO: get rid of it? It's never used
     * only [width] used the [0] is &columnofs[width]  */
    lateinit var columnofs: IntArray

    /** The ACTUAL data is here, nicely deserialized (well, almost)  */
    lateinit var columns: Array<column_t?>

    /** Added for debug aid purposes  */
    var name: String? = null

    /** Synthesizing constructor.
     * You have to provide the columns yourself, a-posteriori.
     *
     * @param name
     * @param width
     * @param height
     * @param leftoffset
     * @param topoffset
     */
    constructor(name: String?, width: Int, height: Int, leftoffset: Int, topoffset: Int) {
        this.name = name
        this.width = width.toShort()
        this.height = height.toShort()
        this.leftoffset = leftoffset.toShort()
        columns = arrayOfNulls(width)
    }

    constructor() {}
    /*  @Override
    public void read(DoomFile f) throws IOException{

        long pos=f.getFilePointer();
        this.width=f.readLEShort();
        this.height=f.readLEShort();
        this.leftoffset=f.readLEShort();
        this.topoffset=f.readLEShort();
        // As many columns as width...right???
        this.columnofs=new int[this.width];
        this.columns=new column_t[this.width];
        C2JUtils.initArrayOfObjects( this.columns, column_t.class);
        
        // Read the column offsets.
        f.readIntArray(this.columnofs, this.columnofs.length, ByteOrder.LITTLE_ENDIAN);
        for (int i=0;i<this.width;i++){
            // Go to offset.
            //f.seek(pos+this.columnofs[i]);
            this.columns[i].read(f);
        }
        
    }*/
    /** In the C code, reading is "aided", aka they know how long the header + all
     * posts/columns actually are on disk, and only "deserialize" them when using them.
     * Here, we strive to keep stuff as elegant and OO as possible, so each column will get
     * deserialized one by one. I thought about reading ALL column data as raw data, but
     * IMO that's shit in the C code, and would be utter shite here too. Ergo, I cleanly
     * separate columns at the patch level (an advantage is that it's now easy to address
     * individual columns). However, column data is still read "raw".
     */
    @Throws(IOException::class)
    override fun unpack(b: ByteBuffer) {
        // Remember to reset the ByteBuffer position each time.
        b.position(0)
        // In ByteBuffers, the order can be conveniently set beforehand :-o
        b.order(ByteOrder.LITTLE_ENDIAN)
        width = b.short
        height = b.short
        leftoffset = b.short
        topoffset = b.short
        // As many columns as width...right???
        columnofs = IntArray(width.toInt())
        columns = arrayOfNulls(width.toInt())
        C2JUtils.initArrayOfObjects(columns, column_t::class.java)

        // Compute the ACTUAL full-column sizes.
        val actualsizes = IntArray(columns.size)
        for (i in 0 until actualsizes.size - 1) {
            actualsizes[i] = columnofs[i + 1] - columnofs[i]
        }

        // The offsets.
        DoomBuffer.readIntArray(b, columnofs, columnofs.size)
        for (i in 0 until width) {
            // Go to offset.
            b.position(columnofs[i])
            try {
                columns[i]!!.unpack(b)
            } catch (e: Exception) {
                // Error during loading of column.
                // If first column (too bad..) set to special error column.
                if (i == 0) columns[i] = patch_t.getBadColumn(height.toInt()) else columns[i] = columns[i - 1]
            }
        }
    }

    companion object {
        // Special safeguard against badly computed columns. Now they can be any size.
        private val badColumns = Hashtable<Int, column_t>()
        private fun getBadColumn(size: Int): column_t {
            if (patch_t.badColumns.get(size) == null) {
                val tmp = column_t()
                tmp.data = ByteArray(size + 5)
                for (i in 3 until size + 3) {
                    tmp.data[i] = (i - 3).toByte()
                }
                tmp.data[size + 4] = 0xFF.toByte()
                tmp.posts = 1
                //tmp.length=(short) size;
                //tmp.topdelta=0;
                tmp.postofs = intArrayOf(3)
                tmp.postdeltas = shortArrayOf(0)
                tmp.postlen = shortArrayOf((size % 256).toShort())
                //tmp.setData();
                patch_t.badColumns.put(size, tmp)
            }
            return patch_t.badColumns.get(size)!!
        }
    }
}