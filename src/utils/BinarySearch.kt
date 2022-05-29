package utils

import utils.BinarySearch.*
import java.util.function.*
import java.util.function.Function
import s.*
import s.DoomIO.Endian
import utils.C2JUtils
import java.nio.ByteOrder
import data.Tables.BITS32;
import data.Tables.finecosine;
import data.Tables.finesine;
import data.info.mobjinfo;
import data.mobjtype_t;
import doom.SourceCode.angle_t;
import m.fixed_t.Companion.FRACBITS;
import m.fixed_t.Companion.FRACUNIT;
import m.fixed_t.Companion.FixedMul;
import m.fixed_t.Companion.FixedDiv
import p.MapUtils.AproxDistance;
import p.mobj_t;
import utils.C2JUtils.eval;
import doom.player_t;
import doom.weapontype_t;
import m.fixed_t.Companion.MAPFRACUNIT;
import doom.SourceCode
import java.nio.ByteBuffer
import m.BBox
import doom.DoomMain
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

enum class BinarySearch {;

    fun interface LongComparator {
        fun compareAsLong(f1: Long, f2: Long): Int
    }

    fun interface DoubleComparator {
        fun compareAsDouble(f1: Double, f2: Double): Int
    }

    fun interface LongGetter {
        fun getAsLong(i: Int): Long
    }

    fun interface DoubleGetter {
        fun getAsDouble(i: Int): Double
    }

