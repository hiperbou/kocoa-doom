package s


import data.*
import data.sounds.musicenum_t
import data.sounds.sfxenum_t
import doom.DoomMain
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FixedMul
import p.mobj_t
import rr.RendererState

/** Some stuff that is not implementation dependant
 * This includes channel management, sound priorities,
 * positioning, distance attenuation etc. It's up to
 * lower-level "drivers" to actually implements those.
 * This particular class needs not be a dummy itself, but
 * the drivers it "talks" to might be.
 *
 *
 */
class AbstractDoomAudio(protected val DS: DoomMain<*, *>, protected val numChannels: Int) : IDoomSound {
    protected val IMUS: IMusic
    protected val ISND: ISoundDriver

    /** the set of channels available. These are "soft" descriptor
     * channels,  not to be confused with actual hardware audio
     * lines, which are an entirely different concern.
     *
     */
    protected val channels: Array<IDoomSound.channel_t?>

    // These are not used, but should be (menu).
    // Maximum volume of a sound effect.
    // Internal default is max out of 0-15.
    protected var snd_SfxVolume = 15

    // Maximum volume of music. Useless so far.
    protected var snd_MusicVolume = 15

    // whether songs are mus_paused
    protected var mus_paused = false

    // music currently being played
    protected var mus_playing: musicinfo_t? = null
    protected var nextcleanup = 0

    /** Volume, pitch, separation  & priority packed for parameter passing  */
    protected inner class vps_t {
        var volume = 0
        var pitch = 0
        var sep = 0
        var priority = 0
    }

    /**
     * Initializes sound stuff, including volume
     * Sets channels, SFX and music volume,
     * allocates channel buffer, sets S_sfx lookup.
     */
    override fun Init(
        sfxVolume: Int,
        musicVolume: Int
    ) {
        var i: Int
        System.err.printf("S_Init: default sfx volume %d\n", sfxVolume)
        snd_SfxVolume = sfxVolume
        snd_MusicVolume = musicVolume
        // Whatever these did with DMX, these are rather dummies now.
        // MAES: any implementation-dependant channel setup should start here.
        ISND.SetChannels(numChannels)
        SetSfxVolume(sfxVolume)
        // No music with Linux - another dummy.
        // MAES: these must be initialized somewhere, perhaps not here?
        IMUS.SetMusicVolume(musicVolume)

        // Allocating the internal channels for mixing
        // (the maximum numer of sounds rendered
        // simultaneously) within zone memory.
        // MAES: already done that in the constructor.

        // Free all channels for use
        i = 0
        while (i < numChannels) {
            channels[i] = IDoomSound.channel_t()
            i++
        }

        // no sounds are playing, and they are not mus_paused
        mus_paused = false

        // Note that sounds have not been cached (yet).
        i = 1
        while (i < sounds.S_sfx.size) {
            sounds.S_sfx[i].usefulness = -1
            sounds.S_sfx[i].lumpnum = sounds.S_sfx[i].usefulness
            i++
        }
    }

    //
    // Per level startup code.
    // Kills playing sounds at start of level,
    //  determines music if any, changes music.
    //
    override fun Start() {
        var cnum: Int
        val mnum: Int

        // kill all playing sounds at start of level
        //  (trust me - a good idea)
        cnum = 0
        while (cnum < numChannels) {
            if (channels[cnum]!!.sfxinfo != null) StopChannel(cnum)
            cnum++
        }

        // start new music for the level
        mus_paused = false
        mnum = if (DS.isCommercial()) musicenum_t.mus_runnin.ordinal + DS.gamemap - 1 else {
            val spmus = arrayOf( // Song - Who? - Where?
                musicenum_t.mus_e3m4,  // American	e4m1
                musicenum_t.mus_e3m2,  // Romero	e4m2
                musicenum_t.mus_e3m3,  // Shawn	e4m3
                musicenum_t.mus_e1m5,  // American	e4m4
                musicenum_t.mus_e2m7,  // Tim 	e4m5
                musicenum_t.mus_e2m4,  // Romero	e4m6
                musicenum_t.mus_e2m6,  // J.Anderson	e4m7 CHIRON.WAD
                musicenum_t.mus_e2m5,  // Shawn	e4m8
                musicenum_t.mus_e1m9 // Tim		e4m9
            )
            if (DS.gameepisode < 4) musicenum_t.mus_e1m1.ordinal + (DS.gameepisode - 1) * 9 + DS.gamemap - 1 else spmus[DS.gamemap - 1].ordinal
        }

        // HACK FOR COMMERCIAL
        //  if (commercial && mnum > mus_e3m9)	
        //      mnum -= mus_e3m9;
        ChangeMusic(mnum, true)
        nextcleanup = 15
    }

