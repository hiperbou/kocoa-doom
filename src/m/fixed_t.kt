package m

import data.Defines

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: fixed_t.java,v 1.14 2011/10/25 19:52:13 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
//
// DESCRIPTION:
//	Fixed point implementation.
//
//-----------------------------------------------------------------------------
//
// Fixed point, 32bit as 16.16.
//
// Most functionality of C-based ports is preserved, EXCEPT that there's
// no typedef of ints into fixed_t, and that there's no actual object fixed_t
// type that is actually instantiated in the current codebase, for performance reasons.
// There are still remnants of a full OO implementation that still do work, 
// and the usual FixedMul/FixedDiv etc. methods are still used throughout the codebase,
// but operate on int operants (signed, 32-bit integers).
class fixed_t : Comparable<fixed_t> {
    var `val` = 0

    constructor() {
        this.set(0)
    }

    fun get(): Int {
        return `val`
    }

    fun set(`val`: Int) {
        this.`val` = `val`
    }

    fun copy(a: fixed_t) {
        this.set(a.get())
    }

    fun equals(a: fixed_t): Boolean {
        return if (this.get() == a.get()) true else false
    }

    constructor(`val`: Int) {
        this.`val` = `val`
    }

    constructor(x: fixed_t) {
        `val` = x.`val`
    }

    /** In-place this=this*a
     *
     * @param a
     * @param b
     * @param c
     */
    fun FixedMul(a: fixed_t) {
        this.set((a.`val`.toLong() * `val`.toLong() shr FRACBITS).toInt())
    }

    override fun compareTo(o: fixed_t): Int {
        if (o.javaClass != fixed_t::class.java) return -1
        if (`val` == o.`val`) return 0
        return if (`val` > o.`val`) 1 else -1
    }

    operator fun compareTo(o: Int): Int {
        if (`val` == o) return 0
        return if (`val` > o) 1 else -1
    }

    fun add(a: fixed_t) {
        `val` += a.`val`
    }

    fun sub(a: fixed_t) {
        `val` -= a.`val`
    }

    fun add(a: Int) {
        `val` += a
    }

    fun sub(a: Int) {
        `val` -= a
    }

    /** Equals Zero
     *
     * @return
     */
    val isEZ: Boolean
        get() = `val` == 0

    /** Greater than Zero
     *
     * @return
     */
    val isGZ: Boolean
        get() = `val` > 0

    /** Less than Zero
     *
     * @return
     */
    val isLZ: Boolean
        get() = `val` < 0

    // These are here to make easier handling all those methods in R 
    // that return "1" or "0" based on one result.
    fun oneEZ(): Int {
        return if (`val` == 0) 1 else 0
    }

    fun oneGZ(): Int {
        return if (`val` > 0) 1 else 0
    }

    fun oneLZ(): Int {
        return if (`val` < 0) 1 else 0
    }

    companion object {
        const val FRACBITS = 16
        const val FRACUNIT = 1 shl FRACBITS
        const val MAPFRACUNIT = FRACUNIT / Defines.TIC_MUL
        fun equals(a: fixed_t, b: fixed_t): Boolean {
            return if (a.get() == b.get()) true else false
        }

        const val rcsid = "\$Id: fixed_t.java,v 1.14 2011/10/25 19:52:13 velktron Exp $"

        /** Creates a new fixed_t object for the result a*b
         *
         * @param a
         * @param b
         * @return
         */
        fun FixedMul(
            a: fixed_t,
            b: fixed_t
        ): Int {
            return (a.`val`.toLong() * b.`val`.toLong() ushr FRACBITS).toInt()
        }

        fun FixedMul(
            a: Int,
            b: fixed_t
        ): Int {
            return (a.toLong() * b.`val`.toLong() ushr FRACBITS).toInt()
        }

        fun FixedMul(
            a: Int,
            b: Int
        ): Int {
            return (a.toLong() * b.toLong() ushr FRACBITS).toInt()
        }

        /** Returns result straight as an int..
         *
         * @param a
         * @param b
         * @return
         */
        fun FixedMulInt(
            a: fixed_t,
            b: fixed_t
        ): Int {
            return (a.`val`.toLong() * b.`val`.toLong() shr FRACBITS).toInt()
        }

        /** In-place c=a*b
         *
         * @param a
         * @param b
         * @param c
         */
        fun FixedMul(
            a: fixed_t,
            b: fixed_t,
            c: fixed_t
        ) {
            c.set((a.`val`.toLong() * b.`val`.toLong() shr FRACBITS).toInt())
        }

        fun FixedDiv(
            a: Int,
            b: Int
        ): Int {
            return if (Math.abs(a) shr 14 >= Math.abs(b)) {
                if (a xor b < 0) Int.MIN_VALUE else Int.MAX_VALUE
            } else {
                val result: Long
                result = (a.toLong() shl 16) / b
                result.toInt()
            }
        }

        fun FixedDiv2(
            a: Int,
            b: Int
        ): Int {
            val c: Int
            c = ((a.toLong() shl 16) / b.toLong()).toInt()
            return c

            /*
    double c;

    c = ((double)a) / ((double)b) * FRACUNIT;

  if (c >= 2147483648.0 || c < -2147483648.0)
      throw new ArithmeticException("FixedDiv: divide by zero");
 
 return (int)c;*/
        }

        /** a+b
         *
         * @param a
         * @param b
         * @return
         */
        fun add(a: fixed_t, b: fixed_t): Int {
            return a.`val` + b.`val`
        }

        /** a-b
         *
         * @param a
         * @param b
         * @return
         */
        fun sub(a: fixed_t, b: fixed_t): Int {
            return a.`val` - b.`val`
        }

        /** c=a+b
         *
         * @param c
         * @param a
         * @param b
         */
        fun add(c: fixed_t, a: fixed_t, b: fixed_t) {
            c.`val` = a.`val` + b.`val`
        }

        /** c=a-b
         *
         * @param c
         * @param a
         * @param b
         */
        fun sub(c: fixed_t, a: fixed_t, b: fixed_t) {
            c.`val` = a.`val` - b.`val`
        }
    }
}