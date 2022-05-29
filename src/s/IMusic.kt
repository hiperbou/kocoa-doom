package s

import doom.CVarManager
import doom.CommandVariable



//
//  MUSIC I/O
//
interface IMusic {
    fun InitMusic()
    fun ShutdownMusic()

    // Volume.
    fun SetMusicVolume(volume: Int)

    /** PAUSE game handling.  */
    fun PauseSong(handle: Int)
    fun ResumeSong(handle: Int)

    /** Registers a song handle to song data.
     * This should handle any conversions from MUS/MIDI/OPL/etc.
     *
     */
    fun RegisterSong(data: ByteArray?): Int

    /** Called by anything that wishes to start music.
     * plays a song, and when the song is done,
     * starts playing it again in an endless loop.
     * Horrible thing to do, considering.  */
    fun PlaySong(
        handle: Int,
        looping: Boolean
    )

    /** Stops a song over 3 seconds.  */
    fun StopSong(handle: Int)

    /** See above (register), then think backwards  */
    fun UnRegisterSong(handle: Int)

    companion object {
        fun chooseModule(CVM: CVarManager): IMusic {
            return if (CVM.bool(CommandVariable.NOMUSIC) || CVM.bool(CommandVariable.NOSOUND)) {
                DummyMusic()
            } else {
                DavidMusicModule()
            }
        }
    }
}