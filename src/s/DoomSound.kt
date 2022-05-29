package s

import data.sfxinfo_t
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding

/** A class representing a sample in memory
 * Convenient for wrapping/mirroring it regardless of what it represents.
 */
internal class DoomSound : sfxinfo_t {
    var format: AudioFormat

    constructor(format: AudioFormat) {
        this.format = format
    }

    constructor() {
        format = DoomSound.DEFAULT_DOOM_FORMAT
    }

    constructor(sfx: sfxinfo_t, format: AudioFormat) : this(format) {
        data = sfx.data
        pitch = sfx.pitch
        _link = sfx._link
        lumpnum = sfx.lumpnum
        name = sfx.name
        priority = sfx.priority
        singularity = sfx.singularity
        usefulness = sfx.usefulness
        volume = sfx.volume
    }

    companion object {
        /** This audio format is the one used by internal samples (16 bit, 11KHz, Stereo)
         * for Clips and AudioLines. Sure, it's not general enough... who cares though?
         */
        val DEFAULT_SAMPLES_FORMAT = AudioFormat(
            Encoding.PCM_SIGNED,
            ISoundDriver.SAMPLERATE.toFloat(),
            16,
            2,
            4,
            ISoundDriver.SAMPLERATE.toFloat(),
            true
        )
        val DEFAULT_DOOM_FORMAT = AudioFormat(
            Encoding.PCM_UNSIGNED,
            ISoundDriver.SAMPLERATE.toFloat(),
            8,
            1,
            1,
            ISoundDriver.SAMPLERATE.toFloat(),
            true
        )
    }
}