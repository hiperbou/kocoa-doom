package automap



import doom.SourceCode.AM_Map
import doom.event_t

interface IAutoMap<T, V> {
    // Called by main loop.
    @AM_Map.C(AM_Map.AM_Responder)
    fun Responder(ev: event_t): Boolean

    // Called by main loop.
    fun Ticker()

    // Called by main loop,
    // called instead of view drawer if automap active.
    fun Drawer()

    // Added to be informed of gamma changes - Good Sign 2017/04/05
    fun Repalette()

    // Called to force the automap to quit
    // if the level is completed while it is up.
    @AM_Map.C(AM_Map.AM_Stop)
    fun Stop()
    fun Start()

    companion object {
        // Used by ST StatusBar stuff.
        const val AM_MSGHEADER = ('a'.code shl 24) + ('m'.code shl 16)
        val AM_MSGENTERED: Int = IAutoMap.AM_MSGHEADER or ('e'.code shl 8)
        val AM_MSGEXITED: Int = IAutoMap.AM_MSGHEADER or ('x'.code shl 8)

        // Color ranges for automap. Actual colors are bit-depth dependent.
        const val REDRANGE = 16
        const val BLUERANGE = 8
        const val GREENRANGE = 16
        const val GRAYSRANGE = 16
        const val BROWNRANGE = 16
        const val YELLOWRANGE = 1
        const val YOURRANGE = 0
        val WALLRANGE: Int = IAutoMap.REDRANGE
        val TSWALLRANGE: Int = IAutoMap.GRAYSRANGE
        val FDWALLRANGE: Int = IAutoMap.BROWNRANGE
        val CDWALLRANGE: Int = IAutoMap.YELLOWRANGE
        val THINGRANGE: Int = IAutoMap.GREENRANGE
        val SECRETWALLRANGE: Int = IAutoMap.WALLRANGE
        const val GRIDRANGE = 0
    }
}