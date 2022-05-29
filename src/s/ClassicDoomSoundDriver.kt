package s

import data.sounds
import data.sounds.sfxenum_t
import doom.DoomMain
import pooling.AudioChunkPool
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Semaphore
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * A close recreation of the classic linux doom sound mixer.
 *
 * PROS:
 * a) Very faithful to pitch and stereo effects, and original
 * volume ramping.
 * b) Uses only one audioline and one playback thread
 *
 * CONS:
 * a) May sound a bit off if production/consumption rates don't match
 * b) Sounds awful when mixing too many sounds together, just like the original.
 *
 * @author Maes
 */
open class ClassicDoomSoundDriver(DM: DoomMain<*, *>, numChannels: Int) : AbstractSoundDriver(DM, numChannels) {
    protected val produce: Semaphore
    protected val consume: Semaphore
    protected var chunk = 0

    // protected FileOutputStream fos;
    // protected DataOutputStream dao;
    // The one and only line
    protected var line: SourceDataLine? = null
    protected var cachedSounds = HashMap<Int, ByteArray>()

    /** The channel step amount...  */
    protected val channelstep: IntArray

    /** ... and a 0.16 bit remainder of last step.  */
    protected val channelstepremainder: IntArray

    /**
     * The channel data pointers, start and end. These were referred to as
     * "channels" in two different source files: s_sound.c and i_sound.c. In
     * s_sound.c they are actually channel_t (they are only informational). In
     * i_sound.c they are actual data channels.
     */
    protected var channels: Array<ByteArray?>

    /**
     * MAES: we'll have to use this for actual pointing. channels[] holds just
     * the data.
     */
    protected var p_channels: IntArray

    /**
     * The second one is supposed to point at "the end", so I'll make it an int.
     */
    protected var channelsend: IntArray

    /** Hardware left and right channel volume lookup.  */
    protected val channelleftvol_lookup: Array<IntArray?>
    protected val channelrightvol_lookup: Array<IntArray?>

    @Volatile
    protected var mixed = false

