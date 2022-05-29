/*-----------------------------------------------------------------------------
//
// Copyright (C) 1993-1996 Id Software, Inc.
// Copyright (C) 2017 Good Sign
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
// From r_draw.c
//-----------------------------------------------------------------------------*/
package rr.drawfuns

 
import i.IDoomSystem
import v.tables.BlurryTable

/**
 * fuzzMix was preserved, but moved to its own interface.
 * Implemented by BlurryTable if cfg option fuzz_mix is set
 * - Good Sign 2017/04/16
 *
 * Framebuffer postprocessing. Creates a fuzzy image by copying pixels from
 * adjacent ones to left and right. Used with an all black colormap, this
 * could create the SHADOW effect, i.e. spectres and invisible players.
 */
abstract class R_DrawFuzzColumn<T, V>(
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<T, V>?,
    screen: V, I: IDoomSystem
) : DoomColumnFunction<T, V>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    constructor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int,
        ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<T, V>?,
        screen: V, I: IDoomSystem, BLURRY_MAP: BlurryTable?
    ) : this(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
        blurryTable = BLURRY_MAP
    }

    protected var fuzzpos = 0
    protected val FUZZTABLE: Int

    //
    // Spectre/Invisibility.
    //
    protected val FUZZOFF: Int
    protected val fuzzoffset: IntArray

    init {
        _flags = DcFlags.FUZZY
        FUZZOFF = SCREENWIDTH

        // Recompute fuzz table
        fuzzoffset = intArrayOf(
            FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF,
            FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF,
            FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
            -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
            FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF,
            FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF,
            -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
            FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF
        )
        FUZZTABLE = fuzzoffset.size
    }

    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray,  dcvars: ColVars<ByteArray?, ByteArray?>?,
        screen: ByteArray?, I: IDoomSystem, BLURRY_MAP: BlurryTable?
    ) : R_DrawFuzzColumn<ByteArray?, ByteArray?>(
        SCREENWIDTH,
        SCREENHEIGHT,
        ylookup,
        columnofs,
        dcvars,
        screen,
        I,
        BLURRY_MAP
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int

            // Adjust borders. Low...
            if (dcvars.dc_yl == 0) dcvars.dc_yl = 1

            // .. and high.
            if (dcvars.dc_yh == dcvars.viewheight - 1) dcvars.dc_yh = dcvars.viewheight - 2
            count = dcvars.dc_yh - dcvars.dc_yl

            // Zero length.
            if (count < 0) return
            if (RANGECHECK) {
                performRangeCheck()
            }

            // Does not work with blocky mode.
            dest = computeScreenDest()

            // Looks like an attempt at dithering,
            // using the colormap #6 (of 0-31, a bit
            // brighter than average).
            val blurryTable = blurryTable!!
            if (count > 4) { // MAES: unroll by 4
                do {
                    // Lookup framebuffer, and retrieve
                    // a pixel that is either one column
                    // left or right of the current one.
                    // Add index from colormap to index.
                    screen!![dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])

                    // Clamp table lookup index.
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                } while (4.let { count -= it; count } > 4)
            }
            if (count > 0) {
                do {
                    // Lookup framebuffer, and retrieve
                    // a pixel that is either one column
                    // left or right of the current one.
                    // Add index from colormap to index.
                    screen!![dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])

                    // Clamp table lookup index.
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                } while (count-- > 0)
            }
        }
    }

    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>?,
        screen: ShortArray?, I: IDoomSystem, BLURRY_MAP: BlurryTable?
    ) : R_DrawFuzzColumn<ByteArray?, ShortArray?>(
        SCREENWIDTH,
        SCREENHEIGHT,
        ylookup,
        columnofs,
        dcvars,
        screen,
        I,
        BLURRY_MAP
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int

            // Adjust borders. Low...
            if (dcvars.dc_yl == 0) dcvars.dc_yl = 1

            // .. and high.
            if (dcvars.dc_yh == dcvars.viewheight - 1) dcvars.dc_yh = dcvars.viewheight - 2
            count = dcvars.dc_yh - dcvars.dc_yl

            // Zero length.
            if (count < 0) return
            if (RANGECHECK) {
                super.performRangeCheck()
            }

            // Does not work with blocky mode.
            dest = computeScreenDest()

            // Looks like an attempt at dithering,
            // using the colormap #6 (of 0-31, a bit
            // brighter than average).
            val blurryTable = blurryTable!!
            if (count > 4) { // MAES: unroll by 4
                do {
                    // Lookup framebuffer, and retrieve
                    // a pixel that is either one column
                    // left or right of the current one.
                    // Add index from colormap to index.
                    screen!![dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])

                    // Clamp table lookup index.
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                } while (4.let { count -= it; count } > 4)
                if (count > 0) {
                    do {
                        screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])

                        // Clamp table lookup index.
                        if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                        dest += SCREENWIDTH
                    } while (count-- > 0)
                }
            }
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, IntArray?>?,
        screen: IntArray?, I: IDoomSystem, BLURRY_MAP: BlurryTable?
    ) : R_DrawFuzzColumn<ByteArray?, IntArray?>(
        SCREENWIDTH,
        SCREENHEIGHT,
        ylookup,
        columnofs,
        dcvars,
        screen,
        I,
        BLURRY_MAP
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int

            // Adjust borders. Low...
            if (dcvars.dc_yl == 0) dcvars.dc_yl = 1

            // .. and high.
            if (dcvars.dc_yh == dcvars.viewheight - 1) dcvars.dc_yh = dcvars.viewheight - 2
            count = dcvars.dc_yh - dcvars.dc_yl

            // Zero length.
            if (count < 0) return
            if (RANGECHECK) {
                performRangeCheck()
            }

            // Does not work with blocky mode.
            dest = computeScreenDest()

            // Looks like an attempt at dithering,
            // using the colormap #6 (of 0-31, a bit
            // brighter than average).
            val blurryTable = blurryTable!!
            if (count > 4) { // MAES: unroll by 4
                do {
                    // Lookup framebuffer, and retrieve
                    // a pixel that is either one column
                    // left or right of the current one.
                    // Add index from colormap to index.
                    screen!![dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])

                    // Clamp table lookup index.
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                    screen[dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                } while (4.let { count -= it; count } > 4)
            }
            if (count > 0) {
                do {
                    // Lookup framebuffer, and retrieve
                    // a pixel that is either one column
                    // left or right of the current one.
                    // Add index from colormap to index.
                    screen!![dest] = blurryTable.computePixel(screen[dest + fuzzoffset[fuzzpos]])

                    // Clamp table lookup index.
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0
                    dest += SCREENWIDTH
                } while (count-- > 0)
            }
        }
    }
}