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

import m.MenuMisc
import v.graphics.Palettes
import v.renderers.RendererFactory.WithWadLoader
import v.tables.BlurryTable
import v.tables.GammaTables
import java.awt.image.IndexColorModel

/**
 * @author Good Sign
 * @author velktron
 */
internal abstract class SoftwareIndexedVideoRenderer(rf: WithWadLoader<ByteArray, ByteArray>) :
    SoftwareGraphicsSystem<ByteArray, ByteArray>(rf, ByteArray::class.java) {
    /**
     * Indexed renderers keep separate color models for each colormap (intended as gamma levels) and palette levels
     */
    protected val cmaps = Array(GammaTables.LUT.size) { arrayOfNulls<IndexColorModel>(Palettes.NUM_PALETTES) }
    protected val _blurryTable: BlurryTable

    init {
        /**
         * create gamma levels
         * Now we can reuse existing array of cmaps, not allocating more memory
         * each time we change gamma or pick item
         */
        cmapIndexed(cmaps, palette)
        _blurryTable = BlurryTable(liteColorMaps)
    }

    override fun getBaseColor(color: Byte): Int {
        return color.toInt()
    }

    override fun convertPalettedBlock(vararg src: Byte): ByteArray {
        return src
    }

    override fun getBlurryTable(): BlurryTable {
        return _blurryTable
    }

    override fun writeScreenShot(name: String?, screen: DoomScreen?): Boolean {
        // munge planar buffer to linear
        //DOOM.videoInterface.ReadScreen(screens[screen.ordinal()]);
        MenuMisc.WritePNGfile(name, screens[screen], width, height, cmaps[_usegamma][usepalette])
        return true
    }
}