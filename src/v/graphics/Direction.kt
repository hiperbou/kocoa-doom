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

import java.util.*

/**
 *
 * @author Good Sign
 */
enum class Direction {
    LEFT_UP, UP, RIGHT_UP,  /*  \         ||         /   */ /*    \       ||       /     */ /*      \     ||     /       */ /*        \   ||   /         */
    LEFT,  /*===*/
    CENTER,  /*===*/
    RIGHT,  /*        /   ||   \         */ /*      /     ||     \       */ /*    /       ||       \     */ /*  /         ||         \   */
    LEFT_DOWN, DOWN, RIGHT_DOWN;

    /**
     * Categorization constants
     */
    // LEFT_UP, UP, RIGHT_UP
    val hasTop = ordinal < 3

    // LEFT_UP, LEFT, LEFT_DOWN
    val hasLeft = ordinal % 3 == 0

    // RIGHT_UP, RIGHT_ RIGHT_DOWN
    val hasRight = ordinal % 3 == 2

    // LEFT_DOWN, DOWN, RIGHT_DOWN
    val hasBottom = ordinal > 5

    // UP, LEFT, RIGHT, DOWN
    val straight = ordinal % 2 != 0
    fun isAdjacent(dir: Direction): Boolean {
        return straight xor dir.straight
    }

    /**
     * Conversions
     */
    operator fun next(): Direction {
        return if (this == Direction.RIGHT_DOWN) Direction.LEFT_UP else Direction.directions.get(ordinal + 1)
    }

    fun opposite(): Direction {
        return when (this) {
            Direction.LEFT_UP -> Direction.RIGHT_DOWN
            Direction.UP -> Direction.DOWN
            Direction.RIGHT_UP -> Direction.LEFT_DOWN
            Direction.LEFT -> Direction.RIGHT
            Direction.RIGHT -> Direction.LEFT
            Direction.LEFT_DOWN -> Direction.RIGHT_UP
            Direction.DOWN -> Direction.UP
            Direction.RIGHT_DOWN -> Direction.LEFT_UP
            else -> this
        }
    }

    fun rotationHor(sign: Int): Direction {
        return if (sign == 0) this else when (this) {
            Direction.LEFT_UP -> if (sign > 0) Direction.UP else Direction.LEFT
            Direction.UP -> if (sign > 0) Direction.RIGHT_UP else Direction.LEFT_UP
            Direction.RIGHT_UP -> if (sign > 0) Direction.RIGHT else Direction.UP
            Direction.LEFT -> if (sign > 0) Direction.CENTER else this
            Direction.RIGHT -> if (sign > 0) Direction.CENTER else this
            Direction.LEFT_DOWN -> if (sign > 0) Direction.DOWN else Direction.LEFT
            Direction.DOWN -> if (sign > 0) Direction.RIGHT_DOWN else Direction.LEFT_DOWN
            Direction.RIGHT_DOWN -> if (sign > 0) Direction.RIGHT else Direction.DOWN
            else -> if (sign > 0) Direction.RIGHT else Direction.LEFT
        }
    }

    fun rotationVert(sign: Int): Direction {
        return if (sign == 0) this else when (this) {
            Direction.LEFT_UP -> if (sign > 0) Direction.LEFT else Direction.UP
            Direction.UP -> if (sign > 0) Direction.CENTER else this
            Direction.RIGHT_UP -> if (sign > 0) Direction.RIGHT else Direction.UP
            Direction.LEFT -> if (sign > 0) Direction.LEFT_DOWN else Direction.LEFT_UP
            Direction.RIGHT -> if (sign > 0) Direction.RIGHT_DOWN else Direction.RIGHT_UP
            Direction.LEFT_DOWN -> if (sign > 0) Direction.DOWN else Direction.LEFT
            Direction.DOWN -> if (sign < 0) Direction.CENTER else this
            Direction.RIGHT_DOWN -> if (sign > 0) Direction.DOWN else Direction.RIGHT
            else -> if (sign > 0) Direction.DOWN else Direction.UP
        }
    }

    fun rotation(signX: Int, signY: Int): Direction {
        val rotX = rotationHor(signX)
        val rotY = rotationHor(signY)
        if (rotX.isAdjacent(rotY)) {
            if (signX > 0 && signY > 0) return Direction.RIGHT_DOWN else if (signX > 0 && signY < 0) return Direction.RIGHT_UP else if (signX < 0 && signY > 0) return Direction.LEFT_DOWN else if (signX < 0 && signY < 0) return Direction.LEFT_UP
        }

        // otherwise, 2nd takes precedence
        return rotY
    }

    companion object {
        val directions = Collections.unmodifiableList(Arrays.asList(*Direction.values()))
    }
}