package rr


import doom.DoomMain
import rr.drawfuns.*
import java.io.IOException

abstract class UnifiedRenderer<T, V>(DOOM: DoomMain<T, V>) : RendererState<T, V>(DOOM) {
    init {
        MySegs = Segs(this)
    }

    /**
     * A very simple Seg (Wall) drawer, which just completes abstract SegDrawer by calling the final column functions.
     *
     * TODO: move out of RendererState.
     *
     * @author velktron
     */
    protected inner class Segs(R: SceneRenderer<*, *>) : SegDrawer(R) {
        /**
         * For serial version, just complete the call
         */
        override fun CompleteColumn() {
            colfunc!!.main!!.invoke()
        }
    }

    ////////////////// The actual rendering calls ///////////////////////
    class HiColor(DOOM: DoomMain<ByteArray?, ShortArray?>) : UnifiedRenderer<ByteArray?, ShortArray?>(DOOM) {
        init {

            // Init any video-output dependant stuff            
            // Init light levels
            val LIGHTLEVELS = colormaps.lightLevels()
            val MAXLIGHTSCALE = colormaps.maxLightScale()
            val MAXLIGHTZ = colormaps.maxLightZ()
            colormaps.scalelight = Array(LIGHTLEVELS) { arrayOfNulls(MAXLIGHTSCALE) }
            colormaps.scalelightfixed = arrayOfNulls(MAXLIGHTSCALE)
            colormaps.zlight = Array(LIGHTLEVELS) { arrayOfNulls(MAXLIGHTZ) }
            completeInit()
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
            BLURRY_MAP = DOOM.graphicSystem.getBlurryTable()
        }

        /**
         * Initializes the various drawing functions. They are all "pegged" to the same dcvars/dsvars object. Any
         * initializations of e.g. parallel renderers and their supporting subsystems should occur here.
         */
        override fun R_InitDrawingFunctions() {

            // Span functions. Common to all renderers unless overriden
            // or unused e.g. parallel renderers ignore them.
            DrawSpan = R_DrawSpan.HiColor(
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

    class Indexed(DOOM: DoomMain<ByteArray?, ByteArray?>) : UnifiedRenderer<ByteArray?, ByteArray?>(DOOM) {
        init {

            // Init light levels
            val LIGHTLEVELS = colormaps.lightLevels()
            val MAXLIGHTSCALE = colormaps.maxLightScale()
            val MAXLIGHTZ = colormaps.maxLightZ()
            colormaps.scalelight = Array(LIGHTLEVELS) { arrayOfNulls<ByteArray>(MAXLIGHTSCALE) }
            colormaps.scalelightfixed = arrayOfNulls<ByteArray>(MAXLIGHTSCALE)
            colormaps.zlight = Array(LIGHTLEVELS) { arrayOfNulls<ByteArray>(MAXLIGHTZ) }
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

        /**
         * Initializes the various drawing functions. They are all "pegged" to the same dcvars/dsvars object. Any
         * initializations of e.g. parallel renderers and their supporting subsystems should occur here.
         */
        override fun R_InitDrawingFunctions() {

            // Span functions. Common to all renderers unless overriden
            // or unused e.g. parallel renderers ignore them.
            DrawSpan = R_DrawSpan.Indexed(
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
            //DrawTLColumn=new R_DrawTLColumn(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);

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

    class TrueColor(DOOM: DoomMain<ByteArray?, IntArray?>) : UnifiedRenderer<ByteArray?, IntArray?>(DOOM) {
        init {

            // Init light levels
            val LIGHTLEVELS = colormaps.lightLevels()
            val MAXLIGHTSCALE = colormaps.maxLightScale()
            val MAXLIGHTZ = colormaps.maxLightZ()
            colormaps.scalelight = Array(LIGHTLEVELS) { arrayOfNulls<IntArray>(MAXLIGHTSCALE) }
            colormaps.scalelightfixed = arrayOfNulls<IntArray>(MAXLIGHTSCALE)
            colormaps.zlight = Array(LIGHTLEVELS) { arrayOfNulls<IntArray>(MAXLIGHTZ) }
            completeInit()
        }

        /**
         * R_InitColormaps This is VERY different for hicolor.
         *
         * @throws IOException
         */
        @Throws(IOException::class)
        override fun InitColormaps() {
            colormaps.colormaps = DOOM.graphicSystem.getColorMap()
            println("COLORS32 Colormaps: " + colormaps.colormaps.size)

            // MAES: blurry effect is hardcoded to this colormap.
            BLURRY_MAP = DOOM.graphicSystem.getBlurryTable()
        }

        /**
         * Initializes the various drawing functions. They are all "pegged" to the same dcvars/dsvars object. Any
         * initializations of e.g. parallel renderers and their supporting subsystems should occur here.
         */
        override fun R_InitDrawingFunctions() {

            // Span functions. Common to all renderers unless overriden
            // or unused e.g. parallel renderers ignore them.
            DrawSpan = R_DrawSpan.TrueColor(
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
            //DrawTLColumn=new R_DrawTLColumn.TrueColor(SCREENWIDTH,SCREENHEIGHT,ylookup,columnofs,maskedcvars,screen,I);

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