    private val vps = vps_t()

    init {
        channels = arrayOfNulls(numChannels)
        IMUS = DS.music
        ISND = DS.soundDriver
    }

    override fun StartSoundAtVolume(
        origin_p: ISoundOrigin?,
        sfx_id: Int,
        volume: Int
    ) {
        var volume = volume
        val rc: Boolean
        var sep = 0 // This is set later.
        var pitch: Int
        val priority: Int
        val sfx: sfxinfo_t
        val cnum: Int
        val origin = origin_p


        // Debug.

        //if (origin!=null && origin.type!=null)
        // System.err.printf(
        //   "S_StartSoundAtVolume: playing sound %d (%s) from %s %d\n",
        //  sfx_id, S_sfx[sfx_id].name , origin.type.toString(),origin.hashCode());


        // check for bogus sound #
        if (sfx_id < 1 || sfx_id > IDoomSound.NUMSFX) {
            val e = Exception()
            e.printStackTrace()
            DS.doomSystem.Error("Bad sfx #: %d", sfx_id)
        }
        sfx = sounds.S_sfx[sfx_id]

        // Initialize sound parameters
        if (sfx._link != null) {
            pitch = sfx.pitch
            priority = sfx.priority
            volume += sfx.volume
            if (volume < 1) return
            if (volume > snd_SfxVolume) volume = snd_SfxVolume
        } else {
            pitch = IDoomSound.NORM_PITCH
            priority = IDoomSound.NORM_PRIORITY
        }


        // Check to see if it is audible,
        //  and if not, modify the params
        if (origin != null && origin !== DS.players[DS.consoleplayer].mo) {
            vps.volume = volume
            vps.pitch = pitch
            vps.sep = sep
            rc = AdjustSoundParams(
                DS.players[DS.consoleplayer].mo!!,
                origin, vps
            )
            volume = vps.volume
            pitch = vps.pitch
            sep = vps.sep
            if (origin.getX() == DS.players[DS.consoleplayer].mo!!._x
                && origin.getY() == DS.players[DS.consoleplayer].mo!!._y
            ) {
                sep = IDoomSound.NORM_SEP
            }
            if (!rc) {
                //System.err.printf("S_StartSoundAtVolume: Sound %d (%s) rejected because: inaudible\n",
                //   sfx_id, S_sfx[sfx_id].name );
                return
            }
        } else {
            sep = IDoomSound.NORM_SEP
        }

        // hacks to vary the sfx pitches
        if (sfx_id >= sfxenum_t.sfx_sawup.ordinal
            && sfx_id <= sfxenum_t.sfx_sawhit.ordinal
        ) {
            pitch += 8 - (DS.random.M_Random() and 15)
            if (pitch < 0) pitch = 0 else if (pitch > 255) pitch = 255
        } else if (sfx_id != sfxenum_t.sfx_itemup.ordinal
            && sfx_id != sfxenum_t.sfx_tink.ordinal
        ) {
            pitch += 16 - (DS.random.M_Random() and 31)
            if (pitch < 0) pitch = 0 else if (pitch > 255) pitch = 255
        }

        // kill old sound
        StopSound(origin)

        // try to find a channel
        cnum = getChannel(origin, sfx)
        if (cnum < 0) return

        //
        // This is supposed to handle the loading/caching.
        // For some odd reason, the caching is done nearly
        //  each time the sound is needed?
        //

        // get lumpnum if necessary
        if (sfx.lumpnum < 0) // Now, it crosses into specific territory.
            sfx.lumpnum = ISND.GetSfxLumpNum(sfx)

        /*
	#ifndef SNDSRV
	  // cache data if necessary
	  if (!sfx->data)
	  {
	    fprintf( stderr,
		     "S_StartSoundAtVolume: 16bit and not pre-cached - wtf?\n");

	    // DOS remains, 8bit handling
	    //sfx->data = (void *) W_CacheLumpNum(sfx->lumpnum, PU_MUSIC);
	    // fprintf( stderr,
	    //	     "S_StartSoundAtVolume: loading %d (lump %d) : 0x%x\n",
	    //       sfx_id, sfx->lumpnum, (int)sfx->data );

	  }
	#endif */

        // increase the usefulness
        if (sfx.usefulness++ < 0) sfx.usefulness = 1

        // Assigns the handle to one of the channels in the
        //  mix/output buffer. This is when things actually
        // become hard (pun intended).
        // TODO: which channel? How do we know how the actual hardware 
        // ones map with the "soft" ones?
        // Essentially we're begging to get an actual channel.		
        channels[cnum]!!.handle = ISND.StartSound(
            sfx_id,  /*sfx->data,*/
            volume,
            sep,
            pitch,
            priority
        )
        if (AbstractDoomAudio.D) System.err.printf(
            "Handle %d for channel %d for sound %s vol %d sep %d\n", channels[cnum]!!.handle,
            cnum, sfx.name, volume, sep
        )
    }

