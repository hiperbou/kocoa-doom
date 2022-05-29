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

import m.Settings
import mochadoom.Engine
import mochadoom.Loggers
import rr.column_t
import rr.patch_t
import java.util.concurrent.ExecutionException
import java.util.concurrent.ForkJoinPool
import java.util.function.IntConsumer
import java.util.logging.Level.SEVERE
import java.util.stream.IntStream

/**
 * Patch columns drawing.
 * The whole class is my custom hand-crafted code
 * - Good Sign 2017/04/03
 *
 * @author Good Sign
 */
interface Columns<V, E : Enum<E>> : Blocks<V, E> {
    /**
     * We have to draw columns to the screen, not rows and is ineffective performance-wise because
     * System.arraycopy only speeds a lot row copying, where it only have to be called once
     */
    fun DrawColumn(screen: V, col: column_t, row: Horizontal, data: V?, scrWidth: Int, dupy: Int) {
        val fullRowShift = scrWidth * dupy

        /**
         * For each post, j is the index of post.
         *
         * A delta is a number of transparent rows to skip, if it is 0xFF then the whole column
         * is transparent, so if we have delta 0xFF, then we've done with column drawing.
         */
        var j = 0
        var delta = 0
        while (j < col.posts && col.postdeltas[j].toInt() != 0xFF) {

            // shift a row down by difference of current and previous delta with respect to scaling
            row.shift(point(0, (-delta + col.postdeltas[j].also { delta = it.toInt() }) * dupy, scrWidth))
            val saveRowStart = row.start

            /**
             * For each pixel in the post: p is a position of pixel in the column's data,
             * column.postlen[j] is how many pixels tall is the post (a vertical string of pixels)
             */
            var p = 0
            while (p < col.postlen[j]) {

                // Fill first line of rect
                screenSet(data!!, col.postofs[j] + p, screen, row)
                // Fill the rest of the rect
                RepeatRow(screen, row, dupy - 1)
                ++p
                row.shift(fullRowShift)
            }
            row.start = saveRowStart
            ++j
        }
    }

    /**
     * Accepts patch columns drawing arguments (usually from Patches::DrawPatch method)
     * and submits the task to the local ForkJoinPool. The task iterates over patch columns in parallel.
     * We need to only iterate through real patch.width and perform scale in-loop
     */
    fun DrawPatchColumns(screen: V, patch: patch_t, x: Int, y: Int, dupx: Int, dupy: Int, flip: Boolean) {
        val scrWidth = getScreenWidth()
        val task = IntConsumer { i: Int ->
            val startPoint = point(x + i * dupx, y, scrWidth)
            val column = if (flip) patch.columns[patch.width - 1 - i]!! else patch.columns[i]!!
            DrawColumn(
                screen, column, Horizontal(startPoint, dupx),
                convertPalettedBlock(*column.data), scrWidth, dupy
            )
        }
        /**
         * As vanilla DOOM does not parallel column computation, we should have the option to turn off
         * the parallelism. Just set it to 0 in cfg:parallelism_patch_columns, and it will process columns in serial.
         *
         * It will also prevent a crash on a dumb negative value set to this option. However, a value of 1000 is even
         * more dumb, but will probably not crash - just take hellion of megabytes memory and waste all the CPU time on
         * computing "what to process" instead of "what will be the result"
         */
        if (U.COLUMN_THREADS > 0) try {
            U.pool!!.submit { IntStream.range(0, patch.width.toInt()).parallel().forEach(task) }.get()
        } catch (ex: InterruptedException) {
            Loggers.getLogger(Columns::class.java.name).log(SEVERE, null, ex)
        } catch (ex: ExecutionException) {
            Loggers.getLogger(Columns::class.java.name).log(SEVERE, null, ex)
        } else for (i in 0 until patch.width) {
            task.accept(i)
        }
    }

    object U {
        val COLUMN_THREADS: Int =
            Engine.getConfig().getValue(Settings.parallelism_patch_columns, Int::class.java)!!
        val pool = if (COLUMN_THREADS > 0) ForkJoinPool(COLUMN_THREADS) else null
    }
}