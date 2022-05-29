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

import v.tables.ColorTint

/**
 * Package containing individual color modification and transformation methods
 */
interface Colors {
    /**
     * Get alpha from packed argb long word.
     *
     * @param argb8888
     * @return
     */
    fun getAlpha(argb8888: Int): Int {
        return argb8888 ushr 24 and 0xFF
    }

    /**
     * Get red from packed argb long word.
     *
     * @param rgb888
     * @return
     */
    fun getRed(rgb888: Int): Int {
        return 0xFF0000 and rgb888 shr 16
    }

    /**
     * Get red from packed rgb555
     *
     * @param rgb555
     * @return
     */
    fun getRed5(rgb555: Int): Int {
        return rgb555 shr 10 and 0x1F
    }

    /**
     * Get green from packed argb long word.
     *
     * @param rgb888
     * @return
     */
    fun getGreen(rgb888: Int): Int {
        return 0xFF00 and rgb888 shr 8
    }

    /**
     * Get green from packed rgb555
     *
     * @param rgb555
     * @return
     */
    fun getGreen5(rgb555: Int): Int {
        return rgb555 shr 5 and 0x1F
    }

    /**
     * Get blue from packed argb long word.
     *
     * @param rgb888
     * @return
     */
    fun getBlue(rgb888: Int): Int {
        return 0xFF and rgb888
    }

    /**
     * Get blue from packed rgb555
     *
     * @param rgb555
     * @return
     */
    fun getBlue5(rgb555: Int): Int {
        return rgb555 and 0x1F
    }

    /**
     * Get all four color channels into an array
     */
    fun getARGB8888(argb8888: Int, container: IntArray): IntArray {
        container[0] = getAlpha(argb8888)
        container[1] = getRed(argb8888)
        container[2] = getGreen(argb8888)
        container[3] = getBlue(argb8888)
        return container
    }

    /**
     * Get all four color channels into an array
     */
    fun getRGB888(rgb888: Int, container: IntArray): IntArray {
        container[0] = getRed(rgb888)
        container[1] = getGreen(rgb888)
        container[2] = getBlue(rgb888)
        return container
    }

    /**
     * Get all three colors into an array
     */
    fun getRGB555(rgb555: Int, container: IntArray): IntArray {
        container[0] = getRed5(rgb555)
        container[1] = getGreen5(rgb555)
        container[2] = getBlue5(rgb555)
        return container
    }

    /**
     * Compose rgb888 color (opaque)
     */
    fun toRGB888(r: Int, g: Int, b: Int): Int {
        return -0x1000000 + (r and 0xFF shl 16) + (g and 0xFF shl 8) + (b and 0xFF)
    }

    /**
     * Compose argb8888 color
     */
    fun toARGB8888(a: Int, r: Int, g: Int, b: Int): Int {
        return (a and 0xFF shl 24) + (r and 0xFF shl 16) + (g and 0xFF shl 8) + (b and 0xFF)
    }

    /**
     * Compose rgb888 color
     */
    fun toRGB555(r: Int, g: Int, b: Int): Short {
        return ((r and 0x1F shl 10) + (g and 0x1F shl 5) + (b and 0x1F)).toShort()
    }

    /**
     * Alter rgb888 color by applying a tint to it
     * @param int[] rgbInput an array containing rgb888 color components
     */
    fun tintRGB888(tint: ColorTint, rgbInput: IntArray, rgbOutput: IntArray): IntArray? {
        rgbOutput[0] = tint.tintRed8(rgbInput[0])
        rgbOutput[1] = tint.tintGreen8(rgbInput[1])
        rgbOutput[2] = tint.tintBlue8(rgbInput[2])
        return rgbOutput
    }

    /**
     * Alter rgb555 color by applying a tint to it
     * @param int[] rgbInput an array containing rgb555 color components
     */
    fun tintRGB555(tint: ColorTint, rgbInput: IntArray, rgbOutput: IntArray): IntArray? {
        rgbOutput[0] = tint.tintRed5(rgbInput[0])
        rgbOutput[1] = tint.tintGreen5(rgbInput[1])
        rgbOutput[2] = tint.tintBlue5(rgbInput[2])
        return rgbOutput
    }