    override fun StartSound(
        origin: ISoundOrigin?,
        sfx_id: sfxenum_t?
    ) {
        //  MAES: necessary sanity check at this point.
        if (sfx_id != null && sfx_id.ordinal > 0) StartSound(origin, sfx_id.ordinal)
    }

    override fun StartSound(
        origin: ISoundOrigin?,
        sfx_id: Int
    ) {
        /* #ifdef SAWDEBUG
	    // if (sfx_id == sfx_sawful)
	    // sfx_id = sfx_itemup;
	#endif */
        StartSoundAtVolume(origin, sfx_id, snd_SfxVolume)


        // UNUSED. We had problems, had we not?
        /* #ifdef SAWDEBUG
	{
	    int i;
	    int n;

	    static mobj_t*      last_saw_origins[10] = {1,1,1,1,1,1,1,1,1,1};
	    static int		first_saw=0;
	    static int		next_saw=0;

	    if (sfx_id == sfx_sawidl
		|| sfx_id == sfx_sawful
		|| sfx_id == sfx_sawhit)
	    {
		for (i=first_saw;i!=next_saw;i=(i+1)%10)
		    if (last_saw_origins[i] != origin)
			fprintf(stderr, "old origin 0x%lx != "
				"origin 0x%lx for sfx %d\n",
				last_saw_origins[i],
				origin,
				sfx_id);

		last_saw_origins[next_saw] = origin;
		next_saw = (next_saw + 1) % 10;
		if (next_saw == first_saw)
		    first_saw = (first_saw + 1) % 10;

		for (n=i=0; i<numChannels ; i++)
		{
		    if (channels[i].sfxinfo == &S_sfx[sfx_sawidl]
			|| channels[i].sfxinfo == &S_sfx[sfx_sawful]
			|| channels[i].sfxinfo == &S_sfx[sfx_sawhit]) n++;
		}

		if (n>1)
		{
		    for (i=0; i<numChannels ; i++)
		    {
			if (channels[i].sfxinfo == &S_sfx[sfx_sawidl]
			    || channels[i].sfxinfo == &S_sfx[sfx_sawful]
			    || channels[i].sfxinfo == &S_sfx[sfx_sawhit])
			{
			    fprintf(stderr,
				    "chn: sfxinfo=0x%lx, origin=0x%lx, "
				    "handle=%d\n",
				    channels[i].sfxinfo,
				    channels[i].origin,
				    channels[i].handle);
			}
		    }
		    fprintf(stderr, "\n");
		}
	    }
	}
	#endif*/
    }

