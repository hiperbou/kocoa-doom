package w


enum class statenum_t(private val value: Int) {
    NoState(-1), StatCount(0), ShowNextLoc(1);

    fun getValue(): Int {
        return value
    }
}