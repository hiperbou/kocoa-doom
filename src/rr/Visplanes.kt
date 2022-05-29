package rr

import data.Limits
import data.Tables
import utils.C2JUtils
import v.scale.VideoScale
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
import java.nio.ByteBuffer
import m.BBox
import doom.DoomMain

/** Actual visplane data and methods are isolate here.
 * This allows more encapsulation and some neat hacks like sharing
 * visplane data among parallel renderers, without duplicating them.
 */
class Visplanes(protected val vs: VideoScale, protected val view: ViewVars, protected val TexMan: TextureManager<*>) {
    // HACK: An all zeroes array used for fast clearing of certain visplanes.
    lateinit var cachedheight: IntArray
    var BLANKCACHEDHEIGHT: IntArray

    /** To treat as fixed_t  */
    var basexscale = 0
    var baseyscale = 0

    /** To treat at fixed_t  */
    var yslope: IntArray

    // initially.
    var MAXVISPLANES = Limits.MAXVISPLANES
    var MAXOPENINGS: Int

    /** visplane_t*, treat as indexes into visplanes  */
    var lastvisplane = 0
    var floorplane = 0
    var ceilingplane = 0
    var visplanes = arrayOfNulls<visplane_t>(MAXVISPLANES)

    /**
     * openings is supposed to show where "openings" in visplanes start and end
     * e.g. due to sprites, windows etc.
     */
    var openings: ShortArray

    /** Maes: this is supposed to be a pointer inside openings  */
    var lastopening = 0
    protected var skyscale = 0

    init {
        MAXOPENINGS = vs.getScreenWidth() * 64
        openings = ShortArray(MAXOPENINGS)
        BLANKCACHEDHEIGHT = IntArray(vs.getScreenHeight())
        yslope = IntArray(vs.getScreenHeight())
    }

    /**
     * Call only after visplanes have been properly resized for resolution.
     * In case of dynamic resolution changes, the old ones should just be
     * discarded, as they would be nonsensical.
     */
    fun initVisplanes() {
        cachedheight = IntArray(vs.getScreenHeight())
        Arrays.setAll(visplanes) { j: Int -> visplane_t() }
    }

    fun getBaseXScale(): Int {
        return basexscale
    }

    fun getBaseYScale(): Int {
        return baseyscale
    }

    fun getSkyScale(): Int {
        return skyscale
    }

    fun setSkyScale(i: Int) {
        skyscale = i
    }

    fun getLength(): Int {
        return visplanes.size
    }

    /** Return the last of visplanes, allocating a new one if needed  */
    fun allocate(): visplane_t? {
        if (lastvisplane == visplanes.size) {
            //  visplane overflows could occur at this point.
            resizeVisplanes()
        }
        return visplanes[lastvisplane++]
    }

    fun resizeVisplanes() {
        // Bye bye, old visplanes.
        visplanes = C2JUtils.resize(visplanes[0], visplanes, visplanes.size * 2)
    }

    /**
     * R_FindPlane
     *
     * Checks whether a visplane with the specified height, picnum and light
     * level exists among those already created. This looks like a half-assed
     * attempt at reusing already existing visplanes, rather than creating new
     * ones. The tricky part is understanding what happens if one DOESN'T exist.
     * Called only from within R_Subsector (so while we're still trasversing
     * stuff).
     *
     * @param height
     * (fixed_t)
     * @param picnum
     * @param lightlevel
     * @return was visplane_t*, returns index into visplanes[]
     */
    fun FindPlane(height: Int, picnum: Int, lightlevel: Int): Int {
        // System.out.println("\tChecking for visplane merging...");
        var height = height
        var lightlevel = lightlevel
        var check = 0 // visplane_t*
        var chk: visplane_t? = null
        if (picnum == TexMan.getSkyFlatNum()) {
            height = 0 // all skys map together
            lightlevel = 0
        }
        chk = visplanes[0]

        // Find visplane with the desired attributes
        check = 0
        while (check < lastvisplane) {
            chk = visplanes[check]
            if (height == chk!!.height && picnum == chk.picnum && lightlevel == chk.lightlevel) {
                // Found a visplane with the desired specs.
                break
            }
            check++
        }
        if (check < lastvisplane) {
            return check
        }

        // This should return the next available visplane and resize if needed,
        // no need to hack with lastvisplane++
        chk = allocate()
        // Add a visplane
        chk!!.height = height
        chk.picnum = picnum
        chk.lightlevel = lightlevel
        chk.minx = vs.getScreenWidth()
        chk.maxx = -1
        // memset (chk.top,0xff,sizeof(chk.top));
        chk.clearTop()
        return check
    }

    /**
     * R_ClearPlanes At begining of frame.
     *
     */
    fun ClearPlanes() {
        val angle: Int

        /*
         * View planes are cleared at the beginning of every plane, by
         * setting them "just outside" the borders of the screen (-1 and
         * viewheight).
         */

        // Point to #1 in visplane list? OK... ?!
        lastvisplane = 0

        // We point back to the first opening of the list openings[0],
        // again.
        lastopening = 0

        // texture calculation
        System.arraycopy(
            BLANKCACHEDHEIGHT, 0, cachedheight, 0,
            BLANKCACHEDHEIGHT.size
        )

        // left to right mapping
        // FIXME: If viewangle is ever < ANG90, you're fucked. How can this
        // be prevented?
        // Answer: 32-bit unsigned are supposed to roll over. You can & with
        // 0xFFFFFFFFL.
        angle = Tables.toBAMIndex(view.angle - Tables.ANG90)

        // scale will be unit scale at vs.getScreenWidth()/2 distance
        basexscale = FixedDiv(Tables.finecosine[angle], view.centerxfrac)
        baseyscale = -FixedDiv(Tables.finesine[angle], view.centerxfrac)
    }

