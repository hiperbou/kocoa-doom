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
package v.graphics


import v.graphics.*
import v.tables.GreyscaleFilter

/**
 * This package provides methods to dynamically generate lightmaps
 * They are intended to be used instead of COLORMAP lump to
 * compute sector brightness
 *
 * @author Good Sign
 * @author John Carmack
 * @author Velktron
 */
interface Lights : Colors {
    /**
     * Builds TrueColor lights based on standard COLORMAP lump in DOOM format
     * Currently only supports lightmap manipulation, but does not change colors
     * for hacked COLORMAP lumps
     *
     * Color indexes in colormaps on darker color levels point to less matching
     * colors so only the direction of increase/decrease of lighting is actually
     * used from COLORMAP lump. Everything else is computed based on PLAYPAL
     *
     * @param int[] palette A packed RGB888 256-entry int palette
     * @param byete[][] colormap read from COLORMAP lump
     * @author Good Sign
     */
    fun BuildLights24(palette: IntArray, colormap: Array<ByteArray>): Array<IntArray>? {
        val targetColormap = Array(
            Math.max(
                colormap.size,
                Lights.COLORMAP_STD_LENGTH_15
            ) - Lights.COLORMAP_LIGHTS_15 + Lights.COLORMAP_LIGHTS_24
        ) { IntArray(Palettes.PAL_NUM_COLORS) }

        // init operation containers
        val color0 = IntArray(3)
        val color1 = IntArray(3)
        val color2 = IntArray(3)
        val ratio0 = FloatArray(3)
        var weight = 0.0f
        /**
         * Fixed color map - just copy it, only translating palette to real color
         * It is presumably the brightest colormap, but maybe not: we shall check weight of color ratios
         */
        for (i in 0 until Palettes.PAL_NUM_COLORS) {
            targetColormap[0][i] = palette[colormap[0][i].toInt() and 0xFF]
            getRGB888(targetColormap[0][i], color0)
            getRGB888(palette[i], color1)
            // calculate color ratio
            ColorRatio(color0, color1, ratio0)
            // add average ratio to the weight
            weight += GreyscaleFilter.component(ratio0[0], ratio0[1], ratio0[2])
        }

        // initialize ratio to relate weight with number of colors, with default PLAYPAL should always be 1.0f
        var currentLightRatio = Math.min(weight / Palettes.PAL_NUM_COLORS, 1.0f)

        // [1 .. 255]: all colormaps except 1 fixed, 1 inverse and 1 unused
        for (i in 1 until Lights.COLORMAP_LIGHTS_24) {
            // [1 .. 31] the index of the colormap to be target for gradations: max 31 of ceiling of i / 8
            val div = Math.ceil(i.toDouble() / 8).toInt()
            val target = Math.min(div, Lights.COLORMAP_LIGHTS_15 - 1)
            val remainder = if (div < Lights.COLORMAP_LIGHTS_15) i % 8 else 0
            val gradient = 1.0f - remainder * 0.125f

            // calculate weight again for each colormap
            weight = 0.0f
            for (j in 0 until Palettes.PAL_NUM_COLORS) {
                // translated indexed color from wad-read colormap i at position j
                getRGB888(palette[colormap[target][j].toInt() and 0xFF], color0)
                // translated indexed color from our previous generated colormap at position j
                getRGB888(targetColormap[i - 1][j], color1)
                // calculate color ratio
                ColorRatio(color0, color1, ratio0)
                // add average ratio to the weight
                weight += GreyscaleFilter.component(ratio0[0], ratio0[1], ratio0[2])
                // to detect which color we will use, get the fixed colormap one
                getRGB888(targetColormap[0][j], color2)
                /**
                 * set our color using smooth TrueColor formula: we well use the brighter color as a base
                 * since the brighter color simply have more information not omitted
                 * if we are going up in brightness, not down, it will be compensated by ratio
                 */
                targetColormap[i][j] = toRGB888(
                    sigmoidGradient(
                        color1[0],
                        (Math.max(color2[0], color0[0]) * currentLightRatio + 0.5).toInt(),
                        gradient
                    ),
                    sigmoidGradient(
                        color1[1],
                        (Math.max(color2[1], color0[1]) * currentLightRatio + 0.5).toInt(),
                        gradient
                    ),
                    sigmoidGradient(
                        color1[2],
                        (Math.max(color2[2], color0[2]) * currentLightRatio + 0.5).toInt(),
                        gradient
                    )
                )
            }

            // now detect if we are lightening or darkening
            currentLightRatio += if (weight > Palettes.PAL_NUM_COLORS) Lights.LIGHT_INCREMENT_RATIO_24 else -Lights.LIGHT_INCREMENT_RATIO_24
        }

        // copy all other parts of colormap
        var i: Int = Lights.COLORMAP_LIGHTS_24
        var j: Int = Lights.COLORMAP_LIGHTS_15
        while (j < colormap.size) {
            CopyMap24(targetColormap[i], palette, colormap[j])
            ++i
            ++j
        }
        return targetColormap
    }

