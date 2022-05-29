package s

class DummyMusic : IMusic {
    override fun InitMusic() {
        // TODO Auto-generated method stub
    }

    override fun ShutdownMusic() {
        // TODO Auto-generated method stub
    }

    override fun SetMusicVolume(volume: Int) {
        // TODO Auto-generated method stub
    }

    override fun PauseSong(handle: Int) {
        // TODO Auto-generated method stub
    }

    override fun ResumeSong(handle: Int) {
        // TODO Auto-generated method stub
    }

    override fun RegisterSong(data: ByteArray?): Int {
        // TODO Auto-generated method stub
        return 0
    }

    override fun PlaySong(handle: Int, looping: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun StopSong(handle: Int) {
        // TODO Auto-generated method stub
    }

    override fun UnRegisterSong(handle: Int) {
        // TODO Auto-generated method stub
    }
}