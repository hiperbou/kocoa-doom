package rr.parallel


import data.Tables
import doom.DoomMain
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FixedMul
import rr.PlaneDrawer
import rr.RendererState
import rr.SceneRenderer
import rr.drawfuns.ColVars
import rr.parallel.RWI.Get
import utils.C2JUtils
import java.io.IOException
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Features and functionality which is common among parallel renderers
 *
 * @author velktron
 */
abstract class AbstractParallelRenderer<T, V> : RendererState<T, V>, RWI.Init<T, V> {
    constructor(DM: DoomMain<T, V>, wallthread: Int, floorthreads: Int, nummaskedthreads: Int) : super(DM) {
        NUMWALLTHREADS = wallthread
        NUMFLOORTHREADS = floorthreads
        NUMMASKEDTHREADS = nummaskedthreads
        // Prepare the barriers for MAXTHREADS + main thread.
        drawsegsbarrier = CyclicBarrier(NUMWALLTHREADS + 1)
        visplanebarrier = CyclicBarrier(NUMFLOORTHREADS + 1)
        maskedbarrier = CyclicBarrier(NUMMASKEDTHREADS + 1)
        tp = Executors.newCachedThreadPool()
    }

    constructor(
        DM: DoomMain<T, V>, wallthread: Int,
        floorthreads: Int
    ) : super(DM) {
        NUMWALLTHREADS = wallthread
        NUMFLOORTHREADS = floorthreads
        NUMMASKEDTHREADS = 1
        // Prepare the barriers for MAXTHREADS + main thread.
        drawsegsbarrier = CyclicBarrier(NUMWALLTHREADS + 1)
        visplanebarrier = CyclicBarrier(NUMFLOORTHREADS + 1)
        maskedbarrier = CyclicBarrier(NUMMASKEDTHREADS + 1)
        tp = Executors.newCachedThreadPool()
    }

    // //////// PARALLEL OBJECTS /////////////
    protected val NUMWALLTHREADS: Int
    protected val NUMMASKEDTHREADS: Int
    protected val NUMFLOORTHREADS: Int
    protected var tp: Executor
    protected lateinit var vpw: Array<Runnable?>
    protected lateinit var maskedworkers: Array<MaskedWorker<T, V>>
    protected var drawsegsbarrier: CyclicBarrier
    protected var visplanebarrier: CyclicBarrier
    protected var maskedbarrier: CyclicBarrier

    protected inner class ParallelSegs internal constructor(R: SceneRenderer<*, *>) : SegDrawer(R), Get<T, V> {
        /**
         * Parallel version. Since there's so much crap to take into account
         * when rendering, the number of walls to render is unknown a-priori and
         * the BSP trasversal itself is not worth parallelizing, it makes more
         * sense to store "rendering instructions" as quickly as the BSP can be
         * transversed, and then execute those in parallel. Also saves on having
         * to duplicate way too much status.
         */
        override fun CompleteColumn() {

            // Don't wait to go over
            if (RWIcount >= _RWI.size) {
                ResizeRWIBuffer()
            }

            // A deep copy is still necessary, as dc
            _RWI[RWIcount].copyFrom(dcvars!!)

            // We only need to point to the next one in the list.
            RWIcount++
        }

        /**
         * Starts the RenderWallExecutors. Sync is EXTERNAL, however.
         */
        override fun CompleteRendering() {
            for (i in 0 until NUMWALLTHREADS) {
                RWIExec[i].setRange(
                    i * RWIcount / NUMWALLTHREADS,
                    (i + 1) * RWIcount / NUMWALLTHREADS
                )
                // RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
                tp.execute(RWIExec[i])
            }

            // System.out.println("RWI count"+RWIcount);
            RWIcount = 0
        }

        /*
         * Just what are "RWIs"? Stored wall rendering instructions. They can be
         * at most 3*SCREENWIDTH (if there are low, mid and high textures on
         * every column of the screen) Remember to init them and set screen and
         * ylookup for all of them. Their max number is static and work
         * partitioning can be done in any way, as long as you keep track of how
         * many there are in any given frame. This value is stored inside
         * RWIcount. TODO: there are cases when more than 3*SCREENWIDTH
         * instructions need to be stored. therefore we really need a resizeable
         * array here, but ArrayList is way too slow for our needs. Storing a
         * whole wall is not an option, as, once again, a wall may have a
         * variable number of columns and an irregular height profile -> we'd
         * need to look into visplanes ugh...
         */
        lateinit var RWIExec: Array<RenderWallExecutor<T, V>>

        /** Array of "wall" (actually, column) instructions  */
        var _RWI: Array<ColVars<T, V>>

        /**
         * Increment this as you submit RWIs to the "queue". Remember to reset
         * to 0 when you have drawn everything!
         */
        var RWIcount = 0

        init {
            val fake = ColVars<T, V>()
            _RWI = C2JUtils.createArrayOfObjects(fake, 3 * DOOM.vs.getScreenWidth())
        }

        /**
         * Resizes RWI buffer, updates executors. Sorry for the hackish
         * implementation but ArrayList and pretty much everything in
         * Collections is way too slow for what we're trying to accomplish.
         */
        fun ResizeRWIBuffer() {
            val fake = ColVars<T, V>()

            // Bye bye, old RWI.
            _RWI = C2JUtils.resize(fake, _RWI, _RWI.size * 2)
            for (i in 0 until NUMWALLTHREADS) {
                RWIExec[i].updateRWI(_RWI)
            }
            // System.err.println("RWI Buffer resized. Actual capacity " +
            // RWI.length);
        }

        override fun getRWI(): Array<ColVars<T, V>> {
            return _RWI
        }

        override fun setExecutors(RWIExec: Array<RenderWallExecutor<T, V>>) {
            this.RWIExec = RWIExec
        }

        override fun sync() {
            try {
                drawsegsbarrier.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: BrokenBarrierException) {
                e.printStackTrace()
            }
        }
    }