    /**
     * This function loops all active (internal) sound channels, retrieves a
     * given number of samples from the raw sound data, modifies it according to
     * the current (internal) channel parameters, mixes the per channel samples
     * into the global mixbuffer, clamping it to the allowed range, and sets up
     * everything for transferring the contents of the mixbuffer to the (two)
     * hardware channels (left and right, that is). This function currently
     * supports only 16bit.
     */
    override fun UpdateSound() {
        mixed = false

        // Mix current sound data.
        // Data, from raw sound, for right and left.
        var sample = 0
        var dl: Int
        var dr: Int

        // Pointers in global mixbuffer, left, right, end.
        // Maes: those were explicitly signed short pointers...
        var leftout: Int
        var rightout: Int
        val leftend: Int
        // Step in mixbuffer, left and right, thus two.
        val step: Int

        // Mixing channel index.
        var chan: Int

        // POINTERS to Left and right channel
        // which are in global mixbuffer, alternating.
        leftout = 0
        rightout = 2
        step = 4

        // Determine end, for left channel only
        // (right channel is implicit).
        // MAES: this implies that the buffer will only mix
        // that many samples at a time, and that the size is just right.
        // Thus, it must be flushed (p_mixbuffer=0) before reusing it.
        leftend = ISoundDriver.SAMPLECOUNT * step
        chan = 0
        while (chan < numChannels) {
            if (channels[chan] != null) // SOME mixing has taken place.
                mixed = true
            chan++
        }

        // Mix sounds into the mixing buffer.
        // Loop over step*SAMPLECOUNT,
        // that is SAMPLECOUNT values for two channels.
        while (leftout < leftend) {
            // Reset left/right value.
            dl = 0
            dr = 0

            // Love thy L2 chache - made this a loop.
            // Now more channels could be set at compile time
            // as well. Thus loop those channels.
            chan = 0
            while (chan < numChannels) {


                // if (D) System.err.printf("Checking channel %d\n",chan);
                // Check channel, if active.
                // MAES: this means that we must point to raw data here.
                if (channels[chan] != null) {
                    var channel_pointer = p_channels[chan]

                    // Get the raw data from the channel.
                    // Maes: this is supposed to be an 8-bit unsigned value.
                    sample = 0x00FF and channels[chan]!![channel_pointer].toInt()

                    // Add left and right part for this channel (sound)
                    // to the current data. Adjust volume accordingly.                        
                    // Q: could this be optimized by converting samples to 16-bit
                    // at load time, while also allowing for stereo samples?
                    // A: Only for the stereo part. You would still look a lookup
                    // for the CURRENT volume level.
                    dl += channelleftvol_lookup[chan]!![sample]
                    dr += channelrightvol_lookup[chan]!![sample]

                    // This should increment the index inside a channel, but is
                    // expressed in 16.16 fixed point arithmetic.
                    channelstepremainder[chan] += channelstep[chan]

                    // The actual channel pointer is increased here.
                    // The above trickery allows playing back different pitches.
                    // The shifting retains only the integer part.
                    channel_pointer += channelstepremainder[chan] shr 16

                    // This limits it to the "decimal" part in order to
                    // avoid undue accumulation.
                    channelstepremainder[chan] = channelstepremainder[chan] and 0xFFFF

                    // Check whether we are done. Also to avoid overflows.
                    if (channel_pointer >= channelsend[chan]) {
                        // Reset pointer for a channel.
                        if (AbstractSoundDriver.D) System.err
                            .printf(
                                "Channel %d handle %d pointer %d thus done, stopping\n",
                                chan, channelhandles[chan],
                                channel_pointer
                            )
                        channels[chan] = null
                        channel_pointer = 0
                    }

                    // Write pointer back, so we know where a certain channel
                    // is the next time UpdateSounds is called.
                    p_channels[chan] = channel_pointer
                }
                chan++
            }

            // MAES: at this point, the actual values for a single sample
            // (YIKES!) are in d1 and d2. We must use the leftout/rightout
            // pointers to write them back into the mixbuffer.

            // Clamp to range. Left hardware channel.
            // Remnant of 8-bit mixing code? That must have raped ears
            // and made them bleed.
            // if (dl > 127) *leftout = 127;
            // else if (dl < -128) *leftout = -128;
            // else *leftout = dl;
            if (dl > 0x7fff) dl = 0x7fff else if (dl < -0x8000) dl = -0x8000

            // Write left channel
            mixbuffer[leftout] = (dl and 0xFF00 ushr 8).toByte()
            mixbuffer[leftout + 1] = (dl and 0x00FF).toByte()

            // Same for right hardware channel.
            if (dr > 0x7fff) dr = 0x7fff else if (dr < -0x8000) dr = -0x8000

            // Write right channel.
            mixbuffer[rightout] = (dr and 0xFF00 ushr 8).toByte()
            mixbuffer[rightout + 1] = (dr and 0x00FF).toByte()

            // Increment current pointers in mixbuffer.
            leftout += 4
            rightout += 4
        } // End leftend/leftout while

        // Q: how do we know whether the mixbuffer isn't entirely used
        // and instead it has residual garbage samples in it?
        // A: DOOM kind of padded samples in memory, so presumably
        // they all played silence.
        // Q: what's the purpose of channelremainder etc?
        // A: pitch variations were done with fractional pointers 16.16
        // style.
    }

    /**
     * SFX API Note: this was called by S_Init. However, whatever they did in
     * the old DPMS based DOS version, this were simply dummies in the Linux
     * version. See soundserver initdata().
     */
    override fun SetChannels(numChannels: Int) {
        // Init internal lookups (raw data, mixing buffer, channels).
        // This function sets up internal lookups used during
        // the mixing process.
        val steptablemid = 128

        // Okay, reset internal mixing channels to zero.
        for (i in 0 until this.numChannels) {
            channels[i] = null
        }
        generateStepTable(steptablemid)
        generateVolumeLUT()
    }

