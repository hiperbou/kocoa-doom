package s

import data.sfxinfo_t
import data.sounds
import doom.DoomMain
import java.util.*

/**
 * Functionality and fields that are common among the various "sound drivers"
 * should go here.
 *
 * @author Maes
 */
abstract class AbstractSoundDriver(protected val DM: DoomMain<*, *>, protected val numChannels: Int) : ISoundDriver {
    /**
     * The global mixing buffer. Basically, samples from all active internal
     * channels are modifed and added, and stored in the buffer that is
     * submitted to the audio device. This is a 16-bit stereo signed PCM
     * mixbuffer. Memory order is LSB (?) and channel order is L-R-L-R...
     *
     * Not all i
     *
     */
    protected lateinit var mixbuffer // = new byte[MIXBUFFERSIZE];
            : ByteArray

    /** The actual lengths of all sound effects.  */
    protected val lengths = IntArray(ISoundDriver.NUMSFX)

    /**
     * The sound in channel handles, determined on registration, might be used
     * to unregister/stop/modify, currently unused.
     */
    protected val channelhandles: IntArray

    /**
     * SFX id of the playing sound effect. Used to catch duplicates (like
     * chainsaw).
     */
    protected val channelids: IntArray

    /**
     * Pitch to stepping lookup, used in ClassicSoundDriver It's actually rigged
     * to have a -/+ 400% pitch variation!
     */
    protected val steptable = IntArray(256)

    /** Volume lookups. 128 levels  */
    protected val vol_lookup = Array(128) { IntArray(256) }

    /**
     * Time/gametic that the channel started playing, used to determine oldest,
     * which automatically has lowest priority. In case number of active sounds
     * exceeds available channels.
     */
    protected val channelstart: IntArray

    /**
     * Generates volume lookup tables which also turn the unsigned samples into
     * signed samples.
     */
    protected fun generateVolumeLUT() {
        for (i in 0..127) for (j in 0..255) vol_lookup[i][j] = i * (j - 128) * 256 / 127
    }

    /**
     * This table provides step widths for pitch parameters. Values go from 16K
     * to 256K roughly, with the middle of the table being 64K, and presumably
     * representing unitary pitch. So the pitch variation can be quite extreme,
     * allowing -/+ 400% stepping :-S
     *
     * @param steptablemid
     * @return
     */
    protected fun generateStepTable(steptablemid: Int) {
        for (i in -128..127) {
            steptable[steptablemid + i] = (Math.pow(2.0, i / 64.0) * 65536.0).toInt()
            //System.out.printf("Pitch %d %d %f\n",i,steptable[steptablemid + i],FixedFloat.toFloat(steptable[steptablemid + i]));
        }
    }

    /** Read a Doom-format sound effect from disk, leaving it in 8-bit mono format but
     * upsampling it to the target sample rate.
     *
     * @param sfxname
     * @param len
     * @param index
     * @return
     */
    protected open fun getsfx(sfxname: String?, len: IntArray, index: Int): ByteArray {
        val sfx: ByteArray
        val paddedsfx: ByteArray
        var i: Int
        var size: Int
        val paddedsize: Int
        val name: String
        val sfxlump: Int

        // Get the sound data from the WAD, allocate lump
        // in zone memory.
        name = String.format("ds%s", sfxname).uppercase(Locale.getDefault())

        // Now, there is a severe problem with the
        // sound handling, in it is not (yet/anymore)
        // gamemode aware. That means, sounds from
        // DOOM II will be requested even with DOOM
        // shareware.
        // The sound list is wired into sounds.c,
        // which sets the external variable.
        // I do not do runtime patches to that
        // variable. Instead, we will use a
        // default sound for replacement.
        sfxlump =
            if (DM.wadLoader.CheckNumForName(name) == -1) DM.wadLoader.GetNumForName("dspistol") else DM.wadLoader.GetNumForName(
                name
            )
        val dmx = DM.wadLoader.CacheLumpNum(sfxlump, 0, DMXSound::class.java)!!

        // KRUDE
        if (dmx.speed == ISoundDriver.SAMPLERATE / 2) {
            // Plain linear interpolation.
            dmx.data = DSP.crudeResample(dmx.data, 2)!!
            //DSP.filter(dmx.data,SAMPLERATE, SAMPLERATE/4);
            dmx.datasize = dmx.data.size
        }
        sfx = dmx.data

        // MAES: A-ha! So that's how they do it.
        // SOund effects are padded to the highest multiple integer of
        // the mixing buffer's size (with silence)
        paddedsize =
            (dmx.datasize + (ISoundDriver.SAMPLECOUNT - 1)) / ISoundDriver.SAMPLECOUNT * ISoundDriver.SAMPLECOUNT

        // Allocate from zone memory.
        paddedsfx = ByteArray(paddedsize)

        // Now copy and pad. The first 8 bytes are header info, so we discard
        // them.
        System.arraycopy(sfx, 0, paddedsfx, 0, dmx.datasize)

        // Pad with silence (unsigned)
        i = dmx.datasize
        while (i < paddedsize) {
            paddedsfx[i] = 127.toByte()
            i++
        }

        // Remove the cached lump.
        DM.wadLoader.UnlockLumpNum(sfxlump)
        if (AbstractSoundDriver.D) System.out.printf(
            "SFX %d name %s size %d speed %d padded to %d\n",
            index,
            sounds.S_sfx[index].name,
            dmx.datasize,
            dmx.speed,
            paddedsize
        )
        // Preserve padded length.
        len[index] = paddedsize

        // Return allocated padded data.
        // So the first 8 bytes are useless?
        return paddedsfx
    }

