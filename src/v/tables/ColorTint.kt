/**
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package v.tables

import java.util.*

/**
 * Default generated tints for berserk, radsuit, bonus pickup and so on.
 * I think they may be invalid if the game uses custom COLORMAP, so we need an ability
 * to regenerate them when loading such lump.
 * Thus, it is an Enum... but only almost.
 *
 * Added new LUT's for HiColor and TrueColor renderers
 * They are capable of tinting and gamma correcting full direct colors(not indexed) on the fly
 * - Good Sign
 */
class ColorTint internal constructor(
    private val r: Float,
    private val r5: Float,
    private val g: Float,
    private val g5: Float,
    private val b: Float,
    private val b5: Float,
    private val purepart: Float
) {
    /*public static List<ColorTint> generateTints(byte cmaps[][]) {
    }*/
    internal constructor(r: Int, g: Int, b: Int, tint: Float) : this(
        r * tint,
        (r shr 3) * tint,
        g * tint,
        (g shr 3) * tint,
        b * tint,
        (b shr 3) * tint,
        1 - tint
    ) {
    }

    internal constructor(mid8: Float, mid5: Float, purepart: Float) : this(
        mid8,
        mid5,
        mid8,
        mid5,
        mid8,
        mid5,
        purepart
    ) {
    }

    val LUT_r8 = Array(5) { ByteArray(0x100) }
    val LUT_g8 = Array(5) { ByteArray(0x100) }
    val LUT_b8 = Array(5) { ByteArray(0x100) }
    val LUT_r5 = Array(5) { ByteArray(0x20) }
    val LUT_g5 = Array(5) { ByteArray(0x20) }
    val LUT_b5 = Array(5) { ByteArray(0x20) }

    init {
        for (j in GammaTables.LUT.indices) {
            for (i in 0..0xFF) {
                LUT_r8[j][i] = GammaTables.LUT[j][tintRed8(i)].toByte()
                LUT_g8[j][i] = GammaTables.LUT[j][tintGreen8(i)].toByte()
                LUT_b8[j][i] = GammaTables.LUT[j][tintBlue8(i)].toByte()
                if (i <= 0x1F) {
                    LUT_r5[j][i] = (GammaTables.LUT[j][tintRed5(i) shl 3] shr 3).toByte()
                    LUT_g5[j][i] = (GammaTables.LUT[j][tintGreen5(i) shl 3] shr 3).toByte()
                    LUT_b5[j][i] = (GammaTables.LUT[j][tintBlue5(i) shl 3] shr 3).toByte()
                }
            }
        }
    }

    fun mid(): Float {
        return (r + g + b) / 3
    }

    fun mid5(): Float {
        return (r5 + g5 + b5) / 3
    }

    fun tintGreen8(green8: Int): Int {
        return Math.min((green8 * purepart + g).toInt(), 0xFF)
    }

    fun tintGreen5(green5: Int): Int {
        return Math.min((green5 * purepart + g5).toInt(), 0x1F)
    }

    fun tintBlue8(blue8: Int): Int {
        return Math.min((blue8 * purepart + b).toInt(), 0xFF)
    }

    fun tintBlue5(blue5: Int): Int {
        return Math.min((blue5 * purepart + b5).toInt(), 0x1F)
    }

    fun tintRed8(red8: Int): Int {
        return Math.min((red8 * purepart + r).toInt(), 0xFF)
    }

    fun tintRed5(red5: Int): Int {
        return Math.min((red5 * purepart + r5).toInt(), 0x1F)
    }

    companion object {
        val NORMAL: ColorTint = ColorTint(0, 0, 0, .0f)
        val RED_11: ColorTint = ColorTint(255, 2, 3, 0.11f)
        val RED_22: ColorTint = ColorTint(255, 0, 0, 0.22f)
        val RED_33: ColorTint = ColorTint(255, 0, 0, 0.33f)
        val RED_44: ColorTint = ColorTint(255, 0, 0, 0.44f)
        val RED_55: ColorTint = ColorTint(255, 0, 0, 0.55f)
        val RED_66: ColorTint = ColorTint(255, 0, 0, 0.66f)
        val RED_77: ColorTint = ColorTint(255, 0, 0, 0.77f)
        val RED_88: ColorTint = ColorTint(255, 0, 0, 0.88f)
        val BERSERK_SLIGHT: ColorTint = ColorTint(215, 185, 68, 0.12f)
        val BERSERK_SOMEWHAT: ColorTint = ColorTint(215, 185, 68, 0.25f)
        val BERSERK_NOTICABLE: ColorTint = ColorTint(215, 185, 68, 0.375f)
        val BERSERK_HEAVY: ColorTint = ColorTint(215, 185, 68, 0.50f)
        val RADSUIT: ColorTint = ColorTint(3, 253, 3, 0.125f)
        val GREY_NORMAL: ColorTint = ColorTint(
            ColorTint.NORMAL.mid(),
            ColorTint.NORMAL.mid5(),
            ColorTint.NORMAL.purepart
        )
        val GREY_RED_11: ColorTint = ColorTint(
            ColorTint.RED_11.mid(),
            ColorTint.RED_11.mid5(),
            ColorTint.RED_11.purepart
        )
        val GREY_RED_22: ColorTint = ColorTint(
            ColorTint.RED_22.mid(),
            ColorTint.RED_22.mid5(),
            ColorTint.RED_22.purepart
        )
        val GREY_RED_33: ColorTint = ColorTint(
            ColorTint.RED_33.mid(),
            ColorTint.RED_33.mid5(),
            ColorTint.RED_33.purepart
        )
        val GREY_RED_44: ColorTint = ColorTint(
            ColorTint.RED_44.mid(),
            ColorTint.RED_44.mid5(),
            ColorTint.RED_44.purepart
        )
        val GREY_RED_55: ColorTint = ColorTint(
            ColorTint.RED_55.mid(),
            ColorTint.RED_55.mid5(),
            ColorTint.RED_55.purepart
        )
        val GREY_RED_66: ColorTint = ColorTint(
            ColorTint.RED_66.mid(),
            ColorTint.RED_66.mid5(),
            ColorTint.RED_66.purepart
        )
        val GREY_RED_77: ColorTint = ColorTint(
            ColorTint.RED_77.mid(),
            ColorTint.RED_77.mid5(),
            ColorTint.RED_77.purepart
        )
        val GREY_RED_88: ColorTint = ColorTint(
            ColorTint.RED_88.mid(),
            ColorTint.RED_88.mid5(),
            ColorTint.RED_88.purepart
        )
        val GREY_BERSERK_SLIGHT: ColorTint = ColorTint(
            ColorTint.BERSERK_SLIGHT.mid(),
            ColorTint.BERSERK_SLIGHT.mid5(),
            ColorTint.BERSERK_SLIGHT.purepart
        )
        val GREY_BERSERK_SOMEWHAT: ColorTint = ColorTint(
            ColorTint.BERSERK_SOMEWHAT.mid(),
            ColorTint.BERSERK_SOMEWHAT.mid5(),
            ColorTint.BERSERK_SOMEWHAT.purepart
        )
        val GREY_BERSERK_NOTICABLE: ColorTint = ColorTint(
            ColorTint.BERSERK_NOTICABLE.mid(),
            ColorTint.BERSERK_NOTICABLE.mid5(),
            ColorTint.BERSERK_NOTICABLE.purepart
        )
        val GREY_BERSERK_HEAVY: ColorTint = ColorTint(
            ColorTint.BERSERK_HEAVY.mid(),
            ColorTint.BERSERK_HEAVY.mid5(),
            ColorTint.BERSERK_HEAVY.purepart
        )
        val GREY_RADSUIT: ColorTint = ColorTint(
            ColorTint.RADSUIT.mid(),
            ColorTint.RADSUIT.mid5(),
            ColorTint.RADSUIT.purepart
        )
        val NORMAL_TINTS = Collections.unmodifiableList(
            Arrays.asList<ColorTint>(
                ColorTint.NORMAL,
                ColorTint.RED_11,
                ColorTint.RED_22,
                ColorTint.RED_33,
                ColorTint.RED_44,
                ColorTint.RED_55,
                ColorTint.RED_66,
                ColorTint.RED_77,
                ColorTint.RED_88,
                ColorTint.BERSERK_SLIGHT,
                ColorTint.BERSERK_SOMEWHAT,
                ColorTint.BERSERK_NOTICABLE,
                ColorTint.BERSERK_HEAVY,
                ColorTint.RADSUIT
            )
        )
        val GREY_TINTS = Collections.unmodifiableList(
            Arrays.asList<ColorTint>(
                ColorTint.GREY_NORMAL,
                ColorTint.GREY_RED_11,
                ColorTint.GREY_RED_22,
                ColorTint.GREY_RED_33,
                ColorTint.GREY_RED_44,
                ColorTint.GREY_RED_55,
                ColorTint.GREY_RED_66,
                ColorTint.GREY_RED_77,
                ColorTint.GREY_RED_88,
                ColorTint.GREY_BERSERK_SLIGHT,
                ColorTint.GREY_BERSERK_SOMEWHAT,
                ColorTint.GREY_BERSERK_NOTICABLE,
                ColorTint.GREY_BERSERK_HEAVY,
                ColorTint.GREY_RADSUIT
            )
        )
    }
}