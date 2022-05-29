package rr

import m.*
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FixedMul
import p.Resettable
import s.*

/**
 * The LineSeg. Must be built from on-disk mapsegs_t, which are much simpler.
 *
 * @author Maes
 */
class seg_t : Resettable {
    /** To be used as references  */
    var v1: vertex_t? = null
    var v2: vertex_t? = null

    /** Local caching. Spares us using one extra reference level  */
    var v1x = 0
    var v1y = 0
    var v2x = 0
    var v2y = 0

    /** (fixed_t)  */
    var offset = 0

    /** (angle_t)  */
    var angle: Long = 0

    // MAES: all were single pointers.
    var sidedef: side_t? = null
    var linedef: line_t? = null

    /**
     * Sector references. Could be retrieved from linedef, too. backsector is
     * NULL for one sided lines
     */
    var frontsector: sector_t? = null
    var backsector: sector_t? = null

    // Boom stuff
    var miniseg = false
    var length = 0f

    /** proff 11/05/2000: needed for OpenGL  */
    var iSegID = 0
    fun assignVertexValues() {
        v1x = v1!!.x
        v1y = v1!!.y
        v2x = v2!!.x
        v2y = v2!!.y
    }

    /**
     * R_PointOnSegSide
     *
     * @param x
     * @param y
     * @param line
     * @return
     */
    fun PointOnSegSide(x: Int, y: Int): Int {
        val lx: Int
        val ly: Int
        val ldx: Int
        val ldy: Int
        val dx: Int
        val dy: Int
        val left: Int
        val right: Int
        lx = v1x
        ly = v1y
        ldx = v2x - lx
        ldy = v2y - ly
        if (ldx == 0) {
            if (x <= lx) return if (ldy > 0) 1 else 0
            return if (ldy < 0) 1 else 0
        }
        if (ldy == 0) {
            if (y <= ly) return if (ldx < 0) 1 else 0
            return if (ldx > 0) 1 else 0
        }
        dx = x - lx
        dy = y - ly

        // Try to quickly decide by looking at sign bits.
        if (ldy xor ldx xor dx xor dy and -0x80000000 != 0) {
            return if (ldy xor dx and -0x80000000 != 0) {
                // (left is negative)
                1
            } else 0
        }
        left = FixedMul(ldy shr FRACBITS, dx)
        right = FixedMul(dy, ldx shr FRACBITS)
        return if (right < left) {
            // front side
            0
        } else 1
        // back side
    }

    override fun toString(): String {
        return String.format(
            "Seg %d\n\tFrontsector: %s\n\tBacksector: %s\n\tVertexes: %x %x %x %x",
            iSegID, frontsector, backsector, v1x, v1y, v2x, v2y
        )
    }

    override fun reset() {
        v2 = null
        v1 = v2
        v2y = 0
        v2x = v2y
        v1y = v2x
        v1x = v1y
        angle = 0
        backsector = null
        frontsector = backsector
        iSegID = 0
        linedef = null
        miniseg = false
        offset = 0
        length = 0f
    }

    companion object {
        /**
         * R_PointOnSegSide
         *
         * @param x
         * @param y
         * @param line
         * @return
         */
        fun PointOnSegSide(x: Int, y: Int, line: seg_t): Int {
            val lx: Int
            val ly: Int
            val ldx: Int
            val ldy: Int
            val dx: Int
            val dy: Int
            val left: Int
            val right: Int
            lx = line.v1x
            ly = line.v1y
            ldx = line.v2x - lx
            ldy = line.v2y - ly
            if (ldx == 0) {
                if (x <= lx) return if (ldy > 0) 1 else 0
                return if (ldy < 0) 1 else 0
            }
            if (ldy == 0) {
                if (y <= ly) return if (ldx < 0) 1 else 0
                return if (ldx > 0) 1 else 0
            }
            dx = x - lx
            dy = y - ly

            // Try to quickly decide by looking at sign bits.
            if (ldy xor ldx xor dx xor dy and -0x80000000 != 0) {
                return if (ldy xor dx and -0x80000000 != 0) {
                    // (left is negative)
                    1
                } else 0
            }
            left = FixedMul(ldy shr FRACBITS, dx)
            right = FixedMul(dy, ldx shr FRACBITS)
            return if (right < left) {
                // front side
                0
            } else 1
            // back side
        }
    }
}