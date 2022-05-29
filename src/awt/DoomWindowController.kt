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

import java.awt.*
import awt.FullscreenOptions.FullscreenFunction
import doom.event_t
import m.Settings
import mochadoom.Engine
import mochadoom.Loggers
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level

/**
 * Display, its configuration and resolution related stuff,
 * DoomFrame creation, full-screen related code. Window recreation control.
 * That sort of things.
 */
class DoomWindowController<E, H> internal constructor(
    handlerClass: Class<EventHandler>,
    val device: GraphicsDevice,
    imageSource: Supplier<Image>,
    doomEventConsumer: Consumer<in event_t>,
    component: E,
    defaultWidth: Int,
    defaultHeight: Int
) : FullscreenOptions where E : Component, E : DoomWindow<E>, H : Enum<H>, H : EventBase<H> {
    val switcher: FullscreenFunction
    val defaultWidth: Int
    val defaultHeight: Int
    private val component: E
    val _observer: EventObserver<H>
    private var doomFrame: DoomFrame<E>

    /**
     * Default window size. It might change upon entering full screen, so don't consider it absolute. Due to letter
     * boxing and screen doubling, stretching etc. it might be different that the screen buffer (typically, larger).
     */
    private val dimension: DimensionImpl
    private var isFullScreen = false

    init {
        switcher = createFullSwitcher(device)!!
        this.component = component
        this.defaultWidth = defaultWidth
        this.defaultHeight = defaultHeight
        dimension = DimensionImpl(defaultWidth, defaultHeight)
        doomFrame = DoomFrame(dimension, component, imageSource)
        _observer = EventObserver(handlerClass, component, doomEventConsumer) as EventObserver<H>
        Toolkit.getDefaultToolkit().addAWTEventListener(
            { ev -> _observer.observe(ev) },
            ALL_EVENTS_MASK
        )
        sizeInit()
        doomFrame.turnOn()
    }

    private fun sizeInit() {
        try {
            if (!(Engine.getConfig()
                    .equals(Settings.fullscreen, java.lang.Boolean.TRUE) && switchToFullScreen())
            ) {
                updateSize()
            }
        } catch (e: Exception) {
            Loggers.getLogger(DoomWindow::class.java.name)
                .log(Level.SEVERE, String.format("Error creating DOOM AWT frame. Exiting. Reason: %s", e.message), e)
            throw e
        }
    }

    fun updateFrame() {
        doomFrame.update()
    }

    fun getObserver(): EventObserver<H> {
        return _observer
    }

    fun switchFullscreen(): Boolean {
        Loggers.getLogger(DoomFrame::class.java.name).log(Level.WARNING, "FULLSCREEN SWITHED")
        // remove the frame from view
        doomFrame.dispose()
        doomFrame = DoomFrame(dimension, component, doomFrame.imageSupplier)
        // change all the properties
        val ret = switchToFullScreen()
        // now show back the frame
        doomFrame.turnOn()
        return ret
    }

    /**
     * FULLSCREEN SWITCH CODE TODO: it's not enough to do this without also switching the screen's resolution.
     * Unfortunately, Java only has a handful of options which depend on the OS, driver, display, JVM etc. and it's not
     * possible to switch to arbitrary resolutions.
     *
     * Therefore, a "best fit" strategy with centering is used.
     */
    fun switchToFullScreen(): Boolean {
        if (!isFullScreen) {
            isFullScreen = device.isFullScreenSupported
            if (!isFullScreen) {
                return false
            }
        } else {
            isFullScreen = false
        }
        val displayMode = switcher[defaultWidth, defaultHeight]!!
        doomFrame.isUndecorated = isFullScreen

        // Full-screen mode
        device.fullScreenWindow = if (isFullScreen) doomFrame else null
        if (device.isDisplayChangeSupported) {
            device.displayMode = displayMode
        }
        component.validate()
        dimension.setSize(displayMode)
        updateSize()
        return isFullScreen
    }

    private fun updateSize() {
        doomFrame.preferredSize = if (isFullscreen()) dimension else null
        component.preferredSize = dimension
        component.setBounds(0, 0, defaultWidth - 1, defaultHeight - 1)
        component.background = Color.black
        doomFrame.renewGraphics()
    }

    fun isFullscreen(): Boolean {
        return isFullScreen
    }

    private inner class DimensionImpl internal constructor(width: Int, height: Int) : Dimension(),
        FullscreenOptions.Dimension {
        private var offsetX: Int
        private var offsetY: Int
        private var fitWidth: Int
        private var fitHeight: Int

        init {
            this.width = defaultWidth
            this.height = defaultHeight
            offsetY = 0
            offsetX = offsetY
            fitWidth = width
            fitHeight = height
        }

        override fun width(): Int {
            return width
        }

        override fun height(): Int {
            return height
        }

        override fun defWidth(): Int {
            return defaultWidth
        }

        override fun defHeight(): Int {
            return defaultHeight
        }

        override fun fitX(): Int {
            return fitWidth
        }

        override fun fitY(): Int {
            return fitHeight
        }

        override fun offsX(): Int {
            return offsetX
        }

        override fun offsY(): Int {
            return offsetY
        }

        fun setSize(mode: DisplayMode) {
            if (isFullScreen) {
                width = mode.width
                height = mode.height
                offsetX = super.offsX()
                offsetY = super.offsY()
                fitWidth = super.fitX()
                fitHeight = super.fitY()
            } else {
                width = defaultWidth
                height = defaultHeight
                offsetY = 0
                offsetX = offsetY
                fitWidth = width
                fitHeight = height
            }
        }

        /*companion object {
            private const val serialVersionUID = 4598094740125688728L
        }*/
    }

    companion object {
        private const val ALL_EVENTS_MASK = -0x1L
    }
}