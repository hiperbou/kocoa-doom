package p


import doom.SourceCode
import p.mobj_t
import rr.line_t

/**
 * An object that carries...interception information, I guess...with either a line
 * or an object?
 *
 * @author Velktron
 */
class intercept_t {
    /**
     * most intercepts will belong to a static pool
     */
    constructor() {}
    constructor(frac: Int, thing: mobj_t?) {
        this.frac = frac
        this.thing = thing
        isaline = false
    }

    constructor(frac: Int, line: line_t?) {
        this.frac = frac
        this.line = line
        isaline = true
    }

    /**
     * fixed_t, along trace line
     */
    @SourceCode.fixed_t
    var frac = 0
    var isaline = false

    // MAES: this was an union of a mobj_t and a line_t,
    // returned as "d".
    var thing: mobj_t? = null
    var line: line_t? = null
    fun d(): Interceptable? {
        return if (isaline) line else thing
    }
}