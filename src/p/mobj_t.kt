package p


import data.*
import data.info.states
import data.sounds.sfxenum_t
import defines.statenum_t
import doom.*
import rr.subsector_t
import s.ISoundOrigin
import utils.C2JUtils
import utils.C2JUtils.eval
import w.IPackableDoomObject
import w.IReadableDoomObject
import w.IWritableDoomObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 *
 * NOTES: mobj_t
 *
 * mobj_ts are used to tell the refresh where to draw an image, tell the world
 * simulation when objects are contacted, and tell the sound driver how to
 * position a sound.
 *
 * The refresh uses the next and prev links to follow lists of things in sectors
 * as they are being drawn. The sprite, frame, and angle elements determine
 * which patch_t is used to draw the sprite if it is visible. The sprite and
 * frame values are allmost allways set from state_t structures. The
 * statescr.exe utility generates the states.h and states.c files that contain
 * the sprite/frame numbers from the statescr.txt source file. The xyz origin
 * point represents a point at the bottom middle of the sprite (between the feet
 * of a biped). This is the default origin position for patch_ts grabbed with
 * lumpy.exe. A walking creature will have its z equal to the floor it is
 * standing on.
 *
 * The sound code uses the x,y, and subsector fields to do stereo positioning of
 * any sound effited by the mobj_t.
 *
 * The play simulation uses the blocklinks, x,y,z, radius, height to determine
 * when mobj_ts are touching each other, touching lines in the map, or hit by
 * trace lines (gunshots, lines of sight, etc). The mobj_t->flags element has
 * various bit flags used by the simulation.
 *
 * Every mobj_t is linked into a single sector based on its origin coordinates.
 * The subsector_t is found with R_PointInSubsector(x,y), and the sector_t can
 * be found with subsector->sector. The sector links are only used by the
 * rendering code, the play simulation does not care about them at all.
 *
 * Any mobj_t that needs to be acted upon by something else in the play world
 * (block movement, be shot, etc) will also need to be linked into the blockmap.
 * If the thing has the MF_NOBLOCK flag set, it will not use the block links. It
 * can still interact with other things, but only as the instigator (missiles
 * will run into other things, but nothing can run into a missile). Each block
 * in the grid is 128*128 units, and knows about every line_t that it contains a
 * piece of, and every interactable mobj_t that has its origin contained.
 *
 * A valid mobj_t is a mobj_t that has the proper subsector_t filled in for its
 * xy coordinates and is linked into the sector from which the subsector was
 * made, or has the MF_NOSECTOR flag set (the subsector_t needs to be valid even
 * if MF_NOSECTOR is set), and is linked into a blockmap block or has the
 * MF_NOBLOCKMAP flag set. Links should only be modified by the
 * P_[Un]SetThingPosition() functions. Do not change the MF_NO? flags while a
 * thing is valid.
 *
 * Any questions?
 *
 * @author admin
 */
class mobj_t : thinker_t, ISoundOrigin, Interceptable, IWritableDoomObject, IPackableDoomObject, IReadableDoomObject {
    val A: ActionFunctions?

    private constructor() {
        spawnpoint = mapthing_t()
        A = null
    }

    private constructor(A: ActionFunctions) {
        spawnpoint = mapthing_t()
        this.A = A
        // A mobj_t is ALSO a thinker, as it always contains the struct.
        // Don't fall for C's trickery ;-)
        // this.thinker=new thinker_t();
    }
    /* List: thinker links. */ // public thinker_t thinker;
    /** Info for drawing: position.  */
    @SourceCode.fixed_t
    var _x = 0

    @SourceCode.fixed_t
    var _y = 0

    @SourceCode.fixed_t
    var _z = 0

    /** More list: links in sector (if needed)  */
    var snext: thinker_t? = null
    var sprev: thinker_t? = null
    // More drawing info: to determine current sprite.
    /**
     * orientation. This needs to be long or else certain checks will fail...but
     * I need to see it working in order to confirm
     */
    var angle: Long = 0

    /** used to find patch_t and flip value  */
    var mobj_sprite: spritenum_t? = null

    /** might be ORed with FF_FULLBRIGHT  */
    var mobj_frame = 0

    /** Interaction info, by BLOCKMAP. Links in blocks (if needed).  */
    var bnext: thinker_t? = null
    var bprev: thinker_t? = null

    /** MAES: was actually a pointer to a struct subsector_s  */
    var subsector: subsector_t? = null

    /** The closest interval over all contacted Sectors.  */
    @SourceCode.fixed_t
    var floorz = 0

    @SourceCode.fixed_t
    var ceilingz = 0

    /** For movement checking.  */
    @SourceCode.fixed_t
    var radius = 0

    @SourceCode.fixed_t
    var height = 0

