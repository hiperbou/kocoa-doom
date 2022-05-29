package m


import data.mobjtype_t
import p.ActiveStates
import java.util.*

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: JavaRandom.java,v 1.3 2013/06/03 11:00:03 velktron Exp $
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
//	Random number LUT using java.util.Random
// Don't expect vanilla demo compatibility with THIS!
//
//-----------------------------------------------------------------------------
/**
 * Actually, now there is demo compatilbility switch: use of JavaRandom is now
 * default in singleplayer, unless you play demo, unless you record demo,
 * when you play demo, DoomRandom is picked instead, same for record, unless
 * you specify -javarandom command line argument, in that case when you record
 * demo, version information will be changed, and JavaRandom used,
 * when you play this demo, DoomRandom will not be picked, when you play
 * another demo, it will pick DoomRandom.
 *
 * When you dont pass -javarandom, but play demo recorded with JavaRandom,
 * it will pick JavaRandom for this demo playback
 * - Good Sign 2017/04/14
 */
internal class JavaRandom : IRandom {
    protected var rndindex = 0
    protected var prndindex = 0

    // Which one is deterministic?
    override fun P_Random(): Int {
        rndindex++
        return 0xFF and r.nextInt()
    }

    override fun M_Random(): Int {
        prndindex++
        return 0xFF and m.nextInt()
    }

    override fun ClearRandom() {
        prndindex = 0
        rndindex = prndindex
        r.setSeed(666)
    }

    override val index: Int
        get() = rndindex

    private val r: Random
    private val m: Random

    init {
        r = Random(666)
        m = Random(666)
        ClearRandom()
    }

    override fun P_Random(caller: Int): Int {
        // DUMMY
        return P_Random()
    }

    override fun P_Random(message: String?): Int {
        // DUMMY
        return P_Random()
    }

    override fun P_Random(caller: ActiveStates?, sequence: Int): Int {
        // DUMMY
        return P_Random()
    }

    override fun P_Random(caller: ActiveStates?, type: mobjtype_t?, sequence: Int): Int {
        // DUMMY
        return P_Random()
    }
}