    fun sigmoid(r: Double): Double {
        return 1 / (1 + Math.pow(Math.E, -1 * r))
    }

    fun sigmoidGradient(component1: Int, component2: Int, ratio: Float): Int {
        return (ratio * component1 + (1 - ratio) * component2).toInt()
    }

    /**
     * Tells which color is further by comparing distance between two packed rgb888 ints
     */
    fun CompareColors888(rgb888_1: Int, rgb888_2: Int): Int {
        val distance = ColorDistance888(rgb888_1, rgb888_2)
        return if (distance > 0) 1 else if (distance < 0) -1 else 0
    }

    /**
     * Computes simplified Euclidean color distance (without extracting square root) between two packed rbg888 ints
     */
    fun ColorDistance888(rgb888_1: Int, rgb888_2: Int): Long {
        val r1 = getRed(rgb888_1)
        val g1 = getGreen(rgb888_1)
        val b1 = getBlue(rgb888_1)
        val r2 = getRed(rgb888_2)
        val g2 = getGreen(rgb888_2)
        val b2 = getBlue(rgb888_2)
        val dr = (r1 - r2).toLong()
        val dg = (g1 - g2).toLong()
        val db = (b1 - b2).toLong()
        return dr * dr + dg * dg + db * db
    }

    /**
     * Tells which color is further by comparing hue, saturation, value distance between two packed rgb888 ints
     */
    fun CompareColorsHSV888(rgb888_1: Int, rgb888_2: Int): Int {
        val distance = ColorDistanceHSV888(rgb888_1, rgb888_2)
        return if (distance > 0) 1 else if (distance < 0) -1 else 0
    }

    /**
     * Computes simplified Euclidean color distance (without extracting square root) between two packed rbg888 ints
     * based on hue, saturation and value
     */
    fun ColorDistanceHSV888(rgb888_1: Int, rgb888_2: Int): Long {
        val r1 = (0.21 * getRed(rgb888_1)).toInt()
        val g1 = (0.72 * getGreen(rgb888_1)).toInt()
        val b1 = (0.07 * getBlue(rgb888_1)).toInt()
        val r2 = (0.21 * getRed(rgb888_2)).toInt()
        val g2 = (0.72 * getGreen(rgb888_2)).toInt()
        val b2 = (0.07 * getBlue(rgb888_2)).toInt()
        val dr = (r1 - r2).toLong()
        val dg = (g1 - g2).toLong()
        val db = (b1 - b2).toLong()
        return dr * dr + dg * dg + db * db
    }

    /**
     * Tells which color is further by comparing distance between two packed rgb555 shorts
     */
    fun CompareColors555(rgb555_1: Short, rgb555_2: Short): Int {
        val distance = ColorDistance555(rgb555_1, rgb555_2)
        return if (distance > 0) 1 else if (distance < 0) -1 else 0
    }

    /**
     * Computes simplified Euclidean color distance (without extracting square root) between two packed rbg555 shorts
     */
    fun ColorDistance555(rgb1: Short, rgb2: Short): Long {
        val r1 = getRed5(rgb1.toInt())
        val g1 = getGreen5(rgb1.toInt())
        val b1 = getBlue5(rgb1.toInt())
        val r2 = getRed5(rgb2.toInt())
        val g2 = getGreen5(rgb2.toInt())
        val b2 = getBlue5(rgb2.toInt())
        val dr = (r1 - r2).toLong()
        val dg = (g1 - g2).toLong()
        val db = (b1 - b2).toLong()
        return dr * dr + dg * dg + db * db
    }

    /**
     * Tells which color is further by comparing hue, saturation, value distance between two packed rgb555 shorts
     */
    fun CompareColorsHSV555(rgb555_1: Short, rgb555_2: Short): Int {
        val distance = ColorDistanceHSV555(rgb555_1, rgb555_2.toInt())
        return if (distance > 0) 1 else if (distance < 0) -1 else 0
    }

