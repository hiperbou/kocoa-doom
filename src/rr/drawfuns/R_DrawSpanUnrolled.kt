package rr.drawfuns

 
import i.IDoomSystem

/**
 * Drawspan loop unrolled by 4. However it has low rendering quality and bad
 * distortion. However it does actually does give a small speed boost (120
 * -> 130 fps with a Mul of 3.0)
 *
 */
abstract class R_DrawSpanUnrolled<T, V>(
    sCREENWIDTH: Int, sCREENHEIGHT: Int, ylookup: IntArray,
    columnofs: IntArray, dsvars: SpanVars<T, V>, screen: V, I: IDoomSystem
) : DoomSpanFunction<T, V>(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I) {
    class HiColor(
        sCREENWIDTH: Int, sCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, ShortArray?>, screen: ShortArray?,
        I: IDoomSystem
    ) : R_DrawSpanUnrolled<ByteArray?, ShortArray?>(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I) {
        override fun invoke() {
            var position: Int
            val step: Int
            val source: ByteArray
            val colormap: ShortArray
            var dest: Int
            var count: Int
            var spot: Int
            var xtemp: Int
            var ytemp: Int
            position = (dsvars.ds_xfrac shl 10 and -0x10000
                    or (dsvars.ds_yfrac shr 6 and 0xffff))
            step = dsvars.ds_xstep shl 10 and -0x10000 or (dsvars.ds_ystep shr 6 and 0xffff)
            source = dsvars.ds_source!!
            colormap = dsvars.ds_colormap!!
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]
            count = dsvars.ds_x2 - dsvars.ds_x1 + 1
            //int rolls = 0;
            while (count >= 4) {
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen!![dest] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 1] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 2] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 3] = colormap[0x00FF and source[spot].toInt()]
                count -= 4
                dest += 4

                // Half-assed attempt to fix precision by forced periodic
                // realignment.

                /*
                * if ((rolls++)%64==0){ position =
                * ((((rolls*4)*ds_xstep+ds_xfrac) << 10) & 0xffff0000) |
                * ((((rolls*4)*ds_ystep+ds_yfrac) >> 6) & 0xffff); }
                */
            }
            while (count > 0) {
                ytemp = position shr 4
                ytemp = ytemp and 4032
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen!![dest++] = colormap[0x00FF and source[spot].toInt()]
                count--
            }
        }
    }

    class Indexed(
        sCREENWIDTH: Int, sCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, ByteArray?>, screen: ByteArray?,
        I: IDoomSystem
    ) : R_DrawSpanUnrolled<ByteArray?, ByteArray?>(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I) {
        override fun invoke() {
            var position: Int
            val step: Int
            val source: ByteArray
            val colormap: ByteArray
            var dest: Int
            var count: Int
            var spot: Int
            var xtemp: Int
            var ytemp: Int
            position = (dsvars.ds_xfrac shl 10 and -0x10000
                    or (dsvars.ds_yfrac shr 6 and 0xffff))
            step = dsvars.ds_xstep shl 10 and -0x10000 or (dsvars.ds_ystep shr 6 and 0xffff)
            source = dsvars.ds_source!!
            colormap = dsvars.ds_colormap!!
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]
            count = dsvars.ds_x2 - dsvars.ds_x1 + 1
            //int rolls = 0;
            while (count >= 4) {
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen!![dest] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 1] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 2] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 3] = colormap[0x00FF and source[spot].toInt()]
                count -= 4
                dest += 4

                // Half-assed attempt to fix precision by forced periodic
                // realignment.

                /*
                * if ((rolls++)%64==0){ position =
                * ((((rolls*4)*ds_xstep+ds_xfrac) << 10) & 0xffff0000) |
                * ((((rolls*4)*ds_ystep+ds_yfrac) >> 6) & 0xffff); }
                */
            }
            while (count > 0) {
                ytemp = position shr 4
                ytemp = ytemp and 4032
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen!![dest++] = colormap[0x00FF and source[spot].toInt()]
                count--
            }
        }
    }

    class TrueColor(
        sCREENWIDTH: Int, sCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dsvars: SpanVars<ByteArray?, IntArray?>, screen: IntArray?,
        I: IDoomSystem
    ) : R_DrawSpanUnrolled<ByteArray?, IntArray?>(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I) {
        override fun invoke() {
            var position: Int
            val step: Int
            val source: ByteArray
            val colormap: IntArray
            var dest: Int
            var count: Int
            var spot: Int
            var xtemp: Int
            var ytemp: Int
            position = (dsvars.ds_xfrac shl 10 and -0x10000
                    or (dsvars.ds_yfrac shr 6 and 0xffff))
            step = dsvars.ds_xstep shl 10 and -0x10000 or (dsvars.ds_ystep shr 6 and 0xffff)
            source = dsvars.ds_source!!
            colormap = dsvars.ds_colormap!!
            dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1]
            count = dsvars.ds_x2 - dsvars.ds_x1 + 1
            //int rolls = 0;
            while (count >= 4) {
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen!![dest] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 1] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 2] = colormap[0x00FF and source[spot].toInt()]
                ytemp = position shr 4
                ytemp = ytemp and 0xfc0
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen[dest + 3] = colormap[0x00FF and source[spot].toInt()]
                count -= 4
                dest += 4

                // Half-assed attempt to fix precision by forced periodic
                // realignment.

                /*
             * if ((rolls++)%64==0){ position =
             * ((((rolls*4)*ds_xstep+ds_xfrac) << 10) & 0xffff0000) |
             * ((((rolls*4)*ds_ystep+ds_yfrac) >> 6) & 0xffff); }
             */
            }
            while (count > 0) {
                ytemp = position shr 4
                ytemp = ytemp and 4032
                xtemp = position ushr 26
                spot = xtemp or ytemp
                position += step
                screen!![dest++] = colormap[0x00FF and source[spot].toInt()]
                count--
            }
        }
    }
}