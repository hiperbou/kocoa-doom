package rr.parallel


import data.Defines
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedMul
import p.mobj_t
import p.pspdef_t
import rr.*
import rr.drawfuns.*
import v.graphics.Lights
import v.scale.VideoScale
import v.tables.BlurryTable
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier

/** A "Masked Worker" draws sprites in a split-screen strategy. Used by
 * ParallelRenderer2. Each Masked Worker is essentially a complete Things
 * drawer, and reuses much of the serial methods.
 *
 * @author velktron
 *
 * @param <T>
 * @param <V>
</V></T> */
abstract class MaskedWorker<T, V>(
    vs: VideoScale,
    R: SceneRenderer<T, V>,
    id: Int,
    numthreads: Int,
    barrier: CyclicBarrier
) : AbstractThings<T, V>(vs, R), Runnable, IDetailAware {
    protected val barrier: CyclicBarrier
    protected val id: Int
    protected val numthreads: Int
    override fun completeColumn() {
        // Does nothing. Shuts up inheritance
    }

    class HiColor(
        vs: VideoScale, R: SceneRenderer<ByteArray?, ShortArray?>, id: Int,
        ylookup: IntArray, columnofs: IntArray, numthreads: Int, screen: ShortArray?,
        barrier: CyclicBarrier, BLURRY_MAP: BlurryTable?
    ) : MaskedWorker<ByteArray?, ShortArray?>(vs, R, id, numthreads, barrier) {
        init {

            // Non-optimized stuff for masked.
            colfuncshi.masked =
                R_DrawColumnBoom.HiColor(vs.getScreenWidth(), vs.getScreenHeight(), ylookup, columnofs, maskedcvars, screen, I)
            colfuncshi.main = colfuncshi.masked
            colfuncshi.base = colfuncshi.main
            colfuncslow.masked =
                R_DrawColumnBoomLow.HiColor(vs.getScreenWidth(), vs.getScreenHeight(), ylookup, columnofs, maskedcvars, screen, I)

            // Fuzzy columns. These are also masked.
            colfuncshi.fuzz = R_DrawFuzzColumn.HiColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I,
                BLURRY_MAP
            )
            colfuncslow.fuzz = R_DrawFuzzColumnLow.HiColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I,
                BLURRY_MAP
            )

            // Translated columns are usually sprites-only.
            colfuncshi.trans = R_DrawTranslatedColumn.HiColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I
            )
            colfuncslow.trans = R_DrawTranslatedColumnLow.HiColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I
            )
            colfuncs = colfuncshi
        }
    }

    class Indexed(
        vs: VideoScale, R: SceneRenderer<ByteArray?, ByteArray?>, id: Int,
        ylookup: IntArray, columnofs: IntArray, numthreads: Int, screen: ByteArray?,
        barrier: CyclicBarrier, BLURRY_MAP: BlurryTable?
    ) : MaskedWorker<ByteArray?, ByteArray?>(vs, R, id, numthreads, barrier) {
        init {
            colfuncshi.masked =
                R_DrawColumnBoom.Indexed(vs.getScreenWidth(), vs.getScreenHeight(), ylookup, columnofs, maskedcvars, screen, I)
            colfuncshi.main = colfuncshi.masked
            colfuncshi.base = colfuncshi.main
            colfuncslow.masked =
                R_DrawColumnBoomLow.Indexed(vs.getScreenWidth(), vs.getScreenHeight(), ylookup, columnofs, maskedcvars, screen, I)

            // Fuzzy columns. These are also masked.
            colfuncshi.fuzz = R_DrawFuzzColumn.Indexed(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I,
                BLURRY_MAP
            )
            colfuncslow.fuzz = R_DrawFuzzColumnLow.Indexed(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I,
                BLURRY_MAP
            )

            // Translated columns are usually sprites-only.
            colfuncshi.trans = R_DrawTranslatedColumn.Indexed(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I
            )
            colfuncslow.trans = R_DrawTranslatedColumnLow.Indexed(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I
            )
            colfuncs = colfuncshi
        }
    }

    class TrueColor(
        vs: VideoScale, R: SceneRenderer<ByteArray?, IntArray?>, id: Int,
        ylookup: IntArray, columnofs: IntArray, numthreads: Int, screen: IntArray?,
        barrier: CyclicBarrier, BLURRY_MAP: BlurryTable?
    ) : MaskedWorker<ByteArray?, IntArray?>(vs, R, id, numthreads, barrier) {
        init {

            // Non-optimized stuff for masked.
            colfuncshi.masked =
                R_DrawColumnBoom.TrueColor(vs.getScreenWidth(), vs.getScreenHeight(), ylookup, columnofs, maskedcvars, screen, I)
            colfuncshi.main = colfuncshi.masked
            colfuncshi.base = colfuncshi.main
            colfuncslow.masked = R_DrawColumnBoomLow.TrueColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I
            )

            // Fuzzy columns. These are also masked.
            colfuncshi.fuzz = R_DrawFuzzColumn.TrueColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I,
                BLURRY_MAP
            )
            colfuncslow.fuzz = R_DrawFuzzColumnLow.TrueColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I,
                BLURRY_MAP
            )

            // Translated columns are usually sprites-only.
            colfuncshi.trans = R_DrawTranslatedColumn.TrueColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I
            )
            colfuncslow.trans = R_DrawTranslatedColumnLow.TrueColor(
                vs.getScreenWidth(),
                vs.getScreenHeight(),
                ylookup,
                columnofs,
                maskedcvars,
                screen,
                I
            )
            colfuncs = colfuncshi
        }
    }

    protected var startx = 0
    protected var endx = 0

    //protected ColVars<T,V> maskedcvars;
    init {
        // Workers have their own set, not a "pegged" one.
        colfuncshi = ColFuncs()
        colfuncslow = ColFuncs()
        maskedcvars = ColVars()
        this.id = id
        this.numthreads = numthreads
        this.barrier = barrier
    }

    /**
     * R_DrawVisSprite mfloorclip and mceilingclip should also be set.
     *
     * Sprites are actually drawn here. Obviously overrides the serial
     * method, and only draws a portion of the sprite.
     *
     *
     */
    override fun DrawVisSprite(vis: vissprite_t<V?>) {
        var column: column_t
        var texturecolumn: Int
        var frac: Int // fixed_t
        val patch: patch_t
        // The sprite may have been partially drawn on another portion of the
        // screen.
        var bias = startx - vis.x1
        if (bias < 0) bias = 0 // nope, it ain't.

        // Trim bounds to zone NOW
        val x1 = Math.max(startx, vis.x1)
        val x2 = Math.min(endx, vis.x2)

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
            val translation = colormaps.getTranslationTable(vis.mobjflags) as T
            maskedcvars.dc_translation = translation
        }
        maskedcvars.dc_iscale = Math.abs(vis.xiscale) shr view.detailshift
        maskedcvars.dc_texturemid = vis.texturemid
        // Add bias to compensate for partially drawn sprite which has not been rejected.
        frac = vis.startfrac + vis.xiscale * bias
        spryscale = vis.scale
        sprtopscreen = view.centeryfrac - FixedMul(maskedcvars.dc_texturemid, spryscale)

        // A texture height of 0 means "not tiling" and holds for
        // all sprite/masked renders.
        maskedcvars.dc_texheight = 0
        maskedcvars.dc_x = x1
        while (maskedcvars.dc_x <= x2) {
            texturecolumn = frac shr FRACBITS
            if (true) {
                if (texturecolumn < 0 || texturecolumn >= patch.width) {
                    I.Error("R_DrawSpriteRange: bad texturecolumn %d vs %d %d %d", texturecolumn, patch.width, x1, x2)
                }
            }
             
            patch.columns[texturecolumn]?.let { column = it; DrawMaskedColumn(it) } //TODO: column was modified if null
                ?: System.err.printf("Null column for texturecolumn %d\n", texturecolumn, x1, x2)
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
    override fun RenderMaskedSegRange(ds: drawseg_t, x1: Int, x2: Int) {

        // Trivial rejection
        var x1 = x1
        var x2 = x2
        if (ds.x1 > endx || ds.x2 < startx) return

        // Trim bounds to zone NOW
        x1 = Math.max(startx, x1)
        x2 = Math.min(endx, x2)
        var index: Int
        var lightnum: Int
        val texnum: Int
        var bias = startx - ds.x1 // Correct for starting outside
        if (bias < 0) {
            bias = 0 // nope, it ain't.
        }
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
            maskedcvars.dc_texturemid -= view.z
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
                val data = TexMan.GetSmpColumn(
                    texnum,
                    maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x].toInt(), id
                )!!
                DrawMaskedColumn(data)
                maskedtexturecol[pmaskedtexturecol + maskedcvars.dc_x] = Short.MAX_VALUE
            }
            spryscale += rw_scalestep
            maskedcvars.dc_x++
        }
    }

    /**
     * R_DrawPSprite
     *
     * Draws a "player sprite" with slighly different rules than normal
     * sprites. This is actually a PITA, at best :-/
     *
     * Also different than normal implementation.
     *
     */
    override fun DrawPSprite(psp: pspdef_t) {
        var tx: Int
        val x1: Int
        val x2: Int
        val sprdef: spritedef_t
        val sprframe: spriteframe_t
        val vis: vissprite_t<V?>
        val lump: Int
        val flip: Boolean

        //
        val state = psp.state!!
        val sprite = state.sprite!!

        // decide which patch to use (in terms of angle?)
        if (MaskedWorker.RANGECHECK) {
            if (sprite.ordinal >= SM.getNumSprites()) {
                I.Error("R_ProjectSprite: invalid sprite number %d ", state.sprite)
            }
        }
        sprdef = SM.getSprite(sprite.ordinal)
        if (MaskedWorker.RANGECHECK) {
            if (state.frame and Defines.FF_FRAMEMASK >= sprdef.numframes) {
                I.Error("R_ProjectSprite: invalid sprite frame %d : %d ", state.sprite, state.frame)
            }
        }
        sprframe = sprdef.spriteframes[state.frame and Defines.FF_FRAMEMASK]!!

        // Base frame for "angle 0" aka viewed from dead-front.
        lump = sprframe.lump[0]
        // Q: where can this be set? A: at sprite loadtime.
        flip = sprframe.flip[0].toInt() != 0

        // calculate edges of the shape. tx is expressed in "view units".
        tx = FixedMul(psp.sx, view.BOBADJUST) - view.WEAPONADJUST
        tx -= spriteoffset[lump]

        // So...centerxfrac is the center of the screen (pixel coords in
        // fixed point).
        x1 = view.centerxfrac + FixedMul(tx, pspritescale) shr FRACBITS

        // off the right side
        if (x1 > endx) return
        tx += spritewidth[lump]
        x2 = (view.centerxfrac + FixedMul(tx, pspritescale) shr FRACBITS) - 1

        // off the left side
        if (x2 < startx) return

        // store information in a vissprite ?
        vis = avis
        vis.mobjflags = 0
        vis.texturemid =
            ((IMaskedDrawer.BASEYCENTER + view.lookdir shl FRACBITS) + FRACUNIT / 2
                    - (psp.sy - spritetopoffset[lump]))
        vis.x1 = if (x1 < startx) startx else x1
        vis.x2 = if (x2 >= endx) endx - 1 else x2
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

        //System.out.printf("Weapon draw from %d to %d\n",vis.x1,vis.x2);
        DrawVisSprite(vis)
    }

    /**
     * R_DrawMasked
     *
     * Sorts and draws vissprites (room for optimization in sorting func.)
     * Draws masked textures. Draws player weapons and overlays (psprites).
     *
     * Sorting function can be swapped for almost anything, and it will work
     * better, in-place and be simpler to draw, too.
     *
     *
     */
    override fun run() {
        // vissprite_t spr;
        var ds: Int
        var dss: drawseg_t

        // Sprites should already be sorted for distance 
        colfunc = colfuncs!!.masked // Sprites use fully-masked capable
        // function.

        // Update view height
        maskedcvars.viewheight = view.height
        maskedcvars.centery = view.centery
        startx = id * view.width / numthreads
        endx = (id + 1) * view.width / numthreads

        // Update thread's own vissprites
        val vissprites = VIS.getVisSprites()
        val numvissprites = VIS.getNumVisSprites()

        //System.out.printf("Sprites to render: %d\n",numvissprites);

        // Try drawing all sprites that are on your side of
        // the screen. Limit by x1 and x2, if you have to.
        for (i in 0 until numvissprites) {
            DrawSprite(vissprites[i])
        }

        //System.out.printf("Segs to render: %d\n",ds_p);

        // render any remaining masked mid textures
        ds = seg_vars.ds_p - 1
        while (ds >= 0) {
            dss = seg_vars.drawsegs[ds]
            if (!(dss.x1 > endx || dss.x2 < startx) && !dss.nullMaskedTextureCol()) RenderMaskedSegRange(
                dss,
                dss.x1,
                dss.x2
            )
            ds--
        }
        // draw the psprites on top of everything
        // but does not draw on side views
        // if (viewangleoffset==0)
        colfunc = colfuncs!!.player
        DrawPlayerSprites()
        colfunc = colfuncs!!.masked
        try {
            barrier.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: BrokenBarrierException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val DEBUG = false
        private const val RANGECHECK = false
    }
}