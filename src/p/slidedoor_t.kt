package p


import rr.SectorAction
import rr.line_t
import rr.sector_t

class slidedoor_t : SectorAction() {
    var type: sdt_e
    var line: line_t? = null
    var frame = 0
    var whichDoorIndex = 0
    var timer = 0
    var frontsector: sector_t? = null
    var backsector: sector_t? = null
    var status: sd_e

    init {
        type = sdt_e.sdt_closeOnly
        status = sd_e.sd_closing
        thinkerFunction = ActiveStates.T_SlidingDoor
    }
}