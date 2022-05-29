package rr.drawfuns

import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS

abstract class R_DrawTranslatedColumn<T, V>(
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<T, V>?, screen: V,
    I: IDoomSystem
) : DoomColumnFunction<T, V>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    init {
        _flags = DcFlags.TRANSLATED
    }

    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>?,
        screen: ShortArray?, I: IDoomSystem
    ) : R_DrawTranslatedColumn<ByteArray?, ShortArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            // MAES: you know the deal by now...
            var dest: Int
            var frac: Int
            val fracstep: Int
            val dc_source_ofs = dcvars.dc_source_ofs
            val dc_source = dcvars.dc_source!!
            val dc_colormap = dcvars.dc_colormap!!
            val dc_translation = dcvars.dc_translation!!
            count = dcvars.dc_yh - dcvars.dc_yl
            if (count < 0) return
            if (RANGECHECK) {
                super.performRangeCheck()
            }

            // WATCOM VGA specific.
            /*
             * Keep for fixing. if (detailshift) { if (dc_x & 1) outp
             * (SC_INDEX+1,12); else outp (SC_INDEX+1,3); dest = destview +
             * dc_yl*80 + (dc_x>>1); } else { outp (SC_INDEX+1,1<<(dc_x&3));
             * dest = destview + dc_yl*80 + (dc_x>>2); }
             */

            // FIXME. As above.
            dest = computeScreenDest()

            // Looks familiar.
            fracstep = dcvars.dc_iscale
            frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * fracstep

            // Here we do an additional index re-mapping.
            // Maes: Unroll by 4
            if (count >= 4) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
            } while (4.let { count -= it; count } > 4)
            if (count > 0) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
            } while (count-- != 0)
        }
    }

    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray,  dcvars: ColVars<ByteArray?, ByteArray?>?, screen: ByteArray?,
        I: IDoomSystem
    ) : R_DrawTranslatedColumn<ByteArray?, ByteArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            // MAES: you know the deal by now...
            var dest: Int
            var frac: Int
            val fracstep: Int
            val dc_source_ofs = dcvars.dc_source_ofs
            val dc_source = dcvars.dc_source!!
            val dc_colormap = dcvars.dc_colormap!!
            val dc_translation = dcvars.dc_translation!!
            count = dcvars.dc_yh - dcvars.dc_yl
            if (count < 0) return
            if (RANGECHECK) {
                super.performRangeCheck()
            }

            // WATCOM VGA specific.
            /*
             * Keep for fixing. if (detailshift) { if (dc_x & 1) outp
             * (SC_INDEX+1,12); else outp (SC_INDEX+1,3); dest = destview +
             * dc_yl*80 + (dc_x>>1); } else { outp (SC_INDEX+1,1<<(dc_x&3));
             * dest = destview + dc_yl*80 + (dc_x>>2); }
             */

            // FIXME. As above.
            dest = computeScreenDest()

            // Looks familiar.
            fracstep = dcvars.dc_iscale
            frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * fracstep

            // Here we do an additional index re-mapping.
            // Maes: Unroll by 4
            if (count >= 4) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
            } while (4.let { count -= it; count } > 4)
            if (count > 0) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
            } while (count-- != 0)
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, IntArray?>?, screen: IntArray?,
        I: IDoomSystem
    ) : R_DrawTranslatedColumn<ByteArray?, IntArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            // MAES: you know the deal by now...
            var dest: Int
            var frac: Int
            val fracstep: Int
            val dc_source_ofs = dcvars.dc_source_ofs
            val dc_source = dcvars.dc_source!!
            val dc_colormap = dcvars.dc_colormap!!
            val dc_translation = dcvars.dc_translation!!
            count = dcvars.dc_yh - dcvars.dc_yl
            if (count < 0) return
            if (RANGECHECK) {
                super.performRangeCheck()
            }

            // WATCOM VGA specific.
            /*
             * Keep for fixing. if (detailshift) { if (dc_x & 1) outp
             * (SC_INDEX+1,12); else outp (SC_INDEX+1,3); dest = destview +
             * dc_yl*80 + (dc_x>>1); } else { outp (SC_INDEX+1,1<<(dc_x&3));
             * dest = destview + dc_yl*80 + (dc_x>>2); }
             */

            // FIXME. As above.
            dest = computeScreenDest()

            // Looks familiar.
            fracstep = dcvars.dc_iscale
            frac = dcvars.dc_texturemid + (dcvars.dc_yl - dcvars.centery) * fracstep

            // Here we do an additional index re-mapping.
            // Maes: Unroll by 4
            if (count >= 4) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
                screen[dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
            } while (4.let { count -= it; count } > 4)
            if (count > 0) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                dest += SCREENWIDTH
                frac += fracstep
            } while (count-- != 0)
        }
    }
}