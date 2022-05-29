package s

import data.sfxinfo_t

class DummySFX : ISoundDriver {
    override fun InitSound(): Boolean {
        // Dummy is super-reliable ;-)
        return true
    }

    override fun UpdateSound() {
        // TODO Auto-generated method stub
    }

    override fun SubmitSound() {
        // TODO Auto-generated method stub
    }

    override fun ShutdownSound() {
        // TODO Auto-generated method stub
    }

    override fun GetSfxLumpNum(sfxinfo: sfxinfo_t): Int {
        // TODO Auto-generated method stub
        return 0
    }

    override fun StartSound(id: Int, vol: Int, sep: Int, pitch: Int, priority: Int): Int {
        // TODO Auto-generated method stub
        return 0
    }

    override fun StopSound(handle: Int) {
        // TODO Auto-generated method stub
    }

    override fun SoundIsPlaying(handle: Int): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun UpdateSoundParams(handle: Int, vol: Int, sep: Int, pitch: Int) {
        // TODO Auto-generated method stub
    }

    override fun SetChannels(numChannels: Int) {
        // TODO Auto-generated method stub
    }
}