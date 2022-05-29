package s

import data.sounds.musicenum_t
import data.sounds.sfxenum_t
import p.mobj_t

/** Does nothing. Just allows me to code without
 * commenting out ALL sound-related code. Hopefully
 * it will be superseded by a real sound driver one day.
 *
 * @author Velktron
 */
class DummySoundDriver : IDoomSound {
    override fun Init(sfxVolume: Int, musicVolume: Int) {
        // TODO Auto-generated method stub
    }

    override fun Start() {
        // TODO Auto-generated method stub
    }

    override fun StartSound(origin: ISoundOrigin?, sound_id: Int) {
        // TODO Auto-generated method stub
    }

    override fun StartSound(origin: ISoundOrigin?, sound_id: sfxenum_t?) {
        // TODO Auto-generated method stub
    }

    override fun StartSoundAtVolume(origin: ISoundOrigin?, sound_id: Int, volume: Int) {
        // TODO Auto-generated method stub
    }

    override fun StopSound(origin: ISoundOrigin?) {
        // TODO Auto-generated method stub
    }

    override fun ChangeMusic(musicnum: Int, looping: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun StopMusic() {
        // TODO Auto-generated method stub
    }

    override fun PauseSound() {
        // TODO Auto-generated method stub
    }

    override fun ResumeSound() {
        // TODO Auto-generated method stub
    }

    override fun UpdateSounds(listener: mobj_t) {
        // TODO Auto-generated method stub
    }

    override fun SetMusicVolume(volume: Int) {
        // TODO Auto-generated method stub
    }

    override fun SetSfxVolume(volume: Int) {
        // TODO Auto-generated method stub
    }

    override fun StartMusic(music_id: Int) {
        // TODO Auto-generated method stub
    }

    override fun StartMusic(music_id: musicenum_t) {
        // TODO Auto-generated method stub
    }

    override fun ChangeMusic(musicnum: musicenum_t, looping: Boolean) {
        // TODO Auto-generated method stub
    }
}