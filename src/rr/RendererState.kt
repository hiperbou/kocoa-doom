package rr


import data.Defines
import data.Limits
import data.Tables
import doom.CommandVariable
import doom.DoomMain
import doom.SourceCode.R_Draw
import doom.player_t
import doom.thinker_t
import i.IDoomSystem
import m.BBox
import m.IDoomMenu
import m.MenuMisc
import m.Settings
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedDiv
import m.fixed_t.Companion.FixedMul
import mochadoom.Engine
import p.ActiveStates
import p.mobj_t
import rr.drawfuns.*
import rr.visplane_t.Companion.SENTINEL
import utils.C2JUtils
import utils.GenericCopy
import v.DoomGraphicSystem
import v.graphics.Lights
import v.renderers.DoomScreen
import v.tables.BlurryTable
import v.tables.LightsAndColors
import w.IWadLoader
import java.awt.Rectangle
import java.io.IOException

/**
 * Most shared -essential- status information, methods and classes related to
 * the software rendering subsystem are found here, shared between the various
 * implementations of the Doom's renderer. Not the cleanest or more OO way
 * possible, but still a good way to avoid duplicating common code. Some stuff
 * like Texture, Flat and Sprite management are also found -or at least
 * implemented temporarily- here, until a cleaner split can be made. This is a
 * kind of "Jack of all trades" class, but hopefully not for long.
 *
 * @author velktron
 */
