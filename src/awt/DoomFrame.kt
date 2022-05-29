package awt

import doom.CommandVariable
import mochadoom.Engine
import mochadoom.Loggers
import java.awt.*
import java.util.function.Supplier
import java.util.logging.Level
import javax.swing.JFrame

/**
 * Common code for Doom's video frames
 */
class DoomFrame<Window> internal constructor(
    /**
     * Default window size. It might change upon entering full screen, so don't consider it absolute. Due to letter
     * boxing and screen doubling, stretching etc. it might be different that the screen buffer (typically, larger).
     */
    val dim: FullscreenOptions.Dimension,
    /**
     * Canvas or JPanel
     */
    private val content: Window,
    /**
     * Provider of video content to display
     */
    val imageSupplier: Supplier<out Image>
) : JFrame(), FullscreenOptions where Window : Component?, Window : DoomWindow<Window> {
    /**
     * Graphics to draw image on
     */
    @Volatile
    private var g2d: Graphics2D? = null

    /**
     * Initialize properties
     */
    private fun init() {
        /**
         * This should fix Tab key
         * - Good Sign 2017/04/21
         */
        focusTraversalKeysEnabled = false
        defaultCloseOperation = EXIT_ON_CLOSE
        title = Engine.getEngine()!!.getWindowTitle(0.0)
    }

    fun turnOn() {
        add(content)
        content!!.focusTraversalKeysEnabled = false
        if (content is Container) {
            setContentPane(content as Container)
        } else {
            contentPane.preferredSize = content.preferredSize
        }
        isResizable = false
        /**
         * Set it to be later then setResizable to avoid extra space on right and bottom
         * - Good Sign 2017/04/09
         *
         * JFrame's size is auto-set here.
         */
        pack()
        isVisible = true

        // Gently tell the eventhandler to wake up and set itself.	  
        requestFocus()
        content.requestFocusInWindow()
    }

    /**
     * Uninitialize graphics, so it can be reset on the next repaint
     */
    fun renewGraphics() {
        val localG2d = g2d
        g2d = null
        localG2d?.dispose()
    }

    /**
     * Modified update method: no context needs to passed.
     * Will render only internal screens.
     */
    fun update() {
        if (!content!!.isDisplayable) {
            return
        }
        /**
         * Work on a local copy of the stack - global one can become null at any moment
         */
        val localG2d = getGraphics2D()
        /**
         * If the game starts too fast, it is possible to raise an exception there
         * We don't want to bother player with "something bad happened"
         * but we wouldn't just be quiet either in case of "something really bad happened"
         * - Good Sign 2017/04/09
         */
        if (localG2d == null) {
            Loggers.getLogger(DoomFrame::class.java.name)
                .log(Level.INFO, "Starting or switching fullscreen, have no Graphics2d yet, skipping paint")
        } else {
            draw(g2d!!, imageSupplier.get(), dim, this)
            if (showFPS) {
                ++frames
                val now = System.currentTimeMillis()
                val lambda = now - lastTime
                if (lambda >= 100L) {
                    title = Engine.getEngine()!!.getWindowTitle(frames * 1000.0 / lambda)
                    frames = 0
                    lastTime = now
                }
            }
        }
    }

    /**
     * Techdemo v1.3: Mac OSX fix, compatible with Windows and Linux.
     * Should probably run just once. Overhead is minimal
     * compared to actually DRAWING the stuff.
     */
    private fun getGraphics2D(): Graphics2D {
        var localG2d: Graphics2D? = g2d
        /*
        if ((localG2d = g2d) == null)
         */
        if (g2d == null) {
            // add double-checked locking
            synchronized(DoomFrame::class.java) {
                localG2d = g2d
                if (g2d == null) {
                    localG2d = content.graphics as Graphics2D
                    g2d = localG2d
                    localG2d!!.setRenderingHint(
                        RenderingHints.KEY_ALPHA_INTERPOLATION,
                        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
                    )
                    localG2d!!.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
                    localG2d!!.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
                }
            }
        }

        return localG2d!!
    }

    private val showFPS: Boolean = Engine.getCVM().bool(CommandVariable.SHOWFPS)
    private var lastTime = System.currentTimeMillis()
    private var frames = 0

    /**
     * Very generic JFrame. Along that it only initializes various properties of Doom Frame.
     */
    init {
        init()
    }

    companion object {
        private const val serialVersionUID = -4130528877723831825L
    }
}