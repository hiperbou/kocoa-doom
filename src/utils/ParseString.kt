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
package utils


import java.util.*

/**
 * @author Good Sign
 */
enum class ParseString {
    ;

    companion object {
        fun parseString(stringSource: String): Any {
            var stringSource = stringSource
            val qt: Optional<QuoteType> = QuoteType.getQuoteType(stringSource)
            val quoted = qt.isPresent
            if (quoted) {
                stringSource = qt.get().unQuote(stringSource)!!
            }
            if (quoted && stringSource.length == 1) {
                val test = stringSource[0]
                if (test.code >= 0 && test.code < 255) {
                    return test
                }
            }
            var ret: Optional<out Any> = ParseString.checkInt(stringSource)
            if (!ret.isPresent) {
                ret = ParseString.checkDouble(stringSource)
                if (!ret.isPresent) {
                    ret = ParseString.checkBoolean(stringSource) //TODO: this returns false with an invaild string
                    if (!ret.isPresent) {
                        return stringSource
                    }
                }
            }
            return ret.get()
        }

        fun checkInt(stringSource: String): Optional<Any> {
            var ret: Optional<Any?>
            try {
                val longRet = stringSource.toLong()
                return if (longRet < Int.MAX_VALUE) Optional.of(longRet.toInt()) else Optional.of(longRet)
            } catch (e: NumberFormatException) {
            }
            try {
                val longRet = java.lang.Long.decode(stringSource)
                return if (longRet < Int.MAX_VALUE) Optional.of(longRet.toInt()) else Optional.of(longRet)
            } catch (e: NumberFormatException) {
            }
            return Optional.empty()
        }

        fun checkDouble(stringSource: String): Optional<Double> {
            try {
                return Optional.of(stringSource.toDouble())
            } catch (e: NumberFormatException) {
            }
            return Optional.empty()
        }

        fun checkBoolean(stringSource: String?): Optional<Boolean> {
            try {
                return Optional.of(java.lang.Boolean.parseBoolean(stringSource))
            } catch (e: NumberFormatException) {
            }
            return if ("false".compareTo(stringSource!!, ignoreCase = true) == 0) {
                Optional.of(java.lang.Boolean.FALSE)
            } else Optional.empty()
        }
    }
}