package rr


/** Purpose unknown, probably unused.
 * On a closer examination, it could have been part of a system to
 * "enqueue" masked draws, not much unlike the current parallel
 * rendering subsystem, but discarded because of simplifications.
 * In theory it could be brought back one day if parallel sprite
 * drawing comes back.. just a thought ;-)
 *
 *
 * @author Maes
 */
class maskdraw_t {
    var x1 = 0
    var x2 = 0
    var column = 0
    var topclip = 0
    var bottomclip = 0
}