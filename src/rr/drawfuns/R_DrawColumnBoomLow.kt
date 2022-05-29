package rr.drawfuns

import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS

/**
 * Adapted from Killough's Boom code. Low-detail variation, no DC_SOURCE
 * optimization.
 *
 * @author admin
 */
abstract class R_DrawColumnBoomLow<T, V>(
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<T, V>?, screen: V,
    I: IDoomSystem
) : DoomColumnFunction<T, V>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    init {
        _flags = DcFlags.LOW_DETAIL
    }

    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>?,
        screen: ShortArray?, I: IDoomSystem
    ) : R_DrawColumnBoomLow<ByteArray?, ShortArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int
            var dest2: Int // killough
            var frac: Int // killough
            val fracstep: Int
            val dc_source_ofs: Int
            count = dcvars.dc_yh - dcvars.dc_yl + 1
            dc_source_ofs = dcvars.dc_source_ofs
            if (count <= 0) // Zero length, column does not exceed a pixel.
                return
            if (RANGECHECK) {
                performRangeCheck()
            }

            // Framebuffer destination address.
            // Use ylookup LUT to avoid multiply with ScreenWidth.
            // Use columnofs LUT for subwindows?
            dest = blockyDest1()
            dest2 = blockyDest2()

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

                        // heightmask is the Tutti-Frutti fix -- killough
                        screen!![dest2] = colormap[0x00FF and source[frac shr FRACBITS].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        if (fracstep.let { frac += it; frac } >= heightmask) frac -= heightmask
                    } while (--count > 0)
                } else {
                    while (count >= 4) // texture height is a power of 2 --
                    // killough
                    {
                        screen!![dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        count -= 4
                    }
                    while (count > 0) {
                        screen!![dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
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
    ) : R_DrawColumnBoomLow<ByteArray?, ByteArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int
            var dest2: Int // killough
            var frac: Int // killough
            val fracstep: Int
            val dc_source_ofs: Int
            count = dcvars.dc_yh - dcvars.dc_yl + 1
            dc_source_ofs = dcvars.dc_source_ofs
            if (count <= 0) // Zero length, column does not exceed a pixel.
                return
            if (RANGECHECK) {
                performRangeCheck()
            }

            // Framebuffer destination address.
            // Use ylookup LUT to avoid multiply with ScreenWidth.
            // Use columnofs LUT for subwindows?
            dest = blockyDest1()
            dest2 = blockyDest2()

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

                        // heightmask is the Tutti-Frutti fix -- killough
                        screen!![dest2] = colormap[0x00FF and source[frac shr FRACBITS].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        if (fracstep.let { frac += it; frac } >= heightmask) frac -= heightmask
                    } while (--count > 0)
                } else {
                    while (count >= 4) // texture height is a power of 2 --
                    // killough
                    {
                        screen!![dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        count -= 4
                    }
                    while (count > 0) {
                        screen!![dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        count--
                    }
                }
            }
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, IntArray?>?,
        screen: IntArray?, I: IDoomSystem
    ) : R_DrawColumnBoomLow<ByteArray?, IntArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            var dest: Int
            var dest2: Int // killough
            var frac: Int // killough
            val fracstep: Int
            val dc_source_ofs: Int
            count = dcvars.dc_yh - dcvars.dc_yl + 1
            dc_source_ofs = dcvars.dc_source_ofs
            if (count <= 0) // Zero length, column does not exceed a pixel.
                return
            if (RANGECHECK) {
                performRangeCheck()
            }

            // Framebuffer destination address.
            // Use ylookup LUT to avoid multiply with ScreenWidth.
            // Use columnofs LUT for subwindows?
            dest = blockyDest1()
            dest2 = blockyDest2()

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

                        // heightmask is the Tutti-Frutti fix -- killough
                        screen!![dest2] = colormap[0x00FF and source[frac shr FRACBITS].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        if (fracstep.let { frac += it; frac } >= heightmask) frac -= heightmask
                    } while (--count > 0)
                } else {
                    while (count >= 4) // texture height is a power of 2 --
                    // killough
                    {
                        screen!![dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        screen[dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        count -= 4
                    }
                    while (count > 0) {
                        screen!![dest2] = colormap[0x00FF and source[dc_source_ofs
                                + (frac shr FRACBITS and heightmask)].toInt()]
                        screen[dest] = screen[dest2]
                        dest += SCREENWIDTH
                        dest2 += SCREENWIDTH
                        frac += fracstep
                        count--
                    }
                }
            }
        }
    }
}