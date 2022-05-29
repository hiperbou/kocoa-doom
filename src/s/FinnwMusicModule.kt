package s

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.sound.midi.*

/** A music driver that bypasses Sequences and sends events from a MUS lump
 * directly to a MIDI device.
 *
 * Some songs (e.g. D_E1M8) vary individual channel volumes dynamically. This
 * driver multiplies the dynamic volume by the music volume set in the menu.
 * This does not work well with a [Sequence] because changes to events
 * (e.g. channel volume change events) do not take effect while the sequencer
 * is running.
 *
 * Disadvantages of this driver:
 *  * Supports MUS lumps only (no MID, OGG etc.)
 *  * Creates its own thread
 *  * Pausing is not implemented yet
 *
 * @author finnw
 */
class FinnwMusicModule : IMusic {
    override fun InitMusic() {
        try {
            receiver = FinnwMusicModule.getReceiver()
            val genMidiEG = FinnwMusicModule.EventGroup(1f)
            genMidiEG.generalMidi(1)
            genMidiEG.sendTo(receiver)
            FinnwMusicModule.sleepUninterruptibly(100, TimeUnit.MILLISECONDS)
        } catch (ex: MidiUnavailableException) {
            System.err.println(ex)
            receiver = null
        }
        exec = Executors.newSingleThreadScheduledExecutor(ThreadFactoryImpl())
    }

    /** Not yet implemented  */
    override fun PauseSong(handle: Int) {}
    override fun PlaySong(handle: Int, looping: Boolean) {
        lock.lock()
        try {
            if (currentTransmitter != null) {
                currentTransmitter!!.stop()
            }
            currentTransmitter = null
            if (0 <= handle && handle < songs.size) {
                prepare(receiver)
                val song = songs[handle]
                currentTransmitter = ScheduledTransmitter(song!!.getScoreBuffer(), looping)
                currentTransmitter!!._receiver = receiver!!
            }
        } finally {
            lock.unlock()
        }
    }

    override fun RegisterSong(data: ByteArray?): Int {
        return RegisterSong(ByteBuffer.wrap(data))
    }

    fun RegisterSong(data: ByteBuffer): Int {
        val song = Song(data)
        lock.lock()
        return try {
            var result = songs.indexOf(null)
            if (result >= 0) {
                songs[result] = song
            } else {
                result = songs.size
                songs.add(song)
            }
            result
        } finally {
            lock.unlock()
        }
    }

    override fun ResumeSong(handle: Int) {}
    override fun SetMusicVolume(volume: Int) {
        var fVol = volume * (1 / 127f)
        fVol = Math.max(0f, Math.min(fVol, 1f))
        lock.lock()
        try {
            this.volume = fVol
            if (currentTransmitter != null) {
                currentTransmitter!!.volumeChanged()
            }
        } finally {
            lock.unlock()
        }
    }

    override fun ShutdownMusic() {
        exec!!.shutdown()
    }

    override fun StopSong(handle: Int) {
        lock.lock()
        try {
            if (currentTransmitter != null) {
                currentTransmitter!!.stop()
                currentTransmitter = null
            }
        } finally {
            lock.unlock()
        }
    }

    override fun UnRegisterSong(handle: Int) {
        lock.lock()
        try {
            if (0 <= handle && handle < songs.size) {
                songs[handle] = null
            }
        } finally {
            lock.unlock()
        }
    }

