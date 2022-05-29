package p

import doom.SourceCode
import rr.SectorAction
import rr.sector_t
import w.DoomIO
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer

class plat_t : SectorAction(), IReadableDoomObject {
    //var sector: sector_t? = null //TODO: This is already in SectorAction

    @SourceCode.fixed_t
    var speed = 0

    @SourceCode.fixed_t
    var low = 0

    @SourceCode.fixed_t
    var high = 0
    var wait = 0
    var count = 0
    var status: plat_e
    var oldstatus: plat_e
    var crush = false
    var tag = 0
    var type: plattype_e? = null

    init {
        // These must never be null so they get the lowest ordinal value.
        // by default.
        status = plat_e.up
        oldstatus = plat_e.up
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        super.read(f) // Call thinker reader first            
        super.sectorid = DoomIO.readLEInt(f) // Sector index
        speed = DoomIO.readLEInt(f)
        low = DoomIO.readLEInt(f)
        high = DoomIO.readLEInt(f)
        wait = DoomIO.readLEInt(f)
        count = DoomIO.readLEInt(f)
        status = plat_e.values()[DoomIO.readLEInt(f)]
        oldstatus = plat_e.values()[DoomIO.readLEInt(f)]
        println(status)
        println(oldstatus)
        crush = DoomIO.readIntBoolean(f)
        tag = DoomIO.readLEInt(f)
        type = plattype_e.values()[DoomIO.readLEInt(f)]
    }

    @Throws(IOException::class)
    override fun pack(b: ByteBuffer) {
        super.pack(b) //12            
        b.putInt(super.sectorid) // 16
        b.putInt(speed) //20
        b.putInt(low) // 24
        b.putInt(high) //28
        b.putInt(wait) //32
        b.putInt(count) //36
        b.putInt(status.ordinal) //40
        b.putInt(oldstatus.ordinal) //44
        println(status)
        println(oldstatus)
        b.putInt(if (crush) 1 else 0) // 48
        b.putInt(tag) // 52
        b.putInt(type!!.ordinal) // 56
    }

    fun asVlDoor(sectors: Array<sector_t>): vldoor_t {
        /*
        	typedef struct
        	{
        	    thinker_t	thinker;
        	    vldoor_e	type;
        	    sector_t*	sector;
        	    fixed_t	topheight;
        	    fixed_t	speed;

        	    // 1 = up, 0 = waiting at top, -1 = down
        	    int             direction;
        	    
        	    // tics to wait at the top
        	    int             topwait;
        	    // (keep in case a door going down is reset)
        	    // when it reaches 0, start going down
        	    int             topcountdown;
        	    
        	} vldoor_t;
         */
        val tmp = vldoor_t()
        tmp.next = next
        tmp.prev = prev
        tmp.thinkerFunction = thinkerFunction
        tmp.type = vldoor_e.values()[sector!!.id % vldoor_e.VALUES]
        tmp.sector = sectors[speed % sectors.size]
        tmp.topheight = low
        tmp.speed = high
        tmp.direction = wait
        tmp.topwait = count
        tmp.topcountdown = status.ordinal
        return tmp
    }
}