    protected var SOUNDSRV: ClassicDoomSoundDriver.MixServer? = null
    protected var SOUNDTHREAD: Thread? = null
    override fun InitSound(): Boolean {

        // Secure and configure sound device first.
        println("I_InitSound: ")

        // We only need a single data line.
        // PCM, signed, 16-bit, stereo, 22025 KHz, 2048 bytes per "frame",
        // maximum of 44100/2048 "fps"
        val format = AudioFormat(ISoundDriver.SAMPLERATE.toFloat(), 16, 2, true, true)
        val info = DataLine.Info(SourceDataLine::class.java, format)
        if (AudioSystem.isLineSupported(info)) try {
            line = AudioSystem.getSourceDataLine(format) as SourceDataLine
            line!!.open(format, ISoundDriver.AUDIOLINE_BUFFER)
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.print("Could not play signed 16 data\n")
            return false
        }
        if (line != null) {
            System.err.print(" configured audio device\n")
            line!!.start()
        } else {
            System.err.print(" could not configure audio device\n")
            return false
        }

        // This was here only for debugging purposes
        /*
         * try { fos=new FileOutputStream("test.raw"); dao=new
         * DataOutputStream(fos); } catch (FileNotFoundException e) {
         * Auto-generated catch block e.printStackTrace(); }
         */SOUNDSRV = MixServer(line!!)
        SOUNDTHREAD = Thread(SOUNDSRV)
        SOUNDTHREAD!!.start()

        // Initialize external data (all sounds) at start, keep static.
        System.err.print("I_InitSound: ")
        super.initSound8()
        System.err.print(" pre-cached all sound data\n")

        // Now initialize mixbuffer with zero.
        initMixBuffer()

        // Finished initialization.
        System.err.print("I_InitSound: sound module ready\n")
        return true
    }

    override fun addsfx(sfxid: Int, volume: Int, step: Int, seperation: Int): Int {
        var seperation = seperation
        var i: Int
        var rc = -1
        var oldest = DM.gametic
        var oldestnum = 0
        val slot: Int
        var rightvol: Int
        var leftvol: Int
        var broken = -1

        // Chainsaw troubles.
        // Play these sound effects only one at a time.
        if ((sfxid >= sfxenum_t.sfx_sawup.ordinal
                    && sfxid <= sfxenum_t.sfx_sawhit.ordinal) || sfxid == sfxenum_t.sfx_stnmov.ordinal || sfxid == sfxenum_t.sfx_pistol.ordinal
        ) {
            // Loop all channels, check.
            i = 0
            while (i < numChannels) {

                // Active, and using the same SFX?
                if (channels[i] != null && channelids[i] == sfxid) {
                    // Reset.
                    p_channels[i] = 0
                    channels[i] = null
                    // We are sure that iff,
                    // there will only be one.
                    broken = i
                    break
                }
                i++
            }
        }

        // Loop all channels to find oldest SFX.
        if (broken >= 0) {
            i = broken
            oldestnum = broken
        } else {
            i = 0
            while (i < numChannels && channels[i] != null) {
                if (channelstart[i] < oldest) {
                    oldestnum = i
                }
                i++
            }
        }
        oldest = channelstart[oldestnum]

        // Tales from the cryptic.
        // If we found a channel, fine.
        // If not, we simply overwrite the first one, 0.
        // Probably only happens at startup.
        slot = if (i == numChannels) oldestnum else i

        // Okay, in the less recent channel,
        // we will handle the new SFX.
        // Set pointer to raw data.
        channels[slot] = sounds.S_sfx[sfxid].data

        // MAES: if you don't zero-out the channel pointer here, it gets ugly
        p_channels[slot] = 0

        // Set pointer to end of raw data.
        channelsend[slot] = lengths[sfxid]

        // Reset current handle number, limited to 0..100.
        if (handlenums.toInt() == 0) // was !handlenums, so it's actually 1...100?
            handlenums = 100

        // Assign current handle number.
        // Preserved so sounds could be stopped (unused).
        // Maes: this should really be decreasing, otherwide handles
        // should start at 0 and go towards 100. Just saying.
        rc = handlenums--.toInt()
        channelhandles[slot] = rc

        // Set stepping???
        // Kinda getting the impression this is never used.
        // MAES: you're wrong amigo.
        channelstep[slot] = step
        // ???
        channelstepremainder[slot] = 0
        // Should be gametic, I presume.
        channelstart[slot] = DM.gametic

        // Separation, that is, orientation/stereo.
        // range is: 1 - 256
        seperation += 1

        // Per left/right channel.
        // x^2 seperation,
        // adjust volume properly.
        leftvol = volume - (volume * seperation * seperation shr 16) // /(256*256);
        seperation = seperation - 257
        rightvol = volume - (volume * seperation * seperation shr 16)

        // Sanity check, clamp volume.
        // Maes: better to clamp than to crash, no?
        if (rightvol < 0) rightvol = 0
        if (rightvol > 127) rightvol = 127
        if (leftvol < 0) leftvol = 0
        if (leftvol > 127) leftvol = 127

        // Get the proper lookup table piece
        // for this volume level???
        channelleftvol_lookup[slot] = vol_lookup[leftvol]
        channelrightvol_lookup[slot] = vol_lookup[rightvol]

        // Preserve sound SFX id,
        // e.g. for avoiding duplicates of chainsaw.
        channelids[slot] = sfxid
        if (AbstractSoundDriver.D) System.err.println(channelStatus())
        if (AbstractSoundDriver.D) System.err.printf(
            "Playing sfxid %d handle %d length %d vol %d on channel %d\n",
            sfxid, rc, sounds.S_sfx[sfxid].data.size, volume, slot
        )

        // You tell me.
        return rc
    }

