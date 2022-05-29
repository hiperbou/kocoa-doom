package automap


import m.fixed_t

class mpoint_t {
    constructor(x: fixed_t, y: fixed_t) {
        this.x = x.`val`
        this.y = y.`val`
    }

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    constructor(x: Double, y: Double) {
        this.x = x.toInt()
        this.y = y.toInt()
    }

    constructor() {
        x = 0
        y = 0
    }

    /** fixed_t  */
    var x: Int
    var y: Int
    override fun toString(): String {
        return Integer.toHexString(x) + " , " + Integer.toHexString(y)
    }
}