package rr


import data.Defines
import doom.DoomMain
import doom.SourceCode.*
import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import p.AbstractLevelLoader
import rr.flat_t
import rr.patch_t
import w.DoomBuffer
import w.IWadLoader
import w.li_namespace
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/** An attempt to separate texture mapping functionality from
 * the rest of the rendering. Seems to work like a charm, and
 * it makes it clearer what needs and what doesn't need to be
 * exposed.
 *
 * @author Maes
 */
class SimpleTextureManager(var DM: DoomMain<*, *>) : TextureManager<ByteArray> {
    var W: IWadLoader
    var I: IDoomSystem
    var LL: AbstractLevelLoader

    //
    // Graphics.
    // DOOM graphics for walls and sprites
    // is stored in vertical runs of opaque pixels (posts).
    // A column is composed of zero or more posts,
    // a patch or sprite is composed of zero or more columns.
    // 
    protected var firstflat = 0
    protected var lastflat = 0
    protected var numflats = 0

    /** HACK  */
    protected lateinit var flats: Array<flat_t?>

    //protected int     firstpatch;
    //protected int     lastpatch;
    protected var numpatches = 0
    protected var numtextures = 0

    /** The unchached textures themselves, stored just as patch lists and various properties  */
    protected lateinit var textures: Array<texture_t?>

    /** Width per texture?  */
    protected lateinit var texturewidthmask: IntArray
    /** fixed_t[] needed for texture pegging  */
    /** How tall each composite texture is supposed to be  */
    protected lateinit var textureheight: IntArray

    /** How large each composite texture is supposed to be  */
    protected lateinit var texturecompositesize: IntArray

    /** Tells us which patch lump covers which column of which texture  */
    protected lateinit var texturecolumnlump: Array<ShortArray?>

    /** This is supposed to store indexes into a patch_t lump which point to the columns themselves
     * Instead, we're going to return indexes to columns inside a particular patch.
     * In the case of patches inside a non-cached multi-patch texture (e.g. those made of non-overlapping
     * patches), we're storing indexes INSIDE A PARTICULAR PATCH. E.g. for STARTAN1, which is made of two
     * 32-px wide patches, it should go something like 0, 1,2 ,3...31, 0,1,2,....31.
     *
     */
    protected lateinit var texturecolumnofs: Array<CharArray?>

    /** couple with texturecomposite  */
    protected var texturecoloffset = 0.toChar()
    //short[][]    texturecolumnindexes;
    /** Stores [textures][columns][data].  */
    protected lateinit var texturecomposite: Array<Array<ByteArray>?>

    /** HACK to store "composite masked textures", a Boomism.  */
    protected lateinit var patchcomposite: Array<patch_t?>

    /** for global animation. Storage stores actual lumps, translation is a relative -> relative map  */
    protected lateinit var flattranslation: IntArray
    protected lateinit var flatstorage: IntArray
    protected lateinit var texturetranslation: IntArray

    // This is also in DM, but one is enough, really.
    protected var skytexture = 0
    protected var skytexturemid = 0
    protected var skyflatnum = 0

    /** Hash table used for matching flat *lump* to flat *num*  */
    var FlatCache: Hashtable<Int, Int>? = null
    var FlatPatchCache: Hashtable<Int, patch_t>

    /**
     * R_CheckTextureNumForName Check whether texture is available. Filter out
     * NoTexture indicator. Can be sped up with a hash table, but it's pointless.
     */
    override fun CheckTextureNumForName(name: String): Int {
        val i: Int?
        // "NoTexture" marker.
        // "NoTexture" marker.
        if (name[0] == '-') return 0

        i = TextureCache!![name]
        return i ?: -1
    }

    /** Hash table used for fast texture lookup  */
    var TextureCache: Hashtable<String, Int>? = null

    /**
     * R_TextureNumForName
     * Calls R_CheckTextureNumForName,
     * aborts with error message.
     */
    override fun TextureNumForName(name: String): Int {
        val i: Int
        i = CheckTextureNumForName(name)
        if (i == -1) {
            I.Error("R_TextureNumForName: %s not found", name)
        }
        return i
    }

