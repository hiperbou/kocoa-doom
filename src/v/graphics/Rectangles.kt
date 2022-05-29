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

import java.awt.Rectangle

/**
 * Rectangles fill and copy
 *
 * TODO: range checks on Fill & Copy
 *
 * @author Good Sign
 */
interface Rectangles<V, E : Enum<E>> : Blocks<V, E>, Points<V, E> {
    /**
     * Computes a Horizontal with a row from the Rectangle at heightIndex
     * @param rect
     * @param heightIndex
     * @return
     */
    fun GetRectRow(rect: Rectangle, heightIndex: Int): Horizontal {
        if (heightIndex < 0 || heightIndex > rect.height) {
            throw IndexOutOfBoundsException("Bad row index: $heightIndex")
        }
        return Horizontal(point(rect.x, rect.y) + heightIndex * getScreenWidth(), rect.width)
    }

    /**
     * V_CopyRect
     */
    fun CopyRect(srcScreenType: E?, rectangle: Rectangle, dstScreenType: E?) {
        val srcScreen = getScreen(srcScreenType!!)!!
        val dstScreen = getScreen(dstScreenType!!)!!
        val screenWidth = getScreenWidth()
        val point = point(rectangle.x, rectangle.y)
        val rel = Relocation(point, point, rectangle.width)
        var h = rectangle.height
        while (h > 0) {
            screenCopy(srcScreen, dstScreen, rel)
            --h
            rel.shift(screenWidth)
        }
    }

    fun CopyRect(srcScreenType: E?, rectangle: Rectangle, dstScreenType: E?, dstPoint: Int) {
        val srcScreen = getScreen(srcScreenType!!)!!
        val dstScreen = getScreen(dstScreenType!!)!!
        val screenWidth = getScreenWidth()
        val rel = Relocation(point(rectangle.x, rectangle.y), dstPoint, rectangle.width)
        var h = rectangle.height
        while (h > 0) {
            screenCopy(srcScreen, dstScreen, rel)
            --h
            rel.shift(screenWidth)
        }
    }

    /**
     * V_FillRect
     */
    fun FillRect(screenType: E?, rectangle: Rectangle, patternSrc: V, pattern: Horizontal?) {
        val screen = getScreen(screenType!!)!!
        if (rectangle.height > 0) {
            val row = GetRectRow(rectangle, 0)
            // Fill first line of rect
            screenSet(patternSrc, pattern!!, screen, row)
            // Fill the rest of the rect
            RepeatRow(screen, row, rectangle.height - 1)
        }
    }

    fun FillRect(screenType: E?, rectangle: Rectangle, patternSrc: V, point: Int) {
        val screen = getScreen(screenType!!)!!
        if (rectangle.height > 0) {
            val row = GetRectRow(rectangle, 0)
            // Fill first line of rect
            screenSet(patternSrc, point, screen, row)
            // Fill the rest of the rect
            RepeatRow(screen, row, rectangle.height - 1)
        }
    }

    fun FillRect(screenType: E, rectangle: Rectangle, color: Int) {
        FillRect(screenType, rectangle, color.toByte())
    }

    fun FillRect(screenType: E, rectangle: Rectangle, color: Byte) {
        val screen = getScreen(screenType)!!
        if (rectangle.height > 0) {
            val filler = convertPalettedBlock(color)
            val row = GetRectRow(rectangle, 0)
            // Fill first line of rect
            screenSet(filler, 0, screen, row)
            // Fill the rest of the rect
            RepeatRow(screen, row, rectangle.height - 1)
        }
    }
}