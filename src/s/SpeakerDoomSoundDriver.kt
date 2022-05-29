package s

import doom.DoomMain
import s.SpeakerSound
import java.util.*

/** A variation of the Classic Sound Driver, decoding the DP-lumps
 * instead of the DS. A better way would be to build-in an
 * automatic "WAV to SPEAKER" conversion, but that can wait...
 *
 * @author Maes
 */
class SpeakerDoomSoundDriver(DM: DoomMain<*, *>, numChannels: Int) : ClassicDoomSoundDriver(DM, numChannels) {
    /** Rigged so it gets SPEAKER sounds instead of regular ones  */
    override fun getsfx(
        sfxname: String?,
        len: IntArray, index: Int
    ): ByteArray {
        val sfx: ByteArray
        val paddedsfx: ByteArray
        var i: Int
        val size: Int
        val paddedsize: Int
        val name: String
        val sfxlump: Int

        // Get the sound data from the WAD, allocate lump
        //  in zone memory.
        name = String.format("dp%s", sfxname).uppercase(Locale.getDefault())

        // Now, there is a severe problem with the
        //  sound handling, in it is not (yet/anymore)
        //  gamemode aware. That means, sounds from
        //  DOOM II will be requested even with DOOM
        //  shareware.
        // The sound list is wired into sounds.c,
        //  which sets the external variable.
        // I do not do runtime patches to that
        //  variable. Instead, we will use a
        //  default sound for replacement.
        sfxlump =
            if (DM.wadLoader.CheckNumForName(name) == -1) DM.wadLoader.GetNumForName("dppistol") else DM.wadLoader.GetNumForName(
                name
            )

        // We must first load and convert it to raw samples.
        val SP = DM.wadLoader.CacheLumpNum(sfxlump, 0, SpeakerSound::class.java) as SpeakerSound
        sfx = SP.toRawSample()
        size = sfx.size

        // MAES: A-ha! So that's how they do it.
        // SOund effects are padded to the highest multiple integer of 
        // the mixing buffer's size (with silence)
        paddedsize =
            (size - 8 + (ISoundDriver.SAMPLECOUNT - 1)) / ISoundDriver.SAMPLECOUNT * ISoundDriver.SAMPLECOUNT

        // Allocate from zone memory.
        paddedsfx = ByteArray(paddedsize)

        // Now copy and pad. The first 8 bytes are header info, so we discard them.
        System.arraycopy(sfx, 8, paddedsfx, 0, size - 8)
        i = size - 8
        while (i < paddedsize) {
            paddedsfx[i] = 127.toByte()
            i++
        }


        // Hmm....silence?
        i = size - 8
        while (i < paddedsize) {
            paddedsfx[i] = 127.toByte()
            i++
        }

        // Remove the cached lump.
        DM.wadLoader.UnlockLumpNum(sfxlump)
        if (AbstractSoundDriver.D) System.out.printf("SFX %d size %d padded to %d\n", index, size, paddedsize)
        // Preserve padded length.
        len[index] = paddedsize

        // Return allocated padded data.
        // So the first 8 bytes are useless?
        return paddedsfx
    }
}