    /**
     * R_InitTextures
     * Initializes the texture list
     * with the textures from the world map.
     */
    @Throws(IOException::class)
    override fun InitTextures() {
        // This drives the rest
        val mtexture = maptexture_t()
        var texture: texture_t?
        var mpatch: Array<mappatch_t>
        var patch: Array<texpatch_t>
        val maptex = arrayOfNulls<ByteBuffer>(TextureManager.texturelumps.size)
        val patchlookup: IntArray
        var totalwidth: Int
        var offset: Int
        val maxoff = IntArray(TextureManager.texturelumps.size)
        val _numtextures = IntArray(TextureManager.texturelumps.size)
        var directory = 1
        var texset: Int = TextureManager.TEXTURE1
        // Load the patch names from pnames.lmp.
        //name[8] = 0;    
        patchlookup = loadPatchNames("PNAMES")

        // Load the map texture definitions from textures.lmp.
        // The data is contained in one or two lumps,
        //  TEXTURE1 for shareware, plus TEXTURE2 for commercial.
        for (i in TextureManager.texturelumps.indices) {
            val TEXTUREx: String = TextureManager.texturelumps.get(i)
            if (W.CheckNumForName(TEXTUREx) != -1) {
                maptex[i] = W.CacheLumpName(TEXTUREx, Defines.PU_STATIC).getBuffer()
                maptex[i]!!.rewind()
                maptex[i]!!.order(ByteOrder.LITTLE_ENDIAN)
                _numtextures[i] = maptex[i]!!.getInt()
                maxoff[i] = W.LumpLength(W.GetNumForName(TEXTUREx))
            }
        }

        // Total number of textures.
        numtextures = _numtextures[0] + _numtextures[1]
        textures = arrayOfNulls(numtextures)
        // MAES: Texture hashtable.          
        TextureCache = Hashtable(numtextures)
        texturecolumnlump = arrayOfNulls(numtextures)
        texturecolumnofs = arrayOfNulls(numtextures)
        patchcomposite = arrayOfNulls(numtextures)
        texturecomposite = arrayOfNulls<Array<ByteArray>?>(numtextures)
        texturecompositesize = IntArray(numtextures)
        texturewidthmask = IntArray(numtextures)
        textureheight = IntArray(numtextures)
        totalwidth = 0

        //  Really complex printing shit...
        print("[")
        run {
            var i = 0
            while (i < numtextures) {
                if (i and 63 == 0) print('.')
                if (i == _numtextures[TextureManager.TEXTURE1]) {
                    // Start looking in second texture file.
                    texset = TextureManager.TEXTURE2
                    directory = 1 // offset "1" inside maptex buffer
                    //System.err.print("Starting looking into TEXTURE2\n");
                }
                offset = maptex[texset]!!.getInt(directory shl 2)
                if (offset > maxoff[texset]) I.Error("R_InitTextures: bad texture directory")
                maptex[texset]!!.position(offset)
                // Read "maptexture", which is the on-disk form.
                mtexture.unpack(maptex[texset]!!)

                // MAES: the HashTable only needs to know the correct names.
                TextureCache!![mtexture.name!!.uppercase(Locale.getDefault())] = i

                // We don't need to manually copy trivial fields.
                textures[i] = texture_t()
                textures[i]!!.copyFromMapTexture(mtexture)
                texture = textures[i]

                // However we do need to correct the "patch.patch" field through the patchlookup
                mpatch = mtexture.patches
                patch = texture!!.patches as Array<texpatch_t>
                for (j in 0 until texture!!.patchcount) {
                    //System.err.printf("Texture %d name %s patch %d lookup %d\n",i,mtexture.name,j,mpatch[j].patch);
                    patch[j].patch = patchlookup[mpatch[j].patch.toInt()]
                    if (patch[j].patch == -1) {
                        I.Error(
                            "R_InitTextures: Missing patch in texture %s",
                            texture!!.name
                        )
                    }
                }

                // Columns and offsets of taxture = textures[i]
                texturecolumnlump[i] = ShortArray(texture!!.width.toInt())
                //C2JUtils.initArrayOfObjects( texturecolumnlump[i], column_t.class);
                texturecolumnofs[i] = CharArray(texture!!.width.toInt())
                var j = 1
                while (j * 2 <= texture!!.width) j = j shl 1
                texturewidthmask[i] = j - 1
                textureheight[i] = texture!!.height.toInt() shl FRACBITS
                totalwidth += texture!!.width.toInt()
                i++
                directory++
            }
        }

        // Precalculate whatever possible.  
        for (i in 0 until numtextures) GenerateLookup(i)

        // Create translation table for global animation.
        texturetranslation = IntArray(numtextures)
        for (i in 0 until numtextures) texturetranslation[i] = i
    }

    /** Assigns proper lumpnum to patch names. Check whether flats and patches of the same name coexist.
     * If yes, priority should go to patches. Otherwise, it's a "flats on walls" case.
     *
     * @param pnames
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun loadPatchNames(pnames: String): IntArray {
        val patchlookup: IntArray
        val nummappatches: Int
        var name: String
        val names = W.CacheLumpName(pnames, Defines.PU_STATIC).getBuffer()!!
        names.order(ByteOrder.LITTLE_ENDIAN)

        // Number of patches.
        names.rewind()
        nummappatches = names.int
        patchlookup = IntArray(nummappatches)
        for (i in 0 until nummappatches) {
            // Get a size limited string;
            name = DoomBuffer.getNullTerminatedString(names, 8)!!.uppercase(Locale.getDefault())

            // Resolve clashes
            val stuff = W.CheckNumsForName(name)

            // Move backwards.
            for (k in stuff.indices) {

                // Prefer non-flat, with priority
                if (W.GetLumpInfo(stuff[k])!!.namespace != li_namespace.ns_flats) {
                    patchlookup[i] = stuff[k]
                    break
                }

                // Suck it down :-/
                patchlookup[i] = stuff[k]
            }
        }
        return patchlookup
    }

    private fun retrievePatchSafe(lump: Int): patch_t? {

        // If this is a known troublesome lump, get it from the cache.
        if (FlatPatchCache.containsKey(lump)) {
            return FlatPatchCache[lump]
        }
        val info = W.GetLumpInfo(lump)!!
        val realpatch: patch_t

        // Patch is actually a flat or something equally nasty. Ouch.
        if (info.namespace == li_namespace.ns_flats) {
            val flat = W.CacheLumpNumAsRawBytes(lump, Defines.PU_CACHE)
            realpatch = MultiPatchSynthesizer.synthesizePatchFromFlat(info.name, flat, 64, 64)
            FlatPatchCache[lump] = realpatch
            W.UnlockLumpNum(lump)
        } else  // It's probably safe, at this point.
            realpatch = W.CacheLumpNum(lump, Defines.PU_CACHE, patch_t::class.java) as patch_t
        return realpatch
    }

    /**
     * R_GenerateLookup
     *
     * Creates the lookup tables for a given texture (aka, where inside the texture cache
     * is the offset for particular column... I think.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun GenerateLookup(texnum: Int) {
        val texture: texture_t?
        val patchcount: ShortArray //Keeps track of how many patches overlap a column.
        val patch: Array<texpatch_t>
        var realpatch: patch_t? = null
        var x: Int
        var x1: Int
        var x2: Int
        val collump: ShortArray?
        val colofs: CharArray?
        texture = textures[texnum]

        // Composited texture not created yet.
        texturecomposite[texnum] = null

        // We don't know ho large the texture will be, yet, but it will be a multiple of its height.
        texturecompositesize[texnum] = 0

        // This is the only place where those can be actually modified.
        // They are still null at this point.
        collump = texturecolumnlump[texnum]
        colofs = texturecolumnofs[texnum]

        /* Now count the number of columns  that are covered by more 
         * than one patch. Fill in the lump / offset, so columns
         * with only a single patch are all done.
         */patchcount = ShortArray(texture!!.width.toInt())
        patch = texture.patches as Array<texpatch_t>

