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
package awt

import awt.FullscreenOptions.FullscreenFunction
import awt.FullscreenOptions.StretchMode
import m.Settings
import mochadoom.Engine
import java.awt.*
import java.awt.image.ImageObserver

/**
 * Full-screen switch and scale governor
 *
 * @author Good Sign
 */
interface FullscreenOptions {
    enum class InterpolationMode {
        Nearest, Bilinear, Biqubic
    }

    enum class StretchMode(
        val widthFun: Fitter,
        val heightFun: Fitter,
        val offsXFun: Fitter,
        val offsYFun: Fitter
    ) {
        Centre(
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Math.min(defW, w) },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Math.min(defH, h) },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Math.max(0, (w - defW) / 2) },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Math.max(0, (h - defH) / 2) }
        ),
        Stretch(
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> w },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> h },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> 0 },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> 0 }
        ),
        Fit(
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> (defW * minScale(w, defW, h, defH)).toInt() },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> (defH * minScale(w, defW, h, defH)).toInt() },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> (w - (defW * minScale(w, defW, h, defH)).toInt()) / 2 },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> (h - (defH * minScale(w, defW, h, defH)).toInt()) / 2 }
        ),
        Aspect_4_3(
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Fit.widthFun.fit(w, defW, h, (defH * 1.2f).toInt()) },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Fit.heightFun.fit(w, defW, h, (defH * 1.2f).toInt()) },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Fit.offsXFun.fit(w, defW, h, (defH * 1.2f).toInt()) },
            Fitter { w: Int, defW: Int, h: Int, defH: Int -> Fit.offsYFun.fit(w, defW, h, (defH * 1.2f).toInt()) }
        );

        companion object {
            private fun minScale(w: Int, defW: Int, h: Int, defH: Int): Float {
                val scaleX = w / defW.toFloat()
                val scaleY = h / defH.toFloat()
                return Math.min(scaleX, scaleY)
            }
        }
    }

    enum class FullMode {
        Best, Native
    }

    interface Dimension {
        fun width(): Int
        fun height(): Int
        fun defWidth(): Int
        fun defHeight(): Int
        fun fitX(): Int {
            return FullscreenOptions.STRETCH.widthFun.fit(width(), defWidth(), height(), defHeight())
        }

        fun fitY(): Int {
            return FullscreenOptions.STRETCH.heightFun.fit(width(), defWidth(), height(), defHeight())
        }

        fun offsX(): Int {
            return FullscreenOptions.STRETCH.offsXFun.fit(width(), defWidth(), height(), defHeight())
        }

        fun offsY(): Int {
            return FullscreenOptions.STRETCH.offsYFun.fit(width(), defWidth(), height(), defHeight())
        }
    }

    fun interface Fitter {
        fun fit(width: Int, defWidth: Int, height: Int, defHeight: Int): Int
    }

    fun options(graphics: Graphics2D) {
        when (FullscreenOptions.INTERPOLATION) {
            InterpolationMode.Nearest -> graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            )
            InterpolationMode.Bilinear -> graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            )
            InterpolationMode.Biqubic -> graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            )
        }
    }

    fun draw(graphics: Graphics2D, image: Image?, dim: FullscreenOptions.Dimension, observer: ImageObserver?) {
        graphics.drawImage(image, dim.offsX(), dim.offsY(), dim.fitX(), dim.fitY(), observer)
    }

    fun createFullSwitcher(device: GraphicsDevice): FullscreenFunction? {
        when (FullscreenOptions.FULLMODE) {
            FullMode.Best -> return FullscreenSwitch(device, DisplayModePicker(device))
            FullMode.Native -> return FullscreenFunction { w: Int, h: Int -> device.displayMode }
        }
        throw Error("Enum reflection overuse?")
    }

    fun interface FullscreenFunction {
        operator fun get(width: Int, height: Int): DisplayMode?
    }

    class FullscreenSwitch(private val dev: GraphicsDevice, private val dmp: DisplayModePicker) : FullscreenFunction {
        private var oldDisplayMode: DisplayMode? = null
        private var displayMode: DisplayMode? = null
        override fun get(width: Int, height: Int): DisplayMode? {
            if (oldDisplayMode == null) {
                // In case we need to revert.
                oldDisplayMode = dev.displayMode
                // TODO: what if bit depths are too small?
                displayMode = dmp.pickClosest(width, height)
            } else {
                // We restore the original resolution
                displayMode = oldDisplayMode
                oldDisplayMode = null
            }
            return displayMode
        }
    }

    companion object {
        val FULLMODE: FullMode =
            Engine.getConfig().getValue<FullMode>(Settings.fullscreen_mode, FullMode::class.java)!!
        val STRETCH: StretchMode =
            Engine.getConfig().getValue<StretchMode>(Settings.fullscreen_stretch, StretchMode::class.java)!!
        val INTERPOLATION: InterpolationMode = Engine.getConfig()
            .getValue<InterpolationMode>(Settings.fullscreen_interpolation, InterpolationMode::class.java)!!
    }
}