    // This one is public.
    override fun StopSound(origin: ISoundOrigin?) {
        var cnum: Int
        cnum = 0
        while (cnum < numChannels) {
            if (channels[cnum]!!.sfxinfo != null && channels[cnum]!!.origin === origin) {
                // This one is not.
                StopChannel(cnum)
                break
            }
            cnum++
        }
    }

    //
    // Stop and resume music, during game PAUSE.
    //
    override fun PauseSound() {
        if (mus_playing != null && !mus_paused) {
            IMUS.PauseSong(mus_playing!!.handle)
            mus_paused = true
        }
    }

    override fun ResumeSound() {
        if (mus_playing != null && mus_paused) {
            IMUS.ResumeSong(mus_playing!!.handle)
            mus_paused = false
        }
    }

    override fun UpdateSounds(listener: mobj_t) {
        var audible: Boolean
        var cnum: Int
        //int		volume;
        //int		sep;
        //int		pitch;
        var sfx: sfxinfo_t
        var c: IDoomSound.channel_t?

        // Clean up unused data.
        // This is currently not done for 16bit (sounds cached static).
        // DOS 8bit remains. 
        /*if (gametic.nextcleanup)
		    {
			for (i=1 ; i<NUMSFX ; i++)
			{
			    if (S_sfx[i].usefulness < 1
				&& S_sfx[i].usefulness > -1)
			    {
				if (--S_sfx[i].usefulness == -1)
				{
				    Z_ChangeTag(S_sfx[i].data, PU_CACHE);
				    S_sfx[i].data = 0;
				}
			    }
			}
			nextcleanup = gametic + 15;
		    }*/cnum = 0
        while (cnum < numChannels) {
            c = channels[cnum]!!
            //sfx = c!!.sfxinfo!!

            //System.out.printf("Updating channel %d %s\n",cnum,c);
            if (c.sfxinfo != null) {
                if (ISND.SoundIsPlaying(c.handle)) {
                    // initialize parameters
                    vps.volume = snd_SfxVolume
                    vps.pitch = IDoomSound.NORM_PITCH
                    vps.sep = IDoomSound.NORM_SEP
                    sfx = c.sfxinfo!!
                    if (sfx._link != null) {
                        vps.pitch = sfx.pitch
                        vps.volume += sfx.volume
                        if (vps.volume < 1) {
                            StopChannel(cnum)
                            cnum++
                            continue
                        } else if (vps.volume > snd_SfxVolume) {
                            vps.volume = snd_SfxVolume
                        }
                    }

                    // check non-local sounds for distance clipping
                    //  or modify their params
                    if (c.origin != null && listener !== c.origin) {
                        audible = AdjustSoundParams(
                            listener,
                            c.origin!!,
                            vps
                        )
                        if (!audible) {
                            StopChannel(cnum)
                        } else ISND.UpdateSoundParams(c.handle, vps.volume, vps.sep, vps.pitch)
                    }
                } else {
                    // if channel is allocated but sound has stopped,
                    //  free it
                    StopChannel(cnum)
                }
            }
            cnum++
        }
        // kill music if it is a single-play && finished
        // if (	mus_playing
        //      && !I_QrySongPlaying(mus_playing->handle)
        //      && !mus_paused )
        // S_StopMusic();
    }

    override fun SetMusicVolume(volume: Int) {
        if (volume < 0 || volume > 127) {
            DS.doomSystem.Error(
                "Attempt to set music volume at %d",
                volume
            )
        }
        IMUS.SetMusicVolume(volume)
        snd_MusicVolume = volume
    }

    override fun SetSfxVolume(volume: Int) {
        if (volume < 0 || volume > 127) DS.doomSystem.Error("Attempt to set sfx volume at %d", volume)
        snd_SfxVolume = volume
    }

    //
    // Starts some music with the music id found in sounds.h.
    //
    override fun StartMusic(m_id: Int) {
        ChangeMusic(m_id, false)
    }

    //
    // Starts some music with the music id found in sounds.h.
    //
    override fun StartMusic(m_id: musicenum_t) {
        ChangeMusic(m_id.ordinal, false)
    }

    override fun ChangeMusic(
        musicnum: musicenum_t,
        looping: Boolean
    ) {
        ChangeMusic(musicnum.ordinal, false)
    }

