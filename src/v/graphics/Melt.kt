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

import utils.C2JUtils
import v.graphics.Wipers.WiperImpl

interface Melt : ColorTransform {
    /**
     * No more fucking column-major transpose!
     * A funny fast thing for 1993, but able to make Intel i7 think hard in 2017
     * (well, at least, in energy saving mode :p)
     * - Good Sign, 2017/04/10
     */
    fun initMeltScaled(wiper: WiperImpl<*, *>): Boolean {
        return initMelt(wiper, true)
    }

    //@JvmOverloads
    fun initMelt(wiper: WiperImpl<*, *>, scaled: Boolean = false): Boolean {
        // copy start screen to main screen
        C2JUtils.memcpy(wiper.wipeStartScr, wiper.wipeScr, wiper.screenWidth * wiper.screenHeight)
        setupColumnPositions(wiper, scaled)
        return false
    }

    /**
     * setup initial column positions
     * (y<0 => not ready to scroll yet)
     */
    fun setupColumnPositions(wiper: WiperImpl<*, *>, scaled: Boolean) {
        val lim = if (scaled) wiper.screenWidth / wiper.dupy else wiper.screenWidth
        wiper.y = IntArray(lim)
        val wiper_y = wiper.y!!
        wiper_y[0] = -(wiper.random.M_Random() % 16)
        for (i in 1 until lim) {
            val r = wiper.random.M_Random() % 3 - 1
            wiper_y[i] = wiper_y[i - 1] + r
            if (wiper_y[i] > 0) {
                wiper_y[i] = 0
            } else if (wiper_y[i] == -16) {
                wiper_y[i] = -15
            }
        }
    }

    /**
     * The only place where we cannot have generic code, because it is 1 pixel copy operation
     * which to be called tens thousands times and will cause overhead on just literally any more intermediate function
     * The "same object" comparison is actually comparison of two integers - pointers in memory, - so it is instant
     * and branching is predictable, so a good cache will negate the class checks completely
     * - Good Sign 2017/04/10
     */
    fun toScreen(bufType: Class<*>, src: Any, dest: Any, width: Int, dy: Int, ps: Int, pd: Int) {
        if (bufType == IntArray::class.java) {
            val to = src as IntArray
            val from = dest as IntArray
            for (i in 0 until dy) {
                val iWidth = width * i
                to[pd + iWidth] = from[ps + iWidth]
            }
        } else if (bufType == ShortArray::class.java) {
            val to = src as ShortArray
            val from = dest as ShortArray
            for (i in 0 until dy) {
                val iWidth = width * i
                to[pd + iWidth] = from[ps + iWidth]
            }
        } else if (bufType == ByteArray::class.java) {
            val to = src as ByteArray
            val from = dest as ByteArray
            for (i in 0 until dy) {
                val iWidth = width * i
                to[pd + iWidth] = from[ps + iWidth]
            }
        } else throw UnsupportedOperationException("Do not have support for: $bufType")
    }

    /**
     * Completely opposite of the previous method. Only performant when scaling is on.
     * Stick to System.arraycopy since there is certainly several pixels to get and set.
     * Also, it doesn't even need to check and cast to classes
     * - Good Sign 2017/04/10
     */
    fun toScreenScaled(wiper: WiperImpl<*, *>, from: Any?, dy: Int, ps: Int, pd: Int) {
        for (i in 0 until dy) {
            val iWidth = wiper.screenWidth * i
            System.arraycopy(from, ps + iWidth, wiper.wipeScr, pd + iWidth, wiper.dupy)
        }
    }

    /**
     * Scrolls down columns ready for scroll and those who aren't makes a bit more ready
     * Finally no more shitty transpose!
     * - Good Sign 2017/04/10
     */
    fun doMeltScaled(wiper: WiperImpl<*, *>): Boolean {
        return doMelt(wiper, true)
    }

    //@JvmOverloads
    fun doMelt(wiper: WiperImpl<*, *>, scaled: Boolean = false): Boolean {
        val lim = if (scaled) wiper.screenWidth / wiper.dupy else wiper.screenWidth
        var done = true
        val wiper_y = wiper.y!!
        while (wiper.ticks-- > 0) {
            for (i in 0 until lim) {
                // Column won't start yet.
                if (wiper_y[i] < 0) {
                    wiper_y[i]++
                    done = false
                } else if (wiper_y[i] < wiper.screenHeight) {
                    var dy =
                        if (wiper_y[i] < wiper.scaled_16) wiper_y[i] + (if (scaled) wiper.dupy else 1) else wiper.scaled_8
                    if (wiper_y[i] + dy >= wiper.screenHeight) dy = wiper.screenHeight - wiper_y[i]
                    var pd = wiper_y[i] * wiper.screenWidth + if (scaled) i * wiper.dupx else i

                    // MAES: this part should draw the END SCREEN "behind" the melt.
                    if (scaled) toScreenScaled(wiper, wiper.wipeEndScr, dy, pd, pd) else toScreen(
                        wiper.bufferType,
                        wiper.wipeScr!!,
                        wiper.wipeEndScr!!,
                        wiper.screenWidth,
                        dy,
                        pd,
                        pd
                    )
                    wiper_y[i] += dy
                    pd += dy * wiper.screenWidth

                    // This draws a column shifted by y[i]
                    if (scaled) toScreenScaled(
                        wiper,
                        wiper.wipeStartScr,
                        wiper.screenHeight - wiper_y[i],
                        i * wiper.dupy,
                        pd
                    ) else toScreen(
                        wiper.bufferType,
                        wiper.wipeScr!!,
                        wiper.wipeStartScr!!,
                        wiper.screenWidth,
                        wiper.screenHeight - wiper_y[i],
                        i,
                        pd
                    )
                    done = false
                }
            }
        }
        return done
    }

    fun exitMelt(wiper: WiperImpl<*, *>): Boolean {
        wiper.y = null //Z_Free(y);
        wiper.ticks = 0
        return false
    }
}