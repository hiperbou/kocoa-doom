package f


import v.graphics.Wipers
import v.graphics.Wipers.WipeFunc

interface Wiper {
    fun ScreenWipe(type: Wipers.WipeType, x: Int, y: Int, width: Int, height: Int, ticks: Int): Boolean
    fun EndScreen(x: Int, y: Int, width: Int, height: Int): Boolean
    fun StartScreen(x: Int, y: Int, width: Int, height: Int): Boolean
    enum class Wipe(val _initFunc: WipeFunc, val _doFunc: WipeFunc, val _exitFunc: WipeFunc): Wipers.WipeType {
        // simple gradual pixel change for 8-bit only
        // MAES: this transition isn't guaranteed to always terminate
        // see Chocolate Strife develpment. Unused in Doom anyway.
        ColorXForm(
            WipeFunc.initColorXForm,
            WipeFunc.doColorXForm,
            WipeFunc.exitColorXForm
        ),  // weird screen melt
        Melt(
            WipeFunc.initMelt,
            WipeFunc.doMelt,
            WipeFunc.exitMelt
        ),
        ScaledMelt(
            WipeFunc.initScaledMelt,
            WipeFunc.doScaledMelt,
            WipeFunc.exitMelt
        );

        override fun getDoFunc() = _doFunc
        override fun getExitFunc() = _exitFunc

        override fun getInitFunc() = _initFunc
    }
}