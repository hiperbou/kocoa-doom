package s

import data.sounds
import data.sounds.sfxenum_t
import doom.DoomMain
import java.util.concurrent.Semaphore
import javax.sound.sampled.*

/** David Martel's sound driver for Mocha Doom. Excellent work!
 *
 * However, it's based on Java Audiolines, and as such has a number
 * of drawbacks:
 *
 * a) Sounds are forcibly blown to be stereo, 16-bit otherwise it's
 * impossible to get panning controls.
 * b) Volume, master gain, panning, pitch etc. controls are NOT guaranteed
 * to be granted across different OSes , and your mileage may vary. It's
 * fairly OK under Windows and OS X, but Linux is very clunky. The only
 * control that is -somewhat- guaranteed is the volume one.
 * c) Spawns as many threads as channels. Even if semaphore waiting it used,
 * that can be taxing for slower systems.
 *
 *
 * @author David
 * @author Velktron
 */
class DavidSFXModule(DM: DoomMain<*, *>, numChannels: Int) : AbstractSoundDriver(DM, numChannels) {
    private var cachedSounds = ArrayList<DoomSound>()
    val linear2db: FloatArray
    private lateinit var channels: Array<SoundWorker?>
    private lateinit var soundThread: Array<Thread?>
    private fun computeLinear2DB(): FloatArray {

        // Maximum volume is 0 db, minimum is ... -96 db.
        // We rig this so that half-scale actually gives quarter power,
        // and so is -6 dB.
        val tmp = FloatArray(ISoundDriver.VOLUME_STEPS)
        for (i in 0 until ISoundDriver.VOLUME_STEPS) {
            var linear =
                (10 * Math.log10((i.toFloat() / ISoundDriver.VOLUME_STEPS.toFloat()).toDouble())).toFloat()
            // Hack. The minimum allowed value as of now is -80 db.
            if (linear < -36.0) linear = -36.0f
            tmp[i] = linear
        }
        return tmp
    }

    override fun InitSound(): Boolean {
        // Secure and configure sound device first.
        System.err.println("I_InitSound: ")

        // Initialize external data (all sounds) at start, keep static.
        initSound16()

        // Cache sounds internally so they can be "fed" to AudioLine threads later.
        // These can be more than the usual built-in sounds.
        for (i in sounds.S_sfx.indices) {
            val tmp = DoomSound(sounds.S_sfx[i], DoomSound.DEFAULT_SAMPLES_FORMAT)
            cachedSounds.add(tmp)
        }
        System.err.print(" pre-cached all sound data\n")
        // Finished initialization.
        System.err.print("I_InitSound: sound module ready\n")
        return true
    }

    override fun UpdateSound() {
        // In theory, we should update volume + panning for each active channel.
        // Ouch. Ouch Ouch.
    }

    override fun SubmitSound() {
        // Sound should be submitted to the sound threads, which they pretty much
        // do themselves.
    }