    /**
     * RF_BuildLights lifted from dcolors.c
     *
     * Used to compute extended-color colormaps even in absence of the
     * COLORS15 lump. Must be recomputed if gamma levels change, since
     * they actually modify the RGB envelopes.
     *
     * Variation that produces TrueColor lightmaps
     *
     * @param int[] palette A packed RGB888 256-entry int palette
     */
    fun BuildLights24(palette: IntArray): Array<IntArray>? {
        val targetColormap =
            Array(Lights.COLORMAP_STD_LENGTH_24) { IntArray(Palettes.PAL_NUM_COLORS) }
        val palColor = IntArray(3)

        // Don't repeat work more then necessary - loop first over colors, not lights
        for (c in 0 until Palettes.PAL_NUM_COLORS) {
            getRGB888(palette[c], palColor)
            for (l in 0 until Lights.COLORMAP_LIGHTS_24) {
                // Full-quality truecolor.
                targetColormap[l][c] = toRGB888(
                    AddLight8(palColor[0], l),  // R
                    AddLight8(palColor[1], l),  // G
                    AddLight8(palColor[2], l) // B
                )
            }

            // Special map for invulnerability. Do not waste time, build it right now
            BuildSpecials24(targetColormap[Lights.COLORMAP_LIGHTS_24], palColor, c)
        }
        return targetColormap
    }

    /**
     * RF_BuildLights lifted from dcolors.c
     *
     * Used to compute extended-color colormaps even in absence of the
     * COLORS15 lump. Must be recomputed if gamma levels change, since
     * they actually modify the RGB envelopes.
     *
     * @param int[] palette A packed RGB888 256-entry int palette
     * @param byte[][] colormap, if supplied it will be used to translate the lights,
     * the inverse colormap will be translated from it and all unused copied.
     * - Good Sign 2017/04/17
     */
    fun BuildLights15(palette: IntArray, colormaps: Array<ByteArray>): Array<ShortArray>? {
        val targetColormap = Array(
            Math.max(
                colormaps.size,
                Lights.COLORMAP_STD_LENGTH_15
            )
        ) { ShortArray(Palettes.PAL_NUM_COLORS) }
        for (c in colormaps.indices) {
            CopyMap15(targetColormap[c], palette, colormaps[c])
        }
        return targetColormap
    }

    /**
     * RF_BuildLights lifted from dcolors.c
     *
     * Used to compute extended-color colormaps even in absence of the
     * COLORS15 lump. Must be recomputed if gamma levels change, since
     * they actually modify the RGB envelopes.
     *
     * @param int[] palette A packed RGB888 256-entry int palette
     */
    fun BuildLights15(palette: IntArray): Array<ShortArray>? {
        val targetColormap =
            Array(Lights.COLORMAP_STD_LENGTH_15) { ShortArray(Palettes.PAL_NUM_COLORS) }
        val palColor = IntArray(3)

        // Don't repeat work more then necessary - loop first over colors, not lights
        for (c in 0 until Palettes.PAL_NUM_COLORS) {
            getRGB888(palette[c], palColor)
            for (l in 0 until Lights.COLORMAP_LIGHTS_15) {
                // RGB555 for HiColor, eight times less smooth then TrueColor version
                targetColormap[l][c] = toRGB555(
                    AddLight5(palColor[0], l),  // R
                    AddLight5(palColor[1], l),  // G
                    AddLight5(palColor[2], l) // B
                )
            }

            // Special map for invulnerability. Do not waste time, build it right now
            BuildSpecials15(targetColormap[Lights.COLORMAP_LIGHTS_15], palColor, c)
        }
        return targetColormap
    }

