package i


interface IDiskDrawer : IDrawer {
    /**
     * Set a timeout (in tics) for displaying the disk icon
     *
     * @param timeout
     */
    fun setReading(reading: Int)

    /**
     * Disk displayer is currently active
     *
     * @return
     */
    fun isReading(): Boolean

    /**
     * Only call after the Wadloader is instantiated and initialized itself.
     *
     */
    fun Init()

    /**
     * Status only valid after the last tic has been drawn. Use to know when to redraw status bar.
     *
     * @return
     */
    fun justDoneReading(): Boolean
}