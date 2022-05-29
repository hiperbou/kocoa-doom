package rr

import defines.slopetype_t
import doom.SourceCode.Compatible
import doom.SourceCode.P_Spec
import p.Interceptable
import p.Resettable
import s.degenmobj_t
import utils.C2JUtils
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.util.*
import data.Tables.BITS32;
import data.Tables.finecosine;
import data.Tables.finesine;
import data.info.mobjinfo;
import data.mobjtype_t;
import doom.SourceCode.angle_t;
import m.fixed_t.Companion.FRACBITS;
import m.fixed_t.Companion.FRACUNIT;
import m.fixed_t.Companion.FixedMul;
import m.fixed_t.Companion.FixedDiv
import p.MapUtils.AproxDistance;
import p.mobj_t;
import utils.C2JUtils.eval;
import doom.player_t;
import doom.weapontype_t;
import m.fixed_t.Companion.MAPFRACUNIT;
import doom.SourceCode
import doom.thinker_t
import m.BBox
import w.DoomIO
import w.IPackableDoomObject
import java.nio.ByteBuffer

/** This is the actual linedef  */
class line_t : Interceptable, IReadableDoomObject, IPackableDoomObject, Resettable {
    /**
     * Vertices, from v1 to v2. NOTE: these are almost never passed as-such, nor
     * linked to Maybe we can get rid of them and only use the value semantics?
     */
    var v1: vertex_t? = null
    var v2: vertex_t? = null

    /** remapped vertex coords, for quick lookup with value semantics  */
    var v1x = 0
    var v1y = 0
    var v2x = 0
    var v2y = 0

    /** (fixed_t) Precalculated v2 - v1 for side checking.  */
    var dx = 0
    var dy = 0

    /** Animation related.  */
    var flags: Short = 0
    var special: Short = 0
    var tag: Short = 0

    /**
     * Visual appearance: SideDefs. sidenum[1] will be 0xFFFF if one sided
     */
    var sidenum: CharArray

    /**
     * Neat. Another bounding box, for the extent of the LineDef. MAES: make
     * this a proper bbox? fixed_t bbox[4];
     */
    var bbox: IntArray

    /** To aid move clipping.  */
    var slopetype: slopetype_t

    /**
     * Front and back sector. Note: redundant? Can be retrieved from SideDefs.
     * MAES: pointers
     */
    var frontsector: sector_t? = null
    var backsector: sector_t? = null
    var frontsectorid = 0
    var backsectorid = 0

    /** if == validcount, already checked  */
    var validcount = 0

    /** thinker_t for reversable actions MAES: (void*)  */
    var specialdata: thinker_t? = null
    var specialdataid = 0
    var soundorg: degenmobj_t? = null

    // From Boom
    var tranlump = 0
    var id = 0

    /** killough 4/17/98: improves searches for tags.  */
    var firsttag = 0
    var nexttag = 0

    /** For Boom stuff, interprets sidenum specially  */
    fun getSpecialSidenum(): Int {
        return sidenum[0].code shl 16 and (0x0000ffff and sidenum[1].code)
    }

    fun assignVertexValues() {
        v1x = v1!!.x
        v1y = v1!!.y
        v2x = v2!!.x
        v2y = v2!!.y
    }

    /**
     * P_PointOnLineSide
     *
     * @param x
     * fixed_t
     * @param y
     * fixed_t
     * @return 0 or 1 (false, true) - (front, back)
     */
    fun PointOnLineSide(x: Int, y: Int): Boolean {
        return if (dx == 0) if (x <= v1x) dy > 0 else dy < 0 else if (dy == 0) if (y <= v1y) dx < 0 else dx > 0 else FixedMul(
            y - v1y,
            dx shr FRACBITS
        ) >=
                FixedMul(dy shr FRACBITS, x - v1x)
        /*
        int dx, dy, left, right;
        if (this.dx == 0) {
            if (x <= this.v1x)
                return this.dy > 0;

            return this.dy < 0;
        }
        if (this.dy == 0) {
            if (y <= this.v1y)
                return this.dx < 0;

            return this.dx > 0;
        }

        dx = (x - this.v1x);
        dy = (y - this.v1y);

        left = FixedMul(this.dy >> FRACBITS, dx);
        right = FixedMul(dy, this.dx >> FRACBITS);

        if (right < left)
            return false; // front side
        return true; // back side*/
    }

    /**
     * P_BoxOnLineSide Considers the line to be infinite Returns side 0 or 1, -1
     * if box crosses the line. Doubles as a convenient check for whether a
     * bounding box crosses a line at all
     *
     * @param tmbox
     * fixed_t[]
     */
    fun BoxOnLineSide(tmbox: IntArray): Int {
        var p1 = false
        var p2 = false
        when (slopetype) {
            slopetype_t.ST_HORIZONTAL -> {
                p1 = tmbox[BBox.BOXTOP] > v1y
                p2 = tmbox[BBox.BOXBOTTOM] > v1y
                if (dx < 0) {
                    p1 = p1 xor true
                    p2 = p2 xor true
                }
            }
            slopetype_t.ST_VERTICAL -> {
                p1 = tmbox[BBox.BOXRIGHT] < v1x
                p2 = tmbox[BBox.BOXLEFT] < v1x
                if (dy < 0) {
                    p1 = p1 xor true
                    p2 = p2 xor true
                }
            }
            slopetype_t.ST_POSITIVE -> {
                // Positive slope, both points on one side.
                p1 = PointOnLineSide(tmbox[BBox.BOXLEFT], tmbox[BBox.BOXTOP])
                p2 = PointOnLineSide(tmbox[BBox.BOXRIGHT], tmbox[BBox.BOXBOTTOM])
            }
            slopetype_t.ST_NEGATIVE -> {
                // Negative slope, both points (mirrored horizontally) on one side.
                p1 = PointOnLineSide(tmbox[BBox.BOXRIGHT], tmbox[BBox.BOXTOP])
                p2 = PointOnLineSide(tmbox[BBox.BOXLEFT], tmbox[BBox.BOXBOTTOM])
            }
        }
        return if (p1 == p2) if (p1) 1 else 0 else -1
        // Any other result means non-inclusive crossing.
    }

