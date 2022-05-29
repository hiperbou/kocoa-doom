package m


class default_t(
    var name: String,
    /** this is supposed to be a pointer  */
    var location: IntArray, var defaultvalue: Int
) {
    var scantranslate // PC scan code hack
            = 0
    var untranslated // lousy hack
            = 0
}