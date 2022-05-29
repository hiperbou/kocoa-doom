package rr.drawfuns

import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS

/**
 * Adapted from Killough's Boom code. There are optimized as well as
 * low-detail versions of it.
 *
 * @author admin
 */
/**
 * A column is a vertical slice/span from a wall texture that, given the
 * DOOM style restrictions on the view orientation, will always have
 * constant z depth. Thus a special case loop for very fast rendering can be
 * used. It has also been used with Wolfenstein 3D. MAES: this is called
 * mostly from inside Draw and from an external "Renderer"
 */
class R_DrawColumn(
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>,
    screen: ShortArray?, I: IDoomSystem
) : DoomColumnFunction<ByteArray?, ShortArray?>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    override fun invoke() {
        var count: Int
        // byte* dest;
        var dest: Int // As pointer
        // fixed_t
        var frac: Int
        val fracstep: Int
        val colmask: Byte = 127
        count = dcvars.dc_yh - dcvars.dc_yl
        // How much we should draw
        // count = Math.min(dc_yh - dc_yl,dc_source.length-dc_source_ofs-1);
        // colmask = (byte) Math.min(dc_source.length-dc_source_ofs-1,127);

        // Zero length, column does not exceed a pixel.
        if (count <= 0) return
        if (RANGECHECK) {
            if (dcvars.dc_x >= SCREENWIDTH || dcvars.dc_yl < 0 || dcvars.dc_yh >= SCREENHEIGHT) I!!.Error(
                "R_DrawColumn: %i to %i at %i",
                dcvars.dc_yl,
                dcvars.dc_yh,
                dcvars.dc_x
            )
        }

        // Trying to draw a masked column? Then something gross will happen.
        /*
         * if (count>=dc_source.length-dc_source_ofs) { int
         * diff=count-(dc_source.length-dc_source_ofs);
         * count=dc_source.length-dc_source_ofs-1; dc_source_ofs=0;
         * //dc_yl=dc_yh-count; gross=true; }
         */dest = computeScreenDest()

        // Determine scaling,
        // which is the only mapping to be done.
        fracstep = dcvars.dc_iscale
        frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * fracstep

        // Inner loop that does the actual texture mapping,
        // e.g. a DDA-lile scaling.
        // This is as fast as it gets.
        do {
            /*
             * Re-map color indices from wall texture column using a
             * lighting/special effects LUT. 
             * Q: Where is "*dest"supposed to be pointing?
             * A: it's pointing inside screen[0] (set long before we came here)
             * dc_source is a pointer to a decompressed column's data.
             * Oh Woe if it points at non-pixel data.
             */
            // if (gross) System.out.println(frac >> FRACBITS);
            screen!![dest] =
                dcvars.dc_colormap!![0x00FF and dcvars.dc_source!![dcvars.dc_source_ofs + (frac shr FRACBITS)
                        and colmask.toInt()].toInt()]

            /*
             * MAES: ok, so we have (from inside out):
             * 
             * frac is a fixed-point number representing a pointer inside a
             * column. It gets shifted to an integer, and AND-ed with 128
             * (this causes vertical column tiling).
             */
            dest += SCREENWIDTH
            frac += fracstep
        } while (count-- > 0)
    }
}