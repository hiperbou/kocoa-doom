package w

import data.Defines
import data.mapvertex_t
import doom.SourceCode.W_Wad
import rr.patch_t
import utils.GenericCopy.ArraySupplier
import v.graphics.Lights
import v.graphics.Palettes
import v.tables.Playpal
import java.io.IOException
import java.nio.ByteBuffer
import java.util.function.IntFunction

interface IWadLoader {
    /**
     * W_Reload Flushes any of the reloadable lumps in memory and reloads the
     * directory.
     *
     * @throws Exception
     */
    @W_Wad.C(W_Wad.W_Reload)
    @Throws(Exception::class)
    fun Reload()

    /**
     * W_InitMultipleFiles
     *
     * Pass a null terminated list of files to use (actually
     * a String[] array in Java).
     *
     * All files are optional, but at least one file
     * must be found.
     *
     * Files with a .wad extension are idlink files
     * with multiple lumps.
     *
     * Other files are single lumps with the base filename
     * for the lump name.
     *
     * Lump names can appear multiple times.
     * The name searcher looks backwards, so a later file
     * does override all earlier ones.
     *
     * @param filenames
     */
    @W_Wad.C(W_Wad.W_InitMultipleFiles)
    @Throws(Exception::class)
    fun InitMultipleFiles(filenames: Array<String?>)

    /**
     * W_InitFile
     *
     * Just initialize from a single file.
     *
     * @param filename
     */
    @Throws(Exception::class)
    fun InitFile(filename: String?)

    /**
     * W_NumLumps
     *
     * Returns the total number of lumps loaded in this Wad manager. Awesome.
     *
     */
    fun NumLumps(): Int

    /**
     * Returns actual lumpinfo_t object for a given name. Useful if you want to
     * access something on a file, I guess?
     *
     * @param name
     * @return
     */
    fun GetLumpinfoForName(name: String): lumpinfo_t?

    /**
     * W_GetNumForName
     * Calls W_CheckNumForName, but bombs out if not found.
     */
    @W_Wad.C(W_Wad.W_GetNumForName)
    fun GetNumForName(name: String): Int

    /**
     *
     * @param lumpnum
     * @return
     */
    fun GetNameForNum(lumpnum: Int): String?

    //
    // W_LumpLength
    // Returns the buffer size needed to load the given lump.
    //
    @W_Wad.C(W_Wad.W_LumpLength)
    fun LumpLength(lump: Int): Int

    /**
     * W_CacheLumpNum Modified to read a lump as a specific type of
     * CacheableDoomObject. If the class is not identified or is null, then a
     * generic DoomBuffer object is left in the lump cache and returned.
     *
     * @param <T>
    </T> */
    @W_Wad.C(W_Wad.W_CacheLumpNum)
    fun <T> CacheLumpNum(
        lump: Int, tag: Int,
        what: Class<T>?
    ): T?

    // MAES 24/8/2011: superseded by auto-allocating version with proper 
    // container-based caching.
    @Deprecated("")
    @Throws(IOException::class)
    fun CacheLumpNumIntoArray(
        lump: Int, tag: Int,
        array: Array<mapvertex_t>, what: Class<*>?
    )

    /**
     * Return a cached lump based on its name, as raw bytes, no matter what.
     * It's rare, but has its uses.
     *
     * @param name
     * @param tag
     * @param what
     * @return
     */
    fun CacheLumpNameAsRawBytes(name: String, tag: Int): ByteArray

    /**
     * Return a cached lump based on its num, as raw bytes, no matter what.
     * It's rare, but has its uses.
     *
     * @param name
     * @param tag
     * @param what
     * @return
     */
    fun CacheLumpNumAsRawBytes(num: Int, tag: Int): ByteArray

    /**
     * Get a DoomBuffer of the specified lump name
     *
     * @param name
     * @param tag
     * @return
     */
    @W_Wad.C(W_Wad.W_CacheLumpName)
    fun CacheLumpName(name: String, tag: Int): DoomBuffer

    /**
     * Get a DoomBuffer of the specified lump num
     *
     * @param lump
     * @return
     */
    fun CacheLumpNumAsDoomBuffer(lump: Int): DoomBuffer

    /**
     * Specific method for loading cached patches by name, since it's by FAR the
     * most common operation.
     *
     * @param name
     * @return
     */
    fun CachePatchName(name: String): patch_t

    /**
     * Specific method for loading cached patches, since it's by FAR the most
     * common operation.
     *
     * @param name
     * @param tag
     * @return
     */
    fun CachePatchName(name: String, tag: Int): patch_t

    /**
     * Specific method for loading cached patches by number.
     *
     * @param num
     * @return
     */
    fun CachePatchNum(num: Int): patch_t

    @W_Wad.C(W_Wad.W_CacheLumpName)
    fun <T : CacheableDoomObject?> CacheLumpName(name: String, tag: Int, what: Class<T>?): T

