package s

class AudioChunk {
    fun setStuff(chunk: Int, time: Int) {
        this.chunk = chunk
        this.time = time
    }

    var chunk = 0
    var time = 0
    var buffer: ByteArray
    var free: Boolean

    init {
        buffer = ByteArray(ISoundDriver.MIXBUFFERSIZE)
        setStuff(0, 0)
        free = true
    }
}