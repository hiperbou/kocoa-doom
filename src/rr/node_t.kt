package rr


import doom.*
import doom.SourceCode.R_Main
import m.BBox
import m.ISyncLogger
import m.Settings
import m.Settings.LOS
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FixedMul
import mochadoom.Engine
import p.Resettable
import utils.C2JUtils
import utils.C2JUtils.eval

/**
 * BSP node.
 *
 * @author Maes
 */
class node_t : Resettable {
    constructor() {
        bbox = arrayOf(BBox(), BBox())
        children = IntArray(2)
    }

    constructor(
        x: Int, y: Int, dx: Int, dy: Int, bbox: Array<BBox>,
        children: IntArray
    ) {
        this.x = x
        this.y = y
        this.dx = dx
        this.dy = dy
        this.bbox = bbox
        this.children = children
    }

    /**
     * Partition line.
     */
    @SourceCode.fixed_t
    var x = 0

    @SourceCode.fixed_t
    var y = 0

    @SourceCode.fixed_t
    var dx = 0

    @SourceCode.fixed_t
    var dy = 0

    /**
     * Bounding box for each child.
     */
    // Maes: make this into two proper bboxes?
    @SourceCode.fixed_t
    var bbox: Array<BBox>

    /**
     * If NF_SUBSECTOR its a subsector.
     *
     * e6y: support for extented nodes
     */
    var children: IntArray

    /**
     * Since no context is needed, this is perfect for an instance method
     *
     * @param x fixed
     * @param y fixed
     * @return
     */
    @SourceCode.Exact
    @R_Main.C(R_Main.R_PointOnSide)
    fun PointOnSide(@SourceCode.fixed_t x: Int, @SourceCode.fixed_t y: Int): Int {
        // MAES: These are used mainly as ints, no need to use fixed_t internally.
        // fixed_t will only be used as a "pass type", but calculations will be done with ints, preferably.
        @SourceCode.fixed_t val lDx: Int
        @SourceCode.fixed_t val lDy: Int
        @SourceCode.fixed_t val left: Int
        @SourceCode.fixed_t val right: Int
        if (dx == 0) {
            if (x <= this.x) {
                return if (dy > 0) 1 else 0
            }
            return if (dy < 0) 1 else 0
        }
        if (dy == 0) {
            if (y <= this.y) {
                return if (dx < 0) 1 else 0
            }
            return if (dx > 0) 1 else 0
        }
        lDx = x - this.x
        lDy = y - this.y

        // Try to quickly decide by looking at sign bits.
        if (dy xor dx xor lDx xor lDy and -0x80000000 != 0) {
            return if (dy xor lDx and -0x80000000 != 0) {
                // (left is negative)
                1
            } else 0
        }
        left = FixedMul(dy shr FRACBITS, lDx)
        right = FixedMul(lDy, dx shr FRACBITS)
        return if (right < left) {
            // front side
            0
        } else 1
        // back side
    }

    /**
     * P_DivlineSide
     * Returns side 0 (front), 1 (back), or 2 (on).
     * Clone of divline_t's method. Same contract, but working on node_t's to avoid casts.
     * Boom-style code. Da fack.
     * [Maes]: using it leads to very different DEMO4 UD behavior.
     */
    /*
        public int DivlineSide(int x, int y) {
        int left, right;
        return (this.dx == 0) ? x == this.x ? 2 : x <= this.x ? eval(this.dy > 0) : eval(this.dy < 0) : (this.dy == 0)
            ? (OLDDEMO ? x : y) == this.y ? 2 : y <= this.y ? eval(this.dx < 0) : eval(this.dx > 0) : (this.dy == 0)
            ? y == this.y ? 2 : y <= this.y ? eval(this.dx < 0) : eval(this.dx > 0)
            : (right = ((y - this.y) >> FRACBITS) * (this.dx >> FRACBITS))
            < (left = ((x - this.x) >> FRACBITS) * (this.dy >> FRACBITS)) ? 0 : right == left ? 2 : 1;
    }
     */
    fun DivlineSide(x: Int, y: Int): Int {
        var left: Int
        var right: Int
        return if (dx == 0) if (x == this.x) 2 else if (x <= this.x) eval(dy > 0) else eval(dy < 0) else if (dy == 0) if ((if (OLDDEMO) x else y) == this.y) 2 else if (y <= this.y) eval(
            dx < 0
        ) else eval(dx > 0) else if (dy == 0) if (y == this.y) 2 else if (y <= this.y) eval(dx < 0) else eval(
            dx > 0
        ) else if ((y - this.y shr FRACBITS) * (dx shr FRACBITS).also {
                right = it
            } < (x - this.x shr FRACBITS) * (dy shr FRACBITS).also {
                left = it
            }) 0 else if (right == left) 2 else 1
    }

    fun DivlineSide(x: Int, y: Int, SL: ISyncLogger, sync: Boolean): Int {
        val result = DivlineSide(x, y)
        if (sync) {
            SL.sync("DLS %d\n", result)
        }
        return result
    }

    override fun reset() {
        dy = 0
        dx = dy
        y = dx
        x = y
        for (i in 0..1) {
            bbox[i].ClearBox()
        }
        C2JUtils.memset(children, 0, children.size)
    }

    companion object {
        /**
         * R_PointOnSide
         * Traverse BSP (sub) tree,
         * check point against partition plane.
         * Returns side 0 (front) or 1 (back).
         *
         * @param x fixed
         * @param y fixed
         * @param node
         */
        @R_Main.C(R_Main.R_PointOnSide)
        fun PointOnSide(@SourceCode.fixed_t x: Int, @SourceCode.fixed_t y: Int, node: node_t): Int {
            // MAES: These are used mainly as ints, no need to use fixed_t internally.
            // fixed_t will only be used as a "pass type", but calculations will be done with ints, preferably.
            @SourceCode.fixed_t val dx: Int
            @SourceCode.fixed_t val dy: Int
            @SourceCode.fixed_t val left: Int
            @SourceCode.fixed_t val right: Int
            if (node.dx == 0) {
                if (x <= node.x) {
                    return if (node.dy > 0) 1 else 0
                }
                return if (node.dy < 0) 1 else 0
            }
            if (node.dy == 0) {
                if (y <= node.y) {
                    return if (node.dx < 0) 1 else 0
                }
                return if (node.dx > 0) 1 else 0
            }
            dx = x - node.x
            dy = y - node.y

            // Try to quickly decide by looking at sign bits.
            if (node.dy xor node.dx xor dx xor dy and -0x80000000 != 0) {
                return if (node.dy xor dx and -0x80000000 != 0) {
                    // (left is negative)
                    1
                } else 0
            }
            left = FixedMul(node.dy shr FRACBITS, dx)
            right = FixedMul(dy, node.dx shr FRACBITS)
            return if (right < left) {
                // front side
                0
            } else 1
            // back side
        }

        private val OLDDEMO: Boolean = Engine.getConfig().equals(Settings.line_of_sight, LOS.Vanilla)
    }
}