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

import doom.DoomMain
import doom.player_t
import p.MobjFlags
import v.renderers.BppMode

/**
 * Combined colormap and light LUTs.
 * Used for z-depth cuing per column/row,
 * and other lighting effects (sector ambient, flash).
 *
 * @author velktron
 *
 * @param <V> The data type of the SCREEN
</V> */
class LightsAndColors<V>(DM: DoomMain<*, V>) {
    private val LC_DATA: LCData
    /** For HiColor, these are, effectively, a bunch of 555 RGB palettes,
     * for TrueColor they are a bunch of 32-bit ARGB palettes etc.
     * Only for indexed they represent index remappings.
     */
    /** "peg" this to the one from RendererData  */
    lateinit var colormaps: Array<V>

    /** lighttable_t**  */
    lateinit var walllights: Array<V>

    /** Use in conjunction with player.fixedcolormap  */
    var fixedcolormap: V? = null

    /**
     * Color tables for different players, translate a limited part to another
     * (color ramps used for suit colors).
     */
    lateinit var translationtables: Array<ByteArray>

    // bumped light from gun blasts
    var extralight = 0
    lateinit var scalelight: Array<Array<V?>>
    lateinit var scalelightfixed: Array<V?>
    lateinit var zlight: Array<Array<V?>>
    lateinit var spritelights: Array<V>

    init {
        LC_DATA = LCData(DM.bppMode)
    }

    fun lightBits(): Int {
        return LC_DATA.bpp.lightBits
    }

    fun lightBright(): Int {
        return LC_DATA.LIGHTBRIGHT
    }

    fun lightLevels(): Int {
        return LC_DATA.LIGHTLEVELS
    }

    fun lightScaleShift(): Int {
        return LC_DATA.LIGHTSCALESHIFT
    }

    fun lightSegShift(): Int {
        return LC_DATA.LIGHTSEGSHIFT
    }

    fun lightZShift(): Int {
        return LC_DATA.LIGHTZSHIFT
    }

    fun maxLightScale(): Int {
        return LC_DATA.MAXLIGHTSCALE
    }

    fun maxLightZ(): Int {
        return LC_DATA.MAXLIGHTZ
    }

    fun numColorMaps(): Int {
        return LC_DATA.NUMCOLORMAPS
    }

    /**
     * player_t.fixedcolormap have a range of 0..31 in vanilla.
     * We must respect it. However, we can have more lightlevels then vanilla.
     * So we must scale player_t.fixedcolormap by the difference with vanilla lightBits
     *
     * @param player
     * @return index in rich bit liteColorMaps
     */
    fun getFixedColormap(player: player_t): V {
        return if (LC_DATA.bpp.lightBits > 5) {
            colormaps[player.fixedcolormap shl LC_DATA.bpp.lightBits - 5]
        } else colormaps[player.fixedcolormap]
    }

    fun getTranslationTable(mobjflags: Int): ByteArray {
        return translationtables[(mobjflags and MobjFlags.MF_TRANSLATION shr MobjFlags.MF_TRANSSHIFT)]
    }

    private class LCData internal constructor(val bpp: BppMode) {
        /**
         * These two are tied by an inverse relationship. E.g. 256 levels, 0 shift
         * 128 levels, 1 shift ...etc... 16 levels, 4 shift (default). Or even less,
         * if you want.
         *
         * By setting it to the max however you get smoother light and get rid of
         * lightsegshift globally, too. Of course, by increasing the number of light
         * levels, you also put more memory pressure, and due to their being only
         * 256 colors to begin with, visually, there won't be many differences.
         */
        val LIGHTLEVELS: Int
        val LIGHTSEGSHIFT: Int

        /** Number of diminishing brightness levels.
         * There a 0-31, i.e. 32 LUT in the COLORMAP lump.
         * TODO: how can those be distinct from the light levels???
         */
        val NUMCOLORMAPS: Int
        // These are a bit more tricky to figure out though.
        /** Maximum index used for light levels of sprites. In practice,
         * it's capped by the number of light levels???
         *
         * Normally set to 48 (32 +16???)
         */
        val MAXLIGHTSCALE: Int

        /** Used to scale brightness of walls and sprites. Their "scale" is shifted by
         * this amount, and this results in an index, which is capped by MAXLIGHTSCALE.
         * Normally it's 12 for 32 levels, so 11 for 64, 10 for 128, ans 9 for 256.
         *
         */
        val LIGHTSCALESHIFT: Int

        /** This one seems arbitrary. Will auto-fit to 128 possible levels?  */
        val MAXLIGHTZ: Int
        val LIGHTBRIGHT: Int

        /** Normally 20 for 32 colormaps, applied to distance.
         * Formula: 25-LBITS
         *
         */
        val LIGHTZSHIFT: Int

        init {
            LIGHTLEVELS = 1 shl bpp.lightBits
            MAXLIGHTZ = LIGHTLEVELS * 4
            LIGHTBRIGHT = 2
            LIGHTSEGSHIFT = 8 - bpp.lightBits
            NUMCOLORMAPS = LIGHTLEVELS
            MAXLIGHTSCALE = 3 * LIGHTLEVELS / 2
            LIGHTSCALESHIFT = 17 - bpp.lightBits
            LIGHTZSHIFT = 25 - bpp.lightBits
        }
    }
}