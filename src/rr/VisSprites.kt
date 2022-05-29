package rr

import data.Defines
import data.Limits
import data.Tables
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedDiv
import m.fixed_t.Companion.FixedMul
import p.mobj_t
import utils.C2JUtils
import v.graphics.Lights
import java.util.*

/** Visualized sprite manager. Depends on: SpriteManager, DoomSystem,
 * Colormaps, Current View.
 *
 * @author velktron
 *
 * @param <V>
</V> */
class VisSprites<V>(rendererState: RendererState<*, V>) : IVisSpriteManagement<V> {
    protected val rendererState: RendererState<*, V>
    protected var vissprites: Array<vissprite_t<V?>>
    protected var vissprite_p = 0
    protected var newvissprite = 0

    // UNUSED
    // private final vissprite_t unsorted;
    // private final vissprite_t vsprsortedhead;
    // Cache those you get from the sprite manager
    protected lateinit var spritewidth: IntArray
    protected lateinit var spriteoffset: IntArray
    protected lateinit var spritetopoffset: IntArray

    init {
        val tmp = vissprite_t<V?>()
        vissprites = C2JUtils.createArrayOfObjects(tmp, Limits.MAXVISSPRITES)
        this.rendererState = rendererState
    }

    /**
     * R_AddSprites During BSP traversal, this adds sprites by sector.
     */
    override fun AddSprites(sec: sector_t) {
        if (VisSprites.DEBUG) println("AddSprites")
        var thing: mobj_t?
        val lightnum: Int

        // BSP is traversed by subsector.
        // A sector might have been split into several
        // subsectors during BSP building.
        // Thus we check whether its already added.
        if (sec.validcount == rendererState.getValidCount()) return

        // Well, now it will be done.
        sec.validcount = rendererState.getValidCount()
        lightnum =
            (sec.lightlevel.toInt() shr rendererState.colormaps.lightSegShift()) + rendererState.colormaps.extralight
        if (lightnum < 0)
            rendererState.colormaps.spritelights = rendererState.colormaps.scalelight[0] as Array<V>
        else if (lightnum >= rendererState.colormaps.lightLevels())
            rendererState.colormaps.spritelights = rendererState.colormaps.scalelight[rendererState.colormaps.lightLevels() - 1] as Array<V>
        else
            rendererState.colormaps.spritelights = rendererState.colormaps.scalelight[lightnum] as Array<V>


        // Handle all things in sector.
        thing = sec.thinglist
        while (thing != null) {
            ProjectSprite(thing)
            thing = thing.snext as mobj_t?
        }
    }

