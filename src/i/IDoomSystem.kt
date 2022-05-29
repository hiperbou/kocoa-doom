package i


import doom.ticcmd_t

interface IDoomSystem {
    fun AllocLow(length: Int)
    fun BeginRead()
    fun EndRead()
    fun WaitVBL(count: Int)
    fun ZoneBase(size: Int): ByteArray?
    fun GetHeapSize(): Int
    fun Tactile(on: Int, off: Int, total: Int)
    fun Quit()
    fun BaseTiccmd(): ticcmd_t?
    fun Error(error: String?, vararg args: Any?)
    fun Error(error: String?)
    fun Init()

    /** Generate a blocking alert with the intention of continuing or aborting
     * a certain game-altering action. E.g. loading PWADs, or upon critical
     * level loading failures. This can be either a popup panel or console
     * message.
     *
     * @param cause Provide a clear string explaining why the alert was generated
     * @return true if we should continue, false if an alternate action should be taken.
     */
    fun GenerateAlert(title: String?, cause: String?): Boolean
}