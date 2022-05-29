package rr.drawfuns


class ColFuncs<T, V> {
    var main: DoomColumnFunction<T, V>? = null
    var base: DoomColumnFunction<T, V>? = null
    var masked: DoomColumnFunction<T, V>? = null
    var fuzz: DoomColumnFunction<T, V>? = null
    var trans: DoomColumnFunction<T, V>? = null
    var glass: DoomColumnFunction<T, V>? = null
    var player: DoomColumnFunction<T, V>? = null
    var sky: DoomColumnFunction<T, V>? = null
}