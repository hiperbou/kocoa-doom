package w

import java.io.IOException
import java.nio.ByteBuffer

interface IPackableDoomObject {
    @Throws(IOException::class)
    fun pack(buf: ByteBuffer)
}