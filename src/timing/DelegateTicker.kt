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
package timing

/**
 *
 * @author Good Sign
 */
class DelegateTicker : ITicker {
    private val ft = FastTicker()
    private val mt = MilliTicker()
    private val nt = NanoTicker()
    private var currentTicker: ITicker = ft
    override fun GetTime(): Int {
        return currentTicker.GetTime()
    }

    fun changeTicker() {
        if (currentTicker === nt) {
            currentTicker = mt
            (currentTicker as MilliTicker).basetime = 0
            (currentTicker as MilliTicker).oldtics = 0
        } else if (currentTicker === mt) {
            currentTicker = ft
            (currentTicker as FastTicker).fasttic = 0
        } else {
            currentTicker = nt
            (currentTicker as NanoTicker).basetime = 0
            (currentTicker as NanoTicker).oldtics = 0
        }
    }
}