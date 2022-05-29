package s

import data.sfxinfo_t
import data.sounds.sfxenum_t
import doom.CVarManager
import doom.CommandVariable
import doom.DoomMain

//-----------------------------------------------------------------------------
//
// $Id: ISoundDriver.java,v 1.1 2012/11/08 17:12:42 velktron Exp $
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
//
// DESCRIPTION:
// System interface, sound. Anything implementation-specific should
// implement this.
//
//-----------------------------------------------------------------------------
interface ISoundDriver {
    /** Init at program start. Return false if device invalid,
     * so that caller can decide best course of action.
     * The suggested one is to swap the sound "driver" for a dummy.
     *
     * @return
     */
    fun InitSound(): Boolean

    // ... update sound buffer and audio device at runtime...
    fun UpdateSound()
    fun SubmitSound()

    // ... shut down and relase at program termination.
    fun ShutdownSound()

    //
    //  SFX I/O
    //
    // Initialize channels?
    fun SetChannels(numChannels: Int)

    // Get raw data lump index for sound descriptor.
    fun GetSfxLumpNum(sfxinfo: sfxinfo_t): Int

    // Starts a sound in a particular sound channel.
    fun StartSound(
        id: Int,
        vol: Int,
        sep: Int,
        pitch: Int,
        priority: Int
    ): Int

    // Stops a sound channel.
    fun StopSound(handle: Int)

    /** Called by S_*() functions to see if a channel is still playing.
     * Returns false if no longer playing, true if playing. This is
     * a relatively "high level" function, so its accuracy relies on
     * what the "system specific" sound code reports back  */
    fun SoundIsPlaying(handle: Int): Boolean

    /* Updates the volume, separation,
	   and pitch of a sound channel. */
    fun UpdateSoundParams(
        handle: Int,
        vol: Int,
        sep: Int,
        pitch: Int
    )

    companion object {
        const val VOLUME_STEPS = 128
        const val PANNING_STEPS = 256
        const val IDLE_HANDLE = -1
        const val BUSY_HANDLE = -2

        // Needed for calling the actual sound output
        // We mix 1024 samples each time, but we only call UpdateSound()
        // 1 time out of three.
        const val NUM_CHANNELS = 8

        // It is 2 for 16bit, and 2 for two channels.
        const val BUFMUL = 4
        const val SAMPLERATE = 22050 // Hz

        // Update all 30 millisecs, approx. 30fps synchronized.
        // Linux resolution is allegedly 10 millisecs,
        //  scale is microseconds.
        const val SOUND_INTERVAL = 500

        /** Yes, it's possible to select a different sound frame rate  */
        const val SND_FRAME_RATE = 21

        // Was 512, but if you mix that many samples per tic you will
        // eventually outrun the buffer :-/ I fail to see the mathematical
        // justification behind this, unless they simply wanted the buffer to 
        // be a nice round number in size.
        val SAMPLECOUNT: Int = ISoundDriver.SAMPLERATE / ISoundDriver.SND_FRAME_RATE
        val MIXBUFFERSIZE: Int = ISoundDriver.SAMPLECOUNT * ISoundDriver.BUFMUL
        const val SAMPLESIZE = 16 // 16bit
        val NUMSFX = sfxenum_t.NUMSFX.ordinal
        const val MAXHANDLES = 100

        /** How many audio chunks/frames to mix before submitting them to
         * the output.
         */
        const val BUFFER_CHUNKS = 5

        /** Ths audio buffer size of the audioline itself.
         * Increasing this is the only effective way to combat output stuttering on
         * slower machines.
         */
        val AUDIOLINE_BUFFER: Int = 2 * ISoundDriver.BUFFER_CHUNKS * ISoundDriver.MIXBUFFERSIZE
        val SOUND_PERIOD: Int = 1000 / ISoundDriver.SND_FRAME_RATE // in ms
        fun chooseModule(DM: DoomMain<*, *>, CVM: CVarManager): ISoundDriver {
            val driver: ISoundDriver
            driver = if (CVM.bool(CommandVariable.NOSFX) || CVM.bool(CommandVariable.NOSOUND)) {
                DummySFX()
            } else {
                // Switch between possible sound drivers.
                if (CVM.bool(CommandVariable.AUDIOLINES)) { // Crudish.
                    DavidSFXModule(DM, DM.numChannels)
                } else if (CVM.bool(CommandVariable.SPEAKERSOUND)) { // PC Speaker emulation
                    SpeakerDoomSoundDriver(DM, DM.numChannels)
                } else if (CVM.bool(CommandVariable.CLIPSOUND)) {
                    ClipSFXModule(DM, DM.numChannels)
                } else if (CVM.bool(CommandVariable.CLASSICSOUND)) { // This is the default
                    ClassicDoomSoundDriver(DM, DM.numChannels)
                } else { // This is the default
                    SuperDoomSoundDriver(DM, DM.numChannels)
                }
            }
            // Check for sound init failure and revert to dummy
            if (!driver.InitSound()) {
                System.err.println("S_InitSound: failed. Reverting to dummy...\n")
                return DummySFX()
            }
            return driver
        }
    }
}