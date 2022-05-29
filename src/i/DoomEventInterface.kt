package i


/** Interface for Doom-to-System event handling methods
 *
 * @author Velktron
 */
interface DoomEventInterface {
    /** The implementation is windowing subsystem-specific
     * e.g. DOS, XServer, AWT or Swing or whatever.
     *
     */
    fun GetEvent()
    fun mouseMoving(): Boolean
    fun setMouseMoving(mousMoving: Boolean)
}