    protected inner class ParallelPlanes<T, V>(DOOM: DoomMain<T, V>, R: SceneRenderer<T, V>) :
        PlaneDrawer<T, V>(DOOM, R) {
        /**
         * R_DrawPlanes At the end of each frame. This also means that visplanes
         * must have been set BEFORE we called this function. Therefore, look
         * for errors behind.
         *
         * @throws IOException
         */
        override fun DrawPlanes() {
            if (RANGECHECK) {
                rangeCheckErrors()
            }

            // vpw[0].setRange(0,lastvisplane/2);
            // vpw[1].setRange(lastvisplane/2,lastvisplane);
            for (i in 0 until NUMFLOORTHREADS) tp.execute(vpw[i])
        }
    } // End Plane class

    protected inner class ParallelSegs2<T, V> internal constructor(val APR: AbstractParallelRenderer<T, V>) : SegDrawer(
        APR
    ) {
        /**
         * RenderSeg subsystem. Similar concept to RWI, but stores
         * "Render Seg Instructions" instead. More complex to build, but
         * potentially faster in some situations, as it allows distributing load
         * per-wall, rather than per-screen portion. Requires careful
         * concurrency considerations.
         */
        lateinit var RSI: Array<RenderSegInstruction<V>>

        /**
         * Increment this as you submit RSIs to the "queue". Remember to reset
         * to 0 when you have drawn everything!
         */
        var RSIcount = 0
        lateinit var RSIExec: Array<RenderSegExecutor<ByteArray, V>?>
        override fun RenderSegLoop() {
            var angle: Int
            var yl: Int
            var top: Int
            var bottom: Int
            var yh: Int
            var mid: Int
            var texturecolumn = 0

            // Generate Seg rendering instruction BEFORE the looping start
            // and anything is modified. The loop will be repeated in the
            // threads, but without marking ceilings/floors etc.
            GenerateRSI()
            while (rw_x < rw_stopx) {

                // mark floor / ceiling areas
                yl = topfrac + HEIGHTUNIT - 1 shr HEIGHTBITS

                // no space above wall?
                if (yl < ceilingclip[rw_x] + 1) yl = ceilingclip[rw_x] + 1
                if (markceiling) {
                    top = ceilingclip[rw_x] + 1
                    bottom = yl - 1
                    if (bottom >= floorclip[rw_x]) bottom = floorclip[rw_x] - 1
                    if (top <= bottom) {
                        vp_vars.visplanes[vp_vars.ceilingplane]!!.setTop(rw_x, top.toChar())
                        vp_vars.visplanes[vp_vars.ceilingplane]!!.setBottom(rw_x, bottom.toChar())
                    }
                }
                yh = bottomfrac shr HEIGHTBITS
                if (yh >= floorclip[rw_x]) yh = floorclip[rw_x] - 1

                // System.out.printf("Precompute: rw %d yl %d yh %d\n",rw_x,yl,yh);

                // A particular seg has been identified as a floor marker.
                if (markfloor) {
                    top = yh + 1
                    bottom = floorclip[rw_x] - 1
                    if (top <= ceilingclip[rw_x]) top = ceilingclip[rw_x] + 1
                    if (top <= bottom) {
                        vp_vars.visplanes[vp_vars.floorplane]!!.setTop(rw_x, top.toChar())
                        vp_vars.visplanes[vp_vars.floorplane]!!.setBottom(rw_x, bottom.toChar())
                    }
                }

                // texturecolumn and lighting are independent of wall tiers
                if (segtextured) {
                    // calculate texture offset. Still important to do because
                    // of masked
                    angle = Tables.toBAMIndex(
                        rw_centerangle
                                + APR._view.xtoviewangle[rw_x].toInt()
                    )
                    texturecolumn = rw_offset - FixedMul(Tables.finetangent[angle], rw_distance)
                    texturecolumn = texturecolumn shr FRACBITS
                }

                // Don't to any drawing, only compute bounds.
                if (midtexture != 0) {
                    APR.dcvars!!.dc_source = APR.TexMan.GetCachedColumn(midtexture, texturecolumn)
                    // dc_m=dcvars.dc_source_ofs;
                    // single sided line
                    ceilingclip[rw_x] = APR._view.height.toShort()
                    floorclip[rw_x] = -1
                } else {
                    // two sided line
                    if (toptexture != 0) {
                        // top wall
                        mid = pixhigh shr HEIGHTBITS
                        pixhigh += pixhighstep
                        if (mid >= floorclip[rw_x]) mid = floorclip[rw_x] - 1
                        if (mid >= yl) {
                            APR.dcvars!!.dc_source = APR.TexMan.GetCachedColumn(toptexture, texturecolumn)
                            ceilingclip[rw_x] = mid.toShort()
                        } else ceilingclip[rw_x] = (yl - 1).toShort()
                    } else {
                        // no top wall
                        if (markceiling) ceilingclip[rw_x] = (yl - 1).toShort()
                    }
                    if (bottomtexture != 0) {
                        // bottom wall
                        mid = pixlow + HEIGHTUNIT - 1 shr HEIGHTBITS
                        pixlow += pixlowstep

                        // no space above wall?
                        if (mid <= ceilingclip[rw_x]) mid = ceilingclip[rw_x] + 1
                        if (mid <= yh) {
                            APR.dcvars!!.dc_source = APR.TexMan.GetCachedColumn(bottomtexture, texturecolumn)
                            floorclip[rw_x] = mid.toShort()
                        } else floorclip[rw_x] = (yh + 1).toShort()
                    } else {
                        // no bottom wall
                        if (markfloor) floorclip[rw_x] = (yh + 1).toShort()
                    }
                    if (maskedtexture) {
                        // save texturecol
                        // for backdrawing of masked mid texture
                        seg_vars.maskedtexturecol[seg_vars.pmaskedtexturecol
                                + rw_x] = texturecolumn.toShort()
                    }
                }
                rw_scale += rw_scalestep
                topfrac += topstep
                bottomfrac += bottomstep
                rw_x++
            }
        }

        fun GenerateRSI() {
            if (RSIcount >= RSI.size) {
                ResizeRSIBuffer()
            }
            val rsi = RSI[RSIcount]
            rsi.centery = APR._view.centery
            rsi.bottomfrac = bottomfrac
            rsi.bottomstep = bottomstep
            rsi.bottomtexture = bottomtexture
            rsi.markceiling = markceiling
            rsi.markfloor = markfloor
            rsi.midtexture = midtexture
            rsi.pixhigh = pixhigh
            rsi.pixhighstep = pixhighstep
            rsi.pixlow = pixlow
            rsi.pixlowstep = pixlowstep
            rsi.rw_bottomtexturemid = rw_bottomtexturemid
            rsi.rw_centerangle = rw_centerangle
            rsi.rw_distance = rw_distance
            rsi.rw_midtexturemid = rw_midtexturemid
            rsi.rw_offset = rw_offset
            rsi.rw_scale = rw_scale
            rsi.rw_scalestep = rw_scalestep
            rsi.rw_stopx = rw_stopx
            rsi.rw_toptexturemid = rw_toptexturemid
            rsi.rw_x = rw_x
            rsi.segtextured = segtextured
            rsi.topfrac = topfrac
            rsi.topstep = topstep
            rsi.toptexture = toptexture
            rsi.walllights = APR.colormaps.walllights
            rsi.viewheight = APR._view.height
            // rsi.floorplane=floorplane;
            // rsi.ceilingplane=ceilingplane;
            RSIcount++
        }

        override fun CompleteColumn() { }

        fun RenderRSIPipeline() {
            for (i in 0 until APR.NUMWALLTHREADS) {
                RSIExec[i]!!.setRSIEnd(RSIcount)
                // RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
                APR.tp.execute(RSIExec[i])
            }

            // System.out.println("RWI count"+RWIcount);
            RSIcount = 0
        }

        /**
         * Resizes RWI buffer, updates executors. Sorry for the hackish
         * implementation but ArrayList and pretty much everything in
         * Collections is way too slow for what we're trying to accomplish.
         */
        fun ResizeRSIBuffer() {
            val fake = RenderSegInstruction<V>()
            // Bye bye, old RSI.
            RSI = C2JUtils.resize(fake, RSI, RSI.size * 2)
            for (i in 0 until APR.NUMWALLTHREADS) {
                RSIExec[i]!!.updateRSI(RSI)
            }
            println("RWI Buffer resized. Actual capacity " + RSI.size)
        }
    }

