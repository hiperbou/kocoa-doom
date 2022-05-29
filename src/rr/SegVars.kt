package rr

import data.Limits
import utils.C2JUtils

class SegVars {
    // /// FROM BSP /////////
    var MAXDRAWSEGS = Limits.MAXDRAWSEGS

    /** pointer to drawsegs  */
    var ds_p = 0
    lateinit var drawsegs: Array<drawseg_t>
    lateinit var maskedtexturecol: ShortArray
    var pmaskedtexturecol = 0

    /**
     * R_ClearDrawSegs
     *
     * The drawseg list is reset by pointing back at 0.
     *
     */
    fun ClearDrawSegs() {
        ds_p = 0
    }

    fun ResizeDrawsegs() {
        drawsegs = C2JUtils.resize(drawsegs[0], drawsegs, drawsegs.size * 2)
    }
}