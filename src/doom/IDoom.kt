package doom


import java.io.IOException

/** Stuff that the "main" is supposed to do. DoomMain implements those.
 *
 * @author Maes
 */
interface IDoom {
    /** Called by IO functions when input is detected.  */
    fun PostEvent(ev: event_t)
    fun PageTicker()
    fun PageDrawer()
    fun AdvanceDemo()
    fun StartTitle()

    @Throws(IOException::class)
    fun QuitNetGame()
}