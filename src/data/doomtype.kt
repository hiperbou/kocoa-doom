package data


// Emacs style mode select   -*- C++ -*-
//-----------------------------------------------------------------------------
//
// $Id: doomtype.java,v 1.3 2011/02/11 00:11:13 velktron Exp $
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
//	Simple basic typedefs, isolated here to make it easier
//	 separating modules.
//    
//-----------------------------------------------------------------------------
object doomtype {
    // C's "chars" are actually Java signed bytes.
    var MAXCHAR = 0x7f.toByte()
    var MAXSHORT = 0x7fff.toShort()

    // Max pos 32-bit int.
    var MAXINT = 0x7fffffff
    var MAXLONG = 0x7fffffffL
    var MINCHAR = 0x80.toByte()
    var MINSHORT = 0x8000.toShort()

    // Max negative 32-bit integer.
    var MININT = -0x80000000
    var MINLONG = 0x80000000L
}