    /**
     * Computes simplified Euclidean color distance (without extracting square root) between two packed rbg888 ints
     * based on hue, saturation and value
     */
    fun ColorDistanceHSV555(rgb555_1: Short, rgb555_2: Int): Long {
        val r1 = (0.21 * getRed5(rgb555_1.toInt())).toInt()
        val g1 = (0.72 * getGreen5(rgb555_1.toInt())).toInt()
        val b1 = (0.07 * getBlue5(rgb555_1.toInt())).toInt()
        val r2 = (0.21 * getRed5(rgb555_2)).toInt()
        val g2 = (0.72 * getGreen5(rgb555_2)).toInt()
        val b2 = (0.07 * getBlue5(rgb555_2)).toInt()
        val dr = (r1 - r2).toLong()
        val dg = (g1 - g2).toLong()
        val db = (b1 - b2).toLong()
        return dr * dr + dg * dg + db * db
    }

    fun ColorRatio(rgb1: IntArray, rgb2: IntArray, out: FloatArray): FloatArray? {
        for (i in 0..2) {
            out[i] = if (rgb2[i] > 0) rgb1[i] / rgb2[i].toFloat() else 1.0f
        }
        return out
    }

    /**
     * Get ARGB_8888 from RGB_555, with proper higher-bit
     * replication.
     *
     * @param rgb555
     * @return rgb888 packed int
     * @author velktron
     */
    fun rgb555to888(rgb555: Short): Int {
        // .... .... .... ....
        // 111 11 = 7C00
        // 11 111 = 03E0
        // 1F= 1 1111
        var ri = 0x7C00 and rgb555.toInt() shr 7
        var gi = 0x3E0 and rgb555.toInt() shr 2
        var bi = 0x1F and rgb555.toInt() shl 3
        // replicate 3 higher bits
        var bits = ri and 224 shr 5
        ri += bits
        bits = gi and 224 shr 5
        gi += bits
        bits = bi and 224 shr 5
        bi += bits
        // ARGB 8888 packed
        return toRGB888(ri, gi, bi)
    }

    /**
     * Get RGB_555 from packed ARGB_8888.
     *
     * @param argb
     * @return rgb555 packed short
     * @authoor velktron
     */
    fun argb8888to555(argb8888: Int): Short {
        val ri = -0xff0000 and argb8888 shr 19
        val gi = 0xFF00 and argb8888 shr 11
        val bi = 0xFF and argb8888 shr 3
        return toRGB555(ri, gi, bi)
    }

    /**
     * Get packed RGB_555 word from individual 8-bit RGB components.
     *
     * WARNING: there's no sanity/overflow check for performance reasons.
     *
     * @param r
     * @param g
     * @param b
     * @return rgb888 packed int
     * @author velktron
     */
    fun rgb888to555(r: Int, g: Int, b: Int): Short {
        return toRGB555(r shr 3, g shr 3, b shr 3)
    }

    /**
     * Finds a color in the palette's range from rangel to rangeh closest to specified r, g, b
     * by distortion, the lesst distorted color is the result. Used for rgb555 invulnerability colormap
     */
    fun BestColor(r: Int, g: Int, b: Int, palette: IntArray, rangel: Int, rangeh: Int): Int {
        /**
         * let any color go to 0 as a last resort
         */
        var bestdistortion = r.toLong() * r + g.toLong() * g + b.toLong() * b * 2
        var bestcolor = 0
        for (i in rangel..rangeh) {
            val dr = (r - getRed(palette[i])).toLong()
            val dg = (g - getGreen(palette[i])).toLong()
            val db = (b - getBlue(palette[i])).toLong()
            val distortion = dr * dr + dg * dg + db * db
            if (distortion < bestdistortion) {
                if (distortion == 0L) {
                    return i // perfect match
                }
                bestdistortion = distortion
                bestcolor = i
            }
        }
        return bestcolor
    }
}