    override fun ShutdownSound() {
        // Wait till all pending sounds are finished.
        var done = false
        var i: Int
        while (!done) {
            i = 0
            while (i < numChannels && !channels[i]!!.isPlaying()) {
                i++
            }
            if (i == numChannels) done = true
        }
        i = 0
        while (i < numChannels) {
            channels[i]!!.terminate = true
            channels[i]!!.wait.release()
            try {
                soundThread[i]!!.join()
            } catch (e: InterruptedException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            i++
        }
        // Done.
        return
    }

    override fun SetChannels(numChannels: Int) {
        channels = arrayOfNulls(numChannels)
        soundThread = arrayOfNulls(numChannels)

        // This is actually called from IDoomSound.
        for (i in 0 until numChannels) {
            channels[i] = SoundWorker(i)
            soundThread[i] = Thread(channels[i])
            soundThread[i]!!.start()
        }
    }

    /** This one will only create datalines for common clip/audioline samples
     * directly.
     *
     * @param c
     * @param sfxid
     */
    private fun createDataLineForChannel(c: Int, sfxid: Int) {

        // None? Make a new one.
        if (channels[c]!!.auline == null) {
            try {
                val tmp = cachedSounds[sfxid]
                // Sorry, Charlie. Gotta make a new one.
                val info = DataLine.Info(SourceDataLine::class.java, DoomSound.DEFAULT_SAMPLES_FORMAT)
                channels[c]!!.auline = AudioSystem.getLine(info) as SourceDataLine
                channels[c]!!.auline!!.open(tmp.format)
            } catch (e: LineUnavailableException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            var errors = false
            // Add individual volume control.
            if (channels[c]!!.auline!!.isControlSupported(FloatControl.Type.MASTER_GAIN)) channels[c]!!.vc =
                channels[c]!!.auline!!
                    .getControl(FloatControl.Type.MASTER_GAIN) as FloatControl else {
                System.err.print("MASTER_GAIN, ")
                errors = true
                if (channels[c]!!.auline!!.isControlSupported(FloatControl.Type.VOLUME)) channels[c]!!.vc =
                    channels[c]!!.auline!!
                        .getControl(FloatControl.Type.VOLUME) as FloatControl else System.err.print("VOLUME, ")
            }


            // Add individual pitch control.
            if (channels[c]!!.auline!!.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
                channels[c]!!.pc = channels[c]!!.auline!!
                    .getControl(FloatControl.Type.SAMPLE_RATE) as FloatControl
            } else {
                errors = true
                System.err.print("SAMPLE_RATE, ")
            }

            // Add individual pan control
            if (channels[c]!!.auline!!.isControlSupported(FloatControl.Type.BALANCE)) {
                channels[c]!!.bc = channels[c]!!.auline!!
                    .getControl(FloatControl.Type.BALANCE) as FloatControl
            } else {
                System.err.print("BALANCE, ")
                errors = true
                if (channels[c]!!.auline!!.isControlSupported(FloatControl.Type.PAN)) {
                    channels[c]!!.bc = channels[c]!!.auline!!
                        .getControl(FloatControl.Type.PAN) as FloatControl
                } else {
                    System.err.print("PANNING ")
                }
            }
            if (errors) System.err.printf("for channel %d NOT supported!\n", c)
            channels[c]!!.auline!!.start()
        }
    }

    /* UNUSED version, designed to work on any type of sample (in theory).
	   Requires a DoomSound container for separate format information.
	  
	 private final void  createDataLineForChannel(int c, DoomSound sound){
		if (channels[c].auline == null) {
        	AudioFormat format = sound.ais.getFormat();
        	DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        	try {
				channels[c].auline = (SourceDataLine) AudioSystem.getLine(info);
				channels[c].auline.open(format);
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        			// Add individual volume control.
        			if (channels[c].auline.isControlSupported(Type.MASTER_GAIN))
        				channels[c].vc=(FloatControl) channels[c].auline
        				.getControl(Type.MASTER_GAIN);
        			else {
        			System.err.printf("MASTER_GAIN for channel %d NOT supported!\n",c);
        			if (channels[c].auline.isControlSupported(Type.VOLUME))
            				channels[c].vc=(FloatControl) channels[c].auline
            				.getControl(Type.VOLUME);
        			else 
        				System.err.printf("VOLUME for channel %d NOT supported!\n",c);
        			} 
        			

        			// Add individual pitch control.
        			if (channels[c].auline.isControlSupported(Type.SAMPLE_RATE)){
        				channels[c].pc=(FloatControl) channels[c].auline
        				.getControl(Type.SAMPLE_RATE);
        			} else {
        				System.err.printf("SAMPLE_RATE for channel %d NOT supported!\n",c);
        			} 
        			
        			// Add individual pan control (TODO: proper positioning).
        			if (channels[c].auline.isControlSupported(Type.BALANCE)){
        				channels[c].bc=(FloatControl) channels[c].auline
        				.getControl(FloatControl.Type.BALANCE);
        			} else {
        				System.err.printf("BALANCE for channel %d NOT supported!\n",c);
        				if (channels[c].auline.isControlSupported(Type.PAN)){        					
        				channels[c].bc=(FloatControl) channels[c].auline
        				.getControl(FloatControl.Type.PAN);
        			} else {
        				System.err.printf("PAN for channel %d NOT supported!\n",c);
        			}
        			}

        			channels[c].auline.start();
        		}
	}
	*/
    override fun addsfx(sfxid: Int, volume: Int, pitch: Int, seperation: Int): Int {
        var seperation = seperation
        var i: Int
        var rc = -1
        var oldest = DM.gametic
        var oldestnum = 0
        val slot: Int
        val rightvol: Int
        val leftvol: Int

        // Chainsaw troubles.
        // Play these sound effects only one at a time.
        if (sfxid == sfxenum_t.sfx_sawup.ordinal || sfxid == sfxenum_t.sfx_sawidl.ordinal || sfxid == sfxenum_t.sfx_sawful.ordinal || sfxid == sfxenum_t.sfx_sawhit.ordinal || sfxid == sfxenum_t.sfx_stnmov.ordinal || sfxid == sfxenum_t.sfx_pistol.ordinal) {
            // Loop all channels, check.
            i = 0
            while (i < numChannels) {

                // Active, and using the same SFX?
                if (channels[i]!!.isPlaying() && channelids[i] == sfxid) {
                    // Reset.
                    channels[i]!!.stopSound()
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
        // Set pointer to raw data.

        // Create a dataline for the "lucky" channel,
        // or reuse an existing one if it exists.
        createDataLineForChannel(slot, sfxid)

        // Reset current handle number, limited to 0..100.
        if (handlenums.toInt() == 0) // was !handlenums, so it's actually 1...100?
            handlenums = ISoundDriver.MAXHANDLES.toShort()

        // Assign current handle number.
        // Preserved so sounds could be stopped (unused).
        rc = handlenums--.toInt()
        channelhandles[slot] = rc
        channelstart[slot] = DM.gametic

        // Separation, that is, orientation/stereo.
        //  range is: 1 - 256
        seperation += 1

        // Per left/right channel.
        //  x^2 seperation,
        //  adjust volume properly.
        leftvol = volume - (volume * seperation * seperation shr 16) ///(256*256);
        seperation = seperation - 257
        rightvol = volume - (volume * seperation * seperation shr 16)


        // Sanity check, clamp volume.
        if (rightvol < 0 || rightvol > 127) DM.doomSystem.Error("rightvol out of bounds")
        if (leftvol < 0 || leftvol > 127) DM.doomSystem.Error("leftvol out of bounds")

        // Preserve sound SFX id,
        //  e.g. for avoiding duplicates of chainsaw.
        channelids[slot] = sfxid
        channels[slot]!!.setVolume(volume)
        channels[slot]!!.setPanning(seperation + 256)
        channels[slot]!!.addSound(cachedSounds[sfxid].data, handlenums.toInt())
        channels[slot]!!.setPitch(pitch)
        if (AbstractSoundDriver.D) System.err.println(channelStatus())
        if (AbstractSoundDriver.D) System.err.printf("Playing %d vol %d on channel %d\n", rc, volume, slot)
        // You tell me.
        return rc
    }

    override fun StopSound(handle: Int) {
        // Which channel has it?
        val hnd = getChannelFromHandle(handle)
        if (hnd >= 0) channels[hnd]!!.stopSound()
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
            channels[i]!!.setVolume(vol)
            channels[i]!!.setPitch(pitch)
            channels[i]!!.setPanning(sep)
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

    /** A Thread for playing digital sound effects.
     *
     * Obviously you need as many as channels?
     *
     * In order not to end up in a hell of effects,
     * certain types of sounds must be limited to 1 per object.
     *
     */
    private inner class SoundWorker(var id: Int) : Runnable {
        var wait // Holds the worker still until there's a new sound
                : Semaphore
        var vc // linear volume control
                : FloatControl? = null
        var bc // balance/panning control
                : FloatControl? = null
        var pc // pitch control
                : FloatControl? = null
        var currentSoundSync: ByteArray? = null
        var currentSound: ByteArray? = null

        /** Used to find out whether the same object is continuously making
         * sounds. E.g. the player, ceilings etc. In that case, they must be
         * interrupted.
         */
        var handle: Int
        var terminate = false
        var auline: SourceDataLine? = null

        init {
            handle = ISoundDriver.IDLE_HANDLE
            wait = Semaphore(1)
        }

        /** This is how you tell the thread to play a sound,
         * I suppose.   */
        fun addSound(ds: ByteArray?, handle: Int) {
            if (AbstractSoundDriver.D) System.out.printf("Added handle %d to channel %d\n", handle, id)
            this.handle = handle
            currentSound = ds
            auline!!.stop()
            auline!!.start()
            wait.release()
        }

        /** Accepts volume in "Doom" format (0-127).
         *
         * @param volume
         */
        fun setVolume(volume: Int) {
            if (vc != null) {
                if (vc!!.type === FloatControl.Type.MASTER_GAIN) {
                    val vol = linear2db[volume]
                    vc!!.value = vol
                } else if (vc!!.type === FloatControl.Type.VOLUME) {
                    val vol = vc!!.minimum + (vc!!.maximum - vc!!.minimum) * volume.toFloat() / 127f
                    vc!!.value = vol
                }
            }
        }

        fun setPanning(sep: Int) {
            // Q: how does Doom's sep map to stereo panning?
            // A: Apparently it's 0-255 L-R.
            if (bc != null) {
                val pan: Float =
                    bc!!.minimum + (bc!!.maximum - bc!!.minimum) * sep.toFloat() / ISoundDriver.PANNING_STEPS
                //System.err.printf("Panning %d %f %f %f\n",sep,bc.getMinimum(),bc.getMaximum(),pan);
                bc!!.value = pan
            }
        }

        /** Expects a steptable value between 16K and 256K, with
         * 64K being the middle.
         *
         * @param pitch
         */
        fun setPitch(pitch: Int) {
            if (pc != null) {
                val pan = (pc!!.value * (pitch.toFloat() / 65536.0)).toFloat()
                pc!!.value = pan
            }
        }

        override fun run() {
            System.err.printf("Sound thread %d started\n", id)
            while (!terminate) {
                currentSoundSync = currentSound
                if (currentSoundSync != null) {
                    try {
                        auline!!.write(currentSoundSync, 0, currentSoundSync!!.size)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return
                    } finally {
                        // The previous steps are actually VERY fast.
                        // However this one waits until the data has been
                        // consumed, Interruptions/signals won't reach  here,
                        // so it's pointless trying to interrupt the actual filling.
                        //long a=System.nanoTime();
                        auline!!.drain()
                        //long b=System.nanoTime();
                        //System.out.printf("Channel %d completed in %f.\n",id,(float)(b-a)/1000000000f);
                    }
                    // Report that this channel is free.
                    currentSound = null
                    // Remove its handle.

                    //System.out.printf("Channel  %d with handle %d done. Marking as free\n",id,handle);
                    if (handle > 0) channelhandles[id] = ISoundDriver.IDLE_HANDLE
                    handle = ISoundDriver.IDLE_HANDLE
                }

                // If we don't sleep at least a bit here, busy waiting becomes
                // way too taxing. Waiting on a semaphore (triggered by adding a new sound)
                // seems like a better method.
                try {
                    wait.acquire()
                } catch (e: InterruptedException) {
                }
            }
        }

        fun stopSound() {
            auline!!.stop()
            auline!!.flush()
            //System.out.printf("Channel %d with handle %d interrupted. Marking as free\n",id,handle);
            channelhandles[id] = ISoundDriver.IDLE_HANDLE
            handle = ISoundDriver.IDLE_HANDLE
            currentSound = null
            auline!!.start()
        }

        fun isPlaying(): Boolean {
            //System.out.printf("Channel %d with handle %d queried\n",id,handle);
            return handle != ISoundDriver.IDLE_HANDLE || currentSound != null
        }
    }

    var sb = StringBuilder()

    init {
        linear2db = computeLinear2DB()
    }

    fun channelStatus(): String {
        sb.setLength(0)
        for (i in 0 until numChannels) {
            if (channels[i]!!.isPlaying()) sb.append(i) else sb.append('-')
        }
        return sb.toString()
    }
}