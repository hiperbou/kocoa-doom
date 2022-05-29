package f

import data.Defines
import data.Defines.PU_STATIC
import data.Limits
import data.sounds
import defines.Language_t
import doom.*
import doom.SourceCode.Compatible
import doom.SourceCode.WI_Stuff
import rr.patch_t
import v.DoomGraphicSystem
import v.renderers.DoomScreen
import w.animenum_t

/*
//-----------------------------------------------------------------------------
//
// $Id: EndLevel.java,v 1.11 2012/09/24 17:16:23 velktron Exp $
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
// $Log: EndLevel.java,v $
// Revision 1.11  2012/09/24 17:16:23  velktron
// Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
// Revision 1.8.2.2  2012/09/24 16:57:43  velktron
// Addressed generics warnings.
//
// Revision 1.8.2.1  2011/11/27 18:18:34  velktron
// Use cacheClear() on deactivation.
//
// Revision 1.8  2011/11/01 19:02:57  velktron
// Using screen number constants
//
// Revision 1.7  2011/10/23 18:11:32  velktron
// Generic compliance for DoomVideoInterface
//
// Revision 1.6  2011/08/23 16:13:53  velktron
// Got rid of Z remnants.
//
// Revision 1.5  2011/07/31 21:49:38  velktron
// Changed endlevel drawer's behavior to be closer to prBoom+'s. Allows using 1994TU.WAD while backwards compatible.
//
// Revision 1.4  2011/06/02 14:56:48  velktron
// imports
//
// Revision 1.3  2011/06/02 14:53:21  velktron
// Moved Endlevel constants to AbstractEndLevel
//
// Revision 1.2  2011/06/02 14:14:28  velktron
// Implemented endlevel unloading of graphics, changed state enum.
//
// Revision 1.1  2011/06/02 14:00:48  velktron
// Moved Endlevel stuff  to f, where it makes more sense.
//
// Revision 1.18  2011/05/31 12:25:14  velktron
// Endlevel -mostly- scaled correctly.
//
// Revision 1.17  2011/05/29 22:15:32  velktron
// Introduced IRandom interface.
//
// Revision 1.16  2011/05/24 17:54:02  velktron
// Defaults tester
//
// Revision 1.15  2011/05/23 17:00:39  velktron
// Got rid of verbosity
//
// Revision 1.14  2011/05/21 16:53:24  velktron
// Adapted to use new gamemode system.
//
// Revision 1.13  2011/05/18 16:58:04  velktron
// Changed to DoomStatus
//
// Revision 1.12  2011/05/17 16:52:19  velktron
// Switched to DoomStatus
//
// Revision 1.11  2011/05/11 14:12:08  velktron
// Interfaced with DoomGame
//
// Revision 1.10  2011/05/10 10:39:18  velktron
// Semi-playable Techdemo v1.3 milestone
//
// Revision 1.9  2011/05/06 14:00:54  velktron
// More of _D_'s changes committed.
//
// Revision 1.8  2011/02/11 00:11:13  velktron
// A MUCH needed update to v1.3.
//
// Revision 1.7  2010/12/20 17:15:08  velktron
// Made the renderer more OO -> TextureManager and other changes as well.
//
// Revision 1.6  2010/11/12 13:37:25  velktron
// Rationalized the LUT system - now it's 100% procedurally generated.
//
// Revision 1.5  2010/09/23 07:31:11  velktron
// fuck
//
// Revision 1.4  2010/09/02 15:56:54  velktron
// Bulk of unified renderer copyediting done.
//
// Some changes like e.g. global separate limits class and instance methods for seg_t and node_t introduced.
//
// Revision 1.3  2010/08/23 14:36:08  velktron
// Menu mostly working, implemented Killough's fast hash-based GetNumForName, although it can probably be finetuned even more.
//
// Revision 1.2  2010/08/13 14:06:36  velktron
// Endlevel screen fully functional!
//
// Revision 1.1  2010/07/06 16:32:38  velktron
// Threw some work in WI, now EndLevel. YEAH THERE'S GONNA BE A SEPARATE EndLevel OBJECT THAT'S HOW PIMP THE PROJECT IS!!!!11!!!
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
//	Intermission screens.
//
//-----------------------------------------------------------------------------*/
/**
 * This class (stuff.c) seems to implement the endlevel screens.
 *
 * @author Maes
 */
