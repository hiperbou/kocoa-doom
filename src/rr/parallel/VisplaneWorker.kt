package rr.parallel

import data.Defines
import data.Tables
import doom.DoomMain
import m.fixed_t.Companion.FRACBITS
import rr.IDetailAware
import rr.PlaneDrawer
import rr.SceneRenderer
import rr.drawfuns.*
import v.graphics.Lights
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier

/** Visplane worker which shares work in an equal-visplane number strategy
 * with other workers. Might be unbalanced if one worker gets too large
 * visplanes and others get smaller ones. Balancing strategy is applied in
 * run(), otherwise it's practically similar to a PlaneDrwer.
 *
 *
 * @author velktron
 */
abstract class VisplaneWorker<T, V>(
    DOOM: DoomMain<T, V>, // Private to each thread.
    protected val id: Int,
    SCREENWIDTH: Int,
    SCREENHEIGHT: Int,
    R: SceneRenderer<T, V>,
    protected val barrier: CyclicBarrier,
    protected val NUMFLOORTHREADS: Int
) : PlaneDrawer<T, V>(DOOM, R), Runnable, IDetailAware {
    protected var vpw_planeheight = 0
    protected lateinit var vpw_planezlight: Array<V>
    protected var vpw_basexscale = 0
    protected var vpw_baseyscale = 0
    protected lateinit var vpw_dsvars: SpanVars<T, V>
    protected lateinit var vpw_dcvars: ColVars<T, V>

    // OBVIOUSLY each thread must have its own span functions.
    protected var vpw_spanfunc: DoomSpanFunction<T, V>? = null
    protected var vpw_skyfunc: DoomColumnFunction<T, V>? = null
    protected var vpw_spanfunchi: DoomSpanFunction<T, V>? = null
    protected var vpw_spanfunclow: DoomSpanFunction<T, V>? = null
    protected var vpw_skyfunchi: DoomColumnFunction<T, V>? = null
    protected var vpw_skyfunclow: DoomColumnFunction<T, V>? = null

    class HiColor(
        DOOM: DoomMain<ByteArray?, ShortArray?>,
        id: Int,
        SCREENWIDTH: Int,
        SCREENHEIGHT: Int,
        R: SceneRenderer<ByteArray?, ShortArray?>,
        columnofs: IntArray,
        ylookup: IntArray,
        screen: ShortArray?,
        visplanebarrier: CyclicBarrier,
        NUMFLOORTHREADS: Int
    ) : VisplaneWorker<ByteArray?, ShortArray?>(
        DOOM,
        id,
        SCREENWIDTH,
        SCREENHEIGHT,
        R,
        visplanebarrier,
        NUMFLOORTHREADS
    ) {
        init {
            // Alias to those of Planes.
            vpw_dsvars = SpanVars()
            vpw_dcvars = ColVars()
            vpw_spanfunchi =
                R_DrawSpanUnrolled.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, vpw_dsvars, screen, I)
            vpw_spanfunc = vpw_spanfunchi
            vpw_spanfunclow =
                R_DrawSpanLow.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, vpw_dsvars, screen, I)
            vpw_skyfunchi =
                R_DrawColumnBoomOpt.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, vpw_dcvars, screen, I)
            vpw_skyfunc = vpw_skyfunchi
            vpw_skyfunclow =
                R_DrawColumnBoomOptLow.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, vpw_dcvars, screen, I)
        }
    }

    override fun setDetail(detailshift: Int) {
        if (detailshift == 0) {
            vpw_spanfunc = vpw_spanfunchi
            vpw_skyfunc = vpw_skyfunchi
        } else {
            vpw_spanfunc = vpw_spanfunclow
            vpw_skyfunc = vpw_skyfunclow
        }
    }

    override fun run() {
        //var pln: visplane_t? = null //visplane_t
        // These must override the global ones
        var light: Int
        var x: Int
        var stop: Int
        var angle: Int

        // Now it's a good moment to set them.
        vpw_basexscale = vpvars.getBaseXScale()
        vpw_baseyscale = vpvars.getBaseYScale()

        // TODO: find a better way to split work. As it is, it's very uneven
        // and merged visplanes in particular are utterly dire.
        var pl = id
        while (pl < vpvars.lastvisplane) {
            val pln = vpvars.visplanes[pl]!!
            // System.out.println(id +" : "+ pl);
            if (pln.minx > pln.maxx) {
                pl += NUMFLOORTHREADS
                continue
            }


            // sky flat
            if (pln.picnum == TexMan.getSkyFlatNum()) {
                // Cache skytexture stuff here. They aren't going to change while
                // being drawn, after all, are they?
                val skytexture = TexMan.getSkyTexture()
                // MAES: these must be updated to keep up with screen size changes.
                vpw_dcvars!!.viewheight = view.height
                vpw_dcvars!!.centery = view.centery
                vpw_dcvars!!.dc_texheight = TexMan.getTextureheight(skytexture) shr FRACBITS
                vpw_dcvars!!.dc_iscale = vpvars.getSkyScale() shr view.detailshift
                vpw_dcvars!!.dc_colormap = colormap.colormaps[Lights.COLORMAP_FIXED]
                vpw_dcvars!!.dc_texturemid = TexMan.getSkyTextureMid()
                x = pln.minx
                while (x <= pln.maxx) {
                    vpw_dcvars!!.dc_yl = pln.getTop(x).code
                    vpw_dcvars!!.dc_yh = pln.getBottom(x)
                    if (vpw_dcvars!!.dc_yl <= vpw_dcvars!!.dc_yh) {
                        angle =
                            (Tables.addAngles(view.angle, view.xtoviewangle[x]) ushr Defines.ANGLETOSKYSHIFT).toInt()
                        vpw_dcvars!!.dc_x = x
                        // Optimized: texheight is going to be the same during normal skies drawing...right?
                        vpw_dcvars!!.dc_source = TexMan.GetCachedColumn(TexMan.getSkyTexture(), angle)
                        vpw_skyfunc!!.invoke()
                    }
                    x++
                }
                pl += NUMFLOORTHREADS
                continue
            }

            // regular flat
            vpw_dsvars!!.ds_source = TexMan.getSafeFlat(pln.picnum)
            vpw_planeheight = Math.abs(pln.height - view.z)
            light = (pln.lightlevel ushr colormap.lightSegShift()) + colormap.extralight
            if (light >= colormap.lightLevels()) light = colormap.lightLevels() - 1
            if (light < 0) light = 0
            vpw_planezlight = colormap.zlight[light] as Array<V>

            // We set those values at the border of a plane's top to a "sentinel" value...ok.
            pln.setTop(pln.maxx + 1, 0xffff.toChar())
            pln.setTop(pln.minx - 1, 0xffff.toChar())
            stop = pln.maxx + 1
            x = pln.minx
            while (x <= stop) {
                MakeSpans(
                    x, pln.getTop(x - 1).code,
                    pln.getBottom(x - 1),
                    pln.getTop(x).code,
                    pln.getBottom(x)
                )
                x++
            }
            pl += NUMFLOORTHREADS
        }
        // We're done, wait.
        try {
            barrier.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            e.printStackTrace()
        }
    }
}