    /**
     * Modified getsfx, which transforms samples into 16-bit, signed, stereo
     * beforehand, before being "fed" to the audio clips.
     *
     * @param sfxname
     * @param index
     * @return
     */
    protected fun getsfx16(sfxname: String?, len: IntArray, index: Int): ByteArray {
        val sfx: ByteArray
        val paddedsfx: ByteArray
        var i: Int
        val size: Int
        val paddedsize: Int
        val name: String
        val sfxlump: Int

        // Get the sound data from the WAD, allocate lump
        // in zone memory.
        name = String.format("ds%s", sfxname).uppercase(Locale.getDefault())

        // Now, there is a severe problem with the
        // sound handling, in it is not (yet/anymore)
        // gamemode aware. That means, sounds from
        // DOOM II will be requested even with DOOM
        // shareware.
        // The sound list is wired into sounds.c,
        // which sets the external variable.
        // I do not do runtime patches to that
        // variable. Instead, we will use a
        // default sound for replacement.
        sfxlump =
            if (DM.wadLoader.CheckNumForName(name) == -1) DM.wadLoader.GetNumForName("dspistol") else DM.wadLoader.GetNumForName(
                name
            )
        size = DM.wadLoader.LumpLength(sfxlump)
        sfx = DM.wadLoader.CacheLumpNumAsRawBytes(sfxlump, 0)

        // Size blown up to accommodate two channels and 16 bits.
        // Sampling rate stays the same.
        paddedsize = (size - 8) * 2 * 2
        // Allocate from zone memory.
        paddedsfx = ByteArray(paddedsize)

        // Skip first 8 bytes (header), blow up the data
        // to stereo, BIG ENDIAN, SIGNED, 16 bit. Don't expect any fancy DSP
        // here!
        var sample = 0
        i = 8
        while (i < size) {

            // final short sam=(short) vol_lookup[127][0xFF&sfx[i]];
            val sam = (0xFF and sfx[i] - 128 shl 8).toShort()
            paddedsfx[sample++] = (0xFF and (sam.toInt() shr 8)).toByte()
            paddedsfx[sample++] = (0xFF and sam.toInt()).toByte()
            paddedsfx[sample++] = (0xFF and (sam.toInt() shr 8)).toByte()
            paddedsfx[sample++] = (0xFF and sam.toInt()).toByte()
            i++
        }

        // Remove the cached lump.
        DM.wadLoader.UnlockLumpNum(sfxlump)

        // Preserve padded length.
        len[index] = paddedsize

        // Return allocated padded data.
        // So the first 8 bytes are useless?
        return paddedsfx
    }

