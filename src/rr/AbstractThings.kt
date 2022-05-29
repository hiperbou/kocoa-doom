package rr

import data.Defines
import doom.player_t
import i.IDoomSystem
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedMul
import p.mobj_t
import p.pspdef_t
import rr.drawfuns.ColFuncs
import rr.drawfuns.ColVars
import rr.drawfuns.ColumnFunction
import v.graphics.Lights
import v.scale.VideoScale
import v.tables.LightsAndColors
import w.IWadLoader

/**
 * Refresh of things, i.e. objects represented by sprites. This abstract
 * class is the base for all implementations, and contains the gory clipping
 * and priority stuff. It can terminate by drawing directly, or by buffering
 * into a pipeline for parallelized drawing.
 *
 * It need to be aware of almost everything in the renderer, which means that
 * it's a PITA to keep "disembodied". Then again, this probably means it's more
 * extensible...
 *
 *
 *
 */
abstract class AbstractThings<T, V>(vs: VideoScale, R: SceneRenderer<T, V>) : IMaskedDrawer<T, V> {
    protected lateinit var maskedtexturecol: ShortArray
    protected var pmaskedtexturecol = 0

    // Cache those you get from the sprite manager
    protected lateinit var spritewidth: IntArray
    protected lateinit var spriteoffset: IntArray
    protected lateinit var spritetopoffset: IntArray

    /** fixed_t  */
    protected var pspritescale = 0
    protected var pspriteiscale = 0
    protected var pspritexscale = 0
    protected var pspriteyscale = 0
    protected var skyscale = 0

    // Used for masked segs
    protected var rw_scalestep = 0
    protected var spryscale = 0
    protected var sprtopscreen = 0
    protected lateinit var mfloorclip: ShortArray
    protected var p_mfloorclip = 0
    protected lateinit var mceilingclip: ShortArray
    protected var p_mceilingclip = 0
    protected var frontsector: sector_t? = null
    protected var backsector: sector_t? = null

    // This must be "pegged" to the one used by the default renderer.
    protected var maskedcvars: ColVars<T, V>
    protected var colfunc: ColumnFunction<T, V>? = null
    protected var colfuncs: ColFuncs<T, V>? = null
    protected var colfuncshi: ColFuncs<T, V>
    protected var colfuncslow: ColFuncs<T, V>
    protected val vs: VideoScale
    protected val colormaps: LightsAndColors<V>
    protected val view: ViewVars
    protected val seg_vars: SegVars
    protected val TexMan: TextureManager<T>
    protected val I: IDoomSystem
    protected val SM: ISpriteManager
    protected val MyBSP: BSPVars
    protected val VIS: IVisSpriteManagement<V>
    protected val W: IWadLoader
    protected val avis: vissprite_t<V?>
    override fun cacheSpriteManager(SM: ISpriteManager) {
        spritewidth = SM.getSpriteWidth()
        spriteoffset = SM.getSpriteOffset()
        spritetopoffset = SM.getSpriteTopOffset()
    }

    /**
     * R_DrawVisSprite mfloorclip and mceilingclip should also be set.
     * Sprites are actually drawn here. MAES: Optimized. No longer needed to
     * pass x1 and x2 parameters (useless) +2 fps on nuts.wad timedemo.
     */
    protected open fun DrawVisSprite(vis: vissprite_t<V?>) {
        var column: column_t
        var texturecolumn: Int
        var frac: Int // fixed_t
        val patch: patch_t

        // At this point, the view angle (and patch) has already been
        // chosen. Go back.
        patch = W.CachePatchNum(vis.patch + SM.getFirstSpriteLump())
        maskedcvars.dc_colormap = vis.colormap
        // colfunc=glasscolfunc;
        if (maskedcvars.dc_colormap == null) {
            // NULL colormap = shadow draw
            colfunc = colfuncs!!.fuzz
        } else if (vis.mobjflags and mobj_t.MF_TRANSLATION != 0) {
            colfunc = colfuncs!!.trans
            maskedcvars.dc_translation = colormaps.getTranslationTable(vis.mobjflags) as T
        }
        maskedcvars.dc_iscale = Math.abs(vis.xiscale) shr view.detailshift
        maskedcvars.dc_texturemid = vis.texturemid
        frac = vis.startfrac
        spryscale = vis.scale
        sprtopscreen = (view.centeryfrac
                - FixedMul(maskedcvars.dc_texturemid, spryscale))

        // A texture height of 0 means "not tiling" and holds for
        // all sprite/masked renders.
        maskedcvars.dc_texheight = 0
        maskedcvars.dc_x = vis.x1
        while (maskedcvars.dc_x <= vis.x2) {
            texturecolumn = frac shr FRACBITS
            if (AbstractThings.RANGECHECK) {
                if (texturecolumn < 0 || texturecolumn >= patch.width) I.Error("R_DrawSpriteRange: bad texturecolumn")
            }
            column = patch.columns[texturecolumn]!!
            DrawMaskedColumn(column)
            maskedcvars.dc_x++
            frac += vis.xiscale
        }
        colfunc = colfuncs!!.masked
    }

