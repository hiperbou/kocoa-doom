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
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.VolatileImage
import java.util.concurrent.BrokenBarrierException
import java.util.logging.Level

/**
 * Merged with ParallelTruecolorRenderer as it fixed both bugs of parallel and single-core versions
 * Now only parallel BufferedRenderer32 available in TrueColor mode because
 * single-core post-processing in software is too slow, and is the only way to apply tints and gamma properly
 * Parallelization of post-processing is so effective, that on my 4-core i7 it gives me at least equal FPS with
 * indexed renderer in no-JIT configuration of Java, and with JIT compiler it gives me much more FPS than indexed.
 * So, you will probably want this renderer if you have at least Core2Duo processor
 * - Good Sign 2017/04/12
 */
internal class BufferedRenderer32(rf: WithWadLoader<ByteArray, IntArray>) :
    SoftwareParallelVideoRenderer<ByteArray, IntArray>(rf, IntArray::class.java) {
    protected val raster: IntArray

    // VolatileImage speeds up delivery to VRAM - it is 30-40 fps faster then directly rendering BufferedImage
    protected var screen: VolatileImage

    // indicated whether machine display in the same mode as this renderer
    protected val compatible: Boolean = SoftwareParallelVideoRenderer.checkConfigurationTruecolor()
    protected val transparency: Int
    protected val _blurryTable: BlurryTable

    /**
     * This implementation will "tie" a BufferedImage to the underlying byte raster.
     *
     * NOTE: this relies on the ability to "tap" into a BufferedImage's backing array, in order to have fast writes
     * without setPixel/getPixel. If that is not possible, then we'll need to use a special renderer.
     */
    init {
        /**
         * Try to create as accelerated Images as possible - these would not lose
         * more performance from attempt (in contrast to 16-bit ones)
         */
        screen = SoftwareParallelVideoRenderer.GRAPHICS_CONF.createCompatibleVolatileImage(width, height)
        transparency = rf.getBppMode()!!.transparency
        /**
         * It is very probably that you have 32-bit display mode, so high chance of success,
         * and if you have, for example, 24-bit mode, the TYPE_INT_RGB BufferedImage will
         * still get accelerated
         */
        currentscreen = if (compatible) SoftwareParallelVideoRenderer.GRAPHICS_CONF.createCompatibleImage(
            width,
            height,
            transparency
        ) else BufferedImage(
            width,
            height,
            if (transparency == Transparency.TRANSLUCENT) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        )
        currentscreen!!.accelerationPriority = 1.0f

        // extract raster from the created image
        raster = ((currentscreen as BufferedImage).raster.dataBuffer as DataBufferInt).data
        _blurryTable = BlurryTable(liteColorMaps)
        /**
         * Create postprocess worker threads
         * 320 is dividable by 16, so any scale of it would
         * TODO: support for custom resolutions?
         */
        val len = raster.size
        val chunk: Int = len / SoftwareParallelVideoRenderer.PARALLELISM
        for (i in 0 until SoftwareParallelVideoRenderer.PARALLELISM) {
            paletteThreads[i] = IntPaletteThread(i * chunk, (i + 1) * chunk)
        }
    }

    /**
     * This method is accessed by AWTDoom to render the screen
     * As we use VolatileImage that can lose its contents, it must have special care.
     * doWriteScreen is called in the moment, when the VolatileImage is ready and
     * we can copy to it and post-process
     */
    override fun getScreenImage(): Image? {
        do {
            if (screen.validate(SoftwareParallelVideoRenderer.GRAPHICS_CONF) == VolatileImage.IMAGE_INCOMPATIBLE) {
                screen.flush()
                // old vImg doesn't work with new GraphicsConfig; re-create it
                screen =
                    SoftwareParallelVideoRenderer.GRAPHICS_CONF.createCompatibleVolatileImage(width, height)
            }
            doWriteScreen()
        } while (screen.contentsLost())
        return screen
    }

    override fun doWriteScreen() {
        for (i in 0 until SoftwareParallelVideoRenderer.PARALLELISM) {
            executor.execute(paletteThreads[i])
        }
        try {
            updateBarrier.await()
        } catch (e: InterruptedException) {
            Loggers.getLogger(BufferedRenderer32::class.java.name).log(Level.SEVERE, e, null)
        } catch (e: BrokenBarrierException) {
            Loggers.getLogger(BufferedRenderer32::class.java.name).log(Level.SEVERE, e, null)
        }
        val g = screen.createGraphics()
        g.drawImage(currentscreen, 0, 0, null)
        g.dispose()
    }

    /**
     * Returns pure color without tinting and gamma
     */
    override fun getBaseColor(color: Byte): Int {
        return palette!![color.toInt() and 0xFF]
    }

    override fun getBlurryTable(): BlurryTable {
        return _blurryTable
    }

    /**
     * Looks monstrous. Works swiss.
     * - Good Sign 2017/04/12
     */
    private inner class IntPaletteThread internal constructor(private val start: Int, private val stop: Int) :
        Runnable {
        private val FG: IntArray?

        init {
            FG = screens[DoomScreen.FG]
        }

        /**
         * BFG-9000. Definitely not the pesky pistol in the Indexed renderer
         */
        override fun run() {
            val t: ColorTint =
                (if (GRAYPAL_SET) ColorTint.GREY_TINTS else ColorTint.NORMAL_TINTS).get(usepalette)
            val LUT_R = t.LUT_r8[_usegamma]
            val LUT_G = t.LUT_g8[_usegamma]
            val LUT_B = t.LUT_b8[_usegamma]
            var i = start
            while (i < stop) {
                raster[i] =
                    (FG!![i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
                raster[i] =
                    (FG[i] and -0x1000000) + (LUT_R[FG[i] shr 16 and 0xFF].toInt() and 0xFF shl 16) + (LUT_G[FG[i] shr 8 and 0xFF].toInt() and 0xFF shl 8) + (LUT_B[FG[i++] and 0xFF].toInt() and 0xFF)
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