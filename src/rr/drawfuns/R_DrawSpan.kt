package rr.drawfuns


import i.IDoomSystem

/**
 * Draws the actual span.
 *
 * ds_frac, ds_yfrac, ds_x2, ds_x1, ds_xstep and ds_ystep must be set.
 *
 */
abstract class R_DrawSpan<T, V>(
    sCREENWIDTH: Int, sCREENHEIGHT: Int, ylookup: IntArray,
    columnofs: IntArray, dsvars: SpanVars<T, V>, screen: V, I: IDoomSystem
) : DoomSpanFunction<T, V>(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I) {
    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, ByteArray?>,
        screen: ByteArray?, I: IDoomSystem
    ) : R_DrawSpan<ByteArray?, ByteArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dsvars,
        screen, I
    ) {
        override fun invoke() {
            var f_xfrac: Int // fixed_t
            var f_yfrac: Int // fixed_t
            var dest: Int
            var count: Int
            var spot: Int
            val ds_colormap = dsvars.ds_colormap!!
            val ds_source = dsvars.ds_source!!

            // System.out.println("R_DrawSpan: "+ds_x1+" to "+ds_x2+" at "+
            // ds_y);
            if (RANGECHECK) {
                doRangeCheck()
                // dscount++;
            }
            f_xfrac = dsvars.ds_xfrac
            f_yfrac = dsvars.ds_yfrac
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]

            // We do not check for zero spans here?
            count = dsvars.ds_x2 - dsvars.ds_x1
            do {
                // Current texture index in u,v.
                spot = ((f_yfrac shr (16 - 6) and (63 * 64)) + ((f_xfrac shr 16) and 63))

                // Lookup pixel from flat texture tile,
                // re-index using light/colormap.
                screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]

                // Next step in u,v.
                f_xfrac += dsvars.ds_xstep
                f_yfrac += dsvars.ds_ystep
            } while (count-- > 0)
        }
    }

    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, ShortArray?>,
        screen: ShortArray?, I: IDoomSystem
    ) : R_DrawSpan<ByteArray?, ShortArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dsvars,
        screen, I
    ) {
        override fun invoke() {
            var f_xfrac: Int // fixed_t
            var f_yfrac: Int // fixed_t
            var dest: Int
            var count: Int
            var spot: Int
            val ds_colormap = dsvars.ds_colormap!!
            val ds_source = dsvars.ds_source!!

            // System.out.println("R_DrawSpan: "+ds_x1+" to "+ds_x2+" at "+
            // ds_y);
            if (RANGECHECK) {
                doRangeCheck()
                // dscount++;
            }
            f_xfrac = dsvars.ds_xfrac
            f_yfrac = dsvars.ds_yfrac
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]

            // We do not check for zero spans here?
            count = dsvars.ds_x2 - dsvars.ds_x1
            do {
                // Current texture index in u,v.
                spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)

                // Lookup pixel from flat texture tile,
                // re-index using light/colormap.
                screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]

                // Next step in u,v.
                f_xfrac += dsvars.ds_xstep
                f_yfrac += dsvars.ds_ystep
            } while (count-- > 0)
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, IntArray?>, screen: IntArray?,
        I: IDoomSystem
    ) : R_DrawSpan<ByteArray?, IntArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dsvars,
        screen, I
    ) {
        override fun invoke() {
            var f_xfrac: Int // fixed_t
            var f_yfrac: Int // fixed_t
            var dest: Int
            var count: Int
            var spot: Int
            val ds_colormap = dsvars.ds_colormap!!
            val ds_source = dsvars.ds_source!!

            // System.out.println("R_DrawSpan: "+ds_x1+" to "+ds_x2+" at "+
            // ds_y);
            if (RANGECHECK) {
                doRangeCheck()
                // dscount++;
            }
            f_xfrac = dsvars.ds_xfrac
            f_yfrac = dsvars.ds_yfrac
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]

            // We do not check for zero spans here?
            count = dsvars.ds_x2 - dsvars.ds_x1
            do {
                // Current texture index in u,v.
                spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)

                // Lookup pixel from flat texture tile,
                // re-index using light/colormap.
                screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]

                // Next step in u,v.
                f_xfrac += dsvars.ds_xstep
                f_yfrac += dsvars.ds_ystep
            } while (count-- > 0)
        }
    }
}