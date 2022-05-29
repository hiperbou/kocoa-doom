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

/**
 * Interface for an object that conveys screen resolution/scaling information, meant to replace the static declarations
 * in Defines.
 *
 * @author velktron
 */
interface VideoScale {
    fun getScreenWidth(): Int
    fun getScreenHeight(): Int
    fun getScalingX(): Int
    fun getScalingY(): Int

    /**
     * Safest global scaling for fixed stuff like menus, titlepic etc
     */
    fun getSafeScaling(): Int

    /**
     * Get floating point screen multiplier. Not recommended, as it causes visual glitches. Replace with safe scale,
     * whenever possible
     */
    fun getScreenMul(): Float

    /**
     * Future, should signal aware objects that they should refresh their resolution-dependent state, structures,
     * variables etc.
     *
     * @return
     */
    fun changed(): Boolean

    companion object {
        //It is educational but futile to change this
        //scaling e.g. to 2. Drawing of status bar,
        //menues etc. is tied to the scale implied
        //by the graphics.
        const val INV_ASPECT_RATIO = 0.625 // 0.75, ideally

        //
        // For resize of screen, at start of game.
        // It will not work dynamically, see visplanes.
        //
        const val BASE_WIDTH = 320
        val BASE_HEIGHT: Int = (VideoScale.INV_ASPECT_RATIO * 320).toInt() // 200
    }
}