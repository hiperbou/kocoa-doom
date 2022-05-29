/*
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package v.graphics

import f.Wiper
import m.IRandom
import utils.GenericCopy
import v.graphics.Plotter.Thin
import v.renderers.DoomScreen
import java.lang.reflect.Array

/**
 * Screen surface library
 *
 * @author Good Sign
 */
interface Screens<V, E : Enum<E>> {
    fun getScreen(screenType: E): V?
    fun getScalingX(): Int
    fun getScalingY(): Int
    fun getScreenWidth(): Int
    fun getScreenHeight(): Int
    fun createWiper(random: IRandom): Wiper
    /**
     * memset-like methods for screen surfaces
     */
    /**
     * Will fill destPortion on the screen with color of the specified point on it
     * The point argument IS NOT a color to fill, only a POINTER to the pixel on the screen
     */
    fun screenSet(screen: V, point: Int, destination: Horizontal) {
        GenericCopy.memset(screen, destination.start, destination.length, screen, point, 1)
    }

    /**
     * Will fill destPortion on the dstScreen by scrPortion pattern from srcScreen
     */
    fun screenSet(srcScreen: V, pattern: Horizontal, dstScreen: V, destination: Horizontal) {
        GenericCopy.memset(dstScreen, destination.start, destination.length, srcScreen, pattern.start, pattern.length)
    }

    /**
     * Will fill destPortion on the dstScreen with color of the specified point on the srcScreen
     * The point argument IS NOT a color to fill, only a POINTER to the pixel on the screen
     */
    fun screenSet(srcScreen: V, point: Int, dstScreen: V, destination: Horizontal) {
        GenericCopy.memset(dstScreen, destination.start, destination.length, srcScreen, point, 1)
    }

    /**
     * Will fill destPortion on the screen with srcPortion pattern from the same screen
     */
    fun screenSet(screen: V, pattern: Horizontal, destination: Horizontal) {
        GenericCopy.memset(screen, destination.start, destination.length, screen, pattern.start, pattern.length)
    }

    /**
     * memcpy-like method for screen surfaces
     */
    fun screenCopy(srcScreen: V, dstScreen: V, relocation: Relocation) {
        GenericCopy.memcpy(srcScreen, relocation.source, dstScreen, relocation.destination, relocation.length)
    }

    fun screenCopy(srcScreen: E, dstScreen: E) {
        val dstScreenObj: Any? = getScreen(dstScreen)
        GenericCopy.memcpy(getScreen(srcScreen), 0, dstScreenObj, 0, Array.getLength(dstScreenObj))
    }

    fun createPlotter(screen: E): Plotter<V?> {
        return Thin(getScreen(screen), getScreenWidth())
    }

    class BadRangeException : Exception {
        constructor(m: String?) : super(m) {}
        constructor() {}

        companion object {
            private const val serialVersionUID = 2903441181162189295L
        }
    }

    companion object {
        val SCREENS_COUNT = DoomScreen.values().size
    }
}