    /**
     * Variant of P_BoxOnLineSide. Uses inclusive checks, so that even lines on
     * the border of a box will be considered crossing. This is more useful for
     * building blockmaps.
     *
     * @param tmbox
     * fixed_t[]
     */
    fun BoxOnLineSideInclusive(tmbox: IntArray): Int {
        var p1 = false
        var p2 = false
        when (slopetype) {
            slopetype_t.ST_HORIZONTAL -> {
                p1 = tmbox[BBox.BOXTOP] >= v1y
                p2 = tmbox[BBox.BOXBOTTOM] >= v1y
                if (dx < 0) {
                    p1 = p1 xor true
                    p2 = p2 xor true
                }
            }
            slopetype_t.ST_VERTICAL -> {
                p1 = tmbox[BBox.BOXRIGHT] <= v1x
                p2 = tmbox[BBox.BOXLEFT] <= v1x
                if (dy < 0) {
                    p1 = p1 xor true
                    p2 = p2 xor true
                }
            }
            slopetype_t.ST_POSITIVE -> {
                // Positive slope, both points on one side.
                p1 = PointOnLineSide(tmbox[BBox.BOXLEFT], tmbox[BBox.BOXTOP])
                p2 = PointOnLineSide(tmbox[BBox.BOXRIGHT], tmbox[BBox.BOXBOTTOM])
            }
            slopetype_t.ST_NEGATIVE -> {
                // Negative slope, both points (mirrored horizontally) on one side.
                p1 = PointOnLineSide(tmbox[BBox.BOXRIGHT], tmbox[BBox.BOXTOP])
                p2 = PointOnLineSide(tmbox[BBox.BOXLEFT], tmbox[BBox.BOXBOTTOM])
            }
        }
        return if (p1 == p2) if (p1) 1 else 0 else -1
        // Any other result means non-inclusive crossing.
    }

    /**
     * getNextSector() Return sector_t * of sector next to current. NULL if not
     * two-sided line
     */
    @Compatible("getNextSector(line_t line, sector_t sec)")
    @P_Spec.C(P_Spec.getNextSector)
    fun getNextSector(sec: sector_t): sector_t? {
        if (!C2JUtils.eval(flags.toInt() and line_t.ML_TWOSIDED)) {
            return null
        }
        return if (frontsector === sec) {
            backsector
        } else frontsector
    }

    override fun toString(): String {
        return String.format(
            "Line %d Flags: %x Special %d Tag: %d ", id, flags,
            special, tag
        )
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {

        // For histerical reasons, these are the only parts of line_t that
        // are archived in vanilla savegames. Go figure.
        flags = DoomIO.readLEShort(f)
        special = DoomIO.readLEShort(f)
        tag = DoomIO.readLEShort(f)
    }

    override fun pack(buffer: ByteBuffer) {
        buffer.putShort(flags)
        buffer.putShort(special)
        buffer.putShort(tag)
        // buffer.putShort((short) 0XDEAD);
        // buffer.putShort((short) 0XBABE);
        // buffer.putShort((short) 0XBEEF);
    }

    override fun reset() {
        v2 = null
        v1 = v2
        v2y = 0
        v2x = v2y
        v1y = v2x
        v1x = v1y
        dy = 0
        dx = dy
        tag = 0
        special = tag
        flags = special
        C2JUtils.memset(sidenum, 0.toChar(), sidenum.size)
        Arrays.fill(bbox, 0)
        slopetype = slopetype_t.ST_HORIZONTAL
        backsector = null
        frontsector = backsector
        backsectorid = 0
        frontsectorid = backsectorid
        validcount = 0
        specialdata = null
        specialdataid = 0
        soundorg = null
        tranlump = 0
    }

    init {
        sidenum = CharArray(2)
        bbox = IntArray(4)
        slopetype = slopetype_t.ST_HORIZONTAL
    }

    companion object {
        const val NO_INDEX = 0xFFFF.toChar()

        /**
         * LUT, motion clipping, walls/grid element // // LineDef attributes. // / **
         * Solid, is an obstacle.
         */
        const val ML_BLOCKING = 1

        /** Blocks monsters only.  */
        const val ML_BLOCKMONSTERS = 2

        /** Backside will not be present at all if not two sided.  */
        const val ML_TWOSIDED = 4
        // If a texture is pegged, the texture will have
        // the end exposed to air held constant at the
        // top or bottom of the texture (stairs or pulled
        // down things) and will move with a height change
        // of one of the neighbor sectors.
        // Unpegged textures allways have the first row of
        // the texture at the top pixel of the line for both
        // top and bottom textures (use next to windows).
        /** upper texture unpegged  */
        const val ML_DONTPEGTOP = 8

        /** lower texture unpegged  */
        const val ML_DONTPEGBOTTOM = 16

        /** In AutoMap: don't map as two sided: IT'S A SECRET!  */
        const val ML_SECRET = 32

        /** Sound rendering: don't let sound cross two of these.  */
        const val ML_SOUNDBLOCK = 64

        /** Don't draw on the automap at all.  */
        const val ML_DONTDRAW = 128

        /** Set if already seen, thus drawn in automap.  */
        const val ML_MAPPED = 256
    }
}