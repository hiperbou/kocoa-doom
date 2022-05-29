package p

import data.*
import defines.skill_t
import defines.slopetype_t
import doom.CommandVariable
import doom.DoomMain
import m.BBox
import m.fixed_t
import rr.*
import s.degenmobj_t
import utils.C2JUtils
import utils.GenericCopy
import utils.GenericCopy.ArraySupplier
import w.DoomBuffer
import java.io.IOException
import java.nio.ByteOrder
import java.util.function.IntFunction

//-----------------------------------------------------------------------------
//
// $Id: LevelLoader.java,v 1.44 2012/09/24 17:16:23 velktron Exp $
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
//  Do all the WAD I/O, get map description,
//  set up initial state and misc. LUTs.
//
//-----------------------------------------------------------------------------
class LevelLoader(DM: DoomMain<*, *>) : AbstractLevelLoader(DM) {
    /**
     * P_LoadVertexes
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun LoadVertexes(lump: Int) {
        // Make a lame-ass attempt at loading some vertexes.

        // Determine number of lumps:
        //  total lump length / vertex record length.
        numvertexes = DOOM.wadLoader.LumpLength(lump) / mapvertex_t.sizeOf()

        // Load data into cache.
        // MAES: we now have a mismatch between memory/disk: in memory, we need an array.
        // On disk, we have a single lump/blob. Thus, we need to find a way to deserialize this...
        vertexes = DOOM.wadLoader.CacheLumpNumIntoArray(lump, numvertexes, { vertex_t() }, IntFunction { arrayOfNulls<vertex_t>(it) } as IntFunction<Array<vertex_t>?>)

        // Copy and convert vertex coordinates,
        // MAES: not needed. Intermediate mapvertex_t struct skipped.
    }

    /**
     * P_LoadSegs
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun LoadSegs(lump: Int) {
        val data: Array<mapseg_t>
        var ml: mapseg_t
        var li: seg_t
        var ldef: line_t
        var linedef: Int
        var side: Int

        // Another disparity between disk/memory. Treat it the same as VERTEXES.
        numsegs = DOOM.wadLoader.LumpLength(lump) / mapseg_t.sizeOf()
        segs = GenericCopy.malloc({ seg_t() }, numsegs)
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsegs,
            { mapseg_t() },
            IntFunction { arrayOfNulls<mapseg_t>(it) } as IntFunction<Array<mapseg_t>?>
        )

        // We're not done yet!
        for (i in 0 until numsegs) {
            li = segs[i]
            ml = data[i]
            li.v1 = vertexes!![ml.v1.code]
            li.v2 = vertexes!![ml.v2.code]
            li.assignVertexValues()
            li.angle = (ml.angle.code shl 16).toLong() and 0xFFFFFFFFL
            li.offset = ml.offset.code shl 16
            linedef = ml.linedef.code
            ldef = lines[linedef]
            li.linedef = ldef
            side = ml.side.code
            li.sidedef = sides[ldef.sidenum[side].code]
            li.frontsector = sides[ldef.sidenum[side].code].sector
            if (C2JUtils.flags(ldef.flags.toInt(), line_t.ML_TWOSIDED)) {
                // MAES: Fix double sided without back side. E.g. Linedef 16103 in Europe.wad
                if (ldef.sidenum[side xor 1] != line_t.NO_INDEX) {
                    li.backsector = sides[ldef.sidenum[side xor 1].code].sector
                }
                // Fix two-sided with no back side.
                //else {
                //li.backsector=null;
                //ldef.flags^=ML_TWOSIDED;
                //}
            } else {
                li.backsector = null
            }
        }
    }

    /**
     * P_LoadSubsectors
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun LoadSubsectors(lump: Int) {
        var ms: mapsubsector_t
        var ss: subsector_t
        val data: Array<mapsubsector_t>
        numsubsectors = DOOM.wadLoader.LumpLength(lump) / mapsubsector_t.sizeOf()
        subsectors = GenericCopy.malloc({ subsector_t() }, numsubsectors)

        // Read "mapsubsectors"
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsubsectors,
            { mapsubsector_t() },
            IntFunction { arrayOfNulls<mapsubsector_t>(it) } as IntFunction<Array<mapsubsector_t>?>
        )
        for (i in 0 until numsubsectors) {
            ms = data[i]
            ss = subsectors[i]
            ss.numlines = ms.numsegs.code
            ss.firstline = ms.firstseg.code
        }
    }

    /**
     * P_LoadSectors
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun LoadSectors(lump: Int) {
        val data: Array<mapsector_t>
        var ms: mapsector_t
        var ss: sector_t
        numsectors = DOOM.wadLoader.LumpLength(lump) / mapsector_t.sizeOf()
        sectors = GenericCopy.malloc({ sector_t() }, numsectors)

        // Read "mapsectors"
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsectors,
            { mapsector_t() },
            IntFunction { arrayOfNulls<mapsector_t>(it) } as IntFunction<Array<mapsector_t>?>
        )
        for (i in 0 until numsectors) {
            ms = data[i]
            ss = sectors[i]
            ss.floorheight = ms.floorheight.toInt() shl fixed_t.FRACBITS
            ss.ceilingheight = ms.ceilingheight.toInt() shl fixed_t.FRACBITS
            ss.floorpic = DOOM.textureManager.FlatNumForName(ms.floorpic!!).toShort()
            ss.ceilingpic = DOOM.textureManager.FlatNumForName(ms.ceilingpic!!).toShort()
            ss.lightlevel = ms.lightlevel
            ss.special = ms.special
            ss.tag = ms.tag
            ss.thinglist = null
            ss.id = i
            ss.TL = DOOM.actions
            ss.RND = DOOM.random
        }
    }

    /**
     * P_LoadNodes
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun LoadNodes(lump: Int) {
        val data: Array<mapnode_t>
        var i: Int
        var j: Int
        var k: Int
        var mn: mapnode_t
        var no: node_t
        numnodes = DOOM.wadLoader.LumpLength(lump) / mapnode_t.sizeOf()
        nodes = GenericCopy.malloc({ node_t() }, numnodes)

        // Read "mapnodes"
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numnodes,
            { mapnode_t() },
            IntFunction { arrayOfNulls<mapnode_t>(it) } as IntFunction<Array<mapnode_t>?>
        )
        i = 0
        while (i < numnodes) {
            mn = data[i]
            no = nodes[i]
            no.x = mn.x.toInt() shl fixed_t.FRACBITS
            no.y = mn.y.toInt() shl fixed_t.FRACBITS
            no.dx = mn.dx.toInt() shl fixed_t.FRACBITS
            no.dy = mn.dy.toInt() shl fixed_t.FRACBITS
            j = 0
            while (j < 2) {

                // e6y: support for extended nodes
                no.children[j] = mn.children[j].code

                // e6y: support for extended nodes
                if (no.children[j] == 0xFFFF) {
                    no.children[j] = -0x1
                } else if (C2JUtils.flags(no.children[j], Defines.NF_SUBSECTOR_CLASSIC)) {
                    // Convert to extended type
                    no.children[j] = no.children[j] and Defines.NF_SUBSECTOR_CLASSIC.inv()

                    // haleyjd 11/06/10: check for invalid subsector reference
                    if (no.children[j] >= numsubsectors) {
                        System.err
                            .printf(
                                "P_LoadNodes: BSP tree references invalid subsector %d.\n",
                                no.children[j]
                            )
                        no.children[j] = 0
                    }
                    no.children[j] = no.children[j] or Defines.NF_SUBSECTOR
                }
                k = 0
                while (k < 4) {
                    no.bbox[j][k] = mn.bbox[j][k].toInt() shl fixed_t.FRACBITS
                    k++
                }
                j++
            }
            i++
        }
    }

    /**
     * P_LoadThings
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun LoadThings(lump: Int) {
        val data: Array<mapthing_t>
        var mt: mapthing_t
        val numthings: Int
        var spawn: Boolean
        numthings = DOOM.wadLoader.LumpLength(lump) / mapthing_t.sizeOf()
        // VERY IMPORTANT: since now caching is near-absolute,
        // the mapthing_t instances must be CLONED rather than just
        // referenced, otherwise missing mobj bugs start  happening.
        data = DOOM.wadLoader.CacheLumpNumIntoArray(lump, numthings, { mapthing_t() }, IntFunction { arrayOfNulls<mapthing_t>(it) } as IntFunction<Array<mapthing_t>?>)
        for (i in 0 until numthings) {
            mt = data[i]
            spawn = true

            // Do not spawn cool, new monsters if !commercial
            if (!DOOM.isCommercial()) {
                when (mt.type.toInt()) {
                    68, 64, 88, 89, 69, 67, 71, 65, 66, 84 -> spawn = false
                }
            }
            if (spawn == false) {
                break
            }

            // Do spawn all other stuff.
            // MAES: we have loaded the shit with the proper endianness, so no fucking around, bitch.
            /*mt.x = SHORT(mt.x);
      mt.y = SHORT(mt.y);
      mt.angle = SHORT(mt.angle);
      mt.type = SHORT(mt.type);
      mt.options = SHORT(mt.options);*/
            //System.out.printf("Spawning %d %s\n",i,mt.type);
            DOOM.actions.SpawnMapThing(mt)
        }

        // Status may have changed. It's better to release the resources anyway
        //W.UnlockLumpNum(lump);
    }

    /**
     * P_LoadLineDefs
     * Also counts secret lines for intermissions.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun LoadLineDefs(lump: Int) {
        val data: Array<maplinedef_t>
        var mld: maplinedef_t
        var ld: line_t
        var v1: vertex_t
        var v2: vertex_t
        numlines = DOOM.wadLoader.LumpLength(lump) / maplinedef_t.sizeOf()
        lines = GenericCopy.malloc({ line_t() }, numlines)

        // Check those actually used in sectors, later on.
        used_lines = BooleanArray(numlines)

        // read "maplinedefs"
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numlines,
            { maplinedef_t() },
            IntFunction { arrayOfNulls<maplinedef_t>(it) } as IntFunction<Array<maplinedef_t>?>
        )
        for (i in 0 until numlines) {
            mld = data[i]
            ld = lines[i]
            ld.flags = mld.flags
            ld.special = mld.special
            ld.tag = mld.tag
            ld.v1 = vertexes!![mld.v1.code]
            v1 = ld.v1!!
            ld.v2 = vertexes!![mld.v2.code]
            v2 = ld.v2!!
            ld.dx = v2.x - v1.x
            ld.dy = v2.y - v1.y
            // Map value semantics.
            ld.assignVertexValues()
            if (ld.dx == 0) {
                ld.slopetype = slopetype_t.ST_VERTICAL
            } else if (ld.dy == 0) {
                ld.slopetype = slopetype_t.ST_HORIZONTAL
            } else {
                if (fixed_t.FixedDiv(ld.dy, ld.dx) > 0) {
                    ld.slopetype = slopetype_t.ST_POSITIVE
                } else {
                    ld.slopetype = slopetype_t.ST_NEGATIVE
                }
            }
            if (v1.x < v2.x) {
                ld.bbox[BBox.BOXLEFT] = v1.x
                ld.bbox[BBox.BOXRIGHT] = v2.x
            } else {
                ld.bbox[BBox.BOXLEFT] = v2.x
                ld.bbox[BBox.BOXRIGHT] = v1.x
            }
            if (v1.y < v2.y) {
                ld.bbox[BBox.BOXBOTTOM] = v1.y
                ld.bbox[BBox.BOXTOP] = v2.y
            } else {
                ld.bbox[BBox.BOXBOTTOM] = v2.y
                ld.bbox[BBox.BOXTOP] = v1.y
            }
            ld.sidenum[0] = mld.sidenum[0]
            ld.sidenum[1] = mld.sidenum[1]

            // Sanity check for two-sided without two valid sides.      
            if (C2JUtils.flags(ld.flags.toInt(), line_t.ML_TWOSIDED)) {
                if (ld.sidenum[0] == line_t.NO_INDEX || ld.sidenum[1] == line_t.NO_INDEX) {
                    // Well, dat ain't so tu-sided now, ey esse?
                    ld.flags = (ld.flags.toInt() xor line_t.ML_TWOSIDED).toShort()
                }
            }

            // Front side defined without a valid frontsector.
            if (ld.sidenum[0] != line_t.NO_INDEX) {
                ld.frontsector = sides[ld.sidenum[0].code].sector
                if (ld.frontsector == null) { // // Still null? Bad map. Map to dummy.
                    ld.frontsector = dummy_sector
                }
            } else {
                ld.frontsector = null
            }

            // back side defined without a valid backsector.
            if (ld.sidenum[1] != line_t.NO_INDEX) {
                ld.backsector = sides[ld.sidenum[1].code].sector
                if (ld.backsector == null) { // Still null? Bad map. Map to dummy.
                    ld.backsector = dummy_sector
                }
            } else {
                ld.backsector = null
            }

            // If at least one valid sector is defined, then it's not null.
            if (ld.frontsector != null || ld.backsector != null) {
                used_lines[i] = true
            }
        }
    }

    /**
     * P_LoadSideDefs
     */
    @Throws(IOException::class)
    fun LoadSideDefs(lump: Int) {
        val data: Array<mapsidedef_t>
        var msd: mapsidedef_t
        var sd: side_t
        numsides = DOOM.wadLoader.LumpLength(lump) / mapsidedef_t.sizeOf()
        sides = GenericCopy.malloc({ side_t() }, numsides)
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsides,
            { mapsidedef_t() },
            IntFunction { arrayOfNulls<mapsidedef_t>(it) } as IntFunction<Array<mapsidedef_t>?>
        )
        for (i in 0 until numsides) {
            msd = data[i]
            sd = sides[i]
            sd.textureoffset = msd.textureoffset.toInt() shl fixed_t.FRACBITS
            sd.rowoffset = msd.rowoffset.toInt() shl fixed_t.FRACBITS
            sd.toptexture = DOOM.textureManager.TextureNumForName(msd.toptexture!!).toShort()
            sd.bottomtexture = DOOM.textureManager.TextureNumForName(msd.bottomtexture!!).toShort()
            sd.midtexture = DOOM.textureManager.TextureNumForName(msd.midtexture!!).toShort()
            if (msd.sector < 0) {
                sd.sector = dummy_sector
            } else {
                sd.sector = sectors[msd.sector.toInt()]
            }
        }
    }

    // MAES 22/5/2011 This hack added for PHOBOS2.WAD, in order to
    // accomodate for some linedefs having a sector number of "-1".
    // Any negative sector will get rewired to this dummy sector.
    // PROBABLY, this will handle unused sector/linedefes cleanly?
    var dummy_sector = sector_t()

    init {
        // Traditional loader sets limit.
        deathmatchstarts = arrayOfNulls(ILevelLoader.MAX_DEATHMATCH_STARTS)
    }

    /**
     * P_LoadBlockMap
     *
     * @throws IOException
     *
     * TODO: generate BLOCKMAP dynamically to
     * handle missing cases and increase accuracy.
     */
    @Throws(IOException::class)
    fun LoadBlockMap(lump: Int) {
        var count = 0
        if (DOOM.cVarManager.bool(CommandVariable.BLOCKMAP) || DOOM.wadLoader.LumpLength(lump) < 8 || DOOM.wadLoader.LumpLength(
                lump
            ) / 2.also { count = it } >= 0x10000
        ) // e6y
        {
            CreateBlockMap()
        } else {
            val data = DOOM.wadLoader.CacheLumpNum(lump, Defines.PU_LEVEL, DoomBuffer::class.java) as DoomBuffer
            count = DOOM.wadLoader.LumpLength(lump) / 2
            blockmaplump = IntArray(count)
            data.setOrder(ByteOrder.LITTLE_ENDIAN)
            data.rewind()
            data.readCharArray(blockmaplump, count)

            // Maes: first four shorts are header data.
            bmaporgx = blockmaplump[0] shl fixed_t.FRACBITS
            bmaporgy = blockmaplump[1] shl fixed_t.FRACBITS
            bmapwidth = blockmaplump[2]
            bmapheight = blockmaplump[3]

            // MAES: use killough's code to convert terminators to -1 beforehand
            for (i in 4 until count) {
                val t = blockmaplump[i].toShort() // killough 3/1/98
                blockmaplump[i] = (if (t.toInt() == -1) -1L else t.toInt() and 0xffff).toInt()
            }

            // haleyjd 03/04/10: check for blockmap problems
            // http://www.doomworld.com/idgames/index.php?id=12935
            if (!VerifyBlockMap(count)) {
                System.err
                    .printf("P_LoadBlockMap: erroneous BLOCKMAP lump may cause crashes.\n")
                System.err
                    .printf("P_LoadBlockMap: use \"-blockmap\" command line switch for rebuilding\n")
            }
        }
        count = bmapwidth * bmapheight

        // IMPORTANT MODIFICATION: no need to have both blockmaplump AND blockmap.
        // If the offsets in the lump are OK, then we can modify them (remove 4)
        // and copy the rest of the data in one single data array. This avoids
        // reserving memory for two arrays (we can't simply alias one in Java)
        blockmap = IntArray(blockmaplump.size - 4)

        // Offsets are relative to START OF BLOCKMAP, and IN SHORTS, not bytes.
        for (i in 0 until blockmaplump.size - 4) {
            // Modify indexes so that we don't need two different lumps.
            // Can probably be further optimized if we simply shift everything backwards.
            // and reuse the same memory space.
            if (i < count) {
                blockmaplump[i] = blockmaplump[i + 4] - 4
            } else {
                // Make terminators definitively -1, different that 0xffff
                val t = blockmaplump[i + 4].toShort() // killough 3/1/98
                blockmaplump[i] = (if (t.toInt() == -1) -1L else t.toInt() and 0xffff).toInt()
            }
        }

        // clear out mobj chains
        // ATTENTION! BUG!!!
        // If blocklinks are "cleared" to void -but instantiated- objects,
        // very bad bugs happen, especially the second time a level is re-instantiated.
        // Probably caused other bugs as well, as an extra object would appear in iterators.
        if (blocklinks != null && blocklinks!!.size == count) {
            for (i in 0 until count) {
                blocklinks!![i] = null
            }
        } else {
            blocklinks = arrayOfNulls(count)
        }

        // Bye bye. Not needed.
        blockmap = blockmaplump
    }

    /**
     * P_GroupLines
     * Builds sector line lists and subsector sector numbers.
     * Finds block bounding boxes for sectors.
     */
    fun GroupLines() {
        var total: Int
        var li: line_t
        var sector: sector_t
        var ss: subsector_t
        var seg: seg_t
        val bbox = IntArray(4)
        var block: Int

        // look up sector number for each subsector
        for (i in 0 until numsubsectors) {
            ss = subsectors[i]
            seg = segs[ss.firstline]
            ss.sector = seg.sidedef!!.sector
        }

        //linebuffer=new line_t[numsectors][0];
        // count number of lines in each sector
        total = 0
        for (i in 0 until numlines) {
            li = lines[i]
            total++
            li.frontsector!!.linecount++
            if (li.backsector != null && li.backsector !== li.frontsector) {
                li.backsector!!.linecount++
                total++
            }
        }

        // build line tables for each sector    
        // MAES: we don't really need this in Java.
        // linebuffer = new line_t[total];
        // int linebuffercount=0;
        // We scan through ALL sectors.
        for (i in 0 until numsectors) {
            sector = sectors[i]
            BBox.ClearBox(bbox)
            //sector->lines = linebuffer;
            // We can just construct line tables of the correct size
            // for each sector.
            var countlines = 0
            // We scan through ALL lines....

            // System.out.println(i+ ": looking for sector -> "+sector);
            for (j in 0 until numlines) {
                li = lines[j]

                //System.out.println(j+ " front "+li.frontsector+ " back "+li.backsector);
                if (li.frontsector === sector || li.backsector === sector) {
                    // This sector will have one more line.
                    countlines++
                    // Expand bounding box...
                    BBox.AddToBox(bbox, li.v1!!.x, li.v1!!.y)
                    BBox.AddToBox(bbox, li.v2!!.x, li.v2!!.y)
                }
            }

            // So, this sector must have that many lines.
            sector.lines = arrayOfNulls(countlines)
            var addedlines = 0
            var pointline = 0

            // Add actual lines into sectors.
            for (j in 0 until numlines) {
                li = lines[j]
                // If
                if (li.frontsector === sector || li.backsector === sector) {
                    // This sector will have one more line.
                    sectors[i].lines!![pointline++] = lines[j]
                    addedlines++
                }
            }
            if (addedlines != sector.linecount) {
                DOOM.doomSystem.Error("P_GroupLines: miscounted")
            }

            // set the degenmobj_t to the middle of the bounding box
            sector.soundorg = degenmobj_t(
                (bbox[BBox.BOXRIGHT] + bbox[BBox.BOXLEFT]) / 2,
                (bbox[BBox.BOXTOP] + bbox[BBox.BOXBOTTOM]) / 2,
                (sector.ceilingheight - sector.floorheight) / 2
            )

            // adjust bounding box to map blocks
            block = bbox[BBox.BOXTOP] - bmaporgy + Limits.MAXRADIUS shr Defines.MAPBLOCKSHIFT
            block = if (block >= bmapheight) bmapheight - 1 else block
            sector.blockbox[BBox.BOXTOP] = block
            block = bbox[BBox.BOXBOTTOM] - bmaporgy - Limits.MAXRADIUS shr Defines.MAPBLOCKSHIFT
            block = if (block < 0) 0 else block
            sector.blockbox[BBox.BOXBOTTOM] = block
            block = bbox[BBox.BOXRIGHT] - bmaporgx + Limits.MAXRADIUS shr Defines.MAPBLOCKSHIFT
            block = if (block >= bmapwidth) bmapwidth - 1 else block
            sector.blockbox[BBox.BOXRIGHT] = block
            block = bbox[BBox.BOXLEFT] - bmaporgx - Limits.MAXRADIUS shr Defines.MAPBLOCKSHIFT
            block = if (block < 0) 0 else block
            sector.blockbox[BBox.BOXLEFT] = block
        }
    }

    override fun SetupLevel(
        episode: Int,
        map: Int,
        playermask: Int,
        skill: skill_t?
    ) {
        var i: Int
        val lumpname: String
        val lumpnum: Int
        try {
            DOOM.wminfo.maxfrags = 0
            DOOM.totalsecret = DOOM.wminfo.maxfrags
            DOOM.totalitems = DOOM.totalsecret
            DOOM.totalkills = DOOM.totalitems
            DOOM.wminfo.partime = 180
            i = 0
            while (i < Limits.MAXPLAYERS) {
                DOOM.players[i].itemcount = 0
                DOOM.players[i].secretcount = DOOM.players[i].itemcount
                DOOM.players[i].killcount = DOOM.players[i].secretcount
                i++
            }

            // Initial height of PointOfView
            // will be set by player think.
            DOOM.players[DOOM.consoleplayer].viewz = 1

            // Make sure all sounds are stopped before Z_FreeTags.
            DOOM.doomSound.Start()

            /*    
  #if 0 // UNUSED
      if (debugfile)
      {
      Z_FreeTags (PU_LEVEL, MAXINT);
      Z_FileDumpHeap (debugfile);
      }
      else
  #endif
             */
            //  Z_FreeTags (PU_LEVEL, PU_PURGELEVEL-1);
            // UNUSED W_Profile ();
            DOOM.actions.InitThinkers()

            // if working with a development map, reload it
            DOOM.wadLoader.Reload()

            // find map name
            lumpname = if (DOOM.isCommercial()) {
                if (map < 10) {
                    "MAP0$map"
                } else {
                    "MAP$map"
                }
            } else {
                ("E"
                        + ('0'.code + episode).toChar() + "M"
                        + ('0'.code + map).toChar())
            }
            lumpnum = DOOM.wadLoader.GetNumForName(lumpname)
            DOOM.leveltime = 0
            if (!DOOM.wadLoader.verifyLumpName(
                    lumpnum + ILevelLoader.ML_BLOCKMAP,
                    ILevelLoader.LABELS.get(ILevelLoader.ML_BLOCKMAP)
                )
            ) {
                System.err.println("Blockmap missing!")
            }

            // note: most of this ordering is important
            LoadVertexes(lumpnum + ILevelLoader.ML_VERTEXES)
            LoadSectors(lumpnum + ILevelLoader.ML_SECTORS)
            LoadSideDefs(lumpnum + ILevelLoader.ML_SIDEDEFS)
            LoadLineDefs(lumpnum + ILevelLoader.ML_LINEDEFS)
            LoadSubsectors(lumpnum + ILevelLoader.ML_SSECTORS)
            LoadNodes(lumpnum + ILevelLoader.ML_NODES)
            LoadSegs(lumpnum + ILevelLoader.ML_SEGS)

            // MAES: in order to apply optimizations and rebuilding, order must be changed.
            LoadBlockMap(lumpnum + ILevelLoader.ML_BLOCKMAP)
            //this.SanitizeBlockmap();
            //this.getMapBoundingBox();
            LoadReject(lumpnum + ILevelLoader.ML_REJECT)
            GroupLines()
            DOOM.bodyqueslot = 0
            // Reset to "deathmatch starts"
            DOOM.deathmatch_p = 0
            LoadThings(lumpnum + ILevelLoader.ML_THINGS)

            // if deathmatch, randomly spawn the active players
            if (DOOM.deathmatch) {
                i = 0
                while (i < Limits.MAXPLAYERS) {
                    if (DOOM.playeringame[i]) {
                        DOOM.players[i].mo = null
                        DOOM.DeathMatchSpawnPlayer(i)
                    }
                    i++
                }
            }

            // clear special respawning que
            DOOM.actions.ClearRespawnQueue()

            // set up world state
            DOOM.actions.SpawnSpecials()

            // build subsector connect matrix
            //  UNUSED P_ConnectSubsectors ();
            // preload graphics
            if (DOOM.precache) {
                DOOM.textureManager.PrecacheLevel()
                // MAES: thinkers are separate than texture management. Maybe split sprite management as well?
                DOOM.sceneRenderer.PreCacheThinkers()
            }
        } catch (e: Exception) {
            System.err.println("Error while loading level")
            e.printStackTrace()
        }
    }

    companion object {
        const val rcsid = "\$Id: LevelLoader.java,v 1.44 2012/09/24 17:16:23 velktron Exp $"
    }
}