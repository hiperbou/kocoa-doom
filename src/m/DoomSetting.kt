package m


import utils.C2JUtils

/** A "Doom setting". Based on current experience, it could
 * represent an integer value, a string, or a boolean value.
 *
 * Therefore, every setting can be interpreted as any of the above,
 * based on some rules. Strings that can be interpreted as parseable
 * numbers are obvious, and numbers can also be interpreted as strings.
 * Strings that can't be interpreted as numbers will return "0" as a default
 * value.
 *
 * A numerical value of 1 means "true", any other value is "false".
 * A string representing the (case insensitive) value "true" will
 * be interpreted as a true boolean, false otherwise.
 *
 * @author velktron
 */
class DoomSetting(val name: String, value: String, persist: Boolean) : Comparable<DoomSetting> {
    var typeFlag: Int

    // Every setting can be readily interpreted as any of these
    var integer = 0
    var long: Long = 0
    private var char_val = 0.toChar()
    var double = 0.0
    var boolean = false
    var string: String? = null

    /** Should be saved to file  */
    val persist: Boolean
    val char: Char
        get() = integer.toChar()

    /** All the gory disambiguation work should go here.
     *
     * @param value
     */
    fun updateValue(value: String) {
        var value = value
        var quoted = false
        if (value.length > 2) if (C2JUtils.isQuoted(value, '"').also { quoted = it }) value =
            C2JUtils.unquote(value, '"')!! else if (C2JUtils.isQuoted(value, '\'').also { quoted = it }) value =
            C2JUtils.unquote(value, '\'')!!

        // String value always available
        string = value

        // If quoted and sensibly ranged, it gets priority as a "character"        
        if (quoted && value.length == 1 && value[0].code >= 0 && value[0].code < 255) {
            char_val = value[0].lowercaseChar()
            integer = char_val.code
            long = char_val.code.toLong()
            double = char_val.code.toDouble()
            typeFlag = typeFlag or DoomSetting.CHAR
            return
        }

        // Not a character, try all other stuff
        try {
            integer = value.toInt()
            typeFlag = typeFlag or DoomSetting.INTEGER
        } catch (e: NumberFormatException) {
            // No nookie
            integer = -1
        }
        try {
            long = value.toLong()
        } catch (e: NumberFormatException) {
            try {
                // Try decoding it as hex, octal, whatever.
                long = java.lang.Long.decode(value)
                typeFlag = typeFlag or DoomSetting.INTEGER
            } catch (h: NumberFormatException) {
                // If even THAT fails, then no nookie.
                long = -1
            }
        }
        try {
            double = value.toDouble()
            typeFlag = typeFlag or DoomSetting.DOUBLE
        } catch (e: NumberFormatException) {
            // No nookie
            double = Double.NaN
        }

        // Use long value to "trump" smaller ones
        integer = long.toInt()
        char_val = integer.toChar()

        // Boolean has a few more options;
        // Only mark something explicitly as boolean if the string reads 
        // actually "true" or "false". Numbers such as 0 and 1 might still get
        // interpreted as booleans, but that shouldn't trump the entire number,
        // otherwise everything and the cat is boolean
        boolean = integer == 1
        if (java.lang.Boolean.parseBoolean(value) || value.compareTo("false", ignoreCase = true) == 0) {
            boolean = integer == 1 || java.lang.Boolean.parseBoolean(value)
            typeFlag = typeFlag or DoomSetting.BOOLEAN
        }
    }// If even THAT fails, then no nookie.

    // Everything OK, I presume...
// Try decoding it as hex, octal, whatever.
    /** Answer definitively if a setting cannot ABSOLUTELY be
     * parsed into a number using simple Integer rules.
     * This excludes some special names like "+Inf" and "NaN".
     *
     * @return
     */
    val isIntegerNumeric: Boolean
        get() {
            try {
                long = string!!.toLong()
            } catch (e: NumberFormatException) {
                try {
                    // Try decoding it as hex, octal, whatever.
                    java.lang.Long.decode(string)
                } catch (h: NumberFormatException) {
                    // If even THAT fails, then no nookie.
                    return false
                }
            }

            // Everything OK, I presume...
            return true
        }

    /** Settings are "comparable" to each other by name, so we can save
     * nicely sorted setting files ;-)
     *
     * @param o
     * @return
     */
    override fun compareTo(o: DoomSetting): Int {
        return name.compareTo(o.name, ignoreCase = true)
    }

    override fun toString(): String {
        return string!!
    }

    init {
        typeFlag = DoomSetting.STRING
        updateValue(value)
        this.persist = persist
    }

    companion object {
        const val BOOLEAN = 1
        const val CHAR = 2
        const val DOUBLE = 4
        const val INTEGER = 8
        const val STRING = 16

        /** A special setting that returns false, 0 and an empty string, if required.
         * Simplifies handling of nulls A LOT. So code that relies on specific settings
         * should be organized to work only on clear positivies (e.g. use a "fullscreen" setting
         * that must exist and be equal to 1 or true, instead of assuming that a zero/false
         * value enables it.  */
        var NULL_SETTING = DoomSetting("NULL", "", false).apply {
            // It's EVERYTHING
            typeFlag = 0x1F
            string = ""
            char_val = 0.toChar()
            double = 0.0
            boolean = false
            integer = 0
            long = 0
        }

    }
}