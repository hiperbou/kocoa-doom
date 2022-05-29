package s

import data.sfxinfo_t
import data.sounds.musicenum_t
import data.sounds.sfxenum_t
import doom.CVarManager
import doom.CommandVariable
import doom.DoomMain
import m.fixed_t.Companion.FRACBITS
import p.mobj_t

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: IDoomSound.java,v 1.5 2011/08/24 15:55:12 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// DESCRIPTION:
//	The not so system specific sound interface (s_sound.*)
// Anything high-level like e.g. handling of panning, sound origin,
// sound multiplicity etc. should be handled here, but not e.g. actual
// sound playback, sound threads, etc.
//  That's the job of ISound and IMusic (roughly equivelnt to i_sound.*, but
// with separate concerns for SFX and MUSIC.
//
//-----------------------------------------------------------------------------
interface IDoomSound {
    class channel_t {
        // sound information (if null, channel avail.)
        var sfxinfo: sfxinfo_t? = null

        // origin of sound
        var origin: ISoundOrigin? = null

        // handle of the sound being played
        var handle = 0
    }

    /**
     * Initializes sound stuff, including volume Sets channels, SFX and music
     * volume, allocates channel buffer, sets S_sfx lookup.
     */
    fun Init(sfxVolume: Int, musicVolume: Int)

    /**
     * Per level startup code. Kills playing sounds at start of level,
     * determines music if any, changes music.
     */
    fun Start()

    /**
     * Start sound for thing at <origin> using <sound_id> from sounds.h
    </sound_id></origin> */
    fun StartSound(origin: ISoundOrigin?, sound_id: Int)

    /**
     * Start sound for thing at <origin> using <sound_id> from sounds.h
     * Convenience method using sfxenum_t instead. Delegated to int version.
     *
    </sound_id></origin> */
    fun StartSound(origin: ISoundOrigin?, sound_id: sfxenum_t?)

    /** Will start a sound at a given volume.  */
    fun StartSoundAtVolume(origin: ISoundOrigin?, sound_id: Int, volume: Int)

    /** Stop sound for thing at <origin> </origin> */
    fun StopSound(origin: ISoundOrigin?)

    /**
     * Start music using <music_id> from sounds.h, and set whether looping
     *
     * @param musicnum
     * @param looping
    </music_id> */
    fun ChangeMusic(musicnum: Int, looping: Boolean)
    fun ChangeMusic(musicnum: musicenum_t, looping: Boolean)

    /** Stops the music fer sure.  */
    fun StopMusic()

    /** Stop and resume music, during game PAUSE.  */
    fun PauseSound()
    fun ResumeSound()

    /**
     * Updates music & sounds
     *
     * @param listener
     */
    fun UpdateSounds(listener: mobj_t)
    fun SetMusicVolume(volume: Int)
    fun SetSfxVolume(volume: Int)

    /** Start music using <music_id> from sounds.h </music_id> */
    fun StartMusic(music_id: Int)

    /** Start music using <music_id> from sounds.h
     * Convenience method using musicenum_t.
    </music_id> */
    fun StartMusic(music_id: musicenum_t) //

    // Internals. 
    // 
    // MAES: these appear to be only of value for internal implementation,
    // and are never called externally. Thus, they might as well
    // not be part of the interface, even though it's convenient to reuse them.
    //
    /*
	int
	S_getChannel
	( mobj_t		origin,
	  sfxinfo_t	sfxinfo );


	int
	S_AdjustSoundParams
	( mobj_t	listener,
	  mobj_t	source,
	  int		vol,
	  int		sep,
	  int		pitch );

	void S_StopChannel(int cnum);
	*/
    companion object {
        fun chooseSoundIsPresent(DM: DoomMain<*, *>, CVM: CVarManager, ISND: ISoundDriver?): IDoomSound {
            return if (!CVM.bool(CommandVariable.NOSOUND) || ISND is DummySFX && DM.music is DummyMusic) {
                AbstractDoomAudio(DM, DM.numChannels)
            } else {
                /**
                 * Saves a lot of distance calculations,
                 * if we're not to output any sound at all.
                 * TODO: create a Dummy that can handle music alone.
                 */
                DummySoundDriver()
            }
        }

        /** Convenience hack  */
        val NUMSFX = sfxenum_t.NUMSFX.ordinal

        // Purpose?
        val snd_prefixen = charArrayOf('P', 'P', 'A', 'S', 'S', 'S', 'M', 'M', 'M', 'S', 'S', 'S')
        const val S_MAX_VOLUME = 127

        // when to clip out sounds
        // Does not fit the large outdoor areas.
        const val S_CLIPPING_DIST = 1200 * 0x10000

        // Distance tp origin when sounds should be maxed out.
        // This should relate to movement clipping resolution
        // (see BLOCKMAP handling).
        // Originally: (200*0x10000).
        const val S_CLOSE_DIST = 160 * 0x10000
        val S_ATTENUATOR: Int =
            IDoomSound.S_CLIPPING_DIST - IDoomSound.S_CLOSE_DIST shr FRACBITS

        // Adjustable by menu.
        //protected final int NORM_VOLUME    		snd_MaxVolume
        const val NORM_PITCH = 128
        const val NORM_PRIORITY = 64
        const val NORM_SEP = 128
        const val S_PITCH_PERTURB = 1
        const val S_STEREO_SWING = 96 * 0x10000

        // percent attenuation from front to back
        const val S_IFRACVOL = 30
        const val NA = 0
        const val S_NUMCHANNELS = 2
    }
}