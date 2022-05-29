/*
 * Copyright (C) 2017 Good Sign
 * Copyright (C) 2022 hiperbou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package v.tables

import m.Settings
import mochadoom.Engine
import v.graphics.Colors
import v.graphics.Lights
import v.graphics.Palettes
import java.util.*

/**
 * Colormap-friendly vanilla-like BlurryMap for HiColor && TrueColor modes
 * (though it shares plain "BLURRYMAP" for Indexed too)
 *
 * DOOM's colormap #6 was deciphered to be actually applying greyscale averaging filter.
 * So, the vanilla effect is something like "n% darker and greyscale", where n% varies
 * I think I've succeeded in replicating it for real color modes
 * - Good Sign 2017/04/15
 *
 * Now should be 100%, I've accounted for shift of number of generated lights for 24 bit color
 *
 * @author Good Sign
 */
class BlurryTable : FuzzMix, Colors {
    /**
     * Indexed LUT, e.g. classic "BLURRYMAP" (unaffected)
     */
    private val LUT_idx: ByteArray?
    private val LUT_r8: ByteArray?
    private val LUT_g8: ByteArray?
    private val LUT_b8: ByteArray?
    private val LUT_a8: ByteArray?
    private val LUT_r5: ByteArray?
    private val LUT_g5: ByteArray?
    private val LUT_b5: ByteArray?
    private val semiTranslucent: Boolean =
        Engine.getConfig().equals(Settings.semi_translucent_fuzz, java.lang.Boolean.TRUE)
    private val fuzzMix: Boolean = Engine.getConfig().equals(Settings.fuzz_mix, java.lang.Boolean.TRUE)

    /**
     * Only support indexed "BLURRYMAP" with indexed colorMap
     * @param colorMap
     */
    constructor(colorMap: Array<ByteArray>) {
        LUT_b5 = null
        LUT_g5 = null
        LUT_r5 = null
        LUT_b8 = null
        LUT_g8 = null
        LUT_r8 = null
        LUT_a8 = null
        LUT_idx = colorMap[Lights.COLORMAP_BLURRY]
    }

    /**
     * HiColor BlurryTable will only support int[][] colormap
     * @param liteColorMaps
     */
    constructor(liteColorMaps: Array<ShortArray>) {
        LUT_b5 = ByteArray(32)
        LUT_g5 = ByteArray(32)
        LUT_r5 = ByteArray(32)
        LUT_b8 = null
        LUT_g8 = null
        LUT_r8 = null
        LUT_a8 = null
        LUT_idx = null
        /**
         * Prepare to sort colors - we will be using the ratio that is next close to apply for current color
         */
        val sortedRatios = TreeMap<Short, Float> { rgb555_1: Short?, rgb555_2: Short? ->
            CompareColors555(
                rgb555_1!!, rgb555_2!!
            )
        }
        for (i in 0 until Palettes.PAL_NUM_COLORS) {
            // first get "BLURRYMAP" color components
            val blurryColor = getRGB555(liteColorMaps[Lights.COLORMAP_BLURRY][i].toInt(), IntArray(3))
            // then gen color components from unmodified (fixed) palette
            val fixedColor = getRGB555(liteColorMaps[Lights.COLORMAP_FIXED][i].toInt(), IntArray(3))
            // make grayscale avegrage (or what you set in cfg) colors out of these components
            val avgColor: Short = GreyscaleFilter.grey555(blurryColor[0], blurryColor[1], blurryColor[2])
            val avgOrig: Short = GreyscaleFilter.grey555(fixedColor[0], fixedColor[1], fixedColor[2])
            // get grayscale color components
            val blurryAvg = getRGB555(avgColor.toInt(), IntArray(3))
            val fixedAvg = getRGB555(avgOrig.toInt(), IntArray(3))

            // now, calculate the ratios
            val ratioR = if (fixedAvg[0] > 0) blurryAvg[0] / fixedAvg[0].toFloat() else 0.0f
            val ratioG = if (fixedAvg[1] > 0) blurryAvg[1] / fixedAvg[1].toFloat() else 0.0f
            val ratioB = if (fixedAvg[2] > 0) blurryAvg[2] / fixedAvg[2].toFloat() else 0.0f

            // best ratio is weighted towards red and blue, but should not be multiplied or it will be too dark
            val bestRatio: Float = GreyscaleFilter.component(
                ratioR,
                ratioG,
                ratioB
            ) //ratioR * ratioR * ratioG * ratioB * ratioB;

            // associate normal color from colormaps avegrage with this ratio
            sortedRatios[avgOrig] = bestRatio
        }

        // now we have built our sorted maps, time to calculate color component mappings
        for (i in 0..0x1F) {
            val rgb555 = toRGB555(i, i, i)
            // now the best part - approximation. we just pick the closest grayscale color ratio
            val ratio = sortedRatios.floorEntry(rgb555).value
            LUT_b5[i] = ((i * ratio).toInt() and 0x1F).toByte()
            LUT_g5[i] = LUT_b5[i]
            LUT_r5[i] = LUT_g5[i]
        }
        // all done
    }

