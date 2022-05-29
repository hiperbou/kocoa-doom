/**
 * Copyright (C) 2017 Good Sign
 * Copyright (C) 2022 hiperbou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package v.scale

internal class VideoScaleInfo : VideoScale {
    protected var scale: Float
    protected var width: Int
    protected var height: Int
    protected var bestScaleX: Int
    protected var bestScaleY: Int
    protected var bestSafeScale: Int

    /** Scale is intended as a multiple of the base resolution, 320 x 200.
     * If changing the ratio is also desired, then keep in mind that
     * the base width is always considered fixed, while the base height
     * is not.
     *
     * @param scale
     */
    constructor(scale: Float) {
        this.scale = scale
        width = (VideoScale.BASE_WIDTH * scale).toInt()
        height = (scale * VideoScale.BASE_WIDTH * VideoScale.INV_ASPECT_RATIO).toInt()
        bestScaleX = Math.floor((width.toFloat() / VideoScale.BASE_WIDTH.toFloat()).toDouble()).toInt()
        bestScaleY = Math.floor((height.toFloat() / VideoScale.BASE_HEIGHT.toFloat()).toDouble()).toInt()
        bestSafeScale = Math.min(bestScaleX, bestScaleY)
    }

    /** It's possible to specify other aspect ratios, too, keeping in mind
     * that there are maximum width and height limits to take into account,
     * and that scaling of graphics etc. will be rather problematic. Default
     * ratio is 0.625, 0.75 will give a nice 4:3 ratio.
     *
     * TODO: pretty lame...
     *
     * @param scale
     * @param ratio
     */
    constructor(scale: Float, ratio: Float) {
        this.scale = scale
        width = (VideoScale.BASE_WIDTH * scale).toInt()
        height = (scale * VideoScale.BASE_WIDTH * ratio).toInt()
        bestScaleX = Math.floor((width.toFloat() / VideoScale.BASE_WIDTH.toFloat()).toDouble()).toInt()
        bestScaleY = Math.floor((height.toFloat() / VideoScale.BASE_HEIGHT.toFloat()).toDouble()).toInt()
        bestSafeScale = Math.min(bestScaleX, bestScaleY)
    }

    override fun getScreenWidth(): Int {
        return width
    }

    override fun getScreenHeight(): Int {
        return height
    }

    override fun getScalingX(): Int {
        return bestScaleX
    }

    override fun getScalingY(): Int {
        return bestScaleY
    }

    override fun getSafeScaling(): Int {
        return bestSafeScale
    }

    override fun changed(): Boolean {
        return false
    }

    override fun getScreenMul(): Float {
        return scale
    }
}