package data

import m.fixed_t

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Tables.java,v 1.22 2011/05/06 09:21:59 velktron Exp $
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
// $Log: Tables.java,v $
// Revision 1.22  2011/05/06 09:21:59  velktron
// Cleaned up and reorganized common renderer code.
//
// Revision 1.21  2011/02/11 00:11:13  velktron
// A MUCH needed update to v1.3.
//
// Revision 1.20  2010/12/20 17:15:08  velktron
// Made the renderer more OO -> TextureManager and other changes as well.
//
// Revision 1.19  2010/12/10 17:38:56  velktron
// pspritescale fixed, weapon actions won't crash (but not work either).
//
// Revision 1.18  2010/11/25 20:12:44  velktron
// Fixed blockmap bug and viewangletox overflow bug.
//
// Revision 1.17  2010/11/22 01:17:16  velktron
// Fixed blockmap (for the most part), some actions implemented and functional, ambient animation/lighting functional.
//
// Revision 1.16  2010/11/17 23:55:06  velktron
// Kind of playable/controllable.
//
// Revision 1.15  2010/11/15 17:15:54  velktron
// Fixed masked columns rendering, introduced unrolled span and column functions from Boom (thanks, Lee Killough :-)
//
// Revision 1.14  2010/11/14 20:00:21  velktron
// Bleeding floor bug fixed!
//
// Revision 1.13  2010/11/12 13:37:25  velktron
// Rationalized the LUT system - now it's 100% procedurally generated.
//
// Revision 1.12  2010/11/03 16:48:04  velktron
// "Bling" view angles fixed (perhaps related to the "bleeding line bug"?)
//
// Revision 1.11  2010/10/14 18:37:14  velktron
// Rendering kinda works. Wow.
//
// Revision 1.10  2010/10/08 16:55:50  velktron
// Duh
//
// Revision 1.9  2010/10/01 16:47:51  velktron
// Fixed tab interception.
//
// Revision 1.8  2010/09/27 15:07:44  velktron
// meh
//
// Revision 1.7  2010/09/27 02:27:29  velktron
// BEASTLY update
//
// Revision 1.6  2010/09/22 16:40:02  velktron
// MASSIVE changes in the status passing model.
// DoomMain and DoomGame unified.
// Doomstat merged into DoomMain (now status and game functions are one).
//
// Most of DoomMain implemented. Possible to attempt a "classic type" start but will stop when reading sprites.
//
// Revision 1.5  2010/09/21 15:53:37  velktron
// Split the Map ...somewhat...
//
// Revision 1.4  2010/09/16 00:16:27  velktron
// Velvet FM 96.8
//
// Revision 1.3  2010/09/15 16:17:38  velktron
// Arithmetic
//
// Revision 1.2  2010/09/09 16:09:09  velktron
// Yer more enhancements to the display system...
//
// Revision 1.1  2010/07/05 16:18:40  velktron
// YOU DON'T WANNA KNOW
//
// Revision 1.1  2010/06/30 08:58:51  velktron
// Let's see if this stuff will finally commit....
//
//
// Most stuff is still  being worked on. For a good place to start and get an idea of what is being done, I suggest checking out the "testers" package.
//
// Revision 1.1  2010/06/29 11:07:34  velktron
// Release often, release early they say...
//
// Commiting ALL stuff done so far. A lot of stuff is still broken/incomplete, and there's still mixed C code in there. I suggest you load everything up in Eclpise and see what gives from there.
//
// A good place to start is the testers/ directory, where you  can get an idea of how a few of the implemented stuff works.
//
//
// DESCRIPTION:
//	Lookup tables.
//	Do not try to look them up :-).
//	In the order of appearance: 
//
//	int finetangent[4096]	- Tangens LUT.
//	 Should work with BAM fairly well (12 of 16bit,
//      effectively, by shifting).
//
//	int finesine[10240]		- Sine lookup.
//	 Guess what, serves as cosine, too.
//	 Remarkable thing is, how to use BAMs with this?
//
//	int tantoangle[2049]	- ArcTan LUT,
//	  maps tan(angle) to angle fast. Gotta search.
//	
//    
//-----------------------------------------------------------------------------
object Tables {
    const val rcsid = "\$Id:"
    const val PI = 3.141592657

