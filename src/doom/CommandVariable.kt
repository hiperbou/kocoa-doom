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
package doom


import java.util.*

/**
 * A new way to define Command Line Arguments for the Engine
 *
 * @author Good Sign
 */
enum class CommandVariable(val prefix: Char, vararg arguments: Class<*>) {
    DISP(String::class.java), GEOM(Array<String>::class.java), CONFIG(Array<String>::class.java), TRANMAP(
        String::class.java
    ),
    PLAYDEMO(String::class.java), FASTDEMO(String::class.java), TIMEDEMO(
        String::class.java
    ),
    RECORD(String::class.java), STATCOPY(String::class.java), TURBO(Int::class.java), SKILL(
        Int::class.java
    ),
    EPISODE(Int::class.java), TIMER(Int::class.java), PORT(Int::class.java), MULTIPLY(Int::class.java), WIDTH(
        Int::class.java
    ),
    HEIGHT(Int::class.java), PARALLELRENDERER(Int::class.java, Int::class.java, Int::class.java), PARALLELRENDERER2(
        Int::class.java, Int::class.java, Int::class.java
    ),
    LOADGAME(Char::class.java), DUP(Char::class.java), NET(
        Char::class.java, Array<String>::class.java
    ),
    WART(Int::class.java, Int::class.java), WARP(
        WarpFormat::class.java
    ),
    MAP('+', MapFormat::class.java), FILE(Array<String>::class.java), IWAD(
        String::class.java
    ),
    NOVERT(ForbidFormat::class.java), NOVOLATILEIMAGE(ForbidFormat::class.java), AWTFRAME, DEBUGFILE, SHDEV, REGDEV, FRDMDEV, FR1DEV, FR2DEV, COMDEV, NOMONSTERS, RESPAWN, FAST, DEVPARM, ALTDEATH, DEATHMATCH, MILLIS, FASTTIC, CDROM, AVG, NODRAW, NOBLIT, NOPLAYPAL, NOCOLORMAP, SERIALRENDERER, EXTRATIC, NOMUSIC, NOSOUND, NOSFX, AUDIOLINES, SPEAKERSOUND, CLIPSOUND, CLASSICSOUND, INDEXED, HICOLOR, TRUECOLOR, ALPHATRUECOLOR, BLOCKMAP, SHOWFPS, JAVARANDOM, GREYPAL;

    val arguments: Array<Class<*>>

    init {
        this.arguments = arguments as Array<Class<*>>
    }

    constructor(vararg arguments: Class<*>) : this('-', *arguments) {}

    fun getType(): CommandVariable.Type {
        return if (arguments.size > 0) if (arguments[arguments.size - 1].isArray) CommandVariable.Type.VARARG else CommandVariable.Type.PARAMETER else CommandVariable.Type.SWITCH
    }

    enum class Type {
        PARAMETER, VARARG, SWITCH
    }

    interface WarpMetric {
        fun getEpisode(): Int
        fun getMap(): Int
    }

    class ForbidFormat(forbidString: String?) {
        private val isForbidden: Boolean

        init {
            isForbidden = "disable" == forbidString
        }

        override fun hashCode(): Int {
            var hash = 3
            hash = 67 * hash + if (isForbidden) 1 else 0
            return hash
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) {
                return true
            }
            if (obj == null) {
                return false
            }
            if (javaClass != obj.javaClass) {
                return false
            }
            val other = obj as ForbidFormat
            return isForbidden == other.isForbidden
        }

        companion object {
            var FORBID = ForbidFormat("disable")
            var ALLOW = ForbidFormat(null)
        }
    }

    class WarpFormat {
        val warpInt: Int

        constructor(warpInt: Int) {
            this.warpInt = warpInt
        }

        constructor(warpString: String) {
            val tryParse: Int
            tryParse = try {
                warpString.toInt()
            } catch (e: NumberFormatException) {
                // swallow exception. No warp.
                0
            }
            warpInt = tryParse
        }

        fun getMetric(commercial: Boolean): WarpMetric {
            return Metric(commercial)
        }

        private inner class Metric internal constructor(commercial: Boolean) : WarpMetric {
            var _episode = 0
            var _map = 0

            init {
                if (commercial) {
                    _episode = 1
                    _map = warpInt
                } else {
                    val evalInt = if (warpInt > 99) warpInt % 100 else warpInt
                    _episode = evalInt / 10
                    _map = evalInt % 10
                }
            }

            override fun getEpisode(): Int {
                return _episode
            }

            override fun getMap(): Int {
                return _map
            }
        }
    }

    class MapFormat(mapString: String) {
        val mapString: String

        init {
            this.mapString = mapString.lowercase(Locale.getDefault())
        }

        protected fun parseAsMapXX(): Int {
            if (mapString.length != 5 || mapString.lastIndexOf("map") != 0) {
                return -1 // Meh.
            }
            val map: Int
            map = try {
                mapString.substring(3).toInt()
            } catch (e: NumberFormatException) {
                return -1 // eww
            }
            return map
        }

        protected fun parseAsExMx(): Int {
            if (mapString.length != 4 || mapString[0] != 'e' || mapString[2] != 'm') {
                return -1 // Nah.
            }
            val episode = mapString[1]
            val mission = mapString[3]
            return if (episode < '0' || episode > '9' || mission < '0' || mission > '9') -1 else (episode.code - '0'.code) * 10 + (mission.code - '0'.code)
        }

        fun getMetric(commercial: Boolean): WarpMetric {
            val parse = if (commercial) parseAsMapXX() else parseAsExMx()
            return WarpFormat(Math.max(parse, 0)).getMetric(commercial)
        }
    }

    companion object {
        const val MIN_CVAR_LENGTH = 4
    }
}