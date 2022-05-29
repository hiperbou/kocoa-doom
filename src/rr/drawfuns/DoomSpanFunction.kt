package rr.drawfuns


import i.IDoomSystem

abstract class DoomSpanFunction<T, V>(
    protected val SCREENWIDTH: Int,
    protected val SCREENHEIGHT: Int,
    protected val ylookup: IntArray,
    protected val columnofs: IntArray,
    protected var dsvars: SpanVars<T, V>,
    protected val screen: V,
    protected val I: IDoomSystem
) : SpanFunction<T, V> {
    protected val RANGECHECK = false
    protected fun doRangeCheck() {
        if (dsvars.ds_x2 < dsvars.ds_x1 || dsvars.ds_x1 < 0 || dsvars.ds_x2 >= SCREENWIDTH || dsvars.ds_y > SCREENHEIGHT) {
            I.Error("R_DrawSpan: %d to %d at %d", dsvars.ds_x1, dsvars.ds_x2, dsvars.ds_y)
        }
    }

    override fun invoke(dsvars: SpanVars<T, V>) {
        this.dsvars = dsvars
        invoke()
    }
}