    /** Normally set to 12, and this sets the value of other constants too.
     * Howevever changing it will also distort the view, resulting in a
     * nightmare-like vision. There are some practical minimum and
     * maximums as well.
     *
     *
     */
    const val BITSPRECISION = 12
    const val FINEANGLES = 2 shl Tables.BITSPRECISION
    const val FINETANS = Tables.FINEANGLES / 2 // 4096 for normal precision.
    const val QUARTERMARK = 2 shl Tables.BITSPRECISION - 2
    const val FINEMASK = Tables.FINEANGLES - 1

    /** Mod long angle_t's with this value to cut off rollover  */
    const val ANGLEMODULE = 0x100000000L

    /** AND with this to remove unwanted sign extensions  */
    const val BITS32 = 0x00000000FFFFFFFFL

    /** Sign elimination  */
    const val BITS31 = 0x7FFFFFFF
    // Maes: we have to procedurally generate finesine/finecosine, else we run into a Java static limit.
    // Either that, or I split the files. Guess what I did.
    // public static int PRECISION = 10240 ;
    /** 0x100000000 to 0x2000  */
    const val ANGLETOFINESHIFT = 31 - Tables.BITSPRECISION
    /* Binary Angle Measurement.
 * Some maths: their definition means that a range of 2pi is actually
 * mapped to 2^32 values!!! But the lookup tables are only 8K (2^13)
 * long (for sine/cosine), which means that we're 19 bits too precise
 * -> ergo, >>ANGLETOFINESHIFT must be applied.
 * 
 * Also, the original angle_t type was "unsigned int", so we should be
 * using longs here. However, as BAM is used only after shifting, so 
 * using ints doesn't cause a problem for LUT access.
 *  
 * However, some problems may arise with comparisons and ordinary arithmetic: 
 * e.g. ANG270 is supposed to be larger not only than ANG180, but also from 
 * ANG45, which does not hold true if those constants were stored as ints.
 * 
 * As a rule of thumb, whenever you need to store JUST a BAM index, then 
 * ints are ok (actually, you'll have to cast down to int anyway).
 * 
 * Whenever you need to add or compare angles directly however, you need 
 * longs. Furthermore, you must account for possible rollovers by modding
 * with 0x100000000 or else long ints will store angles exceeding 360 degrees!
 * Under no circumstances the value actually stored in the "long angles" should
 * exceed 0xFFFFFFFF.
 * 
 * An example: comparing any two long angles directly is OK, provided they were
 * constructed correctly.
 * 
 * Adding, subtracting, multiplying etc. with two or more angles however requires
 * rollover compensation (e.g. result=(a+b+c) is wrong, result=(a+b+c)%0xFFFFFFFF
 * is correct and will produce an angle you can "trust".
 * 
 * 
 */
    /** Doom angle constants.  */
    const val ANG45 = 0x20000000L
    const val ANG90 = 0x40000000L
    const val ANG180 = 0x80000000L
    const val ANG270 = 0xc0000000L
    const val SLOPERANGE = 2 shl Tables.BITSPRECISION - 2 // Normally 2048.
    const val SLOPEBITS = Tables.BITSPRECISION - 1
    val DBITS: Int = fixed_t.FRACBITS - Tables.SLOPEBITS
    //  typedef unsigned angle_t;
    // Effective size is 2049;
    // The +1 size is to handle the case when x==y
    //  without additional checking.
    //extern angle_t      tantoangle[SLOPERANGE+1];
    /**
     * Original size was 10240, but includes 5PI/4 periods. We can get away with
     * ints on this one because of the small range. MAES: WTF? -64 ~ 64K
     * range... so 17-bit accuracy? heh.
     */
    val finesine = IntArray(Tables.FINEANGLES + Tables.QUARTERMARK)
    val finecosine = IntArray(Tables.FINEANGLES)

    /** Any denominator smaller than 512 will result in
     * maximum slope (45 degrees, or an index into tantoangle of 2048)
     * The original method used unsigned args. So if this returns NEGATIVES
     * in any way, it means you fucked up. Another "consistency" for Doom.
     * Even though it was called upon fixed_t signed numbers.
     *
     */
    fun SlopeDiv(num: Long, den: Long): Int {
        val ans: Int
        if (den < 512) return Tables.SLOPERANGE
        ans = ((num shl 3) / (den ushr 8)).toInt()
        return if (ans <= Tables.SLOPERANGE) ans else Tables.SLOPERANGE
    }

