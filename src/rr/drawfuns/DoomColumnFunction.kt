package rr.drawfuns

 
import i.IDoomSystem
import v.tables.BlurryTable

/** Prototype for
 *
 * @author velktron
 *
 * @param <T>
</T> */
abstract class DoomColumnFunction<T, V> : ColumnFunction<T, V> {
    protected val RANGECHECK = false
    protected val SCREENWIDTH: Int
    protected val SCREENHEIGHT: Int
    protected var dcvars: ColVars<T, V>
    protected val screen: V
    protected val I: IDoomSystem?
    protected val ylookup: IntArray
    protected val columnofs: IntArray
    protected var blurryTable: BlurryTable?
    protected var _flags = 0

    constructor(
        sCREENWIDTH: Int, sCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<T, V>?, screen: V, I: IDoomSystem?
    ) {
        SCREENWIDTH = sCREENWIDTH
        SCREENHEIGHT = sCREENHEIGHT
        this.ylookup = ylookup
        this.columnofs = columnofs
        this.dcvars = dcvars!!
        this.screen = screen
        this.I = I
        blurryTable = null
    }

    constructor(
        sCREENWIDTH: Int, sCREENHEIGHT: Int, ylookup: IntArray,
        columnofs: IntArray, dcvars: ColVars<T, V>?, screen: V, I: IDoomSystem?, BLURRY_MAP: BlurryTable?
    ) {
        SCREENWIDTH = sCREENWIDTH
        SCREENHEIGHT = sCREENHEIGHT
        this.ylookup = ylookup
        this.columnofs = columnofs
        this.dcvars = dcvars!!
        this.screen = screen
        this.I = I
        blurryTable = BLURRY_MAP
    }

    protected fun performRangeCheck() {
        if (dcvars.dc_x >= SCREENWIDTH || dcvars.dc_yl < 0 || dcvars.dc_yh >= SCREENHEIGHT) I?.Error(
            "R_DrawColumn: %d to %d at %d",
            dcvars.dc_yl,
            dcvars.dc_yh,
            dcvars.dc_x
        )
    }

    /**
     *
     * Use ylookup LUT to avoid multiply with ScreenWidth.
     * Use columnofs LUT for subwindows?
     *
     * @return Framebuffer destination address.
     */
    protected fun computeScreenDest(): Int {
        return ylookup[dcvars.dc_yl] + columnofs[dcvars.dc_x]
    }

    protected fun blockyDest1(): Int {
        return ylookup[dcvars.dc_yl] + columnofs[dcvars.dc_x shl 1]
    }

    protected fun blockyDest2(): Int {
        return ylookup[dcvars.dc_yl] + columnofs[(dcvars.dc_x shl 1) + 1]
    }

    override fun invoke(dcvars: ColVars<T, V>) {
        this.dcvars = dcvars
        invoke()
    }

    override fun getFlags(): Int {
        return _flags
    }
}