package automap

import data.Defines
import data.Limits
import data.Tables
import doom.*
import doom.SourceCode.AM_Map
import g.Signals.ScanCode
import m.cheatseq_t
import m.fixed_t
import p.mobj_t
import rr.line_t
import rr.patch_t
import utils.GenericCopy
import utils.GenericCopy.malloc
import v.DoomGraphicSystem
import v.graphics.Plotter
import v.renderers.DoomScreen
import java.awt.Rectangle
import java.util.*


// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: Map.java,v 1.37 2012/09/24 22:36:28 velktron Exp $
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
//
//
// $Log: Map.java,v $
// Revision 1.37  2012/09/24 22:36:28  velktron
// Map get color
//
// Revision 1.36  2012/09/24 17:16:23  velktron
// Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
// Revision 1.34.2.4  2012/09/24 16:58:06  velktron
// TrueColor, Generics.
//
// Revision 1.34.2.3  2012/09/20 14:06:43  velktron
// Generic automap
//
// Revision 1.34.2.2 2011/11/27 18:19:19 velktron
// Configurable colors, more parametrizable.
//
// Revision 1.34.2.1 2011/11/14 00:27:11 velktron
// A barely functional HiColor branch. Most stuff broken. DO NOT USE
//
// Revision 1.34 2011/11/03 18:11:14 velktron
// Fixed long-standing issue with 0-rot vector being reduced to pixels. Fixed
// broken map panning functionality after keymap change.
//
// Revision 1.33 2011/11/01 23:48:43 velktron
// Using FillRect
//
// Revision 1.32 2011/11/01 19:03:10 velktron
// Using screen number constants
//
// Revision 1.31 2011/10/23 18:10:32 velktron
// Generic compliance for DoomVideoInterface
//
// Revision 1.30 2011/10/07 16:08:23 velktron
// Now using g.Keys and line_t
//
// Revision 1.29 2011/09/29 13:25:09 velktron
// Eliminated "intermediate" AbstractAutoMap. Map implements IAutoMap directly.
//
// Revision 1.28 2011/07/28 16:35:03 velktron
// Well, we don't need to know that anymore.
//
// Revision 1.27 2011/06/18 23:16:34 velktron
// Added extreme scale safeguarding (e.g. for Europe.wad).
//
// Revision 1.26 2011/05/30 15:45:44 velktron
// AbstractAutoMap and IAutoMap
//
// Revision 1.25 2011/05/24 11:31:47 velktron
// Adapted to IDoomStatusBar
//
// Revision 1.24 2011/05/23 16:57:39 velktron
// Migrated to VideoScaleInfo.
//
// Revision 1.23 2011/05/17 16:50:02 velktron
// Switched to DoomStatus
//
// Revision 1.22 2011/05/10 10:39:18 velktron
// Semi-playable Techdemo v1.3 milestone
//
// Revision 1.21 2010/12/14 17:55:59 velktron
// Fixed weapon bobbing, added translucent column drawing, separated rendering
// commons.
//
// Revision 1.20 2010/12/12 19:06:18 velktron
// Tech Demo v1.1 release.
//
// Revision 1.19 2010/11/17 23:55:06 velktron
// Kind of playable/controllable.
//
// Revision 1.18 2010/11/12 13:37:25 velktron
// Rationalized the LUT system - now it's 100% procedurally generated.
//
// Revision 1.17 2010/10/01 16:47:51 velktron
// Fixed tab interception.
//
// Revision 1.16 2010/09/27 15:07:44 velktron
// meh
//
// Revision 1.15 2010/09/27 02:27:29 velktron
// BEASTLY update
//
// Revision 1.14 2010/09/23 07:31:11 velktron
// fuck
//
// Revision 1.13 2010/09/13 15:39:17 velktron
// Moving towards an unified gameplay approach...
//
// Revision 1.12 2010/09/08 21:09:01 velktron
// Better display "driver".
//
// Revision 1.11 2010/09/08 15:22:18 velktron
// x,y coords in some structs as value semantics. Possible speed increase?
//
// Revision 1.10 2010/09/06 16:02:59 velktron
// Implementation of palettes.
//
// Revision 1.9 2010/09/02 15:56:54 velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for
// seg_t and node_t introduced.
//
// Revision 1.8 2010/09/01 15:53:42 velktron
// Graphics data loader implemented....still need to figure out how column
// caching works, though.
//
// Revision 1.7 2010/08/27 23:46:57 velktron
// Introduced Buffered renderer, which makes tapping directly into byte[] screen
// buffers mapped to BufferedImages possible.
//
// Revision 1.6 2010/08/26 16:43:42 velktron
// Automap functional, biatch.
//
// Revision 1.5 2010/08/25 00:50:59 velktron
// Some more work...
//
// Revision 1.4 2010/08/22 18:04:21 velktron
// Automap
//
// Revision 1.3 2010/08/19 23:14:49 velktron
// Automap
//
// Revision 1.2 2010/08/10 16:41:57 velktron
// Threw some work into map loading.
//
// Revision 1.1 2010/07/20 15:52:56 velktron
// LOTS of changes, Automap almost complete. Use of fixed_t inside methods
// severely limited.
//
// Revision 1.1 2010/06/30 08:58:51 velktron
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
// DESCRIPTION: the automap code
//
// -----------------------------------------------------------------------------
class Map<T, V>(  /////////////////// Status objects ///////////////////
    val DOOM: DoomMain<T, V>
) : IAutoMap<T, V> {
    /**
     * Configurable colors - now an enum
     * - Good Sign 2017/04/05
     *
     * Use colormap-specific colors to support extended modes.
     * Moved hardcoding in here. Potentially configurable.
     */
    enum class Color(val range: Int, val value: Byte) {
        CLOSE_TO_BLACK(1, 246.toByte()), REDS(16, 176.toByte()), BLUES(8, 200.toByte()), GREENS(
            16,
            112.toByte()
        ),
        GRAYS(16, 96.toByte()), BROWNS(16, 64.toByte()), YELLOWS(8, 160.toByte()), BLACK(1, 0.toByte()), WHITE(
            1,
            4.toByte()
        ),
        GRAYS_DARKER_25(13, (GRAYS.value + 4).toByte()), DARK_GREYS(
            8,
            (GRAYS.value + GRAYS.range / 2).toByte()
        ),
        DARK_REDS(8, (REDS.value + REDS.range / 2).toByte());

        val liteBlock: ByteArray?
        val NUM_LITES = 8
        companion object {
            const val NUM_LITES = 8
            val LITE_LEVELS_FULL_RANGE = intArrayOf(0, 4, 7, 10, 12, 14, 15, 15)
            val LITE_LEVELS_HALF_RANGE = intArrayOf(0, 2, 3, 5, 6, 6, 7, 7)

            init {
                for (c in values()) {
                    when (c.range) {
                        16 -> {
                            var i = 0
                            while (i < NUM_LITES) {
                                c.liteBlock!![i] = (c.value + LITE_LEVELS_FULL_RANGE[i]).toByte()
                                ++i
                            }
                        }
                        8 -> {
                            var i = 0
                            while (i < LITE_LEVELS_HALF_RANGE.size) {
                                c.liteBlock!![i] = (c.value + LITE_LEVELS_HALF_RANGE[i]).toByte()
                                ++i
                            }
                        }
                    }
                }
            }
        }

        init {
            if (range >= NUM_LITES) {
                liteBlock = ByteArray(NUM_LITES)
            } else {
                liteBlock = null
            }
        }
    }

    val fixedColorSources = EnumMap<Color, V>(Color::class.java)
    val litedColorSources = EnumMap<Color, V>(Color::class.java)
    override fun Repalette() {
        GENERATE_LITE_LEVELS_FOR.stream()
            .forEach { c: Color ->
                if (c.liteBlock != null) {
                    litedColorSources[c] = DOOM.graphicSystem.convertPalettedBlock(*c.liteBlock)
                }
            }
        Arrays.stream(Color.values())
            .forEach { c: Color ->
                val converted = DOOM.graphicSystem.convertPalettedBlock(c.value)!!
                val extended =
                    java.lang.reflect.Array.newInstance(converted::class.java.getComponentType(), Color.NUM_LITES) as V
                GenericCopy.memset(extended, 0, Color.NUM_LITES, converted, 0, 1)
                fixedColorSources[c] = extended
            }
    }

    /** translates between frame-buffer and map distances  */
    private fun FTOM(x: Int): Int {
        return fixed_t.FixedMul(x shl 16, scale_ftom)
    }

    /** translates between frame-buffer and map distances  */
    private fun MTOF(x: Int): Int {
        return fixed_t.FixedMul(x, scale_mtof) shr 16
    }

    /** translates between frame-buffer and map coordinates  */
    private fun CXMTOF(x: Int): Int {
        return f_x + MTOF(x - m_x)
    }

    /** translates between frame-buffer and map coordinates  */
    private fun CYMTOF(y: Int): Int {
        return f_y + (f_h - MTOF(y - m_y))
    }
    //
    // The vector graphics for the automap.
    /**
     * A line drawing of the player pointing right, starting from the middle.
     */
    protected lateinit var player_arrow: Array<mline_t>
    protected var NUMPLYRLINES = 0
    protected lateinit var cheat_player_arrow: Array<mline_t>
    protected var NUMCHEATPLYRLINES = 0
    protected lateinit var triangle_guy: Array<mline_t>
    protected var NUMTRIANGLEGUYLINES = 0
    protected lateinit var thintriangle_guy: Array<mline_t>
    protected var NUMTHINTRIANGLEGUYLINES = 0
    protected fun initVectorGraphics() {
        var R = 8 * Defines.PLAYERRADIUS / 7
        player_arrow = arrayOf(
            mline_t(-R + R / 8, 0, R, 0),  // -----
            mline_t(R, 0, R - R / 2, R / 4),  // ----
            mline_t(R, 0, R - R / 2, -R / 4),
            mline_t(-R + R / 8, 0, -R - R / 8, R / 4),  // >---
            mline_t(-R + R / 8, 0, -R - R / 8, -R / 4),
            mline_t(-R + 3 * R / 8, 0, -R + R / 8, R / 4),  // >>--
            mline_t(-R + 3 * R / 8, 0, -R + R / 8, -R / 4)
        )
        NUMPLYRLINES = player_arrow.size
        cheat_player_arrow = arrayOf(
            mline_t(-R + R / 8, 0, R, 0),  // -----
            mline_t(R, 0, R - R / 2, R / 6),  // ----
            mline_t(R, 0, R - R / 2, -R / 6),
            mline_t(-R + R / 8, 0, -R - R / 8, R / 6),  // >----
            mline_t(-R + R / 8, 0, -R - R / 8, -R / 6),
            mline_t(-R + 3 * R / 8, 0, -R + R / 8, R / 6),  // >>----
            mline_t(-R + 3 * R / 8, 0, -R + R / 8, -R / 6),
            mline_t(-R / 2, 0, -R / 2, -R / 6),  // >>-d--
            mline_t(-R / 2, -R / 6, -R / 2 + R / 6, -R / 6),
            mline_t(-R / 2 + R / 6, -R / 6, -R / 2 + R / 6, R / 4),
            mline_t(-R / 6, 0, -R / 6, -R / 6),  // >>-dd-
            mline_t(-R / 6, -R / 6, 0, -R / 6),
            mline_t(0, -R / 6, 0, R / 4),
            mline_t(R / 6, R / 4, R / 6, -R / 7),  // >>-ddt
            mline_t(R / 6, -R / 7, R / 6 + R / 32, -R / 7 - R / 32),
            mline_t(
                R / 6 + R / 32, -R / 7 - R / 32,
                R / 6 + R / 10, -R / 7
            )
        )
        NUMCHEATPLYRLINES = cheat_player_arrow.size
        R = fixed_t.FRACUNIT
        triangle_guy = arrayOf(
            mline_t(-.867 * R, -.5 * R, .867 * R, -.5 * R),
            mline_t(.867 * R, -.5 * R, 0.0, R.toDouble()),
            mline_t(0.0, R.toDouble(), -.867 * R, -.5 * R)
        )
        NUMTRIANGLEGUYLINES = triangle_guy.size
        thintriangle_guy = arrayOf(
            mline_t(-.5 * R, -.7 * R, R.toDouble(), 0.0),
            mline_t(R.toDouble(), 0.0, -.5 * R, .7 * R),
            mline_t(-.5 * R, .7 * R, -.5 * R, -.7 * R)
        )
        NUMTHINTRIANGLEGUYLINES = thintriangle_guy.size
    }

    /** Planned overlay mode  */
    protected var overlay = 0
    protected var cheating = 0
    protected var grid = false
    protected var leveljuststarted = 1 // kluge until AM_LevelInit() is called
    protected var finit_width: Int
    protected var finit_height: Int

    // location of window on screen
    protected var f_x = 0
    protected var f_y = 0

    // size of window on screen
    protected var f_w = 0
    protected var f_h = 0
    protected var f_rect: Rectangle? = null

    /** used for funky strobing effect  */
    protected var lightlev = 0
    /** pseudo-frame buffer  */ //protected V fb;
    /**
     * I've made this awesome change to draw map lines on the renderer
     * - Good Sign 2017/04/05
     */
    protected val plotter: Plotter<V?>
    protected var amclock = 0

    /** (fixed_t) how far the window pans each tic (map coords)  */
    protected var m_paninc: mpoint_t

    /** (fixed_t) how far the window zooms in each tic (map coords)  */
    protected var mtof_zoommul = 0

    /** (fixed_t) how far the window zooms in each tic (fb coords)  */
    protected var ftom_zoommul = 0

    /** (fixed_t) LL x,y where the window is on the map (map coords)  */
    protected var m_x = 0
    protected var m_y = 0

    /** (fixed_t) UR x,y where the window is on the map (map coords)  */
    protected var m_x2 = 0
    protected var m_y2 = 0

    /** (fixed_t) width/height of window on map (map coords)  */
    protected var m_w = 0
    protected var m_h = 0

    /** (fixed_t) based on level size  */
    protected var min_x = 0
    protected var min_y = 0
    protected var max_x = 0
    protected var max_y = 0

    /** (fixed_t) max_x-min_x  */
    protected var max_w //
            = 0

    /** (fixed_t) max_y-min_y  */
    protected var max_h = 0

    /** (fixed_t) based on player size  */
    protected var min_w = 0
    protected var min_h = 0

    /** (fixed_t) used to tell when to stop zooming out  */
    protected var min_scale_mtof = 0

    /** (fixed_t) used to tell when to stop zooming in  */
    protected var max_scale_mtof = 0

    /** (fixed_t) old stuff for recovery later  */
    protected var old_m_w = 0
    protected var old_m_h = 0
    protected var old_m_x = 0
    protected var old_m_y = 0

    /** old location used by the Follower routine  */
    protected var f_oldloc: mpoint_t

    /** (fixed_t) used by MTOF to scale from map-to-frame-buffer coords  */
    protected var scale_mtof = INITSCALEMTOF

    /** used by FTOM to scale from frame-buffer-to-map coords (=1/scale_mtof)  */
    protected var scale_ftom = 0

    /** the player represented by an arrow  */
    protected var plr: player_t

    /** numbers used for marking by the automap  */
    private val marknums = arrayOfNulls<patch_t>(10)

    /** where the points are  */
    private val markpoints: Array<mpoint_t>

    /** next point to be assigned  */
    private var markpointnum = 0

    /** specifies whether to follow the player around  */
    protected var followplayer = true
    protected var cheat_amap_seq =
        charArrayOf(0xb2.toChar(), 0x26.toChar(), 0x26.toChar(), 0x2e.toChar(), 0xff.toChar()) // iddt
    protected var cheat_amap = cheatseq_t(cheat_amap_seq, 0)

    // MAES: STROBE cheat. It's not even cheating, strictly speaking.
    private val cheat_strobe_seq = charArrayOf(
        0x6e.toChar(), 0xa6.toChar(), 0xea.toChar(), 0x2e.toChar(), 0x6a.toChar(), 0xf6.toChar(),
        0x62.toChar(), 0xa6.toChar(), 0xff // vestrobe
            .toChar()
    )
    private val cheat_strobe = cheatseq_t(cheat_strobe_seq, 0)
    private var stopped = true
    // extern boolean viewactive;
    // extern byte screens[][DOOM.vs.getScreenWidth()*DOOM.vs.getScreenHeight()];
    /**
     * Calculates the slope and slope according to the x-axis of a line segment
     * in map coordinates (with the upright y-axis n' all) so that it can be
     * used with the brain-dead drawing stuff.
     *
     * @param ml
     * @param is
     */
    fun getIslope(ml: mline_t, `is`: islope_t) {
        val dx: Int
        val dy: Int
        dy = ml.ay - ml.by
        dx = ml.bx - ml.ax
        if (dy == 0) `is`.islp = (if (dx < 0) -Limits.MAXINT else Limits.MAXINT) else `is`.islp =
            fixed_t.FixedDiv(dx, dy)
        if (dx == 0) `is`.slp = (if (dy < 0) -Limits.MAXINT else Limits.MAXINT) else `is`.slp = fixed_t.FixedDiv(dy, dx)
    }

    //
    //
    //
    fun activateNewScale() {
        m_x += m_w / 2
        m_y += m_h / 2
        m_w = FTOM(f_w)
        m_h = FTOM(f_h)
        m_x -= m_w / 2
        m_y -= m_h / 2
        m_x2 = m_x + m_w
        m_y2 = m_y + m_h
        plotter.setThickness(
            Math.min(MTOF(fixed_t.FRACUNIT), DOOM.graphicSystem.getScalingX()),
            Math.min(MTOF(fixed_t.FRACUNIT), DOOM.graphicSystem.getScalingY())
        )
    }

    //
    //
    //
    fun saveScaleAndLoc() {
        old_m_x = m_x
        old_m_y = m_y
        old_m_w = m_w
        old_m_h = m_h
    }

    private fun restoreScaleAndLoc() {
        m_w = old_m_w
        m_h = old_m_h
        if (!followplayer) {
            m_x = old_m_x
            m_y = old_m_y
        } else {
            val plr_mo = plr.mo!!
            m_x = plr_mo._x - m_w / 2
            m_y = plr_mo._y - m_h / 2
        }
        m_x2 = m_x + m_w
        m_y2 = m_y + m_h

        // Change the scaling multipliers
        scale_mtof = fixed_t.FixedDiv(f_w shl fixed_t.FRACBITS, m_w)
        scale_ftom = fixed_t.FixedDiv(fixed_t.FRACUNIT, scale_mtof)
        plotter.setThickness(
            Math.min(MTOF(fixed_t.FRACUNIT), Color.NUM_LITES),
            Math.min(MTOF(fixed_t.FRACUNIT), Color.NUM_LITES)
        )
    }

    /**
     * adds a marker at the current location
     */
    fun addMark() {
        markpoints[markpointnum].x = m_x + m_w / 2
        markpoints[markpointnum].y = m_y + m_h / 2
        markpointnum = (markpointnum + 1) % AM_NUMMARKPOINTS
    }

    /**
     * Determines bounding box of all vertices, sets global variables
     * controlling zoom range.
     */
    fun findMinMaxBoundaries() {
        val a: Int // fixed_t
        val b: Int
        min_y = Limits.MAXINT
        min_x = min_y
        max_y = -Limits.MAXINT
        max_x = max_y
        for (i in 0 until DOOM.levelLoader.numvertexes) {
            if (DOOM.levelLoader.vertexes!![i].x < min_x) min_x =
                DOOM.levelLoader.vertexes!![i].x else if (DOOM.levelLoader.vertexes!![i].x > max_x) max_x =
                DOOM.levelLoader.vertexes!![i].x
            if (DOOM.levelLoader.vertexes!![i].y < min_y) min_y =
                DOOM.levelLoader.vertexes!![i].y else if (DOOM.levelLoader.vertexes!![i].y > max_y) max_y =
                DOOM.levelLoader.vertexes!![i].y
        }
        max_w = max_x - min_x
        max_h = max_y - min_y
        min_w = 2 * Defines.PLAYERRADIUS // const? never changed?
        min_h = 2 * Defines.PLAYERRADIUS
        a = fixed_t.FixedDiv(f_w shl fixed_t.FRACBITS, max_w)
        b = fixed_t.FixedDiv(f_h shl fixed_t.FRACBITS, max_h)
        min_scale_mtof = if (a < b) a else b
        if (min_scale_mtof < 0) {
            // MAES: safeguard against negative scaling e.g. in Europe.wad
            // This seems to be the limit.
            min_scale_mtof = MINIMUM_VIABLE_SCALE
        }
        max_scale_mtof = fixed_t.FixedDiv(f_h shl fixed_t.FRACBITS, 2 * Defines.PLAYERRADIUS)
    }

    fun changeWindowLoc() {
        if (m_paninc.x != 0 || m_paninc.y != 0) {
            followplayer = false
            f_oldloc.x = Limits.MAXINT
        }
        m_x += m_paninc.x
        m_y += m_paninc.y
        if (m_x + m_w / 2 > max_x) m_x = max_x - m_w / 2 else if (m_x + m_w / 2 < min_x) m_x = min_x - m_w / 2
        if (m_y + m_h / 2 > max_y) m_y = max_y - m_h / 2 else if (m_y + m_h / 2 < min_y) m_y = min_y - m_h / 2
        m_x2 = m_x + m_w
        m_y2 = m_y + m_h
    }

    fun initVariables() {
        var pnum: Int
        DOOM.automapactive = true
        f_oldloc.x = Limits.MAXINT
        amclock = 0
        lightlev = 0
        m_paninc.y = 0
        m_paninc.x = m_paninc.y
        ftom_zoommul = fixed_t.FRACUNIT
        mtof_zoommul = fixed_t.FRACUNIT
        m_w = FTOM(f_w)
        m_h = FTOM(f_h)

        // find player to center on initially
        if (!DOOM.playeringame[DOOM.consoleplayer.also { pnum = it }]) {
            pnum = 0
            while (pnum < Limits.MAXPLAYERS) {
                println(pnum)
                if (DOOM.playeringame[pnum]) break
                pnum++
            }
        }
        plr = DOOM.players[pnum]!!
        val plr_mo = plr.mo!!
        m_x = plr_mo._x - m_w / 2
        m_y = plr_mo._y - m_h / 2
        changeWindowLoc()

        // for saving & restoring
        old_m_x = m_x
        old_m_y = m_y
        old_m_w = m_w
        old_m_h = m_h

        // inform the status bar of the change
        DOOM.statusBar.NotifyAMEnter()
    }

    //
    //
    //
    fun loadPics() {
        var i: Int
        var namebuf: String
        i = 0
        while (i < 10) {
            namebuf = "AMMNUM$i"
            marknums[i] = DOOM.wadLoader.CachePatchName(namebuf)
            i++
        }
    }

    fun unloadPics() {
        var i: Int
        i = 0
        while (i < 10) {
            DOOM.wadLoader.UnlockLumpNum(marknums[i])
            i++
        }
    }

/*
    public final void clearMarks() {
        int i;

        for (i = 0; i < AM_NUMMARKPOINTS; i++)
            markpoints[i].x = -1; // means empty
        markpointnum = 0;
    }
*/
    fun clearMarks() {
        var i: Int
        i = 0
        while (i < AM_NUMMARKPOINTS) {
            markpoints[i].x = -1 // means empty
            i++
        }
        markpointnum = 0
    }

    /**
     * should be called at the start of every level right now, i figure it out
     * myself
     */
    fun LevelInit() {
        leveljuststarted = 0
        f_y = 0
        f_x = f_y
        f_w = finit_width
        f_h = finit_height
        f_rect = Rectangle(0, 0, f_w, f_h)

        // scanline=new byte[f_h*f_w];
        clearMarks()
        findMinMaxBoundaries()
        scale_mtof = fixed_t.FixedDiv(min_scale_mtof, MINIMUM_SCALE)
        if (scale_mtof > max_scale_mtof) scale_mtof = min_scale_mtof
        scale_ftom = fixed_t.FixedDiv(fixed_t.FRACUNIT, scale_mtof)
        plotter.setThickness(
            Math.min(MTOF(fixed_t.FRACUNIT), DOOM.graphicSystem.getScalingX()),
            Math.min(MTOF(fixed_t.FRACUNIT), DOOM.graphicSystem.getScalingY())
        )
    }

    override fun Stop() {
        unloadPics()
        DOOM.automapactive = false
        // This is the only way to notify the status bar responder that we're
        // exiting the automap.
        DOOM.statusBar.NotifyAMExit()
        stopped = true
    }

    // More "static" stuff.
    protected var lastlevel = -1
    protected var lastepisode = -1
    override fun Start() {
        if (!stopped) Stop()
        stopped = false
        if (lastlevel != DOOM.gamemap || lastepisode != DOOM.gameepisode) {
            LevelInit()
            lastlevel = DOOM.gamemap
            lastepisode = DOOM.gameepisode
        }
        initVectorGraphics()
        LevelInit()
        initVariables()
        loadPics()
    }

    /**
     * set the window scale to the maximum size
     */
    fun minOutWindowScale() {
        scale_mtof = min_scale_mtof
        scale_ftom = fixed_t.FixedDiv(fixed_t.FRACUNIT, scale_mtof)
        plotter.setThickness(DOOM.graphicSystem.getScalingX(), DOOM.graphicSystem.getScalingY())
        activateNewScale()
    }

    /**
     * set the window scale to the minimum size
     */
    fun maxOutWindowScale() {
        scale_mtof = max_scale_mtof
        scale_ftom = fixed_t.FixedDiv(fixed_t.FRACUNIT, scale_mtof)
        plotter.setThickness(0, 0)
        activateNewScale()
    }

    /** These belong to AM_Responder  */
    protected var cheatstate = false
    protected var bigstate = false

    /** static char buffer[20] in AM_Responder  */
    protected var buffer: String? = null

    /**
     * Handle events (user inputs) in automap mode
     */
    @AM_Map.C(AM_Map.AM_Responder)
    override fun Responder(ev: event_t): Boolean {
        var rc: Boolean
        rc = false

        // System.out.println(ev.data1==AM_STARTKEY);
        if (!DOOM.automapactive) {
            if (ev.isKey(AM_STARTKEY, evtype_t.ev_keyup)) {
                Start()
                DOOM.viewactive = false
                rc = true
            }
        } else if (ev.isType(evtype_t.ev_keydown)) {
            rc = true
            if (ev.isKey(AM_PANRIGHTKEY)) { // pan right
                if (!followplayer) m_paninc.x = FTOM(F_PANINC) else rc = false
            } else if (ev.isKey(AM_PANLEFTKEY)) { // pan left
                if (!followplayer) m_paninc.x = -FTOM(F_PANINC) else rc = false
            } else if (ev.isKey(AM_PANUPKEY)) { // pan up
                if (!followplayer) m_paninc.y = FTOM(F_PANINC) else rc = false
            } else if (ev.isKey(AM_PANDOWNKEY)) { // pan down
                if (!followplayer) m_paninc.y = -FTOM(F_PANINC) else rc = false
            } else if (ev.isKey(AM_ZOOMOUTKEY)) { // zoom out
                mtof_zoommul = M_ZOOMOUT
                ftom_zoommul = M_ZOOMIN
            } else if (ev.isKey(AM_ZOOMINKEY)) { // zoom in
                mtof_zoommul = M_ZOOMIN
                ftom_zoommul = M_ZOOMOUT
            } else if (ev.isKey(AM_GOBIGKEY)) {
                bigstate = !bigstate
                if (bigstate) {
                    saveScaleAndLoc()
                    minOutWindowScale()
                } else restoreScaleAndLoc()
            } else if (ev.isKey(AM_FOLLOWKEY)) {
                followplayer = !followplayer
                f_oldloc.x = Limits.MAXINT
                plr.message = if (followplayer) englsh.AMSTR_FOLLOWON else englsh.AMSTR_FOLLOWOFF
            } else if (ev.isKey(AM_GRIDKEY)) {
                grid = !grid
                plr.message = if (grid) englsh.AMSTR_GRIDON else englsh.AMSTR_GRIDOFF
            } else if (ev.isKey(AM_MARKKEY)) {
                buffer = englsh.AMSTR_MARKEDSPOT + " " + markpointnum
                plr.message = buffer
                addMark()
            } else if (ev.isKey(AM_CLEARMARKKEY)) {
                clearMarks()
                plr.message = englsh.AMSTR_MARKSCLEARED
            } else {
                cheatstate = false
                rc = false
            }
            if (!DOOM.deathmatch && ev.ifKeyAsciiChar { key: Int -> cheat_amap.CheckCheat(key) }) {
                rc = false
                cheating = (cheating + 1) % 3
            }
            /**
             * MAES: brought back strobe effect
             * Good Sign: setting can be saved/loaded from config
             */
            if (ev.ifKeyAsciiChar { key: Int -> cheat_strobe.CheckCheat(key) }) {
                DOOM.mapstrobe = !DOOM.mapstrobe
            }
        } else if (ev.isType(evtype_t.ev_keyup)) {
            rc = false
            if (ev.isKey(AM_PANRIGHTKEY)) {
                if (!followplayer) m_paninc.x = 0
            } else if (ev.isKey(AM_PANLEFTKEY)) {
                if (!followplayer) m_paninc.x = 0
            } else if (ev.isKey(AM_PANUPKEY)) {
                if (!followplayer) m_paninc.y = 0
            } else if (ev.isKey(AM_PANDOWNKEY)) {
                if (!followplayer) m_paninc.y = 0
            } else if (ev.isKey(AM_ZOOMOUTKEY) || ev.isKey(AM_ZOOMINKEY)) {
                mtof_zoommul = fixed_t.FRACUNIT
                ftom_zoommul = fixed_t.FRACUNIT
            } else if (ev.isKey(AM_ENDKEY)) {
                bigstate = false
                DOOM.viewactive = true
                Stop()
            }
        }
        return rc
    }

    /**
     * Zooming
     */
    private fun changeWindowScale() {

        // Change the scaling multipliers
        scale_mtof = fixed_t.FixedMul(scale_mtof, mtof_zoommul)
        scale_ftom = fixed_t.FixedDiv(fixed_t.FRACUNIT, scale_mtof)
        if (scale_mtof < min_scale_mtof) minOutWindowScale() else if (scale_mtof > max_scale_mtof) maxOutWindowScale() else activateNewScale()
    }

    //
    //
    //
    private fun doFollowPlayer() {
        val plr_mo = plr.mo!!
        if (f_oldloc.x != plr_mo._x || f_oldloc.y != plr_mo._y) {
            m_x = FTOM(MTOF(plr_mo._x)) - m_w / 2
            m_y = FTOM(MTOF(plr_mo._y)) - m_h / 2
            m_x2 = m_x + m_w
            m_y2 = m_y + m_h
            f_oldloc.x = plr_mo._x
            f_oldloc.y = plr_mo._y

            // m_x = FTOM(MTOF(plr.mo.x - m_w/2));
            // m_y = FTOM(MTOF(plr.mo.y - m_h/2));
            // m_x = plr.mo.x - m_w/2;
            // m_y = plr.mo.y - m_h/2;
        }
    }

    private fun updateLightLev() {
        // Change light level
        // no more buggy nexttic - Good Sign 2017/04/01
        // no more additional lightlevelcnt - Good Sign 2017/04/05
        // no more even lightlev and changed to array access - Good Sign 2017/04/08
        if (amclock % 6 == 0) {
            val sourceLength = Color.NUM_LITES
            val intermeditate = DOOM.graphicSystem.convertPalettedBlock(0.toByte())
            litedColorSources.forEach { (c: Color?, source: V) ->
                GenericCopy.memcpy(source, sourceLength - 1, intermeditate, 0, 1)
                GenericCopy.memcpy(source, 0, source, 1, sourceLength - 1)
                GenericCopy.memcpy(intermeditate, 0, source, 0, 1)
            }
        }
    }

    /**
     * Updates on Game Tick
     */
    override fun Ticker() {
        if (!DOOM.automapactive || DOOM.menuactive) return
        amclock++
        if (followplayer) doFollowPlayer()

        // Change the zoom if necessary
        if (ftom_zoommul != fixed_t.FRACUNIT) changeWindowScale()

        // Change x,y location
        if (m_paninc.x or m_paninc.y != 0) changeWindowLoc()

        // Update light level
        if (DOOM.mapstrobe) updateLightLev()
    }
    // private static int BUFFERSIZE=f_h*f_w;
    /**
     * Automap clipping of lines. Based on Cohen-Sutherland clipping algorithm
     * but with a slightly faster reject and precalculated slopes. If the speed
     * is needed, use a hash algorithm to handle the common cases.
     */
    private var tmpx = 0
    private var tmpy // =new fpoint_t();
            = 0

    private fun clipMline(ml: mline_t, fl: fline_t): Boolean {

        // System.out.print("Asked to clip from "+FixedFloat.toFloat(ml.a.x)+","+FixedFloat.toFloat(ml.a.y));
        // System.out.print(" to clip "+FixedFloat.toFloat(ml.b.x)+","+FixedFloat.toFloat(ml.b.y)+"\n");
        // These were supposed to be "registers", so they exhibit by-ref
        // properties.
        var outcode1 = 0
        var outcode2 = 0
        var outside: Int
        var dx: Int
        var dy: Int
        /*
         * fl.a.x=0; fl.a.y=0; fl.b.x=0; fl.b.y=0;
         */

        // do trivial rejects and outcodes
        if (ml.ay > m_y2) outcode1 = TOP else if (ml.ay < m_y) outcode1 = BOTTOM
        if (ml.by > m_y2) outcode2 = TOP else if (ml.by < m_y) outcode2 = BOTTOM
        if (outcode1 and outcode2 != 0) return false // trivially outside
        if (ml.ax < m_x) outcode1 = outcode1 or LEFT else if (ml.ax > m_x2) outcode1 = outcode1 or RIGHT
        if (ml.bx < m_x) outcode2 = outcode2 or LEFT else if (ml.bx > m_x2) outcode2 = outcode2 or RIGHT
        if (outcode1 and outcode2 != 0) return false // trivially outside

        // transform to frame-buffer coordinates.
        fl.ax = CXMTOF(ml.ax)
        fl.ay = CYMTOF(ml.ay)
        fl.bx = CXMTOF(ml.bx)
        fl.by = CYMTOF(ml.by)

        // System.out.println(">>>>>> ("+fl.a.x+" , "+fl.a.y+" ),("+fl.b.x+" , "+fl.b.y+" )");
        outcode1 = DOOUTCODE(fl.ax, fl.ay)
        outcode2 = DOOUTCODE(fl.bx, fl.by)
        if (outcode1 and outcode2 != 0) return false
        while (outcode1 or outcode2 != 0) {
            // may be partially inside box
            // find an outside point
            outside = if (outcode1 != 0) outcode1 else outcode2

            // clip to each side
            if (outside and TOP != 0) {
                dy = fl.ay - fl.by
                dx = fl.bx - fl.ax
                tmpx = fl.ax + dx * fl.ay / dy
                tmpy = 0
            } else if (outside and BOTTOM != 0) {
                dy = fl.ay - fl.by
                dx = fl.bx - fl.ax
                tmpx = fl.ax + dx * (fl.ay - f_h) / dy
                tmpy = f_h - 1
            } else if (outside and RIGHT != 0) {
                dy = fl.by - fl.ay
                dx = fl.bx - fl.ax
                tmpy = fl.ay + dy * (f_w - 1 - fl.ax) / dx
                tmpx = f_w - 1
            } else if (outside and LEFT != 0) {
                dy = fl.by - fl.ay
                dx = fl.bx - fl.ax
                tmpy = fl.ay + dy * -fl.ax / dx
                tmpx = 0
            }
            if (outside == outcode1) {
                fl.ax = tmpx
                fl.ay = tmpy
                outcode1 = DOOUTCODE(fl.ax, fl.ay)
            } else {
                fl.bx = tmpx
                fl.by = tmpy
                outcode2 = DOOUTCODE(fl.bx, fl.by)
            }
            if (outcode1 and outcode2 != 0) return false // trivially outside
        }
        return true
    }

    /**
     * MAES: the result was supposed to be passed in an "oc" parameter by
     * reference. Not convenient, so I made some changes...
     *
     * @param mx
     * @param my
     */
    private fun DOOUTCODE(mx: Int, my: Int): Int {
        var oc = 0
        if (my < 0) oc = oc or TOP else if (my >= f_h) oc = oc or BOTTOM
        if (mx < 0) oc = oc or LEFT else if (mx >= f_w) oc = oc or RIGHT
        return oc
    }

    /** Not my idea ;-)  */
    protected var fuck = 0

    /**
     * Clip lines, draw visible parts of lines.
     */
    protected var singlepixel = 0
    private fun drawMline(ml: mline_t, colorSource: V?) {
        // fl.reset();
        if (clipMline(ml, fl)) {
            // if ((fl.a.x==fl.b.x)&&(fl.a.y==fl.b.y)) singlepixel++;
            // draws the line using coords
            DOOM.graphicSystem
                .drawLine(
                    plotter
                        .setColorSource(colorSource, 0)
                        .setPosition(fl.ax, fl.ay),
                    fl.bx, fl.by
                )
        }
    }

    private val fl = fline_t()
    private val ml = mline_t()

    /**
     * Draws flat (floor/ceiling tile) aligned grid lines.
     */
    private fun drawGrid(colorSource: V?) {
        var x: Int
        var y: Int // fixed_t
        var start: Int
        var end: Int // fixed_t

        // Figure out start of vertical gridlines
        start = m_x
        if ((start - DOOM.levelLoader.bmaporgx) % (Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS) != 0) start += (Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS) - (start - DOOM.levelLoader.bmaporgx) % (Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS)
        end = m_x + m_w

        // draw vertical gridlines
        ml.ay = m_y
        ml.by = m_y + m_h
        x = start
        while (x < end) {
            ml.ax = x
            ml.bx = x
            drawMline(ml, colorSource)
            x += Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS
        }

        // Figure out start of horizontal gridlines
        start = m_y
        if ((start - DOOM.levelLoader.bmaporgy) % (Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS) != 0) start += (Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS) - (start - DOOM.levelLoader.bmaporgy) % (Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS)
        end = m_y + m_h

        // draw horizontal gridlines
        ml.ax = m_x
        ml.bx = m_x + m_w
        y = start
        while (y < end) {
            ml.ay = y
            ml.by = y
            drawMline(ml, colorSource)
            y += Defines.MAPBLOCKUNITS shl fixed_t.FRACBITS
        }
    }

    protected var l = mline_t()

    /**
     * Determines visible lines, draws them. This is LineDef based, not LineSeg
     * based.
     */
    private fun drawWalls() {
        val teleColorSource = litedColorSources[TELECOLORS]
        val wallColorSource = litedColorSources[WALLCOLORS]
        val fdWallColorSource = litedColorSources[FDWALLCOLORS]
        val cdWallColorSource = litedColorSources[CDWALLCOLORS]
        val tsWallColorSource = litedColorSources[TSWALLCOLORS]
        val secretWallColorSource = litedColorSources[SECRETWALLCOLORS]
        for (i in 0 until DOOM.levelLoader.numlines) {
            l.ax = DOOM.levelLoader.lines[i].v1x
            l.ay = DOOM.levelLoader.lines[i].v1y
            l.bx = DOOM.levelLoader.lines[i].v2x
            l.by = DOOM.levelLoader.lines[i].v2y
            if (cheating or (DOOM.levelLoader.lines[i].flags.toInt() and line_t.ML_MAPPED) != 0) {
                if (DOOM.levelLoader.lines[i].flags.toInt() and LINE_NEVERSEE.toInt() and cheating.inv() != 0) continue
                if (DOOM.levelLoader.lines[i].backsector == null) {
                    drawMline(l, wallColorSource)
                } else {
                    if (DOOM.levelLoader.lines[i].special.toInt() == 39) { // teleporters
                        drawMline(l, teleColorSource)
                    } else if (DOOM.levelLoader.lines[i].flags.toInt() and line_t.ML_SECRET != 0) // secret
                    // door
                    {
                        if (cheating != 0) drawMline(l, secretWallColorSource) else drawMline(l, wallColorSource)
                    } else if (DOOM.levelLoader.lines[i].backsector?.floorheight != DOOM.levelLoader.lines[i].frontsector?.floorheight) {
                        drawMline(l, fdWallColorSource) // floor level change
                    } else if (DOOM.levelLoader.lines[i].backsector?.ceilingheight != DOOM.levelLoader.lines[i].frontsector?.ceilingheight) {
                        drawMline(l, cdWallColorSource) // ceiling level change
                    } else if (cheating != 0) {
                        drawMline(l, tsWallColorSource)
                    }
                }
            } else if (plr.powers[Defines.pw_allmap] != 0) {
                // Some are never seen even with that!
                if (DOOM.levelLoader.lines[i].flags.toInt() and LINE_NEVERSEE.toInt() == 0) drawMline(
                    l,
                    litedColorSources[MAPPOWERUPSHOWNCOLORS]
                )
            }
        }

        // System.out.println("Single pixel draws: "+singlepixel+" out of "+P.lines.length);
        // singlepixel=0;
    }

    //
    // Rotation in 2D.
    // Used to rotate player arrow line character.
    //
    private var rotx = 0
    private var roty = 0

    init {
        // Some initializing...
        markpoints = malloc({ mpoint_t() }, AM_NUMMARKPOINTS)
        f_oldloc = mpoint_t()
        m_paninc = mpoint_t()
        plotter = DOOM.graphicSystem.createPlotter(DoomScreen.FG)
        plr = DOOM.players[DOOM.displayplayer]!!
        Repalette()
        // Pre-scale stuff.
        finit_width = DOOM.vs.getScreenWidth()
        finit_height = DOOM.vs.getScreenHeight() - 32 * DOOM.vs.getSafeScaling()
    }

    /**
     * Rotation in 2D. Used to rotate player arrow line character.
     *
     * @param x
     * fixed_t
     * @param y
     * fixed_t
     * @param a
     * angle_t -> this should be a LUT-ready BAM.
     */
    private fun rotate(x: Int, y: Int, a: Int) {
        // int tmpx;
        rotx = fixed_t.FixedMul(x, Tables.finecosine[a]) - fixed_t.FixedMul(y, Tables.finesine[a])
        roty = fixed_t.FixedMul(x, Tables.finesine[a]) + fixed_t.FixedMul(y, Tables.finecosine[a])

        // rotx.val = tmpx;
    }

    private fun drawLineCharacter(
        lineguy: Array<mline_t>, lineguylines: Int,
        scale: Int,  // fixed_t
        angle: Int,  // This should be a LUT-ready angle.
        colorSource: V?,
        x: Int,  // fixed_t
        y: Int // fixed_t
    ) {
        var i: Int
        val rotate = angle != 0
        val l = mline_t()
        i = 0
        while (i < lineguylines) {
            l.ax = lineguy[i].ax
            l.ay = lineguy[i].ay
            if (scale != 0) {
                l.ax = fixed_t.FixedMul(scale, l.ax)
                l.ay = fixed_t.FixedMul(scale, l.ay)
            }
            if (rotate) {
                rotate(l.ax, l.ay, angle)
                // MAES: assign rotations
                l.ax = rotx
                l.ay = roty
            }
            l.ax += x
            l.ay += y
            l.bx = lineguy[i].bx
            l.by = lineguy[i].by
            if (scale != 0) {
                l.bx = fixed_t.FixedMul(scale, l.bx)
                l.by = fixed_t.FixedMul(scale, l.by)
            }
            if (rotate) {
                rotate(l.bx, l.by, angle)
                // MAES: assign rotations
                l.bx = rotx
                l.by = roty
            }
            l.bx += x
            l.by += y
            drawMline(l, colorSource)
            i++
        }
    }

    fun drawPlayers() {
        var p: player_t
        var their_color = -1
        var colorSource: V?

        // System.out.println(Long.toHexString(plr.mo.angle));
        if (!DOOM.netgame) {
            val plr_mo = plr.mo!!
            if (cheating != 0) drawLineCharacter(
                cheat_player_arrow, NUMCHEATPLYRLINES, 0,
                Tables.toBAMIndex(plr_mo.angle), fixedColorSources[Color.WHITE], plr_mo._x,
                plr_mo._y
            ) else drawLineCharacter(
                player_arrow, NUMPLYRLINES, 0,
                Tables.toBAMIndex(plr_mo.angle), fixedColorSources[Color.WHITE], plr_mo._x,
                plr_mo._y
            )
            return
        }
        for (i in 0 until Limits.MAXPLAYERS) {
            their_color++
            p = DOOM.players[i]
            if (DOOM.deathmatch && !DOOM.singledemo && p !== plr) continue
            if (!DOOM.playeringame[i]) continue
            colorSource =
                if (p.powers[Defines.pw_invisibility] != 0) fixedColorSources[Color.CLOSE_TO_BLACK] else fixedColorSources[THEIR_COLORS[their_color]]
            val p_mo = p.mo!!
            drawLineCharacter(player_arrow, NUMPLYRLINES, 0, p_mo.angle.toInt(), colorSource, p_mo._x, p_mo._y)
        }
    }

    fun drawThings(colors: Color, colorrange: Int) {
        var t: mobj_t?
        val colorSource = litedColorSources[colors] // Ain't gonna change
        for (i in 0 until DOOM.levelLoader.numsectors) {
            // MAES: get first on the list.
            t = DOOM.levelLoader.sectors[i].thinglist
            while (t != null) {
                drawLineCharacter(
                    thintriangle_guy, NUMTHINTRIANGLEGUYLINES,
                    16 shl fixed_t.FRACBITS, Tables.toBAMIndex(t.angle), colorSource, t._x, t._y
                )
                t = t.snext as mobj_t?
            }
        }
    }

    fun drawMarks() {
        var i: Int
        var fx: Int
        var fy: Int
        var w: Int
        var h: Int
        i = 0
        while (i < AM_NUMMARKPOINTS) {
            if (markpoints[i].x != -1) {
                w = marknums[i]!!.width.toInt()
                h = marknums[i]!!.height.toInt()
                // Nothing wrong with v1.9 IWADs, but I wouldn't put my hand on
                // the fire for older ones.
                // w = 5; // because something's wrong with the wad, i guess
                // h = 6; // because something's wrong with the wad, i guess
                fx = CXMTOF(markpoints[i].x)
                fy = CYMTOF(markpoints[i].y)
                if (fx >= f_x && fx <= f_w - w && fy >= f_y && fy <= f_h - h) DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG,
                    marknums[i]!!,
                    DOOM.vs,
                    fx,
                    fy,
                    DoomGraphicSystem.V_NOSCALESTART
                )
            }
            i++
        }
    }

    private fun drawCrosshair(colorSource: V?) {
        /*plotter.setPosition(
                DOOM.videoRenderer.getScreenWidth() / 2,
                DOOM.videoRenderer.getScreenHeight()/ 2
            ).setColorSource(colorSource, 0)
            .plot();*/
        //fb[(f_w * (f_h + 1)) / 2] = (short) color; // single point for now
    }

    override fun Drawer() {
        if (!DOOM.automapactive) return
        // System.out.println("Drawing map");
        if (overlay < 1) DOOM.graphicSystem.FillRect(DoomScreen.FG, f_rect!!, BACKGROUND.value) // BACKGROUND
        if (grid) drawGrid(fixedColorSources[GRIDCOLORS])
        drawWalls()
        drawPlayers()
        if (cheating == 2) drawThings(THINGCOLORS, IAutoMap.THINGRANGE)
        drawCrosshair(fixedColorSources[CROSSHAIRCOLORS])
        drawMarks()

        //DOOM.videoRenderer.MarkRect(f_x, f_y, f_w, f_h);
    }

    companion object {
        // For use if I do walls with outsides/insides
        // Automap colors
        val BACKGROUND = Color.BLACK
        val YOURCOLORS = Color.WHITE
        val WALLCOLORS = Color.REDS
        val TELECOLORS = Color.DARK_REDS
        val TSWALLCOLORS = Color.GRAYS
        val FDWALLCOLORS = Color.BROWNS
        val CDWALLCOLORS = Color.YELLOWS
        val THINGCOLORS = Color.GREENS
        val SECRETWALLCOLORS = Color.REDS
        val GRIDCOLORS = Color.DARK_GREYS
        val MAPPOWERUPSHOWNCOLORS = Color.GRAYS
        val CROSSHAIRCOLORS = Color.GRAYS
        val GENERATE_LITE_LEVELS_FOR = EnumSet.of(
            TELECOLORS,
            WALLCOLORS,
            FDWALLCOLORS,
            CDWALLCOLORS,
            TSWALLCOLORS,
            SECRETWALLCOLORS,
            MAPPOWERUPSHOWNCOLORS,
            THINGCOLORS
        )
        val THEIR_COLORS = arrayOf(
            Color.GREENS,
            Color.GRAYS,
            Color.BROWNS,
            Color.REDS
        )

        // drawing stuff
        val AM_PANDOWNKEY = ScanCode.SC_DOWN
        val AM_PANUPKEY = ScanCode.SC_UP
        val AM_PANRIGHTKEY = ScanCode.SC_RIGHT
        val AM_PANLEFTKEY = ScanCode.SC_LEFT
        val AM_ZOOMINKEY = ScanCode.SC_EQUALS
        val AM_ZOOMOUTKEY = ScanCode.SC_MINUS
        val AM_STARTKEY = ScanCode.SC_TAB
        val AM_ENDKEY = ScanCode.SC_TAB
        val AM_GOBIGKEY = ScanCode.SC_0
        val AM_FOLLOWKEY = ScanCode.SC_F
        val AM_GRIDKEY = ScanCode.SC_G
        val AM_MARKKEY = ScanCode.SC_M
        val AM_CLEARMARKKEY = ScanCode.SC_C
        const val AM_NUMMARKPOINTS = 10

        // (fixed_t) scale on entry
        const val INITSCALEMTOF = (.2 * fixed_t.FRACUNIT).toInt()

        // how much the automap moves window per tic in frame-buffer coordinates
        // moves 140 pixels in 1 second
        const val F_PANINC = 4

        // how much zoom-in per tic
        // goes to 2x in 1 second
        const val M_ZOOMIN = (1.02 * fixed_t.FRACUNIT).toInt()

        // how much zoom-out per tic
        // pulls out to 0.5x in 1 second
        const val M_ZOOMOUT = (fixed_t.FRACUNIT / 1.02).toInt()

        // the following is crap
        const val LINE_NEVERSEE = line_t.ML_DONTDRAW.toShort()

        // This seems to be the minimum viable scale before things start breaking
        // up.
        private const val MINIMUM_SCALE = (0.7 * fixed_t.FRACUNIT).toInt()

        // This seems to be the limit for some maps like europe.wad
        private const val MINIMUM_VIABLE_SCALE = fixed_t.FRACUNIT shr 5
        protected var LEFT = 1
        protected var RIGHT = 2
        protected var BOTTOM = 4
        protected var TOP = 8
    }
}