    /** Finetangent table. It only has 4096 values corresponding roughly
     * to -90/+90 angles, with values between are -/+ 2607 for "infinity".
     *
     * Since in vanilla accesses to the table can overflow way beyond 4096
     * indexes, the access index must be clipped to 4K tops via an accessor,
     * or, in order to simulate some aspects of vanilla overflowing, replicate
     * 4K of finesine's values AFTER the 4K index. This removes the need
     * for access checking, at the cost of some extra memory. It also allows
     * a small degree of "vanilla like" compatibility.
     *
     *
     */
    val finetangent = IntArray(2 * Tables.FINETANS)
    // MAES: original range 2049
    // This obviously 
    // Range goes from 0x00000000 to 0x20000000, so in theory plain ints should be enough...
    /** This maps a value 0-2048 to a BAM unsigned integer angle, ranging from 0x0 to 0x2000000:
     *
     * In practice, this means there are only tangent values for angles up to 45 degrees.
     *
     * These values are valid BAM measurements in the first quadrant
     *
     *
     */
    val tantoangle = IntArray(Tables.SLOPERANGE + 1)

    /** Use this to get a value from the finesine table. It will be automatically shifte,
     * Equivalent to finesine[angle>>>ANGLETOFINESHIFT]
     *
     * @param angle in BAM units
     * @return
     */
    fun finesine(angle: Int): Int {
        return Tables.finesine[angle ushr Tables.ANGLETOFINESHIFT]
    }

    /** Use this to get a value from the finesine table using a long argument.
     * It will automatically shift, apply rollover module and cast.
     *
     * Equivalent to finesine[(int) ((angle>>ANGLETOFINESHIFT)%ANGLEMODULE)];
     *
     * @param angle in BAM units
     * @return
     */
    fun finesine(angle: Long): Int {
        return Tables.finesine[(angle and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT).toInt()]
    }

    /** Use this to get a value from the finecosine table. It will be automatically shifted,
     * Equivalent to finecosine[angle>>>ANGLETOFINESHIFT]
     * @param angle in BAM units
     * @return
     */
    fun finecosine(angle: Int): Int {
        return Tables.finecosine[angle ushr Tables.ANGLETOFINESHIFT]
    }

    /** Use this to get a value from the finecosine table.
     * It will automatically shift, apply rollover module and cast.
     *
     * Equivalent to finecosine[(int) ((angle&BITS32)>>>ANGLETOFINESHIFT)]
     * @param angle in BAM units
     * @return
     */
    fun finecosine(angle: Long): Int {
        return Tables.finecosine[(angle and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT).toInt()]
    }

    /** Compare BAM angles in 32-bit format
     * "Greater or Equal" bam0>bam1
     */
    fun GE(bam0: Int, bam1: Int): Boolean {
        // Handle easy case.
        var bam0 = bam0
        var bam1 = bam1
        if (bam0 == bam1) return true

        // bam0 is greater than 180 degrees.
        if (bam0 < 0 && bam1 >= 0) return true
        // bam1 is greater than 180 degrees.
        if (bam0 >= 0 && bam1 < 0) return false

        // Both "greater than 180", No other way to compare.
        bam0 = bam0 and Tables.BITS31
        bam1 = bam1 and Tables.BITS31
        return bam0 > bam1
    }

    fun GT(bam0: Int, bam1: Int): Boolean {
        // bam0 is greater than 180 degrees.
        var bam0 = bam0
        var bam1 = bam1
        if (bam0 < 0 && bam1 >= 0) return true
        // bam1 is greater than 180 degrees.
        if (bam0 >= 0 && bam1 < 0) return false

        // Both "greater than 180", No other way to compare.
        bam0 = bam0 and Tables.BITS31
        bam1 = bam1 and Tables.BITS31
        return bam0 > bam1
    }

    fun BAMDiv(bam0: Int, bam1: Int): Int {
        // bam0 is greater than 180 degrees.
        if (bam0 >= 0) return bam0 / bam1
        // bam0 is greater than 180 degrees.
        // We have to make is so that ANG270 0xC0000000 becomes ANG135, aka 60000000
        return if (bam1 >= 0) ((0x0FFFFFFFFL and bam0.toLong()) / bam1).toInt() else ((0x0FFFFFFFFL and bam0.toLong()) / (0x0FFFFFFFFL and bam1.toLong())).toInt()
    }

    /** Converts a long angle to a BAM LUT-ready angle (13 bits, between 0-8191).
     * Cuts away rollover.
     *
     * @param angle
     * @return
     */
    fun toBAMIndex(angle: Long): Int {
        return (angle and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT).toInt()
    }

