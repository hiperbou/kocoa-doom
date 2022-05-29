package rr

import data.Limits
import doom.SourceCode
import doom.SourceCode.P_Spec
import m.IRandom
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import mochadoom.Loggers
import p.Resettable
import p.ThinkerList
import p.mobj_t
import s.degenmobj_t
import utils.C2JUtils
import w.DoomIO
import w.IPackableDoomObject
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.logging.Level

/**
 * The SECTORS record, at runtime. Stores things/mobjs. Can be
 * archived/unarchived during savegames.
 *
 * @author Maes
 */
class sector_t : IReadableDoomObject, IPackableDoomObject, Resettable {
    var TL: ThinkerList? = null
    var RND: IRandom? = null

    /** (fixed_t)  */
    var floorheight = 0
    var ceilingheight = 0
    var floorpic: Short = 0
    var ceilingpic: Short = 0
    var lightlevel: Short = 0
    var special: Short = 0
    var tag: Short = 0

    /** 0 = untraversed, 1,2 = sndlines -1  */
    var soundtraversed = 0

    /** thing that made a sound (or null) (MAES: single pointer)  */
    var soundtarget: mobj_t? = null

    /** mapblock bounding box for height changes  */
    var blockbox: IntArray

    /**
     * origin for any sounds played by the sector. Used to be degenmobj_t, but
     * that's really a futile distinction.
     */
    var soundorg: degenmobj_t? = null

    /** if == validcount, already checked  */
    var validcount = 0

    /** list of mobjs in sector (MAES: it's used as a linked list)  */
    var thinglist: mobj_t? = null

    /**
     * thinker_t for reversable actions. This actually was a void*, and in
     * practice it could store doors, plats, floors and ceiling objects.
     */
    var specialdata: SectorAction? = null
    var linecount = 0

    // struct line_s** lines; // [linecount] size
    // MAES: make this line_t[] for now?
    var lines: Array<line_t?>? = null

    /** Use for internal identification  */
    var id: Int

    /** killough 1/30/98: improves searches for tags.  */
    var nexttag = 0
    var firsttag = 0

    init {
        blockbox = IntArray(4)
        id = -1
    }

    override fun toString(): String {
        return String.format(
            "Sector: %d %x %x %d %d %d %d %d", id, floorheight,
            ceilingheight, floorpic, ceilingpic, lightlevel, special,  // needed?
            tag
        )
    }

    //
    // P_FindLowestFloorSurrounding()
    // FIND LOWEST FLOOR HEIGHT IN SURROUNDING SECTORS
    //
    fun FindLowestFloorSurrounding(): Int {
        var i: Int
        var check: line_t
        var other: sector_t?
        var floor = floorheight
        i = 0
        while (i < linecount) {
            check = lines!![i]!!
            other = check.getNextSector(this)
            if (other == null) {
                i++
                continue
            }
            if (other.floorheight < floor) floor = other.floorheight
            i++
        }
        return floor
    }

    /**
     * P_FindHighestFloorSurrounding() FIND HIGHEST FLOOR HEIGHT IN SURROUNDING
     * SECTORS Compatibility problem: apparently this is hardcoded for vanilla
     * compatibility (instead of Integer.MIN_VALUE), but it will cause some
     * "semi-Boom" maps not to work, since it won't be able to lower stuff below
     * -500 units. The correct fix here would be to allow for -compatlevel style
     * options. Maybe later.
     *
     * @param sec
     */
    fun FindHighestFloorSurrounding(): Int {
        var i: Int
        var check: line_t
        var other: sector_t?
        var floor: Int = -500 * FRACUNIT
        i = 0
        while (i < linecount) {
            check = lines!![i]!!
            other = check.getNextSector(this)

            // The compiler nagged about this being unreachable, with
            // some older 1.6 JDKs, but that's obviously not true.
            if (other == null) {
                i++
                continue
            }
            if (other.floorheight > floor) floor = other.floorheight
            i++
        }
        return floor
    }

