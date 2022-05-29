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

import mochadoom.Loggers
import v.renderers.RendererFactory.WithWadLoader
import v.tables.BlurryTable
import v.tables.ColorTint
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.DataBufferUShort
import java.awt.image.VolatileImage
import java.util.concurrent.BrokenBarrierException
import java.util.logging.Level

/**
 * Redesigned to follow as closely as possible its 32-bit complement
 *
 * It ulitizes now the same parallelization as 32-bit TrueColor renderer,
 * becasue it allows palettes and gammas to be applied properly on post-process.
 * The separate LUT's are generated for this renderer
 *
 * Most likely, this renderer will be the least performant.
 * - Good Sign 2017/04/12
 */
internal class BufferedRenderer16(rf: WithWadLoader<ByteArray, ShortArray>) :
    SoftwareParallelVideoRenderer<ByteArray, ShortArray>(rf, ShortArray::class.java) {
    protected val raster: ShortArray

    // VolatileImage speeds up delivery to VRAM - it is 30-40 fps faster then directly rendering BufferedImage
    protected var screen: VolatileImage? = null

    // indicated whether machine display in the same mode as this renderer
    protected val compatible: Boolean = checkConfigurationHicolor()
    protected val _blurryTable: BlurryTable

    /**
     * This implementation will "tie" a bufferedimage to the underlying byte raster.
     *
     * NOTE: this relies on the ability to "tap" into a BufferedImage's backing array, in order to have fast writes
     * without setpixel/getpixel. If that is not possible, then we'll need to use a special renderer.
     */
    init {
        /**
         * There is only sense to create and use VolatileImage if it can use native acceleration
         * which is impossible if we rendering into alien color space or bit depth
         */
        if (compatible) {
            // if we lucky to have 16-bit accelerated screen
            screen = GRAPHICS_CONF.createCompatibleVolatileImage(width, height)
            currentscreen = GRAPHICS_CONF.createCompatibleImage(width, height)
        } else {
            currentscreen = BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB)
        }

        // extract raster from the created image
        currentscreen!!.accelerationPriority = 1.0f
        raster = ((currentscreen as BufferedImage).raster.dataBuffer as DataBufferUShort).data
        _blurryTable = BlurryTable(liteColorMaps)
        /**
         * Create postprocess worker threads
         * 320 is dividable by 16, so any scale of it would
         * TODO: support for custom resolutions?
         */
        val len = raster.size
        val chunk: Int = len / PARALLELISM
        for (i in 0 until PARALLELISM) {
            paletteThreads[i] = ShortPaletteThread(i * chunk, (i + 1) * chunk)
        }
    }

    /**
     * This method is accessed by AWTDoom to render the screen
     * As we use VolatileImage that can lose its contents, it must have special care.
     * doWriteScreen is called in the moment, when the VolatileImage is ready and
     * we can copy to it and post-process
     *
     * If we use incompatible display, just draw our existing BufferedImage - it would be faster
     */
    override fun getScreenImage(): Image? {
        doWriteScreen()
        if (!compatible) {
            return currentscreen
        } else do {
            if (screen!!.validate(GRAPHICS_CONF) == VolatileImage.IMAGE_INCOMPATIBLE) {
                screen!!.flush()
                // old vImg doesn't work with new GraphicsConfig; re-create it
                screen =
                    GRAPHICS_CONF.createCompatibleVolatileImage(width, height)
            }
            val g = screen!!.createGraphics()
            g.drawImage(currentscreen, 0, 0, null)
            g.dispose()
        } while (screen!!.contentsLost())
        return screen
    }

    override fun doWriteScreen() {
        for (i in 0 until PARALLELISM) {
            executor.execute(paletteThreads[i])
        }
        try {
            updateBarrier.await()
        } catch (e: InterruptedException) {
            Loggers.getLogger(BufferedRenderer32::class.java.name).log(Level.SEVERE, e, null)
        } catch (e: BrokenBarrierException) {
            Loggers.getLogger(BufferedRenderer32::class.java.name).log(Level.SEVERE, e, null)
        }
    }

    override fun getBaseColor(color: Byte): Int {
        return palette!![color.toInt() and 0xFF].toInt()
    }

    override fun getBlurryTable(): BlurryTable {
        return _blurryTable
    }

    /**
     * Looks monstrous. Works swiss.
     * - Good Sign 2017/04/12
     */
    private inner class ShortPaletteThread internal constructor(private val start: Int, private val stop: Int) :
        Runnable {
        private val FG: ShortArray?

        init {
            FG = screens[DoomScreen.FG]
        }

        /**
         * For BFG-9000 look at BufferedRenderer32.IntPaletteThread
         * But there is BFG-2704
         */
        override fun run() {
            val t: ColorTint =
                (if (GRAYPAL_SET) ColorTint.GREY_TINTS else ColorTint.NORMAL_TINTS).get(usepalette)
            val LUT_R = t.LUT_r5[_usegamma]
            val LUT_G = t.LUT_g5[_usegamma]
            val LUT_B = t.LUT_b5[_usegamma]
            var i = start
            while (i < stop) {
                raster[i] =
                    (LUT_R[FG!![i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
                raster[i] =
                    (LUT_R[FG[i].toInt() shr 10 and 0x1F].toInt() and 0x1F shl 10 or (LUT_G[FG[i].toInt() shr 5 and 0x1F].toInt() and 0x1F shl 5) or (LUT_B[FG[i++].toInt() and 0x1F].toInt() and 0x1F)).toShort()
            }
            try {
                updateBarrier.await()
            } catch (e: InterruptedException) {
                Loggers.getLogger(BufferedRenderer32::class.java.name).log(Level.WARNING, e, null)
            } catch (e: BrokenBarrierException) {
                Loggers.getLogger(BufferedRenderer32::class.java.name).log(Level.WARNING, e, null)
            }
        }
    }
}