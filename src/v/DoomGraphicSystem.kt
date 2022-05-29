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
package v

import f.Wiper
import m.IRandom
import m.Settings
import mochadoom.Engine
import rr.patch_t
import v.graphics.Horizontal
import v.graphics.Plotter
import v.graphics.Plotter.Deep
import v.graphics.Plotter.Thick
import v.graphics.Plotter.Thin
import v.graphics.Relocation
import v.renderers.DoomScreen
import v.scale.VideoScale
import v.tables.BlurryTable
import java.awt.Image
import java.awt.Rectangle

/**
 * Refactored a lot of it; most notable changes:
 *
 * - 2d rendering methods are unified, standartized, generized, typehinted and incapsulated,
 * they are moved into separate interfaces and those interfaces to separate package
 *
 * - Fixed buggy 2d alrorithms, such as scaling, rewritten and parallelized column drawing logic,
 * unified and simplified calculation of areas on column-major surface
 *
 * - Renderer drivers are separated from drawing API and refactored a lot: fixed all issues with
 * improper gammas, lights and tinting, fixed delay before it applied on non-indexed render,
 * parallelized HiColor and TrueColor renderers. Only standard indexed 8-bit renderer is still
 * single-threaded, and he is very performant and is cool too!
 *
 * -- Good Sign 2017/04/12
 *
 * Notes about method hiding:
 * - (A comment on the notes below) It would be only wonderful, if it also will make reflection-access
 * (that what happens when some lame Java developer cannot access something and he just use reflection to
 * set method public) harder on these methods. I hate lame Java developers.
 *
 * Never trust clients. Never show them too much. So for those of you, who don't know something like that,
 * I introduce a good method of hiding interface methods. It is called Hiding By Complexity of Implementation.
 * Why I call it that? Because we strike a zombie sergeant using a shotgun.
 *
 * What do we need to hide? Complexity.
 * Why do we need to hide complexity? Because it is hard to use complex internal methods *properly*.
 * That is why they are internal. A here it is the main contract: if you want to use internal methods,
 * you create all their environment properly by sub-contracts of concrete interfaces.
 * So we hide complexity of usage by complexity of implementation the usable case. And the sergeant falls.
 *
 * A lot of interfaces with a lot of default methods. This is intended feature hiding mechanism.
 * Yes, it seems that a lot of PUBLIC default methods (default method is always public)
 * gains much access and power to one who use it... But actually, these interfaces restrict
 * much more, then static methods, because you have to actually *implement* the interface
 * to access any of these methods, and implementing these interfaces means implementing
 * a whole part of DoomGraphicsSystem. And I've thought out the interfaces contracts in the way
 * that if someone *implement* them on purpose, their methods will be safe and useful for him.
 *
 * -- Good Sign 2017/04/14
 *
 * DoomVideoSystem is now an interface, that all "video drivers" (whether do screen, disk, etc.)
 * must implement.
 *
 * 23/10/2011: Made into a generic type, which affects the underlying raw screen data
 * type. This should make -in theory- true color or super-indexed (>8 bits) video modes
 * possible. The catch is that everything directly meddling with the renderer must also
 * be aware of the underlying implementation. E.g. the various screen arrays will not be
 * necessarily byte[].
 *
 * @author Maes
 */
interface DoomGraphicSystem<T, V> {
    /**
     * Public API
     * See documentation in r2d package
     *
     * These are only methods DoomGraphicSystem wants to share from the whole insanely big package r2d
     * Because using only these methods, it is minimal risk of breaking something. Actually,
     * the only problematic cases should be passing null instead of argument or invalid coordinates.
     */
    /* SCREENS */
    fun getScreen(screenType: DoomScreen): V?
    fun getScalingX(): Int
    fun getScalingY(): Int
    fun getScreenWidth(): Int
    fun getScreenHeight(): Int
    fun screenCopy(srcScreen: V, dstScreen: V, relocation: Relocation)
    fun screenCopy(srcScreen: DoomScreen, dstScreen: DoomScreen)

    /* PALETTES */
    fun setUsegamma(gammalevel: Int)
    fun getUsegamma(): Int
    fun setPalette(palette: Int)
    fun getPalette(): Int
    fun getBaseColor(color: Byte): Int
    fun getBaseColor(color: Int): Int

    /* POINTS */
    fun point(x: Int, y: Int): Int
    fun point(x: Int, y: Int, width: Int): Int

    /* LINES */
    fun drawLine(plotter: Plotter<*>, x1: Int, x2: Int)