    /**
     * P_FindNextHighestFloor FIND NEXT HIGHEST FLOOR IN SURROUNDING SECTORS
     * Note: this should be doable w/o a fixed array.
     *
     * @param sec
     * @param currentheight
     * @return fixed
     */
    fun FindNextHighestFloor(currentheight: Int): Int {
        var i: Int
        var h: Int
        var min: Int
        var check: line_t
        var other: sector_t?
        val heightlist = IntArray(Limits.MAX_ADJOINING_SECTORS)
        i = 0
        h = 0
        while (i < linecount) {
            check = lines!![i]!!
            other = check.getNextSector(this)
            if (other == null) {
                i++
                continue
            }
            if (other.floorheight > currentheight) heightlist[h++] = other.floorheight

            // Check for overflow. Exit.
            if (h >= Limits.MAX_ADJOINING_SECTORS) {
                Loggers.getLogger(sector_t::class.java.name).log(
                    Level.WARNING,
                    "Sector with more than 20 adjoining sectors\n"
                )
                break
            }
            i++
        }

        // Find lowest height in list
        if (h == 0) return currentheight
        min = heightlist[0]

        // Range checking?
        i = 1
        while (i < h) {
            if (heightlist[i] < min) min = heightlist[i]
            i++
        }
        return min
    }

    //
    // FIND LOWEST CEILING IN THE SURROUNDING SECTORS
    //
    @SourceCode.Exact
    @P_Spec.C(P_Spec.P_FindLowestCeilingSurrounding)
    @SourceCode.fixed_t
    fun FindLowestCeilingSurrounding(): Int {
        var check: line_t
        var other: sector_t?
        var height = Limits.MAXINT
        for (i in 0 until linecount) {
            check = lines!![i]!!
            getNextSector@ run {
                other = check.getNextSector(this)
            }
            if (other == null) {
                continue
            }
            if (other!!.ceilingheight < height) {
                height = other!!.ceilingheight
            }
        }
        return height
    }

    //
    // FIND HIGHEST CEILING IN THE SURROUNDING SECTORS
    //
    fun FindHighestCeilingSurrounding(): Int {
        var i: Int
        var check: line_t
        var other: sector_t?
        var height = 0
        i = 0
        while (i < linecount) {
            check = lines!![i]!!
            other = check.getNextSector(this)
            if (other == null) {
                i++
                continue
            }
            if (other.ceilingheight > height) height = other.ceilingheight
            i++
        }
        return height
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {

        // ACHTUNG: the only situation where we'd
        // like to read memory-format sector_t's is from
        // savegames, and in vanilla savegames, not all info
        // is saved (or read) from disk.
        floorheight = DoomIO.readLEShort(f).toInt() shl FRACBITS
        ceilingheight = DoomIO.readLEShort(f).toInt() shl FRACBITS
        // MAES: it may be necessary to apply a hack in order to
        // read vanilla savegames.
        floorpic = DoomIO.readLEShort(f)
        ceilingpic = DoomIO.readLEShort(f)
        // f.skipBytes(4);
        lightlevel = DoomIO.readLEShort(f)
        special = DoomIO.readLEShort(f) // needed?
        tag = DoomIO.readLEShort(f) // needed?
    }

    override fun pack(b: ByteBuffer) {
        b.putShort((floorheight shr FRACBITS).toShort())
        b.putShort((ceilingheight shr FRACBITS).toShort())
        // MAES: it may be necessary to apply a hack in order to
        // read vanilla savegames.
        b.putShort(floorpic)
        b.putShort(ceilingpic)
        // f.skipBytes(4);
        b.putShort(lightlevel)
        b.putShort(special)
        b.putShort(tag)
    }

    override fun reset() {
        floorheight = 0
        ceilingheight = 0
        floorpic = 0
        ceilingpic = 0
        lightlevel = 0
        special = 0
        tag = 0
        soundtraversed = 0
        soundtarget = null
        C2JUtils.memset(blockbox, 0, blockbox.size)
        soundorg = null
        validcount = 0
        thinglist = null
        specialdata = null
        linecount = 0
        lines = null
        id = -1
    }
}