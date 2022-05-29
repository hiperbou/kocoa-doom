package s

import w.CacheableDoomObject
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** An object representation of Doom's sound format  */
class DMXSound : CacheableDoomObject {
    /** ushort, all Doom samples are "type 3". No idea how   */
    var type = 0

    /** ushort, speed in Hz.  */
    var speed = 0

    /** uint  */
    var datasize = 0
    lateinit var data: ByteArray
    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        type = buf.char.code
        speed = buf.char.code
        datasize = try {
            buf.int
        } catch (e: BufferUnderflowException) {
            buf.capacity() - buf.position()
        }
        data = ByteArray(Math.min(buf.remaining(), datasize))
        buf[data]
    }
}