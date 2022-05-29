package timing


import data.Defines

class MilliTicker : ITicker {
    /**
     * I_GetTime
     * returns time in 1/70th second tics
     */
    override fun GetTime(): Int {
        val tp: Long
        //struct timezone   tzp;
        val newtics: Int
        tp = System.currentTimeMillis()
        if (basetime == 0L) {
            basetime = tp
        }
        newtics = ((tp - basetime) * Defines.TICRATE / 1000).toInt()
        return newtics
    }

    @Volatile
    var basetime: Long = 0

    @Volatile
    var oldtics = 0

    @Volatile
    protected var discrepancies = 0
}