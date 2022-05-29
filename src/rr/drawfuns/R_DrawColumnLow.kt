package rr.drawfuns

import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS

class R_DrawColumnLow(
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>,
    screen: ShortArray?, I: IDoomSystem
) : DoomColumnFunction<ByteArray?, ShortArray?>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    init {
        _flags = DcFlags.LOW_DETAIL
    }

    override fun invoke() {
        var count: Int
        // MAES: were pointers. Of course...
        var dest: Int
        var dest2: Int
        val dc_source = dcvars.dc_source!!
        val dc_colormap = dcvars.dc_colormap!!
        val dc_source_ofs = dcvars.dc_source_ofs
        // Maes: fixed_t never used as such.
        var frac: Int
        val fracstep: Int
        count = dcvars.dc_yh - dcvars.dc_yl

        // Zero length.
        if (count < 0) return
        if (RANGECHECK) {
            performRangeCheck()
        }

        // The idea is to draw more than one pixel at a time.
        dest = blockyDest1()
        dest2 = blockyDest2()
        fracstep = dcvars.dc_iscale
        frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * fracstep
        // int spot=(frac >>> FRACBITS) & 127;
        do {

            // Hack. Does not work correctly.
            // MAES: that's good to know.
            screen!![dest2] = dc_colormap[0x00FF and dc_source[dc_source_ofs
                    + (frac ushr FRACBITS and 127)].toInt()]
            screen[dest] = screen[dest2]
            dest += SCREENWIDTH
            dest2 += SCREENWIDTH
            frac += fracstep
        } while (count-- != 0)
    }
}