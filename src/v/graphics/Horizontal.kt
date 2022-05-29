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

/**
 * Horizontal represents a range from a screen buffer (byte or short or int array)
 *
 * @author Good Sign
 */
class Horizontal {
    var start = 0
    var length = 0

    constructor() {}
    constructor(start: Int, length: Int) {
        this.start = start
        this.length = length
    }

    fun relocate(amount: Int): Relocation {
        return Relocation(start, start + amount, length)
    }

    fun shift(amount: Int) {
        start += amount
    }
}