    /** Momentums, used to update position.  */
    @SourceCode.fixed_t
    var momx = 0

    @SourceCode.fixed_t
    var momy = 0

    @SourceCode.fixed_t
    var momz = 0

    /** If == validcount, already checked.  */
    var validcount = 0
    var type: mobjtype_t? = null

    // MAES: was a pointer
    var info // &mobjinfo[mobj.type]
            : mobjinfo_t? = null
    var mobj_tics // state tic counter
            : Long = 0

    // MAES: was a pointer
    var mobj_state: state_t? = null
    var flags: Int = 0 //TODO: was Long!?
    var health = 0

    /** Movement direction, movement generation (zig-zagging).  */
    var movedir // 0-7
            = 0
    var movecount // when 0, select a new dir
            = 0

    /**
     * Thing being chased/attacked (or NULL), also the originator for missiles.
     * MAES: was a pointer
     */
    var target: mobj_t? = null
    var p_target // for savegames
            = 0

    /**
     * Reaction time: if non 0, don't attack yet. Used by player to freeze a bit
     * after teleporting.
     */
    var reactiontime = 0

    /**
     * If >0, the target will be chased no matter what (even if shot)
     */
    var threshold = 0

    /**
     * Additional info record for player avatars only. Only valid if type ==
     * MT_PLAYER struct player_s* player;
     */
    var player: player_t? = null

    /** Player number last looked for.  */
    var lastlook = 0

    /** For nightmare respawn.  */
    var spawnpoint // struct
            : mapthing_t

    /** Thing being chased/attacked for tracers.  */
    var tracer // MAES: was a pointer
            : mobj_t? = null
    /*
	 * The following methods were for the most part "contextless" and
	 * instance-specific, so they were implemented here rather that being
	 * scattered all over the package.
	 */
    /**
     * P_SetMobjState Returns true if the mobj is still present.
     */
    /*
	 * The following methods were for the most part "contextless" and
	 * instance-specific, so they were implemented here rather that being
	 * scattered all over the package.
	 */
    open fun SetMobjState(state: statenum_t?): Boolean {
        var state = state
        var st: state_t
        do {
            if (state === statenum_t.S_NULL) {
                mobj_state = null
                // MAES/_D_: uncommented this as it should work by now (?).
                A!!.RemoveMobj(this)
                return false
            }
            st = states.get(state!!.ordinal)
            mobj_state = st
            mobj_tics = st.tics.toLong()
            mobj_sprite = st.sprite
            mobj_frame = st.frame

            // Modified handling.
            // Call action functions when the state is set
            // TODO: try find a bug
            if (st.action!!.activeState is MobjActiveStates) {
                (st.action!!.activeState as MobjActiveStates).accept(A!!, MobjConsumer(this))
            }
            state = st.nextstate
        } while (!eval(mobj_tics))
        return true
    }

    /**
     * P_ZMovement
     */
    fun ZMovement() {
        @SourceCode.fixed_t val dist: Int
        @SourceCode.fixed_t val delta: Int

        // check for smooth step up
        if (player != null && _z < floorz) {
            player!!.viewheight -= floorz - _z
            player!!.deltaviewheight = Defines.VIEWHEIGHT - player!!.viewheight shr 3
        }

        // adjust height
        _z += momz
        if (flags and mobj_t.MF_FLOAT != 0 && target != null) {
            // float down towards target if too close
            if (flags and mobj_t.MF_SKULLFLY == 0 && flags and mobj_t.MF_INFLOAT == 0) {
                dist = MapUtils.AproxDistance(_x - target!!._x, _y - target!!._y)
                delta = target!!._z + (height shr 1) - _z
                if (delta < 0 && dist < -(delta * 3)) _z -= Defines.FLOATSPEED else if (delta > 0 && dist < delta * 3) _z += Defines.FLOATSPEED
            }
        }

        // clip movement
        if (_z <= floorz) {
            // hit the floor

            // Note (id):
            // somebody left this after the setting momz to 0,
            // kinda useless there.
            if (flags and mobj_t.MF_SKULLFLY != 0) {
                // the skull slammed into something
                momz = -momz
            }
            if (momz < 0) {
                if (player != null && momz < -Defines.GRAVITY * 8) {
                    // Squat down.
                    // Decrease viewheight for a moment
                    // after hitting the ground (hard),
                    // and utter appropriate sound.
                    player!!.deltaviewheight = momz shr 3
                    A!!.DOOM.doomSound.StartSound(this, sfxenum_t.sfx_oof)
                }
                momz = 0
            }
            _z = floorz
            if (flags and mobj_t.MF_MISSILE != 0 && flags and mobj_t.MF_NOCLIP == 0) {
                A!!.ExplodeMissile(this)
                return
            }
        } else if (flags and mobj_t.MF_NOGRAVITY == 0) {
            if (momz == 0) momz = -Defines.GRAVITY * 2 else momz -= Defines.GRAVITY
        }
        if (_z + height > ceilingz) {
            // hit the ceiling
            if (momz > 0) momz = 0
            run { _z = ceilingz - height }
            if (flags and mobj_t.MF_SKULLFLY != 0) { // the skull slammed into
                // something
                momz = -momz
            }
            if (flags and mobj_t.MF_MISSILE != 0 && flags and mobj_t.MF_NOCLIP == 0) {
                A!!.ExplodeMissile(this)
            }
        }
    }

