package m


//-----------------------------------------------------------------------------
//
// $Id: Swap.java,v 1.2 2011/07/27 20:48:20 velktron Exp $
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
// DESCRIPTION:
//	Endianess handling, swapping 16bit and 32bit.
//  It's role is much less important than in C-based ports (because of stream
//  built-in endianness settings), but they are still used occasionally.
//
//-----------------------------------------------------------------------------
object Swap {
    // Swap 16bit, that is, MSB and LSB byte.
    fun SHORT(x: Short): Short {
        // No masking with 0xFF should be necessary. 
        // MAES: necessary with java due to sign trailing.
        return ((x.toInt() ushr 8 and 0xFF).toShort().toInt() or (x.toInt() shl 8)).toShort()
    }

    //Swap 16bit, that is, MSB and LSB byte.
    fun SHORT(x: Char): Short {
        // No masking with 0xFF should be necessary. 
        // MAES: necessary with java due to sign trailing.
        return ((x.code ushr 8 and 0xFF).toShort().toInt() or (x.code shl 8)).toShort()
    }

    //Swap 16bit, that is, MSB and LSB byte.
    fun USHORT(x: Char): Char {
        // No masking with 0xFF should be necessary. 
        // MAES: necessary with java due to sign trailing.
        return ((x.code ushr 8 and 0xFF).toChar().code or (x.code shl 8)).toChar()
    }

    // Swapping 32bit.
    // Maes: the "long" here is really 32-bit.
    fun LONG(x: Int): Int {
        return (x ushr 24
                or (x ushr 8 and 0xff00)
                or (x shl 8 and 0xff0000)
                or (x shl 24))
    }
}