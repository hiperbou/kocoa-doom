package rr.parallel


import rr.IDetailAware
import rr.drawfuns.ColVars
import rr.drawfuns.DoomColumnFunction
import rr.drawfuns.R_DrawColumnBoomOpt
import rr.drawfuns.R_DrawColumnBoomOptLow
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier

/**
 * This is what actual executes the RenderWallInstruction. Essentially it's a
 * self-contained column rendering function.
 *
 * @author admin
 */
open class RenderWallExecutor<T, V>(/////////////// VIDEO SCALE STUFF//////////////////////
    protected val SCREENWIDTH: Int, protected val SCREENHEIGHT: Int,
    columnofs: IntArray, ylookup: IntArray, screen: V,
    protected var RWI: Array<ColVars<T, V>>, protected var barrier: CyclicBarrier?
) : Runnable, IDetailAware {
    protected var start = 0
    protected var end = 0
    protected var colfunchi: DoomColumnFunction<T, V>? = null
    protected var colfunclow: DoomColumnFunction<T, V>? = null
    protected var colfunc: DoomColumnFunction<T, V>? = null
    fun setRange(start: Int, end: Int) {
        this.end = end
        this.start = start
    }

    override fun setDetail(detailshift: Int) {
        colfunc = if (detailshift == 0) colfunchi else colfunclow
    }

    override fun run() {

        // System.out.println("Wall executor from "+start +" to "+ end);
        for (i in start until end) {
            colfunc!!.invoke(RWI[i])
        }
        try {
            barrier!!.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            e.printStackTrace()
        }
    }

    fun updateRWI(RWI: Array<ColVars<T, V>>) {
        this.RWI = RWI
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
        ylookup: IntArray, screen: ShortArray?,
        RWI: Array<ColVars<ByteArray?, ShortArray?>>, barrier: CyclicBarrier?
    ) : RenderWallExecutor<ByteArray?, ShortArray?>(
        SCREENWIDTH,
        SCREENHEIGHT,
        columnofs,
        ylookup,
        screen,
        RWI,
        barrier
    ) {
        init {
            colfunchi = R_DrawColumnBoomOpt.HiColor(
                SCREENWIDTH, SCREENHEIGHT, ylookup,
                columnofs, null, screen, null
            )
            colfunc = colfunchi
            colfunclow = R_DrawColumnBoomOptLow.HiColor(
                SCREENWIDTH, SCREENHEIGHT, ylookup,
                columnofs, null, screen, null
            )
        }
    }

    class Indexed(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, columnofs: IntArray,
        ylookup: IntArray, screen: ByteArray?,
        RWI: Array<ColVars<ByteArray?, ByteArray?>>, barrier: CyclicBarrier?
    ) : RenderWallExecutor<ByteArray?, ByteArray?>(
        SCREENWIDTH,
        SCREENHEIGHT,
        columnofs,
        ylookup,
        screen,
        RWI,
        barrier
    ) {
        init {
            colfunchi = R_DrawColumnBoomOpt.Indexed(
                SCREENWIDTH, SCREENHEIGHT, ylookup,
                columnofs, null, screen, null
            )
            colfunc = colfunchi
            colfunclow = R_DrawColumnBoomOptLow.Indexed(
                SCREENWIDTH, SCREENHEIGHT, ylookup,
                columnofs, null, screen, null
            )
        }
    }

    class TrueColor(
        SCREENWIDTH: Int, SCREENHEIGHT: Int, columnofs: IntArray,
        ylookup: IntArray, screen: IntArray?,
        RWI: Array<ColVars<ByteArray?, IntArray?>>, barrier: CyclicBarrier?
    ) : RenderWallExecutor<ByteArray?, IntArray?>(SCREENWIDTH, SCREENHEIGHT, columnofs, ylookup, screen, RWI, barrier) {
        init {
            colfunchi = R_DrawColumnBoomOpt.TrueColor(
                SCREENWIDTH, SCREENHEIGHT, ylookup,
                columnofs, null, screen, null
            )
            colfunc = colfunchi
            colfunclow = R_DrawColumnBoomOptLow.TrueColor(
                SCREENWIDTH, SCREENHEIGHT, ylookup,
                columnofs, null, screen, null
            )
        }
    }
}