    protected inner class ParallelPlanes2<T, V> protected constructor(DOOM: DoomMain<T, V>, R: SceneRenderer<T, V>) :
        PlaneDrawer<T, V>(DOOM, R) {
        /**
         * R_DrawPlanes At the end of each frame. This also means that visplanes
         * must have been set BEFORE we called this function. Therefore, look
         * for errors behind.
         *
         * @throws IOException
         */
        override fun DrawPlanes() {
            if (RANGECHECK) {
                rangeCheckErrors()
            }

            // vpw[0].setRange(0,lastvisplane/2);
            // vpw[1].setRange(lastvisplane/2,lastvisplane);
            for (i in 0 until NUMFLOORTHREADS) {
                tp.execute(vpw[i])
            }
        }
    } // End Plane class
    // / RWI SUBSYSTEM
    // / AKA "Render Wall Instruction": instructions that store only a single
    // column
    // from a wall
    /**
     * R_InitRSISubsystem Initialize RSIs and RSI Executors. Pegs them to the
     * RSI, ylookup and screen[0].
     */
    // protected abstract void InitRSISubsystem();
    /*
     * { // CATCH: this must be executed AFTER screen is set, and // AFTER we
     * initialize the RWI themselves, // before V is set (right?) //offsets=new
     * int[NUMWALLTHREADS]; for (int i=0;i<NUMWALLTHREADS;i++){ RSIExec[i]=new
     * RenderSegExecutor.HiColor( SCREENWIDTH, SCREENHEIGHT, i, screen, this,
     * TexMan, RSI, MySegs.getBLANKCEILINGCLIP(), MySegs.getBLANKFLOORCLIP(),
     * MySegs.getCeilingClip(), MySegs.getFloorClip(), columnofs, xtoviewangle,
     * ylookup, this.visplanes, this.visplanebarrier);
     * RSIExec[i].setVideoScale(this.vs); RSIExec[i].initScaling(); // Each
     * SegExecutor sticks to its own half (or 1/nth) of the screen.
     * RSIExec[i].setScreenRange
     * (i*(SCREENWIDTH/NUMWALLTHREADS),(i+1)*(SCREENWIDTH/NUMWALLTHREADS));
     * detailaware.add(RSIExec[i]); } for (int i=0;i<NUMFLOORTHREADS;i++){
     * vpw[i]=new VisplaneWorker(i,SCREENWIDTH,SCREENHEIGHT,columnofs,ylookup,
     * screen,visplanebarrier,NUMFLOORTHREADS); detailaware.add((IDetailAware)
     * vpw[i]); } }
     */
    /** Creates RMI Executors  */ // protected abstract void InitRMISubsystem();
    /*
     * { for (int i = 0; i < NUMMASKEDTHREADS; i++) { // Each masked executor
     * gets its own set of column functions. RMIExec[i] = new
     * RenderMaskedExecutor(SCREENWIDTH, SCREENHEIGHT, columnofs, ylookup,
     * screen, RMI, maskedbarrier, // Regular masked columns new
     * R_DrawColumnBoom
     * (SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I), new
     * R_DrawColumnBoomLow
     * (SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I), //
     * Fuzzy columns new
     * R_DrawFuzzColumn.HiColor(SCREENWIDTH,SCREENHEIGHT,ylookup
     * ,columnofs,maskedcvars,screen,I), new
     * R_DrawFuzzColumnLow.HiColor(SCREENWIDTH
     * ,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I), // Translated
     * columns new
     * R_DrawTranslatedColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs
     * ,maskedcvars,screen,I), new
     * R_DrawTranslatedColumnLow(SCREENWIDTH,SCREENHEIGHT
     * ,ylookup,columnofs,maskedcvars,screen,I) ); detailaware.add(RMIExec[i]);
     * } }
     */
    override fun Init() {
        super.Init()
        InitParallelStuff()
    }

    /**
     * Any scaling and implementation-specific stuff to do for parallel stuff
     * should go here. This method is called internally by the public Init().
     * The idea is that the renderer should be up & running after you finally
     * called this.
     */
    protected abstract fun InitParallelStuff()

    /** Override this in one of the final implementors, if you want it to work  */
    override fun InitRWIExecutors(num: Int, RWI: Array<ColVars<T, V>?>?): Array<RenderWallExecutor<T, V>?>? {
        return null
    }

    var RWIs: Get<T, V>? = null

    companion object {
        protected const val DEBUG = false
    }
}