    /**
     * RF_BuildLights lifted from dcolors.c
     *
     * Used to compute extended-color colormaps even in absence of the
     * COLORMAP lump. Must be recomputed if gamma levels change, since
     * they actually modify the RGB envelopes.
     *
     * @param int[] palette A packed RGB888 256-entry int palette
     * @return this concrete one builds Indexed colors. Maybe I would regret it
     * - Good Sign 2017/04/19
     */
    fun BuildLightsI(palette: IntArray): Array<ByteArray>? {
        val targetColormap =
            Array(Lights.COLORMAP_STD_LENGTH_15) { ByteArray(Palettes.PAL_NUM_COLORS) }
        val palColor = IntArray(3)

        // Don't repeat work more then necessary - loop first over colors, not lights
        for (c in 0 until Palettes.PAL_NUM_COLORS) {
            getRGB888(palette[c], palColor)
            for (l in 0 until Lights.COLORMAP_LIGHTS_15) {
                // RGB555 for HiColor, eight times less smooth then TrueColor version
                targetColormap[l][c] = BestColor(
                    AddLightI(palColor[0], l),  // R
                    AddLightI(palColor[1], l),  // G
                    AddLightI(palColor[2], l),  // B
                    palette, 0, Palettes.PAL_NUM_COLORS - 1
                ).toByte()
            }

            // Special map for invulnerability. Do not waste time, build it right now
            BuildSpecialsI(targetColormap[Lights.COLORMAP_LIGHTS_15], palColor, palette, c)
        }
        return targetColormap
    }

    /**
     * @param c8 one rgb888 color component value
     * @param light light level to add
     * @return one rgb888 component value with added light level
     */
    fun AddLight8(c8: Int, light: Int): Int {
        return (c8 * (1 - light.toFloat() / Lights.COLORMAP_LIGHTS_24) + 0.5).toInt()
    }

    /**
     * @param c8 one rgb888 color component value (not a mistake - input is rgb888)
     * @param light light level to add
     * @return one rgb555 component value with added light level
     */
    fun AddLight5(c8: Int, light: Int): Int {
        return (c8 * (1 - light.toFloat() / Lights.COLORMAP_LIGHTS_15) + 0.5).toInt() shr 3
    }

    /**
     * @param c8 one rgb888 color component value (not a mistake - input is rgb888)
     * @param light light level to add
     * @return one rgb555 component value with added light level
     */
    fun AddLightI(c8: Int, light: Int): Int {
        return (c8 * (1 - light.toFloat() / Lights.COLORMAP_LIGHTS_15) + 0.5).toInt()
    }

    /**
     * Decides the size of array for colormap and creates it
     * @param hasColormap whether the array have lump-read colormap
     * @param an array that can possibly have colormap read from COLORMAP lump
     * @return empty array for colormap
     */
    fun AllocateColormap24(
        hasColormap: Boolean,
        colormap: Array<Array<ByteArray?>>
    ): Array<IntArray>? {
        // if the lump-read COLORMAP is shorter, we must allocate enough
        val targetLength: Int = if (hasColormap) Lights.COLORMAP_STD_LENGTH_24 + Math.max(
            0,
            colormap[0].size - Lights.COLORMAP_STD_LENGTH_15
        ) else Lights.COLORMAP_STD_LENGTH_24
        return Array(targetLength) { IntArray(Palettes.PAL_NUM_COLORS) }
    }

    /**
     * Decides the size of array for colormap and creates it
     * @param hasColormap whether the array have lump-read colormap
     * @param an array that can possibly have colormap read from COLORMAP lump
     * @return empty array for colormap
     */
    fun AllocateColormap15(
        hasColormap: Boolean,
        colormap: Array<Array<ByteArray?>>
    ): Array<ShortArray>? {
        // if the lump-read COLORMAP is shorter, we must allocate enough
        val targetLength = if (hasColormap) Math.max(
            Lights.COLORMAP_STD_LENGTH_15,
            colormap[0].size
        ) else Lights.COLORMAP_STD_LENGTH_15
        return Array(targetLength) { ShortArray(Palettes.PAL_NUM_COLORS) }
    }

    /**
     * Copy selected colormap from COLORMAP lump with respect to palette
     * @param int[] stuff a 256-entry part of target colormap
     * @param int[] palette A packed RGB888 256-entry int palette
     * @param byte[] map a 256-entry part of COLORMAP lump to copy
     */
    fun CopyMap24(targetColormap: IntArray, palette: IntArray, map: ByteArray) {
        for (c in 0 until Palettes.PAL_NUM_COLORS) {
            targetColormap[c] = palette[map[c].toInt() and 0xFF]
        }
    }

    /**
     * Copy selected colormap from COLORMAP lump with respect to palette
     * @param short[] stuff a 256-entry part of target colormap
     * @param int[] palette A packed RGB888 256-entry int palette
     * @param byte[] map a 256-entry part of COLORMAP lump to copy
     */
    fun CopyMap15(targetColormap: ShortArray, palette: IntArray, map: ByteArray) {
        val palColor = IntArray(3)
        for (c in 0 until Palettes.PAL_NUM_COLORS) {
            getRGB888(palette[map[c].toInt() and 0xFF], palColor)
            targetColormap[c] = rgb888to555(palColor[0], palColor[1], palColor[2])
        }
    }

