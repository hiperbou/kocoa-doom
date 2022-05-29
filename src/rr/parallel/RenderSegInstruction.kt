package rr.parallel


/** This is all the information needed to draw a particular SEG.
 * It's quite a lot, actually, but much better than in testing
 * versions.
 *
 */
class RenderSegInstruction<V> {
    var rw_x = 0
    var rw_stopx = 0
    var toptexture = 0
    var midtexture = 0
    var bottomtexture = 0
    var pixhigh = 0
    var pixlow = 0
    var pixhighstep = 0
    var pixlowstep = 0
    var topfrac = 0
    var topstep = 0
    var bottomfrac = 0
    var bottomstep = 0
    var segtextured = false
    var markfloor = false
    var markceiling = false
    var rw_centerangle // angle_t
            : Long = 0

    /** fixed_t  */
    var rw_offset = 0
    var rw_distance = 0
    var rw_scale = 0
    var rw_scalestep = 0
    var rw_midtexturemid = 0
    var rw_toptexturemid = 0
    var rw_bottomtexturemid = 0
    var viewheight = 0
    lateinit var walllights: Array<V>
    var centery = 0
}