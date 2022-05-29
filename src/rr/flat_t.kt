package rr

import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer

class flat_t : CacheableDoomObject {
    var data: ByteArray

    constructor() {
        data = ByteArray(flat_t.FLAT_SIZE)
    }

    constructor(size: Int) {
        data = ByteArray(size)
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {

        //buf.get(this.data);
        data = buf.array()
    }

    companion object {
        const val FLAT_SIZE = 4096
    }
}