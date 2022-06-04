package p


import boom.*
import boom.DeepBSPNodesV4
import com.hiperbou.lang.times
import data.*
import defines.skill_t
import defines.slopetype_t
import doom.CommandVariable
import doom.DoomMain
import doom.DoomStatus
import doom.SourceCode.CauseOfDesyncProbability
import doom.SourceCode.P_Setup
import doom.SourceCode.Suspicious
import m.BBox
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedDiv
import rr.*
import s.degenmobj_t
import utils.C2JUtils
import utils.GenericCopy
import utils.GenericCopy.ArraySupplier
import w.CacheableDoomObject
import w.CacheableDoomObjectContainer
import w.DoomBuffer
import w.wadfile_info_t
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.function.IntFunction

/*
 * Emacs style mode select -*- C++ -*-
 * -----------------------------------------------------------------------------
 * PrBoom: a Doom port merged with LxDoom and LSDLDoom based on BOOM, a modified
 * and improved DOOM engine Copyright (C) 1999 by id Software, Chi Hoang, Lee
 * Killough, Jim Flynn, Rand Phares, Ty Halderman Copyright (C) 1999-2000 by
 * Jess Haas, Nicolas Kalkhof, Colin Phipps, Florian Schulze Copyright 2005,
 * 2006 by Florian Schulze, Colin Phipps, Neil Stevens, Andrey Budko This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. DESCRIPTION: Do all
 * the WAD I/O, get map description, set up initial state and misc. LUTs.
 * 
 * MAES 30/9/2011: This is a direct translation of prBoom+'s 2.5.0.8 p_setup.c
 * and p_setup.h.
 *
 * Copyright (C) 2022 hiperbou
 * 
 * 
 * -----------------------------------------------------------------------------
 */
class BoomLevelLoader(DM: DoomMain<*, *>) : AbstractLevelLoader(DM) {
    // OpenGL related.
    lateinit var map_subsectors: ByteArray
    var firstglvertex = 0
    var nodesVersion = 0
    var forceOldBsp = false

    // figgi 08/21/00 -- glSegs
    internal inner class glseg_t {
        var v1 // start vertex (16 bit)
                = 0.toChar()
        var v2 // end vertex (16 bit)
                = 0.toChar()
        var linedef // linedef, or -1 for minisegs
                = 0.toChar()
        var side // side on linedef: 0 for right, 1 for left
                : Short = 0
        var partner // corresponding partner seg, or -1 on one-sided walls
                : Short = 0
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // REJECT
    // For fast sight rejection.
    // Speeds up enemy AI by skipping detailed
    // LineOf Sight calculation.
    // Without the special effect, this could
    // be used as a PVS lookup as well.
    //
    private var rejectlump = -1 // cph - store reject lump num if cached
    private var current_episode = -1
    private var current_map = -1
    private var current_nodesVersion = -1
    private var samelevel = false

    /**
     * e6y: Smart malloc Used by P_SetupLevel() for smart data loading. Do
     * nothing if level is the same. Passing a null array forces allocation.
     *
     * @param p
     * generically typed array to consider
     * @param numstuff
     * elements to realloc
     */
    private inline fun <reified T> malloc_IfSameLevel(
        p: Array<T>?,
        numstuff: Int,
        supplier: ArraySupplier<T>//,
        //generator: IntFunction<Array<T>>
    ): Array<T> {
        return if (!samelevel || p == null) {
            GenericCopy.malloc(supplier, numstuff)
        } else p
    }

    // e6y: Smart calloc
    // Used by P_SetupLevel() for smart data loading
    // Clear the memory without allocation if level is the same
    private inline fun <reified T : Resettable?> calloc_IfSameLevel(
        p: Array<T>?,
        numstuff: Int,
        supplier: ArraySupplier<T>//,
        //generator: IntFunction<Array<T>>
    ): Array<T> {
        return if (!samelevel) {
            GenericCopy.malloc(supplier, numstuff)
        } else {
            // TODO: stuff should be resetted!
            C2JUtils.resetAll(p as Array<Resettable>)
            p
        }
    }

    //
    // P_CheckForZDoomNodes
    //
    private fun P_CheckForZDoomNodes(lumpnum: Int, gl_lumpnum: Int): Boolean {
        var data: ByteArray?
        var check: Int
        data = DOOM.wadLoader.CacheLumpNumAsRawBytes(lumpnum + ILevelLoader.ML_NODES, 0)
        check = ByteBuffer.wrap(data).int
        if (check == BoomLevelLoader.ZNOD) DOOM.doomSystem.Error("P_CheckForZDoomNodes: ZDoom nodes not supported yet")
        data = DOOM.wadLoader.CacheLumpNumAsRawBytes(lumpnum + ILevelLoader.ML_SSECTORS, 0)
        check = ByteBuffer.wrap(data).int
        if (check == BoomLevelLoader.ZGLN) {
            DOOM.doomSystem.Error("P_CheckForZDoomNodes: ZDoom GL nodes not supported yet")
        }

        // Unlock them to force different buffering interpretation.
        DOOM.wadLoader.UnlockLumpNum(lumpnum + ILevelLoader.ML_NODES)
        DOOM.wadLoader.UnlockLumpNum(lumpnum + ILevelLoader.ML_SSECTORS)
        return false
    }

    //
    // P_CheckForDeePBSPv4Nodes
    // http://www.sbsoftware.com/files/DeePBSPV4specs.txt
    //
    private fun P_CheckForDeePBSPv4Nodes(lumpnum: Int, gl_lumpnum: Int): Boolean {
        val data: ByteArray
        var result = false
        data = DOOM.wadLoader.CacheLumpNumAsRawBytes(lumpnum + ILevelLoader.ML_NODES, 0)
        val compare = Arrays.copyOfRange(data, 0, 7)
        if (Arrays.equals(compare, DeepBSPNodesV4.DeepBSPHeader)) {
            println("P_CheckForDeePBSPv4Nodes: DeePBSP v4 Extended nodes are detected\n")
            result = true
        }
        DOOM.wadLoader.UnlockLumpNum(lumpnum + ILevelLoader.ML_NODES)
        return result
    }

    private fun P_CheckForZDoomUncompressedNodes(lumpnum: Int, gl_lumpnum: Int): Boolean {
        val data: ByteArray
        val wrapper: Int
        var result = false
        data = DOOM.wadLoader.CacheLumpNumAsRawBytes(lumpnum + ILevelLoader.ML_NODES, 0)
        wrapper = ByteBuffer.wrap(data).int
        if (wrapper == BoomLevelLoader.XNOD) {
            println("P_CheckForZDoomUncompressedNodes: ZDoom uncompressed normal nodes are detected\n")
            result = true
        }
        DOOM.wadLoader.UnlockLumpNum(lumpnum + ILevelLoader.ML_NODES)
        return result
    }

    //
    // P_GetNodesVersion
    //
    fun P_GetNodesVersion(lumpnum: Int, gl_lumpnum: Int) {
        var ver = -1
        nodesVersion = 0
        if (gl_lumpnum > lumpnum && forceOldBsp == false && DoomStatus.compatibility_level >= Compatibility.prboom_2_compatibility) {
            var data = DOOM.wadLoader.CacheLumpNumAsRawBytes(gl_lumpnum + BoomLevelLoader.ML_GL_VERTS, 0)
            var wrapper = ByteBuffer.wrap(data).int
            if (wrapper == BoomLevelLoader.gNd2) {
                data = DOOM.wadLoader.CacheLumpNumAsRawBytes(gl_lumpnum + BoomLevelLoader.ML_GL_SEGS, 0)
                wrapper = ByteBuffer.wrap(data).int
                if (wrapper == BoomLevelLoader.gNd3) {
                    ver = 3
                } else {
                    nodesVersion = BoomLevelLoader.gNd2
                    println("P_GetNodesVersion: found version 2 nodes\n")
                }
            }
            if (wrapper == BoomLevelLoader.gNd4) {
                ver = 4
            }
            if (wrapper == BoomLevelLoader.gNd5) {
                ver = 5
            }
            // e6y: unknown gl nodes will be ignored
            if (nodesVersion == 0 && ver != -1) {
                System.out.printf("P_GetNodesVersion: found version %d nodes\n", ver)
                System.out.printf("P_GetNodesVersion: version %d nodes not supported\n", ver)
            }
        } else {
            nodesVersion = 0
            println("P_GetNodesVersion: using normal BSP nodes\n")
            if (P_CheckForZDoomNodes(lumpnum, gl_lumpnum)) {
                DOOM.doomSystem.Error("P_GetNodesVersion: ZDoom nodes not supported yet")
            }
        }
    }

    //
    // P_LoadVertexes
    //
    // killough 5/3/98: reformatted, cleaned up
    //
    private fun P_LoadVertexes(lump: Int) {

        // Determine number of lumps:
        // total lump length / vertex record length.
        numvertexes = DOOM.wadLoader.LumpLength(lump) / mapvertex_t.sizeOf()

        // Allocate zone memory for buffer.
        vertexes = calloc_IfSameLevel(if(isVertexesInitialized()) vertexes else null, numvertexes, { vertex_t() })

        // Load data into cache.
        // cph 2006/07/29 - cast to mapvertex_t here, making the loop below much
        // neater
        var data: Array<mapvertex_t> // cph - final

        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numvertexes,
            { mapvertex_t() },
            IntFunction { arrayOfNulls<mapvertex_t>(it) } as IntFunction<Array<mapvertex_t>?>
        )

        // Copy and convert vertex coordinates,
        // internal representation as fixed.
        for (i in 0 until numvertexes) {
            vertexes!![i].x = data[i].x.toInt() shl FRACBITS
            vertexes!![i].y = data[i].y.toInt() shl FRACBITS
        }

        // Free buffer memory.
        DOOM.wadLoader.UnlockLumpNum(lump)
    }

