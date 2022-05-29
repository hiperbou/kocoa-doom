package pooling


import s.AudioChunk

// Referenced classes of package pooling:
//            ObjectPool
class AudioChunkPool : ObjectQueuePool<AudioChunk?>(10000L) {
    override fun create(): AudioChunk {
        return AudioChunk()
    }

    override fun expire(o: AudioChunk?) {
        o!!.free = true
    }

    override fun validate(o: AudioChunk?): Boolean {
        return o!!.free
    }
}