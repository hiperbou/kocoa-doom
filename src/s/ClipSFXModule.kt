package s



import data.sounds
import data.sounds.sfxenum_t
import doom.DoomMain
import java.util.*
import javax.sound.sampled.*

/** Experimental Clip based driver. It does work, but it has no
 * tangible advantages over the Audioline or Classic one. If the
 * Audioline can be used, there's no reason to fall back to this
 * one.
 *
 * KNOWN ISSUES:
 *
 * a) Same general restrictions as audiolines (in fact, Clips ARE Audioline
 * in disguise)
 * b) Multiple instances of the same sound require multiple clips, so
 * even caching them is a half-baked solution, and if you have e.g. 40 imps
 * sound in a room....
 *
 *
 * Currently unused.
 *
 * @author Velktron
 */
class ClipSFXModule(DM: DoomMain<*, *>, numChannels: Int) : AbstractSoundDriver(DM, numChannels) {
    var cachedSounds = HashMap<Int, Clip?>()

    // Either it's null (no clip is playing) or non-null (some clip is playing).
    lateinit var channels: Array<Clip?>
    val linear2db: FloatArray
    private fun computeLinear2DB(): FloatArray {

        // Maximum volume is 0 db, minimum is ... -96 db.
        // We rig this so that half-scale actually gives quarter power,
        // and so is -6 dB.
        val tmp = FloatArray(ISoundDriver.VOLUME_STEPS)
        for (i in 0 until ISoundDriver.VOLUME_STEPS) {
            var linear =
                (20 * Math.log10((i.toFloat() / ISoundDriver.VOLUME_STEPS.toFloat()).toDouble())).toFloat()
            // Hack. The minimum allowed value as of now is -80 db.
            if (linear < -36.0) linear = -36.0f
            tmp[i] = linear
        }
        return tmp
    }

    override fun InitSound(): Boolean {
        // Secure and configure sound device first.
        System.err.println("I_InitSound: ")

        // We don't actually do this here (will happen only when we
        // create the first audio clip).

        // Initialize external data (all sounds) at start, keep static.
        initSound16()
        System.err.print(" pre-cached all sound data\n")
        // Finished initialization.
        System.err.print("I_InitSound: sound module ready\n")
        return true
    }

    /** Modified getsfx. The individual length of each sfx is not of interest.
     * However, they must be transformed into 16-bit, signed, stereo samples
     * beforehand, before being "fed" to the audio clips.
     *
     * @param sfxname
     * @param index
     * @return
     */
    protected fun getsfx(sfxname: String?, index: Int): ByteArray {
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
        // to stereo, BIG ENDIAN, SIGNED, 16 bit. Don't expect any fancy DSP here!
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

        // Return allocated padded data.
        // So the first 8 bytes are useless?
        return paddedsfx
    }

    override fun UpdateSound() {
        // We do nothing here, since the mixing is delegated to the OS
        // Just hope that it's more efficient that our own...
    }

    override fun SubmitSound() {
        // Dummy. Nothing actual to do here.
    }

    override fun ShutdownSound() {
        // Wait till all pending sounds are finished.
        var done = false
        var i: Int


        // FIXME (below).
        //fprintf( stderr, "I_ShutdownSound: NOT finishing pending sounds\n");
        //fflush( stderr );
        while (!done) {
            i = 0
            while (i < numChannels && (channels[i] == null || !channels[i]!!.isActive)) {
                i++
            }
            // FIXME. No proper channel output.
            if (i == numChannels) done = true
        }
        i = 0
        while (i < numChannels) {
            if (channels[i] != null) channels[i]!!.close()
            i++
        }

        // Free up resources taken up by cached clips.
        val clips: Collection<Clip?> = cachedSounds.values
        for (c in clips) {
            c!!.close()
        }

        // Done.
        return
    }

    override fun SetChannels(numChannels: Int) {
        channels = arrayOfNulls(numChannels)
    }

