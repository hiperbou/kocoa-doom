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

import v.scale.VideoScale
import java.awt.Rectangle

/**
 * Manipulating Blocks
 *
 * @author Good Sign
 */
interface Blocks<V, E : Enum<E>> : Points<V, E>, Palettes {
    /**
     * Converts a block of paletted pixels into screen format pixels
     * It is advised that implementation should both perform caching
     * and be additionally optimized for 1-value src arrays
     */
    fun convertPalettedBlock(vararg src: Byte): V

    /**
     * Fills the whole dstScreen tiling the copies of block across it
     */
    fun TileScreen(dstScreen: E, block: V, blockArea: Rectangle) {
        val screenHeight = getScreenHeight()
        val screenWidth = getScreenWidth()
        var y = 0
        while (y < screenHeight) {

            // Draw whole blocks.
            var x = 0
            while (x < screenWidth) {
                val destination = point(x, y, screenWidth)
                DrawBlock(dstScreen, block, blockArea, destination)
                x += blockArea.width
            }
            y += blockArea.height
        }
    }

    /**
     * Fills the rectangular part of dstScreen tiling the copies of block across it
     */
    fun TileScreenArea(dstScreen: E, screenArea: Rectangle, block: V, blockArea: Rectangle) {
        val screenWidth = getScreenWidth()
        val fiilLimitX = screenArea.x + screenArea.width
        val fiilLimitY = screenArea.y + screenArea.height
        var y = screenArea.y
        while (y < fiilLimitY) {

            // Draw whole blocks.
            var x = screenArea.x
            while (x < fiilLimitX) {
                val destination = point(x, y, screenWidth)
                DrawBlock(dstScreen, block, blockArea, destination)
                x += blockArea.width
            }
            y += blockArea.height
        }
    }

    /**
     * Draws a linear block of pixels from the source buffer into screen buffer
     * V_DrawBlock
     */
    fun DrawBlock(dstScreen: E, block: V, sourceArea: Rectangle, destinationPoint: Int) {
        val screen = getScreen(dstScreen)!!
        val bufferLength = java.lang.reflect.Array.getLength(screen)
        val screenWidth = getScreenWidth()
        val rel = Relocation(
            point(sourceArea.x, sourceArea.y),
            destinationPoint,
            sourceArea.width
        )
        var h = sourceArea.height
        while (h > 0) {
            if (rel.destination + rel.length >= bufferLength) {
                return
            }
            screenCopy(block, screen, rel)
            --h
            rel.source += sourceArea.width
            rel.destination += screenWidth
        }
    }

    fun ScaleBlock(block: V, vs: VideoScale, width: Int, height: Int): V {
        return ScaleBlock(block, width, height, vs.getScalingX(), vs.getScalingY())
    }

    fun ScaleBlock(block: V, width: Int, height: Int, dupX: Int, dupY: Int): V {
        val newWidth = width * dupX
        val newHeight = height * dupY
        val newBlock = java.lang.reflect.Array.newInstance(block!!::class.java.getComponentType(), newWidth * newHeight) as V

        val row = Horizontal(0, dupX)
        for (i in 0 until width) {
            for (j in 0 until height) {
                val pointSource = point(i, j, width)
                val pointDestination = point(i * dupX, j * dupY, newWidth)
                row.start = pointDestination
                // Fill first line of rect
                screenSet(block, pointSource, newBlock, row)
                // Fill the rest of the rect
                RepeatRow(newBlock, row, dupY - 1, newWidth)
            }
        }
        return newBlock
    }

    /**
     * Given a row, repeats it down the screen
     */
    fun RepeatRow(screen: V, row: Horizontal, times: Int) {
        RepeatRow(screen, row, times, getScreenWidth())
    }

    /**
     * Given a row, repeats it down the screen
     */
    fun RepeatRow(block: V, row: Horizontal, times: Int, blockWidth: Int) {
        var times = times
        if (times > 0) {
            val rel = row.relocate(blockWidth)
            while (times > 0) {
                screenCopy(block, block, rel)
                --times
                rel.shift(blockWidth)
            }
        }
    }
}