    /**
     * R_ProjectSprite Generates a vissprite for a thing if it might be visible.
     *
     * @param thing
     */
    protected fun ProjectSprite(thing: mobj_t) {
        val tr_x: Int
        val tr_y: Int
        var gxt: Int
        var gyt: Int
        var tx: Int
        val tz: Int
        val xscale: Int
        val x1: Int
        val x2: Int
        val sprdef: spritedef_t
        val sprframe: spriteframe_t
        val lump: Int
        val rot: Int
        val flip: Boolean
        var index: Int
        val vis: vissprite_t<V?>
        val ang: Long
        val iscale: Int

        // transform the origin point
        tr_x = thing._x - rendererState._view.x
        tr_y = thing._y - rendererState._view.y
        gxt = FixedMul(tr_x, rendererState._view.cos)
        gyt = -FixedMul(tr_y, rendererState._view.sin)
        tz = gxt - gyt

        // thing is behind view plane?
        if (tz < SceneRenderer.MINZ) return
        /* MAES: so projection/tz gives horizontal scale */xscale =
            FixedDiv(rendererState._view.projection, tz)
        gxt = -FixedMul(tr_x, rendererState._view.sin)
        gyt = FixedMul(tr_y, rendererState._view.cos)
        tx = -(gyt + gxt)

        // too far off the side?
        if (Math.abs(tx) > tz shl 2) return

        // decide which patch to use for sprite relative to player
        if (VisSprites.RANGECHECK) {
            if (thing.mobj_sprite!!.ordinal >= rendererState.DOOM.spriteManager.getNumSprites()) rendererState.DOOM.doomSystem.Error(
                "R_ProjectSprite: invalid sprite number %d ",
                thing.mobj_sprite
            )
        }
        sprdef = rendererState.DOOM.spriteManager.getSprite(thing.mobj_sprite!!.ordinal)
        if (VisSprites.RANGECHECK) {
            if (thing.mobj_frame and Defines.FF_FRAMEMASK >= sprdef.numframes) rendererState.DOOM.doomSystem.Error(
                "R_ProjectSprite: invalid sprite frame %d : %d ",
                thing.mobj_sprite, thing.mobj_frame
            )
        }
        sprframe = sprdef.spriteframes[thing.mobj_frame and Defines.FF_FRAMEMASK]!!
        if (sprframe.rotate != 0) {
            // choose a different rotation based on player view
            ang = rendererState._view.PointToAngle(thing._x, thing._y)
            rot = (ang - thing.angle + Tables.ANG45 * 9 / 2 and Tables.BITS32).toInt() ushr 29
            lump = sprframe.lump[rot]
            flip = (sprframe.flip[rot].toInt() != 0)
        } else {
            // use single rotation for all views
            lump = sprframe.lump[0]
            flip = (sprframe.flip[0].toInt() != 0)
        }

        // calculate edges of the shape
        tx -= spriteoffset[lump]
        x1 = rendererState._view.centerxfrac + FixedMul(tx, xscale) shr FRACBITS

        // off the right side?
        if (x1 > rendererState._view.width) return
        tx += spritewidth[lump]
        x2 =
            (rendererState._view.centerxfrac + FixedMul(tx, xscale) shr FRACBITS) - 1

        // off the left side
        if (x2 < 0) return

        // store information in a vissprite
        vis = NewVisSprite()
        vis.mobjflags = thing.flags
        vis.scale = xscale shl rendererState._view.detailshift
        vis.gx = thing._x
        vis.gy = thing._y
        vis.gz = thing._z
        vis.gzt = thing._z + spritetopoffset[lump]
        vis.texturemid = vis.gzt - rendererState._view.z
        vis.x1 = if (x1 < 0) 0 else x1
        vis.x2 = if (x2 >= rendererState._view.width) rendererState._view.width - 1 else x2
        /*
         * This actually determines the general sprite scale) iscale = 1/xscale,
         * if this was floating point.
         */iscale = FixedDiv(FRACUNIT, xscale)
        if (flip) {
            vis.startfrac = spritewidth[lump] - 1
            vis.xiscale = -iscale
        } else {
            vis.startfrac = 0
            vis.xiscale = iscale
        }
        if (vis.x1 > x1) vis.startfrac += vis.xiscale * (vis.x1 - x1)
        vis.patch = lump

        // get light level
        if (thing.flags and mobj_t.MF_SHADOW != 0) {
            // shadow draw
            vis.colormap = null
        } else if (rendererState.colormaps.fixedcolormap != null) {
            // fixed map
            vis.colormap = rendererState.colormaps.fixedcolormap
            // vis.pcolormap=0;
        } else if (thing.mobj_frame and Defines.FF_FULLBRIGHT != 0) {
            // full bright
            vis.colormap = rendererState.colormaps.colormaps[Lights.COLORMAP_FIXED]
            // vis.pcolormap=0;
        } else {
            // diminished light
            index = xscale shr rendererState.colormaps.lightScaleShift() - rendererState._view.detailshift
            if (index >= rendererState.colormaps.maxLightScale()) index = rendererState.colormaps.maxLightScale() - 1
            vis.colormap = rendererState.colormaps.spritelights[index]
            // vis.pcolormap=index;
        }
    }

    /**
     * R_NewVisSprite Returns either a "new" sprite (actually, reuses a pool),
     * or a special "overflow sprite" which just gets overwritten with bogus
     * data. It's a bit of dumb thing to do, since the overflow sprite is never
     * rendered but we have to copy data over it anyway. Would make more sense
     * to check for it specifically and avoiding copying data, which should be
     * more time consuming. Fixed by making this fully limit-removing.
     *
     * @return
     */
    protected fun NewVisSprite(): vissprite_t<V?> {
        if (vissprite_p == vissprites.size - 1) {
            ResizeSprites()
        }
        // return overflowsprite;
        vissprite_p++
        return vissprites[vissprite_p - 1]
    }

    override fun cacheSpriteManager(SM: ISpriteManager) {
        spritewidth = SM.getSpriteWidth()
        spriteoffset = SM.getSpriteOffset()
        spritetopoffset = SM.getSpriteTopOffset()
    }

    /**
     * R_ClearSprites Called at frame start.
     */
    override fun ClearSprites() {
        // vissprite_p = vissprites;
        vissprite_p = 0
    }

    // UNUSED private final vissprite_t overflowsprite = new vissprite_t();
    protected fun ResizeSprites() {
        vissprites = C2JUtils.resize(vissprites[0], vissprites, vissprites.size * 2) // Bye
        // bye,
        // old
        // vissprites.
    }

    /**
     * R_SortVisSprites UNUSED more efficient Comparable sorting + built-in
     * Arrays.sort function used.
     */
    override fun SortVisSprites() {
        Arrays.sort(vissprites, 0, vissprite_p)

        // Maes: got rid of old vissprite sorting code. Java's is better
        // Hell, almost anything was better than that.
    }

    override fun getNumVisSprites(): Int {
        return vissprite_p
    }

    override fun getVisSprites(): Array<vissprite_t<V?>> {
        return vissprites
    }

    override fun resetLimits() {
        val tmp = C2JUtils.createArrayOfObjects(vissprites[0], Limits.MAXVISSPRITES)
        System.arraycopy(vissprites, 0, tmp, 0, Limits.MAXVISSPRITES)

        // Now, that was quite a haircut!.
        vissprites = tmp
    }

    companion object {
        private const val DEBUG = false
        private const val RANGECHECK = false
    }
}