    fun nextEventGroup(scoreBuffer: ByteBuffer, looping: Boolean): EventGroup? {
        val result = EventGroup(volume)
        var last: Boolean
        do {
            if (!scoreBuffer.hasRemaining()) {
                if (looping) {
                    scoreBuffer.flip()
                } else {
                    return result.emptyToNull()
                }
            }
            val descriptor = scoreBuffer.get().toInt() and 0xff
            last = descriptor and 0x80 != 0
            val eventType = descriptor shr 4 and 7
            val chanIndex = descriptor and 15
            val channel = channels[chanIndex]
            when (eventType) {
                0 -> {
                    val note = scoreBuffer.get().toInt() and 0xff
                    require(note and 0x80 == 0) { "Invalid note byte" }
                    FinnwMusicModule.checkChannelExists("note off", channel).noteOff(note, result)
                }
                1 -> {
                    val note = scoreBuffer.get().toInt() and 0xff
                    val hasVelocity = note and 0x80 != 0
                    if (hasVelocity) {
                        val velocity = scoreBuffer.get().toInt() and 0xff
                        require(velocity and 0x80 == 0) { "Invalid velocity byte" }
                        FinnwMusicModule.checkChannelExists("note on", channel)
                            .noteOn(note and 127, velocity, result)
                    } else {
                        FinnwMusicModule.checkChannelExists("note on", channel).noteOn(note, result)
                    }
                }
                2 -> {
                    val wheelVal = scoreBuffer.get().toInt() and 0xff
                    FinnwMusicModule.checkChannelExists("pitch bend", channel).pitchBend(wheelVal, result)
                }
                3 -> {
                    val sysEvt = scoreBuffer.get().toInt() and 0xff
                    when (sysEvt) {
                        10 -> FinnwMusicModule.checkChannelExists("all sounds off", channel)
                            .allSoundsOff(result)
                        11 -> FinnwMusicModule.checkChannelExists("all notes off", channel)
                            .allNotesOff(result)
                        14 -> FinnwMusicModule.checkChannelExists("reset all controllers", channel)
                            .resetAll(result)
                        else -> {
                            val msg = String.format("Invalid system event (%d)", sysEvt)
                            throw IllegalArgumentException(msg)
                        }
                    }
                }
                4 -> {
                    val cNum = scoreBuffer.get().toInt() and 0xff
                    require(cNum and 0x80 == 0) { "Invalid controller number " }
                    var cVal = scoreBuffer.get().toInt() and 0xff
                    if (cNum == 3 && 133 <= cVal && cVal <= 135) {
                        // workaround for some TNT.WAD tracks
                        cVal = 127
                    }
                    if (cVal and 0x80 != 0) {
                        val msg = String.format("Invalid controller value (%d; cNum=%d)", cVal, cNum)
                        throw IllegalArgumentException(msg)
                    }
                    when (cNum) {
                        0 -> FinnwMusicModule.checkChannelExists("patch change", channel)
                            .patchChange(cVal, result)
                        1 ->                     // Don't forward this to the MIDI device.  Some devices
                            // react badly to banks that are undefined in GM Level 1
                            FinnwMusicModule.checkChannelExists("bank switch", channel)
                        2 -> FinnwMusicModule.checkChannelExists("vibrato change", channel)
                            .vibratoChange(cVal, result)
                        3 -> FinnwMusicModule.checkChannelExists("volume", channel).volume(cVal, result)
                        4 -> FinnwMusicModule.checkChannelExists("pan", channel).pan(cVal, result)
                        5 -> FinnwMusicModule.checkChannelExists("expression", channel)
                            .expression(cVal, result)
                        6 -> FinnwMusicModule.checkChannelExists("reverb depth", channel)
                            .reverbDepth(cVal, result)
                        7 -> FinnwMusicModule.checkChannelExists("chorus depth", channel)
                            .chorusDepth(cVal, result)
                        else -> throw AssertionError("Controller number $cNum: not yet implemented")
                    }
                }
                6 -> if (looping) {
                    scoreBuffer.flip()
                } else {
                    return result.emptyToNull()
                }
                else -> {
                    val msg = String.format(
                        "Unknown event type: last=%5s eventType=%d chanIndex=%d%n",
                        last,
                        eventType,
                        chanIndex
                    )
                    throw IllegalArgumentException(msg)
                }
            }
        } while (!last)
        val qTics = readTime(scoreBuffer)
        result.addDelay(qTics)
        return result
    }

    class EventGroup(volScale: Float) {
        fun addDelay(tics: Int) {
            delay += tics
        }

        fun allNotesOff(midiChan: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CHM_ALL_NOTES_OFF, 0)
        }

