package s

import data.sfxinfo_t
import p.mobj_t
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.SourceDataLine

class channel_t {
    /** Currently playing sound. If null, then it's free  */
    private var currentSound: DoomSound? = null
    var sfxinfo: sfxinfo_t

    // origin of sound (usually a mobj_t).
    var origin: mobj_t? = null

    // handle of the sound being played
    var handle = 0
    var format: AudioFormat? = null
    var sfxVolume = 0
    var auline: SourceDataLine? = null

    init {
        sfxinfo = sfxinfo_t()
    }
}