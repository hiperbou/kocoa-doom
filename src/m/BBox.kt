package m


import data.Limits

/** A fucked-up bounding box class.
 * Fucked-up  because it's supposed to wrap fixed_t's.... no fucking way I'm doing
 * this with fixed_t objects.
 *
 * @author admin
 */
class BBox {
    /** (fixed_t)  */
    var bbox: IntArray

    /** Points of the bbox as an object  */
    init {
        bbox = IntArray(4)
    }

    // Instance method
    fun ClearBox() {
        bbox[BBox.BOXRIGHT] = Limits.MININT
        bbox[BBox.BOXTOP] = Limits.MININT
        bbox[BBox.BOXLEFT] = Limits.MAXINT
        bbox[BBox.BOXBOTTOM] = Limits.MAXINT
    }

    fun AddToBox(x: fixed_t, y: fixed_t) {
        if (x.compareTo(bbox[BBox.BOXLEFT]) < 0) bbox[BBox.BOXLEFT] = x.`val` else if (x.compareTo(
                bbox[BBox.BOXRIGHT]
            ) > 0
        ) bbox[BBox.BOXRIGHT] = x.`val`
        if (y.compareTo(bbox[BBox.BOXBOTTOM]) < 0) bbox[BBox.BOXBOTTOM] =
            y.`val` else if (y.compareTo(bbox[BBox.BOXTOP]) > 0) bbox[BBox.BOXTOP] = y.`val`
    }

    /**
     * MAES: Keeping with C's type (in)consistency, we also allow to input ints
     * -_-
     *
     * @param x
     * @param y
     */
    fun AddToBox(x: Int, y: Int) {
        if (x < bbox[BBox.BOXLEFT]) bbox[BBox.BOXLEFT] = x
        if (x > bbox[BBox.BOXRIGHT]) bbox[BBox.BOXRIGHT] = x
        if (y < bbox[BBox.BOXBOTTOM]) bbox[BBox.BOXBOTTOM] = y
        if (y > bbox[BBox.BOXTOP]) bbox[BBox.BOXTOP] = y
    }

    /**
     * R_AddPointToBox Expand this bbox so that it encloses a given point.
     *
     * @param x
     * @param y
     * @param box
     */
    fun AddPointToBox(x: Int, y: Int) {
        if (x < bbox[BBox.BOXLEFT]) bbox[BBox.BOXLEFT] = x
        if (x > bbox[BBox.BOXRIGHT]) bbox[BBox.BOXRIGHT] = x
        if (y < bbox[BBox.BOXBOTTOM]) bbox[BBox.BOXBOTTOM] = y
        if (y > bbox[BBox.BOXTOP]) bbox[BBox.BOXTOP] = y
    }

    operator fun get(BOXCOORDS: Int): Int {
        return bbox[BOXCOORDS]
    }

    operator fun set(BOXCOORDS: Int, `val`: Int) {
        bbox[BOXCOORDS] = `val`
    }

    companion object {
        const val BOXTOP = 0
        const val BOXBOTTOM = 1
        const val BOXLEFT = 2
        const val BOXRIGHT = 3

        // Static method
        fun ClearBox(box: Array<fixed_t>) {
            box[BBox.BOXRIGHT].set(Limits.MININT)
            box[BBox.BOXTOP].set(Limits.MININT)
            box[BBox.BOXLEFT].set(Limits.MAXINT)
            box[BBox.BOXBOTTOM].set(Limits.MAXINT)
        }

        fun AddToBox(box: Array<fixed_t>, x: fixed_t, y: fixed_t) {
            if (x.compareTo(box[BBox.BOXLEFT]) < 0) box[BBox.BOXLEFT].copy(x) else if (x.compareTo(
                    box[BBox.BOXRIGHT]
                ) > 0
            ) box[BBox.BOXRIGHT].copy(x)
            if (y.compareTo(box[BBox.BOXBOTTOM]) < 0) box[BBox.BOXBOTTOM] = y else if (y.compareTo(
                    box[BBox.BOXTOP]
                ) > 0
            ) box[BBox.BOXTOP] = y
        }

        /**
         * R_AddPointToBox Expand a given bbox so that it encloses a given point.
         *
         * @param x
         * @param y
         * @param box
         */
        fun AddPointToBox(x: Int, y: Int, box: Array<fixed_t>) {
            if (x < box[BBox.BOXLEFT].`val`) box[BBox.BOXLEFT].set(x)
            if (x > box[BBox.BOXRIGHT].`val`) box[BBox.BOXRIGHT].set(x)
            if (y < box[BBox.BOXBOTTOM].`val`) box[BBox.BOXBOTTOM].set(y)
            if (y > box[BBox.BOXTOP].`val`) box[BBox.BOXTOP].set(y)
        }

        fun ClearBox(bbox: IntArray) {
            bbox[BBox.BOXRIGHT] = Limits.MININT
            bbox[BBox.BOXTOP] = Limits.MININT
            bbox[BBox.BOXLEFT] = Limits.MAXINT
            bbox[BBox.BOXBOTTOM] = Limits.MAXINT
        }

        fun AddToBox(box: IntArray, x: Int, y: Int) {
            if (x < box[BBox.BOXLEFT]) box[BBox.BOXLEFT] = x
            if (x > box[BBox.BOXRIGHT]) box[BBox.BOXRIGHT] = x
            if (y < box[BBox.BOXBOTTOM]) box[BBox.BOXBOTTOM] = y
            if (y > box[BBox.BOXTOP]) box[BBox.BOXTOP] = y
        }
    }
}