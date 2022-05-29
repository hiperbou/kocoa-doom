package rr.parallel


import rr.AbstractThings
import rr.IDetailAware
import rr.SceneRenderer
import rr.drawfuns.ColVars
import rr.drawfuns.DcFlags
import utils.C2JUtils
import v.scale.VideoScale
import v.tables.BlurryTable
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executor

/**
 * Parallel Things drawing class, column based, using RMI pipeline.
 * For N threads, each thread only draws those columns of sprites that
 * are in its own 1/N portion of the screen.
 *
 * Overrides only the terminal drawing methods from things, using a
 * mechanism very similar to column-based wall threading. It's not very
 * efficient, since some of the really heavy parts (such as visibility
 * priority) are still done serially, and actually do take up a lot of the
 * actual rendering time, and the number of columns generated is REALLY
 * enormous (100K+ for something like nuts.wad), and the thing chokes on
 * synchronization, more than anything. The only appropriate thing to do
 * would be to have a per-vissprite renderer, which would actually move much
 * of the brunt work away from the main thread. Some interesting benchmarks
 * on nuts.wad timedemo: Normal things serial renderer: 60-62 fps "Dummy"
 * completeColumns: 72 fps "Dummy" things renderer without final drawing: 80
 * fps "Dummy" things renderer without ANY calculations: 90 fps. This means
 * that even a complete parallelization will likely have a quite limited
 * impact.
 *
 * @author velktron
 */
abstract class ParallelThings<T, V>(
    vs: VideoScale,
    R: SceneRenderer<T, V>,
    protected val tp: Executor,
    protected val NUMMASKEDTHREADS: Int
) : AbstractThings<T, V>(vs, R) {
    // stuff to get from container
    /** Render Masked Instuction subsystem. Essentially, a way to split sprite work
     * between threads on a column-basis.
     */
    protected lateinit var RMI: Array<ColVars<T, V>>

    /**
     * Increment this as you submit RMIs to the "queue". Remember to reset to 0
     * when you have drawn everything!
     */
    protected var RMIcount = 0
    protected lateinit var RMIExec: Array<RenderMaskedExecutor<T, V>>
    protected val maskedbarrier: CyclicBarrier
    override fun DrawMasked() {

        // This just generates the RMI instructions.
        super.DrawMasked()
        // This splits the work among threads and fires them up
        RenderRMIPipeline()
        try {
            // Wait for them to be done.
            maskedbarrier.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            e.printStackTrace()
        }
    }

    override fun completeColumn() {
        if (view.detailshift == 1) flags = DcFlags.LOW_DETAIL
        // Don't wait to go over
        if (RMIcount >= RMI.size) {
            ResizeRMIBuffer()
        }

        // A deep copy is still necessary, as well as setting dc_flags
        RMI[RMIcount].copyFrom(maskedcvars, colfunc!!.getFlags())

        // We only need to point to the next one in the list.
        RMIcount++
    }

    var flags = 0

    init {
        maskedbarrier = CyclicBarrier(NUMMASKEDTHREADS + 1)
    }

    protected fun RenderRMIPipeline() {
        for (i in 0 until NUMMASKEDTHREADS) {
            RMIExec[i].setRange(
                i * vs.getScreenWidth() / NUMMASKEDTHREADS,
                (i + 1) * vs.getScreenWidth() / NUMMASKEDTHREADS
            )
            RMIExec[i].setRMIEnd(RMIcount)
            // RWIExec[i].setRange(i%NUMWALLTHREADS,RWIcount,NUMWALLTHREADS);
            tp.execute(RMIExec[i])
        }

        // System.out.println("RWI count"+RWIcount);
        RMIcount = 0
    }

    protected fun ResizeRMIBuffer() {
        val fake = ColVars<T, V>()
        val tmp = C2JUtils.createArrayOfObjects(fake, RMI.size * 2)
        System.arraycopy(RMI, 0, tmp, 0, RMI.size)

        // Bye bye, old RMI.
        RMI = tmp
        for (i in 0 until NUMMASKEDTHREADS) {
            RMIExec[i].updateRMI(RMI)
        }
        System.err.println("RMI Buffer resized. Actual capacity " + RMI.size)
    }

    protected abstract fun InitRMISubsystem(
        columnofs: IntArray,
        ylookup: IntArray,
        screen: V,
        maskedbarrier: CyclicBarrier,
        BLURRY_MAP: BlurryTable?,
        detailaware: MutableList<IDetailAware?>
    )

    class Indexed(vs: VideoScale, R: SceneRenderer<ByteArray?, ByteArray?>, tp: Executor, numthreads: Int) :
        ParallelThings<ByteArray?, ByteArray?>(vs, R, tp, numthreads) {
        protected override fun InitRMISubsystem(
            columnofs: IntArray,
            ylookup: IntArray,
            screen: ByteArray?,
            maskedbarrier: CyclicBarrier,
            BLURRY_MAP: BlurryTable?,
            detailaware: MutableList<IDetailAware?>
        ) {
            for (i in 0 until NUMMASKEDTHREADS) {
                RMIExec[i] = RenderMaskedExecutor.Indexed(
                    vs.getScreenWidth(), vs.getScreenHeight(), columnofs,
                    ylookup, screen, RMI, maskedbarrier, I, BLURRY_MAP
                )
                detailaware.add(RMIExec[i])
            }
        }
    }

    class HiColor(vs: VideoScale, R: SceneRenderer<ByteArray?, ShortArray?>, tp: Executor, numthreads: Int) :
        ParallelThings<ByteArray?, ShortArray?>(vs, R, tp, numthreads) {
        protected override fun InitRMISubsystem(
            columnofs: IntArray,
            ylookup: IntArray,
            screen: ShortArray?,
            maskedbarrier: CyclicBarrier,
            BLURRY_MAP: BlurryTable?,
            detailaware: MutableList<IDetailAware?>
        ) {
            for (i in 0 until NUMMASKEDTHREADS) {
                RMIExec[i] = RenderMaskedExecutor.HiColor(
                    vs.getScreenWidth(), vs.getScreenHeight(), columnofs,
                    ylookup, screen, RMI, maskedbarrier, I, BLURRY_MAP
                )
                detailaware.add(RMIExec[i])
            }
        }
    }

    class TrueColor(vs: VideoScale, R: SceneRenderer<ByteArray?, IntArray?>, tp: Executor, numthreads: Int) :
        ParallelThings<ByteArray?, IntArray?>(vs, R, tp, numthreads) {
        protected override fun InitRMISubsystem(
            columnofs: IntArray,
            ylookup: IntArray,
            screen: IntArray?,
            maskedbarrier: CyclicBarrier,
            BLURRY_MAP: BlurryTable?,
            detailaware: MutableList<IDetailAware?>
        ) {
            for (i in 0 until NUMMASKEDTHREADS) {
                RMIExec[i] = RenderMaskedExecutor.TrueColor(
                    vs.getScreenWidth(), vs.getScreenHeight(), columnofs,
                    ylookup, screen, RMI, maskedbarrier, I, BLURRY_MAP
                )
                detailaware.add(RMIExec[i])
            }
        }
    }
}