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
import v.tables.GreyscaleFilter

/**
 *
 * @author Good Sign
 */
enum class GreyscaleFilter {
    Lightness, Average, Luminance,  // this one is the default for invulnerability map
    Luminosity;

    fun getComponent(r: Int, g: Int, b: Int): Int {
        when (this) {
            GreyscaleFilter.Lightness -> return (Math.max(Math.max(r, g), b) + Math.min(Math.min(r, g), b)) / 2
            GreyscaleFilter.Average -> return (r + g + b) / 3
            GreyscaleFilter.Luminance -> return (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            GreyscaleFilter.Luminosity -> return (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt()
        }

        // should not happen
        return 0
    }

    fun getComponent(r: Float, g: Float, b: Float): Float {
        when (this) {
            GreyscaleFilter.Lightness -> return (Math.max(Math.max(r, g), b) + Math.min(Math.min(r, g), b)) / 2
            GreyscaleFilter.Average -> return (r + g + b) / 3
            GreyscaleFilter.Luminance -> return 0.299f * r + 0.587f * g + 0.114f * b
            GreyscaleFilter.Luminosity -> return 0.2126f * r + 0.7152f * g + 0.0722f * b
        }

        // should not happen
        return 0.0f
    }

    fun getGrey888(r8: Int, g8: Int, b8: Int): Int {
        val component = getComponent(r8, g8, b8) and 0xFF
        return -0x1000000 + (component shl 16) + (component shl 8) + component
    }

    fun getGrey555(r5: Int, g5: Int, b5: Int): Short {
        val component = getComponent(r5, g5, b5) and 0x1F
        return ((component shl 10) + (component shl 5) + component).toShort()
    }

    fun getGrey888(rgb888: Int): Int {
        return getGrey888(rgb888 shr 16 and 0xFF, rgb888 shr 8 and 0xFF, rgb888 and 0xFF)
    }

    fun getGrey555(rgb555: Short): Short {
        return getGrey555(rgb555.toInt() shr 10 and 0x1F, rgb555.toInt() shr 5 and 0x1F, rgb555.toInt() and 0x1F)
    }

    companion object {
        private var FILTER: GreyscaleFilter? = null
        fun component(r: Int, g: Int, b: Int): Int {
            if (GreyscaleFilter.FILTER == null) {
                GreyscaleFilter.readSetting()
            }
            return GreyscaleFilter.FILTER!!.getComponent(r, g, b)
        }

        fun component(r: Float, g: Float, b: Float): Float {
            if (GreyscaleFilter.FILTER == null) {
                GreyscaleFilter.readSetting()
            }
            return GreyscaleFilter.FILTER!!.getComponent(r, g, b)
        }

        fun grey888(rgb888: Int): Int {
            if (GreyscaleFilter.FILTER == null) {
                GreyscaleFilter.readSetting()
            }
            return GreyscaleFilter.FILTER!!.getGrey888(rgb888)
        }

        fun grey888(r8: Int, g8: Int, b8: Int): Int {
            if (GreyscaleFilter.FILTER == null) {
                GreyscaleFilter.readSetting()
            }
            return GreyscaleFilter.FILTER!!.getGrey888(r8, g8, b8)
        }

        fun grey555(r5: Int, g5: Int, b5: Int): Short {
            if (GreyscaleFilter.FILTER == null) {
                GreyscaleFilter.readSetting()
            }
            return GreyscaleFilter.FILTER!!.getGrey555(r5, g5, b5)
        }

        fun grey555(rgb555: Short): Short {
            if (GreyscaleFilter.FILTER == null) {
                GreyscaleFilter.readSetting()
            }
            return GreyscaleFilter.FILTER!!.getGrey555(rgb555)
        }

        private fun readSetting() {
            GreyscaleFilter.FILTER = Engine.getConfig()
                .getValue<GreyscaleFilter>(Settings.greyscale_filter, GreyscaleFilter::class.java)
        }
    }
}