package awt


import doom.CommandVariable
import doom.event_t
import mochadoom.Engine
import java.awt.*
import java.util.*
import java.util.function.*
import javax.swing.JPanel

/**
 * Methods specific to Doom-System video interfacing.
 * In essence, whatever you are using as a final system-specific way to display
 * the screens, should be able to respond to these commands. In particular,
 * screen update requests must be honored, and palette/gamma request changes
 * must be intercepted before they are forwarded to the renderers (in case they
 * are system-specific, rather than renderer-specific).
 *
 * The idea is that the final screen rendering module sees/handles as less as
 * possible, and only gets a screen to render, no matter what depth it is.
 */
interface DoomWindow<E> where E : Component, E : DoomWindow<E>? {
    /**
     * Incomplete. Only checks for -geom format
     */
    fun handleGeom(): Boolean {
        var x = 0
        var y = 0

        // warning: char format, different type arg
        var xsign = ' '.code
        var ysign = ' '.code
        /*
        String displayname;
        String d;
        int n;
        int pnum;
        
        boolean oktodraw;
        long attribmask;
        
        // Try setting the locale the US, otherwise there will be problems
        // with non-US keyboards.
        if (this.getInputContext() == null || !this.getInputContext().selectInputMethod(java.util.Locale.US)) {
            System.err.println("Could not set the input context to US! Keyboard input will be glitchy!");
        } else {
            System.err.println("Input context successfully set to US.");
        }
        
        // check for command-line display name
        displayname = Game.getCVM().get(CommandVariable.DISP, String.class, 0).orElse(null);
        
        // check for command-line geometry*/
        if (Engine.getCVM().present(CommandVariable.GEOM)) {
            try {
                val eval: String =
                    Engine.getCVM().get<String>(CommandVariable.GEOM, String::class.java, 0).get()
                        .trim { it <= ' ' }
                // warning: char format, different type arg 3,5
                //n = sscanf(myargv[pnum+1], "%c%d%c%d", &xsign, &x, &ysign, &y);
                // OK, so we have to read a string that may contain
                // ' '/'+'/'-' and a number. Twice.
                val tk = StringTokenizer(eval, "-+ ")
                // Signs. Consider positive.
                xsign = 1
                ysign = 1
                for (i in 0 until eval.length) {
                    if (eval[i] == '-') {
                        // First '-' on trimmed string: negagive
                        if (i == 0) {
                            xsign = -1
                        } else {
                            ysign = -1
                        }
                    }
                }

                //this should parse two numbers.
                if (tk.countTokens() == 2) {
                    x = xsign * tk.nextToken().toInt()
                    y = ysign * tk.nextToken().toInt()
                }
            } catch (e: NumberFormatException) {
                return false
            }
        }
        return true
    }

    class JPanelWindow : JPanel(), DoomWindow<JPanelWindow> {
        init {
            init()
        }

        private fun init() {
            isDoubleBuffered = true
            isOpaque = true
            background = Color.BLACK
        }

        override fun isOptimizedDrawingEnabled(): Boolean {
            return false
        }

        companion object {
            private const val serialVersionUID = 4031722796186278753L
        }
    }

    class CanvasWindow(config: GraphicsConfiguration) : Canvas(config), DoomWindow<CanvasWindow> {
        companion object {
            private const val serialVersionUID = 1180777361390303859L
        }
    }

    companion object {
        /**
         * Get current graphics device
         */
        fun getDefaultDevice(): GraphicsDevice {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        }

        /**
         * Get an instance of JFrame to draw anything. This will try to create compatible Canvas and
         * will bing all AWT listeners
         */

        fun createCanvasWindowController(
            imageSource: Supplier<Image>,
            doomEventConsume: Consumer<in event_t>,
            width: Int, height: Int
        ) = DoomWindowController(
                EventHandler::class.java,
                getDefaultDevice(),
                imageSource,
                doomEventConsume,
                CanvasWindow(getDefaultDevice().getDefaultConfiguration()),
                width, height
            )


        /**
         * Get an instance of JFrame to draw anything. This will try to create compatible Canvas and
         * will bing all AWT listeners
         */
        fun createJPanelWindowController(
            imageSource: Supplier<Image>,
            doomEventConsume: Consumer<in event_t>,
            width: Int, height: Int
        ) = DoomWindowController(
                EventHandler::class.java,
                getDefaultDevice(),
                imageSource,
                doomEventConsume,
                JPanelWindow(), width, height
            )

    }
}