    override fun ChangeMusic(
        musicnum: Int,
        looping: Boolean
    ) {
        var music: musicinfo_t? = null
        val namebuf: String
        if (musicnum <= musicenum_t.mus_None.ordinal || musicnum >= musicenum_t.NUMMUSIC.ordinal) {
            DS.doomSystem.Error("Bad music number %d", musicnum)
        } else music = sounds.S_music[musicnum]
        if (mus_playing === music) return

        // shutdown old music
        StopMusic()

        // get lumpnum if neccessary
        if (music!!.lumpnum == 0) {
            namebuf = String.format("d_%s", music.name)
            music.lumpnum = DS.wadLoader.GetNumForName(namebuf)
        }

        // load & register it
        music.data = DS.wadLoader.CacheLumpNumAsRawBytes(music.lumpnum, Defines.PU_MUSIC)
        music.handle = IMUS.RegisterSong(music.data)

        // play it
        IMUS.PlaySong(music.handle, looping)
        SetMusicVolume(snd_MusicVolume)
        mus_playing = music
    }

    override fun StopMusic() {
        if (mus_playing != null) {
            if (mus_paused) IMUS.ResumeSong(mus_playing!!.handle)
            IMUS.StopSong(mus_playing!!.handle)
            IMUS.UnRegisterSong(mus_playing!!.handle)
            //Z_ChangeTag(mus_playing->data, PU_CACHE);
            mus_playing!!.data = null
            mus_playing = null
        }
    }

    /** This is S_StopChannel. There's another StopChannel
     * with a similar contract in ISound. Don't confuse the two.
     *
     *
     *
     * @param cnum
     */
    protected fun StopChannel(cnum: Int) {
        var i: Int
        val c = channels[cnum]

        // Is it playing?
        if (c!!.sfxinfo != null) {
            // stop the sound playing
            if (ISND.SoundIsPlaying(c.handle)) {
                /*#ifdef SAWDEBUG
		    if (c.sfxinfo == &S_sfx[sfx_sawful])
			fprintf(stderr, "stopped\n");
	#endif*/
                ISND.StopSound(c.handle)
            }

            // check to see
            //  if other channels are playing the sound
            i = 0
            while (i < numChannels) {
                if (cnum != i
                    && c.sfxinfo === channels[i]!!.sfxinfo
                ) {
                    break
                }
                i++
            }

            // degrade usefulness of sound data
            c.sfxinfo!!.usefulness--
            c.sfxinfo = null
        }
    }

