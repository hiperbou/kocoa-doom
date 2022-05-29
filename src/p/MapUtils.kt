package p


import m.fixed_t
import utils.C2JUtils

object MapUtils {
    /**
     * AproxDistance
     * Gives an estimation of distance (not exact)
     *
     * @param dx fixed_t
     * @param dy fixed_t
     * @return fixed_t
     */
    //
    fun AproxDistance(
        dx: Int,
        dy: Int
    ): Int {
        var dx = dx
        var dy = dy
        dx = Math.abs(dx)
        dy = Math.abs(dy)
        return if (dx < dy) dx + dy - (dx shr 1) else dx + dy - (dy shr 1)
    }

    /**
     * P_InterceptVector
     * Returns the fractional intercept point
     * along the first divline.
     * This is only called by the addthings
     * and addlines traversers.
     *
     * @return int to be treated as fixed_t
     */
    fun InterceptVector(
        v2: divline_t,
        v1: divline_t
    ): Int {
        val frac: Int
        val num: Int
        val den: Int // fixed_t
        den = fixed_t.FixedMul(v1.dy shr 8, v2.dx) - fixed_t.FixedMul(v1.dx shr 8, v2.dy)
        if (den == 0) return 0
        //  I_Error ("P_InterceptVector: parallel");
        num = (fixed_t.FixedMul(v1.x - v2.x shr 8, v1.dy) + fixed_t.FixedMul(v2.y - v1.y shr 8, v1.dx))
        frac = fixed_t.FixedDiv(num, den)
        return frac
        /*
   #else   // UNUSED, float debug.
   float   frac;
   float   num;
   float   den;
   float   v1x;
   float   v1y;
   float   v1dx;
   float   v1dy;
   float   v2x;
   float   v2y;
   float   v2dx;
   float   v2dy;

   v1x = (float)v1.x/FRACUNIT;
   v1y = (float)v1.y/FRACUNIT;
   v1dx = (float)v1.dx/FRACUNIT;
   v1dy = (float)v1.dy/FRACUNIT;
   v2x = (float)v2.x/FRACUNIT;
   v2y = (float)v2.y/FRACUNIT;
   v2dx = (float)v2.dx/FRACUNIT;
   v2dy = (float)v2.dy/FRACUNIT;
   
   den = v1dy*v2dx - v1dx*v2dy;

   if (den == 0)
   return 0;   // parallel
   
   num = (v1x - v2x)*v1dy + (v2y - v1y)*v1dx;
   frac = num / den;

   return frac*FRACUNIT;
  #endif */
    }

    /* cph - this is killough's 4/19/98 version of P_InterceptVector and
   *  P_InterceptVector2 (which were interchangeable). We still use this
   *  in compatibility mode. */
    private fun P_InterceptVector2(v2: divline_t, v1: divline_t): Int {
        var den: Int
        return if (C2JUtils.eval(
                fixed_t.FixedMul(
                    v1.dy shr 8,
                    v2.dx
                ) - fixed_t.FixedMul(v1.dx shr 8, v2.dy).also { den = it })
        ) fixed_t.FixedDiv(
            fixed_t.FixedMul(
                v1.x - v2.x shr 8, v1.dy
            ) +
                    fixed_t.FixedMul(v2.y - v1.y shr 8, v1.dx), den
        ) else 0
    }

    /** Used by CrossSubSector
     *
     * @param v2
     * @param v1
     * @return
     */
    fun P_InterceptVector(v2: divline_t, v1: divline_t): Int {
        return if (false /*compatibility_level < prboom_4_compatibility*/) MapUtils.P_InterceptVector2(v2, v1) else {
            /* cph - This was introduced at prboom_4_compatibility - no precision/overflow problems */
            var den = v1.dy.toLong() * v2.dx - v1.dx.toLong() * v2.dy
            den = den shr 16
            if (!C2JUtils.eval(den)) 0 else (((v1.x - v2.x).toLong() * v1.dy - (v1.y - v2.y).toLong() * v1.dx) / den).toInt()
        }
    }

    /**
     * P_InterceptVector2 Returns the fractional intercept point along the
     * first divline. This is only called by the addthings and addlines
     * traversers.
     *
     * @param v2
     * @param v1
     * @returnP_InterceptVector2
     */
    fun InterceptVector2(v2: divline_t, v1: divline_t): Int {
        val frac: Int // fixed_t
        val num: Int // fixed_t
        val den: Int // fixed_t
        den = fixed_t.FixedMul(v1.dy shr 8, v2.dx) - fixed_t.FixedMul(v1.dx shr 8, v2.dy)
        if (den == 0) return 0
        // I_Error ("P_InterceptVector: parallel");
        num = (fixed_t.FixedMul(v1.x - v2.x shr 8, v1.dy) + fixed_t.FixedMul(v2.y - v1.y shr 8, v1.dx))
        frac = fixed_t.FixedDiv(num, den)
        return frac
    }
}