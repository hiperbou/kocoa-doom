package i


import data.sfxinfo_t

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: SystemSoundInterface.java,v 1.2 2011/05/17 16:51:20 velktron Exp $
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
interface SystemSoundInterface {
    // Init at program start...
    fun InitSound()

    // ... update sound buffer and audio device at runtime...
    fun UpdateSound()
    fun SubmitSound()

    // ... shut down and relase at program termination.
    fun ShutdownSound()

    //
    //  SFX I/O
    //
    // Initialize channels?
    fun SetChannels()

    // Get raw data lump index for sound descriptor.
    fun GetSfxLumpNum(sfxinfo: sfxinfo_t?): Int

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

    // Called by S_*() functions
    //  to see if a channel is still playing.
    // Returns 0 if no longer playing, 1 if playing.
    fun SoundIsPlaying(handle: Int): Boolean

    // Updates the volume, separation,
    //  and pitch of a sound channel.
    fun UpdateSoundParams(
        handle: Int,
        vol: Int,
        sep: Int,
        pitch: Int
    )

    //
    //  MUSIC I/O
    //
    fun InitMusic()
    fun ShutdownMusic()

    // Volume.
    fun SetMusicVolume(volume: Int)

    // PAUSE game handling.
    fun PauseSong(handle: Int)
    fun ResumeSong(handle: Int)

    // Registers a song handle to song data.
    fun RegisterSong(data: ByteArray?): Int

    // Called by anything that wishes to start music.
    //  plays a song, and when the song is done,
    //  starts playing it again in an endless loop.
    // Horrible thing to do, considering.
    fun PlaySong(
        handle: Int,
        looping: Int
    )

    // Stops a song over 3 seconds.
    fun StopSong(handle: Int)

    // See above (register), then think backwards
    fun UnRegisterSong(handle: Int)
}