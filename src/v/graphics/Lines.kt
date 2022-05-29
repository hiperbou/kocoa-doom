/**
 * Copyright (C) 1993-1996 Id Software, Inc.
 * from am_map.c
 *
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
package v.graphics


import v.graphics.*

interface Lines {
    /**
     * Bresenham's line algorithm modified to use custom Plotter
     *
     * @param plotter
     * @param x2
     * @param y2
     */
    fun drawLine(plotter: Plotter<*>, x1: Int, x2: Int) {
        drawLine(plotter, x1, x2, 1, 1)
    }

    fun drawLine(plotter: Plotter<*>, x2: Int, y2: Int, dupX: Int, dupY: Int) {
        // delta of exact value and rounded value of the dependant variable
        var d = 0
        var dy: Int
        var dx: Int
        var ix: Int
        var iy: Int
        run {
            val x = plotter.getX()
            val y = plotter.getY()
            dy = Math.abs(y2 - y)
            dx = Math.abs(x2 - x)
            ix = if (x < x2) 1 else -1 // increment direction
            iy = if (y < y2) 1 else -1
        }
        val dy2 = dy shl 1 // slope scaling factors to avoid floating
        val dx2 = dx shl 1 // point
        if (dy <= dx) {
            while (true) {
                plotter.plot()
                if (plotter.getX() == x2) break
                d += dy2
                if (d > dx) {
                    plotter.shift(ix, iy)
                    d -= dx2
                } else plotter.shiftX(ix)
            }
        } else {
            while (true) {
                plotter.plot()
                if (plotter.getY() == y2) break
                d += dx2
                if (d > dy) {
                    plotter.shift(ix, iy)
                    d -= dy2
                } else plotter.shiftY(iy)
            }
        }
    }
}