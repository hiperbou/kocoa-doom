package timing


import data.Defines

class NanoTicker : ITicker {
    /**
     * I_GetTime
     * returns time in 1/70th second tics
     */
    override fun GetTime(): Int {
        val tp: Long
        //struct timezone   tzp;
        val newtics: Int

        // Attention: System.nanoTime() might not be consistent across multicore CPUs.
        // To avoid the core getting back to the past,
        tp = System.nanoTime()
        if (basetime == 0L) {
            basetime = tp
        }
        newtics = ((tp - basetime) * Defines.TICRATE / 1000000000).toInt() // + tp.tv_usec*TICRATE/1000000;
        if (newtics < oldtics) {
            System.err.printf("Timer discrepancies detected : %d", ++discrepancies)
            return oldtics
        }
        return newtics.also { oldtics = it }
    }

    @Volatile
    var basetime: Long = 0

    @Volatile
    var oldtics = 0

    @Volatile
    protected var discrepancies = 0
}