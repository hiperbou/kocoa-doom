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
 * 
 */
package v.graphics

import v.tables.GammaTables
import java.awt.image.IndexColorModel

/**
 * Refactored and included as the module of new software 2d graphics API
 * - Good Sign 2017/04/14
 *
 * Palettes & colormaps library
 *
 * @author Good Sign
 * @author Maes
 */
interface Palettes : Lights {
    /**
     * Methods to be used by implementor
     */
    /**
     * Perform any action necessary so that palettes get modified according to specified gamma.
     * Consider this a TIME CONSUMING operation, so don't call it unless really necessary.
     *
     * @param gammalevel
     */
    fun setUsegamma(gammalevel: Int)

    /**
     * Getter for gamma level
     *
     * @return
     */
    fun getUsegamma(): Int

    /**
     * Perform any action necessary so that the screen output uses the specified palette
     * Consider this a TIME CONSUMING operation, so don't call it unless really necessary.
     *
     * @param palette
     */
    fun setPalette(palette: Int)

    /**
     * Getter for palette
     *
     * @return
     */
    fun getPalette(): Int

    /**
     * Get the value corresponding to a base color (0-255).
     * Depending on the implementation this might be indexed,
     * RGB etc. Use whenever you need "absolute" colors.
     *
     * @return int
     */
    fun getBaseColor(color: Byte): Int
    fun getBaseColor(color: Int): Int {
        return getBaseColor(color.toByte())
    }

    /**
     * Extracts RGB888 color from an index in the palette
     * @param byte[] pal proper playpal
     * @param int index and index of the color in the palette
     * @return int packed opaque rgb888 pixel
     */
    fun paletteToRGB888(pal: ByteArray, index: Int): Int {
        return toRGB888(pal[index].toInt(), pal[index + 1].toInt(), pal[index + 2].toInt())
    }

    /**
     * Extracts RGB555 color from an index in the palette
     * @param byte[] pal proper playpal
     * @param int index and index of the color in the palette
     * @return int packed rgb555 pixel
     */
    fun paletteToRGB555(pal: ByteArray, index: Int): Short {
        return rgb888to555(pal[index].toInt(), pal[index + 1].toInt(), pal[index + 2].toInt())
    }

    /**
     * Extracts RGB888 color components from an index in the palette to the container
     * @param byte[] pal proper playpal
     * @param byte index and index of the color in the palette
     * @param int[] container to hold individual RGB color components
     * @return int[] the populated container
     */
    fun getPaletteRGB888(pal: ByteArray, index: Int, container: IntArray): IntArray? {
        container[0] = pal[index].toInt() and 0xFF
        container[1] = pal[index + 1].toInt() and 0xFF
        container[2] = pal[index + 2].toInt() and 0xFF
        return container
    }

    /**
     * ColorShiftPalette - lifted from dcolors.c Operates on RGB888 palettes in
     * separate bytes. at shift = 0, the colors are normal at shift = steps, the
     * colors are all the given rgb
     */
    fun ColorShiftPalette(inpal: ByteArray, outpal: ByteArray, r: Int, g: Int, b: Int, shift: Int, steps: Int) {
        var in_p = 0
        var out_p = 0
        for (i in 0 until Palettes.PAL_NUM_COLORS) {
            val dr = r - inpal[in_p + 0]
            val dg = g - inpal[in_p + 1]
            val db = b - inpal[in_p + 2]
            outpal[out_p + 0] = (inpal[in_p + 0] + dr * shift / steps).toByte()
            outpal[out_p + 1] = (inpal[in_p + 1] + dg * shift / steps).toByte()
            outpal[out_p + 2] = (inpal[in_p + 2] + db * shift / steps).toByte()
            in_p += 3
            out_p += 3
        }
    }

