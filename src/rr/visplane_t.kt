package rr

import utils.C2JUtils
import v.scale.VideoScale

/**
 * Now what is a visplane, anyway? Basically, it's a bunch of arrays buffer representing a top and a bottom boundary of
 * a region to be filled with a specific kind of flat. They are as wide as the screen, and actually store height
 * bounding values or sentinel valuesThink of it as an arbitrary boundary.
 *
 * These are refreshed continuously during rendering, and mark the limits between flat regions. Special values mean "do
 * not render this column at all", while clipping out of the map bounds results in well-known bleeding effects.
 *
 * @author admin
 */
class visplane_t {
    constructor() {
        data = CharArray(4 + 2 * vs!!.getScreenWidth())
        updateHashCode()
    }

    constructor(height: Int, picnum: Int, lightlevel: Int) {
        this.height = height
        this.picnum = picnum
        this.lightlevel = lightlevel
        updateHashCode()
        data = CharArray(4 + 2 * vs!!.getScreenWidth())
    }

    /**
     * (fixed_t)
     */
    var height = 0
    var picnum = 0
    var lightlevel = 0
    var minx = 0
    var maxx = 0

    // leave pads for [minx-1]/[maxx+1]
    /*
    public byte      pad1;
    // Here lies the rub for all
    //  dynamic resize/change of resolution.
    public byte[]      top=new byte[vs.getScreenWidth()];
    public byte      pad2;
    public byte      pad3;
    // See above.
    public byte[]      bottom=new byte [vs.getScreenWidth()];
    public byte      pad4;*/
    var data: CharArray

    /**
     * "Clear" the top with FF's.
     */
    fun clearTop() {
        System.arraycopy(
            clearvisplane,
            0,
            data,
            TOPOFFSET,
            vs!!.getScreenWidth()
        )
    }

    /**
     * "Clear" the bottom with FF's.
     */
    fun clearBottom() {
        System.arraycopy(
            clearvisplane,
            0,
            data,
            BOTTOMOFFSET,
            vs!!.getScreenWidth()
        )
    }

    fun setTop(index: Int, value: Char) {
        data[TOPOFFSET + index] = value
    }

    fun getTop(index: Int): Char {
        return data[TOPOFFSET + index]
    }

    fun setBottom(index: Int, value: Char) {
        data[BOTTOMOFFSET + index] = value
    }

    fun getBottom(index: Int): Int {
        return data[BOTTOMOFFSET + index].toInt()
    }

    override fun toString(): String {
        sb.setLength(0)
        sb.append("Visplane\n")
        sb.append('\t')
        sb.append("Height: ")
        sb.append(height)
        sb.append('\t')
        sb.append("Min-Max: ")
        sb.append(minx)
        sb.append('-')
        sb.append(maxx)
        sb.append('\t')
        sb.append("Picnum: ")
        sb.append(picnum)
        sb.append('\t')
        sb.append("Lightlevel: ")
        sb.append(lightlevel)
        return sb.toString()
    }

    protected var hash = 0

    /**
     * Call this upon any changed in height, picnum or lightlevel
     */
    fun updateHashCode() {
        hash = height xor picnum xor lightlevel
    }

    override fun hashCode(): Int {
        return hash
    }

    companion object {
        const val TOPOFFSET = 1
        const val MIDDLEPADDING = 2
        var BOTTOMOFFSET = 0

        // Multithreading trickery (for strictly x-bounded drawers)
        // The thread if is encoded in the upper 3 bits (puts an upper limit 
        // of 8 floor threads), and the stomped value is encoded in the next 12 
        // bits (this puts an upper height limit of 4096 pixels). 
        // Not the cleanest system possible, but it's backwards compatible
        // TODO: expand visplane buffers to full-fledged ints?
        const val SENTINEL = 0x8000.toChar()
        const val THREADIDSHIFT = 12.toChar()
        const val THREADIDCLEAR = 0x8FFF.toChar()
        val THREADIDBITS: Char = (0XFFFF - THREADIDCLEAR.code).toChar()
        val THREADVALUEBITS: Char =
            (THREADIDCLEAR.code - SENTINEL.code).toChar()

        // Hack to allow quick clearing of visplanes.
        protected var clearvisplane: CharArray? = null
        fun visplaneHash(height: Int, picnum: Int, lightlevel: Int): Int {
            return height xor picnum xor lightlevel
        }

        protected var sb = StringBuilder()

        // HACK: the resolution awareness is shared between all visplanes.
        // Change this if you ever plan on running multiple renderers with
        // different resolution or something.
        protected lateinit var vs: VideoScale
        fun setVideoScale(vs: VideoScale) {
            visplane_t.vs = vs
            BOTTOMOFFSET =
                vs.getScreenWidth() + TOPOFFSET + MIDDLEPADDING
            if (clearvisplane == null || clearvisplane!!.size < vs.getScreenWidth()) {
                clearvisplane = CharArray(vs.getScreenWidth())
                C2JUtils.memset(
                    clearvisplane!!,
                    Character.MAX_VALUE,
                    clearvisplane!!.size
                )
            }
        }
    }
}