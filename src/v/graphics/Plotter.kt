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

import utils.GenericCopy
import java.util.*
import java.lang.reflect.Array

/**
 *
 * @author Good Sign
 */
interface Plotter<V> {
    fun setColorSource(colorSource: V): Plotter<V>? {
        return setColorSource(colorSource, 0)
    }

    fun setColorSource(colorSource: V, colorPos: Int): Plotter<V>
    fun setPosition(x: Int, y: Int): Plotter<V>
    fun setThickness(dupX: Int, dupY: Int): Plotter<V>
    fun plot(): Plotter<V>
    fun shiftX(shift: Int): Plotter<V>
    fun shiftY(shift: Int): Plotter<V>
    fun getX(): Int
    fun getY(): Int
    enum class Style {
        Thin, Thick, Deep
    }

    fun shift(shiftX: Int, shiftY: Int): Plotter<V>? {
        return shiftX(shiftX).shiftY(shiftY)
    }

    /**
     * Abstract plotter - without a Plot method
     */
    abstract class Abstract<V> internal constructor(protected val screen: V, protected val rowShift: Int) : Plotter<V> {
        protected var style: Plotter.Style? = null
        protected var colorSource: V? = null
        protected var point = 0
        protected var _x = 0
        protected var _y = 0
        override fun setColorSource(colorSource: V, colorPos: Int): Plotter<V> {
            Objects.requireNonNull(colorSource)
            // cache only necessary part of the source
            this.colorSource = Array.newInstance(colorSource!!::class.java.getComponentType(), 1) as V
            GenericCopy.memcpy(colorSource, colorPos, this.colorSource, 0, 1)
            return this
        }

        override fun setThickness(dupX: Int, dupY: Int): Plotter<V> {
            return this
        }

        override fun setPosition(x: Int, y: Int): Plotter<V> {
            point = y * rowShift + x
            this._x = x
            this._y = y
            return this
        }

        override fun shiftX(shift: Int): Plotter<V> {
            point += shift
            _x += shift
            return this
        }

        override fun shiftY(shift: Int): Plotter<V> {
            if (shift > 0) {
                point += rowShift
                ++_y
            } else {
                point -= rowShift
                --_y
            }
            return this
        }

        override fun getX(): Int {
            return _x
        }

        override fun getY(): Int {
            return _y
        }
    }

    class Thin<V>(screen: V, rowShift: Int) : Abstract<V>(screen, rowShift) {
        override fun plot(): Plotter<V> {
            GenericCopy.memcpy(colorSource, 0, screen, point, 1)
            return this
        }
    }

    /**
     * You give it desired scaling level, it makes lines thicker
     */
    open class Thick<V>     // can overflow!
    //dupX >> 1;
    //dupX >> 1;
        (screen: V, width: Int, protected val height: Int) : Abstract<V>(screen, width) {
        protected var xThick = 1
        protected var yThick = 1
        override fun setThickness(dupX: Int, dupY: Int): Plotter<V> {
            xThick = dupX
            yThick = dupY
            return this
        }

        override fun plot(): Plotter<V> {
            if (xThick == 0 || yThick == 0) {
                GenericCopy.memcpy(colorSource, 0, screen, point, 1)
                return this
            }
            return plotThick(xThick, yThick)
        }

        protected fun plotThick(modThickX: Int, modThickY: Int): Plotter<V> {
            val rows = if (_y < modThickY) _y else if (height < _y + modThickY) height - _y else modThickY
            val spaceLeft = if (_x < modThickX) 0 else modThickX
            val spaceRight = if (rowShift < _x + modThickX) rowShift - _x else modThickX
            for (row in -rows until rows) {
                // color = colorSource[Math.abs(row)]
                GenericCopy.memset(
                    screen,
                    point - spaceLeft + rowShift * row,
                    spaceLeft + spaceRight,
                    colorSource,
                    0,
                    1
                )
            }
            return this
        }
    }

    /**
     * Thick, but the direction of drawing is counted in - i.e., for round borders...
     */
    class Deep<V>(screen: V, width: Int, height: Int) : Thick<V>(screen, width, height) {
        protected var direction: Direction? = null
        override fun setPosition(x: Int, y: Int): Plotter<V> {
            direction = Direction.CENTER
            return super.setPosition(x, y)
        }

        override fun shiftX(shift: Int): Plotter<V> {
            direction = direction!!.rotationHor(shift)
            return super.shiftX(shift)
        }

        override fun shiftY(shift: Int): Plotter<V> {
            direction = direction!!.rotationVert(shift)
            return super.shiftY(shift)
        }

        override fun shift(shiftX: Int, shiftY: Int): Plotter<V>? {
            direction = direction!!.rotation(shiftX, shiftY)
            return super.shift(shiftX, shiftY)
        }

        override fun plot(): Plotter<V> {
            if (xThick <= 1 || yThick <= 1) {
                return super.plot()
            }
            var modThickX = xThick
            var modThickY = yThick
            if (!direction!!.hasTop && !direction!!.hasBottom) {
                modThickX = modThickX shr 1
            }
            if (!direction!!.hasLeft && !direction!!.hasRight) {
                modThickY = modThickY shr 1
            }
            return plotThick(modThickX, modThickY)
        }
    }

    companion object {
        fun getThickness(dupX: Int): Int {
            return Math.max(dupX shr 1, 1)
        }
    }
}