    /**
     * TrueColor invulnerability specials
     * The key is: get the color, compute its luminance (or other method of grey if set in cfg)
     * and substract it from white
     *
     * @param int[] stuff target array to set into
     * @param int[] rgb unpacked color components
     * @param index an index of the color int 256-entry int palette
     */
    fun BuildSpecials24(targetColormap: IntArray, rgb: IntArray, index: Int) {
        val luminance: Float = GreyscaleFilter.component(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat())
        val grey: Int = (255 * (1.0 - luminance / Palettes.PAL_NUM_COLORS)).toInt()
        targetColormap[index] = toRGB888(grey, grey, grey)
    }

    /**
     * HiColor invulnerability specials
     * The key is: get the color, compute its luminance (or other method of grey if set in cfg)
     * and substract it from white
     *
     * @param short[] stuff target array to set into
     * @param int[] rgb unpacked color components
     * @param index an index of the color int 256-entry int palette
     */
    fun BuildSpecials15(targetColormap: ShortArray, rgb: IntArray, index: Int) {
        val luminance: Float = GreyscaleFilter.component(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat())
        val grey: Int = (255 * (1.0 - luminance / Palettes.PAL_NUM_COLORS)).toInt()
        targetColormap[index] = toRGB555(grey shr 3, grey shr 3, grey shr 3)
    }

    /**
     * Indexed invulnerability specials
     * The key is: get the color, compute its luminance (or other method of grey if set in cfg)
     * and substract it from white
     *
     * @param byte[] stuff target array to set into
     * @param int[] rgb unpacked color components
     * @param index an index of the color int 256-entry int palette
     */
    fun BuildSpecialsI(targetColormap: ByteArray, rgb: IntArray, palette: IntArray?, index: Int) {
        val luminance: Float = GreyscaleFilter.component(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat())
        val grey: Int = (255 * (1.0 - luminance / Palettes.PAL_NUM_COLORS)).toInt()
        targetColormap[index] = BestColor(grey, grey, grey, palette!!, 0, Palettes.PAL_NUM_COLORS - 1).toByte()
    }

    companion object {
        /**
         * Light levels. Binded to the colormap subsystem
         */
        const val COLORMAP_LIGHTS_15 = 1 shl 5
        const val COLORMAP_LIGHTS_24 = 1 shl 8

        /**
         * Standard lengths for colormaps
         */
        val COLORMAP_STD_LENGTH_15: Int = Lights.COLORMAP_LIGHTS_15 + 1
        val COLORMAP_STD_LENGTH_24: Int = Lights.COLORMAP_LIGHTS_24 + 1

        /**
         * Default index of inverse colormap. Note that it will be shifted to the actual position
         * in generated lights map by the difference in lights count between 5 and 8 bits lighting.
         * I have discovered, that player_t.fixedcolormap property is *stored* by game when writing files,
         * for example it could be included in savegame or demos.
         *
         * If we preshift inverse colormap, MochaDoom not in TrueColor bppMode or any Vanilla DOOM would crash
         * when trying to load savegame made when under invulnerabilty in TrueColor bppMode.
         * - Good Sign 2017/04/15
         */
        const val COLORMAP_INVERSE = 32

        /**
         * An index of of the lighted palette in colormap used for FUZZ effect and partial invisibility
         */
        const val COLORMAP_BLURRY = 6

        /**
         * An index of of the most lighted palette in colormap
         */
        const val COLORMAP_BULLBRIGHT = 1

        /**
         * An index of of palette0 in colormap which is not altered
         */
        const val COLORMAP_FIXED = 0

        /**
         * A difference in percents between color multipliers of two adjacent light levels
         * It took sometime to dig this out, and this could be possibly used to simplify
         * BuildLight functions without decrease in their perfectness
         *
         * The formula to apply to a color will then be:
         * float ratio = 1.0f - LIGHT_INCREMENT_RATIO_24 * lightLevel;
         * color[0] = (int) (color[0] * ratio + 0.5)
         * color[1] = (int) (color[1] * ratio + 0.5)
         * color[2] = (int) (color[2] * ratio + 0.5)
         *
         * However, this one is untested, and existing formula in function AddLight8 does effectively the same,
         * just a little slower.
         *
         * - Good Sign 2017/04/17
         */
        val LIGHT_INCREMENT_RATIO_24: Float = 1.0f / Lights.COLORMAP_LIGHTS_24
    }
}