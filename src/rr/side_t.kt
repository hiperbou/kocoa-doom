package rr

import m.fixed_t.Companion.FRACBITS
import p.Resettable
import w.DoomIO
import w.IPackableDoomObject
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * The SideDef.
 *
 * @author admin
 */
class side_t : IReadableDoomObject, IPackableDoomObject, Resettable {
    /** (fixed_t) add this to the calculated texture column  */
    var textureoffset = 0

    /** (fixed_t) add this to the calculated texture top  */
    var rowoffset = 0

    /**
     * Texture indices. We do not maintain names here.
     */
    var toptexture: Short = 0
    var bottomtexture: Short = 0
    var midtexture: Short = 0

    /** Sector the SideDef is facing. MAES: pointer  */
    var sector: sector_t? = null
    var sectorid = 0
    var special = 0

    constructor() {}
    constructor(
        textureoffset: Int, rowoffset: Int, toptexture: Short,
        bottomtexture: Short, midtexture: Short, sector: sector_t?
    ) : super() {
        this.textureoffset = textureoffset
        this.rowoffset = rowoffset
        this.toptexture = toptexture
        this.bottomtexture = bottomtexture
        this.midtexture = midtexture
        this.sector = sector
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {
        textureoffset = DoomIO.readLEShort(f).toInt() shl FRACBITS
        rowoffset = DoomIO.readLEShort(f).toInt() shl FRACBITS
        toptexture = DoomIO.readLEShort(f)
        bottomtexture = DoomIO.readLEShort(f)
        midtexture = DoomIO.readLEShort(f)
        // this.sectorid=f.readLEInt();
    }

    override fun pack(buffer: ByteBuffer) {
        buffer.putShort((textureoffset shr FRACBITS).toShort())
        buffer.putShort((rowoffset shr FRACBITS).toShort())
        buffer.putShort(toptexture)
        buffer.putShort(bottomtexture)
        buffer.putShort(midtexture)
    }

    override fun reset() {
        textureoffset = 0
        rowoffset = 0
        toptexture = 0
        bottomtexture = 0
        midtexture = 0
        sector = null
        sectorid = 0
        special = 0
    }
}