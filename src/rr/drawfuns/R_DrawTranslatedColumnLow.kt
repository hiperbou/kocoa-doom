package rr.drawfuns

import i.IDoomSystem
import data.Tables.BITS32;
import data.Tables.finecosine;
import data.Tables.finesine;
import data.info.mobjinfo;
import data.mobjtype_t;
import doom.SourceCode.angle_t;
import m.fixed_t.Companion.FRACBITS;
import m.fixed_t.Companion.FRACUNIT;
import m.fixed_t.Companion.FixedMul;
import m.fixed_t.Companion.FixedDiv
import p.MapUtils.AproxDistance;
import p.mobj_t;
import utils.C2JUtils.eval;
import doom.player_t;
import doom.weapontype_t;
import m.fixed_t.Companion.MAPFRACUNIT;
import doom.SourceCode
import java.nio.ByteBuffer
import m.BBox
import doom.DoomMain

abstract class R_DrawTranslatedColumnLow<T, V>(
    SCREENWIDTH: Int, SCREENHEIGHT: Int,
    ylookup: IntArray, columnofs: IntArray, dcvars: ColVars<T, V>?, screen: V,
    I: IDoomSystem
) : DoomColumnFunction<T, V>(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I) {
    init {
        _flags = DcFlags.TRANSLATED or DcFlags.LOW_DETAIL
    }

    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, ShortArray?>?,
        screen: ShortArray?, I: IDoomSystem
    ) : R_DrawTranslatedColumnLow<ByteArray?, ShortArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            // MAES: you know the deal by now...
            var dest: Int
            var dest2: Int
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

            // The idea is to draw more than one pixel at a time.
            dest = blockyDest1()
            dest2 = blockyDest2()

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
                screen!![dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
            } while (4.let { count -= it; count } > 4)
            if (count > 0) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
            } while (count-- != 0)
        }
    }

    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray,  dcvars: ColVars<ByteArray?, ByteArray?>?, screen: ByteArray?,
        I: IDoomSystem
    ) : R_DrawTranslatedColumnLow<ByteArray?, ByteArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            // MAES: you know the deal by now...
            var dest: Int
            var dest2: Int
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

            // The idea is to draw more than one pixel at a time.
            dest = blockyDest1()
            dest2 = blockyDest2()

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
                screen!![dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
            } while (4.let { count -= it; count } > 4)
            if (count > 0) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
            } while (count-- != 0)
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<ByteArray?, IntArray?>?, screen: IntArray?,
        I: IDoomSystem
    ) : R_DrawTranslatedColumnLow<ByteArray?, IntArray?>(
        SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars,
        screen, I
    ) {
        override fun invoke() {
            var count: Int
            // MAES: you know the deal by now...
            var dest: Int
            var dest2: Int
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

            // The idea is to draw more than one pixel at a time.
            dest = blockyDest1()
            dest2 = blockyDest2()

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
                screen!![dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
                screen[dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
            } while (4.let { count -= it; count } > 4)
            if (count > 0) do {
                // Translation tables are used
                // to map certain colorramps to other ones,
                // used with PLAY sprites.
                // Thus the "green" ramp of the player 0 sprite
                // is mapped to gray, red, black/indigo.
                screen!![dest2] = dc_colormap[0x00FF and dc_translation[0xFF and dc_source[dc_source_ofs
                        + (frac shr FRACBITS)].toInt()].toInt()]
                screen[dest] = screen[dest2]
                dest += SCREENWIDTH
                dest2 += SCREENWIDTH
                frac += fracstep
            } while (count-- != 0)
        }
    }
}