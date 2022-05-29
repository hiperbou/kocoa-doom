package rr.parallel

import doom.DoomMain
import doom.player_t
import rr.SimpleThings
import rr.drawfuns.*
import java.io.IOException

/**
 * This is Mocha Doom's famous parallel software renderer. It builds on the
 * basic software renderer, but adds specialized handling for drawing segs
 * (walls) and spans (floors) in parallel. There's inherent parallelism between
 * walls and floor, and internal parallelism between walls and between floors.
 * However, visplane limits and openings need to be pre-computed before any
 * actual drawing starts, that's why rendering of walls is stored in "RWI"s or
 * "Render Wall Instructions", and then rendered once they are all in place and
 * the can be parallelized between rendering threads. Rendering of sprites is
 * NOT parallelized yet (and probably not worth it, at this point).
 *
 * @author admin
 */
abstract class ParallelRenderer<T, V> @JvmOverloads constructor(
    DM: DoomMain<T, V>, wallthread: Int = 1,
    floorthreads: Int = 1, nummaskedthreads: Int = 2
) : AbstractParallelRenderer<T, V>(DM, wallthread, floorthreads, nummaskedthreads) {
    /**
     * Default constructor, 1 seg, 1 span and two masked threads.
     *
     * @param DM
     */
    init {

        // Register parallel seg drawer with list of RWI subsystems.
        val tmp = ParallelSegs(this)
        MySegs = tmp
        RWIs = tmp
        MyThings = SimpleThings(DM.vs, this)
        //this.MyPlanes = new Planes(this);// new ParallelPlanes<T, V>(DM.R);
    }

    /**
     * R_RenderView As you can guess, this renders the player view of a
     * particular player object. In practice, it could render the view of any
     * mobj too, provided you adapt the SetupFrame method (where the viewing
     * variables are set).
     *
     * @throws IOException
     */
    override fun RenderPlayerView(player: player_t) {

        // Viewing variables are set according to the player's mobj. Interesting
        // hacks like
        // free cameras or monster views can be done.
        SetupFrame(player)

        /*
         * Uncommenting this will result in a very existential experience if
         * (Math.random()>0.999){ thinker_t shit=P.getRandomThinker(); try {
         * mobj_t crap=(mobj_t)shit; player.mo=crap; } catch (ClassCastException
         * e){ } }
         */

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

        // System.out.printf("Submitted %d RWIs\n",RWIcount);
        MySegs!!.CompleteRendering()

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()

        // "Warped floor" fixed, same-height visplane merging fixed.
        MyPlanes.DrawPlanes()

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()
        MySegs!!.sync()
        MyPlanes.sync()

//            drawsegsbarrier.await();
//            visplanebarrier.await();
        MyThings.DrawMasked()

        // RenderRMIPipeline();
        /*
         * try { maskedbarrier.await(); } catch (Exception e) {
         * e.printStackTrace(); }
         */

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()
    }

    class Indexed(
        DM: DoomMain<ByteArray?, ByteArray?>, wallthread: Int,
        floorthreads: Int, nummaskedthreads: Int
    ) : ParallelRenderer<ByteArray?, ByteArray?>(DM, wallthread, floorthreads, nummaskedthreads) {
        init {

            // Init light levels
            colormaps.scalelight = Array(colormaps.lightLevels()) { arrayOfNulls<ByteArray>(colormaps.maxLightScale()) }
            colormaps.scalelightfixed = arrayOfNulls<ByteArray>(colormaps.maxLightScale())
            colormaps.zlight = Array(colormaps.lightLevels()) { arrayOfNulls<ByteArray>(colormaps.maxLightZ()) }
            completeInit()
        }

        /**
         * R_InitColormaps
         *
         * @throws IOException
         */
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

        override fun completeInit() {
            super.completeInit()
            InitMaskedWorkers()
        }

        override fun InitMaskedWorkers() {
            //TODO: arrayOfNulls<MaskedWorker<*, *>>(NUMMASKEDTHREADS)
            maskedworkers = arrayOfNulls<MaskedWorker<ByteArray?, ByteArray?>>(NUMMASKEDTHREADS) as Array<MaskedWorker<ByteArray?, ByteArray?>>
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

        override fun InitRWIExecutors(
            num: Int, RWI: Array<ColVars<ByteArray?, ByteArray?>?>?
        ): Array<RenderWallExecutor<ByteArray?, ByteArray?>?> {
            val tmp = arrayOfNulls<RenderWallExecutor.Indexed>(num)
            for (i in 0 until num) {
                tmp[i] = RenderWallExecutor.Indexed(
                    DOOM.vs.getScreenWidth(),
                    DOOM.vs.getScreenHeight(),
                    columnofs,
                    ylookup,
                    screen,
                    RWI as Array<ColVars<ByteArray?, ByteArray?>>,
                    drawsegsbarrier
                )
            }
            return tmp as Array<RenderWallExecutor<ByteArray?, ByteArray?>?>
        }
    }

    override fun InitParallelStuff() {

        // ...yeah, it works.
        if (RWIs != null) {
            val RWI = RWIs!!.getRWI()
            val RWIExec = InitRWIExecutors(NUMWALLTHREADS, RWI as Array<ColVars<T, V>?>)!!
            RWIs!!.setExecutors(RWIExec as Array<RenderWallExecutor<T, V>>)
            for (i in 0 until NUMWALLTHREADS) {
                detailaware.add(RWIExec[i])
            }
        }

        // CATCH: this must be executed AFTER screen is set, and
        // AFTER we initialize the RWI themselves,
        // before V is set (right?)
        // This actually only creates the necessary arrays and
        // barriers. Things aren't "wired" yet.
        // Using "render wall instruction" subsystem
        // Using masked sprites
        // RMIExec = new RenderMaskedExecutor[NUMMASKEDTHREADS];
        // Using
        //vpw = new Runnable[NUMFLOORTHREADS];
        //maskedworkers = new MaskedWorker.Indexed[NUMMASKEDTHREADS];
        // RWIcount = 0;
        // InitRWISubsystem();
        // InitRMISubsystem();
        // InitPlaneWorkers();
        // InitMaskedWorkers();
        // If using masked threads, set these too.
        TexMan.setSMPVars(NUMMASKEDTHREADS)
    }

    /*
     * private void InitPlaneWorkers(){ for (int i = 0; i < NUMFLOORTHREADS;
     * i++) { vpw[i] = new VisplaneWorker2(i,SCREENWIDTH, SCREENHEIGHT,
     * columnofs, ylookup, screen, visplanebarrier, NUMFLOORTHREADS);
     * //vpw[i].id = i; detailaware.add((IDetailAware) vpw[i]); } }
     */
    /*
         * TODO: relay to dependent objects. super.initScaling();
         * ColVars<byte[],byte[]> fake = new ColVars<byte[],byte[]>(); RWI =
         * C2JUtils.createArrayOfObjects(fake, SCREENWIDTH * 3); // Be MUCH more
         * generous with this one. RMI = C2JUtils.createArrayOfObjects(fake,
         * SCREENWIDTH * 6);
         */
    protected abstract fun InitMaskedWorkers()
    class HiColor(
        DM: DoomMain<ByteArray?, ShortArray?>, wallthread: Int,
        floorthreads: Int, nummaskedthreads: Int
    ) : ParallelRenderer<ByteArray?, ShortArray?>(DM, wallthread, floorthreads, nummaskedthreads) {
        init {

            // Init light levels
            colormaps.scalelight =
                Array(colormaps.lightLevels()) { arrayOfNulls<ShortArray>(colormaps.maxLightScale()) }
            colormaps.scalelightfixed = arrayOfNulls<ShortArray>(colormaps.maxLightScale())
            colormaps.zlight = Array(colormaps.lightLevels()) { arrayOfNulls<ShortArray>(colormaps.maxLightZ()) }
            completeInit()
        }

        override fun InitMaskedWorkers() {
            maskedworkers = arrayOfNulls<MaskedWorker<ByteArray?, ShortArray?>>(NUMMASKEDTHREADS) as Array<MaskedWorker<ByteArray?, ShortArray?>>
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

        /**
         * R_InitColormaps This is VERY different for hicolor.
         *
         * @throws IOException
         */
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

        override fun InitRWIExecutors(
            num: Int, RWI: Array<ColVars<ByteArray?, ShortArray?>?>?
        ): Array<RenderWallExecutor<ByteArray?, ShortArray?>?>? {
            val tmp = arrayOfNulls<RenderWallExecutor.HiColor>(num)!!
            for (i in 0 until num) {
                tmp[i] = RenderWallExecutor.HiColor(
                    DOOM.vs.getScreenWidth(),
                    DOOM.vs.getScreenHeight(),
                    columnofs,
                    ylookup,
                    screen,
                    RWI as Array<ColVars<ByteArray?, ShortArray?>>,
                    drawsegsbarrier
                )
            }
            return tmp as Array<RenderWallExecutor<ByteArray?, ShortArray?>?>
        }
    }

    class TrueColor(
        DM: DoomMain<ByteArray?, IntArray?>, wallthread: Int,
        floorthreads: Int, nummaskedthreads: Int
    ) : ParallelRenderer<ByteArray?, IntArray?>(DM, wallthread, floorthreads, nummaskedthreads) {
        init {

            // Init light levels
            colormaps.scalelight = Array(colormaps.lightLevels()) { arrayOfNulls<IntArray>(colormaps.maxLightScale()) }
            colormaps.scalelightfixed = arrayOfNulls<IntArray>(colormaps.maxLightScale())
            colormaps.zlight = Array(colormaps.lightLevels()) { arrayOfNulls<IntArray>(colormaps.maxLightZ()) }
            completeInit()
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

        /**
         * R_InitColormaps This is VERY different for hicolor.
         *
         * @throws IOException
         */
        @Throws(IOException::class)
        override fun InitColormaps() {
            colormaps.colormaps = DOOM.graphicSystem.getColorMap()
            println("COLORS15 Colormaps: " + colormaps.colormaps.size)

            // MAES: blurry effect is hardcoded to this colormap.
            // Pointless, since we don't use indexes. Instead, a half-brite
            // processing works just fine.
            BLURRY_MAP = DOOM.graphicSystem.getBlurryTable()
        }

        override fun InitMaskedWorkers() {
            maskedworkers = arrayOfNulls<MaskedWorker<ByteArray, IntArray>>(NUMMASKEDTHREADS) as Array<MaskedWorker<ByteArray?, IntArray?>>
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

        override fun InitRWIExecutors(
            num: Int, RWI: Array<ColVars<ByteArray?, IntArray?>?>?
        ): Array<RenderWallExecutor<ByteArray?, IntArray?>?>? {
            val tmp = arrayOfNulls<RenderWallExecutor.TrueColor>(num)!!
            for (i in 0 until num) {
                tmp[i] = RenderWallExecutor.TrueColor(
                    DOOM.vs.getScreenWidth(),
                    DOOM.vs.getScreenHeight(),
                    columnofs,
                    ylookup,
                    screen,
                    RWI as Array<ColVars<ByteArray?, IntArray?>>,
                    drawsegsbarrier
                )
            }
            return tmp as Array<RenderWallExecutor<ByteArray?, IntArray?>?>
        }
    }
}