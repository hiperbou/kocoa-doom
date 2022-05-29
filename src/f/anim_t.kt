package f


import rr.patch_t
import w.animenum_t

//
//Animation.
//There is another anim_t used in p_spec.
//
class anim_t {
    constructor(
        type: animenum_t, period: Int, nanims: Int, loc: point_t,
        data1: Int, data2: Int, p: Array<patch_t?>, nexttic: Int, lastdrawn: Int,
        ctr: Int, state: Int
    ) {
        this.type = type
        this.period = period
        this.nanims = nanims
        this.loc = loc
        this.data1 = data1
        this.data2 = data2
        this.p = p
        this.nexttic = nexttic
        this.lastdrawn = lastdrawn
        this.ctr = ctr
        this.state = state
    }

    // Partial constructor, only 4 first fields.
    constructor(
        animAlways: animenum_t, period: Int, nanims: Int, loc: point_t
    ) {
        type = animAlways
        this.period = period
        this.nanims = nanims
        this.loc = loc
    }

    // Partial constructor, only 5 first fields.
    constructor(
        type: animenum_t, period: Int, nanims: Int, loc: point_t, data1: Int
    ) {
        this.type = type
        this.period = period
        this.nanims = nanims
        this.loc = loc
        this.data1 = data1
    }

    var type: animenum_t

    // period in tics between animations
    var period: Int

    // number of animation frames
    var nanims: Int

    // location of animation
    var loc: point_t

    // ALWAYS: n/a,
    // RANDOM: period deviation (<256),
    // LEVEL: level
    var data1 = 0

    // ALWAYS: n/a,
    // RANDOM: random base period,
    // LEVEL: n/a
    var data2 = 0

    // actual graphics for frames of animations
    //Maes: was pointer to array
    var p = arrayOfNulls<patch_t>(3)

    // following must be initialized to zero before use!
    // next value of bcnt (used in conjunction with period)
    var nexttic = 0

    // last drawn animation frame
    var lastdrawn = 0

    // next frame number to animate
    var ctr = 0

    // used by RANDOM and LEVEL when animating
    var state = 0
}