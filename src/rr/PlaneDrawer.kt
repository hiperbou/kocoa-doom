package rr

import data.Tables
import doom.DoomMain
import i.IDoomSystem
import m.fixed_t.Companion.FixedMul
import rr.RendererState.IPlaneDrawer
import rr.drawfuns.SpanVars
import rr.visplane_t.Companion.setVideoScale
import v.scale.VideoScale
import v.tables.LightsAndColors

abstract class PlaneDrawer<T, V> protected constructor(DOOM: DoomMain<T, V>, R: SceneRenderer<T, V>) : IPlaneDrawer {
    protected val RANGECHECK = false

    //
    // spanstart holds the start of a plane span
    // initialized to 0 at start
    //
    protected var spanstart: IntArray
    protected var spanstop: IntArray

    //
    // texture mapping
    //
    protected lateinit var planezlight // The distance lighting effect you see
            : Array<V>

    /** To treat as fixed_t  */
    protected var planeheight = 0

    /** To treat as fixed_t  */
    protected var distscale: IntArray

    /** To treat as fixed_t  */
    protected var cacheddistance: IntArray
    protected var cachedxstep: IntArray
    protected var cachedystep: IntArray
    protected val view: ViewVars
    protected val seg_vars: SegVars
    protected val dsvars: SpanVars<T, V>

    /** The visplane data. Set separately. For threads, use the same for
     * everyone.
     */
    protected var vpvars: Visplanes
    protected val colormap: LightsAndColors<V>
    protected val TexMan: TextureManager<T>
    protected val I: IDoomSystem
    protected val vs: VideoScale

    init {
        view = R.getView()
        vpvars = R.getVPVars()
        dsvars = R.getDSVars()
        seg_vars = R.getSegVars()
        colormap = R.getColorMap()
        TexMan = R.getTextureManager()
        I = R.getDoomSystem()
        vs = DOOM.vs
        // Pre-scale stuff.
        spanstart = IntArray(vs.getScreenHeight())
        spanstop = IntArray(vs.getScreenHeight())
        distscale = IntArray(vs.getScreenWidth())
        cacheddistance = IntArray(vs.getScreenHeight())
        cachedxstep = IntArray(vs.getScreenHeight()) //TODO: Width?
        cachedystep = IntArray(vs.getScreenHeight())

        // HACK: visplanes are initialized globally.
        setVideoScale(vs)
        vpvars.initVisplanes()
    }

    /**
     * R_MapPlane
     *
     * Called only by R_MakeSpans.
     *
     * This is where the actual span drawing function is called.
     *
     * Uses global vars: planeheight ds_source -> flat data has already been
     * set. basexscale -> actual drawing angle and position is computed from
     * these baseyscale viewx viewy
     *
     * BASIC PRIMITIVE
     */
    override fun MapPlane(y: Int, x1: Int, x2: Int) {
        // MAES: angle_t
        val angle: Int
        // fixed_t
        val distance: Int
        val length: Int
        var index: Int
        if (RANGECHECK) {
            rangeCheck(x1, x2, y)
        }
        if (planeheight != vpvars.cachedheight[y]) {
            vpvars.cachedheight[y] = planeheight
            cacheddistance[y] = FixedMul(planeheight, vpvars.yslope[y])
            distance = cacheddistance[y]
            cachedxstep[y] = FixedMul(distance, vpvars.basexscale)
            dsvars.ds_xstep = cachedxstep[y]
            cachedystep[y] = FixedMul(distance, vpvars.baseyscale)
            dsvars.ds_ystep = cachedystep[y]
        } else {
            distance = cacheddistance[y]
            dsvars.ds_xstep = cachedxstep[y]
            dsvars.ds_ystep = cachedystep[y]
        }
        length = FixedMul(distance, distscale[x1])
        angle = (view.angle + view.xtoviewangle[x1] and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT).toInt()
        dsvars.ds_xfrac = view.x + FixedMul(Tables.finecosine[angle], length)
        dsvars.ds_yfrac = -view.y - FixedMul(Tables.finesine[angle], length)
        if (colormap.fixedcolormap != null) dsvars.ds_colormap = colormap.fixedcolormap else {
            index = distance ushr colormap.lightZShift()
            if (index >= colormap.maxLightZ()) index = colormap.maxLightZ() - 1
            dsvars.ds_colormap = planezlight[index]
        }
        dsvars.ds_y = y
        dsvars.ds_x1 = x1
        dsvars.ds_x2 = x2

        // high or low detail
        dsvars.spanfunc!!.invoke()
    }

    protected fun rangeCheck(x1: Int, x2: Int, y: Int) {
        if (x2 < x1 || x1 < 0 || x2 >= view.width || y > view.height) I.Error(
            "%s: %d, %d at %d",
            this.javaClass.name,
            x1,
            x2,
            y
        )
    }

    /**
     * R_MakeSpans
     *
     * Called only by DrawPlanes. If you wondered where the actual
     * boundaries for the visplane flood-fill are laid out, this is it.
     *
     * The system of coords seems to be defining a sort of cone.
     *
     *
     * @param x
     * Horizontal position
     * @param t1
     * Top-left y coord?
     * @param b1
     * Bottom-left y coord?
     * @param t2
     * Top-right y coord ?
     * @param b2
     * Bottom-right y coord ?
     */
    protected open fun MakeSpans(x: Int, t1: Int, b1: Int, t2: Int, b2: Int) {

        // If t1 = [sentinel value] then this part won't be executed.
        var t1 = t1
        var b1 = b1
        var t2 = t2
        var b2 = b2
        while (t1 < t2 && t1 <= b1) {
            MapPlane(t1, spanstart[t1], x - 1)
            t1++
        }
        while (b1 > b2 && b1 >= t1) {
            MapPlane(b1, spanstart[b1], x - 1)
            b1--
        }

        // So...if t1 for some reason is < t2, we increase t2 AND store the
        // current x
        // at spanstart [t2] :-S
        while (t2 < t1 && t2 <= b2) {
            // System.out.println("Increasing t2");
            spanstart[t2] = x
            t2++
        }

        // So...if t1 for some reason b2 > b1, we decrease b2 AND store the
        // current x
        // at spanstart [t2] :-S
        while (b2 > b1 && b2 >= t2) {
            // System.out.println("Decreasing b2");
            spanstart[b2] = x
            b2--
        }
    }

    /**
     * R_InitPlanes Only at game startup.
     */
    override fun InitPlanes() {
        // Doh!
    }

    protected fun rangeCheckErrors() {
        if (seg_vars.ds_p > seg_vars.MAXDRAWSEGS) I.Error("R_DrawPlanes: drawsegs overflow (%d)", seg_vars.ds_p)
        if (vpvars.lastvisplane > vpvars.MAXVISPLANES) I.Error(
            " R_DrawPlanes: visplane overflow (%d)",
            vpvars.lastvisplane
        )
        if (vpvars.lastopening > vpvars.MAXOPENINGS) I.Error("R_DrawPlanes: opening overflow (%d)", vpvars.lastopening)
    }

    /** Default implementation which DOES NOTHING. MUST OVERRIDE  */
    override fun DrawPlanes() {}
    override fun sync() {
        // Nothing required if serial.
    }

    /////////////// VARIOUS BORING GETTERS ////////////////////
    override fun getDistScale(): IntArray {
        return distscale
    }

    companion object {
        private const val DEBUG2 = false
    }
}