    override fun ShutdownSound() {
        var done = false

        // Unlock sound thread if it's waiting.
        produce.release()
        var i: Int
        while (!done) {
            i = 0
            while (i < numChannels && channels[i] == null) {
                i++
            }

            // System.err.printf("%d channels died off\n",i);
            UpdateSound()
            SubmitSound()
            if (i == numChannels) done = true
        }
        line!!.drain()
        SOUNDSRV!!.terminate = true
        produce.release()
        try {
            SOUNDTHREAD!!.join()
        } catch (e: InterruptedException) {
            // Well, I don't care.
        }
        line!!.close()
    }

    protected inner class MixServer(private val auline: SourceDataLine) : Runnable {
        var terminate = false
        private val audiochunks = ArrayBlockingQueue<AudioChunk>(ISoundDriver.BUFFER_CHUNKS * 2)
        fun addChunk(chunk: AudioChunk) {
            audiochunks.offer(chunk)
        }

        @Volatile
        var currstate = 0
        override fun run() {
            while (!terminate) {

                // while (timing[mixstate]<=mytime){

                // Try acquiring a produce permit before going on.
                try {
                    // System.err.println("Waiting for a permit...");
                    produce.acquire()
                    // System.err.println("Got a permit");
                } catch (e: InterruptedException) {
                    // Well, ouch.
                    e.printStackTrace()
                }
                var chunks = 0

                // System.err.printf("Audio queue has %d chunks\n",audiochunks.size());

                // Play back only at most a given number of chunks once you reach
                // this spot
                var atMost = Math.min(ISoundDriver.BUFFER_CHUNKS, audiochunks.size)
                while (atMost-- > 0) {
                    var chunk: AudioChunk? = null
                    try {
                        chunk = audiochunks.take()
                    } catch (e1: InterruptedException) {
                        // Should not block
                    }
                    // Play back all chunks present in a buffer ASAP
                    auline.write(chunk!!.buffer, 0, ISoundDriver.MIXBUFFERSIZE)
                    chunks++
                    // No matter what, give the chunk back!
                    chunk.free = true
                    audiochunkpool.checkIn(chunk)
                }

                // Signal that we consumed a whole buffer and we are ready for
                // another one.
                consume.release()
            }
        }
    }

