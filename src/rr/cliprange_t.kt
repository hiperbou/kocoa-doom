package rr


class cliprange_t {
    constructor(first: Int, last: Int) {
        this.first = first
        this.last = last
    }

    constructor() {}

    var first = 0
    var last = 0
    fun copy(from: cliprange_t) {
        first = from.first
        last = from.last
    }
}