        // for each patch in a texture...
        for (i in 0 until texture.patchcount) {
            // Retrieve patch...if it IS a patch.
            realpatch = retrievePatchSafe(patch[i].patch)
            x1 = patch[i].originx
            x2 = x1 + realpatch!!.width

            // Where does the patch start, inside the compositetexture?
            x = if (x1 < 0) 0 else x1

            // Correct, starts at originx. Where does it end?
            if (x2 > texture.width) x2 = texture.width.toInt()
            while (x < x2) {

                /* Obviously, if a patch starts at x it does cover the x-th column
             *  of a texture, even if transparent. 
             */patchcount[x]++
                // Column "x" of composite texture "texnum" is covered by this patch.
                collump!![x] = patch[i].patch.toShort()

                /* This is supposed to be a raw pointer to the beginning of the column
             * data, as it appears inside the PATCH.
             * 
             * Instead, we can return the actual column index (x-x1)
             * As an example, the second patch of STARTAN1 (width 64) starts
             * at column 32. Therefore colofs should be something like
             * 0,1,2,...,31,0,1,....31, indicating that the 32-th column of
             * STARTAN1 is the 0-th column of the patch that is assigned to that column
             * (the latter can be looked up in texturecolumnlump[texnum].
             * 
             * Any questions?
             * 
             */colofs!![x] = (x - x1).toChar()
                x++
            }
        } // end patch