    /**
     * A lump with size 0 is a marker. This means that it
     * can/must be skipped, and if we want actual data we must
     * read the next one.
     *
     * @param lump
     * @return
     */
    fun isLumpMarker(lump: Int): Boolean
    fun GetNameForLump(lump: Int): String?

    @W_Wad.C(W_Wad.W_CheckNumForName)
    fun CheckNumForName(name: String /* , int namespace */): Int

    /**
     * Return ALL possible results for a given name, in order to resolve name clashes without
     * using namespaces
     *
     * @param name
     * @return
     */
    fun CheckNumsForName(name: String): IntArray
    fun GetLumpInfo(i: Int): lumpinfo_t?

    /**
     * A way to cleanly close open file handles still pointed at by lumps.
     * Is also called upon finalize
     */
    fun CloseAllHandles()

    /**
     * Null the disk lump associated with a particular object,
     * if any. This will NOT induce a garbage collection, unless
     * you also null any references you have to that object.
     *
     * @param lump
     */
    fun UnlockLumpNum(lump: Int)
    fun UnlockLumpNum(lump: CacheableDoomObject?)
    fun <T : CacheableDoomObject> CacheLumpNumIntoArray(
        lump: Int,
        num: Int,
        what: ArraySupplier<T>,
        arrGen: IntFunction<Array<T>?>
    ): Array<T>

    /**
     * Verify whether a certain lump number is valid and has
     * the expected name.
     *
     * @param lump
     * @param lumpname
     * @return
     */
    fun verifyLumpName(lump: Int, lumpname: String): Boolean

    /**
     * The index of a known loaded wadfile
     *
     * @param wad1
     * @return
     */
    fun GetWadfileIndex(wad1: wadfile_info_t): Int

    /**
     * The number of loaded wadfile
     *
     * @return
     */
    fun GetNumWadfiles(): Int

    /**
     * Force a lump (in memory) to be equal to a dictated content. Useful
     * for when you are e.g. repairing palette lumps or doing other sanity
     * checks.
     *
     * @param lump
     * @param obj
     */
    fun InjectLumpNum(lump: Int, obj: CacheableDoomObject?)

    /**
     * Read a lump into a bunch of bytes straight. No caching, no frills.
     *
     * @param lump
     * @return
     */
    @W_Wad.C(W_Wad.W_ReadLump)
    fun ReadLump(lump: Int): ByteArray

    /**
     * Use your own buffer, of proper size of course.
     *
     * @param lump
     * @param buf
     */
    fun ReadLump(lump: Int, buf: ByteArray?)

    /**
     * Use your own buffer, of proper size AND offset.
     *
     * @param lump
     * @param buf
     */
    fun ReadLump(lump: Int, buf: ByteArray?, offset: Int)

    /**
     * Loads PLAYPAL from wad lump. Repairs if necessary.
     * Also, performs sanity check on *repaired* PLAYPAL.
     *
     * @return byte[] of presumably 256 colors, 3 bytes each
     */
    fun LoadPlaypal(): ByteArray? {
        // Copy over the one you read from disk...
        val pallump = GetNumForName("PLAYPAL")
        val playpal = Playpal.properPlaypal(CacheLumpNumAsRawBytes(pallump, Defines.PU_STATIC))
        val minLength: Int = Palettes.PAL_NUM_COLORS * Palettes.PAL_NUM_STRIDES
        require(playpal.size >= minLength) {
            String.format(
                "Invalid PLAYPAL: has %d entries instead of %d. Try -noplaypal mode",
                playpal.size, minLength
            )
        }
        print("VI_Init: set palettes.\n")
        println("Palette: " + playpal.size / Palettes.PAL_NUM_STRIDES + " colors")
        InjectLumpNum(pallump, DoomBuffer(ByteBuffer.wrap(playpal)))
        return playpal
    }

    /**
     * Loads COLORMAP from wad lump.
     * Performs sanity check on it.
     *
     * @return byte[][] of presumably 34 colormaps 256 entries each with an entry being index in PLAYPAL
     */
    fun LoadColormap(): Array<ByteArray> {
        // Load in the light tables,
        // 256 byte align tables.
        val lump = GetNumForName("COLORMAP")
        val length: Int = LumpLength(lump) + Palettes.PAL_NUM_COLORS
        val colormap =
            Array(length / Palettes.PAL_NUM_COLORS) { ByteArray(Palettes.PAL_NUM_COLORS) }
        val minLength: Int = Lights.COLORMAP_STD_LENGTH_15
        require(colormap.size >= minLength) {
            String.format(
                "Invalid COLORMAP: has %d entries, minimum is %d. Try -nocolormap mode",
                colormap.size, minLength
            )
        }
        print("VI_Init: set colormaps.\n")
        println("Colormaps: " + colormap.size)
        val tmp = ByteArray(length)
        ReadLump(lump, tmp)
        for (i in colormap.indices) {
            System.arraycopy(
                tmp,
                i * Palettes.PAL_NUM_COLORS,
                colormap[i],
                0,
                Palettes.PAL_NUM_COLORS
            )
        }
        return colormap
    }
}