package s


import java.util.*
import javax.sound.midi.*

/** A [Receiver] that scales channel volumes.
 *
 * Works by recognising channel volume change events and scaling the new volume
 * by the global music volume setting before forwarding the event to the
 * synthesizer.
 *
 * @author finnw
 */
class VolumeScalingReceiver(delegate: Receiver) : Receiver {
    override fun close() {
        synthReceiver.close()
    }

    /** Set the scaling factor to be applied to all channel volumes  */
    @Synchronized
    fun setGlobalVolume(globalVolume: Float) {
        this.globalVolume = globalVolume
        for (chan in 0..15) {
            sendVolumeChange(chan, Math.round(channelVolume[chan] * globalVolume), -1)
        }
    }

    /** A collection of kludges to pick a synthesizer until cvars are implemented  */
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

        /** Guess how suitable a MidiDevice is for music output.  */
        private fun score(info: MidiDevice.Info): Float {
            val lcName = info.name.lowercase()
            var result = 0f
            return try {
                val dev = MidiSystem.getMidiDevice(info)
                dev.open()
                try {
                    if (dev is Sequencer) {
                        // The sequencer cannot be the same device as the synthesizer - that would create an infinite loop.
                        return Float.NEGATIVE_INFINITY
                    } else if (lcName.contains("mapper")) {
                        // "Midi Mapper" is ideal, because the user can select the default output device in the control panel
                        result += 100f
                    } else {
                        if (dev is Synthesizer) {
                            // A synthesizer is usually better than a sequencer or USB MIDI port
                            result += 50f
                            if (lcName.contains("java")) {
                                // "Java Sound Synthesizer" often has a low sample rate or no default soundbank;  Prefer another software synth
                                result -= if (dev.defaultSoundbank != null) {
                                    10f
                                } else {
                                    // Probably won't be audible
                                    500f
                                }
                            }
                            if (lcName.contains("microsoft")) {
                                // "Microsoft GS Wavetable Synth" is notoriously unpopular, but sometimes it's the only one
                                // with a decent sample rate.
                                result -= 7f
                            }
                        }
                    }
                    result
                } finally {
                    dev.close()
                }
            } catch (ex: MidiUnavailableException) {
                // Cannot use this one
                Float.NEGATIVE_INFINITY
            }
        }
    }

    /** Forward a message to the synthesizer.
     *
     * If `message` is a volume change message, the volume is
     * first multiplied by the global volume.  Otherwise, the message is
     * passed unmodified to the synthesizer.
     */
    @Synchronized
    override fun send(message: MidiMessage, timeStamp: Long) {
        val chan = getVolumeChangeChannel(message)
        if (chan < 0) {
            synthReceiver.send(message, timeStamp)
        } else {
            val newVolUnscaled = message.message[2].toInt()
            channelVolume[chan] = newVolUnscaled
            sendVolumeChange(chan, Math.round(newVolUnscaled * globalVolume), timeStamp)
        }
    }

    /** Send a volume update to a specific channel.
     *
     * This is used for both local & global volume changes.
     */
    private fun sendVolumeChange(chan: Int, newVolScaled: Int, timeStamp: Long) {
        var newVolScaled = newVolScaled
        newVolScaled = Math.max(0, Math.min(newVolScaled, 127))
        val message = ShortMessage()
        try {
            message.setMessage(0xb0 or (chan and 15), 7, newVolScaled)
            synthReceiver.send(message, timeStamp)
        } catch (ex: InvalidMidiDataException) {
            System.err.println(ex)
        }
    }

    /** Determine if the given message is a channel volume change.
     *
     * @return Channel number for which volume is being changed, or -1 if not a
     * channel volume change command.
     */
    private fun getVolumeChangeChannel(message: MidiMessage): Int {
        if (message.length >= 3) {
            val mBytes = message.message
            if (0xb0.toByte() <= mBytes[0] && mBytes[0] < 0xc0.toByte() && mBytes[1].toInt() == 7) {
                return mBytes[0].toInt() and 15
            }
        }
        return -1
    }

    private val channelVolume: IntArray
    private var globalVolume = 0f
    private val synthReceiver: Receiver

    /** Create a VolumeScalingReceiver connected to a specific receiver.  */
    init {
        channelVolume = IntArray(16)
        synthReceiver = delegate
        Arrays.fill(channelVolume, 127)
    }

    companion object {
        /** Guess which is the "best" available synthesizer & create a
         * VolumeScalingReceiver that forwards to it.
         *
         * @return a `VolumeScalingReceiver` connected to a semi-
         * intelligently-chosen synthesizer.
         */
        fun getInstance(): VolumeScalingReceiver? {
            return try {
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
                Collections.sort(dInfos, VolumeScalingReceiver.MidiDeviceComparator())
                val dInfo = dInfos[0]
                val dev = MidiSystem.getMidiDevice(dInfo)
                dev.open()
                VolumeScalingReceiver(dev.receiver)
            } catch (ex: MidiUnavailableException) {
                null
            }
        }
    }
}