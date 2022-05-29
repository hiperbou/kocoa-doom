/*
 * Copyright (C) 1993-1996 by id Software, Inc.
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
package p.Actions


import doom.SourceCode
import doom.SourceCode.P_Lights
import doom.SourceCode.P_Spec
import p.ActiveStates
import p.DoorDefines
import p.strobe_t
import rr.SectorAction
import rr.line_t
import rr.sector_t
import w.DoomIO
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer

interface ActionsLights : ActionsMoveEvents, ActionsUseEvents {
    fun FindSectorFromLineTag(line: line_t, secnum: Int): Int

    //
    // P_LIGHTS
    //
    class fireflicker_t : SectorAction() {
        var count = 0
        var maxlight = 0
        var minlight = 0
    }

    //
    // BROKEN LIGHT EFFECT
    //
    class lightflash_t : SectorAction() {
        var count = 0
        var maxlight = 0
        var minlight = 0
        var maxtime = 0
        var mintime = 0
        @Throws(IOException::class)
        override fun read(f: DataInputStream) {
            super.read(f) // Call thinker reader first            
            super.sectorid = DoomIO.readLEInt(f) // Sector index
            count = DoomIO.readLEInt(f)
            maxlight = DoomIO.readLEInt(f)
            minlight = DoomIO.readLEInt(f)
            maxtime = DoomIO.readLEInt(f)
            mintime = DoomIO.readLEInt(f)
        }

        @Throws(IOException::class)
        override fun pack(b: ByteBuffer) {
            super.pack(b) //12            
            b.putInt(super.sectorid) // 16
            b.putInt(count) //20
            b.putInt(maxlight) //24
            b.putInt(minlight) //28
            b.putInt(maxtime) //32
            b.putInt(mintime) //36
        }
    }

    class glow_t : SectorAction() {
        var minlight = 0
        var maxlight = 0
        var direction = 0
        @Throws(IOException::class)
        override fun read(f: DataInputStream) {
            super.read(f) // Call thinker reader first            
            super.sectorid = DoomIO.readLEInt(f) // Sector index
            minlight = DoomIO.readLEInt(f)
            maxlight = DoomIO.readLEInt(f)
            direction = DoomIO.readLEInt(f)
        }

        @Throws(IOException::class)
        override fun pack(b: ByteBuffer) {
            super.pack(b) //12            
            b.putInt(super.sectorid) // 16
            b.putInt(minlight) //20
            b.putInt(maxlight) //24
            b.putInt(direction) //38
        }
    }

    //
    // Find minimum light from an adjacent sector
    //
    @SourceCode.Exact
    @P_Spec.C(P_Spec.P_FindMinSurroundingLight)
    fun FindMinSurroundingLight(sector: sector_t, max: Int): Int {
        var min: Int
        var line: line_t
        var check: sector_t?
        min = max
        for (i in 0 until sector.linecount) {
            line = sector.lines!![i]!!
            //getNextSector@ run {
                check = line.getNextSector(sector)
            //}
            if (check == null) {
                continue
            }
            if (check!!.lightlevel < min) {
                min = check!!.lightlevel.toInt()
            }
        }
        return min
    }

    /**
     * P_SpawnLightFlash After the map has been loaded, scan each sector for
     * specials that spawn thinkers
     */
    @SourceCode.Exact
    @P_Lights.C(P_Lights.P_SpawnLightFlash)
    fun SpawnLightFlash(sector: sector_t) {
        var flash: lightflash_t

        // nothing special about it during gameplay
        sector.special = 0
        //Z_Malloc@ run {
            flash = lightflash_t()
        //}
        //P_AddThinker@ run {
            AddThinker(flash)
        //}
        flash.thinkerFunction = ActiveStates.T_LightFlash
        flash.sector = sector
        flash.maxlight = sector.lightlevel.toInt()
        flash.minlight = FindMinSurroundingLight(sector, sector.lightlevel.toInt())
        flash.maxtime = 64
        flash.mintime = 7
        flash.count = (P_Random() and flash.maxtime) + 1
    }

    //
    // P_SpawnStrobeFlash
    // After the map has been loaded, scan each sector
    // for specials that spawn thinkers
    //
    @SourceCode.Exact
    @P_Lights.C(P_Lights.P_SpawnStrobeFlash)
    fun SpawnStrobeFlash(sector: sector_t, fastOrSlow: Int, inSync: Int) {
        var flash: strobe_t
        //Z_Malloc@ run {
            flash = strobe_t()
        //}
        //P_AddThinker@ run {
            AddThinker(flash)
        //}
        flash.sector = sector
        flash.darktime = fastOrSlow
        flash.brighttime = DoorDefines.STROBEBRIGHT
        flash.thinkerFunction = ActiveStates.T_StrobeFlash
        flash.maxlight = sector.lightlevel.toInt()
        flash.minlight = FindMinSurroundingLight(sector, sector.lightlevel.toInt())
        if (flash.minlight == flash.maxlight) {
            flash.minlight = 0
        }

        // nothing special about it during gameplay
        sector.special = 0
        if (inSync == 0) {
            flash.count = (P_Random() and 7) + 1
        } else {
            flash.count = 1
        }
    }

    @SourceCode.Exact
    @P_Lights.C(P_Lights.P_SpawnGlowingLight)
    fun SpawnGlowingLight(sector: sector_t) {
        var g: glow_t
        //Z_Malloc@ run {
            g = glow_t()
        //}
        //P_AddThinker@ run {
            AddThinker(g)
        //}
        g.sector = sector
        //P_FindMinSurroundingLight@ run {
            g.minlight = FindMinSurroundingLight(sector, sector.lightlevel.toInt())
        //}
        g.maxlight = sector.lightlevel.toInt()
        g.thinkerFunction = ActiveStates.T_Glow
        g.direction = -1
        sector.special = 0
    }

    //
    // Start strobing lights (usually from a trigger)
    //
    override fun StartLightStrobing(line: line_t) {
        val ll = levelLoader()
        var secnum: Int
        var sec: sector_t
        secnum = -1
        while (FindSectorFromLineTag(line, secnum).also { secnum = it } >= 0) {
            sec = ll.sectors[secnum]
            if (sec.specialdata != null) {
                continue
            }
            SpawnStrobeFlash(sec, DoorDefines.SLOWDARK, 0)
        }
    }

    //
    // P_SpawnFireFlicker
    //
    @SourceCode.Exact
    @P_Lights.C(P_Lights.P_SpawnFireFlicker)
    fun SpawnFireFlicker(sector: sector_t) {
        var flick: fireflicker_t

        // Note that we are resetting sector attributes.
        // Nothing special about it during gameplay.
        sector.special = 0
        //Z_Malloc@ run {
            flick = fireflicker_t()
        //}
        //P_AddThinker@ run {
            AddThinker(flick)
        //}
        flick.thinkerFunction = ActiveStates.T_FireFlicker
        flick.sector = sector
        flick.maxlight = sector.lightlevel.toInt()
        flick.minlight = FindMinSurroundingLight(sector, sector.lightlevel.toInt()) + 16
        flick.count = 4
    }

    //
    // TURN LINE'S TAG LIGHTS OFF
    //
    override fun TurnTagLightsOff(line: line_t) {
        val ll = levelLoader()
        var i: Int
        var min: Int
        var sector: sector_t
        var tsec: sector_t?
        var templine: line_t
        for (j in 0 until ll.numsectors) {
            sector = ll.sectors[j]
            if (sector.tag == line.tag) {
                min = sector.lightlevel.toInt()
                i = 0
                while (i < sector.linecount) {
                    templine = sector.lines!![i]!!
                    tsec = templine.getNextSector(sector)
                    if (tsec == null) {
                        i++
                        continue
                    }
                    if (tsec.lightlevel < min) {
                        min = tsec.lightlevel.toInt()
                    }
                    i++
                }
                sector.lightlevel = min.toShort()
            }
        }
    }

    //
    // TURN LINE'S TAG LIGHTS ON
    //
    override fun LightTurnOn(line: line_t, bright: Int) {
        var bright = bright
        val ll = levelLoader()
        var sector: sector_t
        var temp: sector_t?
        var templine: line_t
        for (i in 0 until ll.numsectors) {
            sector = ll.sectors[i]
            if (sector.tag == line.tag) {
                // bright = 0 means to search
                // for highest light level
                // surrounding sector
                if (bright == 0) {
                    for (j in 0 until sector.linecount) {
                        templine = sector.lines!![j]!!
                        temp = templine.getNextSector(sector)
                        if (temp == null) {
                            continue
                        }
                        if (temp.lightlevel > bright) {
                            bright = temp.lightlevel.toInt()
                        }
                    }
                }
                sector.lightlevel = bright.toShort()
            }
        }
    }
}