    override fun SoundIsPlaying(handle: Int): Boolean {
        val c = getChannelFromHandle(handle)
        return c != -2 && channels[c] == null
    }

    /**
     * Internal use.
     *
     * @param handle
     * @return the channel that has the handle, or -2 if none has it.
     */
    protected fun getChannelFromHandle(handle: Int): Int {
        // Which channel has it?
        for (i in 0 until numChannels) {
            if (channelhandles[i] == handle) return i
        }
        return ISoundDriver.BUSY_HANDLE
    }

    override fun StopSound(handle: Int) {
        // Which channel has it?
        val hnd = getChannelFromHandle(handle)
        if (hnd >= 0) {
            channels[hnd] = null
            p_channels[hnd] = 0
            channelhandles[hnd] = ISoundDriver.IDLE_HANDLE
        }
    }

    override fun SubmitSound() {

        // It's possible for us to stay silent and give the audio
        // queue a chance to get drained.
        if (mixed) {
            silence = 0
            val gunk = audiochunkpool.checkOut()!!
            // Ha ha you're ass is mine!
            gunk.free = false

            // System.err.printf("Submitted sound chunk %d to buffer %d \n",chunk,mixstate);

            // Copy the currently mixed chunk into its position inside the
            // master buffer.
            System.arraycopy(mixbuffer, 0, gunk.buffer, 0, ISoundDriver.MIXBUFFERSIZE)
            SOUNDSRV!!.addChunk(gunk)

            // System.err.println(chunk++);
            chunk++
            // System.err.println(chunk);
            if (consume.tryAcquire()) produce.release()
        } else {
            silence++
            // MAES: attempt to fix lingering noise error
            if (silence > ISoundDriver.BUFFER_CHUNKS * 5) {
                line!!.flush()
                silence = 0
            }
            // System.err.println("SILENT_CHUNK");
            // this.SOUNDSRV.addChunk(SILENT_CHUNK);
        }
        // line.write(mixbuffer, 0, mixbuffer.length);
    }

    private var silence = 0
    override fun UpdateSoundParams(handle: Int, vol: Int, sep: Int, pitch: Int) {
        var sep = sep
        val chan = getChannelFromHandle(handle)
        // Per left/right channel.
        // x^2 seperation,
        // adjust volume properly.
        val leftvol = vol - (vol * sep * sep shr 16) // /(256*256);
        sep = sep - 257
        val rightvol = vol - (vol * sep * sep shr 16)

        // Sanity check, clamp volume.
        if (rightvol < 0 || rightvol > 127) DM.doomSystem.Error("rightvol out of bounds")
        if (leftvol < 0 || leftvol > 127) DM.doomSystem.Error("leftvol out of bounds")

        // Get the proper lookup table piece
        // for this volume level???
        channelleftvol_lookup[chan] = vol_lookup[leftvol]
        channelrightvol_lookup[chan] = vol_lookup[rightvol]

        // Well, if you can get pitch to change too...
        channelstep[chan] = steptable[pitch]
        channelsend[chan] = lengths[channelids[chan]]
    }

    protected var sb = StringBuilder()
    fun channelStatus(): String {
        sb.setLength(0)
        for (i in 0 until numChannels) {
            if (channels[i] != null) sb.append(i) else sb.append('-')
        }
        return sb.toString()
    }

    protected val SILENT_CHUNK = AudioChunk()
    protected val audiochunkpool = AudioChunkPool()

    init {
        channelleftvol_lookup = arrayOfNulls(numChannels)
        channelrightvol_lookup = arrayOfNulls(numChannels)
        channelstep = IntArray(numChannels)
        channelstepremainder = IntArray(numChannels)
        channels = arrayOfNulls(numChannels)
        p_channels = IntArray(numChannels)
        channelsend = IntArray(numChannels)
        produce = Semaphore(1)
        consume = Semaphore(1)
        produce.drainPermits()
        mixbuffer = ByteArray(ISoundDriver.MIXBUFFERSIZE)
    }
}