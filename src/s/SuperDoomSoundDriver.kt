package s


import com.hiperbou.lang.times
import data.sounds
import data.sounds.sfxenum_t
import doom.DoomMain
import pooling.AudioChunkPool
import s.*
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Semaphore
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * A spiffy new sound system, based on the Classic sound driver.
 * It is entirely asynchronous (runs in its own thread) and even has its own timer.
 * This allows it to continue mixing even when the main loop is not responding
 * (something which, arguably, could be achieved just with a timer calling
 * UpdateSound and SubmitSound). Uses message passing to deliver channel status
 * info, and mixed audio directly without using an intermediate buffer,
 * saving memory bandwidth.
 *
 * PROS:
 * a) All those of ClassicSoundDriver plus:
 * b) Continues normal playback even under heavy CPU load, works smoother
 * even on lower powered CPUs.
 * c) More efficient due to less copying of audio blocks.
 * c) Fewer audio glitches compared to ClassicSoundDriver.
 *
 * CONS:
 * a) All those of ClassicSoundDriver plus regarding timing accuracy.
 *
 * @author Maes
 */
class SuperDoomSoundDriver(DM: DoomMain<*, *>, numChannels: Int) : AbstractSoundDriver(DM, numChannels) {
    protected val produce: Semaphore
    protected val consume: Semaphore
    protected val update_mixer: Semaphore
    protected var chunk = 0

    //protected FileOutputStream fos;
    //protected DataOutputStream dao;
    // The one and only line
    protected var line: SourceDataLine? = null
    protected var cachedSounds = HashMap<Int, ByteArray>()
    protected val MIXTIMER: Timer