    /*******************************************
     * Name : P_LoadVertexes2 * modified : 09/18/00, adapted for PrBoom * author
     * : figgi * what : support for gl nodes
     *
     * @throws IOException
     * *
     */
    // figgi -- FIXME: Automap showes wrong zoom boundaries when starting game
    // when P_LoadVertexes2 is used with classic BSP nodes.
    @Throws(IOException::class)
    private fun P_LoadVertexes2(lump: Int, gllump: Int) {
        val gldata: ByteBuffer
        var ml: Array<mapvertex_t>

        // GL vertexes come after regular ones.
        firstglvertex = DOOM.wadLoader.LumpLength(lump) / mapvertex_t.sizeOf()
        numvertexes = DOOM.wadLoader.LumpLength(lump) / mapvertex_t.sizeOf()
        if (gllump >= 0) { // check for glVertices
            // Read GL lump into buffer. This allows some flexibility
            gldata = DOOM.wadLoader.CacheLumpNumAsDoomBuffer(gllump)._buffer!!
            if (nodesVersion == BoomLevelLoader.gNd2) { // 32 bit GL_VERT format (16.16 fixed)
                // These vertexes are double in size than regular Doom vertexes.
                // Furthermore, we have to skip the first 4 bytes
                // (GL_VERT_OFFSET)
                // of the gl lump.
                numvertexes += (DOOM.wadLoader.LumpLength(gllump) - BoomLevelLoader.GL_VERT_OFFSET) / mapglvertex_t.sizeOf()

                // Vertexes size accomodates both normal and GL nodes.
                vertexes = malloc_IfSameLevel(vertexes, numvertexes, { vertex_t() })
                val mgl = GenericCopy.malloc({ mapglvertex_t() }, numvertexes - firstglvertex)

                // Get lump and skip first 4 bytes
                gldata.rewind()
                gldata.position(BoomLevelLoader.GL_VERT_OFFSET)
                CacheableDoomObjectContainer.unpack(gldata, mgl as Array<CacheableDoomObject>)
                var mgl_count = 0
                for (i in firstglvertex until numvertexes) {
                    vertexes!![i].x = mgl[mgl_count].x
                    vertexes!![i].y = mgl[mgl_count].y
                    mgl_count++
                }
            } else {
                // Vertexes size accomodates both normal and GL nodes.
                numvertexes += DOOM.wadLoader.LumpLength(gllump) / mapvertex_t.sizeOf()
                vertexes = malloc_IfSameLevel(if(isVertexesInitialized()) vertexes else null, numvertexes) { vertex_t() }
                ml = GenericCopy.malloc({ mapvertex_t() },
                    numvertexes - firstglvertex
                )

                // We can read this "directly" because no skipping is involved.
                gldata.rewind()
                CacheableDoomObjectContainer.unpack(gldata, ml as Array<CacheableDoomObject> )
                // ml = W.CacheLumpNumIntoArray(gllump,
                // numvertexes-firstglvertex,mapvertex_t.class);
                var ml_count = 0
                for (i in firstglvertex until numvertexes) {
                    vertexes!![i].x = ml[ml_count].x.toInt()
                    vertexes!![i].y = ml[ml_count].y.toInt()
                    ml_count++
                }
            }
            DOOM.wadLoader.UnlockLumpNum(gllump)
        }

        // Loading of regular lumps (sheesh!)
        ml = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            firstglvertex,
            { mapvertex_t() },
            IntFunction { arrayOfNulls<mapvertex_t>(it) } as IntFunction<Array<mapvertex_t>?>
        )
        for (i in 0 until firstglvertex) {
            vertexes!![i].x = ml[i].x.toInt()
            vertexes!![i].y = ml[i].y.toInt()
        }
        DOOM.wadLoader.UnlockLumpNum(lump)
    }

    /*******************************************
     * created : 08/13/00 * modified : 09/18/00, adapted for PrBoom * author :
     * figgi * what : basic functions needed for * computing gl nodes *
     */
    fun checkGLVertex(num: Int): Int {
        var num = num
        if (num and 0x8000 != 0) num = (num and 0x7FFF) + firstglvertex
        return num
    }

    //
    // P_LoadSegs
    //
    // killough 5/3/98: reformatted, cleaned up
    private fun P_LoadSegs(lump: Int) {
        val data: Array<mapseg_t>? // cph - final

        numsegs = DOOM.wadLoader.LumpLength(lump) / mapseg_t.sizeOf()
        segs = calloc_IfSameLevel(if(isSegsInitialized()) segs else null, numsegs, { seg_t() })
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsegs,
            { mapseg_t() },
            IntFunction { arrayOfNulls<mapseg_t>(it) } as IntFunction<Array<mapseg_t>?>
        )
        // wad
        // lump
        // handling
        // updated
        if (data == null || numsegs == 0) DOOM.doomSystem.Error("P_LoadSegs: no segs in level")
        for (i in 0 until numsegs) {
            val li = segs[i]
            val ml = data[i]
            var v1: Char
            var v2: Char
            var side: Int
            var linedef: Int
            var ldef: line_t
            li.iSegID = i // proff 11/05/2000: needed for OpenGL
            v1 = ml.v1
            v2 = ml.v2

            // e6y
            // moved down for additional checks to avoid overflow
            // if wrong vertexe's indexes are in SEGS lump
            // see below for more detailed information
            // li.v1 = &vertexes[v1];
            // li.v2 = &vertexes[v2];
            li.miniseg = false // figgi -- there are no minisegs in classic BSP
            // nodes

            // e6y: moved down, see below
            // li.length = GetDistance(li.v2.x - li.v1.x, li.v2.y - li.v1.y);
            li.angle = (ml.angle.code shl 16).toLong()
            li.offset = ml.offset.code shl 16
            linedef = ml.linedef.code

            // e6y: check for wrong indexes
            if (linedef >= numlines) {
                DOOM.doomSystem.Error("P_LoadSegs: seg %d references a non-existent linedef %d", i, linedef)
            }
            ldef = lines[linedef]
            li.linedef = ldef
            side = ml.side.code

            // e6y: fix wrong side index
            if (side != 0 && side != 1) {
                System.err.printf("P_LoadSegs: seg %d contains wrong side index %d. Replaced with 1.\n", i, side)
                side = 1
            }

            // e6y: check for wrong indexes
            if (ldef.sidenum[side] >= numsides.toChar()) {
                DOOM.doomSystem.Error(
                    "P_LoadSegs: linedef %d for seg %d references a non-existent sidedef %d",
                    linedef, i, ldef.sidenum[side]
                )
            }
            li.sidedef = sides[ldef.sidenum[side].code]

            /*
             * cph 2006/09/30 - our frontsector can be the second side of the
             * linedef, so must check for NO_INDEX in case we are incorrectly
             * referencing the back of a 1S line
             */if (ldef.sidenum[side] != E6Y.NO_INDEX) li.frontsector =
                sides[ldef.sidenum[side].code].sector else {
                li.frontsector = null
                System.err.printf("P_LoadSegs: front of seg %i has no sidedef\n", i)
            }
            if (C2JUtils.flags(
                    ldef.flags.toInt(),
                    line_t.ML_TWOSIDED
                ) && ldef.sidenum[side xor 1] != E6Y.NO_INDEX
            ) {
                li.backsector = sides[ldef.sidenum[side xor 1].code].sector
            } else {
                li.backsector = null
            }

            // e6y
            // check and fix wrong references to non-existent vertexes
            // see e1m9 @ NIVELES.WAD
            // http://www.doomworld.com/idgames/index.php?id=12647
            if (v1.code >= numvertexes || v2.code >= numvertexes) {
                val str = "P_LoadSegs: compatibility loss - seg %d references a non-existent vertex %d\n"
                if (DOOM.demorecording) {
                    DOOM.doomSystem.Error(
                        str + "Demo recording on levels with invalid nodes is not allowed",
                        i, if (v1.code >= numvertexes) v1 else v2
                    )
                }
                if (v1.code >= numvertexes) {
                    System.err.printf(str, i, v1)
                }
                if (v2.code >= numvertexes) {
                    System.err.printf(str, i, v2)
                }
                if (li.sidedef === sides[li.linedef!!.sidenum[0].code]) {
                    li.v1 = lines[ml.linedef.code].v1
                    li.v2 = lines[ml.linedef.code].v2
                } else {
                    li.v1 = lines[ml.linedef.code].v2
                    li.v2 = lines[ml.linedef.code].v1
                }
            } else {
                li.v1 = vertexes!![v1.code]
                li.v2 = vertexes!![v2.code]
            }
            li.assignVertexValues()

            // e6y: now we can calculate it
            li.length = BoomLevelLoader.GetDistance(li.v2x - li.v1x, li.v2y - li.v1y)

            // Recalculate seg offsets that are sometimes incorrect
            // with certain nodebuilders. Fixes among others, line 20365
            // of DV.wad, map 5
            li.offset = BoomLevelLoader.GetOffset(li.v1!!, if (ml.side.code != 0) ldef.v2!! else ldef.v1!!)
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    private fun P_LoadSegs_V4(lump: Int) {
        val data: Array<mapseg_v4_t>?
        numsegs = DOOM.wadLoader.LumpLength(lump) / mapseg_v4_t.sizeOf()
        segs = calloc_IfSameLevel(segs, numsegs) { seg_t() }
        data = DOOM.wadLoader.CacheLumpNumIntoArray(lump, numsegs, { mapseg_v4_t() }, IntFunction { arrayOfNulls<mapseg_v4_t>(it) } as IntFunction<Array<mapseg_v4_t>?>)
        if (data == null || numsegs == 0) DOOM.doomSystem.Error("P_LoadSegs_V4: no segs in level")

        numsegs.times { i ->
            val li = segs[i]
            val ml = data[i]
            var side: Int
            li.iSegID = i // proff 11/05/2000: needed for OpenGL
            val v1 = ml.v1
            val v2 = ml.v2
            li.miniseg = false // figgi -- there are no minisegs in classic BSP
            // nodes
            li.angle = (ml.angle.code shl 16).toLong()
            li.offset = ml.offset.code shl 16
            val linedef = ml.linedef.code

            // e6y: check for wrong indexes
            if (C2JUtils.unsigned(linedef) >= C2JUtils.unsigned(numlines)) {
                DOOM.doomSystem.Error(
                    "P_LoadSegs_V4: seg %d references a non-existent linedef %d",
                    i, C2JUtils.unsigned(linedef)
                )
            }
            val ldef = lines[linedef]
            li.linedef = ldef
            side = ml.side.code

            // e6y: fix wrong side index
            if (side != 0 && side != 1) {
                System.err.printf("P_LoadSegs_V4: seg %d contains wrong side index %d. Replaced with 1.\n", i, side)
                side = 1
            }

            // e6y: check for wrong indexes
            if (C2JUtils.unsigned(ldef.sidenum[side].code) >= C2JUtils.unsigned(numsides)) {
                DOOM.doomSystem.Error(
                    "P_LoadSegs_V4: linedef %d for seg %d references a non-existent sidedef %d",
                    linedef, i, C2JUtils.unsigned(ldef.sidenum[side].code)
                )
            }
            li.sidedef = sides[ldef.sidenum[side].code]

            /*
             * cph 2006/09/30 - our frontsector can be the second side of the
             * linedef, so must check for NO_INDEX in case we are incorrectly
             * referencing the back of a 1S line
             */if (ldef.sidenum[side] != E6Y.NO_INDEX) {
                li.frontsector = sides[ldef.sidenum[side].code].sector
            } else {
                li.frontsector = null
                System.err.printf("P_LoadSegs_V4: front of seg %i has no sidedef\n", i)
            }
            if (C2JUtils.flags(ldef.flags.toInt(), line_t.ML_TWOSIDED)
                && ldef.sidenum[side xor 1] != E6Y.NO_INDEX
            ) {
                li.backsector = sides[ldef.sidenum[side xor 1].code].sector
            } else {
                li.backsector = null
            }

            // e6y
            // check and fix wrong references to non-existent vertexes
            // see e1m9 @ NIVELES.WAD
            // http://www.doomworld.com/idgames/index.php?id=12647
            if (v1 >= numvertexes || v2 >= numvertexes) {
                val str = "P_LoadSegs_V4: compatibility loss - seg %d references a non-existent vertex %d\n"
                if (DOOM.demorecording) {
                    DOOM.doomSystem.Error(
                        str + "Demo recording on levels with invalid nodes is not allowed",
                        i, if (v1 >= numvertexes) v1 else v2
                    )
                }
                if (v1 >= numvertexes) {
                    System.err.printf(str, i, v1)
                }
                if (v2 >= numvertexes) {
                    System.err.printf(str, i, v2)
                }
                if (li.sidedef === sides[li.linedef!!.sidenum[0].code]) {
                    li.v1 = lines[ml.linedef.code].v1
                    li.v2 = lines[ml.linedef.code].v2
                } else {
                    li.v1 = lines[ml.linedef.code].v2
                    li.v2 = lines[ml.linedef.code].v1
                }
            } else {
                li.v1 = vertexes!![v1]
                li.v2 = vertexes!![v2]
            }

            // e6y: now we can calculate it
            li.length = BoomLevelLoader.GetDistance(li.v2!!.x - li.v1!!.x, li.v2!!.y - li.v1!!.y)

            // Recalculate seg offsets that are sometimes incorrect
            // with certain nodebuilders. Fixes among others, line 20365
            // of DV.wad, map 5
            li.offset = BoomLevelLoader.GetOffset(li.v1!!, if (ml.side.code != 0) ldef.v2!! else ldef.v1!!)
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    /*******************************************
     * Name : P_LoadGLSegs * created : 08/13/00 * modified : 09/18/00, adapted
     * for PrBoom * author : figgi * what : support for gl nodes *
     */
    /*
     * private void P_LoadGLSegs(int lump) { int i; final glseg_t ml; line_t
     * ldef; numsegs = W.LumpLength(lump) / sizeof(glseg_t); segs =
     * malloc_IfSameLevel(segs, numsegs * sizeof(seg_t)); memset(segs, 0,
     * numsegs * sizeof(seg_t)); ml = (final glseg_t*)W.CacheLumpNum(lump); if
     * ((!ml) || (!numsegs)) I_Error("P_LoadGLSegs: no glsegs in level"); for(i
     * = 0; i < numsegs; i++) { // check for gl-vertices segs[i].v1 =
     * &vertexes[checkGLVertex(LittleShort(ml.v1))]; segs[i].v2 =
     * &vertexes[checkGLVertex(LittleShort(ml.v2))]; segs[i].iSegID = i;
     * if(ml.linedef != (unsigned short)-1) // skip minisegs { ldef =
     * &lines[ml.linedef]; segs[i].linedef = ldef; segs[i].miniseg = false;
     * segs[i].angle =
     * R_PointToAngle2(segs[i].v1.x,segs[i].v1.y,segs[i].v2.x,segs[i].v2.y);
     * segs[i].sidedef = &sides[ldef.sidenum[ml.side]]; segs[i].length =
     * GetDistance(segs[i].v2.x - segs[i].v1.x, segs[i].v2.y - segs[i].v1.y);
     * segs[i].frontsector = sides[ldef.sidenum[ml.side]].sector; if (ldef.flags
     * & ML_TWOSIDED) segs[i].backsector =
     * sides[ldef.sidenum[ml.side^1]].sector; else segs[i].backsector = 0; if
     * (ml.side) segs[i].offset = GetOffset(segs[i].v1, ldef.v2); else
     * segs[i].offset = GetOffset(segs[i].v1, ldef.v1); } else { segs[i].miniseg
     * = true; segs[i].angle = 0; segs[i].offset = 0; segs[i].length = 0;
     * segs[i].linedef = NULL; segs[i].sidedef = NULL; segs[i].frontsector =
     * NULL; segs[i].backsector = NULL; } ml++; } W.UnlockLumpNum(lump); }
     */
    //
    // P_LoadSubsectors
    //
    // killough 5/3/98: reformatted, cleaned up
    private fun P_LoadSubsectors(lump: Int) {
        /*
         * cph 2006/07/29 - make data a final mapsubsector_t *, so the loop
         * below is simpler & gives no finalness warnings
         */
        val data: Array<mapsubsector_t>?
        numsubsectors = DOOM.wadLoader.LumpLength(lump) / mapsubsector_t.sizeOf()
        subsectors = calloc_IfSameLevel(if (isSubsectorsInitialized()) subsectors else null, numsubsectors, { subsector_t() })
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsubsectors,
            { mapsubsector_t() },
            IntFunction { arrayOfNulls<mapsubsector_t>(it) } as IntFunction<Array<mapsubsector_t>?>
        )
        if (data == null || numsubsectors == 0) {
            DOOM.doomSystem.Error("P_LoadSubsectors: no subsectors in level")
        }
        for (i in 0 until numsubsectors) {
            // e6y: support for extended nodes
            subsectors[i].numlines = data[i].numsegs.code
            subsectors[i].firstline = data[i].firstseg.code
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    private fun P_LoadSubsectors_V4(lump: Int) {
        /*
         * cph 2006/07/29 - make data a final mapsubsector_t *, so the loop
         * below is simpler & gives no finalness warnings
         */
        val data: Array<mapsubsector_v4_t>?
        numsubsectors = DOOM.wadLoader.LumpLength(lump) / mapsubsector_v4_t.sizeOf()
        subsectors = calloc_IfSameLevel(subsectors, numsubsectors, { subsector_t() })
        data =
            DOOM.wadLoader.CacheLumpNumIntoArray(lump, numsubsectors, { mapsubsector_v4_t() }, IntFunction { arrayOfNulls<mapsubsector_v4_t>(it) }as IntFunction<Array<mapsubsector_v4_t>?>)
        if (data == null || numsubsectors == 0) DOOM.doomSystem.Error("P_LoadSubsectors_V4: no subsectors in level")
        for (i in 0 until numsubsectors) {
            subsectors[i].numlines = data[i].numsegs.code
            subsectors[i].firstline = data[i].firstseg
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    //
    // P_LoadSectors
    //
    // killough 5/3/98: reformatted, cleaned up
    private fun P_LoadSectors(lump: Int) {
        val data: Array<mapsector_t> // cph - final*
        numsectors = DOOM.wadLoader.LumpLength(lump) / mapsector_t.sizeOf()
        val initSectors = if(isSectorsInitialized()) sectors else null
        sectors = calloc_IfSameLevel(initSectors, numsectors, { sector_t() })
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsectors,
            { mapsector_t() },
            IntFunction { arrayOfNulls<mapsector_t>(it) } as IntFunction<Array<mapsector_t>?>
        ) // cph
        // -
        // wad
        // lump
        // handling
        // updated
        for (i in 0 until numsectors) {
            val ss = sectors[i]
            val ms = data[i]
            ss.id = i // proff 04/05/2000: needed for OpenGL
            ss.floorheight = ms.floorheight.toInt() shl FRACBITS
            ss.ceilingheight = ms.ceilingheight.toInt() shl FRACBITS
            ss.floorpic = DOOM.textureManager.FlatNumForName(ms.floorpic!!).toShort()
            ss.ceilingpic = DOOM.textureManager.FlatNumForName(ms.ceilingpic!!).toShort()
            ss.lightlevel = ms.lightlevel
            ss.special = ms.special
            // ss.oldspecial = ms.special; huh?
            ss.tag = ms.tag
            ss.thinglist = null
            // MAES: link to thinker list and RNG
            ss.TL = DOOM.actions
            ss.RND = DOOM.random

            // ss.touching_thinglist = null; // phares 3/14/98

            // ss.nextsec = -1; //jff 2/26/98 add fields to support locking out
            // ss.prevsec = -1; // stair retriggering until build completes

            // killough 3/7/98:
            // ss.floor_xoffs = 0;
            // ss.floor_yoffs = 0; // floor and ceiling flats offsets
            // ss.ceiling_xoffs = 0;
            // ss.ceiling_yoffs = 0;
            // ss.heightsec = -1; // sector used to get floor and ceiling height
            // ss.floorlightsec = -1; // sector used to get floor lighting
            // killough 3/7/98: end changes

            // killough 4/11/98 sector used to get ceiling lighting:
            // ss.ceilinglightsec = -1;

            // killough 4/4/98: colormaps:
            // ss.bottommap = ss.midmap = ss.topmap = 0;

            // killough 10/98: sky textures coming from sidedefs:
            // ss.sky = 0;
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    //
    // P_LoadNodes
    //
    // killough 5/3/98: reformatted, cleaned up
    private fun P_LoadNodes(lump: Int) {
        val data: Array<mapnode_t>? // cph - final*
        numnodes = DOOM.wadLoader.LumpLength(lump) / mapnode_t.sizeOf()
        nodes = malloc_IfSameLevel(if(isNodesInitialized()) nodes else null, numnodes, { node_t() })
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numnodes,
            { mapnode_t() },
            IntFunction { arrayOfNulls<mapnode_t>(it) } as IntFunction<Array<mapnode_t>?>
        ) // cph
        // -
        // wad
        // lump
        // handling
        // updated
        if (data == null || numnodes == 0) {
            // allow trivial maps
            if (numsubsectors == 1) print("P_LoadNodes: trivial map (no nodes, one subsector)\n") else DOOM.doomSystem.Error(
                "P_LoadNodes: no nodes in level"
            )
        }
        for (i in 0 until numnodes) {
            val no = nodes[i]
            val mn = data[i]
            no.x = mn.x.toInt() shl FRACBITS
            no.y = mn.y.toInt() shl FRACBITS
            no.dx = mn.dx.toInt() shl FRACBITS
            no.dy = mn.dy.toInt() shl FRACBITS
            for (j in 0..1) {
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
                        System.err.printf("P_LoadNodes: BSP tree references invalid subsector %d.\n", no.children[j])
                        no.children[j] = 0
                    }
                    no.children[j] = no.children[j] or Defines.NF_SUBSECTOR
                }
                for (k in 0..3) {
                    no.bbox[j][k] = mn.bbox[j][k].toInt() shl FRACBITS
                }
            }
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    private fun P_LoadNodes_V4(lump: Int) {
        val data: DeepBSPNodesV4? // cph - final*
        numnodes = (DOOM.wadLoader.LumpLength(lump) - 8) / mapnode_v4_t.sizeOf()
        nodes = malloc_IfSameLevel(nodes, numnodes, { node_t() })
        data = DOOM.wadLoader.CacheLumpNum(lump, 0, DeepBSPNodesV4::class.java) // cph
        // -
        // wad
        // lump
        // handling
        // updated
        if (data == null || numnodes == 0) {
            // allow trivial maps
            if (numsubsectors == 1) {
                print("P_LoadNodes_V4: trivial map (no nodes, one subsector)\n")
            } else {
                DOOM.doomSystem.Error("P_LoadNodes_V4: no nodes in level")
            }
        } else { //data could be null
            for (i in 0 until numnodes) {
                val no = nodes[i]
                val mn = data.nodes[i]
                no.x = mn.x.toInt() shl FRACBITS
                no.y = mn.y.toInt() shl FRACBITS
                no.dx = mn.dx.toInt() shl FRACBITS
                no.dy = mn.dy.toInt() shl FRACBITS
                for (j in 0..1) {
                    no.children[j] = mn.children[j]
                    for (k in 0..3) {
                        no.bbox[j].bbox[k] = mn.bbox[j][k].toInt() shl FRACBITS
                    }
                }
            }
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    @Throws(IOException::class)
    private fun P_LoadZSegs(data: ByteBuffer) {
        val nodes = GenericCopy.malloc({ mapseg_znod_t() }, numsegs)
        CacheableDoomObjectContainer.unpack(data, nodes as Array<CacheableDoomObject>)
        for (i in 0 until numsegs) {
            var ldef: line_t
            var v1: Int
            var v2: Int
            var linedef: Int
            var side: Char
            val li = segs[i]
            val ml = nodes[i]
            v1 = ml.v1
            v2 = ml.v2
            li.iSegID = i // proff 11/05/2000: needed for OpenGL
            li.miniseg = false
            linedef = ml.linedef.code

            // e6y: check for wrong indexes
            if (C2JUtils.unsigned(linedef) >= C2JUtils.unsigned(numlines)) {
                DOOM.doomSystem.Error(
                    "P_LoadZSegs: seg %d references a non-existent linedef %d",
                    i, C2JUtils.unsigned(linedef)
                )
            }
            ldef = lines[linedef]
            li.linedef = ldef
            side = Char(ml.side.toUShort())

            // e6y: fix wrong side index
            if (side.code != 0 && side.code != 1) {
                System.err.printf("P_LoadZSegs: seg %d contains wrong side index %d. Replaced with 1.\n", i, side)
                side = 1.toChar()
            }

            // e6y: check for wrong indexes
            if (C2JUtils.unsigned(ldef.sidenum[side.code].code) >= C2JUtils.unsigned(numsides)) {
                DOOM.doomSystem.Error(
                    "P_LoadZSegs: linedef %d for seg %d references a non-existent sidedef %d",
                    linedef, i, C2JUtils.unsigned(ldef.sidenum[side.code].code)
                )
            }
            li.sidedef = sides[ldef.sidenum[side.code].code]

            /*
             * cph 2006/09/30 - our frontsector can be the second side of the
             * linedef, so must check for NO_INDEX in case we are incorrectly
             * referencing the back of a 1S line
             */if (ldef.sidenum[side.code] != E6Y.NO_INDEX) {
                li.frontsector = sides[ldef.sidenum[side.code].code].sector
            } else {
                li.frontsector = null
                System.err.printf("P_LoadZSegs: front of seg %i has no sidedef\n", i)
            }
            if (C2JUtils.flags(
                    ldef.flags.toInt(),
                    line_t.ML_TWOSIDED
                ) && ldef.sidenum[side.code xor 1] != E6Y.NO_INDEX
            ) {
                li.backsector = sides[ldef.sidenum[side.code xor 1].code].sector
            } else {
                li.backsector = null
            }
            li.v1 = vertexes!![v1]
            li.v2 = vertexes!![v2]
            li.length = BoomLevelLoader.GetDistance(li.v2!!.x - li.v1!!.x, li.v2!!.y - li.v1!!.y)
            li.offset = BoomLevelLoader.GetOffset(li.v1!!, if (side.code != 0) ldef.v2!! else ldef.v1!!)
            li.angle = RendererState.PointToAngle(segs[i].v1!!.x, segs[i].v1!!.y, segs[i].v2!!.x, segs[i].v2!!.y)
            // li.angle = (int)((float)atan2(li.v2.y - li.v1.y,li.v2.x -
            // li.v1.x) * (ANG180 / M_PI));
        }
    }

    private fun CheckZNodesOverflow(size: Int, count: Int): Int {
        var size = size
        size -= count
        if (size < 0) {
            DOOM.doomSystem.Error("P_LoadZNodes: incorrect nodes")
        }
        return size
    }

    @Throws(IOException::class)
    private fun P_LoadZNodes(lump: Int, glnodes: Int) {
        val data: ByteBuffer
        var len: Int
        val header: Int // for debugging
        val orgVerts: Int
        val newVerts: Int
        val numSubs: Int
        var currSeg: Int
        val numSegs: Int
        val numNodes: Int
        var newvertarray: Array<vertex_t?>? = null
        data = DOOM.wadLoader.CacheLumpNumAsDoomBuffer(lump)._buffer!!
        data.order(ByteOrder.LITTLE_ENDIAN)
        len = DOOM.wadLoader.LumpLength(lump)

        // skip header
        len = CheckZNodesOverflow(len, 4)
        header = data.int

        // Read extra vertices added during node building
        len = CheckZNodesOverflow(len, 4)
        orgVerts = data.int
        len = CheckZNodesOverflow(len, 4)
        newVerts = data.int
        if (!samelevel) {
            if (orgVerts + newVerts == numvertexes) {
                newvertarray = vertexes as Array<vertex_t?>
            } else {
                newvertarray = arrayOfNulls(orgVerts + newVerts)
                // TODO: avoid creating new objects that will be rewritten instantly - Good Sign 2017/05/07
                Arrays.setAll(newvertarray) { ii: Int -> vertex_t() }
                System.arraycopy(vertexes, 0, newvertarray, 0, orgVerts)
            }

            //(sizeof(newvertarray[0].x) + sizeof(newvertarray[0].y))
            len = CheckZNodesOverflow(len, newVerts * vertex_t.sizeOf())
            val tmp = z_vertex_t()
            for (i in 0 until newVerts) {
                tmp.unpack(data)
                newvertarray[i + orgVerts]!!.x = tmp.x
                newvertarray[i + orgVerts]!!.y = tmp.y
            }

            // Extra vertexes read in
            if (vertexes != newvertarray) {
                for (i in 0 until numlines) {
                    //lines[i].v1 = lines[i].v1 - vertexes + newvertarray;
                    //lines[i].v2 = lines[i].v2 - vertexes + newvertarray;
                    // Find indexes of v1 & v2 inside old vertexes array
                    // (.v1-vertexes) and use that index to re-point inside newvertarray              
                    lines[i].v1 = newvertarray[C2JUtils.indexOf(vertexes as Array<Any>, lines[i].v1!!)]
                    lines[i].v2 = newvertarray[C2JUtils.indexOf(vertexes as Array<Any>, lines[i].v2!!)]
                }
                // free(vertexes);
                vertexes = newvertarray as Array<vertex_t>
                numvertexes = orgVerts + newVerts
            }
        } else {
            // Skip the reading of all these new vertices and the expensive indexOf searches.
            val size: Int = newVerts * z_vertex_t.sizeOf()
            len = CheckZNodesOverflow(len, size)
            data.position(data.position() + size)
        }

        // Read the subsectors
        len = CheckZNodesOverflow(len, 4)
        numSubs = data.int
        numsubsectors = numSubs
        if (numsubsectors <= 0) {
            DOOM.doomSystem.Error("P_LoadZNodes: no subsectors in level")
        }
        subsectors = calloc_IfSameLevel(if (isSubsectorsInitialized()) subsectors else null, numsubsectors, { subsector_t() })
        len = CheckZNodesOverflow(len, numSubs * mapsubsector_znod_t.sizeOf())
        val mseg = mapsubsector_znod_t()
        for (i in 0.also { currSeg = it } until numSubs) {
            mseg.unpack(data)
            subsectors[i].firstline = currSeg
            subsectors[i].numlines = mseg.numsegs.toInt()
            currSeg += mseg.numsegs.toInt()
        }

        // Read the segs
        len = CheckZNodesOverflow(len, 4)
        numSegs = data.int

        // The number of segs stored should match the number of
        // segs used by subsectors.
        if (numSegs != currSeg) {
            DOOM.doomSystem.Error("P_LoadZNodes: Incorrect number of segs in nodes.")
        }
        numsegs = numSegs
        segs = calloc_IfSameLevel(if (isSegsInitialized()) segs else null, numsegs, { seg_t() })
        if (glnodes == 0) {
            len = CheckZNodesOverflow(len, numsegs * mapseg_znod_t.sizeOf())
            P_LoadZSegs(data)
        } else {
            //P_LoadGLZSegs (data, glnodes);
            DOOM.doomSystem.Error("P_LoadZNodes: GL segs are not supported.")
        }

        // Read nodes
        len = CheckZNodesOverflow(len, 4)
        numNodes = data.int
        numnodes = numNodes
        nodes = calloc_IfSameLevel(if(isNodesInitialized()) nodes else null, numNodes, { node_t() })
        len = CheckZNodesOverflow(len, numNodes * mapnode_znod_t.sizeOf())
        val znodes = GenericCopy.malloc({ mapnode_znod_t() }, numNodes)
        CacheableDoomObjectContainer.unpack(data, znodes as Array<CacheableDoomObject>)
        for (i in 0 until numNodes) {
            var j: Int
            var k: Int
            val no = nodes[i]
            val mn = znodes[i]
            no.x = mn.x.toInt() shl FRACBITS
            no.y = mn.y.toInt() shl FRACBITS
            no.dx = mn.dx.toInt() shl FRACBITS
            no.dy = mn.dy.toInt() shl FRACBITS
            j = 0
            while (j < 2) {
                no.children[j] = mn.children[j]
                k = 0
                while (k < 4) {
                    no.bbox[j].bbox[k] = mn.bbox[j][k].toInt() shl FRACBITS
                    k++
                }
                j++
            }
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
    }

    private var no_overlapped_sprites = false
    private fun GETXY(mobj: mobj_t): Int {
        return mobj._x + (mobj._y shr 16)
    }

    private fun dicmp_sprite_by_pos(a: Any, b: Any): Int {
        val m1 = a as mobj_t
        val m2 = b as mobj_t
        val res = GETXY(m2) - GETXY(m1)
        no_overlapped_sprites = no_overlapped_sprites && res != 0
        return res
    }

    /*
     * P_LoadThings killough 5/3/98: reformatted, cleaned up cph 2001/07/07 -
     * don't write into the lump cache, especially non-idepotent changes like
     * byte order reversals. Take a copy to edit.
     */
    @Suspicious(CauseOfDesyncProbability.LOW)
    @P_Setup.C(P_Setup.P_LoadThings)
    private fun P_LoadThings(lump: Int) {
        val numthings: Int = DOOM.wadLoader.LumpLength(lump) / mapthing_t.sizeOf()
        val data = DOOM.wadLoader.CacheLumpNumIntoArray(lump, numthings, { mapthing_t() }, IntFunction { arrayOfNulls<mapthing_t>(it) } as IntFunction<Array<mapthing_t>?>)
        var mobj: mobj_t?
        var mobjcount = 0
        val mobjlist = arrayOfNulls<mobj_t>(numthings)
        Arrays.setAll(mobjlist, IntFunction<mobj_t?> { j: Int -> mobj_t.createOn(DOOM) })
        if (data == null || numthings == 0) {
            DOOM.doomSystem.Error("P_LoadThings: no things in level")
        }
        for (i in 0 until numthings) {
            val mt = data!![i]

            /*
             * Not needed. Handled during unmarshaling. mt.x =
             * LittleShort(mt.x); mt.y = LittleShort(mt.y); mt.angle =
             * LittleShort(mt.angle); mt.type = LittleShort(mt.type); mt.options
             * = LittleShort(mt.options);
             */if (!P_IsDoomnumAllowed(mt.type.toInt())) {
                continue
            }

            // Do spawn all other stuff.
            mobj = DOOM.actions.SpawnMapThing(mt /* , i */)
            if (mobj != null && mobj.info!!.speed == 0) {
                mobjlist[mobjcount++] = mobj
            }
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the data
        /*
         * #ifdef GL_DOOM if (V_GetMode() == VID_MODEGL) { no_overlapped_sprites
         * = true; qsort(mobjlist, mobjcount, sizeof(mobjlist[0]),
         * dicmp_sprite_by_pos); if (!no_overlapped_sprites) { i = 1; while (i <
         * mobjcount) { mobj_t *m1 = mobjlist[i - 1]; mobj_t *m2 = mobjlist[i -
         * 0]; if (GETXY(m1) == GETXY(m2)) { mobj_t *mo = (m1.index < m2.index ?
         * m1 : m2); i++; while (i < mobjcount && GETXY(mobjlist[i]) ==
         * GETXY(m1)) { if (mobjlist[i].index < mo.index) { mo = mobjlist[i]; }
         * i++; } // 'nearest' mo.flags |= MF_FOREGROUND; } i++; } } } #endif
         */
    }

    /*
     * P_IsDoomnumAllowed() Based on code taken from P_LoadThings() in
     * src/p_setup.c Return TRUE if the thing in question is expected to be
     * available in the gamemode used.
     */
    fun P_IsDoomnumAllowed(doomnum: Int): Boolean {
        // Do not spawn cool, new monsters if !commercial
        if (!DOOM.isCommercial()) when (doomnum) {
            64, 65, 66, 67, 68, 69, 71, 84, 88, 89 -> return false
        }
        return true
    }

    //
    // P_LoadLineDefs
    // Also counts secret lines for intermissions.
    // ^^^
    // ??? killough ???
    // Does this mean secrets used to be linedef-based, rather than
    // sector-based?
    //
    // killough 4/4/98: split into two functions, to allow sidedef overloading
    //
    // killough 5/3/98: reformatted, cleaned up
    private fun P_LoadLineDefs(lump: Int) {
        val data: Array<maplinedef_t> // cph - final*
        numlines = DOOM.wadLoader.LumpLength(lump) / maplinedef_t.sizeOf()
        lines = calloc_IfSameLevel(if (isLinesInitialized()) lines else null, numlines, { line_t() })
        data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numlines,
            { maplinedef_t() },
            IntFunction { arrayOfNulls<maplinedef_t>(it) } as IntFunction<Array<maplinedef_t>?>) // cph
        // -
        // wad
        // lump
        // handling
        // updated
        for (i in 0 until numlines) {
            val mld = data[i]
            val ld = lines[i]
            ld.id = i
            var v1: vertex_t
            var v2: vertex_t
            ld.flags = mld.flags
            ld.special = mld.special
            ld.tag = mld.tag
            ld.v1 = vertexes!![mld.v1.code]
            v1 = ld.v1!!
            ld.v2 = vertexes!![mld.v2.code]
            v2 = ld.v2!!
            ld.dx = v2.x - v1.x
            ld.dy = v2.y - v1.y
            // Maes: map value semantics.
            ld.assignVertexValues()

            /*
             * #ifdef GL_DOOM // e6y // Rounding the wall length to the nearest
             * integer // when determining length instead of always rounding
             * down // There is no more glitches on seams between identical
             * textures. ld.texel_length = GetTexelDistance(ld.dx, ld.dy);
             * #endif
             */ld.tranlump = -1 // killough 4/11/98: no translucency by default
            ld.slopetype =
                if (ld.dx == 0) slopetype_t.ST_VERTICAL else if (ld.dy == 0) slopetype_t.ST_HORIZONTAL else if (FixedDiv(
                        ld.dy,
                        ld.dx
                    ) > 0
                ) slopetype_t.ST_POSITIVE else slopetype_t.ST_NEGATIVE
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

            /* calculate sound origin of line to be its midpoint */
            // e6y: fix sound origin for large levels
            // no need for comp_sound test, these are only used when comp_sound
            // = 0
            ld.soundorg = degenmobj_t(
                ld.bbox[BBox.BOXLEFT] / 2 + ld.bbox[BBox.BOXRIGHT] / 2, ld.bbox[BBox.BOXTOP] / 2 + ld.bbox[BBox.BOXBOTTOM] / 2, 0
            )

            // TODO
            // ld.iLineID=i; // proff 04/05/2000: needed for OpenGL
            ld.sidenum[0] = mld.sidenum[0]
            ld.sidenum[1] = mld.sidenum[1]
            run {

                /*
                 * cph 2006/09/30 - fix sidedef errors right away. cph
                 * 2002/07/20 - these errors are fatal if not fixed, so apply
                 * them in compatibility mode - a desync is better than a crash!
                 */for (j in 0..1) {
                if (ld.sidenum[j] != E6Y.NO_INDEX && ld.sidenum[j].code >= numsides) {
                    ld.sidenum[j] = E6Y.NO_INDEX
                    System.err.printf(
                        "P_LoadLineDefs: linedef %d has out-of-range sidedef number\n",
                        numlines - i - 1
                    )
                }
            }

                // killough 11/98: fix common wad errors (missing sidedefs):
                if (ld.sidenum[0] == E6Y.NO_INDEX) {
                    ld.sidenum[0] = 0.toChar() // Substitute dummy sidedef for missing
                    // right side
                    // cph - print a warning about the bug
                    System.err.printf("P_LoadLineDefs: linedef %d missing first sidedef\n", numlines - i - 1)
                }
                if (ld.sidenum[1] == E6Y.NO_INDEX && C2JUtils.flags(
                        ld.flags.toInt(),
                        line_t.ML_TWOSIDED
                    )
                ) {
                    // e6y
                    // ML_TWOSIDED flag shouldn't be cleared for compatibility
                    // purposes
                    // see CLNJ-506.LMP at http://doomedsda.us/wad1005.html
                    // TODO: we don't really care, but still...
                    // if (!demo_compatibility ||
                    // !overflows[OVERFLOW.MISSEDBACKSIDE].emulate)
                    // {
                    ld.flags =
                        (ld.flags.toInt() and line_t.ML_TWOSIDED.inv()).toShort() // Clear 2s flag for missing left
                    // side
                    // }
                    // Mark such lines and do not draw them only in
                    // demo_compatibility,
                    // because Boom's behaviour is different
                    // See OTTAWAU.WAD E1M1, sectors 226 and 300
                    // http://www.doomworld.com/idgames/index.php?id=1651
                    // TODO ehhh?
                    // ld.r_flags = RF_IGNORE_COMPAT;
                    // cph - print a warning about the bug
                    System.err.printf(
                        "P_LoadLineDefs: linedef %d has two-sided flag set, but no second sidedef\n",
                        numlines - i - 1
                    )
                }
            }

            // killough 4/4/98: support special sidedef interpretation below
            // TODO:
            // if (ld.sidenum[0] != NO_INDEX && ld.special!=0)
            // sides[(ld.sidenum[0]<<16)& (0x0000FFFF&ld.sidenum[1])].special =
            // ld.special;
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the lump
    }

    // killough 4/4/98: delay using sidedefs until they are loaded
    // killough 5/3/98: reformatted, cleaned up
    private fun P_LoadLineDefs2(lump: Int) {
        var ld: line_t
        for (i in 0 until numlines) {
            ld = lines[i]
            ld.frontsector = sides[ld.sidenum[0].code].sector // e6y: Can't be
            // NO_INDEX here
            ld.backsector = if (ld.sidenum[1] != E6Y.NO_INDEX) sides[ld.sidenum[1].code].sector else null
            when (ld.special.toInt()) {
                260 -> {}
            }
        }
    }

    //
    // P_LoadSideDefs
    //
    // killough 4/4/98: split into two functions
    private fun P_LoadSideDefs(lump: Int) {
        numsides = DOOM.wadLoader.LumpLength(lump) / mapsidedef_t.sizeOf()
        sides = calloc_IfSameLevel(if(isSidesInitialized()) sides else null, numsides, { side_t() })
    }

    // killough 4/4/98: delay using texture names until
    // after linedefs are loaded, to allow overloading.
    // killough 5/3/98: reformatted, cleaned up
    private fun P_LoadSideDefs2(lump: Int) {
        // cph - final*, wad lump handling updated
        val data = DOOM.wadLoader.CacheLumpNumIntoArray(
            lump,
            numsides,
            { mapsidedef_t() },
            IntFunction { arrayOfNulls<mapsidedef_t>(it) } as IntFunction<Array<mapsidedef_t>?>
        )
        for (i in 0 until numsides) {
            val msd = data[i]
            val sd = sides[i]
            var sec: sector_t?
            sd.textureoffset = msd.textureoffset.toInt() shl FRACBITS
            sd.rowoffset = msd.rowoffset.toInt() shl FRACBITS
            run {
                /*
               * cph 2006/09/30 - catch out-of-range sector numbers; use sector
               * 0 instead
               */
                var sector_num = Char(msd.sector.toUShort())
                if (sector_num.code >= numsectors) {
                    System.err.printf("P_LoadSideDefs2: sidedef %i has out-of-range sector num %u\n", i, sector_num)
                    sector_num = 0.toChar()
                }
                sec = sectors[sector_num.code]
                sd.sector = sec
            }
            when (sd.special) {
                242 -> {}
                260 -> {
                    if (msd.midtexture!!.compareTo("TRANMAP", ignoreCase = true) == 0) {
                        if (DOOM.wadLoader.CheckNumForName(msd.midtexture!!).also { sd.special = it } < 0
                            || DOOM.wadLoader.LumpLength(sd.special) != 65536) {
                            sd.special = 0
                            sd.midtexture = DOOM.textureManager.TextureNumForName(msd.midtexture!!).toShort()
                        } else {
                            sd.special++
                            sd.midtexture = 0
                        }
                    } else {
                        sd.midtexture = 0.also { sd.special = it }.toShort()
                    }
                    sd.toptexture = DOOM.textureManager.TextureNumForName(msd.toptexture!!).toShort()
                    sd.bottomtexture = DOOM.textureManager.TextureNumForName(msd.bottomtexture!!).toShort()
                }
                else -> {
                    // TODO: Boom uses "SafeTextureNumForName" here. Find out what
                    // it does.
                    sd.midtexture = DOOM.textureManager.CheckTextureNumForName(msd.midtexture!!).toShort()
                    sd.toptexture = DOOM.textureManager.CheckTextureNumForName(msd.toptexture!!).toShort()
                    sd.bottomtexture = DOOM.textureManager.CheckTextureNumForName(msd.bottomtexture!!).toShort()
                }
            }
        }
        DOOM.wadLoader.UnlockLumpNum(lump) // cph - release the lump
    }

    //
    // P_LoadBlockMap
    //
    // killough 3/1/98: substantially modified to work
    // towards removing blockmap limit (a wad limitation)
    //
    // killough 3/30/98: Rewritten to remove blockmap limit,
    // though current algorithm is brute-force and unoptimal.
    //
    @Throws(IOException::class)
    private fun P_LoadBlockMap(lump: Int) {
        var count = 0
        if (DOOM.cVarManager.bool(CommandVariable.BLOCKMAP) || DOOM.wadLoader.LumpLength(lump) < 8 || DOOM.wadLoader.LumpLength(
                lump
            ) / 2.also { count = it } >= 0x10000
        ) // e6y
        {
            CreateBlockMap()
        } else {
            // cph - final*, wad lump handling updated
            val wadblockmaplump: CharArray
            val data = DOOM.wadLoader.CacheLumpNum(lump, Defines.PU_LEVEL, DoomBuffer::class.java)!!
            count = DOOM.wadLoader.LumpLength(lump) / 2
            wadblockmaplump = CharArray(count)
            data.setOrder(ByteOrder.LITTLE_ENDIAN)
            data.rewind()
            data.readCharArray(wadblockmaplump, count)
            if (!samelevel) // Reallocate if required.
                blockmaplump = IntArray(count)

            // killough 3/1/98: Expand wad blockmap into larger internal one,
            // by treating all offsets except -1 as unsigned and zero-extending
            // them. This potentially doubles the size of blockmaps allowed,
            // because Doom originally considered the offsets as always signed.
            blockmaplump[0] = wadblockmaplump[0].code
            blockmaplump[1] = wadblockmaplump[1].code
            blockmaplump[2] = wadblockmaplump[2].code and 0xffff
            blockmaplump[3] = wadblockmaplump[3].code and 0xffff
            for (i in 4 until count) {
                val t = wadblockmaplump[i].code.toShort() // killough 3/1/98
                blockmaplump[i] = (if (t.toInt() == -1) -1L else t.toInt() and 0xffff).toInt()
            }
            DOOM.wadLoader.UnlockLumpNum(lump) // cph - unlock the lump
            bmaporgx = blockmaplump[0] shl FRACBITS
            bmaporgy = blockmaplump[1] shl FRACBITS
            bmapwidth = blockmaplump[2]
            bmapheight = blockmaplump[3]

            // haleyjd 03/04/10: check for blockmap problems
            // http://www.doomworld.com/idgames/index.php?id=12935
            if (!VerifyBlockMap(count)) {
                System.err.printf("P_LoadBlockMap: erroneous BLOCKMAP lump may cause crashes.\n")
                System.err.printf("P_LoadBlockMap: use \"-blockmap\" command line switch for rebuilding\n")
            }
        }

        // MAES: blockmap was generated, rather than loaded.
        if (count == 0) {
            count = blockmaplump.size - 4
        }

        // clear out mobj chains - CPhipps - use calloc
        // blocklinks = calloc_IfSameLevel(blocklinks, bmapwidth *
        // bmapheight.mobj_t.);
        // clear out mobj chains
        // ATTENTION! BUG!!!
        // If blocklinks are "cleared" to void -but instantiated- objects,
        // very bad bugs happen, especially the second time a level is
        // re-instantiated.
        // Probably caused other bugs as well, as an extra object would appear
        // in iterators.
        if (blocklinks != null && samelevel) {
            for (i in 0 until bmapwidth * bmapheight) {
                blocklinks!![i] = null
            }
        } else {
            blocklinks = arrayOfNulls(bmapwidth * bmapheight)
        }

        // IMPORTANT MODIFICATION: no need to have both blockmaplump AND
        // blockmap.
        // If the offsets in the lump are OK, then we can modify them (remove 4)
        // and copy the rest of the data in one single data array. This avoids
        // reserving memory for two arrays (we can't simply alias one in Java)
        blockmap = IntArray(blockmaplump.size - 4)
        count = bmapwidth * bmapheight
        // Offsets are relative to START OF BLOCKMAP, and IN SHORTS, not bytes.
        for (i in 0 until blockmaplump.size - 4) {
            // Modify indexes so that we don't need two different lumps.
            // Can probably be further optimized if we simply shift everything
            // backwards.
            // and reuse the same memory space.
            if (i < count) {
                blockmaplump[i] = blockmaplump[i + 4] - 4
            } else {
                blockmaplump[i] = blockmaplump[i + 4]
            }
        }


        // MAES: set blockmapxneg and blockmapyneg
        // E.g. for a full 512x512 map, they should be both
        // -1. For a 257*257, they should be both -255 etc.
        if (bmapwidth > 255) {
            blockmapxneg = bmapwidth - 512
        }
        if (bmapheight > 255) {
            blockmapyneg = bmapheight - 512
        }
        blockmap = blockmaplump
    }

    //
    // P_LoadReject - load the reject table
    //
    private fun P_LoadReject(lumpnum: Int, totallines: Int) {
        // dump any old cached reject lump, then cache the new one
        if (rejectlump != -1) {
            DOOM.wadLoader.UnlockLumpNum(rejectlump)
        }
        rejectlump = lumpnum + ILevelLoader.ML_REJECT
        rejectmatrix = DOOM.wadLoader.CacheLumpNumAsRawBytes(rejectlump, 0)

        // e6y: check for overflow
        // TODO: g.Overflow.RejectOverrun(rejectlump, rejectmatrix,
        // totallines,numsectors);
    }

    //
    // P_GroupLines
    // Builds sector line lists and subsector sector numbers.
    // Finds block bounding boxes for sectors.
    //
    // killough 5/3/98: reformatted, cleaned up
    // cph 18/8/99: rewritten to avoid O(numlines * numsectors) section
    // It makes things more complicated, but saves seconds on big levels
    // figgi 09/18/00 -- adapted for gl-nodes
    // modified to return totallines (needed by P_LoadReject)
    private fun P_GroupLines(): Int {
        var li: line_t
        var sector: sector_t
        var total = numlines

        // figgi
        for (i in 0 until numsubsectors) {
            var seg = subsectors[i].firstline
            subsectors[i].sector = null
            for (j in 0 until subsectors[i].numlines) {
                if (segs[seg].sidedef != null) {
                    subsectors[i].sector = segs[seg].sidedef!!.sector
                    break
                }
                seg++
            }
            if (subsectors[i].sector == null) {
                DOOM.doomSystem.Error("P_GroupLines: Subsector a part of no sector!\n")
            }
        }

        // count number of lines in each sector
        for (i in 0 until numlines) {
            li = lines[i]
            li.frontsector!!.linecount++
            if (li.backsector != null && li.backsector !== li.frontsector) {
                li.backsector!!.linecount++
                total++
            }
        }

        // allocate line tables for each sector
        // e6y: REJECT overrun emulation code
        // moved to P_LoadReject
        for (i in 0 until numsectors) {
            sector = sectors[i]
            sector.lines = GenericCopy.malloc({ line_t() }, sector.linecount)
            // linebuffer += sector.linecount;
            sector.linecount = 0
            BBox.ClearBox(sector.blockbox)
        }

        // Enter those lines
        for (i in 0 until numlines) {
            li = lines[i]
            AddLineToSector(li, li.frontsector!!)
            if (li.backsector != null && li.backsector !== li.frontsector) {
                AddLineToSector(li, li.backsector!!)
            }
        }
        for (i in 0 until numsectors) {
            sector = sectors[i]
            val bbox = sector.blockbox // cph - For convenience, so
            // I can sue the old code unchanged
            var block: Int

            // set the degenmobj_t to the middle of the bounding box
            // TODO
            if (true /* comp[comp_sound] */) {
                sector.soundorg = degenmobj_t(
                    (bbox[BBox.BOXRIGHT] + bbox[BBox.BOXLEFT]) / 2,
                    (bbox[BBox.BOXTOP] + bbox[BBox.BOXBOTTOM]) / 2
                )
            } else {
                // e6y: fix sound origin for large levels
                sector.soundorg = degenmobj_t(
                    bbox[BBox.BOXRIGHT] / 2 + bbox[BBox.BOXLEFT] / 2,
                    bbox[BBox.BOXTOP] / 2 + bbox[BBox.BOXBOTTOM] / 2
                )
            }

            // adjust bounding box to map blocks
            block = getSafeBlockY(bbox[BBox.BOXTOP] - bmaporgy + Limits.MAXRADIUS)
            block = if (block >= bmapheight) bmapheight - 1 else block
            sector.blockbox[BBox.BOXTOP] = block
            block = getSafeBlockY(bbox[BBox.BOXBOTTOM] - bmaporgy - Limits.MAXRADIUS)
            block = if (block < 0) 0 else block
            sector.blockbox[BBox.BOXBOTTOM] = block
            block = getSafeBlockX(bbox[BBox.BOXRIGHT] - bmaporgx + Limits.MAXRADIUS)
            block = if (block >= bmapwidth) bmapwidth - 1 else block
            sector.blockbox[BBox.BOXRIGHT] = block
            block = getSafeBlockX(bbox[BBox.BOXLEFT] - bmaporgx - Limits.MAXRADIUS)
            block = if (block < 0) 0 else block
            sector.blockbox[BBox.BOXLEFT] = block
        }
        return total // this value is needed by the reject overrun emulation
        // code
    }

    //
    // killough 10/98
    //
    // Remove slime trails.
    //
    // Slime trails are inherent to Doom's coordinate system -- i.e. there is
    // nothing that a node builder can do to prevent slime trails ALL of the
    // time,
    // because it's a product of the integer coodinate system, and just because
    // two lines pass through exact integer coordinates, doesn't necessarily
    // mean
    // that they will intersect at integer coordinates. Thus we must allow for
    // fractional coordinates if we are to be able to split segs with node
    // lines,
    // as a node builder must do when creating a BSP tree.
    //
    // A wad file does not allow fractional coordinates, so node builders are
    // out
    // of luck except that they can try to limit the number of splits (they
    // might
    // also be able to detect the degree of roundoff error and try to avoid
    // splits
    // with a high degree of roundoff error). But we can use fractional
    // coordinates
    // here, inside the engine. It's like the difference between square inches
    // and
    // square miles, in terms of granularity.
    //
    // For each vertex of every seg, check to see whether it's also a vertex of
    // the linedef associated with the seg (i.e, it's an endpoint). If it's not
    // an endpoint, and it wasn't already moved, move the vertex towards the
    // linedef by projecting it using the law of cosines. Formula:
    //
    // 2 2 2 2
    // dx x0 + dy x1 + dx dy (y0 - y1) dy y0 + dx y1 + dx dy (x0 - x1)
    // {---------------------------------, ---------------------------------}
    // 2 2 2 2
    // dx + dy dx + dy
    //
    // (x0,y0) is the vertex being moved, and (x1,y1)-(x1+dx,y1+dy) is the
    // reference linedef.
    //
    // Segs corresponding to orthogonal linedefs (exactly vertical or horizontal
    // linedefs), which comprise at least half of all linedefs in most wads,
    // don't
    // need to be considered, because they almost never contribute to slime
    // trails
    // (because then any roundoff error is parallel to the linedef, which
    // doesn't
    // cause slime). Skipping simple orthogonal lines lets the code finish
    // quicker.
    //
    // Please note: This section of code is not interchangable with TeamTNT's
    // code which attempts to fix the same problem.
    //
    // Firelines (TM) is a Rezistered Trademark of MBF Productions
    //
    private fun P_RemoveSlimeTrails() { // killough 10/98
        // Hitlist for vertices
        val hit = BooleanArray(numvertexes)

        // Searchlist for
        for (i in 0 until numsegs) { // Go through each seg
            val l: line_t
            if (segs[i].miniseg == true) { // figgi -- skip minisegs
                return
            }
            l = segs[i].linedef!! // The parent linedef
            if (l.dx != 0 && l.dy != 0) { // We can ignore orthogonal lines
                var v = segs[i].v1
                do {
                    val index = C2JUtils.indexOf(vertexes as Array<Any>, v!!)
                    if (!hit[index]) { // If we haven't processed vertex
                        hit[index] = true // Mark this vertex as processed
                        if (v !== l.v1 && v !== l.v2) { // Exclude endpoints of linedefs
                            // Project the vertex back onto the parent linedef
                            val dx2 =
                                ((l.dx shr FRACBITS) * (l.dx shr FRACBITS)).toLong()
                            val dy2 =
                                ((l.dy shr FRACBITS) * (l.dy shr FRACBITS)).toLong()
                            val dxy =
                                ((l.dx shr FRACBITS) * (l.dy shr FRACBITS)).toLong()
                            val s = dx2 + dy2
                            val x0 = v.x
                            val y0 = v.y
                            val x1 = l.v1!!.x
                            val y1 = l.v1!!.y
                            v.x = (dx2 * x0 + dy2 * x1 + dxy * (y0 - y1) / s).toInt()
                            v.y = (dy2 * y0 + dx2 * y1 + dxy * (x0 - x1) / s).toInt()
                        }
                    } // Obsfucated C contest entry: :)
                } while (v !== segs[i].v2 && segs[i].v2.also { v = it } != null)
            }
            // Assign modified vertex values.
            l.assignVertexValues()
        }
    }

    //
    // P_CheckLumpsForSameSource
    //
    // Are these lumps in the same wad file?
    //
    fun P_CheckLumpsForSameSource(lump1: Int, lump2: Int): Boolean {
        val wad1_index: Int
        val wad2_index: Int
        val wad1: wadfile_info_t?
        val wad2: wadfile_info_t?
        if (C2JUtils.unsigned(lump1) >= C2JUtils.unsigned(DOOM.wadLoader.NumLumps()) || C2JUtils.unsigned(lump2) >= C2JUtils.unsigned(
                DOOM.wadLoader.NumLumps()
            )
        ) {
            return false
        }
        wad1 = DOOM.wadLoader.GetLumpInfo(lump1)!!.wadfile
        wad2 = DOOM.wadLoader.GetLumpInfo(lump2)!!.wadfile
        if (wad1 == null || wad2 == null) {
            return false
        }
        wad1_index = DOOM.wadLoader.GetWadfileIndex(wad1)
        wad2_index = DOOM.wadLoader.GetWadfileIndex(wad2)
        if (wad1_index != wad2_index) {
            return false
        }
        return if (wad1_index < 0 || wad1_index >= DOOM.wadLoader.GetNumWadfiles()) {
            false
        } else (wad2_index < 0 || wad2_index < DOOM.wadLoader.GetNumWadfiles())
    }

    //
    // P_CheckLevelFormat
    //
    // Checking for presence of necessary lumps
    //
    fun P_CheckLevelWadStructure(mapname: String?) {
        var i: Int
        val lumpnum: Int
        if (mapname == null) {
            DOOM.doomSystem.Error("P_SetupLevel: Wrong map name")
            throw NullPointerException()
        }
        lumpnum = DOOM.wadLoader.CheckNumForName(mapname.uppercase(Locale.getDefault()))
        if (lumpnum < 0) {
            DOOM.doomSystem.Error("P_SetupLevel: There is no %s map.", mapname)
        }
        i = ILevelLoader.ML_THINGS + 1
        while (i <= ILevelLoader.ML_SECTORS) {
            if (!P_CheckLumpsForSameSource(lumpnum, lumpnum + i)) {
                DOOM.doomSystem.Error(
                    "P_SetupLevel: Level wad structure is incomplete. There is no %s lump. (%s)",
                    BoomLevelLoader.ml_labels.get(i), DOOM.wadLoader.GetNameForLump(lumpnum)
                )
            }
            i++
        }

        // refuse to load Hexen-format maps, avoid segfaults
        i = lumpnum + ILevelLoader.ML_BLOCKMAP + 1
        if (P_CheckLumpsForSameSource(lumpnum, i)) {
            if (DOOM.wadLoader.GetLumpInfo(i)!!.name!!.compareTo("BEHAVIOR", ignoreCase = true) == 0) {
                DOOM.doomSystem.Error("P_SetupLevel: %s: Hexen format not supported", mapname)
            }
        }
    }

    //
    // P_SetupLevel
    //
    // killough 5/3/98: reformatted, cleaned up
    @Suspicious(CauseOfDesyncProbability.LOW)
    @P_Setup.C(P_Setup.P_SetupLevel)
    @Throws(IOException::class)
    override fun SetupLevel(episode: Int, map: Int, playermask: Int, skill: skill_t?) {
        val lumpname: String
        var lumpnum: Int
        val gl_lumpname: String
        var gl_lumpnum: Int

        // e6y
        DOOM.totallive = 0
        // TODO: transparentpresent = false;

        // R_StopAllInterpolations();
        DOOM.wminfo.maxfrags = 0
        DOOM.totalsecret = DOOM.wminfo.maxfrags
        DOOM.totalitems = DOOM.totalsecret
        DOOM.totalkills = DOOM.totalitems
        DOOM.totallive = DOOM.totalkills
        DOOM.wminfo.partime = 180
        for (i in 0 until Limits.MAXPLAYERS) {
            DOOM.players[i].itemcount = 0
            DOOM.players[i].secretcount = DOOM.players[i].itemcount
            DOOM.players[i].killcount = DOOM.players[i].secretcount
            // TODO DM.players[i].resurectedkillcount = 0;//e6y
        }

        // Initial height of PointOfView
        // will be set by player think.
        DOOM.players[DOOM.consoleplayer].viewz = 1

        // Make sure all sounds are stopped before Z_FreeTags.
        S_Start@ run {
            DOOM.doomSound.Start()
        }
        Z_FreeTags@ // Z_FreeTags(PU_LEVEL, PU_PURGELEVEL-1);
        if (rejectlump != -1) { // cph - unlock the reject table
            DOOM.wadLoader.UnlockLumpNum(rejectlump)
            rejectlump = -1
        }
        P_InitThinkers@ run {
            DOOM.actions.InitThinkers()
        }

        // if working with a devlopment map, reload it
        W_Reload@ // killough 1/31/98: W.Reload obsolete

        // find map name
        if (DOOM.isCommercial()) {
            lumpname = String.format("map%02d", map) // killough 1/24/98:
            // simplify
            gl_lumpname = String.format("gl_map%02d", map) // figgi
        } else {
            lumpname = String.format("E%dM%d", episode, map) // killough
            // 1/24/98:
            // simplify
            gl_lumpname = String.format("GL_E%dM%d", episode, map) // figgi
        }
        W_GetNumForName@ run {
            lumpnum = DOOM.wadLoader.GetNumForName(lumpname)
            gl_lumpnum = DOOM.wadLoader.CheckNumForName(gl_lumpname) // figgi
        }

        // e6y
        // Refuse to load a map with incomplete pwad structure.
        // Avoid segfaults on levels without nodes.
        P_CheckLevelWadStructure(lumpname)
        DOOM.leveltime = 0
        DOOM.totallive = 0

        // note: most of this ordering is important

        // killough 3/1/98: P_LoadBlockMap call moved down to below
        // killough 4/4/98: split load of sidedefs into two parts,
        // to allow texture names to be used in special linedefs

        // figgi 10/19/00 -- check for gl lumps and load them
        P_GetNodesVersion(lumpnum, gl_lumpnum)

        // e6y: speedup of level reloading
        // Most of level's structures now are allocated with PU_STATIC instead
        // of PU_LEVEL
        // It is important for OpenGL, because in case of the same data in
        // memory
        // we can skip recalculation of much stuff
        samelevel = map == current_map && episode == current_episode && nodesVersion == current_nodesVersion
        current_episode = episode
        current_map = map
        current_nodesVersion = nodesVersion
        if (!samelevel) {

            /*
             * if (GL_DOOM){ // proff 11/99: clean the memory from textures etc.
             * gld_CleanMemory(); }
             */

            // free(segs);
            // free(nodes);
            // free(subsectors);
            /*
             * #ifdef GL_DOOM free(map_subsectors); #endif
             */

            // free(blocklinks);
            // free(blockmaplump);

            // free(lines);
            // free(sides);
            // free(sectors);
            // free(vertexes);
        }
        if (nodesVersion > 0) {
            P_LoadVertexes2(
                lumpnum + ILevelLoader.ML_VERTEXES,
                gl_lumpnum + BoomLevelLoader.ML_GL_VERTS
            )
        } else {
            P_LoadVertexes(lumpnum + ILevelLoader.ML_VERTEXES)
        }
        P_LoadSectors(lumpnum + ILevelLoader.ML_SECTORS)
        P_LoadSideDefs(lumpnum + ILevelLoader.ML_SIDEDEFS)
        P_LoadLineDefs(lumpnum + ILevelLoader.ML_LINEDEFS)
        P_LoadSideDefs2(lumpnum + ILevelLoader.ML_SIDEDEFS)
        P_LoadLineDefs2(lumpnum + ILevelLoader.ML_LINEDEFS)

        // e6y: speedup of level reloading
        // Do not reload BlockMap for same level,
        // because in case of big level P_CreateBlockMap eats much time
        if (!samelevel) {
            P_LoadBlockMap(lumpnum + ILevelLoader.ML_BLOCKMAP)
        } else {
            // clear out mobj chains
            if (blocklinks != null && blocklinks!!.size == bmapwidth * bmapheight) {
                for (i in 0 until bmapwidth * bmapheight) {
                    blocklinks!![i] = null
                }
            } else {
                blocklinks = arrayOfNulls(bmapwidth * bmapheight)
                Arrays.setAll(blocklinks, IntFunction<mobj_t> { i: Int -> mobj_t.createOn(DOOM) })
            }
        }
        if (nodesVersion > 0) {
            P_LoadSubsectors(gl_lumpnum + BoomLevelLoader.ML_GL_SSECT)
            P_LoadNodes(gl_lumpnum + BoomLevelLoader.ML_GL_NODES)
            // TODO: P_LoadGLSegs(gl_lumpnum + ML_GL_SEGS);
        } else {
            if (P_CheckForZDoomUncompressedNodes(lumpnum, gl_lumpnum)) {
                P_LoadZNodes(lumpnum + ILevelLoader.ML_NODES, 0)
            } else if (P_CheckForDeePBSPv4Nodes(lumpnum, gl_lumpnum)) {
                P_LoadSubsectors_V4(lumpnum + ILevelLoader.ML_SSECTORS)
                P_LoadNodes_V4(lumpnum + ILevelLoader.ML_NODES)
                P_LoadSegs_V4(lumpnum + ILevelLoader.ML_SEGS)
            } else {
                P_LoadSubsectors(lumpnum + ILevelLoader.ML_SSECTORS)
                P_LoadNodes(lumpnum + ILevelLoader.ML_NODES)
                P_LoadSegs(lumpnum + ILevelLoader.ML_SEGS)
            }
        }

        /*
         * if (GL_DOOM){ map_subsectors = calloc_IfSameLevel(map_subsectors,
         * numsubsectors); }
         */

        // reject loading and underflow padding separated out into new function
        // P_GroupLines modified to return a number the underflow padding needs
        // P_LoadReject(lumpnum, P_GroupLines());
        P_GroupLines()
        super.LoadReject(lumpnum + ILevelLoader.ML_REJECT)
        /**
         * TODO: try to fix, since it seems it doesn't work
         * - Good Sign 2017/05/07
         */

        // e6y
        // Correction of desync on dv04-423.lmp/dv.wad
        // http://www.doomworld.com/vb/showthread.php?s=&postid=627257#post627257
        // if (DoomStatus.compatibility_level>=lxdoom_1_compatibility ||
        // Compatibility.prboom_comp[PC.PC_REMOVE_SLIME_TRAILS.ordinal()].state)
        P_RemoveSlimeTrails() // killough 10/98: remove slime trails from wad

        // Note: you don't need to clear player queue slots --
        // a much simpler fix is in g_game.c -- killough 10/98
        DOOM.bodyqueslot = 0

        /* cph - reset all multiplayer starts */for (i in playerstarts.indices) {
            DOOM.playerstarts[i] = null
        }
        deathmatch_p = 0
        for (i in 0 until Limits.MAXPLAYERS) {
            DOOM.players[i].mo = null
        }
        // TODO: TracerClearStarts();

        // Hmm? P_MapStart();
        P_LoadThings@ run {
            P_LoadThings(lumpnum + ILevelLoader.ML_THINGS)
        }

        // if deathmatch, randomly spawn the active players
        if (DOOM.deathmatch) {
            for (i in 0 until Limits.MAXPLAYERS) {
                if (DOOM.playeringame[i]) {
                    DOOM.players[i].mo = null // not needed? - done before P_LoadThings
                    G_DeathMatchSpawnPlayer@ run {
                        DOOM.DeathMatchSpawnPlayer(i)
                    }
                }
            }
        } else { // if !deathmatch, check all necessary player starts actually exist
            for (i in 0 until Limits.MAXPLAYERS) {
                if (DOOM.playeringame[i] && !C2JUtils.eval(DOOM.players[i].mo)) {
                    DOOM.doomSystem.Error("P_SetupLevel: missing player %d start\n", i + 1)
                }
            }
        }

        // killough 3/26/98: Spawn icon landings:
        // TODO: if (DM.isCommercial())
        // P.SpawnBrainTargets();
        if (!DOOM.isShareware()) {
            // TODO: S.ParseMusInfo(lumpname);
        }

        // clear special respawning que
        DOOM.actions.ClearRespawnQueue()

        // set up world state
        P_SpawnSpecials@ run {
            DOOM.actions.SpawnSpecials()
        }

        // TODO: P.MapEnd();

        // preload graphics
        if (DOOM.precache) {
            /* @SourceCode.Compatible if together */
            R_PrecacheLevel@ run {
                DOOM.textureManager.PrecacheLevel()

                // MAES: thinkers are separate than texture management. Maybe split
                // sprite management as well?
                DOOM.sceneRenderer.PreCacheThinkers()
            }
        }

        /*
         * if (GL_DOOM){ if (V_GetMode() == VID_MODEGL) { // e6y // Do not
         * preprocess GL data during skipping, // because it potentially will
         * not be used. // But preprocessing must be called immediately after
         * stop of skipping. if (!doSkip) { // proff 11/99: calculate all OpenGL
         * specific tables etc. gld_PreprocessLevel(); } } }
         */
        // e6y
        // TODO P_SyncWalkcam(true, true);
        // TODO R_SmoothPlaying_Reset(NULL);
    }

    companion object {
        // //////////////////////////////////////////////////////////////////////////////////////////
        // figgi 08/21/00 -- finalants and globals for glBsp support
        const val gNd2 = 0x32644E67 // figgi -- suppport for new

        // GL_VERT format v2.0
        const val gNd3 = 0x33644E67
        const val gNd4 = 0x34644E67
        const val gNd5 = 0x35644E67
        const val ZNOD = 0x444F4E5A
        const val ZGLN = 0x4E4C475A
        const val GL_VERT_OFFSET = 4
        const val ML_GL_LABEL = 0 // A separator name, GL_ExMx or

        // GL_MAPxx
        const val ML_GL_VERTS = 1 // Extra Vertices
        const val ML_GL_SEGS = 2 // Segs, from linedefs & minisegs
        const val ML_GL_SSECT = 3 // SubSectors, list of segs
        const val ML_GL_NODES = 4 // GL BSP nodes

        //
        // P_CheckForZDoomUncompressedNodes
        // http://zdoom.org/wiki/ZDBSP#Compressed_Nodes
        //
        private const val XNOD = 0x584e4f44
        fun GetDistance(dx: Int, dy: Int): Float {
            val fx: Float = dx.toFloat() / FRACUNIT
            val fy: Float = dy.toFloat() / FRACUNIT
            return Math.sqrt((fx * fx + fy * fy).toDouble()).toFloat()
        }

        fun GetTexelDistance(dx: Int, dy: Int): Float {
            // return (float)((int)(GetDistance(dx, dy) + 0.5f));
            val fx: Float = dx.toFloat() / FRACUNIT
            val fy: Float = dy.toFloat() / FRACUNIT
            return (0.5f + Math.sqrt((fx * fx + fy * fy).toDouble()).toFloat()).toInt().toFloat()
        }

        fun GetOffset(v1: vertex_t, v2: vertex_t): Int {
            val a: Float
            val b: Float
            val r: Int
            a = (v1.x - v2.x) / FRACUNIT.toFloat()
            b = (v1.y - v2.y) / FRACUNIT.toFloat()
            r = (Math.sqrt((a * a + b * b).toDouble()) * FRACUNIT).toInt()
            return r
        }

        private val ml_labels = arrayOf(
            "ML_LABEL",  // A separator, name, ExMx or MAPxx
            "ML_THINGS",  // Monsters, items..
            "ML_LINEDEFS",  // LineDefs, from editing
            "ML_SIDEDEFS",  // SideDefs, from editing
            "ML_VERTEXES",  // Vertices, edited and BSP splits generated
            "ML_SEGS",  // LineSegs, from LineDefs split by BSP
            "ML_SSECTORS",  // SubSectors, list of LineSegs
            "ML_NODES",  // BSP nodes
            "ML_SECTORS",  // Sectors, from editing
            "ML_REJECT",  // LUT, sector-sector visibility
            "ML_BLOCKMAP"
        )
        private const val GL_DOOM = false
    }
}