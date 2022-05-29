package rr.parallel

import data.Tables
import doom.DoomMain
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FixedMul
import rr.IDetailAware
import rr.TextureManager
import rr.drawfuns.ColVars
import rr.drawfuns.DoomColumnFunction
import rr.drawfuns.R_DrawColumnBoomOpt
import rr.drawfuns.R_DrawColumnBoomOptLow
import rr.visplane_t
import v.tables.LightsAndColors
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier

/** This is what actual executes the RenderSegInstructions.
 * *
 * Each thread actually operates on a FIXED PORTION OF THE SCREEN
 * (e.g. half-width, third-width etc.) and only renders the portions
 * of the RenderSegInstructions that are completely contained
 * within its own screen area. For this reason, all threads
 * check out all RenderSegInstructions of the list, and render any
 * and all portions that are within their responsability domain, so
 * to speak.
 *
 * FIXME there's a complex data dependency with ceilingclip/floorclip
 * I was not quite able to fix yet. Practically, in the serial renderer,
 * calls to RenderSegLoop are done in a correct, non-overlapping order,
 * and certain parts are drawn before others in order to set current
 * floor/ceiling markers and visibility e.g. think of a wall visible
 * through windows.
 *
 * FIXME 7/6/2011 Data dependencies and per-thread clipping are now
 * fixed, however there is still visible "jitter" or "noise" on some
 * of the walls, probably related to column offsets.
 *
 * @author velktron
 */
