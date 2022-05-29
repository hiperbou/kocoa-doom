package rr.parallel


import rr.IMaskedDrawer
import rr.ISpriteManager
import rr.IVisSpriteManagement
import rr.SceneRenderer
import v.scale.VideoScale
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executor

/**  Alternate parallel sprite renderer using a split-screen strategy.
 * For N threads, each thread gets to render only the sprites that are entirely
 * in its own 1/Nth portion of the screen.
 *
 * Sprites that span more than one section, are drawn partially. Each thread
 * only has to worry with the priority of its own sprites. Similar to the
 * split-seg parallel drawer.
 *
 * Uses the "masked workers" subsystem, there is no column pipeline: workers
 * "tap" directly in the sprite sorted table and act accordingly (draw entirely,
 * draw nothing, draw partially).
 *
 * It uses masked workers to perform the actual work, each of which is a complete
 * Thing Drawer.
 *
 * @author velktron
 */
class ParallelThings2<T, V>(vs: VideoScale, R: SceneRenderer<T, V>) : IMaskedDrawer<T, V> {
    lateinit var maskedworkers: Array<MaskedWorker<T, V>>
    var maskedbarrier: CyclicBarrier? = null
    var tp: Executor? = null
    protected val VIS: IVisSpriteManagement<V>
    protected val vs: VideoScale

    init {
        VIS = R.getVisSpriteManager()
        this.vs = vs
    }

    override fun DrawMasked() {
        VIS.SortVisSprites()
        for (i in maskedworkers.indices) {
            tp!!.execute(maskedworkers[i])
        }
        try {
            maskedbarrier!!.await()
        } catch (e: InterruptedException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    override fun completeColumn() {
        // Does nothing. Dummy.
    }

    override fun setPspriteScale(scale: Int) {
        for (i in maskedworkers.indices) maskedworkers[i].setPspriteScale(scale)
    }

    override fun setPspriteIscale(scale: Int) {
        for (i in maskedworkers.indices) maskedworkers[i].setPspriteIscale(scale)
    }

    override fun setDetail(detailshift: Int) {
        for (i in maskedworkers.indices) maskedworkers[i].setDetail(detailshift)
    }

    override fun cacheSpriteManager(SM: ISpriteManager) {
        for (i in maskedworkers.indices) maskedworkers[i].cacheSpriteManager(SM)
    }
}