    /** Converts a long angle to a TAN BAM  LUT-ready angle (12 bits, between 0-4195).
     * Cuts away rollover.
     *
     * @param angle
     * @return
     */
    fun toFineTanIndex(angle: Long): Int {
        return (angle and Tables.BITS31.toLong() ushr Tables.ANGLETOFINESHIFT).toInt()
    }

    /** Converts an 32-bit int angle to a BAM LUT-ready angle (13 bits, between 0-8192).
     *
     * @param angle
     * @return
     */
    fun toBAMIndex(angle: Int): Int {
        return angle ushr Tables.ANGLETOFINESHIFT
    }

    /** Add two long angles and correct for overflow  */
    fun addAngles(a: Long, b: Long): Long {
        return a + b and Tables.BITS32
    }

    /** MAES: I brought this function "back from the dead" since
     * Java has some pretty low static limits for statically defined LUTs.
     * In order to keep the codebase clutter and static allocation to a minimum,
     * I decided to procedurally generate the tables during runtime,
     * using the original functions.
     *
     * The code has been thoroughly checked in both Sun's JDK and GCC and was
     * found to, indeed, produce the same values found in the finesine/finecosine
     * and finetangent tables, at least on Intel.
     *
     * The "tantoangle" table is also generated procedurally, but since there
     * was no "dead code" to build upon, it was recreated through reverse
     * engineering and also found to be 100% faithful to the original data.
     *
     *
     */
    fun InitTables() {
        var i: Int
        var a: Float
        var fv: Float
        var t: Int

        // viewangle tangent table
        i = 0
        while (i < Tables.FINEANGLES / 2) {
            a = ((i - Tables.FINEANGLES / 4 + 0.5) * Tables.PI * 2).toFloat() / Tables.FINEANGLES
            fv = (fixed_t.FRACUNIT * Math.tan(a.toDouble())).toFloat()
            t = fv.toInt()
            Tables.finetangent[i] = t
            i++
        }

        // finesine table
        i = 0
        while (i < Tables.FINEANGLES + Tables.QUARTERMARK) {

            // OPTIMIZE: mirror...
            a = ((i + 0.5) * Tables.PI * 2).toFloat() / Tables.FINEANGLES
            t = (fixed_t.FRACUNIT * Math.sin(a.toDouble())).toInt()
            Tables.finesine[i] = t
            if (i >= Tables.QUARTERMARK) {
                Tables.finecosine[i - Tables.QUARTERMARK] = t
            }
            i++
        }

        // HACK: replicate part of finesine after finetangent, to
        // simulate overflow behavior and remove need for capping
        // indexes
        // viewangle tangent table
        i = Tables.FINEANGLES / 2
        while (i < Tables.FINEANGLES) {
            Tables.finetangent[i] = Tables.finesine[i - Tables.FINEANGLES / 2]
            i++
        }

        /* tantoangle table
     * There was actually no dead code for that one, so this is a close recreation.
     * Since there are 2049 values, and the maximum angle considered is 536870912 (0x20000000)
     * which is 45 degrees in BAM, we have to fill in the atan for values up to 45 degrees.
     * Since the argument is a slope ranging from 0...2048, we have 2049 equally spaced (?)
     *  values, with 2048 being being the unitary slope (0x10000 in fixed_t). That value is only
     *  accessed in special cases (overflow) so we only need to consider 0..2047 aka 11 bits.
     *  So: we take "minislopes" 0-2048, we blow them up to a full fixed_t unit with <<5.
     *  We make this into a float (?), then use trigonometric ATAN, and then go to BAM.
     *  
     *  Any questions?
     *  
     */

        /* This is the recreated code 
    for (i=0 ; i<SLOPERANGE+1 ; i++)
    {
    
    a=(float)((i<<DBITS)/65536.0);
    t=(int)((float)(2*Math.atan(a)/PI)*0x40000000); 
    tantoangle[i] = t;
    } 
    */

        // This is the original R_InitPointToAngle code that created this table.
        i = 0
        while (i <= Tables.SLOPERANGE) {
            a = (Math.atan(i.toDouble() / Tables.SLOPERANGE) / (3.141592657 * 2)).toFloat()
            t = (0xffffffffL * a).toInt()
            Tables.tantoangle[i] = t
            i++
        }
    }
}