    companion object {
        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param list of one type of objects
         * @param converter from one type of objects to another
         * @param key a value of another object type
         * @return
         */
        fun <T, E : Comparable<E>?> find(
            list: List<T?>,
            converter: Function<in T?, out E>,
            key: E
        ): Int {
            return BinarySearch.find(list, converter, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param array of one type of objects
         * @param converter from one type of objects to another
         * @param key a value of another object type
         * @return
         */
        fun <T, E : Comparable<E>?> find(
            array: Array<T>,
            converter: Function<in T, out E>,
            key: E
        ): Int {
            return BinarySearch.find<T, E>(array, converter, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param list of one type of objects
         * @param comparator - a comparator for objects of type E
         * @param converter from one type of objects to another
         * @param key a value of another object type
         * @return
         */
        fun <T, E> find(
            list: List<T?>,
            converter: Function<in T?, out E>,
            comparator: Comparator<in E>,
            key: E
        ): Int {
            return BinarySearch.find(list, converter, comparator, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param array of one type of objects
         * @param comparator - a comparator for objects of type E
         * @param converter from one type of objects to another
         * @param key a value of another object type
         * @return
         */
        fun <T, E> find(
            array: Array<T>,
            converter: Function<in T, out E>,
            comparator: Comparator<in E>,
            key: E
        ): Int {
            return BinarySearch.find<T, E>(array, converter, comparator, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            list: List<T?>,
            converter: ToIntFunction<in T?>,
            key: Int
        ): Int {
            return BinarySearch.findByInt(list, converter, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            array: Array<T>,
            converter: ToIntFunction<in T>,
            key: Int
        ): Int {
            return BinarySearch.findByInt<T>(array, converter, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param comparator - a comparator for primitive integer values
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            list: List<T?>,
            converter: ToIntFunction<in T?>,
            comparator: IntBinaryOperator,
            key: Int
        ): Int {
            return BinarySearch.findByInt(list, converter, comparator, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param comparator - a comparator for primitive integer values
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            array: Array<T>,
            converter: ToIntFunction<in T>,
            comparator: IntBinaryOperator,
            key: Int
        ): Int {
            return BinarySearch.findByInt<T>(array, converter, comparator, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            list: List<T?>,
            converter: ToLongFunction<in T?>,
            key: Long
        ): Int {
            return BinarySearch.findByLong(list, converter, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            array: Array<T>,
            converter: ToLongFunction<in T>,
            key: Long
        ): Int {
            return BinarySearch.findByLong<T>(array, converter, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param comparator - a comparator for primitive long values
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            list: List<T?>,
            converter: ToLongFunction<in T?>,
            comparator: LongComparator,
            key: Long
        ): Int {
            return BinarySearch.findByLong(list, converter, comparator, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param comparator - a comparator for primitive long values
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            array: Array<T>,
            converter: ToLongFunction<in T>,
            comparator: LongComparator,
            key: Long
        ): Int {
            return BinarySearch.findByLong<T>(array, converter, comparator, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            list: List<T?>,
            converter: ToDoubleFunction<in T?>,
            key: Double
        ): Int {
            return BinarySearch.findByDouble(list, converter, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            array: Array<T>,
            converter: ToDoubleFunction<in T>,
            key: Double
        ): Int {
            return BinarySearch.findByDouble<T>(array, converter, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param comparator - a comparator for primitive double values
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            list: List<T?>,
            converter: ToDoubleFunction<in T?>,
            comparator: DoubleComparator,
            key: Double
        ): Int {
            return BinarySearch.findByDouble(list, converter, comparator, 0, list.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param comparator - a comparator for primitive double values
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            array: Array<T>,
            converter: ToDoubleFunction<in T>,
            comparator: DoubleComparator,
            key: Double
        ): Int {
            return BinarySearch.findByDouble<T>(array, converter, comparator, 0, array.size, key)
        }

        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param list of one type of objects
         * @param converter from one type of objects to another
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key a value of another object type
         * @return
         */
        fun <T, E : Comparable<E>?> find(
            list: List<T?>?,
            converter: Function<in T?, out E>,
            from: Int, to: Int, key: E
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                converter.apply(getter.apply(i))!!.compareTo(key)
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param array of one type of objects
         * @param converter from one type of objects to another
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key a value of another object type
         * @return
         */
        fun <T, E : Comparable<E>?> find(
            array: Array<T>,
            converter: Function<in T, out E>,
            from: Int, to: Int, key: E
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                converter.apply(array[i])!!.compareTo(key)
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param list of one type of objects
         * @param converter from one type of objects to another
         * @param comparator - a comparator for objects of type E
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key a value of another object type
         * @return
         */
        fun <T, E> find(
            list: List<T?>?,
            converter: Function<in T?, out E>,
            comparator: Comparator<in E>,
            from: Int, to: Int, key: E
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compare(
                    converter.apply(
                        getter.apply(i)
                    ), key
                )
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using object of another type, given from any object of one type
         * a function can get an object of another type
         *
         * @param array of one type of objects
         * @param converter from one type of objects to another
         * @param comparator - a comparator for objects of type E
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key a value of another object type
         * @return
         */
        fun <T, E> find(
            array: Array<T>,
            converter: Function<in T, out E>,
            comparator: Comparator<in E>,
            from: Int, to: Int, key: E
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compare(
                    converter.apply(
                        array[i]
                    ), key
                )
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            list: List<T?>?,
            converter: ToIntFunction<in T?>,
            from: Int, to: Int, key: Int
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByInt(
                IntUnaryOperator { i: Int -> converter.applyAsInt(getter.apply(i)) },
                from,
                to,
                key
            )
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            array: Array<T>,
            converter: ToIntFunction<in T>,
            from: Int, to: Int, key: Int
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByInt(
                IntUnaryOperator { i: Int -> converter.applyAsInt(array[i]) },
                from,
                to,
                key
            )
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param comparator - a comparator for primitive integer values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            list: List<T?>?,
            converter: ToIntFunction<in T?>,
            comparator: IntBinaryOperator,
            from: Int, to: Int, key: Int
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.applyAsInt(
                    converter.applyAsInt(
                        getter.apply(i)
                    ), key
                )
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive integer, given from any object
         * of one type a function can get a primitive integer
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive integer
         * @param comparator - a comparator for primitive integer values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive integer key value
         * @return
         */
        fun <T> findByInt(
            array: Array<T>,
            converter: ToIntFunction<in T>,
            comparator: IntBinaryOperator,
            from: Int, to: Int, key: Int
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.applyAsInt(
                    converter.applyAsInt(
                        array[i]
                    ), key
                )
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            list: List<T?>?,
            converter: ToLongFunction<in T?>,
            from: Int, to: Int, key: Long
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByLong(
                LongGetter { i: Int -> converter.applyAsLong(getter.apply(i)) },
                from,
                to,
                key
            )
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            array: Array<T>,
            converter: ToLongFunction<in T>,
            from: Int, to: Int, key: Long
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByLong(
                LongGetter { i: Int -> converter.applyAsLong(array[i]) },
                from,
                to,
                key
            )
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param comparator - a comparator for primitive long values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            list: List<T?>?,
            converter: ToLongFunction<in T?>,
            comparator: LongComparator,
            from: Int, to: Int, key: Long
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compareAsLong(
                    converter.applyAsLong(
                        getter.apply(i)
                    ), key
                )
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive long, given from any object
         * of one type a function can get a primitive long
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive long
         * @param comparator - a comparator for primitive long values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive long key value
         * @return
         */
        fun <T> findByLong(
            array: Array<T>,
            converter: ToLongFunction<in T>,
            comparator: LongComparator,
            from: Int, to: Int, key: Long
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compareAsLong(
                    converter.applyAsLong(
                        array[i]
                    ), key
                )
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            list: List<T?>?,
            converter: ToDoubleFunction<in T?>,
            from: Int, to: Int, key: Double
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByDouble(
                DoubleGetter { i: Int -> converter.applyAsDouble(getter.apply(i)) },
                from,
                to,
                key
            )
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            array: Array<T>,
            converter: ToDoubleFunction<in T>,
            from: Int, to: Int, key: Double
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByDouble(
                DoubleGetter { i: Int -> converter.applyAsDouble(array[i]) },
                from,
                to,
                key
            )
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param list of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param comparator - a comparator for primitive double values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            list: List<T?>?,
            converter: ToDoubleFunction<in T?>,
            comparator: DoubleComparator,
            from: Int, to: Int, key: Double
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compareAsDouble(
                    converter.applyAsDouble(
                        getter.apply(i)
                    ), key
                )
            }, from, to)
        }

        /**
         * Binary search supporting search for one type of objects
         * using primitive double, given from any object
         * of one type a function can get a primitive double
         *
         * @param array of one type of objects
         * @param converter from one type of objects to a primitive double
         * @param comparator - a comparator for primitive double values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param a primitive double key value
         * @return
         */
        fun <T> findByDouble(
            array: Array<T>,
            converter: ToDoubleFunction<in T>,
            comparator: DoubleComparator,
            from: Int, to: Int, key: Double
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compareAsDouble(
                    converter.applyAsDouble(
                        array[i]
                    ), key
                )
            }, from, to)
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by some key object, using the getter
         * who, given an index in the invisible structure, can produce a key
         * object someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a key object used for sort
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a key object
         */
        fun <E : Comparable<E>?> find(
            getter: IntFunction<out E>,
            from: Int, to: Int, key: E
        ): Int {
            return BinarySearch.findByIndex(
                IntUnaryOperator { i: Int -> getter.apply(i)!!.compareTo(key) },
                from,
                to
            )
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by some key object, using the getter
         * who, given an index in the invisible structure, can produce a key
         * object someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a key object used for sort
         * @param comparator - a comparator for objects of type E
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a key object
         */
        fun <E> find(
            getter: IntFunction<out E>,
            comparator: Comparator<in E>,
            from: Int, to: Int, key: E
        ): Int {
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compare(
                    getter.apply(i),
                    key
                )
            }, from, to)
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by primitive integer key,
         * using the getter who, given an index in the invisible structure, can produce
         * the primitive integer key someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a primitive integer used for sort
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a primitive integer key
         */
        fun findByInt(
            getter: IntUnaryOperator,
            from: Int, to: Int, key: Int
        ): Int {
            return BinarySearch.findByInt(
                getter,
                IntBinaryOperator { x: Int, y: Int -> Integer.compare(x, y) },
                from,
                to,
                key
            )
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by primitive integer key,
         * using the getter who, given an index in the invisible structure, can produce
         * the primitive integer key someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a primitive integer used for sort
         * @param comparator - a comparator for primitive integers
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a primitive integer key
         */
        fun findByInt(
            getter: IntUnaryOperator,
            comparator: IntBinaryOperator,
            from: Int, to: Int, key: Int
        ): Int {
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.applyAsInt(
                    getter.applyAsInt(
                        i
                    ), key
                )
            }, from, to)
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by primitive long key,
         * using the getter who, given an index in the invisible structure, can produce
         * the primitive long key someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a primitive long used for sort
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a primitive long key
         */
        fun findByLong(
            getter: LongGetter,
            from: Int, to: Int, key: Long
        ): Int {
            return BinarySearch.findByLong(
                getter,
                LongComparator { x: Long, y: Long -> java.lang.Long.compare(x, y) },
                from,
                to,
                key
            )
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by primitive long key,
         * using the getter who, given an index in the invisible structure, can produce
         * the primitive long key someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a primitive long used for sort
         * @param comparator - a comparator for primitive long values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a primitive long key
         */
        fun findByLong(
            getter: LongGetter,
            comparator: LongComparator,
            from: Int, to: Int, key: Long
        ): Int {
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compareAsLong(
                    getter.getAsLong(
                        i
                    ), key
                )
            }, from, to)
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by primitive double key,
         * using the getter who, given an index in the invisible structure, can produce
         * the primitive double key someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a primitive double used for sort
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a primitive double key
         */
        fun findByDouble(
            getter: DoubleGetter,
            from: Int, to: Int, key: Double
        ): Int {
            return BinarySearch.findByDouble(
                getter,
                DoubleComparator { d1: Double, d2: Double -> java.lang.Double.compare(d1, d2) },
                from,
                to,
                key
            )
        }

        /**
         * Blind binary search, presuming there is some sorted structure,
         * whose sorting is someway ensured by primitive double key,
         * using the getter who, given an index in the invisible structure, can produce
         * the primitive double key someway used to sort it.
         *
         * @param getter - a function accepting indexes, producing a primitive double used for sort
         * @param comparator - a comparator for primitive double values
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         * @param key - a primitive double key
         */
        fun findByDouble(
            getter: DoubleGetter,
            comparator: DoubleComparator,
            from: Int, to: Int, key: Double
        ): Int {
            return BinarySearch.findByIndex(IntUnaryOperator { i: Int ->
                comparator.compareAsDouble(
                    getter.getAsDouble(
                        i
                    ), key
                )
            }, from, to)
        }

        /**
         * Blind binary search applying array elements to matching function until it returns 0
         * @param list of one type of objects
         * @param matcher - a matcher returning comparison result based on single list element
         */
        fun <T> findByMatch(
            array: Array<T>,
            matcher: ToIntFunction<in T>
        ): Int {
            return BinarySearch.findByMatch<T>(array, matcher, 0, array.size)
        }

        /**
         * Blind binary search applying List elements to matching function until it returns 0
         * @param list of one type of objects
         * @param matcher - a matcher returning comparison result based on single list element
         */
        fun <T> findByMatch(
            list: List<T?>,
            matcher: ToIntFunction<in T?>
        ): Int {
            return BinarySearch.findByMatch(list, matcher, 0, list.size)
        }

        /**
         * Blind binary search applying array elements to matching function until it returns 0
         * @param list of one type of objects
         * @param matcher - a matcher returning comparison result based on single list element
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         */
        fun <T> findByMatch(
            array: Array<T>,
            matcher: ToIntFunction<in T>,
            from: Int,
            to: Int
        ): Int {
            BinarySearch.rangeCheck(array.size, from, to)
            return BinarySearch.findByIndex(
                IntUnaryOperator { i: Int -> matcher.applyAsInt(array[i]) },
                from,
                to
            )
        }

        /**
         * Blind binary search applying List elements to matching function until it returns 0
         * @param list of one type of objects
         * @param matcher - a matcher returning comparison result based on single list element
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         */
        fun <T> findByMatch(
            list: List<T?>?,
            matcher: ToIntFunction<in T?>,
            from: Int,
            to: Int
        ): Int {
            val getter: IntFunction<out T?> = BinarySearch.listGetter(list)
            return BinarySearch.findByIndex(
                IntUnaryOperator { i: Int -> matcher.applyAsInt(getter.apply(i)) },
                from,
                to
            )
        }

        /**
         * Blind binary search applying index to comparison function until it returns 0
         * @param comparator - index-comparing function
         * @param from - an index (inclusive) from which to start search
         * @param to - an index (exclusive) from which to start search
         */
        fun findByIndex(comparator: IntUnaryOperator, from: Int, to: Int): Int {
            var low = from
            var high = to - 1
            while (low <= high) {
                val mid = low + high ushr 1
                val cmp = comparator.applyAsInt(mid)
                if (cmp < 0) low = mid + 1 else if (cmp > 0) high = mid - 1 else return mid // key found
            }
            return -(low + 1) // key not found
        }

        /**
         * A copy of Arrays.rangeCheck private method from JDK
         */
        private fun rangeCheck(arrayLength: Int, fromIndex: Int, toIndex: Int) {
            require(fromIndex <= toIndex) { "fromIndex($fromIndex) > toIndex($toIndex)" }
            if (fromIndex < 0) throw ArrayIndexOutOfBoundsException(fromIndex)
            if (toIndex > arrayLength) throw ArrayIndexOutOfBoundsException(toIndex)
        }

        /**
         * A copy of Collections.get private method from JDK
         */
        private operator fun <T> get(i: ListIterator<T>, index: Int): T? {
            var obj: T? = null
            var pos = i.nextIndex()
            if (pos <= index) do obj = i.next() while (pos++ < index) else do obj = i.previous() while (--pos > index)
            return obj
        }

        private fun <T, L : List<T>?> listGetter(list: L): IntFunction<out T?> {
            if (list is RandomAccess) return IntFunction { index: Int -> list[index] }
            val it = list!!.listIterator()
            return IntFunction { i: Int -> BinarySearch.get(it, i) }
        }
    }
}