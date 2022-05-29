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
package m


import data.Defines
import data.mobjtype_t
import doom.SourceCode
import doom.SourceCode.M_Random
import p.ActiveStates
import utils.C2JUtils

/**
 * A "IRandom" that delegates its function to one of the two available IRandom implementations
 * By default, MochaDoom now uses JavaRandom, however it switches
 * to DoomRandom (supposedly Vanilla DOOM v1.9 compatible, tested only in Chocolate DOOM)
 * whenever you start recording or playing demo. When you start then new game, MochaDoom restores new JavaRandom.
 *
 * However, if you start MochaDoom with -javarandom command line argument and -record demo,
 * then MochaDoom will record the demo using JavaRandom. Such demo will be neither compatible
 * with Vanilla DOOM v1.9, nor with another source port.
 *
 * Only MochaDoom can play JavaRandom demos.
 * - Good Sign 2017/04/10
 *
 * @author Good Sign
 */
class DelegateRandom : IRandom {
    private var random: IRandom
    private var altRandom: IRandom? = null

    init {
        random = JavaRandom()
    }

    fun requireRandom(version: Int) {
        if (C2JUtils.flags(version, Defines.JAVARANDOM_MASK) && random is DoomRandom) {
            switchRandom(true)
        } else if (!C2JUtils.flags(version, Defines.JAVARANDOM_MASK) && random !is DoomRandom) {
            switchRandom(false)
        }
    }

    private fun switchRandom(which: Boolean) {
        val arandom = altRandom
        if (arandom != null && (!which && arandom is DoomRandom || which) && arandom is JavaRandom) {
            altRandom = random
            random = arandom
            print(String.format("M_Random: Switching to %s\n", random.javaClass.simpleName))
        } else {
            altRandom = random
            random = if (which) JavaRandom() else DoomRandom()
            print(String.format("M_Random: Switching to %s (new instance)\n", random.javaClass.simpleName))
        }
        //random.ClearRandom();
    }

    @M_Random.C(M_Random.P_Random)
    override fun P_Random(): Int {
        return random.P_Random()
    }

    @M_Random.C(M_Random.M_Random)
    override fun M_Random(): Int {
        return random.M_Random()
    }

    @M_Random.C(M_Random.M_ClearRandom)
    override fun ClearRandom() {
        random.ClearRandom()
    }

    override val index: Int
        get() = random.index

    override fun P_Random(caller: Int): Int {
        return random.P_Random(caller)
    }

    override fun P_Random(message: String?): Int {
        return random.P_Random(message)
    }

    override fun P_Random(caller: ActiveStates?, sequence: Int): Int {
        return random.P_Random(caller, sequence)
    }

    override fun P_Random(caller: ActiveStates?, type: mobjtype_t?, sequence: Int): Int {
        return random.P_Random(caller, type, sequence)
    }
}