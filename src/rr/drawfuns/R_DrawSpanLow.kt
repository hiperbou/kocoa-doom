package rr.drawfuns


import i.IDoomSystem

abstract class R_DrawSpanLow<T, V>(
    SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
    columnofs: IntArray, dsvars: SpanVars<T, V>, screen: V, I: IDoomSystem
) : DoomSpanFunction<T, V>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dsvars, screen, I) {
    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, ByteArray?>,
        screen: ByteArray?, I: IDoomSystem
    ) : R_DrawSpanLow<ByteArray?, ByteArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dsvars,
        screen, I
    ) {
        override fun invoke() {
            val ds_source = dsvars.ds_source!!
            val ds_colormap = dsvars.ds_colormap!!
            val ds_xstep = dsvars.ds_xstep
            val ds_ystep = dsvars.ds_ystep
            var f_xfrac = dsvars.ds_xfrac
            var f_yfrac = dsvars.ds_yfrac
            var dest: Int
            var count: Int
            var spot: Int
            if (RANGECHECK) {
                doRangeCheck()
                // dscount++;
            }

            // MAES: count must be performed before shifting.
            count = dsvars.ds_x2 - dsvars.ds_x1

            // Blocky mode, need to multiply by 2.
            dsvars.ds_x1 = dsvars.ds_x1 shl 1
            dsvars.ds_x2 = dsvars.ds_x2 shl 1
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]
            do {
                spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)
                // Lowres/blocky mode does it twice,
                // while scale is adjusted appropriately.
                screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
                screen[dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
                f_xfrac += ds_xstep
                f_yfrac += ds_ystep
            } while (count-- > 0)
        }
    }

    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, ShortArray?>,
        screen: ShortArray?, I: IDoomSystem
    ) : R_DrawSpanLow<ByteArray?, ShortArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dsvars,
        screen, I
    ) {
        override fun invoke() {
            val ds_source = dsvars.ds_source!!
            val ds_colormap = dsvars.ds_colormap!!
            val ds_xstep = dsvars.ds_xstep
            val ds_ystep = dsvars.ds_ystep
            var f_xfrac = dsvars.ds_xfrac
            var f_yfrac = dsvars.ds_yfrac
            var dest: Int
            var count: Int
            var spot: Int
            if (RANGECHECK) {
                doRangeCheck()
                // dscount++;
            }

            // MAES: count must be performed before shifting.
            count = dsvars.ds_x2 - dsvars.ds_x1

            // Blocky mode, need to multiply by 2.
            dsvars.ds_x1 = dsvars.ds_x1 shl 1
            dsvars.ds_x2 = dsvars.ds_x2 shl 1
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]
            do {
                spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)
                // Lowres/blocky mode does it twice,
                // while scale is adjusted appropriately.
                screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
                screen[dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
                f_xfrac += ds_xstep
                f_yfrac += ds_ystep
            } while (count-- > 0)
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, IntArray?>, screen: IntArray?,
        I: IDoomSystem
    ) : R_DrawSpanLow<ByteArray?, IntArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dsvars,
        screen, I
    ) {
        override fun invoke() {
            val ds_source = dsvars.ds_source!!
            val ds_colormap = dsvars.ds_colormap!!
            val ds_xstep = dsvars.ds_xstep
            val ds_ystep = dsvars.ds_ystep
            var f_xfrac = dsvars.ds_xfrac
            var f_yfrac = dsvars.ds_yfrac
            var dest: Int
            var count: Int
            var spot: Int
            if (RANGECHECK) {
                doRangeCheck()
                // dscount++;
            }

            // MAES: count must be performed before shifting.
            count = dsvars.ds_x2 - dsvars.ds_x1

            // Blocky mode, need to multiply by 2.
            dsvars.ds_x1 = dsvars.ds_x1 shl 1
            dsvars.ds_x2 = dsvars.ds_x2 shl 1
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]
            do {
                spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)
                // Lowres/blocky mode does it twice,
                // while scale is adjusted appropriately.
                screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
                screen[dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
                f_xfrac += ds_xstep
                f_yfrac += ds_ystep
            } while (count-- > 0)
        }
    }
}