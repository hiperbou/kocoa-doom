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

import rr.patch_t
import v.graphics.Screens.BadRangeException

/**
 *
 * @author Good Sign
 */
interface Points<V, E : Enum<E>> : Screens<V, E> {
    @Throws(BadRangeException::class)
    fun doRangeCheck(x: Int, y: Int, width: Int, height: Int) {
        if (x >= 0 && y >= 0) {
            val scrWidth = this.getScreenWidth()
            val scrHeight = this.getScreenHeight()
            if (x + width > scrWidth || y + height > scrWidth) {
                throw BadRangeException(
                    String.format(
                        "Coordinates overflow screen space: (%d, %d, %d, %d) on screen %dx%d",
                        x, y, x + width, y + height, scrWidth, scrHeight
                    )
                )
            }
        } else {
            throw IllegalArgumentException(String.format("Invalid coordinates: (%d, %d)", x, y))
        }
    }

    @Throws(BadRangeException::class)
    fun doRangeCheck(x: Int, y: Int, patch: patch_t) {
        doRangeCheck(x, y, patch.width.toInt(), patch.height.toInt())
    }

    @Throws(BadRangeException::class)
    fun doRangeCheck(x: Int, y: Int, patch: patch_t, dupx: Int, dupy: Int) {
        doRangeCheck(x, y, patch.width * dupx, patch.height * dupy)
    }

    fun point(x: Int, y: Int): Int {
        return y * getScreenWidth() + x
    }

    fun point(x: Int, y: Int, width: Int): Int {
        return y * width + x
    }
}