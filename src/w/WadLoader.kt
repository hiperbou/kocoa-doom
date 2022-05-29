
// -----------------------------------------------------------------------------
//
// $Id: WadLoader.java,v 1.64 2014/03/28 00:55:32 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// DESCRIPTION:
// Handles WAD file header, directory, lump I/O.
//
// -----------------------------------------------------------------------------
package w

import data.Defines
import data.mapvertex_t
import doom.SourceCode.Compatible
import doom.SourceCode.W_Wad
import i.DummySystem
import i.IDoomSystem
import mochadoom.Loggers
import rr.patch_t
import utils.C2JUtils
import utils.GenericCopy
import utils.GenericCopy.ArraySupplier
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.function.IntFunction
import java.util.logging.Level
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class WadLoader() : IWadLoader {
    protected var I: IDoomSystem

    ///// CONSTRUCTOR
    constructor(I: IDoomSystem) : this() {
        this.I = I
    }
    //// FIELDS
    /** Location of each lump on disk.  */
    var lumpinfo: Array<lumpinfo_t?>
    var numlumps = 0

    /**
     * MAES: probably array of byte[]??? void** lumpcache;
     *
     * Actually, loaded objects will be deserialized here as the general type
     * "CacheableDoomObject" (in the worst case they will be byte[] or
     * ByteBuffer).
     *
     * Not to brag, but this system is FAR superior to the inline unmarshaling
     * used in other projects ;-)
     */
    private var lumpcache: Array<CacheableDoomObject?>? = null
    private lateinit var preloaded: BooleanArray

    /** Added for Boom compliance  */
    private val wadfiles: MutableList<wadfile_info_t>

    /**
     * #define strcmpi strcasecmp MAES: this is just capitalization. However we
     * can't manipulate String object in Java directly like this, so this must
     * be a return type.
     *
     * TODO: maybe move this in utils?
     */
    fun strupr(s: String): String {
        return s.uppercase(Locale.getDefault())
    }

    /* ditto */
    fun strupr(s: CharArray) {
        for (i in s.indices) {
            s[i] = s[i].uppercaseChar()
        }
    }

    //
    // LUMP BASED ROUTINES.
    //
    //
    // W_AddFile
    // All files are optional, but at least one file must be
    // found (PWAD, if all required lumps are present).
    // Files with a .wad extension are wadlink files
    // with multiple lumps.
    // Other files are single lumps with the base filename
    // for the lump name.
    //
    // If filename starts with a tilde, the file is handled
    // specially to allow map reloads.
    // But: the reload feature is a fragile hack...
    var reloadlump = 0

    // MAES: was char*
    var reloadname: String? = null

    /**
     * This is where lumps are actually read + loaded from a file.
     *
     * @param filename
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun AddFile(uri: String, entry: ZipEntry?, type: Int) {
        var uri = uri
        val header = wadinfo_t()
        var lump_p: Int // MAES: was lumpinfo_t* , but we can use it as an array
        // pointer.
        var handle: InputStream
        val storehandle: InputStream?
        val length: Long
        val startlump: Int
        var fileinfo = arrayOfNulls<filelump_t?>(1) // MAES: was *
        val singleinfo = filelump_t()

        // handle reload indicator.
        if (uri[0] == '~') {
            uri = uri.substring(1)
            reloadname = uri
            reloadlump = numlumps
        }

        // open the resource and add to directory
        // It can be any streamed type handled by the "sugar" utilities.
        handle = try {
            InputStreamSugar.createInputStreamFromURI(uri, entry, type)!!
        } catch (e: Exception) {
            I.Error(" couldn't open resource %s \n", uri)
            return
        }

        // Create and set wadfile info
        val wadinfo = wadfile_info_t()
        wadinfo.handle = handle
        wadinfo.name = uri
        wadinfo.entry = entry
        wadinfo.type = type

        // System.out.println(" adding " + filename + "\n");

        // We start at the number of lumps. This allows appending stuff.
        startlump = numlumps
        val checkname = if (wadinfo.entry != null) wadinfo.entry!!.name else uri
        // If not "WAD" then we check for single lumps.
        if (!C2JUtils.checkForExtension(checkname, "wad")) {
            fileinfo[0] = singleinfo
            singleinfo.filepos = 0
            singleinfo.size = InputStreamSugar.getSizeEstimate(handle, wadinfo.entry)

            // Single lumps. Only use 8 characters			
            singleinfo.name = C2JUtils.removeExtension(uri).uppercase(Locale.getDefault())
            singleinfo.actualname = singleinfo.name

            // MAES: check out certain known types of extension
            if (C2JUtils.checkForExtension(uri, "lmp")) wadinfo.src =
                wad_source_t.source_lmp else if (C2JUtils.checkForExtension(uri, "deh")) wadinfo.src =
                wad_source_t.source_deh else if (C2JUtils.checkForExtension(uri, null)) wadinfo.src =
                wad_source_t.source_deh
            numlumps++
        } else {
            // MAES: 14/06/10 this is historical, for this is the first time I
            // implement reading something from RAF into Doom's structs. 
            // Kudos to the JAKE2 team who solved  this problem before me.
            // MAES: 25/10/11: In retrospect, this solution, while functional, was
            // inelegant and limited.
            var dis = DataInputStream(handle)

            // Read header in one go. Usually doesn't cause trouble?
            header.read(dis)
            if (header.identification!!.compareTo("IWAD") != 0) {
                // Homebrew levels?
                if (header.identification!!.compareTo("PWAD") != 0) {
                    I.Error("Wad file %s doesn't have IWAD or PWAD id\n", checkname)
                } else wadinfo.src = wad_source_t.source_pwad

                // modifiedgame = true;
            } else wadinfo.src = wad_source_t.source_iwad
            length = header.numlumps
            // Init everything:
            fileinfo = GenericCopy.malloc({ filelump_t() }, length.toInt())
            dis.close()
            handle = InputStreamSugar.streamSeek(handle, header.infotableofs, wadinfo.maxsize, uri, entry, type)!!

            // FIX: sometimes reading from zip files doesn't work well, so we pre-cache the TOC
            val TOC = ByteArray((length * filelump_t.sizeof()).toInt())
            var read = 0
            while (read < TOC.size) {
                // Make sure we have all of the TOC, sometimes ZipInputStream "misses" bytes.
                // when wrapped.
                read += handle.read(TOC, read, TOC.size - read)
            }
            val bais = ByteArrayInputStream(TOC)

            // MAES: we can't read raw structs here, and even less BLOCKS of
            // structs.
            dis = DataInputStream(bais)
            DoomIO.readObjectArray(dis, fileinfo as Array<IReadableDoomObject>, length.toInt())
            numlumps += header.numlumps.toInt()
            wadinfo.maxsize = estimateWadSize(header, lumpinfo)
        } // end loading wad

        //  At this point, a WADFILE or LUMPFILE been successfully loaded, 
        // and so is added to the list
        wadfiles.add(wadinfo)

        // Fill in lumpinfo
        // MAES: this was a realloc(lumpinfo, numlumps*sizeof(lumpinfo_t)),
        // so we have to increase size and copy over. Maybe this should be
        // an ArrayList?
        val oldsize = lumpinfo.size
        val newlumpinfo = GenericCopy.malloc({ lumpinfo_t() }, numlumps)
        try {
            System.arraycopy(lumpinfo, 0, newlumpinfo, 0, oldsize)
        } catch (e: Exception) {
            // if (!lumpinfo)
            I.Error("Couldn't realloc lumpinfo")
        }

        // Bye bye, old lumpinfo!
        lumpinfo = newlumpinfo as Array<lumpinfo_t?>

        // MAES: lum_p was an alias for lumpinfo[startlump]. I know it's a
        // bit crude as an approximation but heh...
        lump_p = startlump

        // MAES: if reloadname is null, handle is stored...else an invalid
        // handle?
        storehandle = if (reloadname != null) null else handle

        // This iterates through single files.
        var fileinfo_p = 0
        var i = startlump
        while (i < numlumps) {
            lumpinfo[lump_p]!!.handle = storehandle
            lumpinfo[lump_p]!!.position = fileinfo[fileinfo_p]!!.filepos
            lumpinfo[lump_p]!!.size = fileinfo[fileinfo_p]!!.size
            // Make all lump names uppercase. Searches should also be uppercase only.
            lumpinfo[lump_p]!!.name = fileinfo[fileinfo_p]!!.name!!.uppercase(Locale.getDefault())
            lumpinfo[lump_p]!!.hash = lumpinfo[lump_p]!!.name.hashCode()
            // lumpinfo[lump_p].stringhash = name8.getLongHash(strupr(lumpinfo[lump_p].name));
            // LumpNameHash(lumpinfo[lump_p].name);
            lumpinfo[lump_p]!!.intname = name8.getIntName(strupr(lumpinfo[lump_p]!!.name!!))
            //System.out.println(lumpinfo[lump_p]);
            lumpinfo[lump_p]!!.wadfile = wadinfo // MAES: Add Boom provenience info
            i++
            lump_p++
            fileinfo_p++
        }
        if (reloadname != null) handle.close()
    }

    /** Try to guess a realistic wad size limit based only on the number of lumps and their
     * STATED contents, in case it's not possible to get an accurate stream size otherwise.
     * Of course, they may be way off with deliberately malformed files etc.
     *
     * @param header
     * @param lumpinfo2
     * @return
     */
    private fun estimateWadSize(header: wadinfo_t, lumpinfo: Array<lumpinfo_t?>): Long {
        var maxsize = header.infotableofs + header.numlumps * 16
        for (i in lumpinfo.indices) {
            if (lumpinfo[i]!!.position + lumpinfo[i]!!.size > maxsize) {
                maxsize = lumpinfo[i]!!.position + lumpinfo[i]!!.size
            }
        }
        return maxsize
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#Reload()
	 */
    @Throws(Exception::class)
    override fun Reload() {
        val header = wadinfo_t()
        val lumpcount: Int
        var lump_p: Int // Maes: same as in W_WADload
        var i: Int
        var handle: DataInputStream? = null
        val length: Int
        val fileinfo: Array<filelump_t?>
        if (reloadname == null) return
        try {
            handle = DataInputStream(BufferedInputStream(FileInputStream(reloadname)))
        } catch (e: Exception) {
            I.Error("W_Reload: couldn't open %s", reloadname)
        }
        header.read(handle!!)
        // Actual number of lumps in file...
        lumpcount = header.numlumps.toInt()
        header.infotableofs = header.infotableofs
        length = lumpcount
        fileinfo = arrayOfNulls(length)
        handle!!.reset()
        handle.skip(header.infotableofs)

        // MAES: we can't read raw structs here, and even less BLOCKS of
        // structs.
        DoomIO.readObjectArrayWithReflection(handle, fileinfo as Array<IReadableDoomObject?>, length)

        /*
		 * for (int j=0;j<length;j++){ fileinfo[j].load (handle); }
		 */

        // numlumps += header.numlumps;
        // read (handle, fileinfo, length);

        // Fill in lumpinfo
        lump_p = reloadlump
        var fileinfo_p = 0
        i = reloadlump
        while (i < reloadlump + lumpcount) {
            if (lumpcache!![i] != null) {
                // That's like "freeing" it, right?
                lumpcache!![i] = null
                preloaded[i] = false
            }
            lumpinfo[lump_p]!!.position = fileinfo[fileinfo_p]!!.filepos
            lumpinfo[lump_p]!!.size = fileinfo[fileinfo_p]!!.size
            i++
            lump_p++
            fileinfo_p++
        }
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#InitMultipleFiles(java.lang.String[])
	 */
    @Throws(Exception::class)
    override fun InitMultipleFiles(filenames: Array<String?>) {
        val size: Int

        // open all the files, load headers, and count lumps
        numlumps = 0

        // will be realloced as lumps are added
        lumpinfo = arrayOfNulls(0)
        for (s in filenames) {
            if (s != null) {
                if (C2JUtils.testReadAccess(s)) {
                    // Resource is readable, guess type.
                    val type = C2JUtils.guessResourceType(s)
                    if (C2JUtils.flags(type, InputStreamSugar.ZIP_FILE)) {
                        addZipFile(s, type)
                    } else {
                        AddFile(s, null, type)
                    }
                    System.out.printf(
                        "\tadded %s (zipped: %s network: %s)\n", s,
                        C2JUtils.flags(type, InputStreamSugar.ZIP_FILE),
                        C2JUtils.flags(type, InputStreamSugar.NETWORK_FILE)
                    )
                } else System.err.printf("Couldn't open resource %s\n", s)
            }
        }
        if (numlumps == 0) I.Error("W_InitFiles: no files found")
        CoalesceMarkedResource("S_START", "S_END", li_namespace.ns_sprites)
        CoalesceMarkedResource("F_START", "F_END", li_namespace.ns_flats)
        // CoalesceMarkedResource("P_START", "P_END", li_namespace.ns_flats);

        // set up caching
        size = numlumps
        lumpcache = arrayOfNulls(size)
        preloaded = BooleanArray(size)
        if (lumpcache == null) I.Error("Couldn't allocate lumpcache")
        InitLumpHash()
    }

    /**
     * @param s
     * @param type
     * @throws IOException
     * @throws Exception
     */
    @Throws(IOException::class, Exception::class)
    protected fun addZipFile(s: String, type: Int) {
        // Get entries				        
        val `is` = BufferedInputStream(
            InputStreamSugar.createInputStreamFromURI(s, null, type)
        )
        val zip = ZipInputStream(`is`)
        val zes = InputStreamSugar.getAllEntries(zip)
        zip.close()
        for (zz in zes) {
            // The name of a zip file will be used as an identifier
            if (!zz.isDirectory) AddFile(s, zz, type)
        }
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#InitFile(java.lang.String)
	 */
    @Throws(Exception::class)
    override fun InitFile(filename: String?) {
        val names = arrayOfNulls<String>(1)
        names[0] = filename
        // names[1] = null;
        InitMultipleFiles(names)
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#NumLumps()
	 */
    override fun NumLumps(): Int {
        return numlumps
    }
    /**
     * W_CheckNumForName2 Returns -1 if name not found.
     *
     * A slightly better implementation, uses string hashes
     * as direct comparators (though 64-bit long descriptors
     * could be used). It's faster than the old method, but
     * still short from the HashMap's performance by
     * an order of magnitude.
     *
     * @param name
     * @return
     *
     * UNUSED
     *
     * public int CheckNumForName2(String name) {
     *
     * // scan backwards so patch lump files take precedence
     * int lump_p = numlumps;
     *
     * // make the name into two integers for easy compares
     * // case insensitive
     *
     * long hash = name8.getLongHash(name);
     * // System.out.print("Looking for "+name + " with hash "
     * // +Long.toHexString(hash));
     * while (lump_p-- != 0)
     * if (lumpinfo[lump_p].stringhash == hash) {
     * // System.out.print(" found "+lumpinfo[lump_p]+"\n" );
     * return lump_p;
     * }
     *
     * // TFB. Not found.
     * return -1;
     * }
     */
    /**
     * Old, shitty method for CheckNumForName. It's an overly literal
     * translation of how the C original worked, which was none too good
     * even without the overhead of converting a string to
     * its integer representation. It's so bad, that it's two orders
     * of magnitude slower than a HashMap implemetation, and one from
     * a direct hash/longname comparison with linear search.
     *
     * @param name
     * @return
     *
     *
     * public int CheckNumForName3(String name) {
     *
     * int v1;
     * int v2;
     * // lumpinfo_t lump_p;
     *
     * int lump_p;
     * // make the name into two integers for easy compares
     * // case insensitive
     * name8 union = new name8(strupr(name));
     *
     * v1 = union.x[0];
     * v2 = union.x[1];
     *
     * // scan backwards so patch lump files take precedence
     * lump_p = numlumps;
     *
     * while (lump_p-- != 0) {
     * int a = name8.stringToInt(lumpinfo[lump_p].name, 0);
     * int b = name8.stringToInt(lumpinfo[lump_p].name, 4);
     * if ((a == v1) && (b == v2)) {
     * return lump_p;
     * }
     * }
     *
     * // TFB. Not found.
     * return -1;
     * }
     */
    /* (non-Javadoc)
	 * @see w.IWadLoader#GetLumpinfoForName(java.lang.String)
	 */
    override fun GetLumpinfoForName(name: String): lumpinfo_t? {
        val v1: Int
        val v2: Int
        // lumpinfo_t lump_p;
        var lump_p: Int
        // make the name into two integers for easy compares
        // case insensitive
        val union = name8(strupr(name))
        v1 = union.x[0]
        v2 = union.x[1]

        // scan backwards so patch lump files take precedence
        lump_p = numlumps
        while (lump_p-- != 0) {
            val a: Int = name8.stringToInt(lumpinfo[lump_p]!!.name!!, 0)
            val b: Int = name8.stringToInt(lumpinfo[lump_p]!!.name!!, 4)
            if (a == v1 && b == v2) {
                return lumpinfo[lump_p]
            }
        }

        // TFB. Not found.
        return null
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#GetNumForName(java.lang.String)
	 */
    override fun GetNumForName(name: String): Int {
        val i: Int
        i = CheckNumForName(name.uppercase(Locale.getDefault()))
        if (i == -1) {
            val e = Exception()
            e.printStackTrace()
            System.err.println("Error: $name not found")
            System.err.println(
                "Hash: "
                        + java.lang.Long.toHexString(name8.getLongHash(name))
            )
            I.Error("W_GetNumForName: %s not found!", name)
        }
        return i
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#GetNameForNum(int)
	 */
    override fun GetNameForNum(lumpnum: Int): String? {
        return if (lumpnum >= 0 && lumpnum < numlumps) {
            lumpinfo[lumpnum]!!.name
        } else null
    }

    //
    // W_LumpLength
    // Returns the buffer size needed to load the given lump.
    //
    /* (non-Javadoc)
	 * @see w.IWadLoader#LumpLength(int)
	 */
    override fun LumpLength(lump: Int): Int {
        if (lump >= numlumps) I.Error("W_LumpLength: %i >= numlumps", lump)
        return lumpinfo[lump]!!.size.toInt()
    }

    override fun ReadLump(lump: Int): ByteArray {
        val l = lumpinfo[lump]
        val buf = ByteArray(l!!.size.toInt())
        ReadLump(lump, buf, 0)
        return buf
    }

    override fun ReadLump(lump: Int, buf: ByteArray?) {
        ReadLump(lump, buf, 0)
    }

    /**
     * W_ReadLump Loads the lump into the given buffer, which must be >=
     * W_LumpLength(). SKIPS CACHING
     *
     * @throws IOException
     */
    override fun ReadLump(lump: Int, buf: ByteArray?, offset: Int) {
        var c = 0
        val l: lumpinfo_t?
        var handle: InputStream? = null
        if (lump >= numlumps) {
            I.Error("W_ReadLump: %i >= numlumps", lump)
            return
        }
        l = lumpinfo[lump]
        if (l!!.handle == null) {
            // reloadable file, so use open / read / close
            try {
                // FIXME: reloadable files can only be that. Files.
                handle = InputStreamSugar.createInputStreamFromURI(reloadname, null, 0)
            } catch (e: Exception) {
                e.printStackTrace()
                I.Error("W_ReadLump: couldn't open %s", reloadname)
            }
        } else handle = l.handle
        try {
            handle = InputStreamSugar.streamSeek(
                handle, l.position,
                l.wadfile!!.maxsize, l.wadfile!!.name, l.wadfile!!.entry, l.wadfile!!.type
            )

            // read buffered. Unfortunately that interferes badly with 
            // guesstimating the actual stream position.
            val bis = BufferedInputStream(handle, 8192)
            while (c < l.size) c += bis.read(buf, offset + c, (l.size - c).toInt())

            // Well, that's a no-brainer.
            //l.wadfile.knownpos=l.position+c;
            if (c < l.size) System.err.printf(
                "W_ReadLump: only read %d of %d on lump %d %d\n", c, l.size,
                lump, l.position
            )
            if (l.handle == null) handle!!.close() else l.handle = handle
            I.BeginRead()
            return

            // ??? I_EndRead ();
        } catch (e: Exception) {
            e.printStackTrace()
            I.Error("W_ReadLump: could not read lump $lump")
            e.printStackTrace()
            return
        }
    }

    /** The most basic of the Wadloader functions. Will attempt to read a lump
     * off disk, based on the specific class type (it will call the unpack()
     * method). If not possible to call the unpack method, it will leave a
     * DoomBuffer object in its place, with the raw byte contents. It's
     *
     *
     */
    override fun <T> CacheLumpNum(lump: Int, tag: Int, what: Class<T>?): T? {
        if (lump >= numlumps) {
            I.Error("W_CacheLumpNum: %i >= numlumps", lump)
        }

        // Nothing cached here...
        // SPECIAL case : if no class is specified (null), the lump is re-read anyway
        // and you get a raw doombuffer. Plus, it won't be cached.
        if (lumpcache!![lump] == null || what == null) {

            // read the lump in

            // System.out.println("cache miss on lump "+lump);
            // Fake Zone system: mark this particular lump with the tag specified
            // ptr = Z_Malloc (W_LumpLength (lump), tag, &lumpcache[lump]);
            // Read as a byte buffer anyway.
            val thebuffer = ByteBuffer.wrap(ReadLump(lump))

            // Class type specified
            if (what != null) {
                try {
                    // Can it be uncached? If so, deserialize it.
                    if (implementsInterface(what, CacheableDoomObject::class.java)) {
                        // MAES: this should be done whenever single lumps
                        // are read. DO NOT DELEGATE TO THE READ OBJECTS THEMSELVES.
                        // In case of sequential reads of similar objects, use 
                        // CacheLumpNumIntoArray instead.
                        thebuffer.rewind()
                        lumpcache!![lump] = what.newInstance() as CacheableDoomObject
                        lumpcache!![lump]!!.unpack(thebuffer)

                        // Track it for freeing
                        Track(lumpcache!![lump], lump)
                        if (what == patch_t::class.java) {
                            (lumpcache!![lump] as patch_t?)!!.name = lumpinfo[lump]!!.name
                        }
                    } else {
                        // replace lump with parsed object.
                        lumpcache!![lump] = thebuffer as CacheableDoomObject

                        // Track it for freeing
                        Track(thebuffer as CacheableDoomObject, lump)
                    }
                } catch (e: Exception) {
                    System.err.println(
                        "Could not auto-instantiate lump "
                                + lump + " of class " + what
                    )
                    e.printStackTrace()
                }
            } else {
                // Class not specified? Then gimme a containing DoomBuffer!
                val db = DoomBuffer(thebuffer)
                lumpcache!![lump] = db
            }
        } else {
            // System.out.println("cache hit on lump " + lump);
            // Z.ChangeTag (lumpcache[lump],tag);
        }
        return lumpcache!![lump] as T?
    }

    /** A very useful method when you need to load a lump which can consist
     * of an arbitrary number of smaller fixed-size objects (assuming that you
     * know their number/size and the size of the lump). Practically used
     * by the level loader, to handle loading of sectors, segs, things, etc.
     * since their size/lump/number relationship is well-defined.
     *
     * It possible to do this in other ways, but it's extremely convenient this way.
     *
     * MAES 24/8/2011: This method is deprecated, Use the much more convenient
     * and slipstreamed generic version, which also handles caching of arrays
     * and auto-allocation.
     *
     * @param lump The lump number to load.
     * @param tag  Caching tag
     * @param array The array with objects to load. Its size implies how many to read.
     * @return
     */
    @Deprecated("")
    @Throws(IOException::class)
    override fun CacheLumpNumIntoArray(
        lump: Int, tag: Int, array: Array<mapvertex_t>,
        what: Class<*>?
    ) {
        if (lump >= numlumps) {
            I.Error("W_CacheLumpNum: %i >= numlumps", lump)
        }

        // Nothing cached here...
        if (lumpcache!![lump] == null) {

            // read the lump in

            //System.out.println("cache miss on lump " + lump);
            // Read as a byte buffer anyway.
            val thebuffer = ByteBuffer.wrap(ReadLump(lump))
            // Store the buffer anyway (as a DoomBuffer)
            lumpcache!![lump] = DoomBuffer(thebuffer)

            // Track it (as ONE lump)
            Track(lumpcache!![lump], lump)
        } else {
            //System.out.println("cache hit on lump " + lump);
            // Z.ChangeTag (lumpcache[lump],tag);
        }

        // Class type specified. If the previously cached stuff is a
        // "DoomBuffer" we can go on.
        if (what != null && lumpcache!![lump]!!.javaClass == DoomBuffer::class.java) {
            try {
                // Can it be uncached? If so, deserialize it. FOR EVERY OBJECT.
                val b = (lumpcache!![lump] as DoomBuffer?)!!.getBuffer()!!
                b.rewind()

                for (i in array.indices) {
                    if (implementsInterface(what, CacheableDoomObject::class.java)) {
                        (array.get(i) as CacheableDoomObject).unpack(b)
                    }
                }
                // lumpcache[lump]=array;
            } catch (e: Exception) {
                System.err.println(
                    "Could not auto-unpack lump " + lump
                            + " into an array of objects of class " + what
                )
                e.printStackTrace()
            }
        }
        return
    }

    /** A very useful method when you need to load a lump which can consist
     * of an arbitrary number of smaller fixed-size objects (assuming that you
     * know their number/size and the size of the lump). Practically used
     * by the level loader, to handle loading of sectors, segs, things, etc.
     * since their size/lump/number relationship is well-defined.
     *
     * It possible to do this in other (more verbose) ways, but it's
     * extremely convenient this way, as a lot of common and repetitive code
     * is only written once, and generically, here. Trumps the older
     * method in v 1.43 of WadLoader, which is deprecated.
     *
     * @param lump The lump number to load.
     * @param num number of objects to read	 *
     * @return a properly sized array of the correct type.
     */
    override fun <T : CacheableDoomObject> CacheLumpNumIntoArray(
        lump: Int,
        num: Int,
        what: ArraySupplier<T>,
        arrGen: IntFunction<Array<T>?>
    ): Array<T> {
        if (lump >= numlumps) {
            I.Error("CacheLumpNumIntoArray: %i >= numlumps", lump)
        }
        /**
         * Impossible condition unless you hack generics somehow
         * - Good Sign 2017/05/07
         */
        /*if (!implementsInterface(what, CacheableDoomObject.class)){
			I.Error("CacheLumpNumIntoArray: %s does not implement CacheableDoomObject", what.getName());
		}*/

        // Nothing cached here...
        if (lumpcache!![lump] == null && what != null) {
            //System.out.println("cache miss on lump " + lump);
            // Read as a byte buffer anyway.
            val thebuffer = ByteBuffer.wrap(ReadLump(lump))
            //TODO: is arrGen actually used?
            val stuff:Array<T> = GenericCopy.malloc(what, arrGen, num) as Array<T>
            //val stuff = GenericCopy.malloc<CacheableDoomObject>({ what.getWithInt(0) } , /*arrGen!!, */num)

            // Store the buffer anyway (as a CacheableDoomObjectContainer)
            lumpcache!![lump] = CacheableDoomObjectContainer(stuff)

            // Auto-unpack it, if possible.
            try {
                thebuffer.rewind()
                lumpcache!![lump]!!.unpack(thebuffer)
            } catch (e: IOException) {
                Loggers.getLogger(WadLoader::class.java.name).log(
                    Level.WARNING, String.format(
                        "Could not auto-unpack lump %s into an array of objects of class %s", lump, what
                    ), e
                )
            }

            // Track it (as ONE lump)
            Track(lumpcache!![lump], lump)
        } else {
            //System.out.println("cache hit on lump " + lump);
            // Z.ChangeTag (lumpcache[lump],tag);
        }
        if (lumpcache!![lump] == null) {
            throw Exception("Null pointer exception :(")
            //return null
        }
        val cont = lumpcache!![lump] as CacheableDoomObjectContainer<T>?
        return cont!!.getStuff()
    }

    fun CacheLumpNum(lump: Int): CacheableDoomObject? {
        return lumpcache!![lump]
    }

    /** Tells us if a class implements a certain interface.
     * If you know of a better way, be my guest.
     *
     * @param what
     * @param which
     * @return
     */
    protected fun implementsInterface(what: Class<*>, which: Class<*>): Boolean {
        val shit = what.interfaces
        for (i in shit.indices) {
            if (shit[i] == which) return true
        }
        return false
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#CacheLumpNameAsRawBytes(java.lang.String, int)
	 */
    override fun CacheLumpNameAsRawBytes(name: String, tag: Int): ByteArray {
        return (this.CacheLumpNum<Any>(
            GetNumForName(name), tag,
            null
        ) as DoomBuffer).getBuffer()!!.array()
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#CacheLumpNumAsRawBytes(int, int)
	 */
    override fun CacheLumpNumAsRawBytes(num: Int, tag: Int): ByteArray {
        return (this.CacheLumpNum<Any>(
            num, tag,
            null
        ) as DoomBuffer).getBuffer()!!.array()
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#CacheLumpName(java.lang.String, int)
	 */
    override fun CacheLumpName(name: String, tag: Int): DoomBuffer {
        return this.CacheLumpNum(
            GetNumForName(name), tag,
            DoomBuffer::class.java
        )!!
    }

    override fun CacheLumpNumAsDoomBuffer(lump: Int): DoomBuffer {
        return this.CacheLumpNum(
            lump, 0,
            DoomBuffer::class.java
        )!!
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#CachePatchName(java.lang.String)
	 */
    override fun CachePatchName(name: String): patch_t {
        return this.CacheLumpNum(
            GetNumForName(name), Defines.PU_CACHE,
            patch_t::class.java
        )!!
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#CachePatchName(java.lang.String, int)
	 */
    override fun CachePatchName(name: String, tag: Int): patch_t {
        return this.CacheLumpNum(
            GetNumForName(name), tag,
            patch_t::class.java
        )!!
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#CachePatchNum(int, int)
	 */
    override fun CachePatchNum(num: Int): patch_t {
        return this.CacheLumpNum(num, Defines.PU_CACHE, patch_t::class.java)!!
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#CacheLumpName(java.lang.String, int, java.lang.Class)
	 */
    @W_Wad.C(W_Wad.W_CacheLumpName)
    override fun <T : CacheableDoomObject?> CacheLumpName(name: String, tag: Int, what: Class<T>?): T {
        return this.CacheLumpNum(GetNumForName(name.uppercase(Locale.getDefault())), tag, what)!!
    }

    //
    // W_Profile
    //
    /* USELESS
	 char[][] info = new char[2500][10];

	int profilecount;

	void Profile() throws IOException {
		int i;
		// memblock_t block = null;
		Object ptr;
		char ch;
		FileWriter f;
		int j;
		String name;

		for (i = 0; i < numlumps; i++) {
			ptr = lumpcache[i];
			if ((ptr == null)) {
				ch = ' ';
				continue;
			} else {
				// block = (memblock_t *) ( (byte *)ptr - sizeof(memblock_t));
				if (block.tag < PU_PURGELEVEL)
					ch = 'S';
				else
					ch = 'P';
			}
			info[i][profilecount] = ch;
		}
		profilecount++;

		f = new FileWriter(new File("waddump.txt"));
		// name[8] = 0;

		for (i = 0; i < numlumps; i++) {
			name = lumpinfo[i].name;

			f.write(name);

			for (j = 0; j < profilecount; j++)
				f.write("    " + info[i][j]);

			f.write("\n");
		}
		f.close();
	} */
    /* (non-Javadoc)
	 * @see w.IWadLoader#isLumpMarker(int)
	 */
    override fun isLumpMarker(lump: Int): Boolean {
        return lumpinfo[lump]!!.size == 0L
    }

    /* (non-Javadoc)
	 * @see w.IWadLoader#GetNameForLump(int)
	 */
    override fun GetNameForLump(lump: Int): String? {
        return lumpinfo[lump]!!.name
    }
    // /////////////////// HASHTABLE SYSTEM ///////////////////
    //
    // killough 1/31/98: Initialize lump hash table
    //
    /**
     * Maes 12/12/2010: Some credit must go to Killough for first
     * Introducing the hashtable system into Boom. On early releases I had
     * copied his implementation, but it proved troublesome later on and slower
     * than just using the language's built-in hash table. Lesson learned, kids:
     * don't reinvent the wheel.
     *
     * TO get an idea of how superior using a hashtable is, on 1000000 random
     * lump searches the original takes 48 seconds, searching for precomputed
     * hashes takes 2.84, and using a HashMap takes 0.2 sec.
     *
     * And the best part is that Java provides a perfectly reasonable implementation.
     *
     */
    var doomhash: HashMap<String, Int>? = null
    protected fun InitLumpHash() {
        doomhash = HashMap(numlumps)

        //for (int i = 0; i < numlumps; i++)
        //	lumpinfo[i].index = -1; // mark slots empty

        // Insert nodes to the beginning of each chain, in first-to-last
        // lump order, so that the last lump of a given name appears first
        // in any chain, observing pwad ordering rules. killough
        for (i in 0 until numlumps) { // hash function:
            doomhash!![lumpinfo[i]!!.name!!.uppercase(Locale.getDefault())] = i
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see w.IWadLoader#CheckNumForName(java.lang.String)
	 */
    @Compatible
    @W_Wad.C(W_Wad.W_CheckNumForName)
    override fun CheckNumForName(name: String /* , int namespace */): Int {
        val r = doomhash!![name]
        // System.out.print("Found "+r);
        return r ?: -1

        // System.out.print(" found "+lumpinfo[i]+"\n" );
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see w.IWadLoader#CheckNumForName(java.lang.String)
	 */
    override fun CheckNumsForName(name: String): IntArray {
        list.clear()

        // Dumb search, no chained hashtables I'm afraid :-/
        // Move backwards, so list is compiled with more recent ones first.
        for (i in numlumps - 1 downTo 0) {
            if (name.compareTo(lumpinfo[i]!!.name!!, ignoreCase = true) == 0) {
                list.add(i)
            }
        }
        val num = list.size
        val result = IntArray(num)
        for (i in 0 until num) {
            result[i] = list[i]
        }

        // Might be empty/null, so check that out.
        return result
    }

    private val list = ArrayList<Int>()
    override fun GetLumpInfo(i: Int): lumpinfo_t? {
        return lumpinfo[i]
    }

    override fun CloseAllHandles() {
        val d = ArrayList<InputStream>()
        for (i in lumpinfo.indices) {
            if (!d.contains(lumpinfo[i]!!.handle)) d.add(lumpinfo[i]!!.handle!!)
        }
        var count = 0
        for (e in d) {
            try {
                e.close()
                //System.err.printf("%s file handle closed",e.toString());
                count++
            } catch (e1: IOException) {
                // TODO Auto-generated catch block
                e1.printStackTrace()
            }
        }

        //System.err.printf("%d file handles closed",count);
    }

    fun finalize() {
        CloseAllHandles()
    }

    /**
     * Based on Boom's W_CoalesceMarkedResource
     * Sort of mashes similar namespaces together so that they form
     * a continuous space (single start and end, e.g. so that multiple
     * S_START and S_END as well as special DEUTEX lumps mash together
     * under a common S_START/S_END boundary). Also also sort of performs
     * a "bubbling down" of marked lumps at the end of the namespace.
     *
     * It's convenient for sprites, but can be replaced by alternatives
     * for flats.
     *
     * killough 4/17/98: add namespace tags
     *
     * @param start_marker
     * @param end_marker
     * @param namespace
     * @return
     */
    fun CoalesceMarkedResource(
        start_marker: String?,
        end_marker: String?, namespace: li_namespace
    ): Int {
        var result = 0
        val marked = arrayOfNulls<lumpinfo_t>(numlumps)
        // C2JUtils.initArrayOfObjects(marked, lumpinfo_t.class);
        var num_marked = 0
        var num_unmarked = 0
        var is_marked = false
        var mark_end = false
        var lump: lumpinfo_t?

        // Scan for specified start mark
        for (i in 0 until numlumps) {
            lump = lumpinfo[i]
            if (WadLoader.IsMarker(start_marker!!, lump!!.name)) // start marker found
            { // If this is the first start marker, add start marker to marked lumps
//	    	System.err.printf("%s identified as starter mark for %s index %d\n",lump.name,
//	    			start_marker,i);
                if (num_marked == 0) {
                    marked[num_marked] = lumpinfo_t()
                    marked[num_marked]!!.name = "" + start_marker
                    marked[num_marked]!!.size = 0 // killough 3/20/98: force size to be 0
                    marked[num_marked]!!.namespace = li_namespace.ns_global // killough 4/17/98
                    marked[num_marked]!!.handle = lump.handle
                    // No real use for this yet
                    marked[num_marked]!!.wadfile = lump.wadfile
                    num_marked = 1
                    //System.err.printf("%s identified as FIRST starter mark for %s index %d\n",lump.name,
                    //		start_marker,i);
                }
                is_marked = true // start marking lumps
            } else if (WadLoader.IsMarker(end_marker!!, lump.name)) // end marker found
            {
                //	System.err.printf("%s identified as end mark for %s index %d\n",lump.name,
                //			end_marker,i);
                mark_end = true // add end marker below
                is_marked = false // stop marking lumps
            } else if (is_marked || lump.namespace == namespace) {
                // if we are marking lumps,
                // move lump to marked list
                // sf: check for namespace already set

                // sf 26/10/99:
                // ignore sprite lumps smaller than 8 bytes (the smallest possible)
                // in size -- this was used by some dmadds wads
                // as an 'empty' graphics resource
                if (namespace != li_namespace.ns_sprites || lump.size > 8) {
                    marked[num_marked] = lump.clone()
                    // System.err.printf("Marked %s as %d for %s\n",lump.name,num_marked,namespace);
                    marked[num_marked++]!!.namespace = namespace // killough 4/17/98
                    result++
                }
            } else lumpinfo[num_unmarked++] = lump.clone() // else move down THIS list
        }

        // Append marked list to end of unmarked list
        System.arraycopy(marked, 0, lumpinfo, num_unmarked, num_marked)
        numlumps = num_unmarked + num_marked // new total number of lumps
        if (mark_end) // add end marker
        {
            lumpinfo[numlumps]!!.size = 0 // killough 3/20/98: force size to be 0
            //lumpinfo[numlumps].wadfile = NULL;
            lumpinfo[numlumps]!!.namespace = li_namespace.ns_global // killough 4/17/98
            lumpinfo[numlumps++]!!.name = end_marker
        }
        return result
    }

    override fun UnlockLumpNum(lump: Int) {
        lumpcache!![lump] = null
    }

    override fun InjectLumpNum(lump: Int, obj: CacheableDoomObject?) {
        lumpcache!![lump] = obj
    }

    //// Merged remnants from LumpZone here.
    var zone: HashMap<CacheableDoomObject?, Int>

    init {
        lumpinfo = arrayOfNulls(0)
        zone = HashMap()
        wadfiles = ArrayList()
        I = DummySystem()
    }

    /** Add a lump to the tracking  */
    fun Track(lump: CacheableDoomObject?, index: Int) {
        zone[lump] = index
    }

    override fun UnlockLumpNum(lump: CacheableDoomObject?) {
        // Remove it from the reference
        val lumpno = zone.remove(lump)


        // Force nulling. This should trigger garbage collection,
        // and reclaim some memory, provided you also nulled any other 
        // reference to a certain lump. Therefore, make sure you null 
        // stuff right after calling this method, if you want to make sure 
        // that they won't be referenced anywhere else.
        if (lumpno != null) {
            lumpcache!![lumpno] = null
            //System.out.printf("Lump %d %d freed\n",lump.hashCode(),lumpno);
        }
    }

    override fun verifyLumpName(lump: Int, lumpname: String): Boolean {

        // Lump number invalid
        if (lump < 0 || lump > numlumps - 1) return false
        val name = GetNameForLump(lump)

        // Expected lump name not found
        return if (name == null || lumpname.compareTo(name, ignoreCase = true) != 0) false else true

        // Everything should be OK now...
    }

    override fun GetWadfileIndex(wad1: wadfile_info_t): Int {
        return wadfiles.indexOf(wad1)
    }

    override fun GetNumWadfiles(): Int {
        return wadfiles.size
    }

    companion object {
        const val ns_global = 0
        const val ns_flats = 1
        const val ns_sprites = 2
        fun IsMarker(marker: String, name: String?): Boolean {
            // Safeguard against nameless marker lumps e.g. in Galaxia.wad
            return if (name == null || name.length == 0) false else name.equals(
                marker,
                ignoreCase = true
            ) || marker[1] == '_' && name[0] == marker[0] && name.substring(1).equals(marker, ignoreCase = true)
        }
    }
}