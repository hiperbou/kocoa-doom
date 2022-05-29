package rr.parallel

import i.IDoomSystem
import rr.IDetailAware
import rr.drawfuns.*
import v.tables.BlurryTable
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier


/**
 * This is what actual executes the RenderWallInstruction. Essentially it's a
 * self-contained column rendering function.
 *
 * @author velktron
 */
abstract class RenderMaskedExecutor<T, V>(/////////////// VIDEO SCALE STUFF//////////////////////
    protected val SCREENWIDTH: Int, protected val SCREENHEIGHT: Int,
    protected var RMI: Array<ColVars<T, V>>, protected var barrier: CyclicBarrier
) : Runnable, IDetailAware {
    protected var rmiend = 0
    protected var lowdetail = false
    protected var start = 0
    protected var end = 0
    protected var colfunchi: DoomColumnFunction<T, V>? = null
    protected var colfunclow: DoomColumnFunction<T, V>? = null
    protected var fuzzfunchi: DoomColumnFunction<T, V>? = null
    protected var fuzzfunclow: DoomColumnFunction<T, V>? = null
    protected var transfunchi: DoomColumnFunction<T, V>? = null
    protected var transfunclow: DoomColumnFunction<T, V>? = null
    protected var colfunc: DoomColumnFunction<T, V>? = null
    fun setRange(start: Int, end: Int) {
        this.end = end
        this.start = start
    }

    override fun setDetail(detailshift: Int) {
        lowdetail = if (detailshift == 0) false else true
    }

    override fun run() {

        // System.out.println("Wall executor from "+start +" to "+ end);
        var dc_flags = 0

        // Check out ALL valid RMIs, but only draw those on YOUR side of the screen.
        for (i in 0 until rmiend) {
            if (RMI[i].dc_x >= start && RMI[i].dc_x <= end) {
                // Change function type according to flags.
                // No flag change means reusing the last used type
                dc_flags = RMI[i].dc_flags
                //System.err.printf("Flags transition %d\n",dc_flags);
                colfunc = if (lowdetail) {
                    if (dc_flags and DcFlags.FUZZY != 0) fuzzfunclow else if (dc_flags and DcFlags.TRANSLATED != 0) transfunclow else colfunclow
                } else {
                    if (dc_flags and DcFlags.FUZZY != 0) fuzzfunchi else if (dc_flags and DcFlags.TRANSLATED != 0) transfunchi else colfunchi
                }

                // No need to set shared DCvars, because it's passed with the arg.
                colfunc!!.invoke(RMI[i])
            }
        }
        try {
            barrier.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            e.printStackTrace()
        }
    }

    fun setRMIEnd(rmiend: Int) {
        this.rmiend = rmiend
    }

    fun updateRMI(RMI: Array<ColVars<T, V>>) {
        this.RMI = RMI
    }

    /*
     * protected IVideoScale vs;
     * @Override public void setVideoScale(IVideoScale vs) { this.vs=vs; }
     * @Override public void initScaling() {
     * this.SCREENHEIGHT=vs.getScreenHeight();
     * this.SCREENWIDTH=vs.getScreenWidth(); }
     */
    class HiColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, columnofs: IntArray,
        ylookup: IntArray, screen: ShortArray?, RMI: Array<ColVars<ByteArray?, ShortArray?>>,
        barrier: CyclicBarrier, I: IDoomSystem, BLURRY_MAP: BlurryTable?
    ) : RenderMaskedExecutor<ByteArray?, ShortArray?>(SCREENWIDTH, SCREENHEIGHT, RMI, barrier) {
        init {

            // Regular masked columns
            colfunc = R_DrawColumnBoom.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
            colfunclow = R_DrawColumnBoomLow.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)

            // Fuzzy columns
            fuzzfunchi =
                R_DrawFuzzColumn.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I, BLURRY_MAP)
            fuzzfunclow =
                R_DrawFuzzColumnLow.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I, BLURRY_MAP)

            // Translated columns
            transfunchi = R_DrawTranslatedColumn.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
            transfunclow =
                R_DrawTranslatedColumnLow.HiColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
        }
    }

    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, columnofs: IntArray,
        ylookup: IntArray, screen: ByteArray?, RMI: Array<ColVars<ByteArray?, ByteArray?>>,
        barrier: CyclicBarrier, I: IDoomSystem, BLURRY_MAP: BlurryTable?
    ) : RenderMaskedExecutor<ByteArray?, ByteArray?>(SCREENWIDTH, SCREENHEIGHT, RMI, barrier) {
        init {

            // Regular masked columns
            colfunc = R_DrawColumnBoom.Indexed(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
            colfunclow = R_DrawColumnBoomLow.Indexed(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)

            // Fuzzy columns
            fuzzfunchi =
                R_DrawFuzzColumn.Indexed(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I, BLURRY_MAP)
            fuzzfunclow =
                R_DrawFuzzColumnLow.Indexed(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I, BLURRY_MAP)

            // Translated columns
            transfunchi = R_DrawTranslatedColumn.Indexed(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
            transfunclow =
                R_DrawTranslatedColumnLow.Indexed(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, columnofs: IntArray,
        ylookup: IntArray, screen: IntArray?, RMI: Array<ColVars<ByteArray?, IntArray?>>,
        barrier: CyclicBarrier, I: IDoomSystem, BLURRY_MAP: BlurryTable?
    ) : RenderMaskedExecutor<ByteArray?, IntArray?>(SCREENWIDTH, SCREENHEIGHT, RMI, barrier) {
        init {

            // Regular masked columns
            colfunc = R_DrawColumnBoom.TrueColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
            colfunclow = R_DrawColumnBoomLow.TrueColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)

            // Fuzzy columns
            fuzzfunchi =
                R_DrawFuzzColumn.TrueColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I, BLURRY_MAP)
            fuzzfunclow = R_DrawFuzzColumnLow.TrueColor(
                SCREENWIDTH,
                SCREENHEIGHT,
                ylookup,
                columnofs,
                null,
                screen,
                I,
                BLURRY_MAP
            )

            // Translated columns
            transfunchi =
                R_DrawTranslatedColumn.TrueColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
            transfunclow =
                R_DrawTranslatedColumnLow.TrueColor(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, null, screen, I)
        }
    }
}