    /* PATCHES */
    fun DrawPatch(screen: DoomScreen, patch: patch_t, x: Int, y: Int, vararg flags: Int)
    fun DrawPatchCentered(screen: DoomScreen, patch: patch_t, y: Int, vararg flags: Int)
    fun DrawPatchCenteredScaled(screen: DoomScreen, patch: patch_t, vs: VideoScale?, y: Int, vararg flags: Int)
    fun DrawPatchScaled(screen: DoomScreen, patch: patch_t, vs: VideoScale?, x: Int, y: Int, vararg flags: Int)
    fun DrawPatchColScaled(screen: DoomScreen?, patch: patch_t, vs: VideoScale, x: Int, col: Int)

    /* RECTANGLES */
    fun CopyRect(srcScreenType: DoomScreen?, rectangle: Rectangle, dstScreenType: DoomScreen?)
    fun CopyRect(srcScreenType: DoomScreen?, rectangle: Rectangle, dstScreenType: DoomScreen?, dstPoint: Int)
    fun FillRect(screenType: DoomScreen?, rectangle: Rectangle, patternSrc: V, pattern: Horizontal?)
    fun FillRect(screenType: DoomScreen?, rectangle: Rectangle, patternSrc: V, point: Int)
    fun FillRect(screenType: DoomScreen, rectangle: Rectangle, color: Int)
    fun FillRect(screenType: DoomScreen, rectangle: Rectangle, color: Byte)

    /* BLOCKS */
    fun convertPalettedBlock(vararg src: Byte): V
    fun ScaleBlock(block: V, vs: VideoScale, width: Int, height: Int): V
    fun TileScreen(dstScreen: DoomScreen, block: V, blockArea: Rectangle)
    fun TileScreenArea(dstScreen: DoomScreen, screenArea: Rectangle, block: V, blockArea: Rectangle)
    fun DrawBlock(dstScreen: DoomScreen, block: V, sourceArea: Rectangle, destinationPoint: Int)

    /**
     * No matter how complex/weird/arcane palette manipulations you do internally, the AWT module
     * must always be able to "tap" into what's the current, "correct" screen after all manipulation and
     * color juju was applied. Call after a palette/gamma change.
     */
    fun getScreenImage(): Image?

    /**
     * Saves screenshot to a file "filling a planar buffer to linear"
     * (I cannot guarantee I understood - Good Sign 2017/04/01)
     * @param name
     * @param screen
     * @return true if succeed
     */
    fun writeScreenShot(name: String?, screen: DoomScreen?): Boolean

    /**
     * If the renderer operates color maps, get them
     * Used for scene rendering
     */
    fun getColorMap(): Array<V>

    /**
     * Plotter for point-by-point drawing of AutoMap
     */
    fun createPlotter(screen: DoomScreen): Plotter<V?> {
        return when (Engine.getConfig()
            .getValue<Plotter.Style>(Settings.automap_plotter_style, Plotter.Style::class.java)) {
            Plotter.Style.Thick -> Thick(getScreen(screen), getScreenWidth(), getScreenHeight())
            Plotter.Style.Deep -> Deep(getScreen(screen), getScreenWidth(), getScreenHeight())
            else -> Thin(getScreen(screen), getScreenWidth())
        }
    }

    fun createWiper(random: IRandom): Wiper
    fun getBlurryTable(): BlurryTable

    /**
     * Indexed renderer needs to reset its image
     */
    fun forcePalette() {}

    companion object {
        /**
         * Flags used by patch drawing functions
         * Now working as separate and optional varargs argument
         * Added by _D_. Unsure if I should use VSI objects instead, as they
         * already carry scaling information which doesn't need to be repacked...
         */
        const val V_NOSCALESTART = 0x00010000 // dont scale x,y, start coords
        const val V_SCALESTART = 0x00020000 // scale x,y, start coords
        const val V_SCALEPATCH = 0x00040000 // scale patch
        const val V_NOSCALEPATCH = 0x00080000 // don't scale patch
        const val V_WHITEMAP = 0x00100000 // draw white (for v_drawstring)    
        const val V_FLIPPEDPATCH = 0x00200000 // flipped in y
        const val V_TRANSLUCENTPATCH = 0x00400000 // draw patch translucent    
        const val V_PREDIVIDE = 0x00800000 // pre-divide by best x/y scale.    
        const val V_SCALEOFFSET = 0x01000000 // Scale the patch offset
        const val V_NOSCALEOFFSET = 0x02000000 // dont's cale patch offset
        const val V_SAFESCALE = 0x04000000 // scale only by minimal scale of x/y instead of both
    }
}