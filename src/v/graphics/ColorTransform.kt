/**
 * Copyright (C) 1993-1996 Id Software, Inc.
 * from f_wipe.c
 *
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
package v.graphics

import utils.GenericCopy
import v.graphics.Wipers.WiperImpl
import java.lang.reflect.Array

interface ColorTransform {
    fun initTransform(wiper: WiperImpl<*, *>): Boolean {
        GenericCopy.memcpy(wiper.wipeStartScr, 0, wiper.wipeEndScr, 0, Array.getLength(wiper.wipeEndScr))
        return false
    }

    fun colorTransformB(wiper: WiperImpl<ByteArray, *>): Boolean {
        val w = wiper.wipeStartScr
        val e = wiper.wipeEndScr
        var changed = false
        var i = 0
        var newval: Int
        while (i < w.size) {
            if (w[i] != e[i]) {
                w[i] = if (w[i] > e[i]) if (w[i] - wiper.ticks.also {
                        newval = it
                    } < e[i]) e[i] else newval.toByte() else if (w[i] + wiper.ticks.also {
                        newval = it
                    } > e[i]) e[i] else newval.toByte()
                changed = true
            }
            ++i
        }
        return !changed
    }

    fun colorTransformS(wiper: WiperImpl<ShortArray, *>): Boolean {
        val w = wiper.wipeStartScr
        val e = wiper.wipeEndScr
        var changed = false
        var i = 0
        var newval: Int
        while (i < w.size) {
            if (w[i] != e[i]) {
                w[i] = (if (w[i] > e[i]) if (w[i] - wiper.ticks.also {
                        newval = it
                    } < e[i]) e[i] else newval.toByte() else if (w[i] + wiper.ticks.also {
                        newval = it
                    } > e[i]) e[i] else newval.toByte()).toShort()
                changed = true
            }
            ++i
        }
        return !changed
    }

    fun colorTransformI(wiper: WiperImpl<IntArray, *>): Boolean {
        val w = wiper.wipeStartScr
        val e = wiper.wipeEndScr
        var changed = false
        var i = 0
        var newval: Int
        while (i < w.size) {
            if (w[i] != e[i]) {
                w[i] = (if (w[i] > e[i]) if (w[i] - wiper.ticks.also {
                        newval = it
                    } < e[i]) e[i] else newval.toByte() else if (w[i] + wiper.ticks.also {
                        newval = it
                    } > e[i]) e[i] else newval.toByte()).toInt()
                changed = true
            }
            ++i
        }
        return !changed
    }
}