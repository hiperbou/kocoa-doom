package p


import data.Defines
import data.Limits
import data.mapthing_t
import doom.DoomMain
import doom.SourceCode
import doom.SourceCode.*
import m.BBox
import m.Settings
import m.fixed_t.Companion.FRACBITS
import mochadoom.Engine
import rr.*
import utils.C2JUtils

/**
 * The idea is to lump common externally readable properties that need DIRECT
 * ACCESS (aka not behindsetters/getters) here, as well as some common shared
 * internal structures/status objects. If you need access to stuff like the
 * blockmap/reject table etc. then you should ask for this class. If you only
 * need access to some methods like e.g. SetupLevel, you can simply use the
 * ILevelLoader interface.
 *
 * @author velktron
 */
abstract class AbstractLevelLoader(  // ///////////////// Status objects ///////////////////
    val DOOM: DoomMain<*, *>
) : ILevelLoader {
    //
    // MAP related Lookup tables.
    // Store VERTEXES, LINEDEFS, SIDEDEFS, etc.
    //
    var numvertexes = 0
    lateinit var vertexes: Array<vertex_t>
    fun isVertexesInitialized() = ::vertexes.isInitialized
    var numsegs = 0
    lateinit var segs: Array<seg_t>
    fun isSegsInitialized() = ::segs.isInitialized
    var numsectors = 0
    lateinit var sectors: Array<sector_t>
    fun isSectorsInitialized() = ::sectors.isInitialized
    var numsubsectors = 0
    lateinit var subsectors: Array<subsector_t>
    fun isSubsectorsInitialized() = ::subsectors.isInitialized
    var numnodes = 0
    lateinit var nodes: Array<node_t>
    fun isNodesInitialized() = ::nodes.isInitialized
    var numlines = 0
    lateinit var lines: Array<line_t>
    fun isLinesInitialized() = ::lines.isInitialized
    var numsides = 0
    lateinit var sides: Array<side_t>
    fun isSidesInitialized() = ::sides.isInitialized
    // BLOCKMAP
    // Created from axis aligned bounding box
    // of the map, a rectangular array of
    // blocks of size ...
    // Used to speed up collision detection
    // by spatial subdivision in 2D.
    //
    /** Blockmap size.  */
    var bmapwidth // size in mapblocks
            = 0
    var bmapheight // size in mapblocks
            = 0

    /**
     * killough 3/1/98: remove blockmap limit internally. Maes 29/9/2011: Header
     * stripped during loading and pointers pre-modified, so there's no double
     * arraying.
     */
    lateinit var blockmap // was short -- killough
            : IntArray

    /**
     * Maes 29/9/2011: Used only during loading. No more dichotomy with
     * blockmap.
     */
    protected lateinit var blockmaplump // was short -- killough
            : IntArray

    @SourceCode.fixed_t
    var bmaporgx = 0

    @SourceCode.fixed_t
    var bmaporgy = 0

    /** for thing chains  */
    var blocklinks: Array<mobj_t?>? = null

    /**
     * REJECT For fast sight rejection. Speeds up enemy AI by skipping detailed
     * LineOf Sight calculation. Without special effect, this could be used as a
     * PVS lookup as well.
     */
    lateinit var rejectmatrix: ByteArray

    // Maintain single and multi player starting spots.
    // 1/11/98 killough: Remove limit on deathmatch starts
    protected lateinit var deathmatchstarts // killough
            : Array<mapthing_t?>
    protected var num_deathmatchstarts // killough
            = 0

    // mapthing_t* deathmatch_p;
    protected var deathmatch_p = 0
    protected var playerstarts = arrayOfNulls<mapthing_t>(Limits.MAXPLAYERS)

    /**
     * P_SetThingPosition Links a thing into both a block and a subsector based
     * on it's x y. Sets thing.subsector properly
     */
    @SourceCode.Exact
    @P_MapUtl.C(P_MapUtl.P_SetThingPosition)
    override fun SetThingPosition(thing: mobj_t) {
        var ss: subsector_t
        val sec: sector_t
        val blockx: Int
        val blocky: Int
        val link: mobj_t?

        // link into subsector
        //R_PointInSubsector@ run {
            ss = PointInSubsector(thing._x, thing._y)
        //}
        thing.subsector = ss
        if (!C2JUtils.flags(thing.flags, mobj_t.MF_NOSECTOR)) {
            // invisible things don't go into the sector links
            sec = ss.sector!!
            thing.sprev = null
            thing.snext = sec.thinglist
            if (sec.thinglist != null) {
                sec.thinglist!!.sprev = thing
            }
            sec.thinglist = thing
        }

        // link into blockmap
        if (!C2JUtils.flags(thing.flags, mobj_t.MF_NOBLOCKMAP)) {
            // inert things don't need to be in blockmap
            blockx = getSafeBlockX(thing._x - bmaporgx)
            blocky = getSafeBlockY(thing._y - bmaporgy)

            // Valid block?
            if (blockx >= 0 && blockx < bmapwidth && blocky >= 0 && blocky < bmapheight) {
                // Get said block.
                link = blocklinks!![blocky * bmapwidth + blockx]
                thing.bprev = null // Thing is put at head of block...
                thing.bnext = link
                if (link != null) { // block links back at thing...
                    // This will work
                    link.bprev = thing
                }

                // "thing" is now effectively the new head
                // Iterators only follow "bnext", not "bprev".
                // If link was null, then thing is the first entry.
                blocklinks!![blocky * bmapwidth + blockx] = thing
            } else {
                // thing is off the map
                thing.bprev = null
                thing.bnext = thing.bprev
            }
        }
    }

    @SourceCode.Exact
    @R_Main.C(R_Main.R_PointInSubsector)
    override fun PointInSubsector(@SourceCode.fixed_t x: Int, @SourceCode.fixed_t y: Int): subsector_t {
        var node: node_t
        var side: Int
        var nodenum: Int

        // single subsector is a special case
        if (numnodes == 0) {
            return subsectors[0]
        }
        nodenum = numnodes - 1
        while (!C2JUtils.flags(nodenum, Defines.NF_SUBSECTOR)) {
            node = nodes.get(nodenum)
            R_PointOnSide@ run {
                side = node.PointOnSide(x, y)
            }
            nodenum = node.children[side]
        }
        return subsectors[nodenum and Defines.NF_SUBSECTOR.inv()]
    }

    // jff 10/8/98 use guardband>0
    // jff 10/12/98 0 ok with + 1 in rows,cols
    protected inner class linelist_t // type used to list lines in each block
    {
        var num = 0
        var next: linelist_t? = null
    }

    /**
     * Subroutine to add a line number to a block list It simply returns if the
     * line is already in the block
     *
     * @param lists
     * @param count
     * @param done
     * @param blockno
     * @param lineno
     */
    private fun AddBlockLine(
        lists: Array<linelist_t?>,
        count: IntArray,
        done: BooleanArray,
        blockno: Int,
        lineno: Int
    ) {
        val a = System.nanoTime()
        val l: linelist_t
        if (done[blockno]) return
        l = linelist_t()
        l.num = lineno
        l.next = lists[blockno]
        lists[blockno] = l
        count[blockno]++
        done[blockno] = true
        val b = System.nanoTime()
        total += b - a
    }

    var total: Long = 0

    /**
     * Actually construct the blockmap lump from the level data This finds the
     * intersection of each linedef with the column and row lines at the left
     * and bottom of each blockmap cell. It then adds the line to all block
     * lists touching the intersection. MAES 30/9/2011: Converted to Java. It's
     * important that LINEDEFS and VERTEXES are already read-in and defined, so
     * it is necessary to change map lump ordering for this to work.
     */
    protected fun CreateBlockMap() {
        val xorg: Int
        val yorg: Int // blockmap origin (lower left)
        val nrows: Int
        val ncols: Int // blockmap dimensions
        val blocklists: Array<linelist_t?> // array of pointers to lists of lines
        val blockcount: IntArray // array of counters of line lists
        val blockdone: BooleanArray // array keeping track of blocks/line
        val NBlocks: Int // number of cells = nrows*ncols
        var linetotal: Int // total length of all blocklists
        var map_minx = Int.MAX_VALUE // init for map limits search
        var map_miny = Int.MAX_VALUE
        var map_maxx = Int.MIN_VALUE
        var map_maxy = Int.MIN_VALUE
        val a = System.nanoTime()

        // scan for map limits, which the blockmap must enclose
        for (i in 0 until numvertexes) {
            var t: Int
            if (vertexes!![i].x.also { t = it } < map_minx) map_minx = t else if (t > map_maxx) map_maxx = t
            if (vertexes!![i].y.also { t = it } < map_miny) map_miny = t else if (t > map_maxy) map_maxy = t
        }
        map_minx = map_minx shr FRACBITS // work in map coords, not fixed_t
        map_maxx = map_maxx shr FRACBITS
        map_miny = map_miny shr FRACBITS
        map_maxy = map_maxy shr FRACBITS

        // set up blockmap area to enclose level plus margin
        xorg = map_minx - AbstractLevelLoader.BLOCK_MARGIN
        yorg = map_miny - AbstractLevelLoader.BLOCK_MARGIN
        ncols =
            map_maxx + AbstractLevelLoader.BLOCK_MARGIN - xorg + 1 + AbstractLevelLoader.BLOCK_MASK shr AbstractLevelLoader.BLOCK_SHIFT // jff
        // 10/12/98
        nrows =
            map_maxy + AbstractLevelLoader.BLOCK_MARGIN - yorg + 1 + AbstractLevelLoader.BLOCK_MASK shr AbstractLevelLoader.BLOCK_SHIFT // +1
        // needed
        // for
        NBlocks = ncols * nrows // map exactly 1 cell

        // create the array of pointers on NBlocks to blocklists
        // also create an array of linelist counts on NBlocks
        // finally make an array in which we can mark blocks done per line

        // CPhipps - calloc's
        blocklists = arrayOfNulls(NBlocks)
        blockcount = IntArray(NBlocks)
        blockdone = BooleanArray(NBlocks)

        // initialize each blocklist, and enter the trailing -1 in all
        // blocklists
        // note the linked list of lines grows backwards
        for (i in 0 until NBlocks) {
            blocklists[i] = linelist_t()
            blocklists[i]!!.num = -1
            blocklists[i]!!.next = null
            blockcount[i]++
        }

        // For each linedef in the wad, determine all blockmap blocks it
        // touches,
        // and add the linedef number to the blocklists for those blocks
        for (i in 0 until numlines) {
            val x1 = lines[i].v1x shr FRACBITS // lines[i] map coords
            val y1 = lines[i].v1y shr FRACBITS
            val x2 = lines[i].v2x shr FRACBITS
            val y2 = lines[i].v2y shr FRACBITS
            val dx = x2 - x1
            val dy = y2 - y1
            val vert = dx == 0 // lines[i] slopetype
            val horiz = dy == 0
            val spos = dx xor dy > 0
            val sneg = dx xor dy < 0
            var bx: Int
            var by: Int // block cell coords
            val minx = if (x1 > x2) x2 else x1 // extremal lines[i] coords
            val maxx = if (x1 > x2) x1 else x2
            val miny = if (y1 > y2) y2 else y1
            val maxy = if (y1 > y2) y1 else y2

            // no blocks done for this linedef yet
            C2JUtils.memset(blockdone, false, NBlocks)

            // The line always belongs to the blocks containing its endpoints
            bx = x1 - xorg shr AbstractLevelLoader.BLOCK_SHIFT
            by = y1 - yorg shr AbstractLevelLoader.BLOCK_SHIFT
            AddBlockLine(blocklists, blockcount, blockdone, by * ncols + bx, i)
            bx = x2 - xorg shr AbstractLevelLoader.BLOCK_SHIFT
            by = y2 - yorg shr AbstractLevelLoader.BLOCK_SHIFT
            AddBlockLine(blocklists, blockcount, blockdone, by * ncols + bx, i)

            // For each column, see where the line along its left edge, which
            // it contains, intersects the Linedef i. Add i to each
            // corresponding
            // blocklist.
            if (!vert) // don't interesect vertical lines with columns
            {
                for (j in 0 until ncols) {
                    // intersection of Linedef with x=xorg+(j<<blkshift)
                    // (y-y1)*dx = dy*(x-x1)
                    // y = dy*(x-x1)+y1*dx;
                    val x = xorg + (j shl AbstractLevelLoader.BLOCK_SHIFT) // (x,y) is intersection
                    val y = dy * (x - x1) / dx + y1
                    val yb = y - yorg shr AbstractLevelLoader.BLOCK_SHIFT // block row number
                    val yp = y - yorg and AbstractLevelLoader.BLOCK_MASK // y position within block
                    if (yb < 0 || yb > nrows - 1) // outside blockmap, continue
                        continue
                    if (x < minx || x > maxx) // line doesn't touch column
                        continue

                    // The cell that contains the intersection point is always
                    // added
                    AddBlockLine(
                        blocklists, blockcount, blockdone, ncols * yb + j, i
                    )

                    // if the intersection is at a corner it depends on the
                    // slope
                    // (and whether the line extends past the intersection)
                    // which
                    // blocks are hit
                    if (yp == 0) // intersection at a corner
                    {
                        if (sneg) // \ - blocks x,y-, x-,y
                        {
                            if (yb > 0 && miny < y) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * (yb - 1) + j, i
                            )
                            if (j > 0 && minx < x) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * yb + j - 1, i
                            )
                        } else if (spos) // / - block x-,y-
                        {
                            if (yb > 0 && j > 0 && minx < x) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * (yb - 1) + j - 1, i
                            )
                        } else if (horiz) // - - block x-,y
                        {
                            if (j > 0 && minx < x) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * yb + j - 1, i
                            )
                        }
                    } else if (j > 0 && minx < x) // else not at corner: x-,y
                        AddBlockLine(
                            blocklists, blockcount, blockdone, ncols
                                    * yb + j - 1, i
                        )
                }
            }

            // For each row, see where the line along its bottom edge, which
            // it contains, intersects the Linedef i. Add i to all the
            // corresponding
            // blocklists.
            if (!horiz) {
                for (j in 0 until nrows) {
                    // intersection of Linedef with y=yorg+(j<<blkshift)
                    // (x,y) on Linedef i satisfies: (y-y1)*dx = dy*(x-x1)
                    // x = dx*(y-y1)/dy+x1;
                    val y = yorg + (j shl AbstractLevelLoader.BLOCK_SHIFT) // (x,y) is intersection
                    val x = dx * (y - y1) / dy + x1
                    val xb = x - xorg shr AbstractLevelLoader.BLOCK_SHIFT // block column number
                    val xp = x - xorg and AbstractLevelLoader.BLOCK_MASK // x position within block
                    if (xb < 0 || xb > ncols - 1) // outside blockmap, continue
                        continue
                    if (y < miny || y > maxy) // line doesn't touch row
                        continue

                    // The cell that contains the intersection point is always
                    // added
                    AddBlockLine(
                        blocklists, blockcount, blockdone, ncols * j + xb, i
                    )

                    // if the intersection is at a corner it depends on the
                    // slope
                    // (and whether the line extends past the intersection)
                    // which
                    // blocks are hit
                    if (xp == 0) // intersection at a corner
                    {
                        if (sneg) // \ - blocks x,y-, x-,y
                        {
                            if (j > 0 && miny < y) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * (j - 1) + xb, i
                            )
                            if (xb > 0 && minx < x) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * j + xb - 1, i
                            )
                        } else if (vert) // | - block x,y-
                        {
                            if (j > 0 && miny < y) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * (j - 1) + xb, i
                            )
                        } else if (spos) // / - block x-,y-
                        {
                            if (xb > 0 && j > 0 && miny < y) AddBlockLine(
                                blocklists, blockcount, blockdone,
                                ncols * (j - 1) + xb - 1, i
                            )
                        }
                    } else if (j > 0 && miny < y) // else not on a corner: x,y-
                        AddBlockLine(
                            blocklists, blockcount, blockdone, ncols
                                    * (j - 1) + xb, i
                        )
                }
            }
        }

        // Add initial 0 to all blocklists
        // count the total number of lines (and 0's and -1's)
        C2JUtils.memset(blockdone, false, NBlocks)
        linetotal = 0
        for (i in 0 until NBlocks) {
            AddBlockLine(blocklists, blockcount, blockdone, i, 0)
            linetotal += blockcount[i]
        }

        // Create the blockmap lump

        // blockmaplump = malloc_IfSameLevel(blockmaplump, 4 + NBlocks +
        // linetotal);
        blockmaplump = IntArray(4 + NBlocks + linetotal)
        // blockmap header
        bmaporgx = xorg shl FRACBITS
        blockmaplump[0] = bmaporgx
        bmaporgy = yorg shl FRACBITS
        blockmaplump[1] = bmaporgy
        bmapwidth = ncols
        blockmaplump[2] = bmapwidth
        bmapheight = nrows
        blockmaplump[3] = bmapheight

        // offsets to lists and block lists
        for (i in 0 until NBlocks) {
            var bl = blocklists[i]
            blockmaplump[4 + i] = ( // set offset to block's list
                    (if (i != 0) blockmaplump[4 + i - 1] else 4 + NBlocks) + if (i != 0) blockcount[i - 1] else 0)
            var offs = blockmaplump[4 + i]

            // add the lines in each block's list to the blockmaplump
            // delete each list node as we go
            while (bl != null) {
                val tmp = bl.next
                blockmaplump[offs++] = bl.num
                bl = tmp
            }
        }
        val b = System.nanoTime()
        System.err.printf("Blockmap generated in %f sec\n", (b - a) / 1e9)
        System.err.printf("Time spend in AddBlockLine : %f sec\n", total / 1e9)
    }

    // jff 10/6/98
    // End new code added to speed up calculation of internal blockmap
    //
    // P_VerifyBlockMap
    //
    // haleyjd 03/04/10: do verification on validity of blockmap.
    //
    protected fun VerifyBlockMap(count: Int): Boolean {
        var x: Int
        var y: Int
        y = 0
        while (y < bmapheight) {
            x = 0
            while (x < bmapwidth) {
                var offset: Int
                var p_list: Int
                var tmplist: Int
                var blockoffset: Int
                offset = y * bmapwidth + x
                blockoffset = offset + 4 // That's where the shit starts.

                // check that block offset is in bounds
                if (blockoffset >= count) {
                    System.err.printf("P_VerifyBlockMap: block offset overflow\n")
                    return false
                }
                offset = blockmaplump[blockoffset]

                // check that list offset is in bounds
                if (offset < 4 || offset >= count) {
                    System.err.printf("P_VerifyBlockMap: list offset overflow\n")
                    return false
                }
                p_list = offset

                // scan forward for a -1 terminator before maxoffs
                tmplist = p_list
                while (true) {

                    // we have overflowed the lump?
                    if (tmplist >= count) {
                        System.err.printf("P_VerifyBlockMap: open blocklist\n")
                        return false
                    }
                    if (blockmaplump[tmplist] == -1) // found -1
                        break
                    tmplist++
                }

                // scan the list for out-of-range linedef indicies in list
                tmplist = p_list
                while (blockmaplump[tmplist] != -1) {
                    if (blockmaplump[tmplist] < 0 || blockmaplump[tmplist] >= numlines) {
                        System.err.printf("P_VerifyBlockMap: index >= numlines\n")
                        return false
                    }
                    tmplist++
                }
                x++
            }
            y++
        }
        return true
    }

    // cph - convenient sub-function
    protected fun AddLineToSector(li: line_t, sector: sector_t) {
        val bbox = sector.blockbox
        sector.lines!![sector.linecount++] = li
        BBox.AddToBox(bbox, li.v1!!.x, li.v1!!.y)
        BBox.AddToBox(bbox, li.v2!!.x, li.v2!!.y)
    }

    /**
     * Compute density of reject table. Aids choosing LUT optimizations.
     *
     * @return
     */
    protected fun rejectDensity(): Float {
        // float[] rowdensity=new float[numsectors];
        val tabledensity: Float
        var tcount = 0
        for (i in 0 until numsectors) {
            // int colcount=0;
            for (j in 0 until numsectors) {
                // Determine subsector entries in REJECT table.
                val pnum = i * numsectors + j
                val bytenum = pnum shr 3
                val bitnum = 1 shl (pnum and 7)

                // Check in REJECT table.
                if (!C2JUtils.flags(rejectmatrix[bytenum].toInt(), bitnum)) {
                    tcount++
                    // colcount++;
                }
            }
            // rowdensity[i]=((float)colcount/numsectors);
        }
        tabledensity = tcount.toFloat() / (numsectors * numsectors)
        return tabledensity
    }

    /**
     * Updates Reject table dynamically based on what expensive LOS checks say.
     * It does decrease the "reject density" the longer the level runs, however
     * its by no means perfect, and results in many sleeping monsters. When
     * called, visibility between sectors x and y will be set to "false" for the
     * rest of the level, aka they will be rejected based on subsequent sight
     * checks.
     *
     * @param x
     * @param y
     */
    protected fun pokeIntoReject(x: Int, y: Int) {
        // Locate bit pointer e.g. for a 4x4 table, x=2 and y=3 give
        // 3*4+2=14
        val pnum = y * numsectors + x

        // Which byte?
        // 14= 1110 >>3 = 0001 so
        // Byte 0 Byte 1
        // xxxxxxxx xxxxxxxx
        // ^
        // 0.....bits......16
        // We are writing inside the second Byte 1
        val bytenum = pnum shr 3

        // OK, so how we pinpoint that one bit?
        // 1110 & 0111 = 0110 = 6 so it's the sixth bit
        // of the second byte
        val bitnum = pnum and 7

        // This sets only that one bit, and the reject lookup will be faster
        // next time.
        rejectmatrix[bytenum] =
            (rejectmatrix[bytenum].toInt() or AbstractLevelLoader.POKE_REJECT.get(bitnum)).toByte()
        println(rejectDensity())
    }

    protected fun retrieveFromReject(x: Int, y: Int, value: Boolean) {
        // Locate bit pointer e.g. for a 4x4 table, x=2 and y=3 give
        // 3*4+2=14
        val pnum = y * numsectors + x

        // Which byte?
        // 14= 1110 >>3 = 0001 so
        // Byte 0 Byte 1
        // xxxxxxxx xxxxxxxx
        // ^
        // 0.....bits......16
        // We are writing inside the second Byte 1
        val bytenum = pnum shr 3

        // OK, so how we pinpoint that one bit?
        // 1110 & 0111 = 0110 = 6 so it's the sixth bit
        // of the second byte
        val bitnum = pnum and 7

        // This sets only that one bit, and the reject lookup will be faster
        // next time.
        rejectmatrix[bytenum] =
            (rejectmatrix[bytenum].toInt() or AbstractLevelLoader.POKE_REJECT.get(bitnum)).toByte()
        println(rejectDensity())
    }

    // Keeps track of lines that belong to a sector, to exclude e.g.
    // orphaned ones from the blockmap.
    protected lateinit var used_lines: BooleanArray
    var blockmapxneg = -257
    var blockmapyneg = -257

    /**
     * Returns an int[] array with orgx, orgy, and number of blocks. Order is:
     * orgx,orgy,bckx,bcky
     *
     * @return
     */
    protected fun getMapBoundingBox(playable: Boolean): IntArray {
        var minx = Int.MAX_VALUE
        var miny = Int.MAX_VALUE
        var maxx = Int.MIN_VALUE
        var maxy = Int.MIN_VALUE

        // Scan linedefs to detect extremes
        for (i in lines.indices) {
            if (playable || used_lines[i]) {
                if (lines[i].v1x > maxx) {
                    maxx = lines[i].v1x
                }
                if (lines[i].v1x < minx) {
                    minx = lines[i].v1x
                }
                if (lines[i].v1y > maxy) {
                    maxy = lines[i].v1y
                }
                if (lines[i].v1y < miny) {
                    miny = lines[i].v1y
                }
                if (lines[i].v2x > maxx) {
                    maxx = lines[i].v2x
                }
                if (lines[i].v2x < minx) {
                    minx = lines[i].v2x
                }
                if (lines[i].v2y > maxy) {
                    maxy = lines[i].v2y
                }
                if (lines[i].v2y < miny) {
                    miny = lines[i].v2y
                }
            }
        }
        System.err.printf(
            "Map bounding %d %d %d %d\n",
            minx shr FRACBITS,
            miny shr FRACBITS,
            maxx shr FRACBITS,
            maxy shr FRACBITS
        )

        // Blow up bounding to the closest 128-sized block, adding 8 units as
        // padding.
        // This seems to be the "official" formula.
        val orgx = -Defines.BLOCKMAPPADDING + Defines.MAPBLOCKUNITS * (minx / Defines.MAPBLOCKUNITS)
        val orgy = -Defines.BLOCKMAPPADDING + Defines.MAPBLOCKUNITS * (miny / Defines.MAPBLOCKUNITS)
        val bckx = Defines.BLOCKMAPPADDING + maxx - orgx
        val bcky = Defines.BLOCKMAPPADDING + maxy - orgy
        System.err.printf(
            "%d %d %d %d\n", orgx shr FRACBITS, orgy shr FRACBITS,
            1 + (bckx shr Defines.MAPBLOCKSHIFT), 1 + (bcky shr Defines.MAPBLOCKSHIFT)
        )
        return intArrayOf(orgx, orgy, bckx, bcky)
    }

    protected fun LoadReject(lumpnum: Int) {
        var tmpreject = ByteArray(0)

        // _D_: uncommented the rejectmatrix variable, this permitted changing
        // level to work
        try {
            tmpreject = DOOM.wadLoader.CacheLumpNumAsRawBytes(lumpnum, Defines.PU_LEVEL)
        } catch (e: Exception) {
            // Any exception at this point means missing REJECT lump. Fuck that,
            // and move on.
            // If everything goes OK, tmpreject will contain the REJECT lump's
            // data
            // BUT, alas, we're not done yet.
        }

        // Sanity check on matrix.
        // E.g. a 5-sector map will result in ceil(25/8)=4 bytes.
        // If the reject table is broken/corrupt, too bad. It will all be
        // zeroes.
        // Much better than overflowing.
        // TODO: build-in a REJECT-matrix rebuilder?
        rejectmatrix = ByteArray(
            Math
                .ceil(numsectors * numsectors / 8.0).toInt()
        )
        System.arraycopy(
            tmpreject, 0, rejectmatrix, 0,
            Math.min(tmpreject.size, rejectmatrix.size)
        )

        // Do warn on atypical reject map lengths, but use either default
        // all-zeroes one,
        // or whatever you happened to read anyway.
        if (tmpreject.size < rejectmatrix.size) {
            System.err.printf(
                "BROKEN REJECT MAP! Length %d expected %d\n",
                tmpreject.size, rejectmatrix.size
            )
        }

        // Maes: purely academic. Most maps are well above 0.68
        // System.out.printf("Reject table density: %f",rejectDensity());
    }

    /**
     * Added config switch to turn on/off support
     *
     * Gets the proper blockmap block for a given X 16.16 Coordinate, sanitized
     * for 512-wide blockmaps.
     *
     * @param blockx
     * @return
     */
    @Compatible("blockx >> MAPBLOCKSHIFT")
    fun getSafeBlockX(blockx: Int): Int {
        var blockx = blockx
        blockx = blockx shr Defines.MAPBLOCKSHIFT
        return if (AbstractLevelLoader.FIX_BLOCKMAP_512 && blockx <= blockmapxneg) blockx and 0x1FF else blockx
    }

    @Compatible("blockx >> MAPBLOCKSHIFT")
    fun getSafeBlockX(blockx: Long): Int {
        var blockx = blockx
        blockx = blockx shr Defines.MAPBLOCKSHIFT
        return (if (AbstractLevelLoader.FIX_BLOCKMAP_512 && blockx <= blockmapxneg) blockx and 0x1FFL else blockx).toInt()
    }

    /** Gets the proper blockmap block for a given Y 16.16 Coordinate, sanitized
     * for 512-wide blockmaps.
     *
     * @param blocky
     * @return
     */
    @Compatible("blocky >> MAPBLOCKSHIFT")
    fun getSafeBlockY(blocky: Int): Int {
        var blocky = blocky
        blocky = blocky shr Defines.MAPBLOCKSHIFT
        return if (AbstractLevelLoader.FIX_BLOCKMAP_512 && blocky <= blockmapyneg) blocky and 0x1FF else blocky
    }

    @Compatible("blocky >> MAPBLOCKSHIFT")
    fun getSafeBlockY(blocky: Long): Int {
        var blocky = blocky
        blocky = blocky shr Defines.MAPBLOCKSHIFT
        return (if (AbstractLevelLoader.FIX_BLOCKMAP_512 && blocky <= blockmapyneg) blocky and 0x1FFL else blocky).toInt()
    }
    /// Sector tag stuff, lifted off Boom
    /** Hash the sector tags across the sectors and linedefs.
     * Call in SpawnSpecials.
     */
    fun InitTagLists() {
        var i: Int
        i = numsectors
        while (--i >= 0) {
            // Initially make all slots empty.
            sectors[i].firsttag = -1
        }
        i = numsectors
        while (--i >= 0) {
            // so that lower sectors appear first
            val j = sectors[i].tag % numsectors // Hash func
            sectors[i].nexttag = sectors[j].firsttag // Prepend sector to chain
            sectors[j].firsttag = i
        }

        // killough 4/17/98: same thing, only for linedefs
        i = numlines
        while (--i >= 0) {
            // Initially make all slots empty.
            lines[i].firsttag = -1
        }
        i = numlines
        while (--i >= 0) {
            // so that lower linedefs appear first
            val j = lines[i].tag % numlines // Hash func
            lines[i].nexttag = lines[j].firsttag // Prepend linedef to chain
            lines[j].firsttag = i
        }
    }

    companion object {
        //
        // jff 10/6/98
        // New code added to speed up calculation of internal blockmap
        // Algorithm is order of nlines*(ncols+nrows) not nlines*ncols*nrows
        //
        /**
         * places to shift rel position for cell num
         */
        protected const val BLOCK_SHIFT = 7

        /**
         * mask for rel position within cell
         */
        protected val BLOCK_MASK = (1 shl AbstractLevelLoader.BLOCK_SHIFT) - 1

        /**
         * size guardband around map used
         */
        protected const val BLOCK_MARGIN = 0
        protected var POKE_REJECT = intArrayOf(
            1, 2, 4, 8, 16, 32, 64,
            128
        )

        // MAES: extensions to support 512x512 blockmaps.
        // They represent the maximum negative number which represents
        // a positive offset, otherwise they are left at -257, which
        // never triggers a check.
        // If a blockmap index is ever LE than either, then
        // its actual value is to be interpreted as 0x01FF&x.
        // Full 512x512 blockmaps get this value set to -1.
        // A 511x511 blockmap would still have a valid negative number
        // e.g. -1..510, so they would be set to -2
        val FIX_BLOCKMAP_512: Boolean =
            Engine.getConfig().equals(Settings.fix_blockmap, java.lang.Boolean.TRUE)
    }
}