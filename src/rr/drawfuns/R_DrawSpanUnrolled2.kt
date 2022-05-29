package rr.drawfuns


import i.IDoomSystem

/** An unrolled (4x) rendering loop with full quality  */ // public final int dumb=63 * 64;
class R_DrawSpanUnrolled2(
    sCREENWIDTH: Int, sCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dsvars: SpanVars<ByteArray?, ShortArray?>,
    screen: ShortArray?, I: IDoomSystem
) : DoomSpanFunction<ByteArray?, ShortArray?>(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I) {
    override fun invoke() {
        val ds_source = dsvars.ds_source!!
        val ds_colormap = dsvars.ds_colormap!!
        val ds_xstep = dsvars.ds_xstep
        val ds_ystep = dsvars.ds_ystep
        var f_xfrac: Int // fixed_t
        var f_yfrac: Int // fixed_t
        var dest: Int
        var count: Int
        var spot: Int

		// System.out.println("R_DrawSpan: "+ds_x1+" to "+ds_x2+" at "+
		// ds_y);

		if (RANGECHECK) {
		    doRangeCheck();
			// dscount++;
		}

		f_xfrac = dsvars.ds_xfrac;
		f_yfrac = dsvars.ds_yfrac;

		dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1];

		count = dsvars.ds_x2 - dsvars.ds_x1;
		while (count >= 4) {
			// Current texture index in u,v.
			spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)

            // Lookup pixel from flat texture tile,
            // re-index using light/colormap.
            screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]

            // Next step in u,v.
            f_xfrac += ds_xstep
            f_yfrac += ds_ystep

			// UNROLL 2
			spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)
			screen[dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
			f_xfrac += ds_xstep;
			f_yfrac += ds_ystep;

            // UNROLL 3
            spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)
            screen[dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
            f_xfrac += ds_xstep
            f_yfrac += ds_ystep

            // UNROLL 4
            spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)
            screen[dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]
            f_xfrac += ds_xstep
            f_yfrac += ds_ystep
            count -= 4
        }
        while (count > 0) {
            // Current texture index in u,v.
            spot = ((f_yfrac shr (16 - 6)) and (63 * 64)) + ((f_xfrac shr 16) and 63)

            // Lookup pixel from flat texture tile,
            // re-index using light/colormap.
            screen!![dest++] = ds_colormap[0x00FF and ds_source[spot].toInt()]

            // Next step in u,v.
            f_xfrac += ds_xstep
            f_yfrac += ds_ystep
            count--
        }
    }
}