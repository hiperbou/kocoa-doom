package com.hiperbou.lang


//fun Boolean.toInt() = if (this) 1 else 0
//fun Int.toBool() = if (this==0) false else true

inline fun <T>Array<T>.apply(block: (T)->Unit, max:Int = this.size) {
    var i = 0
    while (i< max) {
        block(this[i])
        i++
    }
}

inline fun <T>Array<T>.transform(block: (T)->T, max:Int = this.size) {
    var i = 0
    while (i< max) {
        this[i] = block(this[i])
        i++
    }
}

inline fun BooleanArray.transform(block: (Boolean)->Boolean, max:Int = this.size) {
    var i = 0
    while (i< max) {
        this[i] = block(this[i])
        i++
    }
}
inline fun BooleanArray.transformIndexed(block: (Int)->Boolean, max:Int = this.size) {
    var i = 0
    while (i< max) {
        this[i] = block(i)
        i++
    }
}

inline fun BooleanArray.transformIndexed(block: (Boolean, Int)->Boolean, max:Int = this.size) {
    var i = 0
    while (i< max) {
        this[i] = block(this[i], i)
        i++
    }
}

inline fun IntArray.transform(block: (Int)->Int, max:Int = this.size) {
    var i = 0
    while (i< max) {
        this[i] = block(this[i])
        i++
    }
}
inline fun IntArray.multiply(times:Int, max:Int = this.size) {
    var i = 0
    while (i< max) {
        this[i] *= times
        i++
    }
}

inline operator fun Int.times(block:(Int)->Unit) {
    var i = 0
    while (i < this) {
        block(i)
        i++
    }
}

inline operator fun Short.times(block:(Int)->Unit) {
    var i = 0
    while (i < this) {
        block(i)
        i++
    }
}

inline fun repeatFor(times:Int, block:(Int)->Unit) {
    var i = 0
    while (i < times) {
        block(i)
        i++
    }
}