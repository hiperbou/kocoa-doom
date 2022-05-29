package rr.parallel

import data.Defines
import data.Tables
import doom.DoomMain
import m.fixed_t.Companion.FRACBITS
import rr.IDetailAware
import rr.PlaneDrawer
import rr.SceneRenderer
import rr.drawfuns.*
import rr.visplane_t
import rr.visplane_t.Companion.SENTINEL
import rr.visplane_t.Companion.THREADIDBITS
import rr.visplane_t.Companion.THREADIDCLEAR
import rr.visplane_t.Companion.THREADIDSHIFT
import rr.visplane_t.Companion.THREADVALUEBITS
import v.graphics.Lights
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier

/** Visplane worker which shares work in an equal screen-portions strategy.
 *
 * More balanced, but requires careful synchronization to avoid overdrawing and
 * stomping.
 *
 *
 * @author vepitrop.
 *
 * TODO: fix crashes
 */
abstract class VisplaneWorker2<T, V>(
    DOOM: DoomMain<T, V>, R: SceneRenderer<T, V>, protected val id: Int,
    /**
     * R_MapPlane
     *
     * Called only by R_MakeSpans.
     *
     * This is where the actual span drawing function is called.
     *
     * Uses global vars:
     * planeheight
     * ds_source -> flat data has already been set.
     * basexscale -> actual drawing angle and position is computed from these
     * baseyscale
     * viewx
     * viewy
     *
     * BASIC PRIMITIVE
     */
    /* TODO: entirely similar to serial version?
      private void
      MapPlane
      ( int       y,
        int       x1,
        int       x2 )
      {
          // MAES: angle_t
          int angle;
          // fixed_t
          int distance;
          int length;
          int index;
          
      if (RANGECHECK){
          if (x2 < x1
          || x1<0
          || x2>=view.width
          || y>view.height)
          {
          I.Error ("R_MapPlane: %d, %d at %d",x1,x2,y);
          }
      }

          if (vpw_planeheight != cachedheight[y])
          {
          cachedheight[y] = vpw_planeheight;
          distance = cacheddistance[y] = FixedMul (vpw_planeheight , yslope[y]);
          vpw_dsvars.ds_xstep = cachedxstep[y] = FixedMul (distance,vpw_basexscale);
          vpw_dsvars.ds_ystep = cachedystep[y] = FixedMul (distance,vpw_baseyscale);
          }
          else
          {
          distance = cacheddistance[y];
          vpw_dsvars.ds_xstep = cachedxstep[y];
          vpw_dsvars.ds_ystep = cachedystep[y];
          }
          
          length = FixedMul (distance,distscale[x1]);
          angle = (int)(((view.angle +xtoviewangle[x1])&BITS32)>>>ANGLETOFINESHIFT);
          vpw_dsvars.ds_xfrac = view.x + FixedMul(finecosine[angle], length);
          vpw_dsvars.ds_yfrac = -view.y - FixedMul(finesine[angle], length);

          if (colormap.fixedcolormap!=null)
              vpw_dsvars.ds_colormap = colormap.fixedcolormap;
          else
          {
          index = distance >>> LIGHTZSHIFT;
          
          if (index >= MAXLIGHTZ )
              index = MAXLIGHTZ-1;

          vpw_dsvars.ds_colormap = vpw_planezlight[index];
          }
          
          vpw_dsvars.ds_y = y;
          vpw_dsvars.ds_x1 = x1;
          vpw_dsvars.ds_x2 = x2;

          // high or low detail
          if (view.detailshift==0)
              vpw_spanfunc.invoke();
          else
              vpw_spanfunclow.invoke();         
      }
      */
    // Private to each thread.
    var barrier: CyclicBarrier, NUMFLOORTHREADS: Int
) : PlaneDrawer<T, V>(DOOM, R), Runnable, IDetailAware {
    protected val NUMFLOORTHREADS: Int
    protected var startvp = 0
    protected var endvp = 0
    protected var vpw_planeheight = 0
    protected lateinit var vpw_planezlight: Array<V>
    protected var vpw_basexscale = 0
    protected var vpw_baseyscale = 0
    protected val vpw_dsvars: SpanVars<T, V>
    protected val vpw_dcvars: ColVars<T, V>
    protected var vpw_spanfunc: DoomSpanFunction<T, V>? = null
    protected var vpw_skyfunc: DoomColumnFunction<T, V>? = null
    protected var vpw_spanfunchi: DoomSpanFunction<T, V>? = null
    protected var vpw_spanfunclow: DoomSpanFunction<T, V>? = null
    protected var vpw_skyfunchi: DoomColumnFunction<T, V>? = null
    protected var vpw_skyfunclow: DoomColumnFunction<T, V>? = null
    protected var pln: visplane_t? = null

    class HiColor(
        DOOM: DoomMain<ByteArray?, ShortArray?>, R: SceneRenderer<ByteArray?, ShortArray?>, id: Int,
        columnofs: IntArray, ylookup: IntArray, screen: ShortArray?,
        visplanebarrier: CyclicBarrier, NUMFLOORTHREADS: Int
    ) : VisplaneWorker2<ByteArray?, ShortArray?>(DOOM, R, id, visplanebarrier, NUMFLOORTHREADS) {
        init {
            vpw_spanfunchi = R_DrawSpanUnrolled.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dsvars,
                screen,
                I
            )
            vpw_spanfunc = vpw_spanfunchi
            vpw_spanfunclow = R_DrawSpanLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dsvars,
                screen,
                I
            )
            vpw_skyfunchi = R_DrawColumnBoomOpt.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dcvars,
                screen,
                I
            )
            vpw_skyfunc = vpw_skyfunchi
            vpw_skyfunclow = R_DrawColumnBoomOptLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dcvars,
                screen,
                I
            )
        }
    }

    class Indexed(
        DOOM: DoomMain<ByteArray?, ByteArray?>, R: SceneRenderer<ByteArray?, ByteArray?>, id: Int,
        columnofs: IntArray, ylookup: IntArray, screen: ByteArray?,
        visplanebarrier: CyclicBarrier, NUMFLOORTHREADS: Int
    ) : VisplaneWorker2<ByteArray?, ByteArray?>(DOOM, R, id, visplanebarrier, NUMFLOORTHREADS) {
        init {
            vpw_spanfunchi = R_DrawSpanUnrolled.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dsvars,
                screen,
                I
            )
            vpw_spanfunc = vpw_spanfunchi
            vpw_spanfunclow = R_DrawSpanLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dsvars,
                screen,
                I
            )
            vpw_skyfunchi = R_DrawColumnBoomOpt.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dcvars,
                screen,
                I
            )
            vpw_skyfunc = vpw_skyfunchi
            vpw_skyfunclow = R_DrawColumnBoomOptLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dcvars,
                screen,
                I
            )
        }
    }

    class TrueColor(
        DOOM: DoomMain<ByteArray?, IntArray?>, R: SceneRenderer<ByteArray?, IntArray?>, id: Int,
        columnofs: IntArray, ylookup: IntArray, screen: IntArray?,
        visplanebarrier: CyclicBarrier, NUMFLOORTHREADS: Int
    ) : VisplaneWorker2<ByteArray?, IntArray?>(DOOM, R, id, visplanebarrier, NUMFLOORTHREADS) {
        init {
            vpw_spanfunchi = R_DrawSpanUnrolled.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dsvars,
                screen,
                I
            )
            vpw_spanfunc = vpw_spanfunchi
            vpw_spanfunclow = R_DrawSpanLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dsvars,
                screen,
                I
            )
            vpw_skyfunchi = R_DrawColumnBoomOpt.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dcvars,
                screen,
                I
            )
            vpw_skyfunc = vpw_skyfunchi
            vpw_skyfunclow = R_DrawColumnBoomOptLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                vpw_dcvars,
                screen,
                I
            )
        }
    }

    override fun run() {
        pln = null //visplane_t
        // These must override the global ones
        var light: Int
        var x: Int
        var stop: Int
        var angle: Int
        var minx: Int
        var maxx: Int

        // Now it's a good moment to set them.
        vpw_basexscale = vpvars.getBaseXScale()
        vpw_baseyscale = vpvars.getBaseYScale()
        startvp = id * view.width / NUMFLOORTHREADS
        endvp = (id + 1) * view.width / NUMFLOORTHREADS

        // TODO: find a better way to split work. As it is, it's very uneven
        // and merged visplanes in particular are utterly dire.
        for (pl in 0 until vpvars.lastvisplane) {
            pln = vpvars.visplanes[pl]
            val pln = pln!!
            // System.out.println(id +" : "+ pl);

            // Trivial rejection.
            if (pln.minx > endvp || pln.maxx < startvp) {
                continue
            }

            // Reject non-visible  
            if (pln.minx > pln.maxx) {
                continue
            }

            // Trim to zone
            minx = Math.max(pln.minx, startvp)
            maxx = Math.min(pln.maxx, endvp)

            // sky flat
            if (pln.picnum == TexMan.getSkyFlatNum()) {
                // Cache skytexture stuff here. They aren't going to change while
                // being drawn, after all, are they?
                val skytexture = TexMan.getSkyTexture()
                // MAES: these must be updated to keep up with screen size changes.
                vpw_dcvars.viewheight = view.height
                vpw_dcvars.centery = view.centery
                vpw_dcvars.dc_texheight = TexMan.getTextureheight(skytexture) shr FRACBITS
                vpw_dcvars.dc_iscale = vpvars.getSkyScale() shr view.detailshift
                vpw_dcvars.dc_colormap = colormap.colormaps[Lights.COLORMAP_FIXED]
                vpw_dcvars.dc_texturemid = TexMan.getSkyTextureMid()
                x = minx
                while (x <= maxx) {
                    vpw_dcvars.dc_yl = pln.getTop(x).code
                    vpw_dcvars.dc_yh = pln.getBottom(x)
                    if (vpw_dcvars.dc_yl <= vpw_dcvars.dc_yh) {
                        angle =
                            (Tables.addAngles(view.angle, view.xtoviewangle[x]) ushr Defines.ANGLETOSKYSHIFT).toInt()
                        vpw_dcvars.dc_x = x
                        vpw_dcvars.dc_texheight =
                            TexMan.getTextureheight(TexMan.getSkyTexture()) shr FRACBITS
                        vpw_dcvars.dc_source = TexMan.GetCachedColumn(TexMan.getSkyTexture(), angle)
                        vpw_skyfunc!!.invoke()
                    }
                    x++
                }
                continue
            }

            // regular flat
            vpw_dsvars.ds_source = TexMan.getSafeFlat(pln.picnum)
            vpw_planeheight = Math.abs(pln.height - view.z)
            light = (pln.lightlevel ushr colormap.lightSegShift()) + colormap.extralight
            if (light >= colormap.lightLevels()) {
                light = colormap.lightLevels() - 1
            }
            if (light < 0) {
                light = 0
            }
            vpw_planezlight = colormap.zlight[light] as Array<V>

            // Some tinkering required to make sure visplanes
            // don't end prematurely on each other's stop markers
            var value = pln.getTop(maxx + 1)
            if (!isMarker(value.code)) { // is it a marker?
                value = (value.code or SENTINEL.code).toChar() // Mark it so.
                value = (value.code and THREADIDCLEAR.code).toChar() //clear id bits
                value = (value.code or (id shl THREADIDSHIFT.code)).toChar() // set our own id.
            } // Otherwise, it was set by another thread.
            // Leave it be.
            pln.setTop(maxx + 1, value)
            value = pln.getTop(minx - 1)
            if (!isMarker(value.code)) { // is it a marker?
                value = (value.code or SENTINEL.code).toChar() // Mark it so.
                value = (value.code and THREADIDCLEAR.code).toChar() //clear id bits
                value = (value.code or (id shl THREADIDSHIFT.code)).toChar() // set our own id.
            } // Otherwise, it was set by another thread.
            // Leave it be.
            pln.setTop(minx - 1, value)
            stop = maxx + 1
            x = minx
            while (x <= stop) {
                MakeSpans(
                    x, pln.getTop(x - 1).code,
                    pln.getBottom(x - 1),
                    pln.getTop(x).code,
                    pln.getBottom(x)
                )
                x++
            }
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

    private fun isMarker(t1: Int): Boolean {
        return t1 and SENTINEL.code != 0
    }

    private fun decodeID(t1: Int): Int {
        return t1 and THREADIDBITS.code shr THREADIDSHIFT.code
    }

    private fun decodeValue(t1: Int): Int {
        return t1 and THREADVALUEBITS.code
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

    /**
     * R_MakeSpans
     *
     * Called only by DrawPlanes.
     * If you wondered where the actual boundaries for the visplane
     * flood-fill are laid out, this is it.
     *
     * The system of coords seems to be defining a sort of cone.
     *
     *
     * @param x Horizontal position
     * @param t1 Top-left y coord?
     * @param b1 Bottom-left y coord?
     * @param t2 Top-right y coord ?
     * @param b2 Bottom-right y coord ?
     */
    override fun MakeSpans(x: Int, t1: Int, b1: Int, t2: Int, b2: Int) {

        // Top 1 sentinel encountered.
        var t1 = t1
        var t2 = t2
        if (isMarker(t1)) {
            if (decodeID(t1) != id) // We didn't put it here.
            {
                t1 = decodeValue(t1)
            }
        }

        // Top 2 sentinel encountered.
        if (isMarker(t2)) {
            if (decodeID(t2) != id) {
                t2 = decodeValue(t2)
            }
        }
        super.MakeSpans(x, t1, b1, t2, b2)
    }

    init {
        // Alias to those of Planes.
        vpw_dsvars = SpanVars()
        vpw_dcvars = ColVars()
        this.NUMFLOORTHREADS = NUMFLOORTHREADS
    }
}