package rr.drawfuns

import i.IDoomSystem

/**
 * EI VITTU, this gives a clean 25% boost. Da fack...
 *
 *
 * @author admin
 */
class R_DrawColumnUnrolled  /*
		 * That's shit, doesn't help. private final int
		 * SCREENWIDTH2=SCREENWIDTH*2; private final int
		 * SCREENWIDTH3=SCREENWIDTH*3; private final int
		 * SCREENWIDTH4=SCREENWIDTH*4; private final int
		 * SCREENWIDTH5=SCREENWIDTH*5; private final int
		 * SCREENWIDTH6=SCREENWIDTH*6; private final int
		 * SCREENWIDTH7=SCREENWIDTH*7; private final int
		 * SCREENWIDTH8=SCREENWIDTH*8;
		 */
    (
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>,
    screen: ShortArray?, I: IDoomSystem
) : DoomColumnFunction<ByteArray?, ShortArray?>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    override fun invoke() {
        var count: Int
        var dest: Int
        val source: ByteArray
        val colormap: ShortArray
        val dc_source_ofs = dcvars.dc_source_ofs

        // These are all "unsigned". Watch out for bit shifts!
        var frac: Int
        val fracstep: Int
        val fracstep2: Int
        val fracstep3: Int
        val fracstep4: Int
        count = dcvars.dc_yh - dcvars.dc_yl + 1
        source = dcvars.dc_source!!
        // dc_source_ofs+=15; // ???? WHY
        colormap = dcvars.dc_colormap!!
        dest = computeScreenDest()
        fracstep = dcvars.dc_iscale shl 9
        frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * dcvars.dc_iscale shl 9
        fracstep2 = fracstep + fracstep
        fracstep3 = fracstep2 + fracstep
        fracstep4 = fracstep3 + fracstep
        while (count > 8) {
            screen!![dest] = colormap[0x00FF and source[dc_source_ofs + frac ushr 25].toInt()]
            screen[dest + SCREENWIDTH] = colormap[0x00FF and source[(dc_source_ofs
                    + (frac + fracstep)) ushr 25].toInt()]
            screen[dest + SCREENWIDTH * 2] = colormap[0x00FF and source[(dc_source_ofs
                    + (frac + fracstep2)) ushr 25].toInt()]
            screen[dest + SCREENWIDTH * 3] = colormap[0x00FF and source[(dc_source_ofs
                    + (frac + fracstep3)) ushr 25].toInt()]
            frac += fracstep4
            screen[dest + SCREENWIDTH * 4] = colormap[0x00FF and source[(dc_source_ofs
                    + frac) ushr 25].toInt()]
            screen[dest + SCREENWIDTH * 5] = colormap[0x00FF and source[(dc_source_ofs
                    + (frac + fracstep)) ushr 25].toInt()]
            screen[dest + SCREENWIDTH * 6] = colormap[0x00FF and source[(dc_source_ofs
                    + (frac + fracstep2)) ushr 25].toInt()]
            screen[dest + SCREENWIDTH * 7] = colormap[0x00FF and source[(dc_source_ofs
                    + (frac + fracstep3)) ushr 25].toInt()]
            frac += fracstep4
            dest += SCREENWIDTH * 8
            count -= 8
        }
        while (count > 0) {
            screen!![dest] = colormap[0x00FF and source[dc_source_ofs + frac ushr 25].toInt()]
            dest += SCREENWIDTH
            frac += fracstep
            count--
        }
    }
}