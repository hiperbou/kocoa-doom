package rr.parallel


import data.Limits
import doom.DoomMain
import doom.player_t
import rr.IDetailAware
import rr.drawfuns.*
import utils.GenericCopy
import java.io.IOException
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors

/** This is a second attempt at building a seg-focused parallel renderer, instead of
 * column-based. It does function, but is broken and has unsolved data dependencies.
 * It's therefore not used in official releases, and I chose to deprecate it.
 * If you still want to develop it, be my guest.
 *
 * @author velktron
 */
abstract class ParallelRenderer2<T, V>(
    DOOM: DoomMain<T, V>,
    wallthread: Int,
    floorthreads: Int,
    nummaskedthreads: Int
) : AbstractParallelRenderer<T, V>(DOOM, wallthread, floorthreads, nummaskedthreads) {
    init {
        println("Parallel Renderer 2 (Seg-based)")
        MySegs = ParallelSegs2(this)
        MyPlanes = ParallelPlanes(DOOM, this)
        MyThings = ParallelThings2(DOOM.vs, this)

        // TO BE LATE INIT? AFTER CONS?
        // Masked workers.
        //maskedworkers = arrayOfNulls<MaskedWorker<*, *>>(NUMMASKEDTHREADS)
        maskedworkers = arrayOfNulls<MaskedWorker<T, V>>(NUMMASKEDTHREADS) as Array<MaskedWorker<T, V>>
        (MyThings as ParallelThings2<T, V>).maskedworkers = maskedworkers
        InitMaskedWorkers() //TODO: this is bad
        (MySegs as ParallelSegs2<T, V>).RSI = GenericCopy.malloc({ RenderSegInstruction() },
            Limits.MAXSEGS * 3
        ) //TODO: weird
    }

    override fun InitParallelStuff() {
        // Prepare parallel stuff
        (MySegs as ParallelSegs2<T, V>).RSIExec = arrayOfNulls<RenderSegExecutor<ByteArray, V>>(NUMWALLTHREADS)
        tp = Executors.newFixedThreadPool(NUMWALLTHREADS + NUMFLOORTHREADS)
        // Prepare the barrier for MAXTHREADS + main thread.
        //wallbarrier=new CyclicBarrier(NUMWALLTHREADS+1);
        visplanebarrier = CyclicBarrier(NUMFLOORTHREADS + NUMWALLTHREADS + 1)
        vpw = arrayOfNulls<Runnable>(NUMFLOORTHREADS)

        // Uses "seg" parallel drawer, so RSI.
        InitRSISubsystem()
        maskedbarrier = CyclicBarrier(NUMMASKEDTHREADS + 1)

        // If using masked threads, set these too.
        TexMan.setSMPVars(NUMMASKEDTHREADS)
    }
    ///////////////////////// The actual rendering calls ///////////////////////
    /**
     * R_RenderView
     *
     * As you can guess, this renders the player view of a particular player object.
     * In practice, it could render the view of any mobj too, provided you adapt the
     * SetupFrame method (where the viewing variables are set).
     *
     */
    override fun RenderPlayerView(player: player_t) {
        // Viewing variables are set according to the player's mobj. Interesting hacks like
        // free cameras or monster views can be done.
        SetupFrame(player)

        /* Uncommenting this will result in a very existential experience
  if (Math.random()>0.999){
	  thinker_t shit=P.getRandomThinker();
	  try {
	  mobj_t crap=(mobj_t)shit;
	  player.mo=crap;
	  } catch (ClassCastException e){

	  }
  	}*/

        // Clear buffers. 
        MyBSP.ClearClipSegs()
        seg_vars.ClearDrawSegs()
        vp_vars.ClearPlanes()
        MySegs!!.ClearClips()
        VIS.ClearSprites()

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()

        // The head node is the last node output.
        MyBSP.RenderBSPNode(DOOM.levelLoader.numnodes - 1)

        // RenderRMIPipeline();
        /*
         * try { maskedbarrier.await(); } catch (Exception e) {
         * e.printStackTrace(); }
         */(MySegs as ParallelSegs2<T, V>).RenderRSIPipeline()
        // System.out.printf("Submitted %d RSIs\n",RSIcount);
        MySegs!!.CompleteRendering()

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()

        // "Warped floor" fixed, same-height visplane merging fixed.
        MyPlanes.DrawPlanes()
        try {
            visplanebarrier.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            e.printStackTrace()
        }

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()
        MySegs!!.sync()
        MyPlanes.sync()

//            drawsegsbarrier.await();
//            visplanebarrier.await();
        MyThings.DrawMasked()
    }

    protected abstract fun InitRSISubsystem()
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
    /**
     * R_Init
     */
    //public int  detailLevel;
    //public int  screenblocks=9; // has defa7ult
    protected abstract fun InitMaskedWorkers()
    class Indexed(DM: DoomMain<ByteArray?, ByteArray?>, wallthread: Int, floorthreads: Int, nummaskedthreads: Int) :
        ParallelRenderer2<ByteArray?, ByteArray?>(DM, wallthread, floorthreads, nummaskedthreads) {
        init {

            // Init light levels
            colormaps.scalelight = Array(colormaps.lightLevels()) { arrayOfNulls<ByteArray>(colormaps.maxLightScale()) }
            colormaps.scalelightfixed = arrayOfNulls<ByteArray>(colormaps.maxLightScale())
            colormaps.zlight = Array(colormaps.lightLevels()) { arrayOfNulls<ByteArray>(colormaps.maxLightZ()) }
            completeInit()
        }

        override fun InitRSISubsystem() {
            // int[] offsets = new int[NUMWALLTHREADS];
            val parallelSegs = MySegs as ParallelSegs2<ByteArray, ByteArray>
            for (i in 0 until NUMWALLTHREADS) {
                parallelSegs.RSIExec[i] = RenderSegExecutor.Indexed(
                    DOOM, i, screen, TexMan,
                    parallelSegs.RSI as Array<RenderSegInstruction<ByteArray?>>,
                    parallelSegs.getBLANKCEILINGCLIP(),
                    parallelSegs.getBLANKFLOORCLIP(),
                    parallelSegs.getCeilingClip(),
                    parallelSegs.getFloorClip(),
                    columnofs, _view.xtoviewangle,
                    ylookup, vp_vars.visplanes, visplanebarrier, colormaps
                ) as RenderSegExecutor<ByteArray, ByteArray>
                // SegExecutor sticks to its own half (or 1/nth) of the screen.
                parallelSegs.RSIExec[i]!!.setScreenRange(
                    i * (DOOM.vs.getScreenWidth() / NUMWALLTHREADS),
                    (i + 1) * (DOOM.vs.getScreenWidth() / NUMWALLTHREADS)
                )
                detailaware.add(parallelSegs.RSIExec[i] as IDetailAware)
            }
            for (i in 0 until NUMFLOORTHREADS) {
                val w: VisplaneWorker2<*, *> = VisplaneWorker2.Indexed(
                    DOOM, this, i, columnofs, ylookup, screen, visplanebarrier, NUMFLOORTHREADS
                )
                vpw[i] = w
                detailaware.add(w)
            }
        }

        override fun InitMaskedWorkers() {
            for (i in 0 until NUMMASKEDTHREADS) {
                maskedworkers[i] = MaskedWorker.Indexed(
                    DOOM.vs, this, i, ylookup, columnofs, NUMMASKEDTHREADS,
                    screen, maskedbarrier, BLURRY_MAP
                )
                detailaware.add(maskedworkers[i])
                // "Peg" to sprite manager.
                maskedworkers[i].cacheSpriteManager(DOOM.spriteManager)
            }
        }

        @Throws(IOException::class)
        override fun InitColormaps() {
            // Load in the light tables,
            // 256 byte align tables.
            colormaps.colormaps = DOOM.graphicSystem.getColorMap()
            // MAES: blurry effect is hardcoded to this colormap.
            BLURRY_MAP = DOOM.graphicSystem.getBlurryTable()
            // colormaps = (byte *)( ((int)colormaps + 255)&~0xff);     
        }

        override fun R_InitDrawingFunctions() {

            // Span functions. Common to all renderers unless overriden
            // or unused e.g. parallel renderers ignore them.
            DrawSpan = R_DrawSpanUnrolled.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dsvars,
                screen,
                DOOM.doomSystem
            )
            
            val k = R_DrawSpanLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dsvars,
                screen,
                DOOM.doomSystem
            )
            DrawSpanLow = R_DrawSpanLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dsvars,
                screen,
                DOOM.doomSystem
            )

            // Translated columns are usually sprites-only.
            DrawTranslatedColumn = R_DrawTranslatedColumn.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            DrawTranslatedColumnLow = R_DrawTranslatedColumnLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            //  DrawTLColumn=new R_DrawTLColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);

            // Fuzzy columns. These are also masked.
            DrawFuzzColumn = R_DrawFuzzColumn.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem,
                BLURRY_MAP
            )
            DrawFuzzColumnLow = R_DrawFuzzColumnLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem,
                BLURRY_MAP
            )

            // Regular draw for solid columns/walls. Full optimizations.
            DrawColumn = R_DrawColumnBoomOpt.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnLow = R_DrawColumnBoomOptLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                DOOM.doomSystem
            )

            // Non-optimized stuff for masked.
            DrawColumnMasked = R_DrawColumnBoom.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnMaskedLow = R_DrawColumnBoomLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )

            // Player uses masked
            DrawColumnPlayer = DrawColumnMasked // Player normally uses masked.

            // Skies use their own. This is done in order not to stomp parallel threads.
            DrawColumnSkies = R_DrawColumnBoomOpt.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                skydcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnSkiesLow = R_DrawColumnBoomOptLow.Indexed(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                skydcvars,
                screen,
                DOOM.doomSystem
            )
            super.R_InitDrawingFunctions()
        }
    }

    class HiColor(DM: DoomMain<ByteArray?, ShortArray?>, wallthread: Int, floorthreads: Int, nummaskedthreads: Int) :
        ParallelRenderer2<ByteArray?, ShortArray?>(DM, wallthread, floorthreads, nummaskedthreads) {
        init {

            // Init light levels
            colormaps.scalelight =
                Array(colormaps.lightLevels()) { arrayOfNulls<ShortArray>(colormaps.maxLightScale()) }
            colormaps.scalelightfixed = arrayOfNulls<ShortArray>(colormaps.maxLightScale())
            colormaps.zlight = Array(colormaps.lightLevels()) { arrayOfNulls<ShortArray>(colormaps.maxLightZ()) }
            completeInit()
        }

        override fun InitRSISubsystem() {
            // int[] offsets = new int[NUMWALLTHREADS];
            val parallelSegs = MySegs as ParallelSegs2<ByteArray, ShortArray>
            for (i in 0 until NUMWALLTHREADS) {
                parallelSegs.RSIExec[i] = RenderSegExecutor.HiColor(
                    DOOM, i, screen, TexMan,
                    parallelSegs.RSI as Array<RenderSegInstruction<ShortArray?>>,
                    parallelSegs.getBLANKCEILINGCLIP(),
                    parallelSegs.getBLANKFLOORCLIP(),
                    parallelSegs.getCeilingClip(),
                    parallelSegs.getFloorClip(), columnofs, _view.xtoviewangle,
                    ylookup, vp_vars.visplanes, visplanebarrier, colormaps
                ) as RenderSegExecutor<ByteArray, ShortArray>
                // SegExecutor sticks to its own half (or 1/nth) of the screen.
                parallelSegs.RSIExec[i]!!.setScreenRange(
                    i * (DOOM.vs.getScreenWidth() / NUMWALLTHREADS),
                    (i + 1) * (DOOM.vs.getScreenWidth() / NUMWALLTHREADS)
                )
                detailaware.add(parallelSegs.RSIExec[i] as IDetailAware)
            }
            for (i in 0 until NUMFLOORTHREADS) {
                val w: VisplaneWorker2<*, *> = VisplaneWorker2.HiColor(
                    DOOM, this, i, columnofs, ylookup, screen, visplanebarrier, NUMFLOORTHREADS
                )
                vpw[i] = w
                detailaware.add(w)
            }
        }

        override fun InitMaskedWorkers() {
            for (i in 0 until NUMMASKEDTHREADS) {
                maskedworkers[i] = MaskedWorker.HiColor(
                    DOOM.vs, this, i, ylookup, columnofs, NUMMASKEDTHREADS,
                    screen, maskedbarrier, BLURRY_MAP
                )
                detailaware.add(maskedworkers[i])
                // "Peg" to sprite manager.
                maskedworkers[i].cacheSpriteManager(DOOM.spriteManager)
            }
        }

        @Throws(IOException::class)
        override fun InitColormaps() {
            colormaps.colormaps = DOOM.graphicSystem.getColorMap()
            println("COLORS15 Colormaps: " + colormaps.colormaps.size)

            // MAES: blurry effect is hardcoded to this colormap.
            // Pointless, since we don't use indexes. Instead, a half-brite
            // processing works just fine.
            BLURRY_MAP = DOOM.graphicSystem.getBlurryTable()
        }

        override fun R_InitDrawingFunctions() {

            // Span functions. Common to all renderers unless overriden
            // or unused e.g. parallel renderers ignore them.
            DrawSpan = R_DrawSpanUnrolled.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dsvars,
                screen,
                DOOM.doomSystem
            )
            DrawSpanLow = R_DrawSpanLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dsvars,
                screen,
                DOOM.doomSystem
            )

            // Translated columns are usually sprites-only.
            DrawTranslatedColumn = R_DrawTranslatedColumn.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            DrawTranslatedColumnLow = R_DrawTranslatedColumnLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            DrawTLColumn = R_DrawTLColumn(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )

            // Fuzzy columns. These are also masked.
            DrawFuzzColumn = R_DrawFuzzColumn.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem,
                BLURRY_MAP
            )
            DrawFuzzColumnLow = R_DrawFuzzColumnLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem,
                BLURRY_MAP
            )

            // Regular draw for solid columns/walls. Full optimizations.
            DrawColumn = R_DrawColumnBoomOpt.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnLow = R_DrawColumnBoomOptLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                DOOM.doomSystem
            )

            // Non-optimized stuff for masked.
            DrawColumnMasked = R_DrawColumnBoom.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnMaskedLow = R_DrawColumnBoomLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )

            // Player uses masked
            DrawColumnPlayer = DrawColumnMasked // Player normally uses masked.

            // Skies use their own. This is done in order not to stomp parallel threads.
            DrawColumnSkies = R_DrawColumnBoomOpt.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                skydcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnSkiesLow = R_DrawColumnBoomOptLow.HiColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                skydcvars,
                screen,
                DOOM.doomSystem
            )
            super.R_InitDrawingFunctions()
        }
    }

    class TrueColor(DM: DoomMain<ByteArray?, IntArray?>, wallthread: Int, floorthreads: Int, nummaskedthreads: Int) :
        ParallelRenderer2<ByteArray?, IntArray?>(DM, wallthread, floorthreads, nummaskedthreads) {
        init {

            // Init light levels
            colormaps.scalelight = Array(colormaps.lightLevels()) { arrayOfNulls<IntArray>(colormaps.maxLightScale()) }
            colormaps.scalelightfixed = arrayOfNulls<IntArray>(colormaps.maxLightScale())
            colormaps.zlight = Array(colormaps.lightLevels()) { arrayOfNulls<IntArray>(colormaps.maxLightZ()) }
            completeInit()
        }

        override fun InitRSISubsystem() {
            // int[] offsets = new int[NUMWALLTHREADS];
            val parallelSegs = MySegs as ParallelSegs2<ByteArray, IntArray>
            for (i in 0 until NUMWALLTHREADS) {
                parallelSegs.RSIExec[i] = RenderSegExecutor.TrueColor(
                    DOOM, i, screen, TexMan,
                    parallelSegs.RSI as Array<RenderSegInstruction<IntArray?>>,
                    parallelSegs.getBLANKCEILINGCLIP(),
                    parallelSegs.getBLANKFLOORCLIP(),
                    parallelSegs.getCeilingClip(),
                    parallelSegs.getFloorClip(),
                    columnofs, _view.xtoviewangle,
                    ylookup, vp_vars.visplanes, visplanebarrier, colormaps
                ) as RenderSegExecutor<ByteArray, IntArray>
                // SegExecutor sticks to its own half (or 1/nth) of the screen.
                parallelSegs.RSIExec[i]!!.setScreenRange(
                    i * (DOOM.vs.getScreenWidth() / NUMWALLTHREADS),
                    (i + 1) * (DOOM.vs.getScreenWidth() / NUMWALLTHREADS)
                )
                detailaware.add(parallelSegs.RSIExec[i] as IDetailAware)
            }
            for (i in 0 until NUMFLOORTHREADS) {
                val w: VisplaneWorker2<*, *> = VisplaneWorker2.TrueColor(
                    DOOM, this, i, columnofs, ylookup, screen, visplanebarrier, NUMFLOORTHREADS
                )
                vpw[i] = w
                detailaware.add(w)
            }
        }

        override fun InitMaskedWorkers() {
            for (i in 0 until NUMMASKEDTHREADS) {
                maskedworkers[i] = MaskedWorker.TrueColor(
                    DOOM.vs, this, i, ylookup, columnofs, NUMMASKEDTHREADS, screen,
                    maskedbarrier, BLURRY_MAP
                )
                detailaware.add(maskedworkers[i])
                // "Peg" to sprite manager.
                maskedworkers[i].cacheSpriteManager(DOOM.spriteManager)
            }
        }

        @Throws(IOException::class)
        override fun InitColormaps() {
            colormaps.colormaps = DOOM.graphicSystem.getColorMap()
            println("COLORS15 Colormaps: " + colormaps.colormaps.size)

            // MAES: blurry effect is hardcoded to this colormap.
            // Pointless, since we don't use indexes. Instead, a half-brite
            // processing works just fine.
            BLURRY_MAP = DOOM.graphicSystem.getBlurryTable()
        }

        override fun R_InitDrawingFunctions() {

            // Span functions. Common to all renderers unless overriden
            // or unused e.g. parallel renderers ignore them.
            DrawSpan = R_DrawSpanUnrolled.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dsvars,
                screen,
                DOOM.doomSystem
            )
            DrawSpanLow = R_DrawSpanLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dsvars,
                screen,
                DOOM.doomSystem
            )

            // Translated columns are usually sprites-only.
            DrawTranslatedColumn = R_DrawTranslatedColumn.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            DrawTranslatedColumnLow = R_DrawTranslatedColumnLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            //DrawTLColumn=new R_DrawTLColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);

            // Fuzzy columns. These are also masked.
            DrawFuzzColumn = R_DrawFuzzColumn.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem,
                BLURRY_MAP
            )
            DrawFuzzColumnLow = R_DrawFuzzColumnLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem,
                BLURRY_MAP
            )

            // Regular draw for solid columns/walls. Full optimizations.
            DrawColumn = R_DrawColumnBoomOpt.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnLow = R_DrawColumnBoomOptLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                dcvars,
                screen,
                DOOM.doomSystem
            )

            // Non-optimized stuff for masked.
            DrawColumnMasked = R_DrawColumnBoom.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnMaskedLow = R_DrawColumnBoomLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                DOOM.doomSystem
            )

            // Player uses masked
            DrawColumnPlayer = DrawColumnMasked // Player normally uses masked.

            // Skies use their own. This is done in order not to stomp parallel threads.
            DrawColumnSkies = R_DrawColumnBoomOpt.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                skydcvars,
                screen,
                DOOM.doomSystem
            )
            DrawColumnSkiesLow = R_DrawColumnBoomOptLow.TrueColor(
                DOOM.vs.getScreenWidth(),
                DOOM.vs.getScreenHeight(),
                ylookup,
                columnofs,
                skydcvars,
                screen,
                DOOM.doomSystem
            )
            super.R_InitDrawingFunctions()
        }
    }
}