    var eflags // DOOM LEGACY
            = 0

    // Fields used only during DSG unmarshalling
    var stateid = 0
    var playerid = 0
    var p_tracer = 0

    /** Unique thing id, used during sync debugging  */
    var thingnum = 0
    fun clear() {
        mobj_t.fastclear.rewind()
        try {
            unpack(mobj_t.fastclear)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    // _D_: to permit this object to save/load
    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        // More efficient, avoids duplicating code and
        // handles little endian better.
        mobj_t.buffer.position(0)
        mobj_t.buffer.order(ByteOrder.LITTLE_ENDIAN)
        f.read(mobj_t.buffer.array())
        unpack(mobj_t.buffer)
    }

    @Throws(IOException::class)
    override fun write(f: DataOutputStream) {

        // More efficient, avoids duplicating code and
        // handles little endian better.
        mobj_t.buffer.position(0)
        mobj_t.buffer.order(ByteOrder.LITTLE_ENDIAN)
        pack(mobj_t.buffer)
        f.write(mobj_t.buffer.array())
    }

    @Throws(IOException::class)
    override fun pack(b: ByteBuffer) {
        b.order(ByteOrder.LITTLE_ENDIAN)
        super.pack(b) // Pack the head thinker.
        b.putInt(_x)
        b.putInt(_y)
        b.putInt(_z)
        b.putInt(C2JUtils.pointer(snext))
        b.putInt(C2JUtils.pointer(sprev))
        b.putInt((angle and Tables.BITS32).toInt())
        b.putInt(mobj_sprite!!.ordinal)
        b.putInt(mobj_frame)
        b.putInt(C2JUtils.pointer(bnext))
        b.putInt(C2JUtils.pointer(bprev))
        b.putInt(C2JUtils.pointer(subsector))
        b.putInt(floorz)
        b.putInt(ceilingz)
        b.putInt(radius)
        b.putInt(height)
        b.putInt(momx)
        b.putInt(momy)
        b.putInt(momz)
        b.putInt(validcount)
        b.putInt(type!!.ordinal)
        b.putInt(C2JUtils.pointer(info)) // TODO: mobjinfo
        b.putInt((mobj_tics and Tables.BITS32).toInt())
        b.putInt(mobj_state!!.id) // TODO: state OK?
        b.putInt(flags.toInt()) // truncate
        b.putInt(health)
        b.putInt(movedir)
        b.putInt(movecount)
        b.putInt(C2JUtils.pointer(target)) // TODO: p_target?
        b.putInt(reactiontime)
        b.putInt(threshold)
        // Check for player.
        if (player != null) {
            b.putInt(1 + player!!.identify())

            // System.out.printf("Mobj with hashcode %d is player %d",pointer(this),1+this.player.identify());
        } else b.putInt(0)
        b.putInt(lastlook)
        spawnpoint.pack(b)
        b.putInt(C2JUtils.pointer(tracer)) // tracer pointer stored.
    }

    @Throws(IOException::class)
    override fun unpack(b: ByteBuffer) {
        b.order(ByteOrder.LITTLE_ENDIAN)
        super.unpack(b) // 12 Read the head thinker.
        _x = b.int // 16
        _y = b.int // 20
        _z = b.int // 24
        b.long // TODO: snext, sprev. When are those set? 32
        angle = Tables.BITS32 and b.int.toLong() // 36
        mobj_sprite = spritenum_t.values()[b.int] // 40
        mobj_frame = b.int // 44
        b.long // TODO: bnext, bprev. When are those set? 52
        b.int // TODO: subsector 56
        floorz = b.int // 60
        ceilingz = b.int // 64
        radius = b.int // 68
        height = b.int // 72
        momx = b.int // 76
        momy = b.int // 80
        momz = b.int // 84
        validcount = b.int // 88
        type = mobjtype_t.values()[b.int] // 92
        b.int // TODO: mobjinfo (deduced from type) //96
        mobj_tics = Tables.BITS32 and b.int.toLong() // 100
        // System.out.println("State"+f.readLEInt());
        stateid = b.int // TODO: state OK?
        flags = (b.int.toLong() and Tables.BITS32).toInt() // Only 32-bit flags can be restored //TODO: hacky thing 
        health = b.int
        movedir = b.int
        movecount = b.int
        p_target = b.int
        reactiontime = b.int
        threshold = b.int
        playerid = b.int // TODO: player. Non null should mean that
        // it IS a player.
        lastlook = b.int
        spawnpoint.unpack(b)
        p_tracer = b.int // TODO: tracer
    }

    // TODO: a linked list of sectors where this object appears
    // public msecnode_t touching_sectorlist;
    // Sound origin stuff
    override fun getX(): Int {
        return _x
    }

    override fun getY(): Int {
        return _y
    }

    override fun getZ(): Int {
        return _z
    }

    override fun toString(): String {
        return String.format("%s %d", type, thingnum)
    }

    companion object {
        fun createOn(context: DoomMain<*, *>): mobj_t {
            return if (C2JUtils.eval(context.actions)) {
                mobj_t(context.actions)
            } else mobj_t()
        }

        // // MF_ flags for mobjs.
        // Call P_SpecialThing when touched.
        const val MF_SPECIAL = 1

        // Blocks.
        const val MF_SOLID = 2

        // Can be hit.
        const val MF_SHOOTABLE = 4

        // Don't use the sector links (invisible but touchable).
        const val MF_NOSECTOR = 8

        // Don't use the blocklinks (inert but displayable)
        const val MF_NOBLOCKMAP = 16

        // Not to be activated by sound, deaf monster.
        const val MF_AMBUSH = 32

        // Will try to attack right back.
        const val MF_JUSTHIT = 64

        // Will take at least one step before attacking.
        const val MF_JUSTATTACKED = 128

        // On level spawning (initial position),
        // hang from ceiling instead of stand on floor.
        const val MF_SPAWNCEILING = 256

        // Don't apply gravity (every tic),
        // that is, object will float, keeping current height
        // or changing it actively.
        const val MF_NOGRAVITY = 512

        // Movement flags.
        // This allows jumps from high places.
        const val MF_DROPOFF = 0x400

        // For players, will pick up items.
        const val MF_PICKUP = 0x800

        // Player cheat. ???
        const val MF_NOCLIP = 0x1000

        // Player: keep info about sliding along walls.
        const val MF_SLIDE = 0x2000

        // Allow moves to any height, no gravity.
        // For active floaters, e.g. cacodemons, pain elementals.
        const val MF_FLOAT = 0x4000

        // Don't cross lines
        // ??? or look at heights on teleport.
        const val MF_TELEPORT = 0x8000

        // Don't hit same species, explode on block.
        // Player missiles as well as fireballs of various kinds.
        const val MF_MISSILE = 0x10000

        // Dropped by a demon, not level spawned.
        // E.g. ammo clips dropped by dying former humans.
        const val MF_DROPPED = 0x20000

        // Use fuzzy draw (shadow demons or spectres),
        // temporary player invisibility powerup.
        const val MF_SHADOW = 0x40000

        // Flag: don't bleed when shot (use puff),
        // barrels and shootable furniture shall not bleed.
        const val MF_NOBLOOD = 0x80000

        // Don't stop moving halfway off a step,
        // that is, have dead bodies slide down all the way.
        const val MF_CORPSE = 0x100000

        // Floating to a height for a move, ???
        // don't auto float to target's height.
        const val MF_INFLOAT = 0x200000

        // On kill, count this enemy object
        // towards intermission kill total.
        // Happy gathering.
        const val MF_COUNTKILL = 0x400000

        // On picking up, count this item object
        // towards intermission item total.
        const val MF_COUNTITEM = 0x800000

        // Special handling: skull in flight.
        // Neither a cacodemon nor a missile.
        const val MF_SKULLFLY = 0x1000000

        // Don't spawn this object
        // in death match mode (e.g. key cards).
        const val MF_NOTDMATCH = 0x2000000

        // Player sprites in multiplayer modes are modified
        // using an internal color lookup table for re-indexing.
        // If 0x4 0x8 or 0xc,
        // use a translation table for player colormaps
        const val MF_TRANSLATION = 0xc000000

        // Hmm ???.
        const val MF_TRANSSHIFT = 26
        private val buffer = ByteBuffer.allocate(154)
        private val fastclear = ByteBuffer.allocate(154)

        /*
	 * @Override protected void finalize(){ count++; if (count%100==0)
	 * System.err
	 * .printf("Total %d Mobj %s@%d finalized free memory: %d\n",count,
	 * this.type.name(),this.hashCode(),Runtime.getRuntime().freeMemory()); }
	 */
        protected var count = 0
    }
}