    /**
     * TrueColor BlurryTable will only support int[][] colormap
     * @param liteColorMaps
     */
    constructor(liteColorMaps: Array<IntArray>) {
        LUT_b5 = null
        LUT_g5 = null
        LUT_r5 = null
        LUT_a8 = ByteArray(256)
        LUT_b8 = ByteArray(256)
        LUT_g8 = ByteArray(256)
        LUT_r8 = ByteArray(256)
        LUT_idx = null
        /**
         * Prepare to sort colors - we will be using the ratio that is next close to apply for current color
         */
        val sortedRatios = TreeMap<Int, Float> { rgb888_1: Int?, rgb888_2: Int? ->
            CompareColors888(
                rgb888_1!!, rgb888_2!!
            )
        }
        for (i in 0 until Palettes.PAL_NUM_COLORS) {
            // first get "BLURRYMAP" color components. 24 bit lighting is richer (256 vs 32) so we need to multiply
            val blurryColor = getRGB888(liteColorMaps[Lights.COLORMAP_BLURRY shl 3][i], IntArray(3))
            // then gen color components from unmodified (fixed) palette
            val fixedColor = getRGB888(liteColorMaps[Lights.COLORMAP_FIXED][i], IntArray(3))
            // make grayscale avegrage (or what you set in cfg) colors out of these components
            val avgColor: Int = GreyscaleFilter.grey888(blurryColor[0], blurryColor[1], blurryColor[2])
            val avgOrig: Int = GreyscaleFilter.grey888(fixedColor[0], fixedColor[1], fixedColor[2])
            // get grayscale color components
            val blurryAvg = getRGB888(avgColor, IntArray(3))
            val fixedAvg = getRGB888(avgOrig, IntArray(3))

            // now, calculate the ratios
            val ratioR = if (fixedAvg[0] > 0) blurryAvg[0] / fixedAvg[0].toFloat() else 0.0f
            val ratioG = if (fixedAvg[1] > 0) blurryAvg[1] / fixedAvg[1].toFloat() else 0.0f
            val ratioB = if (fixedAvg[2] > 0) blurryAvg[2] / fixedAvg[2].toFloat() else 0.0f

            // weight ratio towards red and blue and multiply to make darker
            val bestRatio: Float = GreyscaleFilter.component(
                ratioR,
                ratioG,
                ratioB
            ) //ratioR * ratioR * ratioG * ratioB * ratioB;

            // associate normal color from colormaps avegrage with this ratio
            sortedRatios[avgOrig] = bestRatio
        }

        // now we have built our sorted maps, time to calculate color component mappings
        for (i in 0..0xFF) {
            val rgb = toRGB888(i, i, i)
            // now the best part - approximation. we just pick the closest grayscale color ratio
            val ratio = sortedRatios.floorEntry(rgb).value
            LUT_b8[i] = ((i * ratio).toInt() and 0xFF).toByte()
            LUT_g8[i] = LUT_b8[i]
            LUT_r8[i] = LUT_g8[i]
            // for alpha it is different: we use the same ratio as for greyscale color, but the base alpha is min 50%
            LUT_a8[i] = (ratio * (Math.max(i, 0x7F) / 0xf) * 0xFF).toInt().toByte() //TODO: check this int to byte conversion
        }
        // all done
    }

    /**
     * For indexes
     */
    fun computePixel(pixel: Byte): Byte {
        return LUT_idx!![pixel.toInt() and 0xFF]
    }

    /**
     * For HiColor pixels
     */
    fun computePixel(pixel: Short): Short {
        if (fuzzMix) { // if blurry feature enabled, everything else does not apply
            return fuzzMixHi(pixel)
        }
        val rgb = getRGB555(pixel.toInt(), IntArray(4))
        return toRGB555(LUT_r5!![rgb[0]].toInt(), LUT_g5!![rgb[1]].toInt(), LUT_b5!![rgb[2]].toInt())
    }

    /**
     * In high detail mode in AlphaTrueColor color mode will compute special greyscale-to-ratio translucency
     */
    fun computePixel(pixel: Int): Int {
        if (fuzzMix) { // if blurry feature enabled, everything else does not apply
            return fuzzMixTrue(pixel)
        }
        if (!semiTranslucent) {
            return computePixelFast(pixel)
        }
        val argb = getARGB8888(pixel, IntArray(4))
        // the alpha from previous frame would stay until the pixel will not belong to FUZZ holder
        argb[0] = Math.min(argb[0], GreyscaleFilter.component(argb[1], argb[2], argb[3]))
        return toARGB8888(
            LUT_a8!![argb[0]].toInt(),
            LUT_r8!![argb[1]].toInt(),
            LUT_g8!![argb[2]].toInt(),
            LUT_b8!![argb[3]].toInt()
        )
    }

    /**
     * For low detail mode, do not compute translucency
     */
    fun computePixelFast(pixel: Int): Int {
        if (fuzzMix) { // if blurry feature enabled, everything else does not apply
            return fuzzMixTrueLow(pixel)
        }
        val rgb = getRGB888(pixel, IntArray(3))
        return -0x1000000 + (toRGB888(
            LUT_r8!![rgb[0]].toInt(),
            LUT_g8!![rgb[1]].toInt(),
            LUT_b8!![rgb[2]].toInt()
        ) and 0xFFFFFF)
    }
}