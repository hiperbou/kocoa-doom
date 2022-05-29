package i


import data.sfxinfo_t

//-----------------------------------------------------------------------------
//
// $Id: DoomSoundInterface.java,v 1.3 2011/02/11 00:11:13 velktron Exp $
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
//	System interface, sound.
//
//-----------------------------------------------------------------------------
/*
// UNIX hack, to be removed.
#ifdef SNDSERV
#include <stdio.h>
extern FILE* sndserver;
extern char* sndserver_filename;
#endif*/
interface DoomSoundInterface {
    // Init at program start...
    fun I_InitSound()

    // ... update sound buffer and audio device at runtime...
    fun I_UpdateSound()
    fun I_SubmitSound()

    // ... shut down and relase at program termination.
    fun I_ShutdownSound()

    //
    //  SFX I/O
    //
    // Initialize channels?
    fun I_SetChannels()

    // Get raw data lump index for sound descriptor.
    fun I_GetSfxLumpNum(sfxinfo: sfxinfo_t?): Int

    // Starts a sound in a particular sound channel.
    fun I_StartSound(
        id: Int,
        vol: Int,
        sep: Int,
        pitch: Int,
        priority: Int
    ): Int

    // Stops a sound channel.
    fun I_StopSound(handle: Int)

    // Called by S_*() functions
    //  to see if a channel is still playing.
    // Returns 0 if no longer playing, 1 if playing.
    fun I_SoundIsPlaying(handle: Int): Boolean

    // Updates the volume, separation,
    //  and pitch of a sound channel.
    fun I_UpdateSoundParams(
        handle: Int,
        vol: Int,
        sep: Int,
        pitch: Int
    )

    //
    //  MUSIC I/O
    //
    fun I_InitMusic()
    fun I_ShutdownMusic()

    // Volume.
    fun I_SetMusicVolume(volume: Int)

    // PAUSE game handling.
    fun I_PauseSong(handle: Int)
    fun I_ResumeSong(handle: Int)

    // Registers a song handle to song data.
    fun I_RegisterSong(data: ByteArray?): Int

    // Called by anything that wishes to start music.
    //  plays a song, and when the song is done,
    //  starts playing it again in an endless loop.
    // Horrible thing to do, considering.
    fun I_PlaySong(
        handle: Int,
        looping: Int
    )

    // Stops a song over 3 seconds.
    fun I_StopSong(handle: Int)

    // See above (register), then think backwards
    fun I_UnRegisterSong(handle: Int)
}