    /**
     * R_CheckPlane
     *
     * Called from within StoreWallRange
     *
     * Presumably decides if a visplane should be split or not?
     *
     */
    fun CheckPlane(index: Int, start: Int, stop: Int): Int {
        if (Visplanes.DEBUG2) println(
            "Checkplane " + index + " between " + start
                    + " and " + stop
        )

        // Interval ?
        val intrl: Int
        val intrh: Int

        // Union?
        val unionl: Int
        val unionh: Int
        // OK, so we check out ONE particular visplane.
        var pl = visplanes[index]
        if (Visplanes.DEBUG2) println("Checking out plane $pl")
        var x: Int

        // If start is smaller than the plane's min...
        //
        // start minx maxx stop
        // | | | |
        // --------PPPPPPPPPPPPPP-----------
        //
        //
        if (start < pl!!.minx) {
            intrl = pl.minx
            unionl = start
            // Then we will have this:
            //
            // unionl intrl maxx stop
            // | | | |
            // --------PPPPPPPPPPPPPP-----------
            //
        } else {
            unionl = pl.minx
            intrl = start

            // else we will have this:
            //
            // union1 intrl maxx stop
            // | | | |
            // --------PPPPPPPPPPPPPP-----------
            //
            // unionl comes before intrl in any case.
            //
            //
        }

        // Same as before, for for stop and maxx.
        // This time, intrh comes before unionh.
        //
        if (stop > pl.maxx) {
            intrh = pl.maxx
            unionh = stop
        } else {
            unionh = pl.maxx
            intrh = stop
        }

        // An interval is now defined, which is entirely contained in the
        // visplane.
        //

        // If the value FF is NOT stored ANYWWHERE inside it, we bail out
        // early
        x = intrl
        while (x <= intrh) {
            if (pl.getTop(x) != Character.MAX_VALUE) break
            x++
        }

        // This can only occur if the loop above completes,
        // else the visplane we were checking has non-visible/clipped
        // portions within that range: we must split.
        if (x > intrh) {
            // Merge the visplane
            pl.minx = unionl
            pl.maxx = unionh
            // System.out.println("Plane modified as follows "+pl);
            // use the same one
            return index
        }

        // SPLIT: make a new visplane at "last" position, copying materials
        // and light.
        val last = allocate()
        last!!.height = pl.height
        last.picnum = pl.picnum
        last.lightlevel = pl.lightlevel
        pl = last
        pl.minx = start
        pl.maxx = stop

        // memset (pl.top,0xff,sizeof(pl.top));
        pl.clearTop()

        // return pl;

        // System.out.println("New plane created: "+pl);
        return lastvisplane - 1
    } /*
     
       / **
     * A hashtable used to retrieve planes with particular attributes faster
     * -hopefully-. The planes are still stored in the visplane array for
     * convenience, but we can search them in the hashtable too -as a bonus, we
     * can reuse previously created planes that match newer ones-.
     */

    /*
    Hashtable<visplane_t, Integer> planehash = new Hashtable<visplane_t, Integer>(
            128);
    visplane_t check = new visplane_t();
    */
    /*
    protected final int FindPlane2(int height, int picnum, int lightlevel) {
        // System.out.println("\tChecking for visplane merging...");
        // int check=0; // visplane_t*
        visplane_t chk = null;
        Integer checknum;

        if (picnum == TexMan.getSkyFlatNum()) {
            height = 0; // all skys map together
            lightlevel = 0;
        }

        // Try and find this.
        check.lightlevel = lightlevel;
        check.picnum = picnum;
        check.height = height;
        check.updateHashCode();
        checknum = planehash.get(check);

        // Something found, get it.

        if (!(checknum == null)) {

            // Visplane exists and is within those allocated in the current tic.
            if (checknum < lastvisplane) {
                return checknum;
            }

            // Found a visplane, but we can't add anymore.
            // Resize right away. This shouldn't take too long.
            if (lastvisplane == MAXVISPLANES) {
                // I.Error ("R_FindPlane: no more visplanes");
                ResizeVisplanes();
            }
        }

        // We found a visplane (possibly one allocated on a previous tic)
        // but we can't link directly to it, we need to copy its data
        // around.

        checknum = new Integer(Math.max(0, lastvisplane));

        chk = visplanes[checknum];
        // Add a visplane
        lastvisplane++;
        chk.height = height;
        chk.picnum = picnum;
        chk.lightlevel = lightlevel;
        chk.minx = vs.getScreenWidth();
        chk.maxx = -1;
        chk.updateHashCode();
        planehash.put(chk, checknum);
        // memset (chk.top,0xff,sizeof(chk.top));
        chk.clearTop();

        return checknum;
    }
    */
    companion object {
        private const val DEBUG2 = false
    }
}