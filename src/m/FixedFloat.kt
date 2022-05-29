package m

/** Some utilities for switching between floating and signed 16.16 fixed-point at will.
 * They use direct bit manipulation with little -if any- looping.
 *
 * The methods can probably be generalized but not a priority for now.
 * They do not handle Infinities, NaNs and unnormalized numbers.
 *
 * @author Maes
 */
object FixedFloat {
    // Various bit masks for IEEE-754 floating point 
    const val MANTISSA_32 = 0x007FFFFF
    const val EXP_32 = 0x7F800000
    const val IMPLICIT_32 = 0x00800000
    const val SIGN_32 = -0x80000000
    const val NONSIGN_32 = 0x7FFFFFFF
    //public static final long SIGN_64=0x8000000000000000L;
    //val SIGN_64: Long = (-0x8000000000000000L).toLong()
    //val SIGN_64 = 0x8000000000000000L
    //val SIGN_64 = 0x8000000000000000U
    val SIGN_64 =  java.lang.Long.parseUnsignedLong("8000000000000000", 16)
    const val EXP_64 = 0x7FF0000000000000L
    const val IMPLICIT_64 = 0x0010000000000000L
    const val MANTISSA_64 = 0x000fffffffffffffL
    fun toFloat(fixed: Int): Float {
        var fixed = fixed
        if (fixed == 0) return 0.0.toFloat()
        // Remember sign.
        val sign = fixed and SIGN_32
        if (fixed < 0) fixed = -fixed
        val exp = findShift(fixed)
        // First shift to left to "cancel" bits "above" the first.
        val mantissa = fixed shl exp + 2 ushr 9
        val result = sign or (14 - exp + 127 shl 23) or mantissa
        /*if (fixed<0) System.out.println(Integer.toBinaryString(fixed) +"\n"+
                                        Integer.toBinaryString(-fixed) +"\n"+
                                        Integer.toBinaryString(result));*/return java.lang.Float.intBitsToFloat(result)
    }

    private fun findShift(fixed: Int): Int {
        // only non-sign bits.
        var fixed = fixed
        fixed = fixed and NONSIGN_32
        // We assume that the MSb after the sign is set.
        var shift = 30
        while (shift >= 0 && fixed ushr shift == 0) // It's not, apparently
            shift--

        // Positions 0-15 are fractional, anything above 15 is integer.
        // Return two's complement shift.
        return 30 - shift
    }

    fun toDouble(fixed: Int): Double {


        // Remember sign.
        var fixed = fixed
        var fx = fixed.toLong()
        fx = fx shl 32
        val sign = fx and SIGN_64
        if (fixed < 0) {
            fixed = -fixed
            fx = -fx
        }
        val exp = findShift(fixed).toLong()
        // First shift to left to "swallow" sign and implicit 1.
        val bits = fx shl (exp + 2).toInt() ushr 12
        val result = sign or (14 - exp + 1023 shl 52) or bits
        return java.lang.Double.longBitsToDouble(result)
    }

    fun toFixed(fl: Float): Int {
        // Get the raw bits.
        val flbits = java.lang.Float.floatToRawIntBits(fl)
        // Remember sign.
        val sign = flbits and SIGN_32
        // Join together: the implcit 1 and the mantissa bits.
        // We now have the "denormalized" value. 
        val denorm = IMPLICIT_32 or (flbits and MANTISSA_32)
        // Get exponent...acceptable values are (-15 ~ 15), else wrap around (use only sign and lowest 4 bits).
        val exp = (flbits and EXP_32 shr 23) - 127 and -0x7ffffff1
        /* Remember, leftmost "1" will be at position 23.
         * So for an exponent of 0, we must shift to position 16.
         * For positive exponents in general, we must shift -7 + exp.
         * and for one of 15, to position 30, plus the sign.
         * While there is space for all bits, we can't keep them all, 
         * as some (well, many)numbers can't be represented in fixed point.
         * 
         */
        val result: Int
        result = if (exp - 7 >= 0) sign or (denorm shl exp) - 7 else sign or (denorm ushr 7) - exp
        return result
    }

    fun toFixed(fl: Double): Int {

        // Get the raw bits.
        val flbits = java.lang.Double.doubleToRawLongBits(fl)
        // Remember sign.
        val sign = (flbits and SIGN_64 shr 32).toInt()
        // Join together: the implcit 1 and the mantissa bits.
        // We now have the "denormalized" value. 
        val denorm = IMPLICIT_64 or (flbits and MANTISSA_64)
        //System.out.println("Denorm"+Integer.toBinaryString(denorm));
        // Get exponent...acceptable values are (-15 ~ 15), else wrap around (use only sign and lowest 4 bits).
        val exp = ((flbits and EXP_64 shr 52) - 1023).toInt() and -0x7ffffff1
        /* Remember, leftmost "1" will be at position 53.
         * So for an exponent of 0, we must shift to position 16.
         * For positive exponents in general, we must shift -37 + exp.
         * and for one of 15, to position 30, plus the sign.
         * While there is space for all bits, we can't keep them all, 
         * as some (well, many)numbers can't be represented in fixed point.
         * 
         */
        val result: Int
        result =
            if (exp - 36 >= 0) (sign.toLong() or (denorm shl exp) - 36).toInt() else (sign.toLong() or (denorm ushr 36) - exp).toInt()
        //int result=sign|(IMPLICIT_32|(mantissa<<(exp-127)))<<8;
        return result
    }
}