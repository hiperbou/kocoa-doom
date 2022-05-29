package rr

/** A maptexturedef_t describes a rectangular texture,
 * which is composed of one or more mappatch_t structures
 * that arrange graphic patches.
 *
 * This is the in-memory format, which is similar to maptexture_t (which is on-disk).
 *
 * @author Maes
 */
class texture_t {
    /** Keep name for switch changing, etc.  */
    var name: String? = null
    var width: Short = 0
    var height: Short = 0

    // All the patches[patchcount]
    //  are drawn back to front into the cached texture.
    var patchcount: Short = 0
    lateinit var patches: Array<texpatch_t?>

    /** Unmarshalling at its best!  */
    fun copyFromMapTexture(mt: maptexture_t) {
        name = "" + (mt.name)
        width = mt.width
        height = mt.height
        patchcount = mt.patchcount
        patches = arrayOfNulls(patchcount.toInt())
        for (i in 0 until patchcount) {
            patches[i] = texpatch_t()
            patches[i]!!.copyFromMapPatch(mt.patches[i])
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(name)
        sb.append(" Height ")
        sb.append(height.toInt())
        sb.append(" Width ")
        sb.append(width.toInt())
        sb.append(" Patchcount ")
        sb.append(patchcount.toInt())
        return sb.toString()
    }
}