    private fun getClipForChannel(c: Int, sfxid: Int) {

        // Try to see if we already have such a clip.
        var clip = cachedSounds[sfxid]
        var exists = false

        // Does it exist?
        if (clip != null) {

            // Well, it does, but we are not done yet.
            exists = true
            // Is it NOT playing already?
            if (!clip.isActive) {
                // Assign it to the channel.
                channels[c] = clip
                return
            }
        }

        // Sorry, Charlie. Gotta make a new one.
        val info = DataLine.Info(Clip::class.java, DoomSound.DEFAULT_SAMPLES_FORMAT)
        try {
            clip = AudioSystem.getLine(info) as Clip
        } catch (e: LineUnavailableException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        try {
            clip!!.open(
                DoomSound.DEFAULT_SAMPLES_FORMAT,
                sounds.S_sfx[sfxid].data,
                0,
                sounds.S_sfx[sfxid].data.size
            )
        } catch (e: LineUnavailableException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        if (!exists) cachedSounds[sfxid] = clip
        channels[c] = clip


        // Control[] cs=clip.getControls();
        // 
        // for (Control cc:cs){
        // 	System.out.println("Control "+cc.getType().toString());
        // 		}
    }

    //
    // This function adds a sound to the
    //  list of currently active sounds,
    //  which is maintained as a given number
    //  (eight, usually) of internal channels.
    // Returns a handle.
    //
    //protected var handlenums: Short = 0
    override fun addsfx(sfxid: Int, volume: Int, pitch: Int, seperation: Int): Int {
        var i: Int
        var rc = -1
        var oldest = DM.gametic
        var oldestnum = 0
        val slot: Int

        // Chainsaw troubles.
        // Play these sound effects only one at a time.
        if (sfxid == sfxenum_t.sfx_sawup.ordinal || sfxid == sfxenum_t.sfx_sawidl.ordinal || sfxid == sfxenum_t.sfx_sawful.ordinal || sfxid == sfxenum_t.sfx_sawhit.ordinal || sfxid == sfxenum_t.sfx_stnmov.ordinal || sfxid == sfxenum_t.sfx_pistol.ordinal) {
            // Loop all channels, check.
            i = 0
            while (i < numChannels) {

                // Active, and using the same SFX?
                if (channels[i] != null && channels[i]!!.isRunning && channelids[i] == sfxid) {
                    // Reset.
                    channels[i]!!.stop()
                    // We are sure that iff,
                    //  there will only be one.
                    break
                }
                i++
            }
        }

        // Loop all channels to find oldest SFX.
        i = 0
        while (i < numChannels && channels[i] != null) {
            if (channelstart[i] < oldest) {
                oldestnum = i
                oldest = channelstart[i]
            }
            i++
        }

        // Tales from the cryptic.
        // If we found a channel, fine.
        // If not, we simply overwrite the first one, 0.
        // Probably only happens at startup.
        slot = if (i == numChannels) oldestnum else i

        // Okay, in the less recent channel,
        //  we will handle the new SFX.

        // We need to decide whether we can reuse an existing clip
        // or create a new one. In any case, when this method return 
        // we should have a valid clip assigned to channel "slot".
        getClipForChannel(slot, sfxid)


        // Reset current handle number, limited to 0..100.
        if (handlenums.toInt() == 0) // was !handlenums, so it's actually 1...100?
            handlenums = ISoundDriver.MAXHANDLES.toShort()

        // Assign current handle number.
        // Preserved so sounds could be stopped (unused).
        rc = handlenums--.toInt()
        channelhandles[slot] = rc

        // Should be gametic, I presume.
        channelstart[slot] = DM.gametic

        // Get the proper lookup table piece
        //  for this volume level???
        //channelleftvol_lookup[slot] = vol_lookup[leftvol];
        //channelrightvol_lookup[slot] = vol_lookup[rightvol];

        // Preserve sound SFX id,
        //  e.g. for avoiding duplicates of chainsaw.
        channelids[slot] = sfxid
        setVolume(slot, volume)
        setPanning(slot, seperation)
        //channels[slot].addSound(sound, handlenums);
        //channels[slot].setPitch(pitch);
        if (AbstractSoundDriver.D) System.err.println(channelStatus())
        if (AbstractSoundDriver.D) System.err.printf("Playing %d vol %d on channel %d\n", rc, volume, slot)
        // Well...play it.

        // FIXME VERY BIG PROBLEM: stop() is blocking!!!! WTF ?!
        //channels[slot].stop();
        //long  a=System.nanoTime();
        channels[slot]!!.framePosition = 0
        channels[slot]!!.start()
        // b=System.nanoTime();
        //System.err.printf("Sound playback completed in %d\n",(b-a));

        // You tell me.
        return rc
    }

    /** Accepts volume in "Doom" format (0-127).
     *
     * @param volume
     */
    fun setVolume(chan: Int, volume: Int) {
        val c = channels[chan]
        if (c!!.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val vc = c.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val vol = linear2db[volume]
            vc.value = vol
        } else if (c.isControlSupported(FloatControl.Type.VOLUME)) {
            val vc = c.getControl(FloatControl.Type.VOLUME) as FloatControl
            val vol = vc.minimum + (vc.maximum - vc.minimum) * volume.toFloat() / 127f
            vc.value = vol
        }
    }

    fun setPanning(chan: Int, sep: Int) {
        val c = channels[chan]
        if (c!!.isControlSupported(FloatControl.Type.PAN)) {
            val bc = c.getControl(FloatControl.Type.PAN) as FloatControl
            // Q: how does Doom's sep map to stereo panning?
            // A: Apparently it's 0-255 L-R.
            val pan: Float =
                bc.minimum + (bc.maximum - bc.minimum) * sep.toFloat() / ISoundDriver.PANNING_STEPS
            bc.value = pan
        }
    }

    override fun StopSound(handle: Int) {
        // Which channel has it?
        val hnd = getChannelFromHandle(handle)
        if (hnd >= 0) {
            channels[hnd]!!.stop()
            channels[hnd] = null
        }
    }

    override fun SoundIsPlaying(handle: Int): Boolean {
        return getChannelFromHandle(handle) != ISoundDriver.BUSY_HANDLE
    }

    override fun UpdateSoundParams(handle: Int, vol: Int, sep: Int, pitch: Int) {

        // This should be called on sounds that are ALREADY playing. We really need
        // to retrieve channels from their handles.

        //System.err.printf("Updating sound with handle %d vol %d sep %d pitch %d\n",handle,vol,sep,pitch);
        val i = getChannelFromHandle(handle)
        // None has it?
        if (i != ISoundDriver.BUSY_HANDLE) {
            //System.err.printf("Updating sound with handle %d in channel %d\n",handle,i);
            setVolume(i, vol)
            setPanning(i, sep)
            //channels[i].setPanning(sep);
        }
    }

    /** Internal use.
     *
     * @param handle
     * @return the channel that has the handle, or -2 if none has it.
     */
    private fun getChannelFromHandle(handle: Int): Int {
        // Which channel has it?
        for (i in 0 until numChannels) {
            if (channelhandles[i] == handle) return i
        }
        return ISoundDriver.BUSY_HANDLE
    }

    var sb = StringBuilder()

    init {
        linear2db = computeLinear2DB()
    }

    fun channelStatus(): String {
        sb.setLength(0)
        for (i in 0 until numChannels) {
            if (channels[i] != null && channels[i]!!.isActive) sb.append(i) else sb.append('-')
        }
        return sb.toString()
    }
}