abstract class RendererState<T, V>(  // ///////////////////// STATUS ////////////////////////
    var DOOM: DoomMain<T, V>
) : SceneRenderer<T, V>, ILimitResettable {
    protected var MySegs: ISegDrawer? = null
    protected var Menu: IDoomMenu? = null
    protected var MyBSP: BSP
    protected var MyPlanes: PlaneDrawer<T, V>
    protected var MyThings: IMaskedDrawer<T, V>
    var VIS: IVisSpriteManagement<V>
    protected var TexMan: TextureManager<T>
    lateinit var _view: ViewVars
    var colormaps: LightsAndColors<V>
    var seg_vars: SegVars
    var vp_vars: Visplanes

    // Rendering subsystems that are detailshift-aware
    protected var detailaware: MutableList<IDetailAware>

    // The only reason to query scaledviewwidth from outside the renderer, is
    // this.
    override fun isFullHeight(): Boolean {
        return _view.height == DOOM.vs.getScreenHeight()
    }

    fun isFullWidth(): Boolean {
        return _view.scaledwidth == DOOM.vs.getScreenWidth()
    }

    override fun isFullScreen(): Boolean {
        return isFullWidth() && isFullHeight()
    }

    /**
     * Increment every time a check is made For some reason, this needs to be
     * visible even by enemies thinking :-S
     */
    protected var validcount = 1

    /**
     * Who can set this? A: The Menu.
     */
    protected var setsizeneeded = false
    protected var setblocks = 0
    protected var setdetail = 0
    // private BSPVars bspvars;
    /**
     * R_SetViewSize Do not really change anything here, because it might be in
     * the middle of a refresh. The change will take effect next refresh.
     *
     * @param blocks
     * 11 is full screen, 9 default.
     * @param detail
     * 0= high, 1 =low.
     */
    override fun SetViewSize(blocks: Int, detail: Int) {
        // System.out.println("SetViewSize");
        setsizeneeded = true
        setblocks = blocks
        setdetail = detail
        detailaware.forEach({ d: IDetailAware -> d.setDetail(setdetail) })
    }

    /**
     * R_SetupFrame
     */
    fun SetupFrame(player: player_t) {
        _view.player = player
        _view.x = player.mo!!._x
        _view.y = player.mo!!._y
        // viewangle = addAngles(player.mo.angle , viewangleoffset);
        _view.angle = player.mo!!.angle and Tables.BITS32
        // With 32 colormaps, a bump of 1 or 2 is normal.
        // With more than 32, it should be obviously higher.
        var bumplight = Math.max(colormaps.lightBits() - 5, 0)
        // Be a bit more generous, otherwise the effect is not
        // as evident with truecolor maps.
        bumplight += if (bumplight > 0) 1 else 0
        colormaps.extralight = player.extralight shl bumplight
        _view.z = player.viewz
        _view.lookdir = player.lookdir
        val tempCentery: Int

        // MAES: hacks based on Heretic. Weapon movement needs to be compensated
        tempCentery = if (setblocks == 11) {
            _view.height / 2 + (_view.lookdir * DOOM.vs.getScreenMul() * setblocks).toInt() / 11
        } else {
            _view.height / 2 + (_view.lookdir * DOOM.vs.getScreenMul() * setblocks).toInt() / 10
        }
        if (_view.centery != tempCentery) {
            _view.centery = tempCentery
            _view.centeryfrac = _view.centery shl FRACBITS
            val yslope = vp_vars.yslope
            for (i in 0 until _view.height) {
                yslope[i] = FixedDiv(
                    (_view.width shl _view.detailshift) / 2 * FRACUNIT,
                    Math.abs((i - _view.centery shl FRACBITS) + FRACUNIT / 2)
                )
            }
            dcvars.centery = _view.centery
            maskedcvars.centery = dcvars.centery
            skydcvars.centery = maskedcvars.centery
        }
        _view.sin = Tables.finesine(_view.angle)
        _view.cos = Tables.finecosine(_view.angle)
        sscount = 0
        if (player.fixedcolormap != Lights.COLORMAP_FIXED) {
            colormaps.fixedcolormap = colormaps.getFixedColormap(player)
            // Offset by fixedcolomap
            // pfixedcolormap =player.fixedcolormap*256;
            colormaps.walllights = colormaps.scalelightfixed as Array<V>
            for (i in 0 until colormaps.maxLightScale()) {
                colormaps.scalelightfixed[i] = colormaps.fixedcolormap!!
            }
        } else {
            colormaps.fixedcolormap = null
        }
        framecount++
        validcount++
    }

    /**
     * R_SetupFrame for a particular actor.
     */
    fun SetupFrame(actor: mobj_t) {

        // viewplayer = player;
        _view.x = actor._x
        _view.y = actor._y
        // viewangle = addAngles(player.mo.angle , viewangleoffset);
        _view.angle = actor.angle and Tables.BITS32
        // extralight = actor.extralight;
        _view.z = actor._z + actor.height
        _view.sin = Tables.finesine(_view.angle)
        _view.cos = Tables.finecosine(_view.angle)
        sscount = 0
        framecount++
        validcount++
    }

    // ////////////////////////////// THINGS ////////////////////////////////
    protected inner class BSP internal constructor() : BSPVars() {
        /**
         * newend is one past the last valid seg (cliprange_t)
         */
        var newend = 0
        var solidsegs: Array<cliprange_t>

        /**
         * R_ClipSolidWallSegment Does handle solid walls, single sided LineDefs
         * (middle texture) that entirely block the view VERTICALLY. Handles
         * "clipranges" for a solid wall, aka where it blocks the view.
         *
         * @param first
         * starting y coord?
         * @param last
         * ending y coord?
         */
        private fun ClipSolidWallSegment(first: Int, last: Int) {
            var next: Int
            var start: Int
            // int maxlast=Integer.MIN_VALUE;
            start = 0 // within solidsegs

            // Find the first cliprange that touches the range.
            // Actually, the first one not completely hiding it (its last must
            // be lower than first.
            while (solidsegs[start].last < first - 1) {
                start++
            }

            // If the post begins above the lastly found cliprange...
            if (first < solidsegs[start].first) {
                // ..and ends above it, too (no overlapping)
                if (last < solidsegs[start].first - 1) {
                    // ... then the post is entirely visible (above start),
                    // so insert a new clippost. Calling this function
                    // tells the renderer that there is an obstruction.
                    MySegs!!.StoreWallRange(first, last)

                    // Newend should have a value of 2 if we are at the
                    // beginning of a new frame.
                    next = newend
                    newend++
                    if (next >= solidsegs.size) {
                        ResizeSolidSegs()
                    }
                    while (next != start) {
                        // *next=*(next-1);
                        /*
                         * MAES: I think this is supposed to copy the structs
                         * solidsegs[next] = solidsegs[next-1].clone(); OK, so
                         * basically the last solidseg copies its previous, and
                         * so on until we reach the start. This means that at
                         * some point, the value of the start solidseg is
                         * duplicated.
                         */
                        solidsegs[next].copy(solidsegs[next - 1])
                        next--
                    }

                    // At this point, next points at start.
                    // Therefore, start
                    solidsegs[next].first = first
                    solidsegs[next].last = last
                    return
                }

                // There is a fragment above *start. This can occur if it a
                // post does start before another, but its lower edge overlaps
                // (partial, upper occlusion)
                MySegs!!.StoreWallRange(first, solidsegs[start].first - 1)
                // Now adjust the clip size.
                solidsegs[start].first = first
            }

            // We can reach this only if a post starts AFTER another
            // Bottom contained in start? Obviously it won't be visible.
            if (last <= solidsegs[start].last) {
                return
            }
            next = start
            while (last >= solidsegs[next + 1].first - 1) {
                // There is a fragment between two posts.
                MySegs!!.StoreWallRange(
                    solidsegs[next].last + 1,
                    solidsegs[next + 1].first - 1
                )
                next++
                if (last <= solidsegs[next].last) {
                    // Bottom is contained in next.
                    // Adjust the clip size.
                    solidsegs[start].last = solidsegs[next].last
                    // goto crunch;
                    run {
                        // crunch code
                        if (next == start) {
                            // Post just extended past the bottom of one post.
                            return
                        }
                        while (next++ != newend) {
                            // Remove a post.
                            // MAES: this is a struct copy.
                            if (next >= solidsegs.size) {
                                ResizeSolidSegs()
                            }
                            solidsegs[++start].copy(solidsegs[next])
                        }
                        newend = start + 1
                        return
                    }
                }
            }

            // There is a fragment after *next.
            MySegs!!.StoreWallRange(solidsegs[next].last + 1, last)
            // Adjust the clip size.
            solidsegs[start].last = last

            // Remove start+1 to next from the clip list,
            // because start now covers their area.
            run {
                // crunch code
                if (next == start) {
                    // Post just extended past the bottom of one post.
                    return
                }
                while (next++ != newend) {
                    // Remove a post.
                    // MAES: this is a struct copy.
                    // MAES: this can overflow, breaking e.g. MAP30 of Final
                    // Doom.
                    if (next >= solidsegs.size) {
                        ResizeSolidSegs()
                    }
                    solidsegs[++start].copy(solidsegs[next])
                }
                newend = start + 1
            }
        }

        fun ResizeSolidSegs() {
            solidsegs = C2JUtils.resize(solidsegs as Array<cliprange_t?>, solidsegs.size * 2) as  Array<cliprange_t>
        }

        //
        // R_ClipPassWallSegment
        // Clips the given range of columns,
        // but does not includes it in the clip list.
        // Does handle windows,
        // e.g. LineDefs with upper and lower texture.
        //
        private fun ClipPassWallSegment(first: Int, last: Int) {

            // Find the first range that touches the range
            // (adjacent pixels are touching).
            var start = 0
            while (solidsegs[start].last < first - 1) {
                start++
            }
            if (first < solidsegs[start].first) {
                if (last < solidsegs[start].first - 1) {
                    // Post is entirely visible (above start).
                    MySegs!!.StoreWallRange(first, last)
                    return
                }

                // There is a fragment above *start.
                MySegs!!.StoreWallRange(first, solidsegs[start].first - 1)
            }

            // Bottom contained in start?
            if (last <= solidsegs[start].last) {
                return
            }

            // MAES: Java absolutely can't do without a sanity check here.
            // if (startptr>=MAXSEGS-2) return;
            while (last >= solidsegs[start + 1].first - 1) {
                // There is a fragment between two posts.
                MySegs!!.StoreWallRange(
                    solidsegs[start].last + 1,
                    solidsegs[start + 1].first - 1
                )
                start++
                // if (startptr>=MAXSEGS-2) return;
                // start=solidsegs[startptr];
                if (last <= solidsegs[start].last) {
                    return
                }
            }

            // There is a fragment after *next.
            MySegs!!.StoreWallRange(solidsegs[start].last + 1, last)
        }

        /**
         * R_ClearClipSegs Clears the clipping segs list. The list is actually
         * fixed size for efficiency reasons, so it just tells Doom to use the
         * first two solidsegs, which are "neutered". It's interesting to note
         * how the solidsegs begin and end just "outside" the visible borders of
         * the screen.
         */
        fun ClearClipSegs() {
            solidsegs[0].first = -0x7fffffff
            solidsegs[0].last = -1
            solidsegs[1].first = _view.width
            solidsegs[1].last = 0x7fffffff
            newend = 2 // point so solidsegs[2];
        }

        /**
         * R_AddLine Called after a SubSector BSP trasversal ends up in a
         * "final" subsector. Clips the given segment and adds any visible
         * pieces to the line list. It also determines what kind of boundary
         * (line) visplane clipping should be performed. E.g. window, final
         * 1-sided line, closed door etc.) CAREFUL: was the source of much
         * frustration with visplanes...
         */
        private fun AddLine(line: seg_t) {
            if (RendererState.DEBUG) {
                println("Entered AddLine for $line")
            }
            val x1: Int
            val x2: Int
            var angle1: Long
            var angle2: Long
            val span: Long
            var tspan: Long
            curline = line

            // OPTIMIZE: quickly reject orthogonal back sides.
            angle1 = _view.PointToAngle(line.v1x, line.v1y)
            angle2 = _view.PointToAngle(line.v2x, line.v2y)

            // Clip to view edges.
            // OPTIMIZE: make constant out of 2*clipangle (FIELDOFVIEW).
            span = Tables.addAngles(angle1, -angle2)

            // Back side? I.e. backface culling?
            if (span >= Tables.ANG180) {
                return
            }

            // Global angle needed by segcalc.
            MySegs!!.setGlobalAngle(angle1)
            angle1 -= _view.angle
            angle2 -= _view.angle
            angle1 = angle1 and Tables.BITS32
            angle2 = angle2 and Tables.BITS32
            tspan = Tables.addAngles(angle1, clipangle)
            if (tspan > CLIPANGLE2) {
                tspan -= CLIPANGLE2
                tspan = tspan and Tables.BITS32

                // Totally off the left edge?
                if (tspan >= span) {
                    return
                }
                angle1 = clipangle
            }
            tspan = Tables.addAngles(clipangle, -angle2)
            if (tspan > CLIPANGLE2) {
                tspan -= CLIPANGLE2
                tspan = tspan and Tables.BITS32

                // Totally off the left edge?
                if (tspan >= span) {
                    return
                }
                angle2 = -clipangle
                angle2 = angle2 and Tables.BITS32
            }

            // The seg is in the view range,
            // but not necessarily visible.
            angle1 = angle1 + Tables.ANG90 and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT
            angle2 = angle2 + Tables.ANG90 and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT
            x1 = viewangletox[angle1.toInt()]
            x2 = viewangletox[angle2.toInt()]

            // Does not cross a pixel?
            if (x1 == x2) {
                return
            }
            backsector = line.backsector

            // Single sided line?
            if (backsector == null) {
                if (RendererState.DEBUG) {
                    println("Entering ClipSolidWallSegment SS with params " + x1 + " " + (x2 - 1))
                }
                ClipSolidWallSegment(x1, x2 - 1) // to clipsolid
                if (RendererState.DEBUG) {
                    println("Exiting ClipSolidWallSegment")
                }
                return
            }

            val backsector = backsector!!
            val frontsector = frontsector!!
            // Closed door.
            if (backsector.ceilingheight <= frontsector.floorheight
                || backsector.floorheight >= frontsector.ceilingheight
            ) {
                if (RendererState.DEBUG) {
                    println("Entering ClipSolidWallSegment Closed door with params " + x1 + " " + (x2 - 1))
                }
                ClipSolidWallSegment(x1, x2 - 1)
                // to clipsolid
                return
            }

            // Window. This includes same-level floors with different textures
            if (backsector.ceilingheight != frontsector.ceilingheight
                || backsector.floorheight != frontsector.floorheight
            ) {
                if (RendererState.DEBUG) {
                    println("Entering ClipSolidWallSegment window with params " + x1 + " " + (x2 - 1))
                }
                ClipPassWallSegment(x1, x2 - 1) // to clippass
                return
            }

            // Reject empty lines used for triggers
            // and special events.
            // Identical floor and ceiling on both sides,
            // identical light levels on both sides,
            // and no middle texture.
            if (backsector.ceilingpic == frontsector.ceilingpic && backsector.floorpic == frontsector.floorpic && backsector.lightlevel == frontsector.lightlevel && curline!!.sidedef!!.midtexture.toInt() == 0) {
                return
            }

            // If nothing of the previous holds, then we are
            // treating the case of same-level, differently
            // textured floors. ACHTUNG, this caused the "bleeding floor"
            // bug, which is now fixed.
            // Fucking GOTOs....
            ClipPassWallSegment(x1, x2 - 1) // to clippass
            if (RendererState.DEBUG) {
                println("Exiting AddLine for $line")
            }
        }

        //
        // R_CheckBBox
        // Checks BSP node/subtree bounding box.
        // Returns true
        // if some part of the bbox might be visible.
        //
        private val checkcoord = arrayOf(
            intArrayOf(3, 0, 2, 1),
            intArrayOf(3, 0, 2, 0),
            intArrayOf(3, 1, 2, 0),
            intArrayOf(0),
            intArrayOf(2, 0, 2, 1),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(3, 1, 3, 0),
            intArrayOf(0),
            intArrayOf(2, 0, 3, 1),
            intArrayOf(2, 1, 3, 1),
            intArrayOf(2, 1, 3, 0)
        )

        init {
            solidsegs = GenericCopy.malloc({ cliprange_t() }, Limits.MAXSEGS + 1)
        }

        /**
         * @param bspcoord
         * (fixed_t* as bbox)
         * @return
         */
        fun CheckBBox(bspcoord: IntArray): Boolean {
            val boxx: Int
            val boxy: Int
            val boxpos: Int

            // fixed_t
            val x1: Int
            val y1: Int
            val x2: Int
            val y2: Int

            // angle_t
            var angle1: Long
            var angle2: Long
            var span: Long
            var tspan: Long
            var start: cliprange_t
            val sx1: Int
            var sx2: Int

            // Find the corners of the box
            // that define the edges from current viewpoint.
            boxx = if (_view.x <= bspcoord[BBox.BOXLEFT]) {
                0
            } else if (_view.x < bspcoord[BBox.BOXRIGHT]) {
                1
            } else {
                2
            }
            boxy = if (_view.y >= bspcoord[BBox.BOXTOP]) {
                0
            } else if (_view.y > bspcoord[BBox.BOXBOTTOM]) {
                1
            } else {
                2
            }
            boxpos = (boxy shl 2) + boxx
            if (boxpos == 5) {
                return true
            }
            x1 = bspcoord[checkcoord[boxpos][0]]
            y1 = bspcoord[checkcoord[boxpos][1]]
            x2 = bspcoord[checkcoord[boxpos][2]]
            y2 = bspcoord[checkcoord[boxpos][3]]

            // check clip list for an open space
            angle1 = _view.PointToAngle(x1, y1) - _view.angle
            angle2 = _view.PointToAngle(x2, y2) - _view.angle
            angle1 = angle1 and Tables.BITS32
            angle2 = angle2 and Tables.BITS32
            span = angle1 - angle2
            span = span and Tables.BITS32

            // Sitting on a line?
            if (span >= Tables.ANG180) {
                return true
            }
            tspan = angle1 + clipangle
            tspan = tspan and Tables.BITS32
            if (tspan > CLIPANGLE2) {
                tspan -= CLIPANGLE2
                tspan = tspan and Tables.BITS32
                // Totally off the left edge?
                if (tspan >= span) {
                    return false
                }
                angle1 = clipangle
            }
            tspan = clipangle - angle2 and Tables.BITS32
            if (tspan > CLIPANGLE2) {
                tspan -= CLIPANGLE2
                tspan = tspan and Tables.BITS32

                // Totally off the left edge?
                if (tspan >= span) {
                    return false
                }
                angle2 = -clipangle
                angle2 = angle2 and Tables.BITS32
            }

            // Find the first clippost
            // that touches the source post
            // (adjacent pixels are touching).
            angle1 = angle1 + Tables.ANG90 and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT
            angle2 = angle2 + Tables.ANG90 and Tables.BITS32 ushr Tables.ANGLETOFINESHIFT
            sx1 = viewangletox[angle1.toInt()]
            sx2 = viewangletox[angle2.toInt()]

            // Does not cross a pixel.
            if (sx1 == sx2) {
                return false
            }
            sx2--
            var pstart = 0
            start = solidsegs[pstart]
            // FIXME: possible solidseg overflow here overflows
            while (start.last < sx2 && pstart < Limits.MAXSEGS) {
                start = solidsegs[pstart++]
            }
            return !(sx1 >= start.first && sx2 <= start.last)
        }

        /**
         * R_Subsector Determine floor/ceiling planes. Add sprites of things in
         * sector. Draw one or more line segments. It also alters the visplane
         * list!
         *
         * @param num
         * Subsector from subsector_t list in Lever Loader.
         */
        private fun Subsector(num: Int) {
            if (RendererState.DEBUG) {
                println("\t\tSubSector $num to render")
            }
            var count: Int
            var line: Int // pointer into a list of segs instead of seg_t
            val sub: subsector_t
            if (RendererState.RANGECHECK) {
                if (num >= DOOM.levelLoader.numsubsectors) {
                    DOOM.doomSystem.Error(
                        "R_Subsector: ss %d with numss = %d", num,
                        DOOM.levelLoader.numsubsectors
                    )
                }
            }
            sscount++
            sub = DOOM.levelLoader.subsectors[num]
            frontsector = sub.sector
            val frontsector = frontsector!!
            if (RendererState.DEBUG) {
                println("Frontsector to render :$frontsector")
            }
            count = sub.numlines
            // line = LL.segs[sub.firstline];
            line = sub.firstline
            if (RendererState.DEBUG) {
                println("Trying to find an existing FLOOR visplane...")
            }
            if (frontsector.floorheight < _view.z) {
                vp_vars.floorplane = vp_vars.FindPlane(
                    frontsector.floorheight,
                    frontsector.floorpic.toInt(), frontsector.lightlevel.toInt()
                )
            } else {
                // FIXME: unclear what would happen with a null visplane used
                // It's never checked explicitly for either condition, just
                // called straight.
                vp_vars.floorplane = -1 // in lieu of NULL
            }

            // System.out.println("Trying to find an existing CEILING visplane...");
            if (frontsector.ceilingheight > _view.z
                || frontsector.ceilingpic.toInt() == TexMan.getSkyFlatNum()
            ) {
                vp_vars.ceilingplane = vp_vars.FindPlane(
                    frontsector.ceilingheight,
                    frontsector.ceilingpic.toInt(), frontsector.lightlevel.toInt()
                )
            } else {
                vp_vars.ceilingplane = -1 // In lieu of NULL. Will bomb if
                // actually
                // used.
            }
            VIS.AddSprites(frontsector)
            if (RendererState.DEBUG) {
                println("Enter Addline for SubSector $num count $count")
            }
            while (count-- > 0) {
                AddLine(DOOM.levelLoader.segs[line])
                line++
            }
            if (RendererState.DEBUG) {
                println("Exit Addline for SubSector $num")
            }
        }

        /**
         * RenderBSPNode Renders all subsectors below a given node, traversing
         * subtree recursively. Just call with BSP root.
         */
        fun RenderBSPNode(bspnum: Int) {
            if (RendererState.DEBUG) {
                println("Processing BSP Node $bspnum")
            }
            val bsp: node_t
            val side: Int

            // Found a subsector? Then further decisions are taken, in, well,
            // SubSector.
            if (C2JUtils.flags(bspnum, Defines.NF_SUBSECTOR)) {
                if (RendererState.DEBUG) {
                    println("Subsector found.")
                }
                if (bspnum == -1) {
                    Subsector(0)
                } else {
                    Subsector(bspnum and Defines.NF_SUBSECTOR.inv())
                }
                return
            }
            bsp = DOOM.levelLoader.nodes[bspnum]

            // Decide which side the view point is on.
            side = bsp.PointOnSide(_view.x, _view.y)
            if (RendererState.DEBUG) {
                println("\tView side: $side")
            }

            // Recursively divide front space.
            if (RendererState.DEBUG) {
                println("\tEnter Front space of $bspnum")
            }
            RenderBSPNode(bsp.children[side])
            if (RendererState.DEBUG) {
                println("\tReturn Front space of $bspnum")
            }

            // Possibly divide back space.
            if (CheckBBox(bsp.bbox[side xor 1].bbox)) {
                if (RendererState.DEBUG) {
                    println("\tEnter Back space of $bspnum")
                }
                RenderBSPNode(bsp.children[side xor 1])
                if (RendererState.DEBUG) {
                    println("\tReturn Back space of $bspnum")
                }
            }
        }
    }

    protected abstract inner class SegDrawer(R: SceneRenderer<*, *>) : ISegDrawer {
        protected val vp_vars: Visplanes
        protected val seg_vars: SegVars

        // Fast blanking buffers.
        protected var _BLANKFLOORCLIP: ShortArray
        protected var _BLANKCEILINGCLIP: ShortArray

        override fun getBLANKFLOORCLIP(): ShortArray {
            return _BLANKFLOORCLIP
        }

        override fun getBLANKCEILINGCLIP(): ShortArray {
            return _BLANKCEILINGCLIP
        }

        /**
         * fixed_t
         */
        protected var pixhigh = 0
        protected var pixlow = 0
        protected var pixhighstep = 0
        protected var pixlowstep = 0
        protected var topfrac = 0
        protected var topstep = 0
        protected var bottomfrac = 0
        protected var bottomstep = 0
        protected var worldtop = 0
        protected var worldbottom = 0
        protected var worldhigh = 0
        protected var worldlow = 0

        /**
         * True if any of the segs textures might be visible.
         */
        protected var segtextured = false

        /**
         * Clip values are the solid pixel bounding the range. floorclip starts
         * out vs.getScreenHeight() ceilingclip starts out -1
         */
        protected var floorclip: ShortArray
        protected var ceilingclip: ShortArray
        override fun getFloorClip(): ShortArray {
            return floorclip
        }

        override fun getCeilingClip(): ShortArray {
            return ceilingclip
        }

        /**
         * False if the back side is the same plane.
         */
        protected var markfloor = false
        protected var markceiling = false
        protected var maskedtexture = false
        protected var toptexture = 0
        protected var bottomtexture = 0
        protected var midtexture = 0

        /**
         * angle_t, used after adding ANG90 in StoreWallRange
         */
        protected var rw_normalangle: Long = 0

        /**
         * angle to line origin
         */
        protected var rw_angle1: Long = 0

        //
        // regular wall
        //
        protected var rw_x = 0
        protected var rw_stopx = 0
        protected var rw_centerangle // angle_t
                : Long = 0

        /**
         * fixed_t
         */
        protected var rw_offset = 0
        protected var rw_distance = 0
        protected var rw_scale = 0
        protected var rw_scalestep = 0
        protected var rw_midtexturemid = 0
        protected var rw_toptexturemid = 0
        protected var rw_bottomtexturemid = 0
        override fun resetLimits() {
            val tmp = arrayOfNulls<drawseg_t>(seg_vars.MAXDRAWSEGS)
            System.arraycopy(seg_vars.drawsegs, 0, tmp, 0, seg_vars.MAXDRAWSEGS)

            // Now, that was quite a haircut!.
            seg_vars.drawsegs = tmp as Array<drawseg_t>

            // System.out.println("Drawseg buffer cut back to original limit of "+MAXDRAWSEGS);
        }

        override fun sync() {
            // Nothing required if serial.
        }

        /**
         * R_StoreWallRange A wall segment will be drawn between start and stop
         * pixels (inclusive). This is the only place where
         * markceiling/markfloor can be set. Can only be called from
         * ClipSolidWallSegment and ClipPassWallSegment.
         *
         * @throws IOException
         */
        override fun StoreWallRange(start: Int, stop: Int) {
            if (RendererState.DEBUG2) {
                println("\t\t\t\tStorewallrange called between $start and $stop")
            }
            val hyp: Int // fixed_t
            var sineval: Int // fixed_t
            val distangle: Int
            var offsetangle: Long // angle_t
            val vtop: Int // fixed_t
            var lightnum: Int
            val seg: drawseg_t

            // don't overflow and crash
            if (seg_vars.ds_p == seg_vars.drawsegs.size) {
                seg_vars.ResizeDrawsegs()
            }
            if (RendererState.RANGECHECK) {
                if (start >= _view.width || start > stop) {
                    DOOM.doomSystem.Error("Bad R_RenderWallRange: %d to %d", start, stop)
                }
            }
            seg = seg_vars.drawsegs[seg_vars.ds_p]
            val curline = MyBSP.curline!!
            MyBSP.sidedef = curline.sidedef
            MyBSP.linedef = curline.linedef

            // mark the segment as visible for auto map
            MyBSP.linedef!!.flags = (MyBSP.linedef!!.flags.toInt() or line_t.ML_MAPPED).toShort()

            // calculate rw_distance for scale calculation
            rw_normalangle = Tables.addAngles(curline.angle, Tables.ANG90)

            /*
             * MAES: ok, this is a tricky spot. angle_t's are supposed to be
             * always positive 32-bit unsigned integers, so a subtraction should
             * be always positive by definition, right? WRONG: this fucking spot
             * caused "blind spots" at certain angles because ONLY HERE angles
             * are supposed to be treated as SIGNED and result in differences
             * <180 degrees -_- The only way to coerce this behavior is to cast
             * both as signed ints.
             */offsetangle = Math.abs(rw_normalangle.toInt() - rw_angle1.toInt()).toLong()
            if (offsetangle > Tables.ANG90) {
                offsetangle = Tables.ANG90
            }

            // It should fit even in a signed int, by now.
            distangle = (Tables.ANG90 - offsetangle).toInt()
            hyp = PointToDist(curline.v1x, curline.v1y)
            sineval = Tables.finesine(distangle)
            rw_distance = FixedMul(hyp, sineval)
            rw_x = start
            seg.x1 = rw_x
            seg.x2 = stop
            seg.curline = curline
            /*
             * This is the only place it's ever explicitly assigned. Therefore
             * it always starts at stop+1.
             */rw_stopx = stop + 1

            // calculate scale at both ends and step
            // this is the ONLY place where rw_scale is set.
            rw_scale = ScaleFromGlobalAngle(_view.angle + _view.xtoviewangle[start])
            seg.scale1 = rw_scale
            if (stop > start) {
                seg.scale2 = ScaleFromGlobalAngle(_view.angle + _view.xtoviewangle[stop])
                rw_scalestep = (seg.scale2 - rw_scale) / (stop - start)
                seg.scalestep = rw_scalestep
            } else {
                // UNUSED: try to fix the stretched line bug
                /*
                 * #if 0 if (rw_distance < FRACUNIT/2) { fixed_t trx,try;
                 * fixed_t gxt,gyt; trx = curline.v1.x - viewx; try =
                 * curline.v1.y - viewy; gxt = FixedMul(trx,viewcos); gyt =
                 * -FixedMul(try,viewsin); seg.scale1 = FixedDiv(projection,
                 * gxt-gyt)<<detailshift; } #endif
                 */
                seg.scale2 = seg.scale1
            }

            // calculate texture boundaries
            // and decide if floor / ceiling marks are needed
            val frontsector = MyBSP.frontsector!!
            val linedef = MyBSP.linedef!!
            val sidedef = MyBSP.sidedef!!

            worldtop = frontsector.ceilingheight - _view.z
            worldbottom = frontsector.floorheight - _view.z
            bottomtexture = 0
            toptexture = bottomtexture
            midtexture = toptexture
            maskedtexture = false
            seg.setMaskedTextureCol(null, 0)
            // seg.maskedtexturecol = null;
            if (MyBSP.backsector == null) {
                // single sided line
                midtexture = TexMan.getTextureTranslation(sidedef.midtexture.toInt())
                // a single sided line is terminal, so it must mark ends
                markceiling = true
                markfloor = markceiling
                if (linedef.flags.toInt() and line_t.ML_DONTPEGBOTTOM != 0) {
                    vtop = (frontsector.floorheight
                            + TexMan.getTextureheight(sidedef.midtexture.toInt()))
                    // bottom of texture at bottom
                    rw_midtexturemid = vtop - _view.z
                } else {
                    // top of texture at top
                    rw_midtexturemid = worldtop
                }
                rw_midtexturemid += sidedef.rowoffset
                seg.silhouette = Defines.SIL_BOTH
                seg.setSprTopClip(_view.screenheightarray, 0)
                seg.setSprBottomClip(_view.negonearray, 0)
                seg.bsilheight = Int.MAX_VALUE
                seg.tsilheight = Int.MIN_VALUE
            } else {
                // two sided line
                val backsector = MyBSP.backsector!!
                seg.setSprTopClip(null, 0)
                seg.setSprBottomClip(null, 0)
                seg.silhouette = 0
                if (frontsector.floorheight > backsector.floorheight) {
                    seg.silhouette = Defines.SIL_BOTTOM
                    seg.bsilheight = frontsector.floorheight
                } else if (backsector.floorheight > _view.z) {
                    seg.silhouette = Defines.SIL_BOTTOM
                    seg.bsilheight = Int.MAX_VALUE
                    // seg.sprbottomclip = negonearray;
                }
                if (frontsector.ceilingheight < backsector.ceilingheight) {
                    seg.silhouette = seg.silhouette or Defines.SIL_TOP
                    seg.tsilheight = frontsector.ceilingheight
                } else if (backsector.ceilingheight < _view.z) {
                    seg.silhouette = seg.silhouette or Defines.SIL_TOP
                    seg.tsilheight = Int.MIN_VALUE
                    // seg.sprtopclip = screenheightarray;
                }
                if (backsector.ceilingheight <= frontsector.floorheight) {
                    seg.setSprBottomClip(_view.negonearray, 0)
                    seg.bsilheight = Int.MAX_VALUE
                    seg.silhouette = seg.silhouette or Defines.SIL_BOTTOM
                }
                if (backsector.floorheight >= frontsector.ceilingheight) {
                    seg.setSprTopClip(_view.screenheightarray, 0)
                    seg.tsilheight = Int.MIN_VALUE
                    seg.silhouette = seg.silhouette or Defines.SIL_TOP
                }
                worldhigh = backsector.ceilingheight - _view.z
                worldlow = backsector.floorheight - _view.z

                // hack to allow height changes in outdoor areas
                if (frontsector.ceilingpic.toInt() == TexMan.getSkyFlatNum()
                    && backsector.ceilingpic.toInt() == TexMan
                        .getSkyFlatNum()
                ) {
                    worldtop = worldhigh
                }
                markfloor =
                    worldlow != worldbottom || backsector.floorpic != frontsector.floorpic || backsector.lightlevel != frontsector.lightlevel // same plane on both sides
                markceiling =
                    worldhigh != worldtop || backsector.ceilingpic != frontsector.ceilingpic || backsector.lightlevel != frontsector.lightlevel // same plane on both sides
                if (backsector.ceilingheight <= frontsector.floorheight
                    || backsector.floorheight >= frontsector.ceilingheight
                ) {
                    // closed door
                    markfloor = true
                    markceiling = markfloor
                }
                if (worldhigh < worldtop) {
                    // top texture
                    toptexture = TexMan.getTextureTranslation(sidedef.toptexture.toInt())
                    if (linedef.flags.toInt() and line_t.ML_DONTPEGTOP != 0) {
                        // top of texture at top
                        rw_toptexturemid = worldtop
                    } else {
                        vtop = (backsector.ceilingheight
                                + TexMan.getTextureheight(sidedef.toptexture.toInt()))

                        // bottom of texture
                        rw_toptexturemid = vtop - _view.z
                    }
                }
                if (worldlow > worldbottom) {
                    // bottom texture
                    bottomtexture = TexMan.getTextureTranslation(sidedef.bottomtexture.toInt())
                    rw_bottomtexturemid = if (linedef.flags.toInt() and line_t.ML_DONTPEGBOTTOM != 0) {
                        // bottom of texture at bottom
                        // top of texture at top
                        worldtop
                    } else {
                        // top of texture at top
                        worldlow
                    }
                }
                rw_toptexturemid += sidedef.rowoffset
                rw_bottomtexturemid += sidedef.rowoffset

                // allocate space for masked texture tables
                if (sidedef.midtexture.toInt() != 0) {
                    // masked midtexture
                    maskedtexture = true
                    seg_vars.maskedtexturecol = vp_vars.openings
                    seg_vars.pmaskedtexturecol = vp_vars.lastopening - rw_x
                    seg.setMaskedTextureCol(
                        seg_vars.maskedtexturecol,
                        seg_vars.pmaskedtexturecol
                    )
                    vp_vars.lastopening += rw_stopx - rw_x
                }
            }

            // calculate rw_offset (only needed for textured lines)

            // calculate rw_offset (only needed for textured lines)
            segtextured = (midtexture or toptexture or bottomtexture != 0) or maskedtexture
            if (segtextured) {
                offsetangle = Tables.addAngles(rw_normalangle, -rw_angle1)

                // Another "tricky spot": negative of an unsigned number?
                if (offsetangle > Tables.ANG180) {
                    offsetangle = (-offsetangle.toInt()).toLong() and Tables.BITS32
                }
                if (offsetangle > Tables.ANG90) {
                    offsetangle = Tables.ANG90
                }
                sineval = Tables.finesine(offsetangle)
                rw_offset = FixedMul(hyp, sineval)

                // Another bug: we CAN'T assume that the result won't wrap
                // around.
                // If that assumption is made, then texture alignment issues
                // appear
                if (rw_normalangle - rw_angle1 and Tables.BITS32 < Tables.ANG180) {
                    rw_offset = -rw_offset
                }
                rw_offset += sidedef.textureoffset + curline.offset
                // This is OK, however: we can add as much shit as we want,
                // as long as we trim it to the 32 LSB. Proof as to why
                // this is always true is left as an exercise to the reader.
                rw_centerangle = Tables.ANG90 + _view.angle - rw_normalangle and Tables.BITS32

                // calculate light table
                // use different light tables
                // for horizontal / vertical / diagonal
                // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
                if (colormaps.fixedcolormap == null) {
                    lightnum = ((frontsector.lightlevel.toInt() shr colormaps.lightSegShift())
                            + colormaps.extralight)
                    if (curline.v1y == curline.v2y) {
                        lightnum--
                    } else if (curline.v1x == curline.v2x) {
                        lightnum++
                    }
                    if (lightnum < 0) {
                        colormaps.walllights = colormaps.scalelight[0] as Array<V>
                    } else if (lightnum >= colormaps.lightLevels()) {
                        colormaps.walllights = colormaps.scalelight[colormaps.lightLevels() - 1] as Array<V>
                    } else {
                        colormaps.walllights = colormaps.scalelight[lightnum] as Array<V>
                    }
                }
            }

            // if a floor / ceiling plane is on the wrong side
            // of the view plane, it is definitely invisible
            // and doesn't need to be marked.
            if (frontsector.floorheight >= _view.z) {
                // above view plane
                markfloor = false
            }
            if (frontsector.ceilingheight <= _view.z
                && frontsector.ceilingpic.toInt() != TexMan.getSkyFlatNum()
            ) {
                // below view plane
                markceiling = false
            }

            // calculate incremental stepping values for texture edges
            worldtop = worldtop shr 4
            worldbottom = worldbottom shr 4
            topstep = -FixedMul(rw_scalestep, worldtop)
            topfrac = (_view.centeryfrac shr 4) - FixedMul(worldtop, rw_scale)
            bottomstep = -FixedMul(rw_scalestep, worldbottom)
            bottomfrac = (_view.centeryfrac shr 4) - FixedMul(worldbottom, rw_scale)
            if (MyBSP.backsector != null) {
                worldhigh = worldhigh shr 4
                worldlow = worldlow shr 4
                if (worldhigh < worldtop) {
                    pixhigh = (_view.centeryfrac shr 4) - FixedMul(worldhigh, rw_scale)
                    pixhighstep = -FixedMul(rw_scalestep, worldhigh)
                }
                if (worldlow > worldbottom) {
                    pixlow = (_view.centeryfrac shr 4) - FixedMul(worldlow, rw_scale)
                    pixlowstep = -FixedMul(rw_scalestep, worldlow)
                }
            }

            // render it
            if (markceiling) {
                // System.out.println("Markceiling");
                vp_vars.ceilingplane = vp_vars.CheckPlane(vp_vars.ceilingplane, rw_x, rw_stopx - 1)
            }
            if (markfloor) {
                // System.out.println("Markfloor");
                vp_vars.floorplane = vp_vars.CheckPlane(vp_vars.floorplane, rw_x, rw_stopx - 1)
            }
            RenderSegLoop()

            // After rendering is actually performed, clipping is set.
            // save sprite clipping info ... no top clipping?
            if ((C2JUtils.flags(seg.silhouette, Defines.SIL_TOP) || maskedtexture)
                && seg.nullSprTopClip()
            ) {

                // memcpy (lastopening, ceilingclip+start, 2*(rw_stopx-start));
                System.arraycopy(
                    ceilingclip, start, vp_vars.openings,
                    vp_vars.lastopening, rw_stopx - start
                )
                seg.setSprTopClip(vp_vars.openings, vp_vars.lastopening - start)
                // seg.setSprTopClipPointer();
                vp_vars.lastopening += rw_stopx - start
            }
            // no floor clipping?
            if ((C2JUtils.flags(seg.silhouette, Defines.SIL_BOTTOM) || maskedtexture)
                && seg.nullSprBottomClip()
            ) {
                // memcpy (lastopening, floorclip+start, 2*(rw_stopx-start));
                System.arraycopy(
                    floorclip, start, vp_vars.openings,
                    vp_vars.lastopening, rw_stopx - start
                )
                seg.setSprBottomClip(
                    vp_vars.openings, vp_vars.lastopening
                            - start
                )
                vp_vars.lastopening += rw_stopx - start
            }
            if (maskedtexture && C2JUtils.flags(seg.silhouette, Defines.SIL_TOP)) {
                seg.silhouette = seg.silhouette or Defines.SIL_TOP
                seg.tsilheight = Int.MIN_VALUE
            }
            if (maskedtexture && seg.silhouette and Defines.SIL_BOTTOM == 0) {
                seg.silhouette = seg.silhouette or Defines.SIL_BOTTOM
                seg.bsilheight = Int.MAX_VALUE
            }
            seg_vars.ds_p++
        }

        /**
         * R_RenderSegLoop Draws zero, one, or two textures (and possibly a
         * masked texture) for walls. Can draw or mark the starting pixel of
         * floor and ceiling textures. Also sets the actual sprite clipping info
         * (where sprites should be cut) Since rw_x ranges are non-overlapping,
         * rendering all walls means completing the clipping list as well. The
         * only difference between the parallel and the non-parallel version is
         * that the parallel doesn't draw immediately but rather, generates
         * RWIs. This can surely be unified to avoid replicating code. CALLED:
         * CORE LOOPING ROUTINE.
         */
        protected open fun RenderSegLoop() {
            var angle: Int // angle_t
            var index: Int
            var yl: Int // low
            var yh: Int // hight
            var mid: Int
            var texturecolumn = 0 // fixed_t
            var top: Int
            var bottom: Int
            while (rw_x < rw_stopx) {

                // mark floor / ceiling areas
                yl = topfrac + HEIGHTUNIT - 1 shr HEIGHTBITS

                // no space above wall?
                if (yl < ceilingclip[rw_x] + 1) {
                    yl = ceilingclip[rw_x] + 1
                }
                if (markceiling) {
                    top = ceilingclip[rw_x] + 1
                    bottom = yl - 1
                    if (bottom >= floorclip[rw_x]) {
                        bottom = floorclip[rw_x] - 1
                    }
                    if (top <= bottom) {
                        vp_vars.visplanes[vp_vars.ceilingplane]!!.setTop(rw_x, top.toChar())
                        vp_vars.visplanes[vp_vars.ceilingplane]!!.setBottom(rw_x, bottom.toChar())
                    }
                }
                yh = bottomfrac shr HEIGHTBITS
                if (yh >= floorclip[rw_x]) {
                    yh = floorclip[rw_x] - 1
                }

                // A particular seg has been identified as a floor marker.
                if (markfloor) {
                    top = yh + 1
                    bottom = floorclip[rw_x] - 1
                    if (top <= ceilingclip[rw_x]) {
                        top = ceilingclip[rw_x] + 1
                    }
                    if (top <= bottom) {
                        vp_vars.visplanes[vp_vars.floorplane]!!.setTop(rw_x, top.toChar())
                        vp_vars.visplanes[vp_vars.floorplane]!!.setBottom(rw_x, bottom.toChar())
                    }
                }

                // texturecolumn and lighting are independent of wall tiers
                if (segtextured) {
                    // calculate texture offset

                    // CAREFUL: a VERY anomalous point in the code. Their sum is
                    // supposed
                    // to give an angle not exceeding 45 degrees (or an index of
                    // 0x0FFF after
                    // shifting). If added with pure unsigned rules, this
                    // doesn't hold anymore,
                    // not even if accounting for overflow.
                    angle = Tables.toBAMIndex(
                        rw_centerangle
                                + _view.xtoviewangle[rw_x].toInt()
                    )

                    // FIXME: We are accessing finetangent here, the code seems
                    // pretty confident in that angle won't exceed 4K no matter
                    // what.
                    // But xtoviewangle alone can yield 8K when shifted.
                    // This usually only overflows if we idclip and look at
                    // certain directions (probably angles get fucked up),
                    // however it seems rare
                    // enough to just "swallow" the exception. You can eliminate
                    // it by anding
                    // with 0x1FFF if you're so inclined.
                    // FIXED by allowing overflow. See Tables for details.
                    texturecolumn = rw_offset - FixedMul(Tables.finetangent[angle], rw_distance)
                    texturecolumn = texturecolumn shr FRACBITS
                    // calculate lighting
                    index = rw_scale shr colormaps.lightScaleShift()
                    if (index >= colormaps.maxLightScale()) {
                        index = colormaps.maxLightScale() - 1
                    }
                    dcvars.dc_colormap = colormaps.walllights[index]
                    dcvars.dc_x = rw_x
                    dcvars.dc_iscale = (0xffffffffL / rw_scale).toInt()
                }

                // draw the wall tiers
                if (midtexture != 0) {
                    // single sided line
                    dcvars.dc_yl = yl
                    dcvars.dc_yh = yh
                    dcvars.dc_texheight = TexMan.getTextureheight(midtexture) shr FRACBITS // killough
                    dcvars.dc_texturemid = rw_midtexturemid
                    dcvars.dc_source_ofs = 0
                    dcvars.dc_source = TexMan.GetCachedColumn(midtexture, texturecolumn)
                    CompleteColumn()
                    ceilingclip[rw_x] = _view.height.toShort()
                    floorclip[rw_x] = -1
                } else {
                    // two sided line
                    if (toptexture != 0) {
                        // top wall
                        mid = pixhigh shr HEIGHTBITS
                        pixhigh += pixhighstep
                        if (mid >= floorclip[rw_x]) {
                            mid = floorclip[rw_x] - 1
                        }
                        if (mid >= yl) {
                            dcvars.dc_yl = yl
                            dcvars.dc_yh = mid
                            dcvars.dc_texturemid = rw_toptexturemid
                            dcvars.dc_texheight = TexMan.getTextureheight(toptexture) shr FRACBITS
                            dcvars.dc_source = TexMan.GetCachedColumn(toptexture, texturecolumn)
                            dcvars.dc_source_ofs = 0
                            if (dcvars.dc_colormap == null) {
                                println("Two-sided")
                            }
                            CompleteColumn()
                            ceilingclip[rw_x] = mid.toShort()
                        } else {
                            ceilingclip[rw_x] = (yl - 1).toShort()
                        }
                    } else {
                        // no top wall
                        if (markceiling) {
                            ceilingclip[rw_x] = (yl - 1).toShort()
                        }
                    }
                    if (bottomtexture != 0) {
                        // bottom wall
                        mid = pixlow + HEIGHTUNIT - 1 shr HEIGHTBITS
                        pixlow += pixlowstep

                        // no space above wall?
                        if (mid <= ceilingclip[rw_x]) {
                            mid = ceilingclip[rw_x] + 1
                        }
                        if (mid <= yh) {
                            dcvars.dc_yl = mid
                            dcvars.dc_yh = yh
                            dcvars.dc_texturemid = rw_bottomtexturemid
                            dcvars.dc_texheight = TexMan.getTextureheight(bottomtexture) shr FRACBITS
                            dcvars.dc_source = TexMan.GetCachedColumn(bottomtexture, texturecolumn)
                            dcvars.dc_source_ofs = 0
                            CompleteColumn()
                            floorclip[rw_x] = mid.toShort()
                        } else {
                            floorclip[rw_x] = (yh + 1).toShort()
                        }
                    } else {
                        // no bottom wall
                        if (markfloor) {
                            floorclip[rw_x] = (yh + 1).toShort()
                        }
                    }
                    if (maskedtexture) {
                        // save texturecol
                        // for backdrawing of masked mid texture
                        seg_vars.maskedtexturecol[seg_vars.pmaskedtexturecol + rw_x] = texturecolumn.toShort()
                    }
                }
                rw_scale += rw_scalestep
                topfrac += topstep
                bottomfrac += bottomstep
                rw_x++
            }
        }

        override fun ClearClips() {
            System.arraycopy(_BLANKFLOORCLIP, 0, floorclip, 0, _view.width)
            System.arraycopy(_BLANKCEILINGCLIP, 0, ceilingclip, 0, _view.width)
        }

        /**
         * Called from RenderSegLoop. This should either invoke the column
         * function, or store a wall rendering instruction in the parallel
         * version. It's the only difference between the parallel and serial
         * renderer, BTW. So override and implement accordingly.
         */
        protected abstract fun CompleteColumn()
        override fun ExecuteSetViewSize(viewwidth: Int) {
            for (i in 0 until viewwidth) {
                _BLANKFLOORCLIP[i] = _view.height.toShort()
                _BLANKCEILINGCLIP[i] = -1
            }
        }

        override fun CompleteRendering() {
            // Nothing to do for serial. 
        }

        protected var col: column_t

        init {
            this.vp_vars = R.getVPVars()
            this.seg_vars = R.getSegVars()
            col = column_t()
            seg_vars.drawsegs = GenericCopy.malloc({ drawseg_t() }, seg_vars.MAXDRAWSEGS)
            floorclip = ShortArray(DOOM.vs.getScreenWidth())
            ceilingclip = ShortArray(DOOM.vs.getScreenWidth())
            _BLANKFLOORCLIP = ShortArray(DOOM.vs.getScreenWidth())
            _BLANKCEILINGCLIP = ShortArray(DOOM.vs.getScreenWidth())
        }

        /**
         * R_ScaleFromGlobalAngle Returns the texture mapping scale for the
         * current line (horizontal span) at the given angle. rw_distance must
         * be calculated first.
         */
        protected fun ScaleFromGlobalAngle(visangle: Long): Int {
            var scale: Int // fixed_t
            val anglea: Long
            val angleb: Long
            val sinea: Int
            val sineb: Int
            val num: Int // fixed_t
            val den: Int

            // UNUSED
            /*
             * { fixed_t dist; fixed_t z; fixed_t sinv; fixed_t cosv; sinv =
             * finesine[(visangle-rw_normalangle)>>ANGLETOFINESHIFT]; dist =
             * FixedDiv (rw_distance, sinv); cosv =
             * finecosine[(viewangle-visangle)>>ANGLETOFINESHIFT]; z =
             * abs(FixedMul (dist, cosv)); scale = FixedDiv(projection, z);
             * return scale; }
             */anglea = Tables.ANG90 + visangle - _view.angle and Tables.BITS32
            angleb = Tables.ANG90 + visangle - rw_normalangle and Tables.BITS32

            // both sines are allways positive
            sinea = Tables.finesine(anglea)
            sineb = Tables.finesine(angleb)
            num = FixedMul(_view.projection, sineb) shl _view.detailshift
            den = FixedMul(rw_distance, sinea)
            if (den > num shr 16) {
                scale = FixedDiv(num, den)
                if (scale > 64 * FRACUNIT) {
                    scale = 64 * FRACUNIT
                } else if (scale < 256) {
                    scale = 256
                }
            } else {
                scale = 64 * FRACUNIT
            }
            return scale
        }

        override fun setGlobalAngle(angle: Long) {
            rw_angle1 = angle
        }

        //companion object {
            protected /*const */val HEIGHTBITS = 12
            protected /*const*/ val HEIGHTUNIT = 1 shl HEIGHTBITS
        //}
    }

    interface IPlaneDrawer {
        fun InitPlanes()
        fun MapPlane(y: Int, x1: Int, x2: Int)
        fun DrawPlanes()
        fun getDistScale(): IntArray

        /**
         * Sync up in case there's concurrent planes/walls rendering
         */
        fun sync()
    }

    protected interface ISegDrawer : ILimitResettable {
        fun ClearClips()
        fun getBLANKCEILINGCLIP(): ShortArray
        fun getBLANKFLOORCLIP(): ShortArray
        fun getFloorClip(): ShortArray
        fun getCeilingClip(): ShortArray
        fun ExecuteSetViewSize(viewwidth: Int)
        fun setGlobalAngle(angle1: Long)
        fun StoreWallRange(first: Int, last: Int)

        /**
         * If there is anything to do beyond the BPS traversal,
         * e.g. parallel rendering
         */
        fun CompleteRendering()

        /**
         * Sync up in case there's concurrent planes/walls rendering
         */
        fun sync()
    }

    protected inner class Planes internal constructor(DOOM: DoomMain<T, V>, R: RendererState<T, V>) :
        PlaneDrawer<T, V>(DOOM, R) {
        /**
         * R_DrawPlanes At the end of each frame. This also means that visplanes
         * must have been set BEFORE we called this function. Therefore, look
         * for errors behind.
         *
         * @throws IOException
         */
        override fun DrawPlanes() {
            if (RendererState.DEBUG) {
                println(" >>>>>>>>>>>>>>>>>>>>>   DrawPlanes: " + vp_vars.lastvisplane)
            }
            var pln: visplane_t // visplane_t
            var light: Int
            var x: Int
            var stop: Int
            var angle: Int
            if (RANGECHECK) {
                rangeCheckErrors()
            }
            for (pl in 0 until vp_vars.lastvisplane) {
                pln = vp_vars.visplanes[pl]!!
                if (RendererState.DEBUG2) {
                    println(pln)
                }
                if (pln.minx > pln.maxx) {
                    continue
                }
                // sky flat
                if (pln.picnum == TexMan.getSkyFlatNum()) {
                    // Cache skytexture stuff here. They aren't going to change
                    // while
                    // being drawn, after all, are they?
                    val skytexture = TexMan.getSkyTexture()
                    skydcvars.dc_texheight = TexMan.getTextureheight(skytexture) shr FRACBITS
                    skydcvars.dc_iscale = vpvars.getSkyScale() shr view.detailshift
                    /**
                     * Sky is allways drawn full bright, i.e. colormaps[0] is
                     * used. Because of this hack, sky is not affected by INVUL
                     * inverse mapping.
                     * Settings.fixskypalette handles the fix
                     */
                    if (DOOM.CM.equals(
                            Settings.fix_sky_palette,
                            java.lang.Boolean.TRUE
                        ) && colormap.fixedcolormap != null
                    ) {
                        skydcvars.dc_colormap = colormap.fixedcolormap
                    } else {
                        skydcvars.dc_colormap = colormap.colormaps[Lights.COLORMAP_FIXED]
                    }
                    skydcvars.dc_texturemid = TexMan.getSkyTextureMid()
                    x = pln.minx
                    while (x <= pln.maxx) {
                        skydcvars.dc_yl = pln.getTop(x).code
                        skydcvars.dc_yh = pln.getBottom(x)
                        if (skydcvars.dc_yl <= skydcvars.dc_yh) {
                            angle = (Tables.addAngles(
                                view.angle,
                                view.xtoviewangle[x]
                            ) ushr Defines.ANGLETOSKYSHIFT).toInt()
                            skydcvars.dc_x = x
                            // Optimized: texheight is going to be the same
                            // during normal skies drawing...right?
                            skydcvars.dc_source = TexMan.GetCachedColumn(skytexture, angle)
                            colfunc!!.sky!!.invoke()
                        }
                        x++
                    }
                    continue
                }

                // regular flat
                dsvars.ds_source = TexMan.getSafeFlat(pln.picnum)
                planeheight = Math.abs(pln.height - view.z)
                light = (pln.lightlevel shr colormap.lightSegShift()) + colormap.extralight
                if (light >= colormap.lightLevels()) {
                    light = colormap.lightLevels() - 1
                }
                if (light < 0) {
                    light = 0
                }
                planezlight = colormap.zlight[light] as Array<V>

                // We set those values at the border of a plane's top to a
                // "sentinel" value...ok.
                pln.setTop(pln.maxx + 1, SENTINEL)
                pln.setTop(pln.minx - 1, SENTINEL)
                stop = pln.maxx + 1
                x = pln.minx
                while (x <= stop) {
                    MakeSpans(x, pln.getTop(x - 1).code, pln.getBottom(x - 1), pln.getTop(x).code, pln.getBottom(x))
                    x++
                }

                // Z_ChangeTag (ds_source, PU_CACHE);
            }
        }
    } // End Plane class
    // /////////////////////// LIGHTS, POINTERS, COLORMAPS ETC. ////////////////
    // /// FROM R_DATA, R_MAIN , R_DRAW //////////
    /**
     * OK< this is supposed to "peg" into screen buffer 0. It will work AS LONG
     * AS SOMEONE FUCKING ACTUALLY SETS IT !!!!
     */
    protected var screen: V? = null

    /**
     * These are actually offsets inside screen 0 (or any screen). Therefore
     * anything using them should "draw" inside screen 0
     */
    protected var ylookup = IntArray(Limits.MAXHEIGHT)

    /**
     * Columns offset to set where?!
     */
    protected var columnofs = IntArray(Limits.MAXWIDTH)

    /**
     * General purpose. Used for solid walls and as an intermediary for
     * threading
     */
    protected var dcvars: ColVars<T, V>

    /**
     * Used for spans
     */
    protected var dsvars: SpanVars<T, V>

    // Used for sky drawer, to avoid clashing with shared dcvars
    protected var skydcvars: ColVars<T, V>

    /**
     * Masked drawing functions get "pegged" to this set of dcvars, passed upon
     * initialization. However, multi-threaded vars are better off carrying each
     * their own ones.
     */
    protected var maskedcvars: ColVars<T, V>
    /**
     * e6y: wide-res Borrowed from PrBoom+;
     */
    /*
     * protected int wide_centerx, wide_ratio, wide_offsetx, wide_offset2x,
     * wide_offsety, wide_offset2y; protected final base_ratio_t[]
     * BaseRatioSizes = { new base_ratio_t(960, 600, 0, 48, 1.333333f), // 4:3
     * new base_ratio_t(1280, 450, 0, 48 * 3 / 4, 1.777777f), // 16:9 new
     * base_ratio_t(1152, 500, 0, 48 * 5 / 6, 1.6f), // 16:10 new
     * base_ratio_t(960, 600, 0, 48, 1.333333f), new base_ratio_t(960, 640,
     * (int) (6.5 * FRACUNIT), 48 * 15 / 16, 1.25f) // 5:4 };
     */
    /**
     * just for profiling purposes
     */
    protected var framecount = 0
    protected var sscount = 0
    protected var linecount = 0
    protected var loopcount = 0

    //
    // precalculated math tables
    //
    protected var clipangle: Long = 0

    // Set to 2*clipangle later.
    protected var CLIPANGLE2: Long = 0

    // The viewangletox[viewangle + FINEANGLES/4] lookup
    // maps the visible view angles to screen X coordinates,
    // flattening the arc to a flat projection plane.
    // There will be many angles mapped to the same X.
    protected val viewangletox = IntArray(Tables.FINEANGLES / 2)
    /**
     * The xtoviewangle[] table maps a screen pixel to the lowest viewangle that
     * maps back to x ranges from clipangle to -clipangle.
     *
     * @see _view.xtoviewangle
     */
    //protected long[] view.xtoviewangle;// MAES: to resize
    // UNUSED.
    // The finetangentgent[angle+FINEANGLES/4] table
    // holds the fixed_t tangent values for view angles,
    // ranging from MININT to 0 to MAXINT.
    // fixed_t finetangent[FINEANGLES/2];
    // fixed_t finesine[5*FINEANGLES/4];
    // MAES: uh oh. So now all these ints must become finesines? fuck that.
    // Also wtf @ this hack....this points to approx 1/4th of the finesine
    // table, but what happens if I read past it?
    // int[] finecosine = finesine[FINEANGLES/4];
    /*
     * MAES: what's going on with light tables here. OK...so these should be
     * "unsigned bytes", since, after all, they'll be used as pointers inside an
     * array to finally pick a color, so they should be expanded to shorts.
     */
    // //////////// SOME UTILITY METHODS /////////////
    /**
     * Assigns a point of view before calling PointToAngle CAREFUL: this isn't a
     * pure function, as it alters the renderer's state!
     */
    override fun PointToAngle2(x1: Int, y1: Int, x2: Int, y2: Int): Long {
        // Careful with assignments...
        _view.x = x1
        _view.y = y1
        return _view.PointToAngle(x2, y2)
    }

    //
    // R_InitTables
    //
    protected fun InitTables() {
        // UNUSED: now getting from tables.c
        /*
         * int i; float a; float fv; int t; // viewangle tangent table for (i=0
         * ; i<FINEANGLES/2 ; i++) { a = (i-FINEANGLES/4+0.5)*PI*2/FINEANGLES;
         * fv = FRACUNIT*tan (a); t = fv; finetangent[i] = t; } // finesine
         * table for (i=0 ; i<5*FINEANGLES/4 ; i++) { // OPTIMIZE: mirro.. a =
         * (i+0.5)*PI*2/FINEANGLES; t = FRACUNIT*sin (a); finesine[i] = t; }
         */
    }

    /**
     * R_PointToDist
     *
     * @param x
     * fixed_t
     * @param y
     * fixed_t
     * @return
     */
    protected fun PointToDist(x: Int, y: Int): Int {
        var angle: Int
        var dx: Int
        var dy: Int
        val temp: Int
        val dist: Int
        dx = Math.abs(x - _view.x)
        dy = Math.abs(y - _view.y)

        // If something is farther north/south than west/east, it gets swapped.
        // Probably as a crude way to avoid divisions by zero. This divides
        // the field into octants, rather than quadrants, where the biggest
        // angle to
        // consider is 45...right? So dy/dx can never exceed 1.0, in theory.
        if (dy > dx) {
            temp = dx
            dx = dy
            dy = temp
        }

        // If one or both of the distances are *exactly* zero at this point,
        // then this means that the wall is in your face anyway, plus we want to
        // avoid a division by zero. So you get zero.
        if (dx == 0) {
            return 0
        }

        /*
         * If dx is zero, this is going to bomb. Fixeddiv will return MAXINT aka
         * 7FFFFFFF, >> DBITS will make it 3FFFFFF, which is more than enough to
         * break tantoangle[]. In the original C code, this probably didn't
         * matter: there would probably be garbage orientations thrown all
         * around. However this is unacceptable in Java. OK, so the safeguard
         * above prevents that. Still... this method is only called once per
         * visible wall per frame, so one check more or less at this point won't
         * change much. It's better to be safe than sorry.
         */
        // This effectively limits the angle to
        // angle = Math.max(FixedDiv(dy, dx), 2048) >> DBITS;
        angle = FixedDiv(dy, dx) and 0x1FFFF shr Tables.DBITS

        // Since the division will be 0xFFFF at most, DBITS will restrict
        // the maximum angle index to 7FF, about 45, so adding ANG90 with
        // no other safeguards is OK.
        angle = (Tables.tantoangle[angle] + Tables.ANG90 shr Tables.ANGLETOFINESHIFT).toInt()

        // use as cosine
        dist = FixedDiv(dx, Tables.finesine[angle])
        return dist
    }

    // //////////// COMMON RENDERING GLOBALS ////////////////
    // //////////////// COLUMN AND SPAN FUNCTIONS //////////////
    protected var colfunc: ColFuncs<T, V>? = null

    // Keep two sets of functions.
    protected var colfunchi: ColFuncs<T, V>
    protected var colfunclow: ColFuncs<T, V>
    protected fun setHiColFuns() {
        colfunchi.base = DrawColumn
        colfunchi.main = colfunchi.base
        colfunchi.masked = DrawColumnMasked
        colfunchi.fuzz = DrawFuzzColumn
        colfunchi.trans = DrawTranslatedColumn
        colfunchi.glass = DrawTLColumn
        colfunchi.player = DrawColumnPlayer
        colfunchi.sky = DrawColumnSkies
    }

    protected fun setLowColFuns() {
        colfunclow.base = DrawColumnLow
        colfunclow.main = colfunclow.base
        colfunclow.masked = DrawColumnMaskedLow
        colfunclow.fuzz = DrawFuzzColumnLow
        colfunclow.trans = DrawTranslatedColumnLow
        colfunclow.glass = DrawTLColumn
        colfunclow.player = DrawColumnMaskedLow
        colfunclow.sky = DrawColumnSkiesLow
    }

    override fun getColFuncsHi(): ColFuncs<T, V> {
        return colfunchi
    }

    override fun getColFuncsLow(): ColFuncs<T, V> {
        return colfunclow
    }

    override fun getMaskedDCVars(): ColVars<T, V> {
        return maskedcvars
    }

    // These column functions are "fixed" for a given renderer, and are
    // not used directly, but only after passing them to colfuncs
    protected var DrawTranslatedColumn: DoomColumnFunction<T, V>? = null
    protected var DrawTranslatedColumnLow: DoomColumnFunction<T, V>? = null
    protected var DrawColumnPlayer: DoomColumnFunction<T, V>? = null
    protected var DrawColumnSkies: DoomColumnFunction<T, V>? = null
    protected var DrawColumnSkiesLow: DoomColumnFunction<T, V>? = null
    protected var DrawFuzzColumn: DoomColumnFunction<T, V>? = null
    protected var DrawFuzzColumnLow: DoomColumnFunction<T, V>? = null
    protected var DrawColumn: DoomColumnFunction<T, V>? = null
    protected var DrawColumnLow: DoomColumnFunction<T, V>? = null
    protected var DrawColumnMasked: DoomColumnFunction<T, V>? = null
    protected var DrawColumnMaskedLow: DoomColumnFunction<T, V>? = null
    protected var DrawTLColumn: DoomColumnFunction<T, V>? = null

    /**
     * to be set in UnifiedRenderer
     */
    protected var DrawSpan: DoomSpanFunction<T, V>? = null
    protected var DrawSpanLow: DoomSpanFunction<T, V>? = null
    // ////////////// r_draw methods //////////////
    /**
     * R_DrawViewBorder Draws the border around the view for different size windows
     * Made use of CopyRect there
     * - Good Sign 2017/04/06
     */
    override fun DrawViewBorder() {
        if (_view.scaledwidth == DOOM.vs.getScreenWidth()) {
            return
        }
        val top = (DOOM.vs.getScreenHeight() - DOOM.statusBar.getHeight() - _view.height) / 2
        val side = (DOOM.vs.getScreenWidth() - _view.scaledwidth) / 2
        val rect: Rectangle
        // copy top
        rect = Rectangle(0, 0, DOOM.vs.getScreenWidth(), top)
        DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
        // copy left side
        rect.setBounds(0, top, side, _view.height)
        DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
        // copy right side
        rect.x = side + _view.scaledwidth
        DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
        // copy bottom
        rect.setBounds(0, top + _view.height, DOOM.vs.getScreenWidth(), top)
        DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
    }

    override fun ExecuteSetViewSize() {
        var cosadj: Int
        var dy: Int
        var level: Int
        var startmap: Int
        setsizeneeded = false

        // 11 Blocks means "full screen"
        if (setblocks == 11) {
            _view.scaledwidth = DOOM.vs.getScreenWidth()
            _view.height = DOOM.vs.getScreenHeight()
        } else if (DOOM.CM.equals(Settings.scale_screen_tiles, java.lang.Boolean.TRUE)) {
            /**
             * Make it exactly as in vanilla DOOM
             * - Good Sign 2017/05/08
             */
            _view.scaledwidth = setblocks * 32 * DOOM.vs.getScalingX()
            _view.height = (setblocks * 168 / 10 and 7.inv()) * DOOM.vs.getScalingY()
        } else { // Mocha Doom formula looks better for non-scaled tiles
            _view.scaledwidth = setblocks * (DOOM.vs.getScreenWidth() / 10)
            // Height can only be a multiple of 8.
            _view.height =
                (setblocks * (DOOM.vs.getScreenHeight() - DOOM.statusBar.getHeight()) / 10 and 7.inv()).toShort().toInt()
        }
        dcvars.viewheight = _view.height
        maskedcvars.viewheight = dcvars.viewheight
        skydcvars.viewheight = maskedcvars.viewheight
        _view.detailshift = setdetail
        _view.width = _view.scaledwidth shr _view.detailshift
        _view.centery = _view.height / 2
        _view.centerx = _view.width / 2
        _view.centerxfrac = _view.centerx shl FRACBITS
        _view.centeryfrac = _view.centery shl FRACBITS
        _view.projection = _view.centerxfrac
        dcvars.centery = _view.centery
        maskedcvars.centery = dcvars.centery
        skydcvars.centery = maskedcvars.centery

        // High detail
        if (_view.detailshift == 0) {
            colfunc = colfunchi
            dsvars.spanfunc = DrawSpan
        } else {
            // Low detail
            colfunc = colfunclow
            dsvars.spanfunc = DrawSpanLow
        }
        InitBuffer(_view.scaledwidth, _view.height)
        InitTextureMapping()

        // psprite scales
        // pspritescale = FRACUNIT*viewwidth/vs.getScreenWidth();
        // pspriteiscale = FRACUNIT*vs.getScreenWidth()/viewwidth;
        MyThings.setPspriteScale((FRACUNIT * (DOOM.vs.getScreenMul() * _view.width) / DOOM.vs.getScreenWidth()).toInt())
        MyThings.setPspriteIscale((FRACUNIT * (DOOM.vs.getScreenWidth() / (_view.width * DOOM.vs.getScreenMul()))).toInt())
        vp_vars.setSkyScale(
            (FRACUNIT * (DOOM.vs.getScreenWidth() / (_view.width * DOOM.vs.getScreenMul()))).toInt())
        _view.BOBADJUST = DOOM.vs.getSafeScaling() shl 15
        _view.WEAPONADJUST = (DOOM.vs.getScreenWidth() / (2 * DOOM.vs.getScreenMul()) * FRACUNIT).toInt()

        // thing clipping
        for (i in 0 until _view.width) {
            _view.screenheightarray[i] = _view.height.toShort()
        }

        // planes
        for (i in 0 until _view.height) {
            dy = (i - _view.height / 2 shl FRACBITS) + FRACUNIT / 2
            dy = Math.abs(dy)
            vp_vars.yslope[i] =
                FixedDiv((_view.width shl _view.detailshift) / 2 * FRACUNIT, dy)
            // MyPlanes.yslopef[i] = ((viewwidth<<detailshift)/2)/ dy;
        }

        // double cosadjf;
        for (i in 0 until _view.width) {
            // MAES: In this spot we must interpet it as SIGNED, else it's
            // pointless, right?
            // MAES: this spot caused the "warped floor bug", now fixed. Don't
            // forget xtoviewangle[i]!
            cosadj = Math.abs(Tables.finecosine(_view.xtoviewangle[i]))
            // cosadjf =
            // Math.abs(Math.cos((double)xtoviewangle[i]/(double)0xFFFFFFFFL));
            MyPlanes.getDistScale()[i] = FixedDiv(FRACUNIT, cosadj)
            // MyPlanes.distscalef[i] = (float) (1.0/cosadjf);
        }

        // Calculate the light levels to use
        // for each level / scale combination.
        for (i in 0 until colormaps.lightLevels()) {
            startmap =
                (colormaps.lightLevels() - colormaps.lightBright() - i) * 2 * colormaps.numColorMaps() / colormaps.lightLevels()
            for (j in 0 until colormaps.maxLightScale()) {
                level = startmap - j / RendererState.DISTMAP
                if (level < 0) {
                    level = 0
                }
                if (level >= colormaps.numColorMaps()) {
                    level = colormaps.numColorMaps() - 1
                }
                colormaps.scalelight[i][j] = colormaps.colormaps[level]
            }
        }
        MySegs!!.ExecuteSetViewSize(_view.width)
    }

    private val backScreenRect = Rectangle()
    private val tilePatchRect = Rectangle()

    /**
     * R_FillBackScreen Fills the back screen with a pattern for variable screen
     * sizes Also draws a beveled edge. This is actually stored in screen 1, and
     * is only OCCASIONALLY written to screen 0 (the visible one) by calling
     * R_VideoErase.
     */
    @R_Draw.C(R_Draw.R_FillBackScreen)
    override fun FillBackScreen() {
        val scaleSetting: Boolean =
            Engine.getConfig().equals(Settings.scale_screen_tiles, java.lang.Boolean.TRUE)
        val src: flat_t
        val dest: DoomScreen
        var x: Int
        var y: Int
        var patch: patch_t?

        // DOOM border patch.
        val name1 = "FLOOR7_2"

        // DOOM II border patch.
        val name2 = "GRNROCK"
        val name: String
        if (_view.scaledwidth == DOOM.vs.getScreenWidth()) {
            return
        }
        name = if (DOOM.isCommercial()) {
            name2
        } else {
            name1
        }

        /* This is a flat we're reading here */src =
            DOOM.wadLoader.CacheLumpName(name, Defines.PU_CACHE, flat_t::class.java)
        dest = DoomScreen.BG
        /**
         * TODO: cache it?
         * This part actually draws the border itself, without bevels
         *
         * MAES:
         * improved drawing routine for extended bit-depth compatibility.
         *
         * Now supports configurable vanilla-like scaling of tiles
         * - Good Sign 2017/04/09
         *
         * @SourceCode.Compatible
         */
        Tiling@ run {
            backScreenRect.setBounds(0, 0, DOOM.vs.getScreenWidth(), DOOM.vs.getScreenHeight() - DOOM.statusBar.getHeight())
            tilePatchRect.setBounds(0, 0, 64, 64)
            var block = DOOM.graphicSystem.convertPalettedBlock(*src.data)
            if (scaleSetting) {
                block = DOOM.graphicSystem.ScaleBlock(block, DOOM.vs, tilePatchRect.width, tilePatchRect.height)
                tilePatchRect.width *= DOOM.graphicSystem.getScalingX()
                tilePatchRect.height *= DOOM.graphicSystem.getScalingY()
            }
            DOOM.graphicSystem.TileScreenArea(dest, backScreenRect, block, tilePatchRect)
        }
        val scaleFlags: Int =
            DoomGraphicSystem.V_NOSCALESTART or if (scaleSetting) 0 else DoomGraphicSystem.V_NOSCALEOFFSET or DoomGraphicSystem.V_NOSCALEPATCH
        val stepX = if (scaleSetting) DOOM.graphicSystem.getScalingX() shl 3 else 8
        val stepY = if (scaleSetting) DOOM.graphicSystem.getScalingY() shl 3 else 8
        patch = DOOM.wadLoader.CachePatchName("BRDR_T", Defines.PU_CACHE)
        x = 0
        while (x < _view.scaledwidth) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.BG,
                patch,
                DOOM.vs,
                _view.windowx + x,
                _view.windowy - stepY,
                scaleFlags
            )
            x += stepX
        }
        patch = DOOM.wadLoader.CachePatchName("BRDR_B", Defines.PU_CACHE)
        x = 0
        while (x < _view.scaledwidth) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.BG,
                patch,
                DOOM.vs,
                _view.windowx + x,
                _view.windowy + _view.height,
                scaleFlags
            )
            x += stepX
        }
        patch = DOOM.wadLoader.CachePatchName("BRDR_L", Defines.PU_CACHE)
        y = 0
        while (y < _view.height) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.BG,
                patch,
                DOOM.vs,
                _view.windowx - stepX,
                _view.windowy + y,
                scaleFlags
            )
            y += stepY
        }
        patch = DOOM.wadLoader.CachePatchName("BRDR_R", Defines.PU_CACHE)
        y = 0
        while (y < _view.height) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.BG,
                patch,
                DOOM.vs,
                _view.windowx + _view.scaledwidth,
                _view.windowy + y,
                scaleFlags
            )
            y += stepY
        }

        // Draw beveled edge. Top-left
        patch = DOOM.wadLoader.CachePatchName("BRDR_TL", Defines.PU_CACHE)
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.BG,
            patch,
            DOOM.vs,
            _view.windowx - stepX,
            _view.windowy - stepY,
            scaleFlags
        )

        // Top-right.
        patch = DOOM.wadLoader.CachePatchName("BRDR_TR", Defines.PU_CACHE)
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.BG,
            patch,
            DOOM.vs,
            _view.windowx + _view.scaledwidth,
            _view.windowy - stepY,
            scaleFlags
        )

        // Bottom-left
        patch = DOOM.wadLoader.CachePatchName("BRDR_BL", Defines.PU_CACHE)
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.BG,
            patch,
            DOOM.vs,
            _view.windowx - stepX,
            _view.windowy + _view.height,
            scaleFlags
        )

        // Bottom-right.
        patch = DOOM.wadLoader.CachePatchName("BRDR_BR", Defines.PU_CACHE)
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.BG,
            patch,
            DOOM.vs,
            _view.windowx + _view.width,
            _view.windowy + _view.height,
            scaleFlags
        )
    }

    /**
     * R_Init
     */
    override fun Init() {
        // Any good reason for this to be here?
        // drawsegs=new drawseg_t[MAXDRAWSEGS];
        // C2JUtils.initArrayOfObjects(drawsegs);

        // DON'T FORGET ABOUT MEEEEEE!!!11!!!
        screen = DOOM.graphicSystem.getScreen(DoomScreen.FG)
        print("\nR_InitData")
        InitData()
        // InitPointToAngle ();
        print("\nR_InitPointToAngle")

        // ds.DM.viewwidth / ds.viewheight / detailLevel are set by the defaults
        print("\nR_InitTables")
        InitTables()
        SetViewSize(DOOM.menu.screenBlocks, DOOM.menu.detailLevel)
        print("\nR_InitPlanes")
        MyPlanes.InitPlanes()
        print("\nR_InitLightTables")
        InitLightTables()
        print(
            """
    
    R_InitSkyMap: ${TexMan.InitSkyMap()}
    """.trimIndent()
        )
        print("\nR_InitTranslationsTables")
        InitTranslationTables()
        print("\nR_InitTranMap: ")
        R_InitTranMap(0)
        print("\nR_InitDrawingFunctions: ")
        R_InitDrawingFunctions()
        framecount = 0
    }

    /**
     * R_InitBuffer Creates lookup tables that avoid multiplies and other
     * hazzles for getting the framebuffer address of a pixel to draw. MAES:
     * this is "pinned" to screen[0] of a Video Renderer. We will handle this
     * differently elsewhere...
     */
    protected fun InitBuffer(width: Int, height: Int) {
        var i: Int

        // Handle resize,
        // e.g. smaller view windows
        // with border and/or status bar.
        _view.windowx = DOOM.vs.getScreenWidth() - width shr 1

        // Column offset. For windows.
        i = 0
        while (i < width) {
            columnofs[i] = _view.windowx + i
            i++
        }

        // SamE with base row offset.
        if (width == DOOM.vs.getScreenWidth()) {
            _view.windowy = 0
        } else {
            _view.windowy = DOOM.vs.getScreenHeight() - DOOM.statusBar.getHeight() - height shr 1
        }

        // Preclaculate all row offsets.
        i = 0
        while (i < height) {
            ylookup[i] =  /* screens[0] + */(i + _view.windowy) * DOOM.vs.getScreenWidth()
            i++
        }
    }

    /**
     * R_InitTextureMapping Not moved into the TextureManager because it's
     * tighly coupled to the visuals, rather than textures. Perhaps the name is
     * not the most appropriate.
     */
    protected fun InitTextureMapping() {
        var i: Int
        var x: Int
        var t: Int
        val focallength: Int // fixed_t
        val fov: Int = SceneRenderer.FIELDOFVIEW

        // For widescreen displays, increase the FOV so that the middle part of
        // the
        // screen that would be visible on a 4:3 display has the requested FOV.
        /*
         * UNUSED if (wide_centerx != centerx) { // wide_centerx is what centerx
         * would be // if the display was not widescreen fov = (int)
         * (Math.atan((double) centerx Math.tan((double) fov * Math.PI /
         * FINEANGLES) / (double) wide_centerx) FINEANGLES / Math.PI); if (fov >
         * 130 * FINEANGLES / 360) fov = 130 * FINEANGLES / 360; }
         */
        // Use tangent table to generate viewangletox:
        // viewangletox will give the next greatest x
        // after the view angle.
        //
        // Calc focallength
        // so FIELDOFVIEW angles covers vs.getScreenWidth().
        focallength = FixedDiv(
            _view.centerxfrac, Tables.finetangent[Tables.QUARTERMARK + SceneRenderer.FIELDOFVIEW
                    / 2]
        )
        i = 0
        while (i < Tables.FINEANGLES / 2) {
            if (Tables.finetangent[i] > FRACUNIT * 2) {
                t = -1
            } else if (Tables.finetangent[i] < -FRACUNIT * 2) {
                t = _view.width + 1
            } else {
                t = FixedMul(Tables.finetangent[i], focallength)
                t = _view.centerxfrac - t + FRACUNIT - 1 shr FRACBITS
                if (t < -1) {
                    t = -1
                } else if (t > _view.width + 1) {
                    t = _view.width + 1
                }
            }
            viewangletox[i] = t
            i++
        }

        // Scan viewangletox[] to generate xtoviewangle[]:
        // xtoviewangle will give the smallest view angle
        // that maps to x.
        x = 0
        while (x <= _view.width) {
            i = 0
            while (viewangletox[i] > x) {
                i++
            }
            _view.xtoviewangle[x] = Tables.addAngles((i shl Tables.ANGLETOFINESHIFT).toLong(), -Tables.ANG90)
            x++
        }

        // Take out the fencepost cases from viewangletox.
        i = 0
        while (i < Tables.FINEANGLES / 2) {
            t = FixedMul(Tables.finetangent[i], focallength)
            t = _view.centerx - t
            if (viewangletox[i] == -1) {
                viewangletox[i] = 0
            } else if (viewangletox[i] == _view.width + 1) {
                viewangletox[i] = _view.width
            }
            i++
        }
        clipangle = _view.xtoviewangle[0]
        // OPTIMIZE: assign constant for optimization.
        CLIPANGLE2 = 2 * clipangle and Tables.BITS32
    }

    protected fun InitLightTables() {
        var i: Int
        var j: Int
        var startmap: Int
        var scale: Int

        // Calculate the light levels to use
        // for each level / distance combination.
        i = 0
        while (i < colormaps.lightLevels()) {
            startmap =
                (colormaps.lightLevels() - colormaps.lightBright() - i) * 2 * colormaps.numColorMaps() / colormaps.lightLevels()
            j = 0
            while (j < colormaps.maxLightZ()) {

                // CPhipps - use 320 here instead of vs.getScreenWidth(), otherwise hires is
                //           brighter than normal res
                scale =
                    FixedDiv(320 / 2 * FRACUNIT, j + 1 shl colormaps.lightZShift())
                var t: Int
                var level: Int = startmap - colormaps.lightScaleShift()
                    .let { scale = scale shr it; scale } / RendererState.DISTMAP
                if (level < 0) {
                    level = 0
                }
                if (level >= colormaps.numColorMaps()) {
                    level = colormaps.numColorMaps() - 1
                }

                // zlight[i][j] = colormaps + level*256;
                colormaps.zlight[i][j] = colormaps.colormaps[level]
                j++
            }
            i++
        }
    }

    /**
     * number of fixed point digits in
     * filter percent
     */
    lateinit var main_tranmap: ByteArray

    /**
     * A faster implementation of the tranmap calculations. Almost 10x faster
     * than the old one!
     *
     * @param progress
     */
    protected fun R_InitTranMap(progress: Int) {
        val lump = DOOM.wadLoader.CheckNumForName("TRANMAP")
        val ta = System.nanoTime()

        // PRIORITY: a map file has been specified from commandline. Try to read
        // it. If OK, this trumps even those specified in lumps.
        DOOM.cVarManager.with(CommandVariable.TRANMAP, 0) { tranmap: String? ->
            if (C2JUtils.testReadAccess(tranmap)) {
                System.out.printf("Translucency map file %s specified in -tranmap arg. Attempting to use...\n", tranmap)
                main_tranmap = ByteArray(256 * 256) // killough 4/11/98
                val result: Int = MenuMisc.ReadFile(tranmap, main_tranmap)
                if (result > 0) {
                    return@with
                }
                print("...failure.\n")
            }
        }

        // Next, if a tranlucency filter map lump is present, use it
        if (lump != -1) { // Set a pointer to the translucency filter maps.
            print("Translucency map found in lump. Attempting to use...")
            // main_tranmap=new byte[256*256]; // killough 4/11/98
            main_tranmap = DOOM.wadLoader.CacheLumpNumAsRawBytes(lump, Defines.PU_STATIC) // killough
            // 4/11/98
            // Tolerate 64K or more.
            if (main_tranmap.size >= 0x10000) {
                return
            }
            print("...failure.\n") // Not good, try something else.
        }

        // A default map file already exists. Try to read it.
        if (C2JUtils.testReadAccess("tranmap.dat")) {
            print("Translucency map found in default tranmap.dat file. Attempting to use...")
            main_tranmap = ByteArray(256 * 256) // killough 4/11/98
            val result: Int = MenuMisc.ReadFile("tranmap.dat", main_tranmap)
            if (result > 0) {
                return  // Something went wrong, so fuck that.
            }
        }

        // Nothing to do, so we must synthesize it from scratch. And, boy, is it
        // slooow.
        run {
            // Compose a default transparent filter map based on PLAYPAL.
            print("Computing translucency map from scratch...that's gonna be SLOW...")
            val playpal = DOOM.wadLoader.CacheLumpNameAsRawBytes("PLAYPAL", Defines.PU_STATIC)
            main_tranmap = ByteArray(256 * 256) // killough 4/11/98
            val basepal = IntArray(3 * 256)
            val mixedpal = IntArray(3 * 256 * 256)
            main_tranmap = ByteArray(256 * 256)

            // Init array of base colors.
            for (i in 0..255) {
                basepal[3 * i] = 0Xff and playpal[i * 3].toInt()
                basepal[1 + 3 * i] = 0Xff and playpal[1 + i * 3].toInt()
                basepal[2 + 3 * i] = 0Xff and playpal[2 + i * 3].toInt()
            }

            // Init array of mixed colors. These are true RGB.
            // The diagonal of this array will be the original colors.
            var i = 0
            while (i < 256 * 3) {
                var j = 0
                while (j < 256 * 3) {
                    mixColors(basepal, basepal, mixedpal, i, j, j * 256 + i)
                    j += 3
                }
                i += 3
            }

            // Init distance map. Every original palette colour has a
            // certain distance from all the others. The diagonal is zero.
            // The interpretation is that e.g. the mixture of color 2 and 8 will
            // have a RGB value, which is closest to euclidean distance to
            // e.g. original color 9. Therefore we should put "9" in the (2,8)
            // and (8,2) cells of the tranmap.
            val tmpdist = FloatArray(256)
            for (a in 0..255) {
                for (b in a..255) {
                    // We evaluate the mixture of a and b
                    // Construct distance table vs all of the ORIGINAL colors.
                    for (k in 0..255) {
                        tmpdist[k] = colorDistance(mixedpal, basepal, 3 * (a + b * 256), k * 3)
                    }
                    main_tranmap[a shl 8 or b] = findMin(tmpdist).toByte()
                    main_tranmap[b shl 8 or a] = main_tranmap[a shl 8 or b]
                }
            }
            print("...done\n")
            if (MenuMisc.WriteFile(
                    "tranmap.dat", main_tranmap,
                    main_tranmap.size
                )
            ) {
                print("TRANMAP.DAT saved to disk for your convenience! Next time will be faster.\n")
            }
        }
        val b = System.nanoTime()
        System.out.printf("Tranmap %d\n", (b - ta) / 1000000)
    }

    /**
     * Mixes two RGB colors. Nuff said
     */
    protected fun mixColors(
        a: IntArray, b: IntArray, c: IntArray, pa: Int, pb: Int,
        pc: Int
    ) {
        c[pc] = (a[pa] + b[pb]) / 2
        c[pc + 1] = (a[pa + 1] + b[pb + 1]) / 2
        c[pc + 2] = (a[pa + 2] + b[pb + 2]) / 2
    }

    /**
     * Returns the euclidean distance of two RGB colors. Nuff said
     */
    protected fun colorDistance(a: IntArray, b: IntArray, pa: Int, pb: Int): Float {
        return Math.sqrt(((a[pa] - b[pb]) * (a[pa] - b[pb]) + (a[pa + 1] - b[pb + 1]) * (a[pa + 1] - b[pb + 1]) + (a[pa + 2] - b[pb + 2]) * (a[pa + 2] - b[pb + 2])).toDouble())
            .toFloat()
    }

    /**
     * Stuff that is trivially initializable, even with generics,
     * but is only safe to do after all constructors have completed.
     */
    protected open fun completeInit() {
        detailaware.add(MyThings)
    }

    protected fun findMin(a: FloatArray): Int {
        var minindex = 0
        var min = Float.POSITIVE_INFINITY
        for (i in a.indices) {
            if (a[i] < min) {
                min = a[i]
                minindex = i
            }
        }
        return minindex
    }

    /**
     * R_DrawMaskedColumnSinglePost. Used to handle some special cases where
     * cached columns get used as "masked" middle textures. Will be treated as a
     * single-run post of capped length.
     */
    /*
     * protected final void DrawCompositeColumnPost(byte[] column) { int
     * topscreen; int bottomscreen; int basetexturemid; // fixed_t int
     * topdelta=0; // Fixed value int length; basetexturemid = dc_texturemid; //
     * That's true for the whole column. dc_source = column; // for each post...
     * while (topdelta==0) { // calculate unclipped screen coordinates // for
     * post topscreen = sprtopscreen + spryscale * 0; length = column.length;
     * bottomscreen = topscreen + spryscale * length; dc_yl = (topscreen +
     * FRACUNIT - 1) >> FRACBITS; dc_yh = (bottomscreen - 1) >> FRACBITS; if
     * (dc_yh >= mfloorclip[p_mfloorclip + dc_x]) dc_yh =
     * mfloorclip[p_mfloorclip + dc_x] - 1; if (dc_yl <=
     * mceilingclip[p_mceilingclip + dc_x]) dc_yl = mceilingclip[p_mceilingclip
     * + dc_x] + 1; // killough 3/2/98, 3/27/98: Failsafe against
     * overflow/crash: if (dc_yl <= dc_yh && dc_yh < viewheight) { // Set
     * pointer inside column to current post's data // Rremember, it goes
     * {postlen}{postdelta}{pad}[data]{pad} dc_source_ofs = 0; // pointer + 3;
     * dc_texturemid = basetexturemid - (topdelta << FRACBITS); // Drawn by
     * either R_DrawColumn // or (SHADOW) R_DrawFuzzColumn. dc_texheight=0; //
     * Killough try { maskedcolfunc.invoke(); } catch (Exception e){
     * System.err.printf("Error rendering %d %d %d\n", dc_yl,dc_yh,dc_yh-dc_yl);
     * } } topdelta--; } dc_texturemid = basetexturemid; }
     */
    @Throws(IOException::class)
    protected abstract fun InitColormaps()

    // Only used by Fuzz effect
    protected var BLURRY_MAP: BlurryTable? = null

    /**
     * R_InitData Locates all the lumps that will be used by all views Must be
     * called after W_Init.
     */
    fun InitData() {
        try {
            print("\nInit Texture and Flat Manager")
            TexMan = DOOM.textureManager
            print("\nInitTextures")
            TexMan.InitTextures()
            print("\nInitFlats")
            TexMan.InitFlats()
            print("\nInitSprites")
            DOOM.spriteManager.InitSpriteLumps()
            MyThings.cacheSpriteManager(DOOM.spriteManager)
            VIS.cacheSpriteManager(DOOM.spriteManager)
            print("\nInitColormaps\t\t")
            InitColormaps()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    protected var spritememory = 0

    /**
     * To be called right after PrecacheLevel from SetupLevel in LevelLoader.
     * It's an ugly hack, in that it must communicate with the "Game map" class
     * and determine what kinds of monsters are actually in the level and
     * whether it should load their graphics or not. Whenever we implement it,
     * it's going to be ugly and not neatly separated anyway.
     *
     * @return
     */
    override fun PreCacheThinkers() {
        val spritepresent: BooleanArray
        var th: thinker_t
        var sf: spriteframe_t
        var lump: Int
        val sprites = DOOM.spriteManager.getSprites()
        val numsprites = DOOM.spriteManager.getNumSprites()
        spritepresent = BooleanArray(numsprites)
        th = DOOM.actions.getThinkerCap().next!!
        while (th !== DOOM.actions.getThinkerCap()) {
            if (th.thinkerFunction == ActiveStates.P_MobjThinker) {
                spritepresent[(th as mobj_t).mobj_sprite!!.ordinal] = true
            }
            th = th.next!!
        }
        spritememory = 0
        for (i in 0 until numsprites) {
            if (!spritepresent[i]) {
                continue
            }
            for (j in 0 until sprites[i].numframes) {
                sf = sprites[i].spriteframes[j]!!
                for (k in 0..7) {
                    lump = DOOM.spriteManager.getFirstSpriteLump() + sf.lump[k]
                    spritememory += DOOM.wadLoader.GetLumpInfo(lump)!!.size.toInt()
                    DOOM.wadLoader.CacheLumpNum(lump, Defines.PU_CACHE, patch_t::class.java)
                }
            }
        }
    }

    /**
     * R_InitTranslationTables Creates the translation tables to map the green
     * color ramp to gray, brown, red. Assumes a given structure of the PLAYPAL.
     * Could be read from a lump instead.
     */
    protected fun InitTranslationTables() {
        var i: Int
        val TR_COLORS = 28

        // translationtables = Z_Malloc (256*3+255, PU_STATIC, 0);
        // translationtables = (byte *)(( (int)translationtables + 255 )& ~255);
        colormaps.translationtables = Array(TR_COLORS) { ByteArray(256) }
        val translationtables = colormaps.translationtables

        // translate just the 16 green colors
        i = 0
        while (i < 256) {
            translationtables[0][i] = i.toByte()
            if (i >= 0x70 && i <= 0x7f) {
                // Remap green range to other ranges.
                translationtables[1][i] = (0x60 + (i and 0xf)).toByte() // gray
                translationtables[2][i] = (0x40 + (i and 0xf)).toByte() // brown
                translationtables[3][i] = (0x20 + (i and 0xf)).toByte() // red
                translationtables[4][i] = (0x10 + (i and 0xf)).toByte() // pink
                translationtables[5][i] = (0x30 + (i and 0xf)).toByte() // skin
                translationtables[6][i] = (0x50 + (i and 0xf)).toByte() // metal
                translationtables[7][i] = (0x80 + (i and 0xf)).toByte() // copper
                translationtables[8][i] = (0xB0 + (i and 0xf)).toByte() // b.red
                translationtables[9][i] = (0xC0 + (i and 0xf)).toByte() // electric
                // blue
                translationtables[10][i] = (0xD0 + (i and 0xf)).toByte() // guantanamo
                // "Halfhue" colors for which there are only 8 distinct hues
                translationtables[11][i] = (0x90 + (i and 0xf) / 2).toByte() // brown2
                translationtables[12][i] = (0x98 + (i and 0xf) / 2).toByte() // gray2
                translationtables[13][i] = (0xA0 + (i and 0xf) / 2).toByte() // piss
                translationtables[14][i] = (0xA8 + (i and 0xf) / 2).toByte() // gay
                translationtables[15][i] = (0xE0 + (i and 0xf) / 2).toByte() // yellow
                translationtables[16][i] = (0xE8 + (i and 0xf) / 2).toByte() // turd
                translationtables[17][i] = (0xF0 + (i and 0xf) / 2).toByte() // compblue
                translationtables[18][i] = (0xF8 + (i and 0xf) / 2).toByte() // whore
                translationtables[19][i] = (0x05 + (i and 0xf) / 2).toByte() // nigga
                // "Pimped up" colors, using mixed hues.
                translationtables[20][i] = (0x90 + (i and 0xf)).toByte() // soldier
                translationtables[21][i] = (0xA0 + (i and 0xf)).toByte() // drag
                // queen
                translationtables[22][i] = (0xE0 + (i and 0xf)).toByte() // shit &
                // piss
                translationtables[23][i] = (0xF0 + (i and 0xf)).toByte() // raver
                translationtables[24][i] = (0x70 + (0xf - i and 0xf)).toByte() // inv.marine
                translationtables[25][i] = (0xF0 + (0xf - i and 0xf)).toByte() // inv.raver
                translationtables[26][i] = (0xE0 + (0xf - i and 0xf)).toByte() // piss
                // &
                // shit
                translationtables[27][i] = (0xA0 + (i and 0xf)).toByte() // shitty
                // gay
            } else {
                for (j in 1 until TR_COLORS) {
                    // Keep all other colors as is.
                    translationtables[j][i] = i.toByte()
                }
            }
            i++
        }
    }

    // ///////////////// Generic rendering methods /////////////////////
    fun getThings(): IMaskedDrawer<T, V> {
        return MyThings
    }

    /**
     * e6y: this is a precalculated value for more precise flats drawing (see
     * R_MapPlane) "Borrowed" from PrBoom+
     */
    protected var viewfocratio = 0f
    protected var projectiony = 0

    init {

        // These don't change between implementations, yet.
        MyBSP = BSP()
        _view = ViewVars(DOOM.vs)
        seg_vars = SegVars()
        dcvars = ColVars()
        dsvars = SpanVars()
        maskedcvars = ColVars()
        skydcvars = ColVars()
        colfunclow = ColFuncs()
        colfunchi = ColFuncs()
        detailaware = ArrayList()
        colormaps = LightsAndColors(DOOM)
        // It's better to construct this here
        val tm = SimpleTextureManager(DOOM) as TextureManager<T>
        TexMan = tm

        // Visplane variables
        vp_vars = Visplanes(DOOM.vs, _view, TexMan)

        // Set rendering functions only after screen sizes
        // and stuff have been set.
        MyPlanes = Planes(DOOM, this)
        VIS = VisSprites(this)
        MyThings = SimpleThings(DOOM.vs, this)
    }

    // Some more isolation methods....
    override fun getValidCount(): Int {
        return validcount
    }

    override fun increaseValidCount(amount: Int) {
        validcount += amount
    }

    override fun getSetSizeNeeded(): Boolean {
        return setsizeneeded
    }

    override fun getTextureManager(): TextureManager<T> {
        return TexMan
    }

    override fun getPlaneDrawer(): PlaneDrawer<T, V> {
        return MyPlanes
    }

    override fun getView(): ViewVars {
        return _view
    }

    override fun getDSVars(): SpanVars<T, V> {
        return dsvars
    }

    override fun getColorMap(): LightsAndColors<V> {
        return colormaps
    }

    override fun getDoomSystem(): IDoomSystem {
        return DOOM.doomSystem
    }

    override fun getVPVars(): Visplanes {
        return vp_vars
    }

    override fun getSegVars(): SegVars {
        return seg_vars
    }

    override fun getWadLoader(): IWadLoader? {
        return DOOM.wadLoader
    }

    override fun getSpriteManager(): ISpriteManager? {
        return DOOM.spriteManager
    }

    override fun getBSPVars(): BSPVars {
        return MyBSP
    }

    override fun getVisSpriteManager(): IVisSpriteManagement<V> {
        return VIS
    }

    /**
     * Initializes the various drawing functions. They are all "pegged" to the
     * same dcvars/dsvars object. Any initializations of e.g. parallel renderers
     * and their supporting subsystems should occur here.
     */
    protected open fun R_InitDrawingFunctions() {
        setHiColFuns()
        setLowColFuns()
    }

    // //////////////////////////// LIMIT RESETTING //////////////////
    override fun resetLimits() {
        // Call it only at the beginning of new levels.
        VIS.resetLimits()
        MySegs!!.resetLimits()
    }

    /**
     * R_RenderView As you can guess, this renders the player view of a
     * particular player object. In practice, it could render the view of any
     * mobj too, provided you adapt the SetupFrame method (where the viewing
     * variables are set). This is the "vanilla" implementation which just works
     * for most cases.
     */
    override fun RenderPlayerView(player: player_t) {

        // Viewing variables are set according to the player's mobj. Interesting
        // hacks like
        // free cameras or monster views can be done.
        SetupFrame(player)

        // Clear buffers.
        MyBSP.ClearClipSegs()
        seg_vars.ClearDrawSegs()
        vp_vars.ClearPlanes()
        MySegs!!.ClearClips()
        VIS.ClearSprites()

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()

        // The head node is the last node output.
        MyBSP.RenderBSPNode(DOOM.levelLoader.numnodes - 1)

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()

        // FIXME: "Warped floor" fixed, now to fix same-height visplane
        // bleeding.
        MyPlanes.DrawPlanes()

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()
        MyThings.DrawMasked()
        colfunc!!.main = colfunc!!.base

        // Check for new console commands.
        DOOM.gameNetworking.NetUpdate()
    }

    companion object {
        protected const val DEBUG = false
        protected const val DEBUG2 = false
        protected const val RANGECHECK = false
        //
        // R_InitPointToAngle
        //
        /*
     * protected final void InitPointToAngle () { // UNUSED - now getting from
     * tables.c if (false){ int i; long t; float f; // // slope (tangent) to
     * angle lookup // for (i=0 ; i<=SLOPERANGE ; i++) { f = (float) Math.atan(
     * (double)(i/SLOPERANGE )/(3.141592657*2)); t = (long) (0xffffffffL*f);
     * tantoangle[i] = (int) t; } } }
     */
        /**
         * Public, static, stateless version of PointToAngle2. Call this one when
         * "renderless" use of PointToAngle2 is required.
         */
        fun PointToAngle(viewx: Int, viewy: Int, x: Int, y: Int): Long {
            // MAES: note how we don't use &BITS32 here. That is because
            // we know that the maximum possible value of tantoangle is angle
            // This way, we are actually working with vectors emanating
            // from our current position.
            var x = x
            var y = y
            x -= viewx
            y -= viewy
            if (x == 0 && y == 0) {
                return 0
            }
            return if (x >= 0) {
                // x >=0
                if (y >= 0) {
                    // y>= 0
                    if (x > y) {
                        // octant 0
                        Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())].toLong()
                    } else {
                        // octant 1
                        Tables.ANG90 - 1 - Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                    }
                } else {
                    // y<0
                    y = -y
                    if (x > y) {
                        // octant 8
                        (-Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())]).toLong()
                    } else {
                        // octant 7
                        Tables.ANG270 + Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                    }
                }
            } else {
                // x<0
                x = -x
                if (y >= 0) {
                    // y>= 0
                    if (x > y) {
                        // octant 3
                        Tables.ANG180 - 1 - Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())]
                    } else {
                        // octant 2
                        Tables.ANG90 + Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                    }
                } else {
                    // y<0
                    y = -y
                    if (x > y) {
                        // octant 4
                        Tables.ANG180 + Tables.tantoangle[Tables.SlopeDiv(y.toLong(), x.toLong())]
                    } else {
                        // octant 5
                        Tables.ANG270 - 1 - Tables.tantoangle[Tables.SlopeDiv(x.toLong(), y.toLong())]
                    }
                }
            }
            // This is actually unreachable.
            // return 0;
        }

        //
        // R_InitLightTables
        // Only inits the zlight table,
        // because the scalelight table changes with view size.
        //
        protected const val DISTMAP = 2
        protected const val TSC = 12
    }
}