        // Now check all columns again.
        x = 0
        while (x < texture.width) {

            // Can only occur if a column isn't covered by a patch at all, not even a transparent one.
            if (patchcount[x].toInt() == 0) {
                // TODO: somehow handle this. 
                System.err.print(realpatch!!.width.toInt())
                System.err.print(
                    """
    R_GenerateLookup: column without a patch (${texture.name})
    
    """.trimIndent()
                )
                //return;
            }
            // I_Error ("R_GenerateLookup: column without a patch");


            // Columns where more than one patch overlaps.
            if (patchcount[x] > 1) {
                // Use the cached block. This column won't be read from the wad system.
                collump!![x] = -1
                colofs!![x] = texturecompositesize[texnum].toChar()

                /* Do we really mind?
            if (texturecompositesize[texnum] > 0x10000-texture.height)
            {
            I.Error ("R_GenerateLookup: texture no %d (%s) is >64k",
                 texnum,textures[texnum].name);
            } */texturecompositesize[texnum] += texture.height.toInt()
            }
            x++
        }
    }

    /**
     * R_GenerateComposite
     * Using the texture definition, the composite texture is created
     * from the patches and each column is cached. This method is "lazy"
     * aka it's only called when a cached/composite texture is needed.
     *
     * @param texnum
     */
    override fun GenerateComposite(texnum: Int) {
        val block: Array<ByteArray>?
        val texture: texture_t?
        val patch: Array<texpatch_t>
        var realpatch: patch_t? = null
        var x: Int
        var x1: Int
        var x2: Int
        var patchcol: column_t
        val collump: ShortArray?
        val colofs: CharArray? // unsigned short
        // short[] colidxs; // unsigned short
        texture = textures[texnum]

        // BOth allocate the composite texture, and assign it to block.
        // texturecompositesize indicates a size in BYTES. We need a number of columns, though.
        // Now block is divided into columns. We need to allocate enough data for each column
        texturecomposite[texnum] = Array(texture!!.width.toInt()) { ByteArray(texture.height.toInt()) }
        block = texturecomposite[texnum]

        // Lump where a certain column will be read from (actually, a patch)
        collump = texturecolumnlump[texnum]

        // Offset of said column into the patch.
        colofs = texturecolumnofs[texnum]

        // colidxs = texturecolumnindexes[texnum];

        // Composite the columns together.
        patch = texture.patches as Array<texpatch_t>

        // For each patch in the texture...
        for (i in 0 until texture.patchcount) {
            // Retrieve patch...if it IS a patch.
            realpatch = retrievePatchSafe(patch[i].patch)
            x1 = patch[i].originx
            x2 = x1 + realpatch!!.width
            x = if (x1 < 0) 0 else x1
            if (x2 > texture.width) x2 = texture.width.toInt()
            while (x < x2) {

                // Column does not have multiple patches?
                if (collump!![x] >= 0) {
                    x++
                    continue
                }

                // patchcol = (column_t *)((byte *)realpatch
                //            + LONG(realpatch.columnofs[x-x1]));


                // We can look this up cleanly in Java. Ha!
                patchcol = realpatch.columns[x - x1]!!
                DrawColumnInCache(
                    patchcol,
                    block!![x], colofs!![x].code,
                    patch[i].originy,
                    texture.height.toInt()
                )
                x++
            }
        }
    }

    /**
     * R_GenerateMaskedComposite
     *
     * Generates a "masked composite texture": the result is a MASKED texture
     * (with see-thru holes), but this time  multiple patches can be used to
     * assemble it, unlike standard Doom where this is not allowed.
     *
     * Called only if a request for a texture in the general purpose GetColumn
     * method (used only for masked renders) turns out not to be pointing to a standard
     * cached texture, nor to a disk lump(which is the standard Doom way of indicating a
     * composite single patch texture) but to a cached one which, however, is composite.
     *
     * Confusing, huh?
     *
     * Normally, this results in a disaster, as the masked rendering methods
     * don't expect cached/composite textures at all, and you get all sorts of nasty
     * tutti frutti and medusa effects. Not anymore ;-)
     *
     * @param texnum
     */
    override fun GenerateMaskedComposite(texnum: Int) {
        val block: Array<ByteArray>
        val pixmap: Array<BooleanArray> // Solidity map
        val texture: texture_t?
        val patch: Array<texpatch_t>
        var realpatch: patch_t? = null
        var x: Int
        var x1: Int
        var x2: Int
        var patchcol: column_t
        val collump: ShortArray?
        val colofs: CharArray? // unsigned short
        texture = textures[texnum]

        // MAES: we don't want to save a solid block this time. Will only use
        // it for synthesis.
        block = Array(texture!!.width.toInt()) { ByteArray(texture.height.toInt()) }
        pixmap = Array(texture.width.toInt()) { BooleanArray(texture.height.toInt()) } // True values = solid

        // Lump where a certain column will be read from (actually, a patch)
        collump = texturecolumnlump[texnum]

        // Offset of said column into the patch.
        colofs = texturecolumnofs[texnum]

        // Composite the columns together.
        patch = texture.patches as Array<texpatch_t>

        // For each patch in the texture...
        for (i in 0 until texture.patchcount) {
            realpatch = W.CachePatchNum(patch[i].patch)
            x1 = patch[i].originx
            x2 = x1 + realpatch.width
            x = if (x1 < 0) 0 else x1
            if (x2 > texture.width) x2 = texture.width.toInt()
            while (x < x2) {

                // Column does not have multiple patches?
                if (collump!![x] >= 0) {
                    x++
                    continue
                }

                // patchcol = (column_t *)((byte *)realpatch
                // + LONG(realpatch.columnofs[x-x1]));

                // We can look this up cleanly in Java. Ha!
                patchcol = realpatch.columns[x - x1]!!
                DrawColumnInCache(
                    patchcol, block[x], pixmap[x], colofs!![x].code,
                    patch[i].originy, texture.height.toInt()
                )
                x++
            }
        }

        // Patch drawn on cache, synthesize patch_t using it. 
        patchcomposite[texnum] = MultiPatchSynthesizer.synthesize(
            CheckTextureNameForNum(texnum),
            block,
            pixmap,
            texture.width.toInt(),
            texture.height.toInt()
        )
    }

    /**
     * R_DrawColumnInCache
     * Clip and draw a column from a patch into a cached post.
     *
     * This means that columns are effectively "uncompressed" into cache, here,
     * and that composite textures are generally uncompressed...right?
     *
     * Actually: "compressed" or "masked" textures are retrieved in the same way.
     * There are both "masked" and "unmasked" drawing methods. If a masked
     * column is passed to a method that expects a full, dense column...well,
     * it will look fugly/overflow/crash. Vanilla Doom tolerated this,
     * we're probably going to have more problems.
     *
     * @param patch Actually it's a single column to be drawn. May overdraw existing ones or void space.
     * @param cache the column cache itself. Actually it's the third level [texture][column]->data.
     * @param offset an offset inside the column cache (UNUSED)
     * @param originy vertical offset. Caution with masked stuff!
     * @param cacheheight the maximum height it's supposed to reach when drawing?
     */
    fun DrawColumnInCache(
        patch: column_t, cache: ByteArray?, offset: Int,
        originy: Int, cacheheight: Int
    ) {
        var count: Int
        var position: Int
        var source = 0 // treat as pointers

        /*
         * Iterate inside column. This is starkly different from the C code,
         * because post positions AND offsets are already precomputed at load
         * time
         */for (i in 0 until patch.posts) {

            // This should position us at the beginning of the next post
            source = patch.postofs[i]
            count = patch.postlen[i].toInt() // length of this particular post
            position = originy + patch.postdeltas[i] // Position to draw inside
            // cache.

            // Post starts outside of texture's bounds. Adjust offset.
            if (position < 0) {
                count += position // Consider that we have a "drawing debt".
                position = 0
            }

            // Post will go too far outside.
            if (position + count > cacheheight) count = cacheheight - position
            if (count > 0) // Draw this post. Won't draw posts that start
            // "outside"
            // Will start at post's start, but will only draw enough pixels
            // not to overdraw.
                System.arraycopy(patch.data, source, cache, position, count)
        }
    }

    // Version also drawing on a supplied transparency map
    fun DrawColumnInCache(
        patch: column_t, cache: ByteArray?,
        pixmap: BooleanArray?, offset: Int, originy: Int, cacheheight: Int
    ) {
        var count: Int
        var position: Int
        var source = 0 // treat as pointers

        /*
         * Iterate inside column. This is starkly different from the C code,
         * because post positions AND offsets are already precomputed at load
         * time
         */for (i in 0 until patch.posts) {

            // This should position us at the beginning of the next post
            source = patch.postofs[i]
            count = patch.postlen[i].toInt() // length of this particular post
            position = originy + patch.postdeltas[i] // Position to draw inside
            // cache.

            // Post starts outside of texture's bounds. Adjust offset.
            if (position < 0) {
                count += position // Consider that we have a "drawing debt".
                position = 0
            }

            // Post will go too far outside.
            if (position + count > cacheheight) count = cacheheight - position
            if (count > 0) {
                // Draw post, AND fill solidity map
                System.arraycopy(patch.data, source, cache, position, count)
                Arrays.fill(pixmap, position, position + count, true)
            }
            // Repeat for next post(s), if any.
        }
    }

    /**
     * R_InitFlats
     *
     * Scans WADs for F_START/F_END lumps, and also any additional
     * F1_ and F2_ pairs.
     *
     * Correct behavior would be to detect F_START/F_END lumps,
     * and skip any marker lumps sandwiched in between. If F_START and F_END are external,
     * use external override.
     *
     * Also, in the presence of external FF_START lumps, merge their contents
     * with those previously read.
     *
     * The method is COMPATIBLE with resource pre-coalesing, however it's not
     * trivial to change back to the naive code because of the "translationless"
     * system used (all flats are assumed to lie in a linear space). This
     * speeds up lookups.
     *
     */
    override fun InitFlats() {
        numflats = 0
        var extendedflatstart = -1
        firstflat = W.GetNumForName(SimpleTextureManager.LUMPSTART) // This is the start of normal lumps.
        if (FlatCache == null) FlatCache = Hashtable() else FlatCache!!.clear()
        val FlatNames = Hashtable<String?, Int>() // Store names here.

        // Normally, if we don't use Boom features, we could look for F_END and that's it.
        // However we want to avoid using flat translation and instead store absolute lump numbers.
        // So we need to do a clean parse.

        // The rule is: we scan from the very first F_START to the very first F_END.
        // We discard markers, and only assign sequential numbers to valid lumps.
        // These are the vanilla flats, and will work with fully merged PWADs too.

        // Normally, this marks the end of regular lumps. However, if DEUTEX extension
        // are present, it will actually mark the end of the extensions due to lump
        // priority, so its usefulness as an absolute end-index for regular flats
        // is dodgy at best. Gotta love the inconsistent mundo hacks!

        //int lastflatlump=W.GetNumForName(LUMPEND);

        // 
        var lump = firstflat
        var seq = 0
        var name: String?
        while (!W.GetNameForNum(lump).also { name = it }
                .equals(SimpleTextureManager.LUMPEND, ignoreCase = true)) {
            if (!W.isLumpMarker(lump)) {
                // Not a marker. Put in cache.
                FlatCache!![lump] = seq
                // Save its name too.
                FlatNames[name] = lump
                seq++ // Advance sequence
                numflats++ // Total flats do increase
            }
            lump++ // Advance lump.
        }
        extendedflatstart =
            W.CheckNumForName(SimpleTextureManager.DEUTEX_START) // This is the start of DEUTEX flats.
        if (extendedflatstart > -1) {
            // If extended ones are present, then Advance slowly.
            lump = extendedflatstart

            // Safeguard: FF_START without corresponding F_END (e.g. in helltest.wad)
            name = W.GetNameForNum(lump)

            // The end of those extended flats is also marked by F_END or FF_END, as noted above.
            // It can also be non-existent in some broken maps like helltest.wad. Jesus.
            while (!(name == null || name.equals(
                    SimpleTextureManager.LUMPEND,
                    ignoreCase = true
                ) || name.equals(SimpleTextureManager.DEUTEX_END, ignoreCase = true))
            ) {
                if (!W.isLumpMarker(lump)) {
                    // Not a marker. Check if it's supposed to replace something.
                    if (FlatNames.containsKey(name)) {
                        // Well, it is. Off with its name, save the lump number though.
                        val removed = FlatNames.remove(name)!!
                        // Put new name in list
                        FlatNames[name] = lump
                        // Remove old lump, but keep sequence.
                        val oldseq = FlatCache!!.remove(removed)!!
                        // Put new lump number with old sequence. 
                        FlatCache!![lump] = oldseq
                    } else {  // Add normally
                        FlatCache!![lump] = seq
                        // Save its name too.
                        FlatNames[name] = lump
                        seq++ // Advance sequence
                        numflats++ // Total flats do increase
                    }
                }
                lump++ // Advance lump.
                name = W.GetNameForNum(lump)
            }
        }

        // So now we have a lump -> sequence number mapping.

        // Create translation table for global animation.
        flattranslation = IntArray(numflats)
        flatstorage = IntArray(numflats)

        // MAJOR CHANGE: flattranslation stores absolute lump numbers. Adding
        // firstlump is not necessary anymore.      
        // Now, we're pretty sure that we have a progressive value mapping.
        val stuff = FlatCache!!.keys()
        while (stuff.hasMoreElements()) {
            val nextlump = stuff.nextElement()
            flatstorage[FlatCache!![nextlump]!!] = nextlump
            // Lump is used as the key, while the relative lump number is the value.
            //FlatCache.put(j, k-1);
        }
        for (i in 0 until numflats) {
            flattranslation[i] = i
            //  System.out.printf("Verification: flat[%d] is %s in lump %d\n",i,W.GetNameForNum(flattranslation[i]),flatstorage[i]);  
        }
    }

    /**
     * R_PrecacheLevel
     * Preloads all relevant graphics for the level.
     *
     * MAES: Everything except sprites.
     * A Texturemanager != sprite manager.
     * So the relevant functionality has been split into
     * PrecacheThinkers (found in common rendering code).
     *
     *
     */
    var flatmemory = 0
    var texturememory = 0
    @Suspicious(CauseOfDesyncProbability.LOW)
    @R_Data.C(R_Data.R_PrecacheLevel)
    @Throws(IOException::class)
    override fun PrecacheLevel() {
        preCacheFlats()
        preCacheTextures()

        // recache sprites.
        /* MAES: this code into PrecacheThinkers
        spritepresent = new boolean[numsprites];
        
        
        for (th = P.thinkercap.next ; th != P.thinkercap ; th=th.next)
        {
        if (th.function==think_t.P_MobjThinker)
            spritepresent[((mobj_t )th).sprite.ordinal()] = true;
        }
        
        spritememory = 0;
        for (i=0 ; i<numsprites ; i++)
        {
        if (!spritepresent[i])
            continue;

        for (j=0 ; j<sprites[i].numframes ; j++)
        {
            sf = sprites[i].spriteframes[j];
            for (k=0 ; k<8 ; k++)
            {
            lump = firstspritelump + sf.lump[k];
            spritememory += W.lumpinfo[lump].size;
            W.CacheLumpNum(lump , PU_CACHE,patch_t.class);
            }
        }
        }
         */
    }

    protected fun preCacheFlats() {
        val flatpresent: BooleanArray
        var lump: Int
        if (DM.demoplayback) return

        // Precache flats.
        flatpresent = BooleanArray(numflats)
        flats = arrayOfNulls(numflats)
        for (i in 0 until LL.numsectors) {
            flatpresent[LL.sectors[i].floorpic.toInt()] = true
            flatpresent[LL.sectors[i].ceilingpic.toInt()] = true
        }
        flatmemory = 0
        for (i in 0 until numflats) {
            if (flatpresent[i]) {
                lump = firstflat + i
                flatmemory += W.GetLumpInfo(lump)!!.size.toInt()
                flats[i] = W.CacheLumpNum(lump, Defines.PU_CACHE, flat_t::class.java) as flat_t
            }
        }
    }

    protected fun preCacheTextures() {
        val texturepresent: BooleanArray
        var texture: texture_t?
        var lump: Int


        // Precache textures.
        texturepresent = BooleanArray(numtextures)
        for (i in 0 until LL.numsides) {
            texturepresent[LL.sides[i].toptexture.toInt()] = true
            texturepresent[LL.sides[i].midtexture.toInt()] = true
            texturepresent[LL.sides[i].bottomtexture.toInt()] = true
        }

        // Sky texture is always present.
        // Note that F_SKY1 is the name used to
        //  indicate a sky floor/ceiling as a flat,
        //  while the sky texture is stored like
        //  a wall texture, with an episode dependend
        //  name.
        texturepresent[skytexture] = true
        texturememory = 0
        for (i in 0 until numtextures) {
            if (!texturepresent[i]) continue
            texture = textures[i]
            for (j in 0 until texture!!.patchcount) {
                lump = texture.patches[j]!!.patch
                texturememory += W.GetLumpInfo(lump)!!.size.toInt()
                W.CacheLumpNum(lump, Defines.PU_CACHE, patch_t::class.java)
            }
        }
    }

    /**
     * R_FlatNumForName
     * Retrieval, get a flat number for a flat name.
     *
     * Unlike the texture one, this one is not used frequently. Go figure.
     */
    override fun FlatNumForName(name: String): Int {
        val i: Int

        //System.out.println("Checking for "+name);
        i = W.CheckNumForName(name)

        //System.out.printf("R_FlatNumForName retrieved lump %d for name %s picnum %d\n",i,name,FlatCache.get(i));
        if (i == -1) {
            I.Error("R_FlatNumForName: %s not found", name)
        }
        return FlatCache!![i]!!
    }

    override fun getTextureColumnLump(tex: Int, col: Int): Int {
        return texturecolumnlump[tex]!![col].toInt()
    }

    override fun getTextureColumnOfs(tex: Int, col: Int): Char {
        return texturecolumnofs[tex]!![col]
    }

    override fun getTexturewidthmask(tex: Int): Int {
        return texturewidthmask[tex]
    }

    override fun getTextureComposite(tex: Int): Array<ByteArray>? { //TODO: this should be a boolean result meaning texture exists
        return texturecomposite[tex]
    }

    override fun getTextureComposite(tex: Int, col: Int): ByteArray {
        return texturecomposite[tex]!![col]
    }

    override fun getMaskedComposite(tex: Int): patch_t? {
        return patchcomposite[tex]
    }

    override fun getTextureheight(texnum: Int): Int {
        return textureheight[texnum]
    }

    override fun getTextureTranslation(texnum: Int): Int {
        return texturetranslation[texnum]
    }

    /** Returns a flat after it has been modified by the translation table e.g. by animations  */
    override fun getFlatTranslation(flatnum: Int): Int {
        return flatstorage[flattranslation[flatnum]]
    }

    override fun setTextureTranslation(texnum: Int, amount: Int) {
        texturetranslation[texnum] = amount
    }

    /** This affects ONLY THE TRANSLATION TABLE, not the lump storage.
     *
     */
    override fun setFlatTranslation(flatnum: Int, amount: Int) {
        flattranslation[flatnum] = amount
    }
    //////////////////////////////////From r_sky.c /////////////////////////////////////
    //////////////////////////////////From r_sky.c /////////////////////////////////////
    /**
     * R_InitSkyMap
     * Called whenever the view size changes.
     */
    override fun InitSkyMap(): Int {
        skyflatnum = FlatNumForName(Defines.SKYFLATNAME)
        skytexturemid = 100 * FRACUNIT
        return skyflatnum
    }

    override fun getSkyFlatNum(): Int {
        return skyflatnum
    }

    override fun setSkyFlatNum(skyflatnum: Int) {
        this.skyflatnum = skyflatnum
    }

    override fun getSkyTexture(): Int {
        return skytexture
    }

    override fun setSkyTexture(skytexture: Int) {
        this.skytexture = skytexture
    }

    /*@Override
    public int getFirstFlat() {
        return firstflat;
    } */
    override fun getSkyTextureMid(): Int {
        return skytexturemid
    }

    override fun CheckTextureNameForNum(texnum: Int): String? {
        return textures[texnum]!!.name
    }

    override fun getFlatLumpNum(flatnum: Int): Int {
        // TODO Auto-generated method stub
        return 0
    }

    /** Generates a "cached" masked column against a black background.
     * Synchronized so concurrency issues won't cause random glitching and
     * errors.
     *
     * @param lump
     * @param column
     * @return raw, 0-pointed column data.
     */
    @Synchronized
    override fun getRogueColumn(lump: Int, column: Int): ByteArray {

        // If previously requested, speed up gathering.
        //if (lastrogue==lump)
        //	return rogue[column];

        // Not contained? Generate.
        if (!roguePatches.containsKey(lump)) roguePatches[lump] = generateRoguePatch(lump)
        lastrogue = lump
        rogue = roguePatches[lump]!!
        return rogue[column]
    }

    /** Actually generates a tutti-frutti-safe cached patch out of
     * a masked or unmasked single-patch lump.
     *
     * @param lump
     * @return
     */
    private fun generateRoguePatch(lump: Int): Array<ByteArray> {
        // Retrieve patch...if it IS a patch.
        val p = retrievePatchSafe(lump)

        // Allocate space for a cached block.
        val block = Array(p!!.width.toInt()) {
            ByteArray(
                p.height.toInt()
            )
        }
        for (i in 0 until p.width) DrawColumnInCache(p.columns[i]!!, block[i], i, 0, p.height.toInt())

        // Don't keep this twice in memory.
        W.UnlockLumpNum(lump)
        return block
    }

    var lastrogue = -1
    lateinit var rogue: Array<ByteArray>
    var roguePatches = HashMap<Int, Array<ByteArray>>()

    internal inner class TextureDirectoryEntry : Comparable<TextureDirectoryEntry> {
        /** Where an entry starts within the TEXTUREx lump  */
        var offset = 0

        /** Its implicit position as indicated by the directory's ordering  */
        var entry = 0

        /** Its MAXIMUM possible length, depending on what follows it.
         * Not trivial to compute without thoroughtly examining the entire lump  */
        var length = 0

        /** Entries are ranked according to actual offset  */
        override fun compareTo(o: TextureDirectoryEntry): Int {
            if (offset < o.offset) return -1
            return if (offset == o.offset) 0 else 1
        }
    }

    override fun getSafeFlat(flatnum: Int): ByteArray {
        val flat = (W.CacheLumpNum(
            getFlatTranslation(flatnum),
            Defines.PU_STATIC, flat_t::class.java
        ) as flat_t).data
        if (flat.size < 4096) {
            System.arraycopy(flat, 0, safepatch, 0, flat.size)
            return safepatch
        }
        return flat
    }

    private val safepatch = ByteArray(4096)
    // COLUMN GETTING METHODS. No idea why those had to be in the renderer...
    /**
     * Special version of GetColumn meant to be called concurrently by different
     * (MASKED) seg rendering threads, identfiex by index. This serves to avoid stomping
     * on mutual cached textures and causing crashes.
     *
     * Returns column_t, so in theory it could be made data-agnostic.
     *
     */
    override fun GetSmpColumn(tex: Int, col: Int, id: Int): column_t? {
        var col = col
        val lump: Int
        val ofs: Int
        col = col and getTexturewidthmask(tex)
        lump = getTextureColumnLump(tex, col)
        ofs = getTextureColumnOfs(tex, col).code

        // It's always 0 for this kind of access.

        // Speed-increasing trick: speed up repeated accesses to the same
        // texture or patch, if they come from the same lump
        if (tex == smp_lasttex[id] && lump == smp_lastlump[id]) {
            return if (composite) smp_lastpatch[id]!!.columns[col] else smp_lastpatch[id]!!.columns[ofs]
        }

        // If pointing inside a non-zero, positive lump, then it's not a
        // composite texture. Read it from disk.
        if (lump > 0) {
            // This will actually return a pointer to a patch's columns.
            // That is, to the ONE column exactly.{
            // If the caller needs access to a raw column, we must point 3 bytes
            // "ahead".
            smp_lastpatch[id] = W.CachePatchNum(lump)
            smp_lasttex[id] = tex
            smp_lastlump[id] = lump
            smp_composite[id] = false
            // If the column was a disk lump, use ofs.
            return smp_lastpatch[id]!!.columns[ofs]
        }

        // Problem. Composite texture requested as if it was masked
        // but it doesn't yet exist. Create it.
        if (getMaskedComposite(tex) == null) {
            System.err.printf(
                "Forced generation of composite %s\n",
                CheckTextureNameForNum(tex),
                smp_composite[id],
                col,
                ofs
            )
            GenerateMaskedComposite(tex)
            System.err.printf(
                "Composite patch %s %d\n",
                getMaskedComposite(tex)!!.name,
                getMaskedComposite(tex)!!.columns.size
            )
        }

        // Last resort. 
        smp_lastpatch[id] = getMaskedComposite(tex)
        smp_lasttex[id] = tex
        smp_composite[id] = true
        smp_lastlump[id] = 0
        return lastpatch!!.columns[col]
    }

    // False: disk-mirrored patch. True: improper "transparent composite".
    protected lateinit var smp_composite // = false;
            : BooleanArray
    protected lateinit var smp_lasttex // = -1;
            : IntArray
    protected lateinit var smp_lastlump // = -1;
            : IntArray
    protected lateinit var smp_lastpatch // = null;
            : Array<patch_t?>
    ///////////////////////// TEXTURE MANAGEMENT /////////////////////////
    /**
     * R_GetColumn original version: returns raw pointers to byte-based column
     * data. Works for both masked and unmasked columns, but is not
     * tutti-frutti-safe.
     *
     * Use GetCachedColumn instead, if rendering non-masked stuff, which is also
     * faster.
     *
     * @throws IOException
     */
    override fun GetColumn(tex: Int, col: Int): ByteArray {
        var col = col
        val lump: Int
        val ofs: Int
        col = col and getTexturewidthmask(tex)
        lump = getTextureColumnLump(tex, col)
        ofs = getTextureColumnOfs(tex, col).code

        // It's always 0 for this kind of access.

        // Speed-increasing trick: speed up repeated accesses to the same
        // texture or patch, if they come from the same lump
        if (tex == lasttex && lump == lastlump) {
            return if (composite) lastpatch!!.columns[col]!!.data else lastpatch!!.columns[ofs]!!.data
        }

        // If pointing inside a non-zero, positive lump, then it's not a
        // composite texture. Read it from disk.
        if (lump > 0) {
            // This will actually return a pointer to a patch's columns.
            // That is, to the ONE column exactly.{
            // If the caller needs access to a raw column, we must point 3 bytes
            // "ahead".
            lastpatch = W.CachePatchNum(lump)
            lasttex = tex
            lastlump = lump
            composite = false
            // If the column was a disk lump, use ofs.
            return lastpatch!!.columns[ofs]!!.data
        }

        // Problem. Composite texture requested as if it was masked
        // but it doesn't yet exist. Create it.
        if (getMaskedComposite(tex) == null) {
            System.err.printf("Forced generation of composite %s\n", CheckTextureNameForNum(tex), composite, col, ofs)
            GenerateMaskedComposite(tex)
            System.err.printf(
                "Composite patch %s %d\n",
                getMaskedComposite(tex)!!.name,
                getMaskedComposite(tex)!!.columns.size
            )
        }

        // Last resort. 
        lastpatch = getMaskedComposite(tex)
        lasttex = tex
        composite = true
        lastlump = 0
        return lastpatch!!.columns[col]!!.data
    }

    /**
     * R_GetColumnStruct: returns actual pointers to columns.
     * Agnostic of the underlying type.
     *
     * Works for both masked and unmasked columns, but is not
     * tutti-frutti-safe.
     *
     * Use GetCachedColumn instead, if rendering non-masked stuff, which is also
     * faster.
     *
     * @throws IOException
     */
    override fun GetColumnStruct(tex: Int, col: Int): column_t? {
        var col = col
        val lump: Int
        val ofs: Int
        col = col and getTexturewidthmask(tex)
        lump = getTextureColumnLump(tex, col)
        ofs = getTextureColumnOfs(tex, col).code

        // Speed-increasing trick: speed up repeated accesses to the same
        // texture or patch, if they come from the same lump
        if (tex == lasttex && lump == lastlump) {
            return if (composite) lastpatch!!.columns[col] else lastpatch!!.columns[ofs]
        }

        // If pointing inside a non-zero, positive lump, then it's not a
        // composite texture. Read it from disk.
        if (lump > 0) {
            // This will actually return a pointer to a patch's columns.
            // That is, to the ONE column exactly.{
            // If the caller needs access to a raw column, we must point 3 bytes
            // "ahead".
            lastpatch = W.CachePatchNum(lump)
            lasttex = tex
            lastlump = lump
            composite = false
            // If the column was a disk lump, use ofs.
            return lastpatch!!.columns[ofs]
        }

        // Problem. Composite texture requested as if it was masked
        // but it doesn't yet exist. Create it.
        if (getMaskedComposite(tex) == null) {
            System.err.printf("Forced generation of composite %s\n", CheckTextureNameForNum(tex), composite, col, ofs)
            GenerateMaskedComposite(tex)
            System.err.printf(
                "Composite patch %s %d\n",
                getMaskedComposite(tex)!!.name,
                getMaskedComposite(tex)!!.columns.size
            )
        }

        // Last resort. 
        lastpatch = getMaskedComposite(tex)
        lasttex = tex
        composite = true
        lastlump = 0
        return lastpatch!!.columns[col]
    }

    // False: disk-mirrored patch. True: improper "transparent composite".
    private var composite = false
    private var lasttex = -1
    private var lastlump = -1
    private var lastpatch: patch_t? = null

    init {
        W = DM.wadLoader
        I = DM.doomSystem
        LL = DM.levelLoader
        FlatPatchCache = Hashtable()
    }

    /**
     * R_GetColumn variation which is tutti-frutti proof. It only returns cached
     * columns, and even pre-caches single-patch textures intead of trashing the
     * WAD manager (should be faster, in theory).
     *
     * Cannot be used for drawing masked textures, use classic GetColumn
     * instead.
     *
     *
     * @throws IOException
     */
    override fun GetCachedColumn(tex: Int, col: Int): ByteArray {
        var col = col
        val lump: Int
        val ofs: Int
        col = col and getTexturewidthmask(tex)
        lump = getTextureColumnLump(tex, col)
        ofs = getTextureColumnOfs(tex, col).code

        // In the case of cached columns, this is always 0.
        // Done externally, for now.
        //dcvars.dc_source_ofs = 0;

        // If pointing inside a non-zero, positive lump, then it's not a
        // composite texture.
        // Read from disk, and safeguard vs tutti frutti.
        if (lump > 0) {
            // This will actually return a pointer to a patch's columns.
            return getRogueColumn(lump, ofs)
        }

        // Texture should be composite, but it doesn't yet exist. Create it.
        if (getTextureComposite(tex) == null) GenerateComposite(tex)
        return getTextureComposite(tex, col)
    }

    override fun setSMPVars(num_threads: Int) {
        smp_composite = BooleanArray(num_threads) // = false;
        smp_lasttex = IntArray(num_threads) // = -1;
        smp_lastlump = IntArray(num_threads) // = -1;
        smp_lastpatch = arrayOfNulls(num_threads) // = null;        
    }

    companion object {
        private const val LUMPSTART = "F_START"
        private const val LUMPEND = "F_END"
        private const val DEUTEX_END = "FF_END"
        private const val DEUTEX_START = "FF_START"
    }
}