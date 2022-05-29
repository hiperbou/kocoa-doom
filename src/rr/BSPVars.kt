package rr


/** Stuff used to pass information between the BSP and the SegDrawer  */
open class BSPVars {
    /** The sectors of the line currently being considered  */
    var frontsector: sector_t? = null
    var backsector: sector_t? = null
    var curline: seg_t? = null
    var sidedef: side_t? = null
    var linedef: line_t? = null
}