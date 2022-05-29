package s


import m.Swap
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.sound.midi.*

/**
 * A MUS lump reader that loads directly to a Sequence.
 *
 * Unlike QMusToMid, does not keep the MIDI version in a temporary file.
 *
 * @author finnw
 */
object MusReader {
    /** Create a sequence from an InputStream.
     * This is the counterpart of [MidiSystem.getSequence]
     * for MUS format.
     *
     * @param is MUS data (this method does not try to auto-detect the format.)
     */
    @Throws(IOException::class, InvalidMidiDataException::class)
    fun getSequence(`is`: InputStream?): Sequence {
        val dis = DataInputStream(`is`)
        dis.skip(6)
        val rus = dis.readUnsignedShort()
        val scoreStart = Swap.SHORT(rus.toChar())
        dis.skip((scoreStart - 8).toLong())
        val sequence = Sequence(Sequence.SMPTE_30, 14, 1)
        val track = sequence.tracks[0]
        val chanVelocity = IntArray(16)
        Arrays.fill(chanVelocity, 100)
        var eg: EventGroup?
        var tick: Long = 0
        while (MusReader.nextEventGroup(dis, chanVelocity).also { eg = it } != null) {
            tick = eg!!.appendTo(track, tick)
        }
        val endOfSequence = MetaMessage()
        endOfSequence.setMessage(47, byteArrayOf(0), 1)
        track.add(MidiEvent(endOfSequence, tick))
        return sequence
    }

    @Throws(IOException::class)
    private fun nextEventGroup(`is`: InputStream, channelVelocity: IntArray): EventGroup? {
        val result = EventGroup()
        var last: Boolean
        do {
            val b = `is`.read()
            if (b < 0) {
                return result.emptyToNull()
            }
            val descriptor = b and 0xff
            last = descriptor and 0x80 != 0
            val eventType = descriptor shr 4 and 7
            val chanIndex = descriptor and 15
            val midiChan: Int
            midiChan = if (chanIndex < 9) {
                chanIndex
            } else if (chanIndex < 15) {
                chanIndex + 1
            } else {
                9
            }
            when (eventType) {
                0 -> {
                    val note = `is`.read() and 0xff
                    require(note and 0x80 == 0) { "Invalid note byte" }
                    result.noteOff(midiChan, note)
                }
                1 -> {
                    val note = `is`.read() and 0xff
                    val hasVelocity = note and 0x80 != 0
                    val velocity: Int
                    if (hasVelocity) {
                        velocity = `is`.read() and 0xff
                        require(velocity and 0x80 == 0) { "Invalid velocity byte" }
                        channelVelocity[midiChan] = velocity
                    } else {
                        velocity = channelVelocity[midiChan]
                    }
                    result.noteOn(midiChan, note and 0x7f, velocity)
                }
                2 -> {
                    val wheelVal = `is`.read() and 0xff
                    result.pitchBend(midiChan, wheelVal)
                }
                3 -> {
                    val sysEvt = `is`.read() and 0xff
                    when (sysEvt) {
                        10 -> result.allSoundsOff(midiChan)
                        11 -> result.allNotesOff(midiChan)
                        14 -> result.resetAllControllers(midiChan)
                        else -> {
                            val msg = String.format("Invalid system event (%d)", sysEvt)
                            throw IllegalArgumentException(msg)
                        }
                    }
                }
                4 -> {
                    val cNum = `is`.read() and 0xff
                    require(cNum and 0x80 == 0) { "Invalid controller number " }
                    var cVal = `is`.read() and 0xff
                    if (cNum == 3 && 133 <= cVal && cVal <= 135) {
                        // workaround for some TNT.WAD tracks
                        cVal = 127
                    }
                    if (cVal and 0x80 != 0) {
                        val msg = String.format("Invalid controller value (%d; cNum=%d)", cVal, cNum)
                        throw IllegalArgumentException(msg)
                    }
                    when (cNum) {
                        0 -> result.patchChange(midiChan, cVal)
                        1 -> {}
                        2 -> result.vibratoChange(midiChan, cVal)
                        3 -> result.volume(midiChan, cVal)
                        4 -> result.pan(midiChan, cVal)
                        5 -> result.expression(midiChan, cVal)
                        6 -> result.reverbDepth(midiChan, cVal)
                        7 -> result.chorusDepth(midiChan, cVal)
                        8 -> result.sustain(midiChan, cVal)
                        else -> throw AssertionError("Unknown controller number: $cNum(value: $cVal)")
                    }
                }
                6 -> return result.emptyToNull()
                else -> {
                    val msg = String.format("Unknown event type: %d", eventType)
                    throw IllegalArgumentException(msg)
                }
            }
        } while (!last)
        val qTics = MusReader.readVLV(`is`)
        result.addDelay(qTics.toLong())
        return result
    }

