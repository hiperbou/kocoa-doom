package s

import w.CacheableDoomObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/** Blatantly ripping off Chocolate Doom  */
class SpeakerSound : CacheableDoomObject {
    var header: Short = 0
    var length: Short = 0
    lateinit var data: ByteArray

    /** Will return a very basic, 8-bit 11.025 KHz rendition of the sound
     * This ain't no CuBase or MatLab, so if you were expecting perfect
     * sound and solid DSP, go elsewhere.
     *
     */
    fun toRawSample(): ByteArray {
        // Length is in 1/140th's of a second 
        val chunk = ByteArray(length * 11025 / 140)
        var counter = 0
        for (i in 0 until length) {
            val tmp: ByteArray = SpeakerSound.getPhoneme(data[i].toInt())!!
            System.arraycopy(tmp, 0, chunk, counter, tmp.size)
            counter += tmp.size
        }
        return chunk
    }

    @Throws(IOException::class)
    override fun unpack(buf: ByteBuffer) {
        buf.order(ByteOrder.LITTLE_ENDIAN)
        header = buf.short
        length = buf.short
        data = ByteArray(length.toInt())
        buf[data]
    }

    companion object {
        var timer_values = intArrayOf(
            0,
            6818, 6628, 6449, 6279,
            6087, 5906, 5736, 5575,
            5423, 5279, 5120, 4971,
            4830, 4697, 4554, 4435,
            4307, 4186, 4058, 3950,
            3836, 3728, 3615, 3519,
            3418, 3323, 3224, 3131,
            3043, 2960, 2875, 2794,
            2711, 2633, 2560, 2485,
            2415, 2348, 2281, 2213,
            2153, 2089, 2032, 1975,
            1918, 1864, 1810, 1757,
            1709, 1659, 1612, 1565,
            1521, 1478, 1435, 1395,
            1355, 1316, 1280, 1242,
            1207, 1173, 1140, 1107,
            1075, 1045, 1015, 986,
            959, 931, 905, 879,
            854, 829, 806, 783,
            760, 739, 718, 697,
            677, 658, 640, 621,
            604, 586, 570, 553,
            538, 522, 507, 493,
            479, 465, 452
        )

        /* From analysis of fraggle's PC Speaker timings, it was found
     * that their natural logarithm had the following intercept 
     * (starting at x=1) and slope. Therefore, it's possible
     * to go beyong the original 95 hardcoded values.
     */
        const val INTERCEPT = 8.827321453
        const val SLOPE = -0.028890647
        const val CIA_8543_FREQ = 1193182
        var f = FloatArray(256)

        init {
            SpeakerSound.f[0] = 0f
            for (x in 1 until SpeakerSound.f.size) {

                //f[x] = CIA_8543_FREQ/timer_values[x];
                SpeakerSound.f[x] =
                    (SpeakerSound.CIA_8543_FREQ / Math.exp(SpeakerSound.INTERCEPT + SpeakerSound.SLOPE * (x - 1))).toFloat()
            }
        }

        private val phonemes = Hashtable<Int, ByteArray>()
        fun getPhoneme(phoneme: Int): ByteArray? {
            if (!SpeakerSound.phonemes.containsKey(phoneme)) {

                // Generate a square wave with a duration of 1/140th of a second
                val samples = 11025 / 140
                val tmp = ByteArray(samples)
                val frequency: Float = SpeakerSound.f.get(phoneme)
                for (i in 0 until samples) {
                    tmp[i] = (127 + 127 * Math.signum(Math.sin(frequency * Math.PI * 2 * (i / 11025f)))).toInt().toByte() //TODO CHECK THIS
                }
                SpeakerSound.phonemes.put(phoneme, tmp)
            }
            return SpeakerSound.phonemes[phoneme]
        }
    }
}