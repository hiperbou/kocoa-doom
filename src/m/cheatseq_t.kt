package m


// -----------------------------------------------------------------------------
//
// $Id: cheatseq_t.java,v 1.8 2011/11/01 23:47:50 velktron Exp $
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// $Log: cheatseq_t.java,v $
// Revision 1.8  2011/11/01 23:47:50  velktron
// Added constructor method to start from unscrambled strings.
//
// Revision 1.7  2011/05/06 14:00:54  velktron
// More of _D_'s changes committed.
//
// Revision 1.6  2010/12/14 00:53:32  velktron
// Some input sanitizing. Far from perfect but heh...
//
// Revision 1.5  2010/08/25 00:50:59  velktron
// Some more work...
//
// Revision 1.4  2010/07/21 11:41:47  velktron
// Work on menus...
//
// Revision 1.3 2010/07/20 15:52:56 velktron
// LOTS of changes, Automap almost complete. Use of fixed_t inside methods
// severely limited.
//
// Revision 1.2 2010/07/03 23:24:13 velktron
// Added a LOT of stuff, like Status bar code & objects. Now we're cooking with
// gas!
//
// Revision 1.1 2010/06/30 08:58:50 velktron
// Let's see if this stuff will finally commit....
//
//
// Most stuff is still being worked on. For a good place to start and get an
// idea of what is being done, I suggest checking out the "testers" package.
//
// Revision 1.1 2010/06/29 11:07:34 velktron
// Release often, release early they say...
//
// Commiting ALL stuff done so far. A lot of stuff is still broken/incomplete,
// and there's still mixed C code in there. I suggest you load everything up in
// Eclpise and see what gives from there.
//
// A good place to start is the testers/ directory, where you can get an idea of
// how a few of the implemented stuff works.
//
//
// DESCRIPTION:
// Cheat sequence checking.
//
// -----------------------------------------------------------------------------
/**
 * Cheat sequence checking. MAES: all of this stuff used to be in cheat.h and
 * cheat.c, but seeing how all manipulation is done on "cheatseq_t" objects, it
 * makes more sense to move this functionality in here, and go OO all the way.
 * So away with the fugly static methods!!!
 */
class cheatseq_t {
    // This holds the actual data (was a char*).
    var sequence: CharArray

    // This is used as a pointer inside sequence.
    // Was a char*, but in Java it makes more sense to have it as an int.
    var p = 0

    constructor(sequence: CharArray, p: Int) {
        this.sequence = sequence
        this.p = p
    }

    constructor(sequence: CharArray) {
        this.sequence = sequence
        p = 0
    }

    constructor(sequence: String, prescrambled: Boolean) {
        if (prescrambled) {
            this.sequence = sequence.toCharArray()
            p = 0
        } else {
            this.sequence = scrambleString(sequence)
            p = 0
        }
    }

    /**
     * This was in cheat.c, but makes more sense to be used as an
     * initializer/constructor.
     */
    fun GetParam(buffer: CharArray) {

        // char[] p;
        var c: Char
        var ptr = 0

        // p = this.sequence;
        // Increments pointer until the sequence reaches its first internal "1"
        // ???
        while (sequence[ptr++].code != 1);
        var bptr = 0
        // Now it copies the contents of this cheatseq_t into buffer...and nils
        // it???
        do {
            c = sequence[ptr]
            buffer[bptr++] = c
            sequence[ptr++] = 0.toChar()
        } while (c.code != 0 && sequence[ptr].code != 0xff)
        if (sequence[ptr].code == 0xff) buffer[bptr] = 0.toChar()
    }

    /**
     * Called in st_stuff module, which handles the input. Returns true if the
     * cheat was successful, false if failed. MAES: Let's make this boolean.
     *
     * @param cht
     * @param key
     * @return
     */
    fun CheckCheat(cht: cheatseq_t, key: Int): Boolean {
        var rc = false
        if (cht.p < 0) cht.p = 0 // initialize if first time
        if (cht.p == 0) // This actually points inside "sequence"
        // *(cht->p++) = key;
            cht.sequence[cht.p++] =
                key.toChar() else if (cheat_xlate_table[key.toChar().code] == cht.sequence[cht.p]) cht.p++ else  // Failure: back to the beginning.
            cht.p = 0
        if (cht.sequence[cht.p].code == 1) cht.p++ else if (cht.sequence[cht.p].code == 0xff) // end of sequence character
        {
            cht.p = 0
            rc = true
        }
        return rc
    }

    /**
     * Called in st_stuff module, which handles the input. Returns true if the
     * cheat was successful, false if failed. MAES: Let's make this boolean.
     *
     * @param key
     * @return
     */
    fun CheckCheat(key: Int): Boolean {
        var rc = false
        if (p < 0) p = 0 // initialize if first time
        if (sequence[p].code == 0) // This actually points inside "sequence"
        // *(cht->p++) = key;
            sequence[p++] =
                key.toChar() else if (cheat_xlate_table[key.toChar().code] == sequence[p]) p++ else  // Failure: back to the beginning.
            p = 0
        if (sequence[p].code == 1) p++ else if (sequence[p].code == 0xff) // end of sequence character
        {
            p = 0
            rc = true
        }
        return rc
    }

    companion object {
        /**
         * Scrambles a character. 7 -> 0 6 -> 1 5 -> 5 4 -> 3 3 -> 4 2 -> 2 1 -> 6 0
         * -> 7
         *
         * @param a
         * @return
         */
        fun SCRAMBLE(a: Char): Char {
            return ((a.code and 1 shl 7) + (a.code and 2 shl 5) + (a.code and 4)
                    + (a.code and 8 shl 1) + (a.code and 16 ushr 1) + (a.code and 32)
                    + (a.code and 64 ushr 5) + (a.code and 128 ushr 7)).toChar()
        }

        fun scrambleString(s: String): CharArray {
            val tmp = CharArray(s.length + 1)
            for (i in 0 until s.length) {
                tmp[i] = SCRAMBLE(s[i])
            }
            tmp[s.length] = 0xff.toChar()
            return tmp
        }

        /**
         * These should be common among all instances, unless for soooome reason you
         * need multiple different such tables.
         */
        var firsttime = true
        var cheat_xlate_table = CharArray(256)

        init {
            if (firsttime) {
                firsttime = false
                for (i in 0..255) cheat_xlate_table[i] = SCRAMBLE(i.toChar())
            }
        }
    }
}