    @Throws(IOException::class)
    private fun readVLV(`is`: InputStream): Int {
        var result = 0
        var last: Boolean
        do {
            val digit = `is`.read() and 0xff
            last = digit and 0x80 == 0
            result = result shl 7
            result = result or (digit and 127)
        } while (!last)
        return result
    }

    private class EventGroup internal constructor() {
        fun addDelay(ticks: Long) {
            delay += ticks
        }

        fun allNotesOff(midiChan: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CHM_ALL_NOTES_OFF, 0)
        }

        fun allSoundsOff(midiChan: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CHM_ALL_SOUND_OFF, 0)
        }

        fun appendTo(track: Track, tick: Long): Long {
            for (msg in messages) {
                track.add(MidiEvent(msg, tick))
            }
            return tick + delay * 3
        }

        fun chorusDepth(midiChan: Int, depth: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CTRL_CHORUS_DEPTH, depth)
        }

        fun emptyToNull(): MusReader.EventGroup? {
            return if (messages.isEmpty()) {
                null
            } else {
                this
            }
        }

        fun expression(midiChan: Int, expr: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CTRL_EXPRESSION_POT, expr)
        }

        fun noteOn(midiChan: Int, note: Int, velocity: Int) {
            addShortMessage(midiChan, ShortMessage.NOTE_ON, note, velocity)
        }

        fun noteOff(midiChan: Int, note: Int) {
            addShortMessage(midiChan, ShortMessage.NOTE_OFF, note, 0)
        }

        fun pan(midiChan: Int, pan: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CTRL_PAN, pan)
        }

        fun patchChange(midiChan: Int, patchId: Int) {
            addShortMessage(midiChan, ShortMessage.PROGRAM_CHANGE, patchId, 0)
        }

        fun pitchBend(midiChan: Int, wheelVal: Int) {
            val pb14 = wheelVal * 64
            addShortMessage(midiChan, ShortMessage.PITCH_BEND, pb14 % 128, pb14 / 128)
        }

        fun resetAllControllers(midiChan: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CHM_RESET_ALL, 0)
        }

        fun reverbDepth(midiChan: Int, depth: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CTRL_REVERB_DEPTH, depth)
        }

        fun sustain(midiChan: Int, on: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CTRL_SUSTAIN, on)
        }

        fun vibratoChange(midiChan: Int, depth: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CTRL_MODULATION_POT, depth)
        }

        fun volume(midiChan: Int, vol: Int) {
            addControlChange(midiChan, MusReader.EventGroup.CTRL_VOLUME, vol)
        }

        private fun addControlChange(midiChan: Int, ctrlId: Int, ctrlVal: Int) {
            addShortMessage(midiChan, ShortMessage.CONTROL_CHANGE, ctrlId, ctrlVal)
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

        private var delay: Long = 0
        private val messages: MutableList<MidiMessage>

        init {
            messages = ArrayList()
        }

        companion object {
            private const val CHM_ALL_NOTES_OFF = 123
            private const val CHM_ALL_SOUND_OFF = 120
            private const val CTRL_CHORUS_DEPTH = 93
            private const val CTRL_EXPRESSION_POT = 11
            private const val CTRL_PAN = 10
            private const val CTRL_SUSTAIN = 64
            private const val CHM_RESET_ALL = 121
            private const val CTRL_REVERB_DEPTH = 91
            private const val CTRL_MODULATION_POT = 1
            private const val CTRL_VOLUME = 7
        }
    }
}