    /** These are still defined here to decouple them from the mixer's
     * ones, however they serve  more as placeholders/status indicators;
     */
    @Volatile
    protected var channels: BooleanArray

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
        // This is pretty much a dummy.
        // The mixing thread goes on by itself, guaranteeing that it will
        // carry out at least currently enqueued mixing messages, regardless
        // of how badly the engine lags.
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
            channels[i] = false
        }
        generateStepTable(steptablemid)
        generateVolumeLUT()
    }

    protected var SOUNDSRV: PlaybackServer? = null
    protected val MIXSRV: SuperDoomSoundDriver.MixServer
    protected var MIXTHREAD: Thread? = null
    protected var SOUNDTHREAD: Thread? = null
    override fun InitSound(): Boolean {

        // Secure and configure sound device first.
        System.err.print("I_InitSound: ")

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
            System.err.print("configured audio device\n")
            line!!.start()
        } else {
            System.err.print("could not configure audio device\n")
            return false
        }
        SOUNDSRV = PlaybackServer(line!!)
        SOUNDTHREAD = Thread(SOUNDSRV)
        SOUNDTHREAD!!.isDaemon = true
        SOUNDTHREAD!!.start()
        // Vroom!        
        MIXTHREAD = Thread(MIXSRV)
        MIXTHREAD!!.isDaemon = true
        MIXTHREAD!!.start()

        // Initialize external data (all sounds) at start, keep static.
        System.err.print("I_InitSound: ")
        super.initSound8()
        System.err.print("pre-cached all sound data\n")

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
        val rightvol: Int
        val leftvol: Int
        var broken = -1

        // Chainsaw troubles.
        // Play these sound effects only one at a time.
        if (sfxid == sfxenum_t.sfx_sawup.ordinal || sfxid == sfxenum_t.sfx_sawidl.ordinal || sfxid == sfxenum_t.sfx_sawful.ordinal || sfxid == sfxenum_t.sfx_sawhit.ordinal || sfxid == sfxenum_t.sfx_stnmov.ordinal || sfxid == sfxenum_t.sfx_pistol.ordinal) {
            // Loop all channels, check.
            i = 0
            while (i < numChannels) {

                // Active, and using the same SFX?
                if (channels[i] && channelids[i] == sfxid) {
                    // Reset.
                    val m = MixMessage()
                    m.stop = true

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
            while (i < numChannels && channels[i]) {
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
        val m = MixMessage()

        // Okay, in the less recent channel,
        // we will handle the new SFX.
        // Set pointer to raw data.
        channels[slot] = true
        m.channel = slot
        m.data = sounds.S_sfx[sfxid].data

        // MAES: if you don't zero-out the channel pointer here, it gets ugly
        m.pointer = 0

        // Set pointer to end of raw data.
        m.end = lengths[sfxid]

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
        m.step = step
        // ???
        m.remainder = 0
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
        if (rightvol < 0 || rightvol > 127) DM.doomSystem.Error("rightvol out of bounds")
        if (leftvol < 0 || leftvol > 127) DM.doomSystem.Error("leftvol out of bounds")

        // Get the proper lookup table piece
        // for this volume level???
        m.leftvol_lookup = vol_lookup[leftvol]
        m.rightvol_lookup = vol_lookup[rightvol]

        // Preserve sound SFX id,
        // e.g. for avoiding duplicates of chainsaw.
        channelids[slot] = sfxid
        if (AbstractSoundDriver.D) System.err.println(channelStatus())
        if (AbstractSoundDriver.D) System.err.printf(
            "Playing sfxid %d handle %d length %d vol %d on channel %d\n",
            sfxid, rc, sounds.S_sfx[sfxid].data.size, volume, slot
        )
        MIXSRV.submitMixMessage(m)

        // You tell me.
        return rc
    }

    override fun ShutdownSound() {
        var done: Boolean

        // Unlock sound thread if it's waiting.
        produce.release()
        update_mixer.release()
        do {
            done = true
            numChannels.times { i ->

                // If even one channel is playing, loop again.
                done = done and !channels[i]
            }
            //System.out.println(done+" "+this.channelStatus());
        } while (!done)
        line!!.flush()
        SOUNDSRV!!.terminate = true
        MIXSRV.terminate = true
        produce.release()
        update_mixer.release()
        try {
            SOUNDTHREAD!!.join()
            MIXTHREAD!!.join()
        } catch (e: InterruptedException) {
            // Well, I don't care.
        }
        System.err.printf("3\n")
        line!!.close()
        System.err.printf("4\n")
    }

    protected inner class PlaybackServer(private val auline: SourceDataLine) : Runnable {
        var terminate = false
        private val audiochunks = ArrayBlockingQueue<AudioChunk?>(ISoundDriver.BUFFER_CHUNKS * 2)
        fun addChunk(chunk: AudioChunk?) {
            audiochunks.offer(chunk)
        }

        @Volatile
        var currstate = 0
        override fun run() {
            while (!terminate) {

                // while (timing[mixstate]<=mytime){

                // Try acquiring a produce permit before going on.
                try {
                    //System.err.print("Waiting for a permit...");
                    produce.acquire()
                    //System.err.print("...got it\n");
                } catch (e: InterruptedException) {
                    // Well, ouch.
                    e.printStackTrace()
                }
                var chunks = 0

                // System.err.printf("Audio queue has %d chunks\n",audiochunks.size());

                // Play back only at most a given number of chunks once you reach
                // this spot.
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

                //System.err.println(">>>>>>>>>>>>>>>>> CHUNKS " +chunks);
                // Signal that we consumed a whole buffer and we are ready for
                // another one.
                consume.release()
            }
        }
    }

    /** A single channel does carry a lot of crap, figuratively speaking.
     * Instead of making updates to ALL channel parameters, it makes more
     * sense having a "mixing queue" with instructions that tell the
     * mixer routine to do so-and-so with a certain channel. The mixer
     * will then "empty" the queue when it has completed a complete servicing
     * of all messages and mapped them to its internal status.
     *
     */
    protected inner class MixMessage {
        /** If this is set, the mixer considers that channel "muted"  */
        var stop = false

        /** This signals an update of a currently active channel.
         * Therefore pointer, remainder and data should remain untouched.
         * However volume and step of a particular channel can change.
         */
        var update = false
        var remainder = 0
        var end = 0
        var channel = 0
        lateinit var data: ByteArray
        var step = 0
        var stepremainder = 0
        lateinit var leftvol_lookup: IntArray
        lateinit var rightvol_lookup: IntArray
        var pointer = 0
    }

    /** Mixing thread. Mixing and submission must still go on even if
     * the engine lags behind due to excessive CPU load.
     *
     * @author Maes
     */
    protected inner class MixServer(numChannels: Int) : Runnable {
        private val mixmessages: ArrayBlockingQueue<MixMessage>

        /**
         * MAES: we'll have to use this for actual pointing. channels[] holds just
         * the data.
         */
        protected var p_channels: IntArray

        /**
         * The second one is supposed to point at "the end", so I'll make it an int.
         */
        protected var channelsend: IntArray
        private val channels: Array<ByteArray?>

        /** The channel step amount...  */
        protected val channelstep: IntArray

        /** ... and a 0.16 bit remainder of last step.  */
        protected val channelstepremainder: IntArray
        protected val channelrightvol_lookup: Array<IntArray?>
        protected val channelleftvol_lookup: Array<IntArray?>

        //@Volatile
        private val update = false

        /** Adds a channel mixing message to the queue  */
        fun submitMixMessage(m: MixMessage) {
            try {
                mixmessages.add(m)
            } catch (e: IllegalStateException) {
                // Queue full. Force clear (VERY rare).
                mixmessages.clear()
                mixmessages.add(m)
            }
        }

        var terminate = false
        override fun run() {

            // Mix current sound data.
            // Data, from raw sound, for right and left.
            var sample = 0
            var dl: Int
            var dr: Int

            // Pointers in global mixbuffer, left, right, end.
            // Maes: those were explicitly signed short pointers...
            var leftout: Int
            var rightout: Int

            // Step in mixbuffer, left and right, thus two.
            val step = 4

            // Mixing channel index.
            var chan: Int

            // Determine end, for left channel only
            // (right channel is implicit).
            // MAES: this implies that the buffer will only mix
            // that many samples at a time, and that the size is just right.
            // Thus, it must be flushed (p_mixbuffer=0) before reusing it.
            val leftend: Int = ISoundDriver.SAMPLECOUNT * step

            // Mix the next chunk, regardless of what the rest of the game is doing. 
            while (!terminate) {

                // POINTERS to Left and right channel
                // which are in global mixbuffer, alternating.
                leftout = 0
                rightout = 2

                // Wait on interrupt semaphore anyway before draining queue.
                // This allows continuing mixing even if the main game loop
                // is stalled. This will result in continuous sounds,
                // rather than choppy interruptions.
                try {
                    //System.err.print("Waiting on semaphore...");
                    update_mixer.acquire()
                    //System.err.print("...broke free\n");
                } catch (e: InterruptedException) {
                    // Nothing to do. Suck it down.
                }


                // Get current number of element in queue.
                // At worse, there will be none.
                val messages = mixmessages.size

                // Drain the queue, applying changes to currently
                // looping channels, if applicable. This may result in new channels,
                // older ones being stopped, or current ones being altered. Changes
                // will be applied with priority either way.
                if (messages > 0) drainAndApply(messages)

                // This may have changed in the mean.
                mixed = activeChannels()
                if (mixed) { // Avoid mixing entirely if no active channel.

                    // Get audio chunk NOW
                    gunk = audiochunkpool.checkOut()
                    // Ha ha you're ass is mine!
                    gunk!!.free = false
                    mixbuffer = gunk!!.buffer
                    while (leftout < leftend) {
                        // Reset left/right value.
                        dl = 0
                        dr = 0

                        // Love thy L2 chache - made this a loop.
                        // Now more channels could be set at compile time
                        // as well. Thus loop those channels.
                        chan = 0
                        while (chan < numChannels) {


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

                                    // Communicate back to driver.
                                    this@SuperDoomSoundDriver.channels[chan] = false
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
                        leftout += step
                        rightout += step
                    } // End leftend/leftout while

                    // for (chan = 0; chan < numChannels; chan++) {
                    // 	if (channels[chan]!=null){
                    // 		System.err.printf("Channel %d pointer %d\n",chan,this.p_channels[chan]);
                    // 	}
                    // }
                } // if-mixed

                // After an entire buffer has been mixed, we can apply any updates.
                // This includes silent updates.
                submitSound()
            } // terminate loop
        }

        private var gunk: AudioChunk? = null

        init {
            // We can put only so many messages "on hold"
            mixmessages = ArrayBlockingQueue(35 * numChannels)
            p_channels = IntArray(numChannels)
            this.channels = arrayOfNulls(numChannels)
            channelstepremainder = IntArray(numChannels)
            channelsend = IntArray(numChannels)
            channelstep = IntArray(numChannels)
            channelleftvol_lookup = arrayOfNulls(numChannels)
            channelrightvol_lookup = arrayOfNulls(numChannels)
        }

        private fun submitSound() {
            // It's possible to stay entirely silent and give the audio
            // queue a chance to get drained. without sending any data.
            // Saves BW and CPU cycles.
            if (mixed) {
                silence = 0


                // System.err.printf("Submitted sound chunk %d to buffer %d \n",chunk,mixstate);

                // Copy the currently mixed chunk into its position inside the
                // master buffer.
                // System.arraycopy(mixbuffer, 0, gunk.buffer, 0, MIXBUFFERSIZE);
                SOUNDSRV!!.addChunk(gunk)

                // System.err.println(chunk++);
                chunk++
                // System.err.println(chunk);
                if (consume.tryAcquire()) produce.release()
            } else {
                silence++
                // MAES: attempt to fix lingering noise error
                if (silence > ISoundDriver.BUFFER_CHUNKS) {
                    line!!.flush()
                    silence = 0
                }
            }
        }

        /** Drains message queue and applies to individual channels.
         * More recently enqueued messages will trump older ones. This method
         * only changes the STATUS of channels, and actual message submissions
         * can occur at most every sound frame.
         *
         * @param messages
         */
        private fun drainAndApply(messages: Int) {
            var m: MixMessage
            for (i in 0 until messages) {
                // There should be no problems, in theory.
                m = mixmessages.remove()
                if (m.stop) {
                    stopChannel(m.channel)
                } else if (m.update) {
                    updateChannel(m)
                } else insertChannel(m)
            }
        }

        private fun stopChannel(channel: Int) {
            //System.err.printf("Stopping channel %d\n",channel);
            this.channels[channel] = null
            p_channels[channel] = 0
        }

        private fun updateChannel(m: MixMessage) {
            //System.err.printf("Updating channel %d\n",m.channel);
            channelleftvol_lookup[m.channel] = m.leftvol_lookup
            channelrightvol_lookup[m.channel] = m.rightvol_lookup
            channelstep[m.channel] = m.step
            channelsend[m.channel] = m.end
        }

        private fun insertChannel(m: MixMessage) {
            val ch = m.channel
            //System.err.printf("Inserting channel %d\n",ch);
            this.channels[ch] = m.data
            p_channels[ch] = m.pointer
            channelsend[ch] = m.end
            channelstepremainder[ch] = m.remainder
            channelleftvol_lookup[ch] = m.leftvol_lookup
            channelrightvol_lookup[ch] = m.rightvol_lookup
            channelstep[ch] = m.step
        }

        private fun activeChannels(): Boolean {
            for (chan in 0 until numChannels) {
                if (channels[chan] != null) // SOME mixing has taken place.
                    return true
            }
            return false
        }

        fun channelIsPlaying(num: Int): Boolean {
            return channels[num] != null
        }
    }

    override fun SoundIsPlaying(handle: Int): Boolean {
        val c = getChannelFromHandle(handle)
        return c != -2 && channels[c]
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
            channels[hnd] = false
            channelhandles[hnd] = ISoundDriver.IDLE_HANDLE
            val m = MixMessage()
            m.channel = hnd
            m.stop = true
            // We can only "ask" the mixer to stop at the next
            //chunk.
            MIXSRV.submitMixMessage(m)
        }
    }

    override fun SubmitSound() {

        // Also a dummy. The mixing thread is in a better position to
        // judge when sound should be submitted.
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
        val m = MixMessage()

        // We are updating a currently active channel
        m.update = true
        m.channel = chan

        // Get the proper lookup table piece
        // for this volume level???
        m.leftvol_lookup = vol_lookup[leftvol]
        m.rightvol_lookup = vol_lookup[rightvol]

        // Well, if you can get pitch to change too...
        m.step = steptable[pitch]

        // Oddly enough, we could be picking a different channel here? :-S
        m.end = lengths[channelids[chan]]
        MIXSRV.submitMixMessage(m)
    }

    protected var sb = StringBuilder()
    fun channelStatus(): String {
        sb.setLength(0)
        for (i in 0 until numChannels) {
            if (MIXSRV.channelIsPlaying(i)) sb.append(i) else sb.append('-')
        }
        return sb.toString()
    }

    // Schedule this to release the sound thread at regular intervals
    // so that it doesn't outrun the audioline's buffer and game updates.
    protected inner class SoundTimer : TimerTask() {
        override fun run() {
            update_mixer.release()
        }
    }

    protected val SILENT_CHUNK = AudioChunk()
    protected val audiochunkpool = AudioChunkPool()

    init {
        channels = BooleanArray(numChannels)
        produce = Semaphore(1)
        consume = Semaphore(1)
        update_mixer = Semaphore(1)
        produce.drainPermits()
        update_mixer.drainPermits()
        MIXSRV = MixServer(numChannels)
        MIXTIMER = Timer(true)
        // Sound tics every 1/35th of a second. Grossly
        // inaccurate under Windows though, will get rounded
        // down to the closest multiple of 15 or 16 ms.
        MIXTIMER.schedule(SoundTimer(), 0, ISoundDriver.SOUND_PERIOD.toLong())
    }
}