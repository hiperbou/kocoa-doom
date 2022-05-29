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
package v.renderers

import v.graphics.Screens
import v.renderers.RendererFactory.WithWadLoader
import java.awt.Point
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.Raster
import java.awt.image.WritableRaster

internal class BufferedRenderer(rf: WithWadLoader<ByteArray, ByteArray>) : SoftwareIndexedVideoRenderer(rf) {
    private val rasters = arrayOfNulls<WritableRaster>(Screens.SCREENS_COUNT)

    /**
     * This actually creates a raster with a fixed underlying array, but NOT the images themselves. So it's possible to
     * have "imageless" rasters (unless you specifically request to make them visible, of course).
     */
    init {
        for (s in DoomScreen.values()) {
            val index = s.ordinal
            // Only create non-visible data, pegged to the raster. Create visible images only on-demand.
            val db = newBuffer(s) as DataBufferByte
            // should be fully compatible with IndexColorModels from SoftwareIndexedVideoRenderer
            rasters[index] = Raster.createInterleavedRaster(db, width, height, width, 1, intArrayOf(0), Point(0, 0))
        }
        // Thou shalt not best nullt!!! Sets currentscreen
        forcePalette()
    }

    /**
     * Clear the screenbuffer so when the whole screen will be recreated palettes will too
     * These screens represent a complete range of palettes for a specific gamma and specific screen
     */
    override fun forcePalette() {
        currentscreen = BufferedImage(cmaps[_usegamma][usepalette], rasters[DoomScreen.FG.ordinal], true, null)
    }
}