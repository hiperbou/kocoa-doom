package rr.drawfuns


/** This is all the information needed to draw a particular column. Really.
 * So if we store all of this crap somewhere instead of drawing, we can do the
 * drawing when it's more convenient, and since they are non-overlapping we can
 * parallelize them. Any questions?
 *
 */
class ColVars<T, V> {
    /** when passing dc_source around, also set this  */
    var dc_source: T? = null
    var dc_source_ofs = 0
    var dc_translation: T? = null
    var viewheight = 0

    /** Used by functions that accept transparency or other special
     * remapping tables.
     *
     */
    var tranmap: T? = null
    var centery = 0
    var dc_iscale = 0
    var dc_texturemid = 0
    var dc_texheight // Boom enhancement
            = 0
    var dc_x = 0
    var dc_yh = 0
    var dc_yl = 0
    var dc_flags = 0

    /**
     * MAES: this was a typedef for unsigned bytes, called "lighttable_t". It
     * makes more sense to make it generic and parametrize it to an array of
     * primitives since it's performance-critical in the renderer.
     * Now, whether this should be made bytes or shorts or chars or even ints
     * is debatable.
     */
    var dc_colormap: V? = null

    /** Copies all BUT flags  */
    fun copyFrom(dcvars: ColVars<T, V>) {
        dc_source = dcvars.dc_source
        dc_colormap = dcvars.dc_colormap
        dc_source_ofs = dcvars.dc_source_ofs
        viewheight = dcvars.viewheight
        centery = dcvars.centery
        dc_x = dcvars.dc_x
        dc_yh = dcvars.dc_yh
        dc_yl = dcvars.dc_yl
        dc_texturemid = dcvars.dc_texturemid
        dc_iscale = dcvars.dc_iscale
        dc_texheight = dcvars.dc_texheight
    }

    /** Assigns specific flags  */
    fun copyFrom(dcvars: ColVars<T, V>, flags: Int) {
        this.copyFrom(dcvars)
        dc_flags = flags
    }
}