        fun allSoundsOff(midiChan: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CHM_ALL_SOUND_OFF, 0)
        }

        fun appendTo(sequence: javax.sound.midi.Sequence, trackNum: Int, pos: Long): Long {
            val track = sequence.tracks[trackNum]
            for (msg in messages) {
                track.add(MidiEvent(msg, pos))
            }
            return pos + delay * 3
        }

        fun appendTo(track: Track, pos: Long, scale: Int): Long {
            for (msg in messages) {
                track.add(MidiEvent(msg, pos))
            }
            return pos + delay * scale
        }

        fun chorusDepth(midiChan: Int, depth: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CTRL_CHORUS_DEPTH, depth)
        }

        fun generalMidi(mode: Int) {
            addSysExMessage(0xf0, 0x7e.toByte(), 0x7f.toByte(), 9.toByte(), mode.toByte(), 0xf7.toByte())
        }

        fun emptyToNull(): FinnwMusicModule.EventGroup? {
            return if (messages.isEmpty()) {
                null
            } else {
                this
            }
        }

        fun expression(midiChan: Int, expr: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CTRL_EXPRESSION_POT, expr)
        }

        fun getDelay(): Int {
            return delay
        }

        fun noteOn(midiChan: Int, note: Int, velocity: Int) {
            addShortMessage(midiChan, ShortMessage.NOTE_ON, note, velocity)
        }

        fun noteOff(midiChan: Int, note: Int) {
            addShortMessage(midiChan, ShortMessage.NOTE_OFF, note, 0)
        }

        fun pan(midiChan: Int, pan: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CTRL_PAN, pan)
        }

        fun patchChange(midiChan: Int, patchId: Int) {
            addShortMessage(midiChan, ShortMessage.PROGRAM_CHANGE, patchId, 0)
        }

        fun pitchBend(midiChan: Int, wheelVal: Int) {
            val pb14 = wheelVal * 64
            addShortMessage(midiChan, ShortMessage.PITCH_BEND, pb14 % 128, pb14 / 128)
        }

        fun pitchBendSensitivity(midiChan: Int, semitones: Int) {
            addRegParamChange(
                midiChan,
                FinnwMusicModule.EventGroup.RPM_PITCH_BEND_SENSITIVITY,
                FinnwMusicModule.EventGroup.RPL_PITCH_BEND_SENSITIVITY,
                semitones
            )
        }

        fun resetAllControllern(midiChan: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CHM_RESET_ALL, 0)
        }

        fun reverbDepth(midiChan: Int, depth: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CTRL_REVERB_DEPTH, depth)
        }

        fun sendTo(receiver: Receiver?) {
            for (msg in messages) {
                receiver?.send(msg, -1)
            }
        }

        fun vibratoChange(midiChan: Int, depth: Int) {
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CTRL_MODULATION_POT, depth)
        }

        fun volume(midiChan: Int, vol: Int) {
            var vol = vol
            vol = Math.round(vol * volScale)
            addControlChange(midiChan, FinnwMusicModule.EventGroup.CTRL_VOLUME, vol)
        }

        private fun addControlChange(midiChan: Int, ctrlId: Int, ctrlVal: Int) {
            addShortMessage(midiChan, ShortMessage.CONTROL_CHANGE, ctrlId, ctrlVal)
        }

        private fun addRegParamChange(midiChan: Int, paramMsb: Int, paramLsb: Int, valMsb: Int) {
            addControlChange(midiChan, 101, paramMsb)
            addControlChange(midiChan, 100, paramLsb)
            addControlChange(midiChan, 6, valMsb)
        }

        private fun addShortMessage(midiChan: Int, cmd: Int, data1: Int, data2: Int) {
            try {
                val msg = ShortMessage()
                msg.setMessage(cmd, midiChan, data1, data2)
                messages.add(msg)
            } catch (ex: InvalidMidiDataException) {
                throw RuntimeException(ex)
            }
        }

        private fun addSysExMessage(status: Int, vararg data: Byte) {
            try {
                val msg = SysexMessage()
                msg.setMessage(status, data, data.size)
                messages.add(msg)
            } catch (ex: InvalidMidiDataException) {
                throw RuntimeException(ex)
            }
        }

        private var delay = 0
        private val messages: MutableList<MidiMessage>
        private val volScale: Float

        init {
            messages = ArrayList()
            this.volScale = volScale
        }

        companion object {
            private const val CHM_ALL_NOTES_OFF = 123
            private const val CHM_ALL_SOUND_OFF = 120
            private const val CTRL_CHORUS_DEPTH = 93
            private const val CTRL_EXPRESSION_POT = 11
            private const val CTRL_PAN = 10
            private const val RPM_PITCH_BEND_SENSITIVITY = 0
            private const val RPL_PITCH_BEND_SENSITIVITY = 0
            private const val CHM_RESET_ALL = 121
            private const val CTRL_REVERB_DEPTH = 91
            private const val CTRL_MODULATION_POT = 1
            private const val CTRL_VOLUME = 7
        }
    }

    /** A collection of kludges to pick a MIDI output device until cvars are implemented  */
    internal class MidiDeviceComparator : Comparator<MidiDevice.Info> {
        override fun compare(o1: MidiDevice.Info, o2: MidiDevice.Info): Int {
            val score1 = score(o1)
            val score2 = score(o2)
            return if (score1 < score2) {
                1
            } else if (score1 > score2) {
                -1
            } else {
                0
            }
        }

        private fun score(info: MidiDevice.Info): Float {
            val lcName = info.name.lowercase()
            var result = 0f
            if (lcName.contains("mapper")) {
                // "Midi Mapper" is ideal, because the user can select the default output device in the control panel
                result += 100f
            } else {
                if (lcName.contains("synth")) {
                    // A synthesizer is usually better than a sequencer or USB MIDI port
                    result += 50f
                    if (lcName.contains("java")) {
                        // "Java Sound Synthesizer" has a low sample rate; Prefer another software synth
                        result -= 20f
                    }
                    if (lcName.contains("microsoft")) {
                        // "Microsoft GS Wavetable Synth" is notoriously unpopular, but sometimes it's the only one
                        // with a decent sample rate.
                        result -= 7f
                    }
                }
            }
            return result
        }
    }

    internal class ThreadFactoryImpl : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r, String.format("FinnwMusicModule-%d", NEXT_ID.getAndIncrement()))
            thread.priority = Thread.MAX_PRIORITY - 1
            return thread
        }

        companion object {
            private val NEXT_ID = AtomicInteger(1)
        }
    }

    val lock: Lock

    /** Channels in MUS order (0-14 = instruments, 15 = percussion)  */
    private lateinit var channels: MutableList<FinnwMusicModule.Channel>
    var exec: ScheduledExecutorService? = null
    var volume = 0f
    private fun prepare(receiver: Receiver?) {
        val setupEG = FinnwMusicModule.EventGroup(volume)
        for (chan in channels) {
            chan.allSoundsOff(setupEG)
            chan.resetAll(setupEG)
            chan.pitchBendSensitivity(2, setupEG)
            chan.volume(127, setupEG)
        }
        setupEG.sendTo(receiver)
    }

    private fun readTime(scoreBuffer: ByteBuffer): Int {
        var result = 0
        var last: Boolean
        do {
            val digit = scoreBuffer.get().toInt() and 0xff
            last = digit and 0x80 == 0
            result = result shl 7
            result = result or (digit and 127)
        } while (!last)
        return result
    }

    private class Channel internal constructor(private val midiChan: Int) {
        fun allNotesOff(eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.allNotesOff(midiChan)
        }

        fun allSoundsOff(eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.allSoundsOff(midiChan)
        }

        fun chorusDepth(depth: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.chorusDepth(midiChan, depth)
        }

        fun expression(expr: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.expression(midiChan, expr)
        }

        fun noteOff(note: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.noteOff(midiChan, note)
        }

        fun noteOn(note: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.noteOn(midiChan, note, lastVelocity)
        }

        fun noteOn(note: Int, velocity: Int, eventGroup: FinnwMusicModule.EventGroup) {
            lastVelocity = velocity
            noteOn(note, eventGroup)
        }

        fun pan(pan: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.pan(midiChan, pan)
        }

        fun patchChange(patchId: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.patchChange(midiChan, patchId)
        }

        fun pitchBend(wheelVal: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.pitchBend(midiChan, wheelVal)
        }

        fun pitchBendSensitivity(semitones: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.pitchBendSensitivity(midiChan, semitones)
        }

        fun resetAll(eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.resetAllControllern(midiChan)
        }

        fun reverbDepth(depth: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.reverbDepth(midiChan, depth)
        }

        fun vibratoChange(depth: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.vibratoChange(midiChan, depth)
        }

        fun volume(vol: Int, eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.volume(midiChan, vol)
            lastVolume = vol
        }

        fun volumeChanged(eventGroup: FinnwMusicModule.EventGroup) {
            eventGroup.volume(midiChan, lastVolume)
        }

        private var lastVelocity = 0
        private var lastVolume = 0
    }

    private inner class ScheduledTransmitter internal constructor(scoreBuffer: ByteBuffer, looping: Boolean) :
        Transmitter {
        override fun close() {
            lock.lock()
            try {
                if (autoShutdown && exec != null) {
                    exec!!.shutdown()
                }
                autoShutdown = false
                exec = null
            } finally {
                lock.unlock()
            }
        }

        override fun getReceiver(): Receiver {
            return _receiver!!
        }

        override fun setReceiver(receiver: Receiver) {
            var currentGroup: FinnwMusicModule.EventGroup? = null
            lock.lock()
            try {
                if (this._receiver != null) {
                    if (future!!.cancel(false)) {
                        currentGroup = triggerTask!!.eventGroup
                    }
                } else {
                    nextGroupTime = System.nanoTime()
                }
                this._receiver = receiver
                scheduleIfRequired(receiver, currentGroup)
            } finally {
                lock.unlock()
            }
        }

        fun scheduleIfRequired(
            receiver: Receiver?,
            currentGroup: FinnwMusicModule.EventGroup?
        ) {
            var currentGroup = currentGroup
            assert((lock as ReentrantLock).isHeldByCurrentThread)
            if (currentGroup == null) {
                try {
                    currentGroup = nextEventGroup(scoreBuffer, looping)
                    if (currentGroup != null) {
                        triggerTask = TriggerTask(currentGroup, receiver)
                        val delay = Math.max(0, nextGroupTime - System.nanoTime())
                        future = exec!!.schedule(triggerTask, delay, TimeUnit.NANOSECONDS)
                        nextGroupTime += currentGroup.getDelay() * FinnwMusicModule.nanosPerTick
                    } else {
                        triggerTask = null
                        future = null
                    }
                } catch (ex: RejectedExecutionException) {
                    // This is normal when shutting down
                } catch (ex: Exception) {
                    System.err.println(ex)
                }
            }
        }

        fun stop() {
            assert((lock as ReentrantLock).isHeldByCurrentThread)
            if (future != null) {
                future!!.cancel(false)
                try {
                    future!!.get()
                } catch (ex: InterruptedException) {
                } catch (ex: ExecutionException) {
                } catch (ex: CancellationException) {
                }
                future = null
            }
            val cleanup = FinnwMusicModule.EventGroup(0f)
            for (chan in channels) {
                chan.allNotesOff(cleanup)
            }
            cleanup.sendTo(_receiver)
        }

        fun volumeChanged() {
            assert((lock as ReentrantLock).isHeldByCurrentThread)
            val adjust = FinnwMusicModule.EventGroup(volume)
            for (chan in channels) {
                chan.volumeChanged(adjust)
            }
            adjust.sendTo(_receiver)
        }

        var triggerTask: TriggerTask? = null

        private inner class TriggerTask internal constructor(
            val eventGroup: FinnwMusicModule.EventGroup,
            val receiver: Receiver?
        ) : Runnable {
            override fun run() {
                var shouldSend = false
                lock.lock()
                try {
                    if (triggerTask === this) {
                        shouldSend = true
                        scheduleIfRequired(receiver, null)
                    }
                } finally {
                    lock.unlock()
                }
                if (shouldSend) {
                    eventGroup.sendTo(receiver)
                }
            }
        }

        private var autoShutdown = false
        private var exec: ScheduledExecutorService?
        private var future: ScheduledFuture<*>? = null
        private val looping: Boolean
        private var nextGroupTime: Long = 0
        var _receiver: Receiver? = null
        private val scoreBuffer: ByteBuffer

        init {
            this.exec = this@FinnwMusicModule.exec
            this.looping = looping
            this.scoreBuffer = scoreBuffer
        }
    }

    /** Contains unfiltered MUS data  */
    private inner class Song internal constructor(data: ByteBuffer) {
        /** Get only the score part of the data (skipping the header)  */
        fun getScoreBuffer(): ByteBuffer {
            val scoreBuffer = data.duplicate()
            scoreBuffer.position(scoreStart)
            scoreBuffer.limit(scoreStart + scoreLen)
            return scoreBuffer.slice()
        }

        private val data: ByteBuffer
        private val scoreLen: Int
        private val scoreStart: Int

        init {
            this.data = data.asReadOnlyBuffer()
            this.data.order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(4)
            this.data[magic]
            val magicBuf = ByteBuffer.wrap(magic)
            require(FinnwMusicModule.hasMusMagic(magicBuf)) {
                "Expected magic string \"MUS\\x1a\" but found " + Arrays.toString(
                    magic
                )
            }
            scoreLen = this.data.short.toInt() and 0xffff
            scoreStart = this.data.short.toInt() and 0xffff
        }
    }

    private var currentTransmitter: ScheduledTransmitter? = null
    private var receiver: Receiver? = null

    /** Songs indexed by handle  */
    private val songs: MutableList<Song?>

    init {
        lock = ReentrantLock()
        channels = ArrayList(15)
        songs = ArrayList(1)
        for (midiChan in 0..15) {
            if (midiChan != 9) {
                channels.add(FinnwMusicModule.Channel(midiChan))
            }
        }
        channels.add(FinnwMusicModule.Channel(9))
    }

    companion object {
        fun hasMusMagic(magicBuf: ByteBuffer): Boolean {
            return magicBuf[0] == 'M'.code.toByte() && magicBuf[1] == 'U'.code.toByte() && magicBuf[2] == 'S'.code.toByte() && magicBuf[3].toInt() == 0x1a
        }

        const val nanosPerTick = (1000000000 / 140).toLong()
        @Throws(MidiUnavailableException::class)
        private fun getReceiver(): Receiver? {
            val dInfos: MutableList<MidiDevice.Info> = ArrayList(Arrays.asList(*MidiSystem.getMidiDeviceInfo()))
            val it = dInfos.iterator()
            while (it.hasNext()) {
                val dInfo = it.next()
                val dev = MidiSystem.getMidiDevice(dInfo)
                if (dev.maxReceivers == 0) {
                    // We cannot use input-only devices
                    it.remove()
                }
            }
            if (dInfos.isEmpty()) return null
            Collections.sort(dInfos, FinnwMusicModule.MidiDeviceComparator())
            val dInfo = dInfos[0]
            val dev = MidiSystem.getMidiDevice(dInfo)
            dev.open()
            return dev.receiver
        }

        private fun sleepUninterruptibly(timeout: Int, timeUnit: TimeUnit) {
            var interrupted = false
            var now = System.nanoTime()
            val expiry = now + timeUnit.toNanos(timeout.toLong())
            var remaining: Long
            while (expiry - now.also { remaining = it } > 0L) {
                try {
                    TimeUnit.NANOSECONDS.sleep(remaining)
                } catch (ex: InterruptedException) {
                    interrupted = true
                } finally {
                    now = System.nanoTime()
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt()
            }
        }

        @Throws(IllegalArgumentException::class)
        private fun checkChannelExists(type: String, channel: FinnwMusicModule.Channel?): FinnwMusicModule.Channel {
            return if (channel == null) {
                val msg = String.format("Invalid channel for %s message", type)
                throw IllegalArgumentException(msg)
            } else {
                channel
            }
        }
    }
}