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
enum class QuoteType(val quoteChar: Char) {
    SINGLE('\''), DOUBLE('"');

    fun isQuoted(s: String): Boolean {
        return C2JUtils.isQuoted(s, quoteChar)
    }

    fun unQuote(s: String): String? {
        return C2JUtils.unquote(s, quoteChar)
    }

    companion object {
        fun getQuoteType(stringSource: String): Optional<QuoteType> {
            if (stringSource.length > 2) {
                for (type in QuoteType.values()) {
                    if (type.isQuoted(stringSource)) {
                        return Optional.of(type)
                    }
                }
            }
            return Optional.empty()
        }
    }
}