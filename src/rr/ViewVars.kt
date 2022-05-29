package rr

import data.Tables
import doom.player_t
import utils.C2JUtils
import v.scale.VideoScale

class ViewVars(vs: VideoScale) {
    // Found in draw_c. Only ever used in renderer.
    var windowx = 0
    var windowy = 0
    var width = 0
    var height = 0

    // MAES: outsiders have no business peeking into this.
    // Or...well..maybe they do. It's only used to center the "pause" X
    // position.
    // TODO: get rid of this?
    var scaledwidth = 0
    var centerx = 0
    var centery = 0

    /** Used to determine the view center and projection in view units fixed_t  */
    var centerxfrac = 0
    var centeryfrac = 0
    var projection = 0

    /** fixed_t  */
    var x = 0
    var y = 0
    var z = 0

    // MAES: an exception to strict type safety. These are used only in here,
    // anyway (?) and have no special functions.
    // Plus I must use them as indexes. angle_t
    var angle: Long = 0

    /** fixed  */
    var cos = 0
    var sin = 0
    var player: player_t? = null

    /** Heretic/freeview stuff?  */
    var lookdir = 0

    // 0 = high, 1 = low. Normally only the menu and the interface can change
    // that.
    var detailshift = 0
    var WEAPONADJUST = 0
    var BOBADJUST = 0

    /**
     * constant arrays used for psprite clipping and initializing clipping
     */
    val negonearray // MAES: in scaling
            : ShortArray
    var screenheightarray // MAES: in scaling
            : ShortArray

    /** Mirrors the one in renderer...  */
    var xtoviewangle: LongArray

    init {
        negonearray = ShortArray(vs.getScreenWidth()) // MAES: in scaling
        screenheightarray = ShortArray(vs.getScreenWidth()) // MAES: in scaling
        xtoviewangle = LongArray(vs.getScreenWidth() + 1)
        C2JUtils.memset(negonearray, (-1).toShort(), negonearray.size)
    }

    fun PointToAngle(x: Int, y: Int): Long {
        // MAES: note how we don't use &BITS32 here. That is because
        // we know that the maximum possible value of tantoangle is angle
        // This way, we are actually working with vectors emanating
        // from our current position.
        var x = x
        var y = y
        x -= this.x
        y -= this.y
        if (x == 0 && y == 0) return 0
        return if (x >= 0) {
            // x >=0
            if (y >= 0) {
                // y>= 0
                if (x > y) {
                    // octant 0
                    Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())].toLong()
                } else {
                    // octant 1
                    Tables.ANG90 - 1 - Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                }
            } else {
                // y<0
                y = -y
                if (x > y) {
                    // octant 8
                    (-Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())]).toLong()
                } else {
                    // octant 7
                    Tables.ANG270 + Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                }
            }
        } else {
            // x<0
            x = -x
            if (y >= 0) {
                // y>= 0
                if (x > y) {
                    // octant 3
                    Tables.ANG180 - 1 - Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())]
                } else {
                    // octant 2
                    Tables.ANG90 + Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                }
            } else {
                // y<0
                y = -y
                if (x > y) {
                    // octant 4
                    Tables.ANG180 + Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())]
                } else {
                    // octant 5
                    Tables.ANG270 - 1 - Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                }
            }
        }
        // This is actually unreachable.
        // return 0;
    }

    fun getViewWindowX(): Int {
        return windowx
    }

    fun getViewWindowY(): Int {
        return windowy
    }

    fun getScaledViewWidth(): Int {
        return scaledwidth
    }

    fun getScaledViewHeight(): Int {
        return height
    }
}