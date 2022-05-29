package awt


import java.awt.DisplayMode
import java.awt.GraphicsDevice
import java.util.*

class DisplayModePicker(protected var device: GraphicsDevice) {
    protected var default_mode: DisplayMode

    init {
        default_mode = device.displayMode
    }

    fun pickClosest(width: Int, height: Int): DisplayMode? {
        val modes = device.displayModes
        val picks: MutableList<DisplayMode?> = ArrayList()
        val wc = WidthComparator()
        val hc = HeightComparator()

        // Filter out those with too small dimensions.
        for (dm in modes) {
            if (dm.width >= width && dm.height >= height) {
                picks.add(dm)
            }
        }
        if (picks.size > 0) {
            Collections.sort(picks, wc.thenComparing(hc))
        }

        // First one is the minimum that satisfies the desired criteria.
        return picks[0]
    }

    /**
     * Return offsets to center rasters too oddly shaped to fit entirely into
     * a standard display mode (unfortunately, this means most stuff > 640 x 400),
     * with doom's standard 8:5 ratio.
     *
     * @param width
     * @param height
     * @param dm
     * @return array, x-offset and y-offset.
     */
    fun getCentering(width: Int, height: Int, dm: DisplayMode): IntArray {
        val xy = IntArray(2)
        xy[0] = (dm.width - width) / 2
        xy[1] = (dm.height - height) / 2
        return xy
    }

    internal inner class WidthComparator : Comparator<DisplayMode> {
        override fun compare(arg0: DisplayMode, arg1: DisplayMode): Int {
            if (arg0.width > arg1.width) {
                return 1
            }
            return if (arg0.width < arg1.width) {
                -1
            } else 0
        }
    }

    internal inner class HeightComparator : Comparator<DisplayMode> {
        override fun compare(arg0: DisplayMode, arg1: DisplayMode): Int {
            if (arg0.height > arg1.height) {
                return 1
            }
            return if (arg0.height < arg1.height) {
                -1
            } else 0
        }
    }
}