abstract class RenderSegExecutor<T, V>(
    protected val DOOM: DoomMain<T, V>, protected val id: Int, screen: V,
    protected val TexMan: TextureManager<T>,
    protected var RSI: Array<RenderSegInstruction<V>>,
    protected val BLANKCEILINGCLIP: ShortArray,
    // Each thread should do its own ceiling/floor blanking
    protected val BLANKFLOORCLIP: ShortArray,
    protected val ceilingclip: ShortArray,
    protected val floorclip: ShortArray,
    columnofs: IntArray,
    protected val xtoviewangle: LongArray,
    ylookup: IntArray,
    visplanes: Array<visplane_t?>?,
    protected val barrier: CyclicBarrier,
    // These need to be set on creation, and are unchangeable.
    protected val colormaps: LightsAndColors<V>
) : Runnable, IDetailAware {
    // This needs to be set by the partitioner.
    protected var rw_start = 0
    protected var rw_end = 0
    protected var rsiend = 0
    protected var colfunchi: DoomColumnFunction<T, V>? = null
    protected var colfunclow: DoomColumnFunction<T, V>? = null
    protected var colfunc: DoomColumnFunction<T, V>? = null
    protected var dcvars: ColVars<T, V>? = null
    protected fun ProcessRSI(rsi: RenderSegInstruction<V>, startx: Int, endx: Int, contained: Boolean) {
        var angle: Int // angle_t
        var index: Int
        var yl: Int // low
        var yh: Int // hight
        var mid: Int
        var pixlow: Int
        var pixhigh: Int
        val pixhighstep: Int
        val pixlowstep: Int
        var rw_scale: Int
        var topfrac: Int
        var bottomfrac: Int
        val bottomstep: Int
        // These are going to be modified A LOT, so we cache them here.
        pixhighstep = rsi.pixhighstep
        pixlowstep = rsi.pixlowstep
        bottomstep = rsi.bottomstep
        //  We must re-scale it.
        val rw_scalestep = rsi.rw_scalestep
        val topstep = rsi.topstep
        var texturecolumn = 0 // fixed_t
        val bias: Int
        // Well is entirely contained in our screen zone 
        // (or the very least it starts in it).
        if (contained) bias = 0 else bias = startx - rsi.rw_x
        // PROBLEM: these must be pre-biased when multithreading.
        rw_scale = rsi.rw_scale + bias * rw_scalestep
        topfrac = rsi.topfrac + bias * topstep
        bottomfrac = rsi.bottomfrac + bias * bottomstep
        pixlow = rsi.pixlow + bias * pixlowstep
        pixhigh = rsi.pixhigh + bias * pixhighstep
        run {
            for (rw_x in startx until endx) {
                // mark floor / ceiling areas
                yl = topfrac + RenderSegExecutor.HEIGHTUNIT - 1 shr RenderSegExecutor.HEIGHTBITS

                // no space above wall?
                if (yl < ceilingclip[rw_x] + 1) yl = ceilingclip[rw_x] + 1
                yh = bottomfrac shr RenderSegExecutor.HEIGHTBITS
                if (yh >= floorclip[rw_x]) yh = floorclip[rw_x] - 1

                //  System.out.printf("Thread: rw %d yl %d yh %d\n",rw_x,yl,yh);

                // A particular seg has been identified as a floor marker.


                // texturecolumn and lighting are independent of wall tiers
                if (rsi.segtextured) {
                    // calculate texture offset


                    // CAREFUL: a VERY anomalous point in the code. Their sum is supposed
                    // to give an angle not exceeding 45 degrees (or 0x0FFF after shifting).
                    // If added with pure unsigned rules, this doesn't hold anymore,
                    // not even if accounting for overflow.
                    angle = Tables.toBAMIndex(rsi.rw_centerangle + xtoviewangle[rw_x].toInt())
                    //angle = (int) (((rw_centerangle + xtoviewangle[rw_x])&BITS31)>>>ANGLETOFINESHIFT);
                    //angle&=0x1FFF;

                    // FIXME: We are accessing finetangent here, the code seems pretty confident
                    // in that angle won't exceed 4K no matter what. But xtoviewangle
                    // alone can yield 8K when shifted.
                    // This usually only overflows if we idclip and look at certain directions 
                    // (probably angles get fucked up), however it seems rare enough to just 
                    // "swallow" the exception. You can eliminate it by anding with 0x1FFF
                    // if you're so inclined. 
                    texturecolumn =
                        rsi.rw_offset - FixedMul(Tables.finetangent[angle], rsi.rw_distance)
                    texturecolumn = texturecolumn shr FRACBITS
                    // calculate lighting
                    index = rw_scale shr colormaps.lightScaleShift()
                    if (index >= colormaps.maxLightScale()) index = colormaps.maxLightScale() - 1
                    dcvars!!.dc_colormap = rsi.walllights[index]
                    dcvars!!.dc_x = rw_x
                    dcvars!!.dc_iscale = (0xffffffffL / rw_scale).toInt()
                }

                // draw the wall tiers
                if (rsi.midtexture != 0) {
                    // single sided line
                    dcvars!!.dc_yl = yl
                    dcvars!!.dc_yh = yh
                    dcvars!!.dc_texheight =
                        TexMan.getTextureheight(rsi.midtexture) shr FRACBITS // killough
                    dcvars!!.dc_texturemid = rsi.rw_midtexturemid
                    dcvars!!.dc_source = TexMan.GetCachedColumn(rsi.midtexture, texturecolumn)
                    dcvars!!.dc_source_ofs = 0
                    colfunc!!.invoke()
                    ceilingclip[rw_x] = rsi.viewheight.toShort()
                    floorclip[rw_x] = -1
                } else {
                    // two sided line
                    if (rsi.toptexture != 0) {
                        // top wall
                        mid = pixhigh shr RenderSegExecutor.HEIGHTBITS
                        pixhigh += pixhighstep
                        if (mid >= floorclip[rw_x]) mid = floorclip[rw_x] - 1
                        if (mid >= yl) {
                            dcvars!!.dc_yl = yl
                            dcvars!!.dc_yh = mid
                            dcvars!!.dc_texturemid = rsi.rw_toptexturemid
                            dcvars!!.dc_texheight =
                                TexMan.getTextureheight(rsi.toptexture) shr FRACBITS
                            dcvars!!.dc_source = TexMan.GetCachedColumn(rsi.toptexture, texturecolumn)
                            //dc_source_ofs=0;
                            colfunc!!.invoke()
                            ceilingclip[rw_x] = mid.toShort()
                        } else ceilingclip[rw_x] = (yl - 1).toShort()
                    } // if toptexture
                    else {
                        // no top wall
                        if (rsi.markceiling) ceilingclip[rw_x] = (yl - 1).toShort()
                    }
                    if (rsi.bottomtexture != 0) {
                        // bottom wall
                        mid =
                            pixlow + RenderSegExecutor.HEIGHTUNIT - 1 shr RenderSegExecutor.HEIGHTBITS
                        pixlow += pixlowstep

                        // no space above wall?
                        if (mid <= ceilingclip[rw_x]) mid = ceilingclip[rw_x] + 1
                        if (mid <= yh) {
                            dcvars!!.dc_yl = mid
                            dcvars!!.dc_yh = yh
                            dcvars!!.dc_texturemid = rsi.rw_bottomtexturemid
                            dcvars!!.dc_texheight =
                                TexMan.getTextureheight(rsi.bottomtexture) shr FRACBITS
                            dcvars!!.dc_source = TexMan.GetCachedColumn(rsi.bottomtexture, texturecolumn)
                            // dc_source_ofs=0;
                            colfunc!!.invoke()
                            floorclip[rw_x] = mid.toShort()
                        } else floorclip[rw_x] = (yh + 1).toShort()
                    } // end-bottomtexture
                    else {
                        // no bottom wall
                        if (rsi.markfloor) floorclip[rw_x] = (yh + 1).toShort()
                    }
                } // end-else (two-sided line)
                rw_scale += rw_scalestep
                topfrac += topstep
                bottomfrac += bottomstep
            } // end-rw 
        } // end-block
    }

    override fun setDetail(detailshift: Int) {
        colfunc = if (detailshift == 0) colfunchi else colfunclow
    }

    /** Only called once per screen width change  */
    fun setScreenRange(rwstart: Int, rwend: Int) {
        rw_end = rwend
        rw_start = rwstart
    }

    /** How many instructions TOTAL are there to wade through.
     * Not all will be executed on one thread, except in some rare
     * circumstances.
     *
     * @param rsiend
     */
    fun setRSIEnd(rsiend: Int) {
        this.rsiend = rsiend
    }

    override fun run() {
        var rsi: RenderSegInstruction<V>

        // Each worker blanks its own portion of the floor/ceiling clippers.
        System.arraycopy(BLANKFLOORCLIP, rw_start, floorclip, rw_start, rw_end - rw_start)
        System.arraycopy(BLANKCEILINGCLIP, rw_start, ceilingclip, rw_start, rw_end - rw_start)

        // For each "SegDraw" instruction...
        for (i in 0 until rsiend) {
            rsi = RSI[i]
            dcvars!!.centery = RSI[i].centery
            var startx: Int
            var endx: Int
            // Does a wall actually start in our screen zone?
            // If yes, we need no bias, since it was meant for it.
            // If the wall started BEFORE our zone, then we
            // will need to add a bias to it (see ProcessRSI).
            // If its entirely non-contained, ProcessRSI won't be
            // called anyway, so we don't need to check for the end.
            val contained = rsi.rw_x >= rw_start
            // Keep to your part of the screen. It's possible that several
            // threads will process the same RSI, but different parts of it.

            // Trim stuff that starts before our rw_start position.
            startx = Math.max(rsi.rw_x, rw_start)
            // Similarly, trim stuff after our rw_end position.
            endx = Math.min(rsi.rw_stopx, rw_end)
            // Is there anything to actually draw?
            if (endx - startx > 0) {
                ProcessRSI(rsi, startx, endx, contained)
            }
        } // end-instruction
        try {
            barrier.await()
        } catch (e: InterruptedException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    //protected abstract void ProcessRSI(RenderSegInstruction<V> rsi, int startx,int endx,boolean contained);
    ////////////////////////////VIDEO SCALE STUFF ////////////////////////////////
    fun updateRSI(rsi: Array<RenderSegInstruction<V>>) {
        RSI = rsi
    }

    class TrueColor(
        DOOM: DoomMain<ByteArray?, IntArray?>, id: Int,
        screen: IntArray?, texman: TextureManager<ByteArray?>,
        RSI: Array<RenderSegInstruction<IntArray?>>, BLANKCEILINGCLIP: ShortArray,
        BLANKFLOORCLIP: ShortArray, ceilingclip: ShortArray, floorclip: ShortArray,
        columnofs: IntArray, xtoviewangle: LongArray, ylookup: IntArray,
        visplanes: Array<visplane_t?>?, barrier: CyclicBarrier, colormaps: LightsAndColors<IntArray?>
    ) : RenderSegExecutor<ByteArray?, IntArray?>(
        DOOM, id, screen, texman, RSI, BLANKCEILINGCLIP,
        BLANKFLOORCLIP, ceilingclip, floorclip, columnofs, xtoviewangle,
        ylookup, visplanes, barrier, colormaps
    ) {
        init {
            dcvars = ColVars()
            colfunchi = R_DrawColumnBoomOpt.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                null
            )
            colfunc = colfunchi
            colfunclow = R_DrawColumnBoomOptLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                null
            )
        }
    }

    class HiColor(
        DOOM: DoomMain<ByteArray?, ShortArray?>, id: Int,
        screen: ShortArray?, texman: TextureManager<ByteArray?>,
        RSI: Array<RenderSegInstruction<ShortArray?>>, BLANKCEILINGCLIP: ShortArray,
        BLANKFLOORCLIP: ShortArray, ceilingclip: ShortArray, floorclip: ShortArray,
        columnofs: IntArray, xtoviewangle: LongArray, ylookup: IntArray,
        visplanes: Array<visplane_t?>?, barrier: CyclicBarrier, colormaps: LightsAndColors<ShortArray?>
    ) : RenderSegExecutor<ByteArray?, ShortArray?>(
        DOOM, id, screen, texman, RSI, BLANKCEILINGCLIP,
        BLANKFLOORCLIP, ceilingclip, floorclip, columnofs, xtoviewangle,
        ylookup, visplanes, barrier, colormaps
    ) {
        init {
            dcvars = ColVars()
            colfunchi = R_DrawColumnBoomOpt.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                null
            )
            colfunc = colfunchi
            colfunclow = R_DrawColumnBoomOptLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                null
            )
        }
    }

    class Indexed(
        DOOM: DoomMain<ByteArray?, ByteArray?>, id: Int,
        screen: ByteArray?, texman: TextureManager<ByteArray?>,
        RSI: Array<RenderSegInstruction<ByteArray?>>, BLANKCEILINGCLIP: ShortArray,
        BLANKFLOORCLIP: ShortArray, ceilingclip: ShortArray, floorclip: ShortArray,
        columnofs: IntArray, xtoviewangle: LongArray, ylookup: IntArray,
        visplanes: Array<visplane_t?>?, barrier: CyclicBarrier, colormaps: LightsAndColors<ByteArray?>
    ) : RenderSegExecutor<ByteArray?, ByteArray?>(
        DOOM, id, screen, texman, RSI, BLANKCEILINGCLIP,
        BLANKFLOORCLIP, ceilingclip, floorclip, columnofs, xtoviewangle,
        ylookup, visplanes, barrier, colormaps
    ) {
        init {
            dcvars = ColVars()
            colfunchi = R_DrawColumnBoomOpt.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                null
            )
            colfunc = colfunchi
            colfunclow = R_DrawColumnBoomOptLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                null
            )
        }
    }

    companion object {
        protected const val HEIGHTBITS = 12
        protected val HEIGHTUNIT = 1 shl RenderSegExecutor.HEIGHTBITS
    }
}