    /**
     * Given raw palette data, returns an array with proper TrueColor data
     * @param byte[] pal proper palette
     * @return int[] 32 bit Truecolor ARGB colormap
     */
    fun paletteTrueColor(pal: ByteArray): IntArray {
        val pal888 = IntArray(Palettes.PAL_NUM_COLORS)

        // Initial palette can be neutral or based upon "gamma 0",
        // which is actually a bit biased and distorted
        for (x in 0 until Palettes.PAL_NUM_COLORS) {
            pal888[x] = paletteToRGB888(pal, x * Palettes.PAL_NUM_STRIDES)
        }
        return pal888
    }

    /**
     * Given raw palette data, returns an array with proper HiColor data
     * @param byte[] pal proper palette
     * @return short[] 16 bit HiColor RGB colormap
     */
    fun paletteHiColor(pal: ByteArray): ShortArray? {
        val pal555 = ShortArray(Palettes.PAL_NUM_COLORS)

        // Apply gammas a-posteriori, not a-priori.
        // Initial palette can be neutral or based upon "gamma 0",
        // which is actually a bit biased and distorted
        for (x in 0 until Palettes.PAL_NUM_COLORS) {
            pal555[x] = paletteToRGB555(pal, x * Palettes.PAL_NUM_STRIDES)
        }
        return pal555
    }

    /**
     * Given an array of certain length and raw palette data fills array
     * with IndexColorModel's for each palette. Gammas are applied a-priori
     * @param IndexColorModel[][] cmaps preallocated array, as it is often reconstructed for gamma, do not reallocate it
     * @param byte[] pal proper palette
     * @return the same araay as input, but all values set to new IndexColorModels
     */
    fun cmapIndexed(icms: Array<Array<IndexColorModel?>>, pal: ByteArray): Array<Array<IndexColorModel?>>? {
        val colorsXstride: Int = Palettes.PAL_NUM_COLORS * Palettes.PAL_NUM_STRIDES

        // Now we have our palettes.
        for (i in icms[0].indices) {
            //new IndexColorModel(8, PAL_NUM_COLORS, pal, i * colorsXstride, false);
            icms[0][i] = createIndexColorModel(pal, i * colorsXstride)
        }

        // Wire the others according to the gamma table.
        val tmpcmap = ByteArray(colorsXstride)

        // For each gamma value...
        for (j in 1 until GammaTables.LUT.size) {
            // For each palette
            for (i in 0 until Palettes.NUM_PALETTES) {
                for (k in 0 until Palettes.PAL_NUM_COLORS) {
                    val iXcolorsXstride_plus_StrideXk: Int = i * colorsXstride + Palettes.PAL_NUM_STRIDES * k
                    tmpcmap[3 * k] =
                        GammaTables.LUT[j][0xFF and pal[iXcolorsXstride_plus_StrideXk].toInt()].toByte() // R
                    tmpcmap[3 * k + 1] =
                        GammaTables.LUT[j][0xFF and pal[1 + iXcolorsXstride_plus_StrideXk].toInt()].toByte() // G
                    tmpcmap[3 * k + 2] =
                        GammaTables.LUT[j][0xFF and pal[2 + iXcolorsXstride_plus_StrideXk].toInt()].toByte() // B
                }
                icms[j][i] = createIndexColorModel(tmpcmap, 0)
            }
        }
        return icms
    }

    /**
     * @param byte[] cmap a colormap from which to make color model
     * @param int start position in colormap from which to take PAL_NUM_COLORS
     * @return IndexColorModel
     */
    fun createIndexColorModel(cmap: ByteArray?, start: Int): IndexColorModel? {
        return IndexColorModel(8, Palettes.PAL_NUM_COLORS, cmap, start, false)
    }

    companion object {
        /**
         * Maximum number of colors in palette
         */
        const val PAL_NUM_COLORS = 256

        /**
         * Maximum number of palettes
         * PLAYPAL length / (PAL_NUM_COLORS * PAL_NUM_STRIDES)
         *
         * TODO: think some way of support for future Hexen, Heretic, Strife palettes
         */
        const val NUM_PALETTES = 14

        /**
         * There is 256 colors in standard PALYPAL lump, 3 bytes for each color (RGB value)
         * totaling 256 * 3 = 768 bytes
         */
        const val PAL_NUM_STRIDES = 3
    }
}