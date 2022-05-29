package doom


import p.ActiveStates
import utils.C2JUtils
import w.CacheableDoomObject
import w.IPackableDoomObject
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class thinker_t : CacheableDoomObject, IReadableDoomObject, IPackableDoomObject {
    var prev: thinker_t? = null
    var next: thinker_t? = null
    var thinkerFunction: ActiveStates? = null
    /**
     * killough's code for thinkers seems to be totally broken in M.D,
     * so commented it out and will not probably restore, but may invent
     * something new in future
     * - Good Sign 2017/05/1
     *
     * killough 8/29/98: we maintain thinkers in several equivalence classes,
     * according to various criteria, so as to allow quicker searches.
     */
    /**
     * Next, previous thinkers in same class
     */
    //public thinker_t cnext, cprev;
    /**
     * extra fields, to use when archiving/unarchiving for
     * identification. Also in blocklinks, etc.
     */
    var id = 0
    var previd = 0
    var nextid = 0
    var functionid = 0
    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        thinker_t.readbuffer.position(0)
        thinker_t.readbuffer.order(ByteOrder.LITTLE_ENDIAN)
        f.read(thinker_t.readbuffer.array())
        unpack(thinker_t.readbuffer)
    }

    /**
     * This adds 12 bytes
     */
    @Throws(IOException::class)
    override fun pack(b: ByteBuffer) {
        // It's possible to reconstruct even by hashcodes.
        // As for the function, that should be implied by the mobj_t type.
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(C2JUtils.pointer(prev))
        b.putInt(C2JUtils.pointer(next))
        b.putInt(C2JUtils.pointer(thinkerFunction!!.ordinal))
        //System.out.printf("Packed thinker %d %d %d\n",pointer(prev),pointer(next),pointer(function));
    }

    @Throws(IOException::class)
    override fun unpack(b: ByteBuffer) {
        // We are supposed to archive pointers to other thinkers,
        // but they are rather useless once on disk.
        b.order(ByteOrder.LITTLE_ENDIAN)
        previd = b.int
        nextid = b.int
        functionid = b.int
        //System.out.printf("Unpacked thinker %d %d %d\n",pointer(previd),pointer(nextid),pointer(functionid));
    }

    companion object {
        private val readbuffer = ByteBuffer.allocate(12)
    }
}