class EndLevel<T, V>(  ////////////////// STATUS ///////////////////
    private val DOOM: DoomMain<T, V>
) : AbstractEndLevel() {
    enum class endlevel_state {
        NoState, StatCount, ShowNextLoc, JustShutOff
    }

    protected var SP_PAUSE = 1

    // in seconds
    protected var SHOWNEXTLOCDELAY = 4
    protected var SHOWLASTLOCDELAY = SHOWNEXTLOCDELAY

    // used to accelerate or skip a stage
    var acceleratestage = 0

    // wbs->pnum
    var me = 0

    // specifies current state )
    var state: endlevel_state? = null

    // contains information passed into intermission
    var wbs: wbstartstruct_t? = null
    lateinit var plrs // wbs->plyr[]
            : Array<wbplayerstruct_t>

    // used for general timing
    var cnt = 0

    // used for timing of background animation
    var bcnt = 0

    // signals to refresh everything for one frame
    var firstrefresh = 0
    var cnt_kills = IntArray(Limits.MAXPLAYERS)
    var cnt_items = IntArray(Limits.MAXPLAYERS)
    var cnt_secret = IntArray(Limits.MAXPLAYERS)
    var cnt_time = 0
    var cnt_par = 0
    var cnt_pause = 0

    // # of commercial levels
    var NUMCMAPS = 0

    //
    //	GRAPHICS
    //
    // background (map of levels).
    var bg: patch_t? = null

    // You Are Here graphic
    var yah = arrayOfNulls<patch_t>(3)

    // splat
    lateinit var splat: Array<patch_t?>

    /**
     * %, : graphics
     */
    var percent: patch_t? = null
    var colon: patch_t? = null

    /**
     * 0-9 graphic
     */
    var num = arrayOfNulls<patch_t>(10)

    /**
     * minus sign
     */
    var wiminus: patch_t? = null

    // "Finished!" graphics
    var finished: patch_t? = null

    // "Entering" graphic
    var entering: patch_t? = null

    // "secret"
    var sp_secret: patch_t? = null

    /**
     * "Kills", "Scrt", "Items", "Frags"
     */
    var kills: patch_t? = null
    var secret: patch_t? = null
    var items: patch_t? = null
    var frags: patch_t? = null

    /**
     * Time sucks.
     */
    var time: patch_t? = null
    var par: patch_t? = null
    var sucks: patch_t? = null

    /**
     * "killers", "victims"
     */
    var killers: patch_t? = null
    var victims: patch_t? = null

    /**
     * "Total", your face, your dead face
     */
    var total: patch_t? = null
    var star: patch_t? = null
    var bstar: patch_t? = null

    /**
     * "red P[1..MAXPLAYERS]"
     */
    var p = arrayOfNulls<patch_t>(Limits.MAXPLAYERS)

    /**
     * "gray P[1..MAXPLAYERS]"
     */
    var bp = arrayOfNulls<patch_t>(Limits.MAXPLAYERS)

    /**
     * Name graphics of each level (centered)
     */
    var lnames: Array<patch_t?>? = null
    protected fun slamBackground() {
        //    memcpy(screens[0], screens[1], DOOM.vs.getScreenWidth() * DOOM.vs.getScreenHeight());
        // Remember, the second arg is the source!
        DOOM.graphicSystem.screenCopy(DoomScreen.BG, DoomScreen.FG)
        //System.arraycopy(V.getScreen(SCREEN_BG), 0 ,V.getScreen(SCREEN_FG),0, DOOM.vs.getScreenWidth() * DOOM.vs.getScreenHeight());
        //V.MarkRect (0, 0, DOOM.vs.getScreenWidth(), DOOM.vs.getScreenHeight());
    }

    // The ticker is used to detect keys
    //  because of timing issues in netgames.
    fun Responder(ev: event_t?): Boolean {
        return false
    }

    /**
     * Draws "<Levelname> Finished!"
    </Levelname> */
    protected fun drawLF() {
        var y = WI_TITLEY

        // draw <LevelName> 
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            lnames!![wbs!!.last]!!,
            DOOM.vs,
            (320 - lnames!![wbs!!.last]!!.width) / 2,
            y
        )

        // draw "Finished!"
        y += 5 * lnames!![wbs!!.last]!!.height / 4
        DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, finished!!, DOOM.vs, (320 - finished!!.width) / 2, y)
    }

    /**
     * Draws "Entering <LevelName>"
    </LevelName> */
    protected fun drawEL() {
        var y = WI_TITLEY // This is in 320 x 200 coords!

        // draw "Entering"
        DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, entering!!, DOOM.vs, (320 - entering!!.width) / 2, y)

        // HACK: if lnames!![wbs.next] DOES have a defined nonzero topoffset, use it.
        // implicitly in DrawScaledPatch, and trump the normal behavior.
        // FIXME: this is only useful in a handful of prBoom+ maps, which use
        // a modified endlevel screen. The reason it works there is the behavior of the 
        // unified patch drawing function, which is approximated with this hack.
        if (lnames!![wbs!!.next]!!.topoffset.toInt() == 0) {
            y += 5 * lnames!![wbs!!.next]!!.height / 4
        }
        // draw level.
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            lnames!![wbs!!.next]!!,
            DOOM.vs,
            (320 - lnames!![wbs!!.next]!!.width) / 2,
            y
        )
    }

    /**
     * Fixed the issue with splat patch_t[] - a crash caused by null in array - by importing fix from prboom-plus. The
     * issue was: developers intended to be able to pass one patch_t or two at once, when they pass one, they use a
     * pointer expecting an array but without a real array, producing UB. At first, I've bring back an array by redoing
     * splat as patch_t[] instead of single patch_t. Secondly, I've emulated UB by allowing null to be found in splat
     * array Finally, I've 'fixed' this imaginary UB by testing against null, as it is done in prboom-plus.
     *
     * So at the moment it should work exactly as in vanilla if it would not crash. However, additional testing may
     * apply to revert this fix. - Good Sign 2017/04/04
     *
     * For whatever fucked-up reason, it expects c to be an array of patches, and may choose to draw from alternative
     * ones...which however are never loaded, or are supposed to be "next" in memory or whatever. I kept this behavior,
     * however in Java it will NOT work as intended, if ever.
     *
     * @param n
     * @param c
     */
    protected fun drawOnLnode(
        n: Int,
        c: Array<patch_t?>
    ) {
        var i: Int
        var left: Int
        var top: Int
        var right: Int
        var bottom: Int
        var fits = false
        i = 0
        do {
            left = AbstractEndLevel.lnodes.get(wbs!!.epsd).get(n).x - c[i]!!.leftoffset
            top = AbstractEndLevel.lnodes.get(wbs!!.epsd).get(n).y - c[i]!!.topoffset
            right = left + c[i]!!.width
            bottom = top + c[i]!!.height
            if (left >= 0 && right < DOOM.vs.getScreenWidth() && top >= 0 && bottom < DOOM.vs.getScreenHeight()) {
                fits = true
            } else {
                i++
            }
        } while (!fits && i != 2 && c[i] != null)
        if (fits && i < 2) {
            //V.DrawPatch(lnodes[wbs.epsd][n].x, lnodes[wbs.epsd][n].y,
            //	    FB, c[i]);
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                c[i]!!,
                DOOM.vs,
                AbstractEndLevel.lnodes.get(wbs!!.epsd).get(n).x,
                AbstractEndLevel.lnodes.get(
                    wbs!!.epsd
                ).get(n).y
            )
        } else {
            // DEBUG
            println("Could not place patch on level " + n + 1)
        }
    }

    @SourceCode.Exact
    @WI_Stuff.C(WI_Stuff.WI_initAnimatedBack)
    protected fun initAnimatedBack() {
        var a: anim_t
        if (DOOM.isCommercial()) {
            return
        }
        if (wbs!!.epsd > 2) {
            return
        }
        for (i in 0 until AbstractEndLevel.NUMANIMS.get(wbs!!.epsd)) {
            a = AbstractEndLevel.anims.get(wbs!!.epsd).get(i)

            // init variables
            a.ctr = -1
            if (null != a.type) when (a.type) {
                animenum_t.ANIM_ALWAYS -> a.nexttic = bcnt + 1 + DOOM.random.M_Random() % a.period
                animenum_t.ANIM_RANDOM -> a.nexttic = bcnt + 1 + a.data2 + DOOM.random.M_Random() % a.data1
                animenum_t.ANIM_LEVEL -> a.nexttic = bcnt + 1
                else -> {}
            }
        }
    }

    protected fun updateAnimatedBack() {
        var i: Int
        var a: anim_t
        if (DOOM.isCommercial()) {
            return
        }
        if (wbs!!.epsd > 2) {
            return
        }
        val aaptr = wbs!!.epsd
        i = 0
        while (i < AbstractEndLevel.NUMANIMS.get(wbs!!.epsd)) {
            a = AbstractEndLevel.anims.get(aaptr).get(i)
            if (bcnt == a.nexttic) {
                when (a.type) {
                    animenum_t.ANIM_ALWAYS -> {
                        if (++AbstractEndLevel.anims.get(aaptr).get(i).ctr >= a.nanims) {
                            a.ctr = 0
                        }
                        a.nexttic = bcnt + a.period
                    }
                    animenum_t.ANIM_RANDOM -> {
                        a.ctr++
                        if (a.ctr == a.nanims) {
                            a.ctr = -1
                            a.nexttic = bcnt + a.data2 + DOOM.random.M_Random() % a.data1
                        } else {
                            a.nexttic = bcnt + a.period
                        }
                    }
                    animenum_t.ANIM_LEVEL ->                         // gawd-awful hack for level anims
                        if (!(state == endlevel_state.StatCount && i == 7)
                            && wbs!!.next == a.data1
                        ) {
                            a.ctr++
                            if (a.ctr == a.nanims) {
                                a.ctr--
                            }
                            a.nexttic = bcnt + a.period
                        }
                }
            }
            i++
        }
    }

    protected fun drawAnimatedBack() {
        var i: Int
        var a: anim_t
        if (DOOM.isCommercial()) {
            return
        }
        if (wbs!!.epsd > 2) {
            return
        }
        i = 0
        while (i < AbstractEndLevel.NUMANIMS.get(wbs!!.epsd)) {
            a = AbstractEndLevel.anims.get(wbs!!.epsd).get(i)
            if (a.ctr >= 0) {
                DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, a.p[a.ctr]!!, DOOM.vs, a.loc.x, a.loc.y)
            }
            i++
        }
    }

    /**
     * Draws a number. If digits > 0, then use that many digits minimum, otherwise only use as many as necessary.
     * Returns new x position.
     */
    protected fun drawNum(x: Int, y: Int, n: Int, digits: Int): Int {
        var x = x
        var n = n
        var digits = digits
        val fontwidth = num[0]!!.width.toInt()
        val neg: Boolean
        var temp: Int
        if (digits < 0) {
            if (n == 0) {
                // make variable-length zeros 1 digit long
                digits = 1
            } else {
                // figure out # of digits in #
                digits = 0
                temp = n
                while (temp != 0) {
                    temp /= 10
                    digits++
                }
            }
        }
        neg = n < 0
        if (neg) {
            n = -n
        }

        // if non-number, do not draw it
        if (n == 1994) {
            return 0
        }

        // draw the new number
        while (digits-- != 0) {
            x -= fontwidth * DOOM.vs.getScalingX()
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                num[n % 10]!!,
                DOOM.vs,
                x,
                y,
                DoomGraphicSystem.V_NOSCALESTART
            )
            n /= 10
        }

        // draw a minus sign if necessary
        if (neg) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                wiminus!!,
                DOOM.vs,
                8 * DOOM.vs.getScalingX().let { x -= it; x },
                y,
                DoomGraphicSystem.V_NOSCALESTART
            )
        }
        return x
    }

    protected fun drawPercent(x: Int, y: Int, p: Int) {
        if (p < 0) {
            return
        }
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            percent!!,
            DOOM.vs,
            x,
            y,
            DoomGraphicSystem.V_NOSCALESTART
        )
        drawNum(x, y, p, -1)
    }

    //
    // Display level completion time and par,
    //  or "sucks" message if overflow.
    //
    protected fun drawTime(
        x: Int,
        y: Int,
        t: Int
    ) {
        var x = x
        var div: Int
        var n: Int
        if (t < 0) {
            return
        }
        if (t <= 61 * 59) {
            div = 1
            do {
                n = t / div % 60
                x = drawNum(x, y, n, 2) - colon!!.width * DOOM.vs.getScalingX()
                div *= 60

                // draw
                if (div == 60 || t / div > 0) {
                    DOOM.graphicSystem.DrawPatchScaled(
                        DoomScreen.FG,
                        colon!!,
                        DOOM.vs,
                        x,
                        y,
                        DoomGraphicSystem.V_NOSCALESTART
                    )
                }
            } while (t / div > 0)
        } else {
            // "sucks"
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                sucks!!,
                DOOM.vs,
                x - sucks!!.width * DOOM.vs.getScalingX(),
                y,
                DoomGraphicSystem.V_NOSCALESTART
            )
        }
    }

    protected fun End() {
        state = endlevel_state.JustShutOff
        DOOM.graphicSystem.forcePalette()
        unloadData()
    }

    protected fun unloadData() {
        var i: Int
        var j: Int
        DOOM.wadLoader.UnlockLumpNum(wiminus)
        wiminus = null
        i = 0
        while (i < 10) {
            DOOM.wadLoader.UnlockLumpNum(num[i])
            num[i] = null
            i++
        }
        if (DOOM.isCommercial()) {
            i = 0
            while (i < NUMCMAPS) {
                DOOM.wadLoader.UnlockLumpNum(lnames!![i])
                lnames!![i] = null
                i++
            }
        } else {
            DOOM.wadLoader.UnlockLumpNum(yah[0])
            yah[0] = null
            DOOM.wadLoader.UnlockLumpNum(yah[1])
            yah[1] = null
            DOOM.wadLoader.UnlockLumpNum(splat[0])
            splat[0] = null
            i = 0
            while (i < Defines.NUMMAPS) {
                DOOM.wadLoader.UnlockLumpNum(lnames!![i])
                lnames!![i] = null
                i++
            }
            if (wbs!!.epsd < 3) {
                j = 0
                while (j < AbstractEndLevel.NUMANIMS.get(wbs!!.epsd)) {
                    if (wbs!!.epsd != 1 || j != 8) {
                        i = 0
                        while (i < AbstractEndLevel.anims.get(wbs!!.epsd).get(j).nanims) {
                            DOOM.wadLoader.UnlockLumpNum(
                                AbstractEndLevel.anims.get(wbs!!.epsd).get(j).p.get(i)
                            )
                            AbstractEndLevel.anims.get(wbs!!.epsd).get(j).p.set(i, null)
                            i++
                        }
                    }
                    j++
                }
            }
        }
        DOOM.wadLoader.UnlockLumpNum(percent)
        percent = null
        DOOM.wadLoader.UnlockLumpNum(colon)
        colon = null
        DOOM.wadLoader.UnlockLumpNum(finished)
        finished = null
        DOOM.wadLoader.UnlockLumpNum(entering)
        entering = null
        DOOM.wadLoader.UnlockLumpNum(kills)
        kills = null
        DOOM.wadLoader.UnlockLumpNum(secret)
        secret = null
        DOOM.wadLoader.UnlockLumpNum(sp_secret)
        sp_secret = null
        DOOM.wadLoader.UnlockLumpNum(items)
        items = null
        DOOM.wadLoader.UnlockLumpNum(frags)
        frags = null
        DOOM.wadLoader.UnlockLumpNum(time)
        time = null
        DOOM.wadLoader.UnlockLumpNum(sucks)
        sucks = null
        DOOM.wadLoader.UnlockLumpNum(par)
        par = null
        DOOM.wadLoader.UnlockLumpNum(victims)
        victims = null
        DOOM.wadLoader.UnlockLumpNum(killers)
        killers = null
        DOOM.wadLoader.UnlockLumpNum(total)
        total = null
        i = 0
        while (i < Limits.MAXPLAYERS) {
            DOOM.wadLoader.UnlockLumpNum(p[i])
            DOOM.wadLoader.UnlockLumpNum(bp[i])
            p[i] = null
            bp[i] = null
            i++
        }
    }

    protected fun initNoState() {
        state = endlevel_state.NoState
        acceleratestage = 0
        cnt = 10
    }

    protected fun updateNoState() {
        updateAnimatedBack()
        if (--cnt == 0) {
            End()
            DOOM.WorldDone()
        }
    }

    var snl_pointeron = false
    protected fun initShowNextLoc() {
        state = endlevel_state.ShowNextLoc
        acceleratestage = 0
        cnt = SHOWNEXTLOCDELAY * Defines.TICRATE
        initAnimatedBack()
    }

    protected fun updateShowNextLoc() {
        updateAnimatedBack()
        if (--cnt == 0 || acceleratestage != 0) {
            initNoState()
        } else {
            snl_pointeron = cnt and 31 < 20
        }
    }

    protected fun drawShowNextLoc() {
        var i: Int
        val last: Int
        slamBackground()

        // draw animated background
        drawAnimatedBack()
        if (!DOOM.isCommercial()) {
            if (wbs!!.epsd > 2) {
                drawEL()
                return
            }
            last = if (wbs!!.last == 8) wbs!!.next - 1 else wbs!!.last

            // draw a splat on taken cities.
            i = 0
            while (i <= last) {
                drawOnLnode(i, splat)
                i++
            }

            // splat the secret level?
            if (wbs!!.didsecret) {
                drawOnLnode(8, splat)
            }

            // draw flashing ptr
            if (snl_pointeron) {
                drawOnLnode(wbs!!.next, yah)
            }
        }

        // draws which level you are entering..
        if (!DOOM.isCommercial()
            || wbs!!.next != 30
        ) {
            drawEL()
        }
    }

    protected fun drawNoState() {
        snl_pointeron = true
        drawShowNextLoc()
    }

    protected fun fragSum(playernum: Int): Int {
        var i: Int
        var frags = 0
        i = 0
        while (i < Limits.MAXPLAYERS) {
            if (DOOM.playeringame[i]
                && i != playernum
            ) {
                frags += plrs[playernum].frags[i]
            }
            i++
        }

        // JDC hack - negative frags.
        frags -= plrs[playernum].frags[playernum]
        // UNUSED if (frags < 0)
        // 	frags = 0;
        return frags
    }

    var dm_state = 0
    var dm_frags = Array(Limits.MAXPLAYERS) { IntArray(Limits.MAXPLAYERS) }
    var dm_totals = IntArray(Limits.MAXPLAYERS)
    @SourceCode.Exact
    @WI_Stuff.C(WI_Stuff.WI_initDeathmatchStats)
    protected fun initDeathmatchStats() {
        state = endlevel_state.StatCount
        acceleratestage = 0
        dm_state = 1
        cnt_pause = Defines.TICRATE
        for (i in 0 until Limits.MAXPLAYERS) {
            if (DOOM.playeringame[i]) {
                for (j in 0 until Limits.MAXPLAYERS) {
                    if (DOOM.playeringame[j]) {
                        dm_frags[i][j] = 0
                    }
                }
                dm_totals[i] = 0
            }
        }
        WI_initAnimatedBack@ run {
            initAnimatedBack()
        }
    }

    protected fun updateDeathmatchStats() {
        var i: Int
        var j: Int
        var stillticking: Boolean
        updateAnimatedBack()
        if (acceleratestage != 0 && dm_state != 4) {
            acceleratestage = 0
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (DOOM.playeringame[i]) {
                    j = 0
                    while (j < Limits.MAXPLAYERS) {
                        if (DOOM.playeringame[j]) {
                            dm_frags[i][j] = plrs[i].frags[j]
                        }
                        j++
                    }
                    dm_totals[i] = fragSum(i)
                }
                i++
            }
            DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
            dm_state = 4
        }
        if (dm_state == 2) {
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            stillticking = false
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (DOOM.playeringame[i]) {
                    j = 0
                    while (j < Limits.MAXPLAYERS) {
                        if (DOOM.playeringame[j]
                            && dm_frags[i][j] != plrs[i].frags[j]
                        ) {
                            if (plrs[i].frags[j] < 0) {
                                dm_frags[i][j]--
                            } else {
                                dm_frags[i][j]++
                            }
                            if (dm_frags[i][j] > 99) {
                                dm_frags[i][j] = 99
                            }
                            if (dm_frags[i][j] < -99) {
                                dm_frags[i][j] = -99
                            }
                            stillticking = true
                        }
                        j++
                    }
                    dm_totals[i] = fragSum(i)
                    if (dm_totals[i] > 99) {
                        dm_totals[i] = 99
                    }
                    if (dm_totals[i] < -99) {
                        dm_totals[i] = -99
                    }
                }
                i++
            }
            if (!stillticking) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                dm_state++
            }
        } else if (dm_state == 4) {
            if (acceleratestage != 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_slop)
                if (DOOM.isCommercial()) {
                    initNoState()
                } else {
                    initShowNextLoc()
                }
            }
        } else if (dm_state and 1 != 0) {
            if (--cnt_pause == 0) {
                dm_state++
                cnt_pause = Defines.TICRATE
            }
        }
    }

    protected fun drawDeathmatchStats() {
        var i: Int
        var j: Int
        var x: Int
        var y: Int
        val w: Int
        val lh = WI_SPACINGY // line height
        slamBackground()

        // draw animated background
        drawAnimatedBack()
        drawLF()

        // draw stat titles (top line)
        DOOM.graphicSystem.DrawPatch(
            DoomScreen.FG,
            total!!,
            AbstractEndLevel.DM_TOTALSX - total!!.width / 2,
            AbstractEndLevel.DM_MATRIXY - WI_SPACINGY + 10
        )
        DOOM.graphicSystem.DrawPatch(
            DoomScreen.FG,
            killers!!,
            AbstractEndLevel.DM_KILLERSX,
            AbstractEndLevel.DM_KILLERSY
        )
        DOOM.graphicSystem.DrawPatch(
            DoomScreen.FG,
            victims!!,
            AbstractEndLevel.DM_VICTIMSX,
            AbstractEndLevel.DM_VICTIMSY
        )

        // draw P?
        x = AbstractEndLevel.DM_MATRIXX + AbstractEndLevel.DM_SPACINGX
        y = AbstractEndLevel.DM_MATRIXY
        i = 0
        while (i < Limits.MAXPLAYERS) {
            if (DOOM.playeringame[i]) {
                DOOM.graphicSystem.DrawPatch(
                    DoomScreen.FG,
                    p[i]!!,
                    x - p[i]!!.width / 2,
                    AbstractEndLevel.DM_MATRIXY - WI_SPACINGY
                )
                DOOM.graphicSystem.DrawPatch(
                    DoomScreen.FG,
                    p[i]!!,
                    AbstractEndLevel.DM_MATRIXX - p[i]!!.width / 2,
                    y
                )
                if (i == me) {
                    DOOM.graphicSystem.DrawPatch(
                        DoomScreen.FG,
                        bstar!!,
                        x - p[i]!!.width / 2,
                        AbstractEndLevel.DM_MATRIXY - WI_SPACINGY
                    )
                    DOOM.graphicSystem.DrawPatch(
                        DoomScreen.FG,
                        star!!,
                        AbstractEndLevel.DM_MATRIXX - p[i]!!.width / 2,
                        y
                    )
                }
            } else {
                // V_DrawPatch(x-SHORT(bp[i].width)/2,
                //   DM_MATRIXY - WI_SPACINGY, FB, bp[i]);
                // V_DrawPatch(DM_MATRIXX-SHORT(bp[i].width)/2,
                //   y, FB, bp[i]);
            }
            x += AbstractEndLevel.DM_SPACINGX
            y += WI_SPACINGY
            i++
        }

        // draw stats
        y = AbstractEndLevel.DM_MATRIXY + 10
        w = num[0]!!.width.toInt()
        i = 0
        while (i < Limits.MAXPLAYERS) {
            x = AbstractEndLevel.DM_MATRIXX + AbstractEndLevel.DM_SPACINGX
            if (DOOM.playeringame[i]) {
                j = 0
                while (j < Limits.MAXPLAYERS) {
                    if (DOOM.playeringame[j]) {
                        drawNum(x + w, y, dm_frags[i][j], 2)
                    }
                    x += AbstractEndLevel.DM_SPACINGX
                    j++
                }
                drawNum(AbstractEndLevel.DM_TOTALSX + w, y, dm_totals[i], 2)
            }
            y += WI_SPACINGY
            i++
        }
    }

    var cnt_frags = IntArray(Limits.MAXPLAYERS)
    var dofrags = 0
    var ng_state = 0
    @SourceCode.Suspicious(SourceCode.CauseOfDesyncProbability.LOW)
    @WI_Stuff.C(WI_Stuff.WI_initNetgameStats)
    protected fun initNetgameStats() {
        state = endlevel_state.StatCount
        acceleratestage = 0
        ng_state = 1
        cnt_pause = Defines.TICRATE
        for (i in 0 until Limits.MAXPLAYERS) {
            if (!DOOM.playeringame[i]) {
                continue
            }
            cnt_frags[i] = 0
            cnt_secret[i] = cnt_frags[i]
            cnt_items[i] = cnt_secret[i]
            cnt_kills[i] = cnt_items[i]
            dofrags += fragSum(i)
        }

        //Suspicious - Good Sign 2017/05/08
        dofrags = dofrags.inv().inv()

        WI_initAnimatedBack@ run {
            initAnimatedBack()
        }
    }

    protected fun updateNetgameStats() {
        var i: Int
        var fsum: Int
        var stillticking: Boolean
        updateAnimatedBack()
        if (acceleratestage != 0 && ng_state != 10) {
            acceleratestage = 0
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (!DOOM.playeringame[i]) {
                    i++
                    continue
                }
                cnt_kills[i] = plrs[i].skills * 100 / wbs!!.maxkills
                cnt_items[i] = plrs[i].sitems * 100 / wbs!!.maxitems
                cnt_secret[i] = plrs[i].ssecret * 100 / wbs!!.maxsecret
                if (dofrags != 0) {
                    cnt_frags[i] = fragSum(i)
                }
                i++
            }
            DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
            ng_state = 10
        }
        if (ng_state == 2) {
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            stillticking = false
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (!DOOM.playeringame[i]) {
                    i++
                    continue
                }
                cnt_kills[i] += 2
                if (cnt_kills[i] >= plrs[i].skills * 100 / wbs!!.maxkills) {
                    cnt_kills[i] = plrs[i].skills * 100 / wbs!!.maxkills
                } else {
                    stillticking = true
                }
                i++
            }
            if (!stillticking) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                ng_state++
            }
        } else if (ng_state == 4) {
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            stillticking = false
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (!DOOM.playeringame[i]) {
                    i++
                    continue
                }
                cnt_items[i] += 2
                if (cnt_items[i] >= plrs[i].sitems * 100 / wbs!!.maxitems) {
                    cnt_items[i] = plrs[i].sitems * 100 / wbs!!.maxitems
                } else {
                    stillticking = true
                }
                i++
            }
            if (!stillticking) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                ng_state++
            }
        } else if (ng_state == 6) {
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            stillticking = false
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (!DOOM.playeringame[i]) {
                    i++
                    continue
                }
                cnt_secret[i] += 2
                if (cnt_secret[i] >= plrs[i].ssecret * 100 / wbs!!.maxsecret) {
                    cnt_secret[i] = plrs[i].ssecret * 100 / wbs!!.maxsecret
                } else {
                    stillticking = true
                }
                i++
            }
            if (!stillticking) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                ng_state += 1 + 2 * dofrags.inv()
            }
        } else if (ng_state == 8) {
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            stillticking = false
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (!DOOM.playeringame[i]) {
                    i++
                    continue
                }
                cnt_frags[i] += 1
                if (cnt_frags[i] >= fragSum(i).also { fsum = it }) {
                    cnt_frags[i] = fsum
                } else {
                    stillticking = true
                }
                i++
            }
            if (!stillticking) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pldeth)
                ng_state++
            }
        } else if (ng_state == 10) {
            if (acceleratestage != 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_sgcock)
                if (DOOM.isCommercial()) {
                    initNoState()
                } else {
                    initShowNextLoc()
                }
            }
        } else if (ng_state and 1 != 0) {
            if (--cnt_pause == 0) {
                ng_state++
                cnt_pause = Defines.TICRATE
            }
        }
    }

    protected fun drawNetgameStats() {
        var i: Int
        var x: Int
        var y: Int
        val pwidth = percent!!.width.toInt()
        slamBackground()

        // draw animated background
        drawAnimatedBack()
        drawLF()

        // draw stat titles (top line)
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            kills!!,
            DOOM.vs,
            NG_STATSX() + AbstractEndLevel.NG_SPACINGX - kills!!.width,
            AbstractEndLevel.NG_STATSY
        )
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            items!!,
            DOOM.vs,
            NG_STATSX() + 2 * AbstractEndLevel.NG_SPACINGX - items!!.width,
            AbstractEndLevel.NG_STATSY
        )
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            secret!!,
            DOOM.vs,
            NG_STATSX() + 3 * AbstractEndLevel.NG_SPACINGX - secret!!.width,
            AbstractEndLevel.NG_STATSY
        )
        if (dofrags != 0) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                frags!!,
                DOOM.vs,
                NG_STATSX() + 4 * AbstractEndLevel.NG_SPACINGX - frags!!.width,
                AbstractEndLevel.NG_STATSY
            )
        }

        // draw stats
        y = AbstractEndLevel.NG_STATSY + kills!!.height
        i = 0
        while (i < Limits.MAXPLAYERS) {
            if (!DOOM.playeringame[i]) {
                i++
                continue
            }
            x = NG_STATSX()
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, p[i]!!, DOOM.vs, x - p[i]!!.width, y)
            if (i == me) {
                DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, star!!, DOOM.vs, x - p[i]!!.width, y)
            }
            x += AbstractEndLevel.NG_SPACINGX
            drawPercent((x - pwidth) * DOOM.vs.getScalingX(), (y + 10) * DOOM.vs.getScalingY(), cnt_kills[i])
            x += AbstractEndLevel.NG_SPACINGX
            drawPercent((x - pwidth) * DOOM.vs.getScalingX(), (y + 10) * DOOM.vs.getScalingY(), cnt_items[i])
            x += AbstractEndLevel.NG_SPACINGX
            drawPercent((x - pwidth) * DOOM.vs.getScalingX(), (y + 10) * DOOM.vs.getScalingY(), cnt_secret[i])
            x += AbstractEndLevel.NG_SPACINGX
            if (dofrags != 0) {
                drawNum(x * DOOM.vs.getScalingX(), (y + 10) * DOOM.vs.getScalingY(), cnt_frags[i], -1)
            }
            y += WI_SPACINGY
            i++
        }
    }

    var sp_state = 0

    //
    // CODE
    //
    // slam background
    // UNUSED  unsigned char *background=0;
    init {

        // Pre-scale stuff.
        SP_STATSX = 50 * DOOM.vs.getSafeScaling()
        SP_STATSY = 50 * DOOM.vs.getSafeScaling()
        SP_TIMEX = 16 * DOOM.vs.getSafeScaling()
        SP_TIMEY = DOOM.vs.getScreenHeight() - DOOM.statusBar.getHeight()
        // _D_: commented this, otherwise something didn't work
        //this.Start(DS.wminfo);
    }

    @SourceCode.Exact
    @WI_Stuff.C(WI_Stuff.WI_initStats)
    protected fun initStats() {
        state = endlevel_state.StatCount
        acceleratestage = 0
        sp_state = 1
        cnt_secret[0] = -1
        cnt_items[0] = cnt_secret[0]
        cnt_kills[0] = cnt_items[0]
        cnt_par = -1
        cnt_time = cnt_par
        cnt_pause = Defines.TICRATE
        WI_initAnimatedBack@ run {
            initAnimatedBack()
        }
    }

    protected fun updateStats() {
        updateAnimatedBack()

        //System.out.println("SP_State "+sp_state);
        if (acceleratestage != 0 && sp_state != COUNT_DONE) {
            acceleratestage = 0
            cnt_kills[0] = plrs[me].skills * 100 / wbs!!.maxkills
            cnt_items[0] = plrs[me].sitems * 100 / wbs!!.maxitems
            cnt_secret[0] = plrs[me].ssecret * 100 / wbs!!.maxsecret
            cnt_time = plrs[me].stime / Defines.TICRATE
            cnt_par = wbs!!.partime / Defines.TICRATE
            DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
            sp_state = 10
        }
        if (sp_state == COUNT_KILLS) {
            cnt_kills[0] += 2
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            if (cnt_kills[0] >= plrs[me].skills * 100 / wbs!!.maxkills) {
                cnt_kills[0] = plrs[me].skills * 100 / wbs!!.maxkills
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                sp_state++
            }
        } else if (sp_state == COUNT_ITEMS) {
            cnt_items[0] += 2
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            if (cnt_items[0] >= plrs[me].sitems * 100 / wbs!!.maxitems) {
                cnt_items[0] = plrs[me].sitems * 100 / wbs!!.maxitems
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                sp_state++
            }
        } else if (sp_state == COUNT_SECRETS) {
            cnt_secret[0] += 2
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            if (cnt_secret[0] >= plrs[me].ssecret * 100 / wbs!!.maxsecret) {
                cnt_secret[0] = plrs[me].ssecret * 100 / wbs!!.maxsecret
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                sp_state++
            }
        } else if (sp_state == COUNT_TIME) {
            if (bcnt and 3 == 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_pistol)
            }
            cnt_time += 3
            if (cnt_time >= plrs[me].stime / Defines.TICRATE) {
                cnt_time = plrs[me].stime / Defines.TICRATE
            }
            cnt_par += 3
            if (cnt_par >= wbs!!.partime / Defines.TICRATE) {
                cnt_par = wbs!!.partime / Defines.TICRATE
                if (cnt_time >= plrs[me].stime / Defines.TICRATE) {
                    DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_barexp)
                    sp_state++
                }
            }
        } else if (sp_state == COUNT_DONE) {
            if (acceleratestage != 0) {
                DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_sgcock)
                if (DOOM.isCommercial()) {
                    initNoState()
                } else {
                    initShowNextLoc()
                }
            }
        } // Non-drawing, pausing state. Any odd value introduces a 35 tic pause.
        else if (sp_state and 1 > 0) {
            if (--cnt_pause == 0) {
                sp_state++
                cnt_pause = Defines.TICRATE
            }
        }
    }

    protected fun drawStats() {
        // line height
        val lh: Int
        lh = 3 * num[0]!!.height * DOOM.vs.getScalingY() / 2
        slamBackground()

        // draw animated background
        drawAnimatedBack()
        drawLF()
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            kills!!,
            DOOM.vs,
            SP_STATSX,
            SP_STATSY,
            DoomGraphicSystem.V_NOSCALESTART
        )
        drawPercent(DOOM.vs.getScreenWidth() - SP_STATSX, SP_STATSY, cnt_kills[0])
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            items!!,
            DOOM.vs,
            SP_STATSX,
            SP_STATSY + lh,
            DoomGraphicSystem.V_NOSCALESTART
        )
        drawPercent(DOOM.vs.getScreenWidth() - SP_STATSX, SP_STATSY + lh, cnt_items[0])
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            sp_secret!!,
            DOOM.vs,
            SP_STATSX,
            SP_STATSY + 2 * lh,
            DoomGraphicSystem.V_NOSCALESTART
        )
        drawPercent(DOOM.vs.getScreenWidth() - SP_STATSX, SP_STATSY + 2 * lh, cnt_secret[0])
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            time!!,
            DOOM.vs,
            SP_TIMEX,
            SP_TIMEY,
            DoomGraphicSystem.V_NOSCALESTART
        )
        drawTime(DOOM.vs.getScreenWidth() / 2 - SP_TIMEX, SP_TIMEY, cnt_time)
        if (wbs!!.epsd < 3) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                par!!,
                DOOM.vs,
                DOOM.vs.getScreenWidth() / 2 + SP_TIMEX,
                SP_TIMEY,
                DoomGraphicSystem.V_NOSCALESTART
            )
            drawTime(DOOM.vs.getScreenWidth() - SP_TIMEX, SP_TIMEY, cnt_par)
        }
    }

    protected fun checkForAccelerate() {

        // check for button presses to skip delays
        for (i in 0 until Limits.MAXPLAYERS) {
            val player = DOOM.players[i]
            if (DOOM.playeringame[i]) {
                if (player.cmd.buttons.code and Defines.BT_ATTACK != 0) {
                    if (!player.attackdown) {
                        acceleratestage = 1
                    }
                    player.attackdown = true
                } else {
                    player.attackdown = false
                }
                if (player.cmd.buttons.code and Defines.BT_USE != 0) {
                    if (!player.usedown) {
                        acceleratestage = 1
                    }
                    player.usedown = true
                } else {
                    player.usedown = false
                }
            }
        }
    }

    /**
     * Updates stuff each tick
     */
    fun Ticker() {
        // counter for general background animation
        bcnt++
        if (bcnt == 1) {
            // intermission music
            if (DOOM.isCommercial()) {
                DOOM.doomSound.ChangeMusic(sounds.musicenum_t.mus_dm2int.ordinal, true)
            } else {
                DOOM.doomSound.ChangeMusic(sounds.musicenum_t.mus_inter.ordinal, true)
            }
        }
        checkForAccelerate()
        when (state) {
            endlevel_state.StatCount -> if (DOOM.deathmatch) {
                updateDeathmatchStats()
            } else if (DOOM.netgame) {
                updateNetgameStats()
            } else {
                updateStats()
            }
            endlevel_state.ShowNextLoc -> updateShowNextLoc()
            endlevel_state.NoState -> updateNoState()
            endlevel_state.JustShutOff ->                 // We just finished, and graphics have been unloaded.
                // If we don't consume a tick in this way, Doom
                // will try to draw unloaded graphics.
                state = endlevel_state.NoState
        }
    }

    @Compatible
    @WI_Stuff.C(WI_Stuff.WI_loadData)
    protected fun loadData() {
        var name: String?
        var a: anim_t
        if (DOOM.isCommercial()) {
            name = "INTERPIC"
        } else { //sprintf(name, "WIMAP%d", wbs.epsd);
            name = "WIMAP" + Integer.toString(wbs!!.epsd)
        }

        // MAES: For Ultimate Doom
        if (DOOM.isRetail()) {
            if (wbs!!.epsd == 3) {
                name = "INTERPIC"
            }
        }

        // background - draw it to screen 1 for quick redraw.
        bg = DOOM.wadLoader.CacheLumpName(name, Defines.PU_CACHE, patch_t::class.java)
        DOOM.graphicSystem.DrawPatchScaled(DoomScreen.BG, bg!!, DOOM.vs, 0, 0, DoomGraphicSystem.V_SAFESCALE)

        // UNUSED unsigned char *pic = screens[1];
        // if (gamemode == commercial)
        // {
        // darken the background image
        // while (pic != screens[1] + DOOM.vs.getScreenHeight()*DOOM.vs.getScreenWidth())
        // {
        //   *pic = colormaps[256*25 + *pic];
        //   pic++;
        // }
        //}
        if (DOOM.isCommercial()) {
            NUMCMAPS = 32
            lnames = arrayOfNulls(NUMCMAPS)
            val xxx = "CWILV%02d"
            //String buffer;
            for (i in 0 until NUMCMAPS) {
                name = String.format(xxx, i)
                lnames!![i] = DOOM.wadLoader.CacheLumpName(name, PU_STATIC, patch_t::class.java)
            }
        } else {
            lnames = arrayOfNulls(Defines.NUMMAPS)
            var xxx = "WILV%d%d"
            for (i in 0 until Defines.NUMMAPS) {
                name = String.format(xxx, wbs!!.epsd, i)
                lnames!![i] = DOOM.wadLoader.CacheLumpName(name, PU_STATIC, patch_t::class.java)
            }

            // you are here
            yah[0] = DOOM.wadLoader.CacheLumpName("WIURH0", PU_STATIC, patch_t::class.java)

            // you are here (alt.)
            yah[1] = DOOM.wadLoader.CacheLumpName("WIURH1", PU_STATIC, patch_t::class.java)
            yah[2] = null

            // splat
            splat = arrayOf(DOOM.wadLoader.CacheLumpName("WISPLAT", PU_STATIC, patch_t::class.java), null)
            if (wbs!!.epsd < 3) {
                xxx = "WIA%d%02d%02d"
                //xxx=new PrintfFormat("WIA%d%.2d%.2d");
                for (j in 0 until AbstractEndLevel.NUMANIMS.get(wbs!!.epsd)) {
                    a = AbstractEndLevel.anims.get(wbs!!.epsd).get(j)
                    for (i in 0 until a.nanims) {
                        // MONDO HACK!
                        if (wbs!!.epsd != 1 || j != 8) {
                            // animations
                            name = String.format(xxx, wbs!!.epsd, j, i)
                            a.p[i] = DOOM.wadLoader.CacheLumpName(name, PU_STATIC, patch_t::class.java)
                        } else {
                            // HACK ALERT!
                            a.p[i] = AbstractEndLevel.anims.get(1).get(4).p.get(i)
                        }
                    }
                }
            }
        }

        // More hacks on minus sign.
        wiminus = DOOM.wadLoader.CacheLumpName("WIMINUS", PU_STATIC, patch_t::class.java)
        val xxx = "WINUM%d"
        for (i in 0..9) {
            // numbers 0-9
            name = String.format(xxx, i)
            num[i] = DOOM.wadLoader.CacheLumpName(name, PU_STATIC, patch_t::class.java)
        }

        // percent sign
        percent = DOOM.wadLoader.CacheLumpName("WIPCNT", PU_STATIC, patch_t::class.java)

        // "finished"
        finished = DOOM.wadLoader.CacheLumpName("WIF", PU_STATIC, patch_t::class.java)

        // "entering"
        entering = DOOM.wadLoader.CacheLumpName("WIENTER", PU_STATIC, patch_t::class.java)

        // "kills"
        kills = DOOM.wadLoader.CacheLumpName("WIOSTK", PU_STATIC, patch_t::class.java)

        // "scrt"
        secret = DOOM.wadLoader.CacheLumpName("WIOSTS", PU_STATIC, patch_t::class.java)

        // "secret"
        sp_secret = DOOM.wadLoader.CacheLumpName("WISCRT2", PU_STATIC, patch_t::class.java)

        // Yuck. 
        items = if (DOOM.language == Language_t.french) {
            // "items"
            if (DOOM.netgame && !DOOM.deathmatch) {
                DOOM.wadLoader.CacheLumpName("WIOBJ", PU_STATIC, patch_t::class.java)
            } else {
                DOOM.wadLoader.CacheLumpName("WIOSTI", PU_STATIC, patch_t::class.java)
            }
        } else {
            DOOM.wadLoader.CacheLumpName("WIOSTI", PU_STATIC, patch_t::class.java)
        }

        // "frgs"
        frags = DOOM.wadLoader.CacheLumpName("WIFRGS", PU_STATIC, patch_t::class.java)

        // ":"
        colon = DOOM.wadLoader.CacheLumpName("WICOLON", PU_STATIC, patch_t::class.java)

        // "time"
        time = DOOM.wadLoader.CacheLumpName("WITIME", PU_STATIC, patch_t::class.java)

        // "sucks"
        sucks = DOOM.wadLoader.CacheLumpName("WISUCKS", PU_STATIC, patch_t::class.java)

        // "par"
        par = DOOM.wadLoader.CacheLumpName("WIPAR", PU_STATIC, patch_t::class.java)

        // "killers" (vertical)
        killers = DOOM.wadLoader.CacheLumpName("WIKILRS", PU_STATIC, patch_t::class.java)

        // "victims" (horiz)
        victims = DOOM.wadLoader.CacheLumpName("WIVCTMS", PU_STATIC, patch_t::class.java)

        // "total"
        total = DOOM.wadLoader.CacheLumpName("WIMSTT", PU_STATIC, patch_t::class.java)

        // your face
        star = DOOM.wadLoader.CacheLumpName("STFST01", PU_STATIC, patch_t::class.java)

        // dead face
        bstar = DOOM.wadLoader.CacheLumpName("STFDEAD0", PU_STATIC, patch_t::class.java)
        val xx1 = "STPB%d"
        val xx2 = "WIBP%d"
        for (i in 0 until Limits.MAXPLAYERS) {
            // "1,2,3,4"
            name = String.format(xx1, i)
            p[i] = DOOM.wadLoader.CacheLumpName(name, PU_STATIC, patch_t::class.java)

            // "1,2,3,4"
            name = String.format(xx2, i + 1)
            bp[i] = DOOM.wadLoader.CacheLumpName(name, PU_STATIC, patch_t::class.java)
        }
    }

    /*

public void WI_unloadData()
{
    int		i;
    int		j;

    W.UnlockLumpNum(wiminus, PU_CACHE);

    for (i=0 ; i<10 ; i++)
	W.UnlockLumpNum(num[i], PU_CACHE);
    
    if (gamemode == commercial)
    {
  	for (i=0 ; i<NUMCMAPS ; i++)
	    W.UnlockLumpNum(lnames!![i], PU_CACHE);
    }
    else
    {
	W.UnlockLumpNum(yah[0], PU_CACHE);
	W.UnlockLumpNum(yah[1], PU_CACHE);

	W.UnlockLumpNum(splat, PU_CACHE);

	for (i=0 ; i<NUMMAPS ; i++)
	    W.UnlockLumpNum(lnames!![i], PU_CACHE);
	
	if (wbs.epsd < 3)
	{
	    for (j=0;j<NUMANIMS[wbs.epsd];j++)
	    {
		if (wbs.epsd != 1 || j != 8)
		    for (i=0;i<anims[wbs.epsd][j].nanims;i++)
			W.UnlockLumpNum(anims[wbs.epsd][j].p[i], PU_CACHE);
	    }
	}
    }
    
    Z_Free(lnames);

    W.UnlockLumpNum(percent, PU_CACHE);
    W.UnlockLumpNum(colon, PU_CACHE);
    W.UnlockLumpNum(finished, PU_CACHE);
    W.UnlockLumpNum(entering, PU_CACHE);
    W.UnlockLumpNum(kills, PU_CACHE);
    W.UnlockLumpNum(secret, PU_CACHE);
    W.UnlockLumpNum(sp_secret, PU_CACHE);
    W.UnlockLumpNum(items, PU_CACHE);
    W.UnlockLumpNum(frags, PU_CACHE);
    W.UnlockLumpNum(time, PU_CACHE);
    W.UnlockLumpNum(sucks, PU_CACHE);
    W.UnlockLumpNum(par, PU_CACHE);

    W.UnlockLumpNum(victims, PU_CACHE);
    W.UnlockLumpNum(killers, PU_CACHE);
    W.UnlockLumpNum(total, PU_CACHE);
    //  W.UnlockLumpNum(star, PU_CACHE);
    //  W.UnlockLumpNum(bstar, PU_CACHE);
    
    for (i=0 ; i<MAXPLAYERS ; i++)
	W.UnlockLumpNum(p[i], PU_CACHE);

    for (i=0 ; i<MAXPLAYERS ; i++)
	W.UnlockLumpNum(bp[i], PU_CACHE);
}
     */
    fun Drawer() {
        when (state) {
            endlevel_state.StatCount -> if (DOOM.deathmatch) {
                drawDeathmatchStats()
            } else if (DOOM.netgame) {
                drawNetgameStats()
            } else {
                drawStats()
            }
            endlevel_state.ShowNextLoc -> drawShowNextLoc()
            endlevel_state.NoState -> drawNoState()
            else -> {}
        }
    }

    @Compatible
    @WI_Stuff.C(WI_Stuff.WI_initVariables)
    protected fun initVariables(wbstartstruct: wbstartstruct_t) {
        wbs = wbstartstruct.clone()
        if (RANGECHECKING) {
            if (!DOOM.isCommercial()) {
                if (DOOM.isRetail()) {
                    RNGCHECK(wbs!!.epsd, 0, 3)
                } else {
                    RNGCHECK(wbs!!.epsd, 0, 2)
                }
            } else {
                RNGCHECK(wbs!!.last, 0, 8)
                RNGCHECK(wbs!!.next, 0, 8)
            }
            RNGCHECK(wbs!!.pnum, 0, Limits.MAXPLAYERS)
            RNGCHECK(wbs!!.pnum, 0, Limits.MAXPLAYERS)
        }
        acceleratestage = 0
        bcnt = 0
        cnt = bcnt
        firstrefresh = 1
        me = wbs!!.pnum
        plrs = wbs!!.plyr.clone()
        if (wbs!!.maxkills == 0) {
            wbs!!.maxkills = 1
        }
        if (wbs!!.maxitems == 0) {
            wbs!!.maxitems = 1
        }
        if (wbs!!.maxsecret == 0) {
            wbs!!.maxsecret = 1
        }

        // Sanity check for Ultimate.
        if (!DOOM.isRetail()) {
            if (wbs!!.epsd > 2) {
                wbs!!.epsd -= 3
            }
        }
    }

    @SourceCode.Exact
    @WI_Stuff.C(WI_Stuff.WI_Start)
    fun Start(wbstartstruct: wbstartstruct_t) {
        WI_initVariables@ run {
            initVariables(wbstartstruct)
        }
        WI_loadData@ run {
            loadData()
        }
        if (DOOM.deathmatch) {
            WI_initDeathmatchStats@ run {
                initDeathmatchStats()
            }
        } else if (DOOM.netgame) {
            WI_initNetgameStats@ run {
                initNetgameStats()
            }
        } else {
            WI_initStats@ run {
                initStats()
            }
        }
    }

    protected fun NG_STATSX(): Int {
        return 32 + star!!.width / 2 + 32 * if (dofrags <= 0) 1 else 0
    }

    companion object {
        private const val COUNT_KILLS = 2
        private const val COUNT_ITEMS = 4
        private const val COUNT_SECRETS = 6
        private const val COUNT_TIME = 8
        private const val COUNT_DONE = 10

        //GLOBAL LOCATIONS
        private const val WI_TITLEY = 2
        private const val WI_SPACINGY = 3

        //
        // GENERAL DATA
        //
        //
        // Locally used stuff.
        //
        private const val RANGECHECKING = true

        // Where to draw some stuff. To be scaled up, so they
        // are not final.
        var SP_STATSX: Int = 0
        var SP_STATSY: Int = 0
        var SP_TIMEX: Int = 0
        var SP_TIMEY: Int = 0

        // States for single-player
        protected var SP_KILLS = 0
        protected var SP_ITEMS = 2
        protected var SP_SECRET = 4
        protected var SP_FRAGS = 6
        protected var SP_TIME = 8
        protected var SP_PAR = SP_TIME
        protected fun RNGCHECK(what: Int, min: Int, max: Int): Boolean {
            return what >= min && what <= max
        }
    }
}