    /**
     * Starting a sound means adding it to the current list of active sounds in
     * the internal channels. As the SFX info struct contains e.g. a pointer to
     * the raw data it is ignored. As our sound handling does not handle
     * priority, it is ignored. Pitching (that is, increased speed of playback)
     * is set, but whether it's used or not depends on the final implementation
     * (e.g. classic mixer uses it, but AudioLine-based implementations are not
     * guaranteed.
     */
    override fun StartSound(id: Int, vol: Int, sep: Int, pitch: Int, priority: Int): Int {
        return if (id < 1 || id > sounds.S_sfx.size - 1) ISoundDriver.BUSY_HANDLE else addsfx(
            id,
            vol,
            steptable[pitch],
            sep
        )

        // Find a free channel and get a timestamp/handle for the new sound.
    }

    /**
     * This function adds a sound to the list of currently active sounds, which
     * is maintained as a given number (eight, usually) of internal channels.
     * Returns a handle.
     *
     * @param sfxid
     * @param volume
     * @param step
     * @param seperation
     * @return
     */
    protected abstract fun addsfx(
        sfxid: Int, volume: Int, step: Int,
        seperation: Int
    ): Int

    protected var handlenums: Short = 0

    // protected final static DataLine.Info info = new DataLine.Info(Clip.class,
    // format);
    init {
        channelids = IntArray(numChannels)
        channelhandles = IntArray(numChannels)
        channelstart = IntArray(numChannels)
    }

    //
    // Retrieve the raw data lump index
    // for a given SFX name.
    //
    override fun GetSfxLumpNum(sfx: sfxinfo_t): Int {
        val namebuf: String
        namebuf = String.format("ds%s", sfx.name).uppercase(Locale.getDefault())
        if (namebuf == "DSNONE") return -1
        val lump: Int
        lump = try {
            DM.wadLoader.GetNumForName(namebuf)
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        return lump
    }

    /**
     * Initialize
     *
     * @return
     */
    protected fun initMixBuffer() {
        var i = 0
        while (i < ISoundDriver.MIXBUFFERSIZE) {
            mixbuffer[i] = ((0x7FFF * Math.sin(
                1.5 * Math.PI * i.toDouble()
                        / ISoundDriver.MIXBUFFERSIZE
            )).toInt() and 0xff00 ushr 8).toByte()
            mixbuffer[i + 1] = ((0x7FFF * Math.sin(
                1.5 * Math.PI * i.toDouble()
                        / ISoundDriver.MIXBUFFERSIZE
            )).toInt() and 0xff).toByte()
            mixbuffer[i + 2] = ((0x7FFF * Math.sin(
                1.5 * Math.PI * i.toDouble()
                        / ISoundDriver.MIXBUFFERSIZE
            )).toInt() and 0xff00 ushr 8).toByte()
            mixbuffer[i + 3] = ((0x7FFF * Math.sin(
                1.5 * Math.PI * i.toDouble()
                        / ISoundDriver.MIXBUFFERSIZE
            )).toInt() and 0xff).toByte()
            i += 4
        }
    }

    /**
     * Loads samples in 8-bit format, forcibly converts them to the common sampling rate.
     * Used by.
     */
    protected fun initSound8() {
        var i: Int

        // Initialize external data (all sounds) at start, keep static.
        i = 1
        while (i < ISoundDriver.NUMSFX) {

            // Alias? Example is the chaingun sound linked to pistol.
            if (sounds.S_sfx[i]._link == null) {
                // Load data from WAD file.
                sounds.S_sfx[i].data = getsfx(sounds.S_sfx[i].name, lengths, i)
            } else {
                // Previously loaded already?
                sounds.S_sfx[i].data = sounds.S_sfx[i]._link!!.data
            }
            i++
        }
    }

    /**
     * This is only the common part of InitSound that caches sound data in
     * 16-bit, stereo format (used by Audiolines). INTO sfxenum_t.
     *
     * Only used by the Clip and David "drivers".
     *
     */
    protected fun initSound16() {
        var i: Int

        // Initialize external data (all sounds) at start, keep static.
        i = 1
        while (i < ISoundDriver.NUMSFX) {

            // Alias? Example is the chaingun sound linked to pistol.
            if (sounds.S_sfx[i]._link == null) {
                // Load data from WAD file.
                sounds.S_sfx[i].data = getsfx16(sounds.S_sfx[i].name, lengths, i)
            } else {
                // Previously loaded already?
                sounds.S_sfx[i].data = sounds.S_sfx[i]._link!!.data
            }
            i++
        }
    }

    companion object {
        const val D = false // debug
    }
}