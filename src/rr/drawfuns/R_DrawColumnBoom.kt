package rr.drawfuns

import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS

/**
 * Adapted from Killough's Boom code. There are optimized as well as low-detail
 * versions of it.
 *
 * @author admin
 */
abstract class R_DrawColumnBoom<T, V>(
    SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
    columnofs: IntArray, dcvars: ColVars<T, V>?, screen: V, I: IDoomSystem
) : DoomColumnFunction<T, V>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>?,
        screen: ShortArray?, I: IDoomSystem
    ) : R_DrawColumnBoom<ByteArray?, ShortArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int // killough
            var frac: Int // killough
            val fracstep: Int
            val dc_source_ofs = dcvars.dc_source_ofs
            count = dcvars.dc_yh - dcvars.dc_yl + 1
            if (count <= 0) // Zero length, column does not exceed a pixel.
                return
            if (RANGECHECK) {
                performRangeCheck()
            }
            dest = computeScreenDest()

            // Determine scaling, which is the only mapping to be done.
            fracstep = dcvars.dc_iscale
            frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery)* fracstep

            // Inner loop that does the actual texture mapping,
            // e.g. a DDA-lile scaling.
            // This is as fast as it gets. (Yeah, right!!! -- killough)
            //
            // killough 2/1/98: more performance tuning
            run {
                val source = dcvars.dc_source!!
                val colormap = dcvars.dc_colormap!!
                var heightmask = dcvars.dc_texheight - 1
                if (dcvars.dc_texheight and heightmask != 0) // not a power of 2
                // --
                // killough
                {
                    heightmask++
                    heightmask = heightmask shl FRACBITS
                    if (frac < 0) while (heightmask.let { frac += it; frac } < 0); else while (frac >= heightmask) frac -= heightmask
                    do {
                        // Re-map color indices from wall texture column
                        // using a lighting/special effects LUT.

                        // heightmask is the Tutti-Frutti fix -- killoughdcvars
                        screen!![dest] = colormap[0x00FF and source[frac shr FRACBITS].toInt()]
                        dest += SCREENWIDTH
                        if (fracstep.let { frac += it; frac } >= heightmask) frac -= heightmask
                    } while (--count > 0)
                } else {
                    while (count >= 4) // texture height is a power of 2 --
                    // killough
                    {
                        // System.err.println(dest);
                        screen!![dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        count -= 4
                    }
                    while (count > 0) {
                        try {
                            screen!![dest] = colormap[0x00FF and source[dc_source_ofs
                                    + (frac shr FRACBITS and heightmask)].toInt()]
                        } catch (e: Exception) {
                            System.err.printf(
                                "%s %s %x %x %x\n", colormap,
                                source, dc_source_ofs, frac, heightmask
                            )
                        }
                        dest += SCREENWIDTH
                        frac += fracstep
                        count--
                    }
                }
            }
        }
    }

    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray,  dcvars: ColVars<ByteArray?, ByteArray?>?, screen: ByteArray?,
        I: IDoomSystem
    ) : R_DrawColumnBoom<ByteArray?, ByteArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int // killough
            var frac: Int // killough
            val fracstep: Int
            val dc_source_ofs = dcvars.dc_source_ofs
            count = dcvars.dc_yh - dcvars.dc_yl + 1
            if (count <= 0) // Zero length, column does not exceed a pixel.
                return
            if (RANGECHECK) {
                performRangeCheck()
            }
            dest = computeScreenDest()

            // Determine scaling, which is the only mapping to be done.
            fracstep = dcvars.dc_iscale
            frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery)* fracstep

            // Inner loop that does the actual texture mapping,
            // e.g. a DDA-lile scaling.
            // This is as fast as it gets. (Yeah, right!!! -- killough)
            //
            // killough 2/1/98: more performance tuning
            run {
                val source = dcvars.dc_source!!
                val colormap = dcvars.dc_colormap!!
                var heightmask = dcvars.dc_texheight - 1
                if (dcvars.dc_texheight and heightmask != 0) // not a power of 2
                // --
                // killough
                {
                    heightmask++
                    heightmask = heightmask shl FRACBITS
                    if (frac < 0) while (heightmask.let { frac += it; frac } < 0); else while (frac >= heightmask) frac -= heightmask
                    do {
                        // Re-map color indices from wall texture column
                        // using a lighting/special effects LUT.

                        // heightmask is the Tutti-Frutti fix -- killoughdcvars
                        screen!![dest] = colormap[0x00FF and source[frac shr FRACBITS].toInt()]
                        dest += SCREENWIDTH
                        if (fracstep.let { frac += it; frac } >= heightmask) frac -= heightmask
                    } while (--count > 0)
                } else {
                    while (count >= 4) // texture height is a power of 2 --
                    // killough
                    {
                        // System.err.println(dest);
                        screen!![dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        count -= 4
                    }
                    while (count > 0) {
                        try {
                            screen!![dest] = colormap[0x00FF and source[dc_source_ofs
                                    + (frac shr FRACBITS and heightmask)].toInt()]
                        } catch (e: Exception) {
                            System.err.printf(
                                "%s %s %x %x %x\n", colormap,
                                source, dc_source_ofs, frac, heightmask
                            )
                        }
                        dest += SCREENWIDTH
                        frac += fracstep
                        count--
                    }
                }
            }
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, IntArray?>?, screen: IntArray?,
        I: IDoomSystem
    ) : R_DrawColumnBoom<ByteArray?, IntArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int // killough
            var frac: Int // killough
            val fracstep: Int
            val dc_source_ofs = dcvars.dc_source_ofs
            count = dcvars.dc_yh - dcvars.dc_yl + 1
            if (count <= 0) // Zero length, column does not exceed a pixel.
                return
            if (RANGECHECK) {
                performRangeCheck()
            }
            dest = computeScreenDest()

            // Determine scaling, which is the only mapping to be done.
            fracstep = dcvars.dc_iscale
            frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * fracstep

            // Inner loop that does the actual texture mapping,
            // e.g. a DDA-lile scaling.
            // This is as fast as it gets. (Yeah, right!!! -- killough)
            //
            // killough 2/1/98: more performance tuning
            run {
                val source = dcvars.dc_source!!
                val colormap = dcvars.dc_colormap!!
                var heightmask = dcvars.dc_texheight - 1
                if (dcvars.dc_texheight and heightmask != 0) // not a power of 2
                // --
                // killough
                {
                    heightmask++
                    heightmask = heightmask shl FRACBITS
                    if (frac < 0) while (heightmask.let { frac += it; frac } < 0); else while (frac >= heightmask) frac -= heightmask
                    do {
                        // Re-map color indices from wall texture column
                        // using a lighting/special effects LUT.

                        // heightmask is the Tutti-Frutti fix -- killoughdcvars
                        screen!![dest] = colormap[0x00FF and source[frac shr FRACBITS].toInt()]
                        dest += SCREENWIDTH
                        if (fracstep.let { frac += it; frac } >= heightmask) frac -= heightmask
                    } while (--count > 0)
                } else {
                    while (count >= 4) // texture height is a power of 2 --
                    // killough
                    {
                        // System.err.println(dest);
                        screen!![dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        screen[dest] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        dest += SCREENWIDTH
                        frac += fracstep
                        count -= 4
                    }
                    while (count > 0) {
                        try {
                            screen!![dest] = colormap[0x00FF and source[dc_source_ofs
                                    + (frac shr FRACBITS and heightmask)].toInt()]
                        } catch (e: Exception) {
                            System.err.printf(
                                "%s %s %x %x %x\n", colormap,
                                source, dc_source_ofs, frac, heightmask
                            )
                        }
                        dest += SCREENWIDTH
                        frac += fracstep
                        count--
                    }
                }
            }
        }
    }
}