    //
    // Changes volume, stereo-separation, and pitch variables
    //  from the norm of a sound effect to be played.
    // If the sound is not audible, returns a 0.
    // Otherwise, modifies parameters and returns 1.
    //
    protected fun AdjustSoundParams(
        listener: mobj_t,
        source: ISoundOrigin,
        vps: vps_t
    ): Boolean {
        var approx_dist: Int
        val adx: Int
        val ady: Int
        var angle: Long

        // calculate the distance to sound origin
        //  and clip it if necessary
        adx = Math.abs(listener._x - source.getX())
        ady = Math.abs(listener._y - source.getY())

        // From _GG1_ p.428. Appox. eucledian distance fast.
        approx_dist = adx + ady - ((if (adx < ady) adx else ady) shr 1)
        if (DS.gamemap != 8
            && approx_dist > IDoomSound.S_CLIPPING_DIST
        ) {
            return false
        }

        // angle of source to listener
        angle = RendererState.PointToAngle(
            listener._x,
            listener._y,
            source.getX(),
            source.getY()
        )
        angle =
            if (angle > listener.angle) angle - listener.angle else angle + (0xffffffffL - listener.angle and Tables.BITS32)
        angle = angle and Tables.BITS32
        angle = angle shr Tables.ANGLETOFINESHIFT

        // stereo separation
        vps.sep = 128 - (FixedMul(
            IDoomSound.S_STEREO_SWING,
            Tables.finesine[angle.toInt()]
        ) shr FRACBITS)

        // volume calculation
        if (approx_dist < IDoomSound.S_CLOSE_DIST) {
            vps.volume = snd_SfxVolume
        } else if (DS.gamemap == 8) {
            if (approx_dist > IDoomSound.S_CLIPPING_DIST) approx_dist = IDoomSound.S_CLIPPING_DIST
            vps.volume = 15 + ((snd_SfxVolume - 15) * (IDoomSound.S_CLIPPING_DIST - approx_dist shr FRACBITS)) / IDoomSound.S_ATTENUATOR
        } else {
            // distance effect
            vps.volume = ((snd_SfxVolume
                    * (IDoomSound.S_CLIPPING_DIST - approx_dist shr FRACBITS))
                    / IDoomSound.S_ATTENUATOR)
            // Let's do some maths here: S_CLIPPING_DIST-approx_dist
            // can be at most 0x04100000. shifting left means 0x0410,
            // or 1040 in decimal. 
            // The unmultiplied max volume is 15, attenuator is 1040.
            // So snd_SfxVolume should be 0-127.
        }

        // MAES: pitch calculation for doppler effects. Nothing to write
        // home about.

        /*
		
		// calculate the relative speed between source and sound origin.
		//  and clip it if necessary
		adx = Math.abs(listener.momx - source.momx);
		ady = Math.abs(listener.momy - source.momy);
			
		// From _GG1_ p.428. Appox. eucledian distance fast.
		// Here used for "approximate speed"
		approx_dist = adx + ady - ((adx < ady ? adx : ady)>>1);
		
		// The idea is that for low speeds, no doppler effect occurs.
		// For higher ones however, a shift occurs. We don't want this
		// to be annoying, so we'll only apply it for large speed differences
		// Then again, Doomguy can sprint like Carl Lewis...
			
		if (approx_dist>0x100000){
		
		// Quickly decide sign of pitch based on speed vectors
			
			// angle of source (speed) to listener (speed)
			angle = rr.RendererState.PointToAngle(listener.momx,
					listener.momy,
					source.momx,
					source.momy);
			
			if ((0<=angle && angle<=Tables.ANG90)||
				(180<=angle && angle<=Tables.ANG270))
		vps.pitch+=(approx_dist>>16);
			else
		vps.pitch-=(approx_dist>>16);
		}

		if (vps.pitch<0) vps.pitch=0;
		if (vps.pitch>255) vps.pitch=255;
		*/return vps.volume > 0
    }

    //
    // S_getChannel :
    //   If none available, return -1.  Otherwise channel #.
    //
    protected fun getChannel(origin: ISoundOrigin?, sfxinfo: sfxinfo_t): Int {
        // channel number to use
        var cnum: Int
        val c: IDoomSound.channel_t?

        // Find an open channel
        // If it's null, OK, use that.
        // If it's an origin-specific sound and has the same origin, override.
        cnum = 0
        while (cnum < numChannels) {
            if (channels[cnum]!!.sfxinfo == null) break else if (origin != null && channels[cnum]!!.origin === origin) {
                StopChannel(cnum)
                break
            }
            cnum++
        }

        // None available
        if (cnum == numChannels) {
            // Look for lower priority
            cnum = 0
            while (cnum < numChannels) {
                if (channels[cnum]!!.sfxinfo!!.priority >= sfxinfo.priority) break
                cnum++
            }
            if (cnum == numChannels) {
                // FUCK!  No lower priority.  Sorry, Charlie.
                return -1
            } else {
                // Otherwise, kick out lower priority.
                StopChannel(cnum)
            }
        }
        c = channels[cnum]

        // channel is decided to be cnum.
        c!!.sfxinfo = sfxinfo
        c.origin = origin
        return cnum
    }

    /** Nice one. A sound should have a maximum duration in tics,
     * and we can give it a handle proportional to the future tics
     * it should play until. Ofc, this means the minimum timeframe
     * for cutting a sound off is just 1 tic.
     *
     * @param handle
     * @return
     */
    /*
	public boolean SoundIsPlaying(int handle)
	{
	    // Ouch.
	    return (DS.gametic < handle);
	} */
    companion object {
        protected const val D = false
    }
}