package rr

/** A vissprite_t is a thing
 * that will be drawn during a refresh.
 * I.e. a sprite object that is partly visible.
 */
class vissprite_t<V> : Comparable<vissprite_t<V>> {
    // Doubly linked list.
    var prev: vissprite_t<V>? = null
    var next: vissprite_t<V>? = null
    var x1 = 0
    var x2 = 0

    // for line side calculation
    var gx = 0
    var gy = 0

    // global bottom / top for silhouette clipping
    var gz = 0
    var gzt = 0

    // horizontal position of x1
    var startfrac = 0
    var scale = 0

    // negative if flipped
    var xiscale = 0
    var texturemid = 0
    var patch = 0

    /** for color translation and shadow draw,
     * maxbright frames as well.
     *
     * Use paired with pcolormap;
     */
    var colormap: V? = null

    /* pointer into colormap
public int pcolormap; */
    var mobjflags = 0

    /** visspites are sorted by scale  */
    override fun compareTo(o: vissprite_t<V>): Int {
        // We only really care if it's drawn before. 
        if (scale > o.scale) return 1
        return if (scale < o.scale) -1 else 0
    }

    override fun toString(): String {
        return "Effective drawing position x1: $x1 x2: $x2 scale ${scale / 65535.0}"  + " iscale " + xiscale / 65535.0
    }
}