    /**
     * R_RenderMaskedSegRange
     *
     * @param ds
     * @param x1
     * @param x2
     */
    protected open fun RenderMaskedSegRange(ds: drawseg_t, x1: Int, x2: Int) {
        var index: Int
        var lightnum: Int
        val texnum: Int

        // System.out.printf("RenderMaskedSegRange from %d to %d\n",x1,x2);

        // Calculate light table.
        // Use different light tables
        // for horizontal / vertical / diagonal. Diagonal?
        // OPTIMIZE: get rid of LIGHTSEGSHIFT globally
        MyBSP.curline = ds.curline
        val curline = MyBSP.curline!!
        frontsector = curline.frontsector
        val frontsector = frontsector!!
        backsector = curline.backsector
        val backsector = backsector!!
        texnum = TexMan.getTextureTranslation(curline.sidedef!!.midtexture.toInt())
        // System.out.print(" for texture "+textures[texnum].name+"\n:");
        lightnum = (frontsector.lightlevel.toInt() shr colormaps.lightSegShift()) + colormaps.extralight
        if (curline.v1y == curline.v2y) lightnum-- else if (curline.v1x == curline.v2x) lightnum++

        // Killough code.
        colormaps.walllights =
            (if (lightnum >= colormaps.lightLevels()) colormaps.scalelight[colormaps.lightLevels() - 1] else if (lightnum < 0) colormaps.scalelight[0] else colormaps.scalelight[lightnum]) as Array<V>

        // Get the list
        maskedtexturecol = ds.getMaskedTextureColList()!!
        // And this is the pointer.
        pmaskedtexturecol = ds.getMaskedTextureColPointer()
        rw_scalestep = ds.scalestep
        spryscale = ds.scale1 + (x1 - ds.x1) * rw_scalestep

        // HACK to get "pointers" inside clipping lists
        mfloorclip = ds.getSprBottomClipList()!!
        p_mfloorclip = ds.getSprBottomClipPointer()
        mceilingclip = ds.getSprTopClipList()!!
        p_mceilingclip = ds.getSprTopClipPointer()
        // find positioning
        if (curline.linedef!!.flags.toInt() and line_t.ML_DONTPEGBOTTOM != 0) {
            maskedcvars.dc_texturemid =
                if (frontsector.floorheight > backsector.floorheight) frontsector.floorheight else backsector.floorheight
            maskedcvars.dc_texturemid = (maskedcvars.dc_texturemid + TexMan.getTextureheight(texnum)
                    - view.z)
        } else {
            maskedcvars.dc_texturemid =
                if (frontsector.ceilingheight < backsector.ceilingheight) frontsector.ceilingheight else backsector.ceilingheight
            maskedcvars.dc_texturemid = maskedcvars.dc_texturemid - view.z
        }
        maskedcvars.dc_texturemid += curline.sidedef!!.rowoffset
        if (colormaps.fixedcolormap != null) maskedcvars.dc_colormap = colormaps.fixedcolormap

        // Texture height must be set at this point. This will trigger
        // tiling. For sprites, it should be set to 0.
        maskedcvars.dc_texheight = TexMan.getTextureheight(texnum) shr FRACBITS

        // draw the columns
        maskedcvars.dc_x = x1
        while (maskedcvars.dc_x <= x2) {

            // calculate lighting
            if (maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x] != Short.MAX_VALUE) {
                if (colormaps.fixedcolormap == null) {
                    index = spryscale ushr colormaps.lightScaleShift()
                    if (index >= colormaps.maxLightScale()) index = colormaps.maxLightScale() - 1
                    maskedcvars.dc_colormap = colormaps.walllights[index]
                }
                sprtopscreen = (view.centeryfrac
                        - FixedMul(maskedcvars.dc_texturemid, spryscale))
                maskedcvars.dc_iscale = (0xffffffffL / spryscale).toInt()

                // draw the texture
                val data = TexMan.GetColumnStruct(
                    texnum, maskedtexturecol[pmaskedtexturecol
                            + maskedcvars.dc_x].toInt()
                )!!// -3);
                DrawMaskedColumn(data)
                maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x] = Short.MAX_VALUE
            }
            spryscale += rw_scalestep
            maskedcvars.dc_x++
        }
    }

    /**
     * R_DrawPSprite Draws a "player sprite" with slighly different rules
     * than normal sprites. This is actually a PITA, at best :-/
     */
    protected open fun DrawPSprite(psp: pspdef_t) {
        var tx: Int
        val x1: Int
        val x2: Int
        val sprdef: spritedef_t
        val sprframe: spriteframe_t
        val vis: vissprite_t<V?>
        val lump: Int
        val flip: Boolean

        val state = psp.state!!
        val sprite = state.sprite!!
        // decide which patch to use (in terms of angle?)
        if (AbstractThings.RANGECHECK) {
            if (sprite.ordinal >= SM.getNumSprites()) I.Error(
                "R_ProjectSprite: invalid sprite number %d ",
                state.sprite
            )
        }
        sprdef = SM.getSprite(sprite.ordinal)
        if (AbstractThings.RANGECHECK) {
            if (state.frame and Defines.FF_FRAMEMASK >= sprdef.numframes) I.Error(
                "R_ProjectSprite: invalid sprite frame %d : %d ",
                sprite, state.frame
            )
        }
        sprframe = sprdef.spriteframes[state.frame and Defines.FF_FRAMEMASK]!!

        // Base frame for "angle 0" aka viewed from dead-front.
        lump = sprframe.lump[0]
        // Q: where can this be set? A: at sprite loadtime.
        flip = (sprframe.flip[0].toInt() != 0)

        // calculate edges of the shape. tx is expressed in "view units".
        tx = (FixedMul(psp.sx, view.BOBADJUST) - view.WEAPONADJUST)
        tx -= spriteoffset[lump]

        // So...centerxfrac is the center of the screen (pixel coords in
        // fixed point).
        x1 = view.centerxfrac + FixedMul(tx, pspritescale) shr FRACBITS

        // off the right side
        if (x1 > view.width) return
        tx += spritewidth[lump]
        x2 = (view.centerxfrac + FixedMul(tx, pspritescale) shr FRACBITS) - 1

        // off the left side
        if (x2 < 0) return

        // store information in a vissprite ?
        vis = avis
        vis.mobjflags = 0
        vis.texturemid =
            ((IMaskedDrawer.BASEYCENTER + view.lookdir shl FRACBITS) + FRACUNIT / 2
                    - (psp.sy - spritetopoffset[lump]))
        vis.x1 = if (x1 < 0) 0 else x1
        vis.x2 = if (x2 >= view.width) view.width - 1 else x2
        vis.scale = pspritescale shl view.detailshift
        if (flip) {
            vis.xiscale = -pspriteiscale
            vis.startfrac = spritewidth[lump] - 1
        } else {
            vis.xiscale = pspriteiscale
            vis.startfrac = 0
        }
        if (vis.x1 > x1) vis.startfrac += vis.xiscale * (vis.x1 - x1)
        vis.patch = lump
        if (view.player!!.powers[Defines.pw_invisibility] > 4 * 32
            || view.player!!.powers[Defines.pw_invisibility] and 8 != 0
        ) {
            // shadow draw
            vis.colormap = null
        } else if (colormaps.fixedcolormap != null) {
            // fixed color
            vis.colormap = colormaps.fixedcolormap
            // vis.pcolormap=0;
        } else if (state.frame and Defines.FF_FULLBRIGHT != 0) {
            // full bright
            vis.colormap = colormaps.colormaps[Lights.COLORMAP_FIXED]
            // vis.pcolormap=0;
        } else {
            // local light
            vis.colormap = colormaps.spritelights[colormaps.maxLightScale() - 1]
        }

        // System.out.println("Weapon draw "+vis);
        DrawVisSprite(vis)
    }

    protected var PSpriteSY = intArrayOf(
        0,  // staff
        5 * FRACUNIT,  // goldwand
        15 * FRACUNIT,  // crossbow
        15 * FRACUNIT,  // blaster
        15 * FRACUNIT,  // skullrod
        15 * FRACUNIT,  // phoenix rod
        15 * FRACUNIT,  // mace
        15 * FRACUNIT,  // gauntlets
        15 * FRACUNIT // beak
    )

    /**
     * R_DrawPlayerSprites This is where stuff like guns is drawn...right?
     */
    protected fun DrawPlayerSprites() {
        var i: Int
        val lightnum: Int
        var psp: pspdef_t

        // get light level
        lightnum = ((view.player!!.mo!!.subsector!!.sector!!.lightlevel.toInt() shr colormaps.lightSegShift())
                + colormaps.extralight)

        if (lightnum < 0)
            colormaps.spritelights = colormaps.scalelight[0] as Array<V>
        else if (lightnum >= colormaps.lightLevels())
            colormaps.spritelights = colormaps.scalelight[colormaps.lightLevels() - 1] as Array<V>
        else
            colormaps.spritelights = colormaps.scalelight[lightnum] as Array<V>

        // clip to screen bounds
        mfloorclip = view.screenheightarray
        p_mfloorclip = 0
        mceilingclip = view.negonearray
        p_mceilingclip = 0

        // add all active psprites
        // MAES 25/5/2011 Fixed another stupid bug that prevented
        // PSP from actually being updated. This in turn uncovered
        // other bugs in the way psp and state were treated, and the way
        // flash states were set. It should be OK now.
        i = 0
        while (i < player_t.NUMPSPRITES) {
            psp = view.player!!.psprites[i]
            if (psp.state != null && psp.state!!.id != 0) {
                DrawPSprite(psp)
            }
            i++
        }
    }

    // MAES: Scale to vs.getScreenWidth()
    protected var clipbot: ShortArray
    protected var cliptop: ShortArray

    init {
        colfuncshi = R.getColFuncsHi()
        colfuncslow = R.getColFuncsLow()
        colormaps = R.getColorMap()
        view = R.getView()
        seg_vars = R.getSegVars()
        TexMan = R.getTextureManager()
        I = R.getDoomSystem()
        SM = R.getSpriteManager()!!
        MyBSP = R.getBSPVars()
        VIS = R.getVisSpriteManager()
        W = R.getWadLoader()!!
        avis = vissprite_t()
        maskedcvars = R.getMaskedDCVars()
        this.vs = vs
        clipbot = ShortArray(vs.getScreenWidth())
        cliptop = ShortArray(vs.getScreenWidth())
    }

    /**
     * R_DrawSprite
     */
    protected fun DrawSprite(spr: vissprite_t<V?>) {
        var ds: Int
        var dss: drawseg_t
        var x: Int
        var r1: Int
        var r2: Int
        var scale: Int // fixed
        var lowscale: Int // fixed
        var silhouette: Int
        x = spr.x1
        while (x <= spr.x2) {
            cliptop[x] = -2
            clipbot[x] = cliptop[x]
            x++
        }

        // Scan drawsegs from end to start for obscuring segs.
        // The first drawseg that has a greater scale
        // is the clip seg.
        ds = seg_vars.ds_p - 1
        while (ds >= 0) {

            // determine if the drawseg obscures the sprite
            // System.out.println("Drawseg "+ds+"of "+(ds_p-1));
            dss = seg_vars.drawsegs[ds]
            if (dss.x1 > spr.x2 || dss.x2 < spr.x1 || dss.silhouette == 0 && dss
                    .nullMaskedTextureCol()
            ) {
                // does not cover sprite
                ds--
                continue
            }
            r1 = if (dss.x1 < spr.x1) spr.x1 else dss.x1
            r2 = if (dss.x2 > spr.x2) spr.x2 else dss.x2
            if (dss.scale1 > dss.scale2) {
                lowscale = dss.scale2
                scale = dss.scale1
            } else {
                lowscale = dss.scale1
                scale = dss.scale2
            }

            if (scale < spr.scale
                || (lowscale < spr.scale && (dss.curline!!.PointOnSegSide(spr.gx, spr.gy) == 0)))
            {
                // masked mid texture?
                if (!dss.nullMaskedTextureCol()) RenderMaskedSegRange(dss, r1, r2)
                ds--
                // seg is behind sprite
                continue
            }

            // clip this piece of the sprite
            silhouette = dss.silhouette
            if (spr.gz >= dss.bsilheight) silhouette = silhouette and Defines.SIL_BOTTOM.inv()
            if (spr.gzt <= dss.tsilheight) silhouette = silhouette and Defines.SIL_TOP.inv()

            // BOTTOM clipping
            if (silhouette == 1) {
                // bottom sil
                x = r1
                while (x <= r2) {
                    if (clipbot[x].toInt() == -2) clipbot[x] = dss.getSprBottomClip(x)
                    x++
                }
            } else if (silhouette == 2) {
                // top sil
                x = r1
                while (x <= r2) {
                    if (cliptop[x].toInt() == -2) cliptop[x] = dss.getSprTopClip(x)
                    x++
                }
            } else if (silhouette == 3) {
                // both
                x = r1
                while (x <= r2) {
                    if (clipbot[x].toInt() == -2) clipbot[x] = dss.getSprBottomClip(x)
                    if (cliptop[x].toInt() == -2) cliptop[x] = dss.getSprTopClip(x)
                    x++
                }
            }
            ds--
        }

        // all clipping has been performed, so draw the sprite

        // check for unclipped columns
        x = spr.x1
        while (x <= spr.x2) {
            if (clipbot[x].toInt() == -2) clipbot[x] = view.height.toShort()
            // ?? What's this bullshit?
            if (cliptop[x].toInt() == -2) cliptop[x] = -1
            x++
        }
        mfloorclip = clipbot
        p_mfloorclip = 0
        mceilingclip = cliptop
        p_mceilingclip = 0
        DrawVisSprite(spr)
    }

    /**
     * R_DrawMasked Sorts and draws vissprites (room for optimization in
     * sorting func.) Draws masked textures. Draws player weapons and
     * overlays (psprites). Sorting function can be swapped for almost
     * anything, and it will work better, in-place and be simpler to draw,
     * too.
     */
    override fun DrawMasked() {
        // vissprite_t spr;
        var ds: Int
        var dss: drawseg_t

        // Well, it sorts visspite objects.
        // It actually IS faster to sort with comparators, but you need to
        // go into NUTS.WAD-like wads.
        // numbers. The built-in sort if about as good as it gets. In fact,
        // it's hardly slower
        // to draw sprites without sorting them when using the built-in
        // modified mergesort, while
        // the original algorithm is so dreadful it actually does slow
        // things down.
        VIS.SortVisSprites()

        // If you are feeling adventurous, try these ones. They *might*
        // perform
        // better in very extreme situations where all sprites are always on
        // one side
        // of your view, but I hardly see any benefits in that. They are
        // both
        // much better than the original anyway.

        // combSort(vissprites,vissprite_p);
        // shellsort(vissprites,vissprite_p);

        // pQuickSprite.sort(vissprites);

        // The original sort. It's incredibly bad on so many levels (uses a
        // separate
        // linked list for the sorted sequence, which is pointless since the
        // vissprite_t
        // array is gonna be changed all over in the next frame anyway, it's
        // not like
        // it helps preseving or anything. It does work in Java too, but I'd
        // say to Keep Away. No srsly.

        /*
         * SortVisSprites (); // Sprite "0" not visible? / *if (vissprite_p >
         * 0) { // draw all vissprites back to front for (spr =
         * vsprsortedhead.next ; spr != vsprsortedhead ; spr=spr.next) {
         * DrawSprite (spr); } }
         */

        // After using in-place sorts, sprites can be drawn as simply as
        // that.
        colfunc = colfuncs!!.masked // Sprites use fully-masked capable
        // function.
        val vissprites = VIS.getVisSprites()
        val numvissprites = VIS.getNumVisSprites()
        for (i in 0 until numvissprites) {
            DrawSprite(vissprites[i])
        }

        // render any remaining masked mid textures
        ds = seg_vars.ds_p - 1
        while (ds >= 0) {
            dss = seg_vars.drawsegs[ds]
            if (!dss.nullMaskedTextureCol()) RenderMaskedSegRange(dss, dss.x1, dss.x2)
            ds--
        }
        // draw the psprites on top of everything
        // but does not draw on side views
        // if (viewangleoffset==0)
        colfunc = colfuncs!!.player
        DrawPlayerSprites()
        colfunc = colfuncs!!.masked
    }
    /**
     * R_DrawMaskedColumn Used for sprites and masked mid textures. Masked
     * means: partly transparent, i.e. stored in posts/runs of opaque
     * pixels. NOTE: this version accepts raw bytes, in case you know what
     * you're doing.
     */
    /* protected final void DrawMaskedColumn(T column) {
        int topscreen;
        int bottomscreen;
        int basetexturemid; // fixed_t
        int topdelta;
        int length;

        basetexturemid = maskedcvars.dc_texturemid;
        // That's true for the whole column.
        maskedcvars.dc_source = (T) column;
        int pointer = 0;

        // for each post...
        while ((topdelta = 0xFF & column[pointer]) != 0xFF) {
            // calculate unclipped screen coordinates
            // for post
            topscreen = sprtopscreen + spryscale * topdelta;
            length = 0xff & column[pointer + 1];
            bottomscreen = topscreen + spryscale * length;

            maskedcvars.dc_yl = (topscreen + FRACUNIT - 1) >> FRACBITS;
            maskedcvars.dc_yh = (bottomscreen - 1) >> FRACBITS;

            if (maskedcvars.dc_yh >= mfloorclip[p_mfloorclip
                    + maskedcvars.dc_x])
                maskedcvars.dc_yh =
                    mfloorclip[p_mfloorclip + maskedcvars.dc_x] - 1;

            if (maskedcvars.dc_yl <= mceilingclip[p_mceilingclip
                    + maskedcvars.dc_x])
                maskedcvars.dc_yl =
                    mceilingclip[p_mceilingclip + maskedcvars.dc_x] + 1;

            // killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
            if (maskedcvars.dc_yl <= maskedcvars.dc_yh
                    && maskedcvars.dc_yh < view.height) {
                // Set pointer inside column to current post's data
                // Rremember, it goes {postlen}{postdelta}{pad}[data]{pad}
                maskedcvars.dc_source_ofs = pointer + 3;
                maskedcvars.dc_texturemid =
                    basetexturemid - (topdelta << FRACBITS);

                // Drawn by either R_DrawColumn
                // or (SHADOW) R_DrawFuzzColumn.
                maskedcvars.dc_texheight = 0; // Killough

                completeColumn();
            }
            pointer += length + 4;
        }

        maskedcvars.dc_texturemid = basetexturemid;
    }
    */
    /**
     * R_DrawMaskedColumn Used for sprites and masked mid textures. Masked
     * means: partly transparent, i.e. stored in posts/runs of opaque
     * pixels. FIXME: while it does work with "raw columns", if the initial
     * post is drawn outside of the screen the rest appear screwed up.
     * SOLUTION: use the version taking raw byte[] arguments.
     */
    protected fun DrawMaskedColumn(column: column_t) {
        var topscreen: Int
        var bottomscreen: Int
        val basetexturemid: Int // fixed_t
        basetexturemid = maskedcvars.dc_texturemid
        // That's true for the whole column.
        maskedcvars.dc_source = column.data as T
        // dc_source_ofs=0;

        // for each post...
        for (i in 0 until column.posts) {
            maskedcvars.dc_source_ofs = column.postofs[i]
            // calculate unclipped screen coordinates
            // for post
            topscreen = sprtopscreen + spryscale * column.postdeltas[i]
            bottomscreen = topscreen + spryscale * column.postlen[i]
            maskedcvars.dc_yl = topscreen + FRACUNIT - 1 shr FRACBITS
            maskedcvars.dc_yh = bottomscreen - 1 shr FRACBITS
            if (maskedcvars.dc_yh >= mfloorclip[p_mfloorclip
                        + maskedcvars.dc_x]
            ) maskedcvars.dc_yh = mfloorclip[p_mfloorclip + maskedcvars.dc_x] - 1
            if (maskedcvars.dc_yl <= mceilingclip[p_mceilingclip
                        + maskedcvars.dc_x]
            ) maskedcvars.dc_yl = mceilingclip[p_mceilingclip + maskedcvars.dc_x] + 1

            // killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
            if (maskedcvars.dc_yl <= maskedcvars.dc_yh
                && maskedcvars.dc_yh < maskedcvars.viewheight
            ) {

                // Set pointer inside column to current post's data
                // Remember, it goes {postlen}{postdelta}{pad}[data]{pad}
                maskedcvars.dc_texturemid =
                    basetexturemid - (column.postdeltas[i].toInt() shl FRACBITS)

                // Drawn by either R_DrawColumn or (SHADOW)
                // R_DrawFuzzColumn.
                // MAES: when something goes bad here, it means that the
                // following:
                //
                // fracstep = dc_iscale;
                // frac = dc_texturemid + (dc_yl - centery) * fracstep;
                //
                // results in a negative initial frac number.

                // Drawn by either R_DrawColumn
                //  or (SHADOW) R_DrawFuzzColumn.

                // FUN FACT: this was missing and fucked my shit up.
                maskedcvars.dc_texheight = 0 // Killough
                completeColumn()
            }
        }
        maskedcvars.dc_texturemid = basetexturemid
    }

    /*
     * R_DrawMaskedColumn
     * Used for sprites and masked mid textures.
     * Masked means: partly transparent, i.e. stored
     *  in posts/runs of opaque pixels.
     *  
     *  NOTE: this version accepts raw bytes, in case you  know what you're doing.
     *  NOTE: this is a legacy function. Do not reactivate unless
     *  REALLY needed.
     *
     */
    /*
    protected final  void DrawMaskedColumn (byte[] column)
    {
        int topscreen;
        int bottomscreen;
        int basetexturemid; // fixed_t
        int topdelta;
        int length;
        
        basetexturemid = dc_texturemid;
        // That's true for the whole column.
        dc_source = column;
        int pointer=0;
        
        // for each post...
        while((topdelta=0xFF&column[pointer])!=0xFF)
        {
        // calculate unclipped screen coordinates
        //  for post
        topscreen = sprtopscreen + spryscale*topdelta;
        length=0xff&column[pointer+1];
        bottomscreen = topscreen + spryscale*length;

        dc_yl = (topscreen+FRACUNIT-1)>>FRACBITS;
        dc_yh = (bottomscreen-1)>>FRACBITS;
            
        if (dc_yh >= mfloorclip[p_mfloorclip+dc_x])
            dc_yh = mfloorclip[p_mfloorclip+dc_x]-1;
        
        if (dc_yl <= mceilingclip[p_mceilingclip+dc_x])
            dc_yl = mceilingclip[p_mceilingclip+dc_x]+1;

        // killough 3/2/98, 3/27/98: Failsafe against overflow/crash:
        if (dc_yl <= dc_yh && dc_yh < viewheight)
        {
            // Set pointer inside column to current post's data
            // Rremember, it goes {postlen}{postdelta}{pad}[data]{pad} 
            dc_source_ofs = pointer+3;
            dc_texturemid = basetexturemid - (topdelta<<FRACBITS);

            // Drawn by either R_DrawColumn
            //  or (SHADOW) R_DrawFuzzColumn.
            dc_texheight=0; // Killough
                
            maskedcolfunc.invoke();
        }
        pointer+=length + 4;
        }
        
        dc_texturemid = basetexturemid;
    }
      */
    override fun setPspriteIscale(i: Int) {
        pspriteiscale = i
    }

    override fun setPspriteScale(i: Int) {
        pspritescale = i
    }

    override fun setDetail(detailshift: Int) {
        when (detailshift) {
            IDetailAware.HIGH_DETAIL -> colfuncs = colfuncshi
            IDetailAware.LOW_DETAIL -> colfuncs = colfuncslow
        }
    }

    companion object {
        private const val RANGECHECK = false
    }
}