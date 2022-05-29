package timing

class FastTicker : ITicker {
    /**
     * I_GetTime
     * returns time in 1/70th second tics
     */
    override fun GetTime(): Int {
        return fasttic++
    }

    @Volatile
    var fasttic = 0
}