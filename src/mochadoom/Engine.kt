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
package mochadoom


import awt.*
import awt.EventBase.KeyStateInterest
import awt.EventBase.KeyStateSatisfaction
import doom.*
import g.Signals.ScanCode
import i.*
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import doom.DoomMain

class Engine private constructor(vararg argv: String) {
    val cvm: CVarManager
    val cm: ConfigManager
    val windowController: DoomWindowController<*, EventHandler>
    private val DOOM: DoomMain<*, *>

    init {
        Engine.instance = this

        // reads command line arguments
        cvm = CVarManager(Arrays.asList(*argv))

        // reads default.cfg and mochadoom.cfg
        cm = ConfigManager()

        // intiializes stuff
        DOOM = DoomMain<Any, Any>()

        // opens a window
        // opens a window
        windowController =  /*cvm.bool(CommandVariable.AWTFRAME)
            ? */DoomWindow.createCanvasWindowController(
            { DOOM.graphicSystem.getScreenImage()!! },
            DOOM::PostEvent,
            DOOM.graphicSystem.getScreenWidth(),
            DOOM.graphicSystem.getScreenHeight()
        ) as DoomWindowController<*, EventHandler> /* : DoomWindow.createJPanelWindowController(
                DOOM.graphicSystem::getScreenImage,
                DOOM::PostEvent,
                DOOM.graphicSystem.getScreenWidth(),
                DOOM.graphicSystem.getScreenHeight()
            )*/
        windowController.getObserver().addInterest(
            KeyStateInterest({ obs ->
                EventHandler.fullscreenChanges(windowController.getObserver(), windowController.switchFullscreen())
                KeyStateSatisfaction.WANTS_MORE_ATE
            }, ScanCode.SC_LALT, ScanCode.SC_ENTER)
        )/*.addInterest(
            KeyStateInterest({ obs ->
                if (!windowController.isFullscreen()) {
                    switchMouseCapture(obs)
                }
                KeyStateSatisfaction.WANTS_MORE_PASS
            }, ScanCode.SC_LALT)
        )*/.addInterest(
            KeyStateInterest({ obs ->
                if (!windowController.isFullscreen()) {
                    EventHandler.menuCaptureChanges(obs, DOOM.menuactive)
                }
                KeyStateSatisfaction.WANTS_MORE_PASS
            }, ScanCode.SC_ESCAPE)
        ).addInterest(
            KeyStateInterest({ obs ->
                if (!windowController.isFullscreen()) {
                    EventHandler.menuCaptureChanges(obs, DOOM.getPaused())
                }
                KeyStateSatisfaction.WANTS_MORE_PASS
            }, ScanCode.SC_PAUSE)
        )
    }

    fun getWindowTitle(frames: Double): String {
        return if (cvm.bool(CommandVariable.SHOWFPS)) {
            String.format("%s - %s FPS: %.2f", Strings.MOCHA_DOOM_TITLE, DOOM.bppMode, frames)
        } else {
            String.format("%s - %s", Strings.MOCHA_DOOM_TITLE, DOOM.bppMode)
        }
    }

    companion object {
        @Volatile
        private lateinit var instance: Engine

        /**
         * Mocha Doom engine entry point
         */
        @Throws(IOException::class)
        @JvmStatic
        fun main(argv: Array<String>) {
            val local: Engine
            synchronized(Engine::class.java) { local = Engine(*argv) }
            /**
             * Add eventHandler listeners to JFrame and its Canvas elememt
             */
            /*content.addKeyListener(listener);
        content.addMouseListener(listener);
        content.addMouseMotionListener(listener);
        frame.addComponentListener(listener);
        frame.addWindowFocusListener(listener);
        frame.addWindowListener(listener);*/
            // never returns
            local.DOOM.setupLoop()
        }

        /**
         * Temporary solution. Will be later moved in more detalied place
         */
        fun updateFrame() {
            Engine.instance.windowController.updateFrame()
        }

        fun getEngine(): Engine? {
            var local: Engine = Engine.instance
            if (local == null) {
                synchronized(Engine::class.java) {
                    local = Engine.instance
                    if (local == null) {
                        try {
                            local = Engine()
                            Engine.instance = local
                        } catch (ex: IOException) {
                            Logger.getLogger(Engine::class.java.name).log(Level.SEVERE, null, ex)
                            throw Error("This launch is DOOMed")
                        }
                    }
                }
            }
            return local
        }

        fun getCVM(): CVarManager {
            return Engine.getEngine()!!.cvm
        }

        fun getConfig(): ConfigManager {
            return Engine.getEngine()!!.cm
        }
    }
}