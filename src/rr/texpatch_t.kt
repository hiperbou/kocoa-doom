package rr

/**
 * A single patch from a texture definition,
 * basically a rectangular area within
 * the texture rectangle.
 * @author admin
 */
class texpatch_t {
    // Block origin (allways UL),
    // which has allready accounted
    // for the internal origin of the patch.
    var originx = 0
    var originy = 0
    var patch = 0
    fun copyFromMapPatch(mpp: mappatch_t) {
        originx = mpp.originx.toInt()
        originy = mpp.originy.toInt()
        patch = mpp.patch.toInt()
    }
}