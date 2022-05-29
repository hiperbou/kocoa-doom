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

import doom.CommandVariable
import m.MenuMisc
import m.Settings
import mochadoom.Engine
import v.renderers.RendererFactory.WithWadLoader
import java.awt.GraphicsEnvironment
import java.awt.image.ColorModel
import java.util.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Base for HiColor and TrueColor parallel renderers
 *
 * @author Good Sign
 * @author velktron
 */
internal abstract class SoftwareParallelVideoRenderer<T, V>(rf: WithWadLoader<T, V>, bufferType: Class<V>) :
    SoftwareGraphicsSystem<T, V>(rf, bufferType) {
    protected val GRAYPAL_SET: Boolean = Engine.getCVM().bool(CommandVariable.GREYPAL)

    /**
     * We do not need to clear caches anymore - pallettes are applied on post-process
     * - Good Sign 2017/04/12
     *
     * MEGA HACK FOR SUPER-8BIT MODES
     */
    protected val colcache = HashMap<Int, V>()

    // Threads stuff
    protected val paletteThreads = arrayOfNulls<Runnable>(SoftwareParallelVideoRenderer.PARALLELISM)
    protected val executor: Executor = Executors.newFixedThreadPool(SoftwareParallelVideoRenderer.PARALLELISM)
    protected val updateBarrier = CyclicBarrier(SoftwareParallelVideoRenderer.PARALLELISM + 1)
    abstract fun doWriteScreen()
    override fun writeScreenShot(name: String?, screen: DoomScreen?): Boolean {
        // munge planar buffer to linear
        //DOOM.videoInterface.ReadScreen(screens[screen.ordinal()]);
        val screenBuffer = screens[screen]!!
        if (screenBuffer::class.java == ShortArray::class.java) {
            MenuMisc.WritePNGfile(name, screenBuffer as ShortArray?, width, height)
        } else {
            MenuMisc.WritePNGfile(name, screenBuffer as IntArray?, width, height)
        }
        return true
    }

    /**
     * Used to decode textures, patches, etc... It converts to the proper palette,
     * but does not apply tinting or gamma - yet
     */
    override fun convertPalettedBlock(vararg data: Byte): V {
        val isShort = bufferType == ShortArray::class.java
        /**
         * We certainly do not need to cache neither single color value, nor empty data
         * - Good Sign 2017/04/09
         */
        if (data.size > 1) {
            return if (isShort) {
                colcache.computeIfAbsent(Arrays.hashCode(data)) { h: Int? ->
                    //System.out.printf("Generated cache for %d\n",data.hashCode());
                    val stuff = ShortArray(data.size)
                    for (i in data.indices) {
                        stuff[i] = getBaseColor(data[i]).toShort()
                    }
                    stuff as V
                }
            } else {
                colcache.computeIfAbsent(Arrays.hashCode(data)) { h: Int? ->
                    //System.out.printf("Generated cache for %d\n",data.hashCode());
                    val stuff = IntArray(data.size)
                    for (i in data.indices) {
                        stuff[i] = getBaseColor(data[i])
                    }
                    stuff as V
                }
            }
        } else if (data.size == 0) {
            return (if (isShort) EMPTY_SHORT_PALETTED_BLOCK else EMPTY_INT_PALETTED_BLOCK) as V
        }
        return (if (isShort) shortArrayOf(getBaseColor(data[0]).toShort()) else intArrayOf(getBaseColor(data[0]))) as V
    }

    companion object {
        // How many threads it will use, but default it uses all avalable cores
        private val EMPTY_INT_PALETTED_BLOCK = IntArray(0)
        private val EMPTY_SHORT_PALETTED_BLOCK = ShortArray(0)

        @JvmStatic
        protected val PARALLELISM: Int =
            Engine.getConfig().getValue<Int>(Settings.parallelism_realcolor_tint, Int::class.java)
        @JvmStatic
        protected val GRAPHICS_CONF = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration

        /**
         * It will render much faster on machines with display already in HiColor mode
         * Maybe even some acceleration will be possible
         */
        fun checkConfigurationHicolor(): Boolean {
            val cm: ColorModel = SoftwareParallelVideoRenderer.GRAPHICS_CONF.getColorModel()
            val cps = cm.numComponents
            return cps == 3 && cm.getComponentSize(0) == 5 && cm.getComponentSize(1) == 5 && cm.getComponentSize(2) == 5
        }

        /**
         * It will render much faster on machines with display already in TrueColor mode
         * Maybe even some acceleration will be possible
         */
        fun checkConfigurationTruecolor(): Boolean {
            val cm: ColorModel = SoftwareParallelVideoRenderer.GRAPHICS_CONF.getColorModel()
            val cps = cm.numComponents
            return cps == 3 && cm.getComponentSize(0) == 8 && cm.getComponentSize(1) == 8 && cm.getComponentSize(2) == 8
        }
    }
}