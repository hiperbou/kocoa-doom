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
import java.util.function.IntFunction
import java.util.function.Supplier

object GenericCopy {
    private val BOOL_0 = booleanArrayOf(false)
    private val BYTE_0 = byteArrayOf(0)
    private val SHORT_0 = shortArrayOf(0)
    private val CHAR_0 = charArrayOf(0.toChar())
    private val INT_0 = intArrayOf(0)
    private val FLOAT_0 = floatArrayOf(0f)
    private val LONG_0 = longArrayOf(0)
    private val DOUBLE_0 = doubleArrayOf(0.0)
    fun memset(array: LongArray?, start: Int, length: Int, vararg value: Long) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.LONG_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun memset(array: IntArray?, start: Int, length: Int, vararg value: Int) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.INT_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun memset(array: ShortArray?, start: Int, length: Int, vararg value: Short) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.SHORT_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun memset(array: CharArray?, start: Int, length: Int, vararg value: Char) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.CHAR_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun memset(array: ByteArray?, start: Int, length: Int, vararg value: Byte) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.BYTE_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun memset(array: DoubleArray?, start: Int, length: Int, vararg value: Double) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.DOUBLE_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun memset(array: FloatArray?, start: Int, length: Int, vararg value: Float) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.FLOAT_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun memset(array: BooleanArray?, start: Int, length: Int, vararg value: Boolean) {
        var value = value
        if (length > 0) {
            if (value.size == 0) {
                value = GenericCopy.BOOL_0
            }
            System.arraycopy(value, 0, array, start, value.size)
            var i = value.size
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun <T> memset(array: T, start: Int, length: Int, value: T, valueStart: Int, valueLength: Int) {
        if (length > 0 && valueLength > 0) {
            System.arraycopy(value, valueStart, array, start, valueLength)
            var i = valueLength
            while (i < length) {
                System.arraycopy(array, start, array, start + i, if (length - i < i) length - i else i)
                i += i
            }
        }
    }

    fun <T> memcpy(srcArray: T, srcStart: Int, dstArray: T, dstStart: Int, length: Int) {
        System.arraycopy(srcArray, srcStart, dstArray, dstStart, length)
    }

    //fun <T> malloc(supplier: ()->T, generator: IntFunction<Array<T>>, length: Int): Array<T> {
    inline fun <reified T> malloc(supplier: ()->T, length: Int): Array<T> {
        return  Array(length) { supplier() }
    }

    inline fun <reified T> malloc(supplier: ArraySupplier<T>, length: Int): Array<T> {
        return  Array(length) { supplier.getWithInt(length) }
    }

    fun <T> malloc(supplier: ArraySupplier<T>, generator: IntFunction<Array<T>?>, length: Int): Array<T>? {
        val array = generator.apply(length)
        Arrays.setAll(array) { ignoredInt: Int -> supplier.getWithInt(ignoredInt) }
        return array
    }


    fun interface ArraySupplier<T> : Supplier<T> {
        fun getWithInt(ignoredInt: Int): T {
            return get()
        }
    }
}