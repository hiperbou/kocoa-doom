package p


import rr.line_t
import s.degenmobj_t

class button_t : Resettable {
    var line: line_t? = null
    var where: bwhere_e
    var btexture = 0
    var btimer = 0
    var soundorg: degenmobj_t? = null

    init {
        where = bwhere_e.top
    }

    override fun reset() {
        line = null
        where = bwhere_e.top
        btexture = 0
        btimer = 0
        soundorg = null
    }
}