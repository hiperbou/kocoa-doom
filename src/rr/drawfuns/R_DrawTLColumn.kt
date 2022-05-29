package rr.drawfuns

import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS

class R_DrawTLColumn(
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>?,
    screen: ShortArray?, I: IDoomSystem
) : DoomColumnFunction<ByteArray?, ShortArray?>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    init {
        _flags = DcFlags.TRANSPARENT
    }

    override fun invoke() {
        var count: Int
        var dest: Int // killough
        var frac: Int // killough
        val fracstep: Int
        val dc_source_ofs = dcvars.dc_source_ofs
        val tranmap = dcvars.tranmap!!
        count = dcvars.dc_yh - dcvars.dc_yl + 1
        if (count <= 0) // Zero length, column does not exceed a pixel.
            return
        if (RANGECHECK) {
            performRangeCheck()
        }

        // Framebuffer destination address.
        // Use ylookup LUT to avoid multiply with ScreenWidth.
        // Use columnofs LUT for subwindows?
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
            if (dcvars.dc_texheight and heightmask != 0) // not a power of 2 --
            // killough
            {
                heightmask++
                heightmask = heightmask shl FRACBITS
                if (frac < 0) while (heightmask.let { frac += it; frac } < 0); else while (frac >= heightmask) frac -= heightmask
                do {
                    // Re-map color indices from wall texture column
                    // using a lighting/special effects LUT.
                    // heightmask is the Tutti-Frutti fix -- killough
                    screen!![dest] = tranmap[(0xFF00
                            and (screen[dest].toInt() shl 8)) or (0x00FF and colormap[0x00FF and source[dc_source_ofs
                            + (frac shr FRACBITS and heightmask)].toInt()].toInt())].toShort()
                    dest += SCREENWIDTH
                    if (fracstep.let { frac += it; frac } >= heightmask) frac -= heightmask
                } while (--count > 0)
            } else {
                while (4.let { count -= it; count } >= 0) // texture height is a power of 2
                // -- killough
                {
                    // screen[dest] =
                    // main_tranmap[0xFF00&(screen[dest]<<8)|(0x00FF&colormap[0x00FF&source[dc_source_ofs+((frac>>FRACBITS)
                    // & heightmask)]])];
                    screen!![dest] = tranmap[(0xFF00
                            and (screen[dest].toInt() shl 8)) or (0x00FF and colormap[0x00FF and source[dc_source_ofs
                            + (frac shr FRACBITS and heightmask)].toInt()].toInt())].toShort()
                    dest += SCREENWIDTH
                    frac += fracstep
                    screen[dest] = tranmap[(0xFF00
                            and (screen[dest].toInt() shl 8)) or (0x00FF and colormap[0x00FF and source[dc_source_ofs
                            + (frac shr FRACBITS and heightmask)].toInt()].toInt())].toShort()
                    dest += SCREENWIDTH
                    frac += fracstep
                    screen[dest] = tranmap[(0xFF00
                            and (screen[dest].toInt() shl 8)) or (0x00FF and colormap[0x00FF and source[dc_source_ofs
                            + (frac shr FRACBITS and heightmask)].toInt()].toInt())].toShort()
                    dest += SCREENWIDTH
                    frac += fracstep
                    screen[dest] = tranmap[(0xFF00
                            and (screen[dest].toInt() shl 8)) or (0x00FF and colormap[0x00FF and source[dc_source_ofs
                            + (frac shr FRACBITS and heightmask)].toInt()].toInt())].toShort()
                    dest += SCREENWIDTH
                    frac += fracstep
                }
                if (count and 1 != 0) screen!![dest] = tranmap[(0xFF00
                        and (screen[dest].toInt() shl 8)) or (0x00FF and colormap[0x00FF and source[dc_source_ofs
                        + (frac shr FRACBITS and heightmask)].toInt()].toInt())].toShort()
            }
        }
    }
}