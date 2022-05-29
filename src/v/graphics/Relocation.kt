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
 * Relocation represents a move of a fixed length of bytes/shorts/ints
 * from one range in screen buffer to another range of the same size
 *
 * @author Good Sign
 */
class Relocation {
    var source = 0
    var destination = 0
    var length = 0

    constructor() {}
    constructor(source: Int, destination: Int, length: Int) {
        this.source = source
        this.destination = destination
        this.length = length
    }

    fun shift(amount: Int): Relocation {
        source += amount
        destination += amount
        return this
    }

    fun retarget(source: Int, destination: Int): Relocation {
        this.source = source
        this.destination = destination
        return this
    }
}