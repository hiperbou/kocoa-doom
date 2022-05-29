package rr.drawfuns


class SpanVars<T, V> {
    var ds_xfrac = 0
    var ds_yfrac = 0
    var ds_xstep = 0
    var ds_source: T? = null

    /** DrawSpan colormap.  */
    var ds_colormap: V? = null
    var ds_y = 0
    var ds_x2 = 0
    var ds_x1 = 0
    var ds_ystep = 0
    var spanfunc: DoomSpanFunction<T, V>? = null
}