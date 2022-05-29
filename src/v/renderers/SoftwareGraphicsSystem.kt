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
package v.renderers

import doom.CommandVariable
import f.Wiper
import m.IRandom
import m.Settings
import mochadoom.Engine
import rr.patch_t
import s.*
import v.DoomGraphicSystem
import v.graphics.*
import v.renderers.RendererFactory.WithWadLoader
import v.scale.VideoScale
import v.tables.GammaTables
import v.tables.Playpal
import java.awt.Image
import java.awt.Rectangle
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.awt.image.DataBufferUShort
import java.lang.Boolean
import kotlin.Array
import kotlin.Byte
import kotlin.ByteArray
import kotlin.Int
import kotlin.IntArray
import kotlin.ShortArray
import kotlin.String
import kotlin.UnsupportedOperationException

/**
 * A package-protected hub, concentrating together public graphics APIs
 * and support default methods from their interfaces
 *
 * Problems: we cannot change resolution on-fly because it will require re-creating buffers, rasters, etc
 * TODO: decide what needs to be reset and implement resolution change methods (flushing buffers, expanding arrays, etc)
 * (dont forget to run gc!)
 *
 * @author Good Sign
 */
internal abstract class SoftwareGraphicsSystem<T, V>(rf: WithWadLoader<T, V>, bufferType: Class<V>) :
    DoomGraphicSystem<T, V>, Rectangles<V, DoomScreen>, Blocks<V, DoomScreen>, Patches<V, DoomScreen>, Lines {
    /**
     * Each screen is [SCREENWIDTH*SCREENHEIGHT]; This is what the various modules (menu, automap, renderer etc.) get to
     * manipulate at the pixel level. To go beyond 8 bit displays, these must be extended
     */
    protected val screens: Map<DoomScreen, V>
    protected val vs: VideoScale?
    protected val bufferType: Class<V>

    /**
     * They are used in HiColor and TrueColor modes and are separated from tinting and gammas
     * Colormaps are now part of the base software renderer. This allows some flexibility over manipulating them.
     */
    protected val liteColorMaps: Array<V>
    protected val palette: V

    /**
     * Indexed renderer changes this property often when switching gammas and palettes
     * For HiColor and TrueColor renderer it may change or not, depending on compatibility of
     * graphics configuration: if VolatileImage is used, this changes as soon as it may invalidate
     */
    protected var currentscreen: Image? = null

    /**
     * Dynamic properties:
     */
    protected var width: Int
    protected var height: Int
    protected var bufferLength: Int
    protected var _usegamma = 0
    protected var usepalette = 0
    private fun palette(rf: WithWadLoader<T, V>): V {
        /*final byte[] */
        playpal = if (Engine.getCVM()
                .bool(CommandVariable.GREYPAL)
        ) Playpal.greypal() else if (Engine.getCVM().bool(CommandVariable.NOPLAYPAL)) Playpal.properPlaypal(
            null
        ) else rf.getWadLoader()?.LoadPlaypal()
        /**
         * In Indexed mode, read PLAYPAL lump can be used directly
         */
        return if (bufferType == ByteArray::class.java) playpal as V
        /**
         * In HiColor or TrueColor translate PLAYPAL to real colors
         */
        else if (bufferType == ShortArray::class.java) paletteHiColor(playpal!!) as V else paletteTrueColor(playpal!!) as V
    }

    private var playpal: ByteArray? = null

    /**
     * @param vs video scale info
     * @param playpal palette
     */
    init {
        // Defaults
        vs = rf.getVideoScale()
        width = vs!!.getScreenWidth()
        height = vs.getScreenHeight()
        this.bufferType = bufferType
        bufferLength = width * height
        screens = DoomScreen.mapScreensToBuffers<V>(bufferType, bufferLength)
        palette = palette(rf)
        liteColorMaps = colormap(rf)!!
    }

    private fun colormap(rf: WithWadLoader<T, V>): Array<V>? {
        val colormapEnabled = (!Engine.getCVM().bool(CommandVariable.NOCOLORMAP)
                && Engine.getConfig().equals(Settings.enable_colormap_lump, Boolean.TRUE))

        if(playpal == null) return null
        val playpal = playpal!!

        /**
         * In Indexed mode, read COLORMAP lump can be used directly
         */
        return if (bufferType == ByteArray::class.java) if (colormapEnabled) rf.getWadLoader()!!
            .LoadColormap() as Array<V>? else BuildLightsI(
            paletteTrueColor(
                playpal
            )!!
        ) as Array<V>?
        /**
         * In HiColor or TrueColor generate colormaps with lights
         */
        else if (bufferType == ShortArray::class.java) if (colormapEnabled // HiColor, check for cfg setting and command line argument -nocolormap
        ) BuildLights15(paletteTrueColor(playpal), rf.getWadLoader()!!.LoadColormap()) as Array<V>? else BuildLights15(
            paletteTrueColor(playpal)
        ) as Array<V>? else if (colormapEnabled // TrueColor, check for cfg setting and command line argument -nocolormap
        ) BuildLights24(
            palette as IntArray,
            rf.getWadLoader()!!.LoadColormap()
        ) as Array<V>? else BuildLights24(palette as IntArray) as Array<V>?
    }

    /**
     * Getters
     */
    override fun getUsegamma(): Int {
        return _usegamma
    }

    override fun getPalette(): Int {
        return usepalette
    }

    override fun getScreenHeight(): Int {
        return height
    }

    override fun getScreenWidth(): Int {
        return width
    }

    override fun getScalingX(): Int {
        return vs!!.getScalingX()
    }

    override fun getScalingY(): Int {
        return vs!!.getScalingY()
    }

    override fun getScreen(screenType: DoomScreen): V? {
        return screens[screenType]
    }

    override fun getScreenImage(): Image? {
        return currentscreen /* may be null */
    }

    /**
     * API route delegating
     */
    override fun screenCopy(srcScreen: V, dstScreen: V, relocation: Relocation) {
        super<Rectangles>.screenCopy(srcScreen, dstScreen, relocation)
    }

    override fun screenCopy(srcScreen: DoomScreen, dstScreen: DoomScreen) {
        super<Rectangles>.screenCopy(srcScreen, dstScreen)
    }

    override fun getBaseColor(color: Int): Int {
        return super<Rectangles>.getBaseColor(color)
    }

    override fun point(x: Int, y: Int): Int {
        return super<Rectangles>.point(x, y)
    }

    override fun point(x: Int, y: Int, width: Int): Int {
        return super<Rectangles>.point(x, y, width)
    }

    override fun drawLine(plotter: Plotter<*>, x1: Int, x2: Int) {
        super<Lines>.drawLine(plotter, x1, x2)
    }

    override fun DrawPatch(screen: DoomScreen, patch: patch_t, x: Int, y: Int, vararg flags: Int) {
        super<Patches>.DrawPatch(screen, patch, x, y, *flags)
    }

    override fun DrawPatchCentered(screen: DoomScreen, patch: patch_t, y: Int, vararg flags: Int) {
        super<Patches>.DrawPatchCentered(screen, patch, y, *flags)
    }

    override fun DrawPatchCenteredScaled(
        screen: DoomScreen,
        patch: patch_t,
        vs: VideoScale?,
        y: Int,
        vararg flags: Int
    ) {
        super<Patches>.DrawPatchCenteredScaled(screen, patch, vs, y, *flags)
    }

    override fun DrawPatchScaled(
        screen: DoomScreen,
        patch: patch_t,
        vs: VideoScale?,
        x: Int,
        y: Int,
        vararg flags: Int
    ) {
        super<Patches>.DrawPatchScaled(screen, patch, vs, x, y, *flags)
    }

    override fun DrawPatchColScaled(screen: DoomScreen?, patch: patch_t, vs: VideoScale, x: Int, col: Int) {
        super<Patches>.DrawPatchColScaled(screen, patch, vs, x, col)
    }

    override fun CopyRect(srcScreenType: DoomScreen?, rectangle: Rectangle, dstScreenType: DoomScreen?) {
        super<Rectangles>.CopyRect(srcScreenType, rectangle, dstScreenType)
    }

    override fun CopyRect(srcScreenType: DoomScreen?, rectangle: Rectangle, dstScreenType: DoomScreen?, dstPoint: Int) {
        super<Rectangles>.CopyRect(srcScreenType, rectangle, dstScreenType, dstPoint)
    }

    override fun FillRect(screenType: DoomScreen?, rectangle: Rectangle, patternSrc: V, pattern: Horizontal?) {
        super<Rectangles>.FillRect(screenType, rectangle, patternSrc, pattern)
    }

    override fun FillRect(screenType: DoomScreen?, rectangle: Rectangle, patternSrc: V, point: Int) {
        super<Rectangles>.FillRect(screenType, rectangle, patternSrc, point)
    }

    override fun FillRect(screenType: DoomScreen, rectangle: Rectangle, color: Int) {
        super<Rectangles>.FillRect(screenType, rectangle, color)
    }

    override fun FillRect(screenType: DoomScreen, rectangle: Rectangle, color: Byte) {
        super<Rectangles>.FillRect(screenType, rectangle, color)
    }

    override fun ScaleBlock(block: V, vs: VideoScale, width: Int, height: Int): V {
        return super<Rectangles>.ScaleBlock(block, vs, width, height)
    }

    override fun TileScreen(dstScreen: DoomScreen, block: V, blockArea: Rectangle) {
        super<Rectangles>.TileScreen(dstScreen, block, blockArea)
    }

    override fun TileScreenArea(dstScreen: DoomScreen, screenArea: Rectangle, block: V, blockArea: Rectangle) {
        super<Rectangles>.TileScreenArea(dstScreen, screenArea, block, blockArea)
    }

    override fun DrawBlock(dstScreen: DoomScreen, block: V, sourceArea: Rectangle, destinationPoint: Int) {
        super<Rectangles>.DrawBlock(dstScreen, block, sourceArea, destinationPoint)
    }

    override fun createPlotter(screen: DoomScreen): Plotter<V?> {
        return super<DoomGraphicSystem>.createPlotter(screen)
    }

    /**
     * I_SetPalette
     *
     * Any bit-depth specific palette manipulation is performed by the VideoRenderer. It can range from simple
     * (paintjob) to complex (multiple BufferedImages with locked data bits...) ugh!
     *
     * In order to change palette properly, we must invalidate
     * the colormap cache if any, otherwise older colormaps will persist.
     * The screen must be fully updated then
     *
     * @param palette index (normally between 0-14).
     */
    override fun setPalette(palette: Int) {
        usepalette = palette % Palettes.NUM_PALETTES
        forcePalette()
    }

    override fun setUsegamma(gamma: Int) {
        _usegamma = gamma % GammaTables.LUT.size
        /**
         * Because of switching gamma stops powerup palette except for invlunerablity
         * Settings.fixgammapalette handles the fix
         */
        if (Engine.getConfig().equals(Settings.fix_gamma_palette, false)) { //TODO: Boolean.FALSE
            usepalette = 0
        }
        forcePalette()
    }

    override fun getColorMap(): Array<V> {
        return liteColorMaps
    }

    fun newBuffer(screen: DoomScreen): DataBuffer {
        val buffer = screens[screen]!!
        if (buffer::class.java == IntArray::class.java) {
            return DataBufferInt(buffer as IntArray?, (buffer as IntArray?)!!.size)
        } else if (buffer::class.java == ShortArray::class.java) {
            return DataBufferUShort(buffer as ShortArray?, (buffer as ShortArray?)!!.size)
        } else if (buffer::class.java == ByteArray::class.java) {
            return DataBufferByte(buffer as ByteArray?, (buffer as ByteArray?)!!.size)
        }
        throw UnsupportedOperationException(
            String.format(
                "SoftwareVideoRenderer does not support %s buffers",
                buffer::class.java
            )
        )
    }

    override fun createWiper(random: IRandom): Wiper {
        return Wipers.createWiper(random, this, DoomScreen.WS, DoomScreen.WE, DoomScreen.FG)
    }
}