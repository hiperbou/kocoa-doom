package s

import s.IMusic
import s.MusReader
import s.VolumeScalingReceiver
import java.io.ByteArrayInputStream
import javax.sound.midi.*
import kotlin.sequences.Sequence
import data.Tables.BITS32;
import data.Tables.finecosine;
import data.Tables.finesine;
import data.info.mobjinfo;
import data.mobjtype_t;
import doom.SourceCode.angle_t;
import m.fixed_t.Companion.FRACBITS;
import m.fixed_t.Companion.FRACUNIT;
import m.fixed_t.Companion.FixedMul;
import m.fixed_t.Companion.FixedDiv
import p.MapUtils.AproxDistance;
import p.mobj_t;
import utils.C2JUtils.eval;
import doom.player_t;
import doom.weapontype_t;
import m.fixed_t.Companion.MAPFRACUNIT;
import doom.SourceCode
import java.nio.ByteBuffer
import m.BBox
import doom.DoomMain

/** Concern separated from David Martel's MIDI & MUS player
 * for Mocha Doom. Greatly improved upon by finnw, perfecting volume changes
 * and MIDI device detection.
 *
 * @author David Martel
 * @author velktron
 * @author finnw
 */
class DavidMusicModule : IMusic {
    var sequencer: Sequencer? = null
    var receiver: VolumeScalingReceiver? = null
    lateinit var transmitter: Transmitter
    var songloaded = false
    override fun InitMusic() {
        try {
            var x = -1
            val info = MidiSystem.getMidiDeviceInfo()
            for (i in info.indices) {
                val mdev = MidiSystem.getMidiDevice(info[i])
                if (mdev is Sequencer) x = i
                //  System.out.println(info[i].getName()+"\t\t\t"+ mdev.isOpen()+"\t"+mdev.hashCode());
            }

            //System.out.printf("x %d y %d \n",x,y);
            //--This sets the Sequencer and Synthesizer  
            //--The indices x and y correspond to the correct entries for the  
            //--default Sequencer and Synthesizer, as determined above  	       
            sequencer =
                if (x != -1) MidiSystem.getMidiDevice(info[x]) as Sequencer else MidiSystem.getSequencer(false) as Sequencer
            sequencer!!.open()
            receiver = VolumeScalingReceiver.getInstance()
            // Configure General MIDI level 1
            DavidMusicModule.sendSysexMessage(
                receiver,
                0xf0.toByte(),
                0x7e.toByte(),
                0x7f.toByte(),
                9.toByte(),
                1.toByte(),
                0xf7.toByte()
            )
            transmitter = sequencer!!.transmitter
            transmitter.setReceiver(receiver)
        } catch (e: MidiUnavailableException) {
            e.printStackTrace()
        }
    }

    override fun ShutdownMusic() {
        sequencer!!.stop()
        sequencer!!.close()
    }

    override fun SetMusicVolume(volume: Int) {
        println("Midi volume set to $volume")
        receiver!!.setGlobalVolume(volume / 127f)
    }

    override fun PauseSong(handle: Int) {
        if (songloaded) sequencer!!.stop()
    }

    override fun ResumeSong(handle: Int) {
        if (songloaded) {
            println("Resuming song")
            sequencer!!.start()
        }
    }

    override fun RegisterSong(data: ByteArray?): Int {
        try {
            var sequence: javax.sound.midi.Sequence? = null
            var bis: ByteArrayInputStream
            try {
                // If data is a midi file, load it directly
                bis = ByteArrayInputStream(data)
                sequence = MidiSystem.getSequence(bis)
            } catch (ex: InvalidMidiDataException) {
                // Well, it wasn't. Dude.
                bis = ByteArrayInputStream(data)
                sequence = MusReader.getSequence(bis)
            }
            sequencer!!.stop() // stops current music if any
            sequencer!!.sequence = sequence // Create a sequencer for the sequence
            songloaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
        // In good old C style, we return 0 upon success?
        return 0
    }

    override fun PlaySong(handle: Int, looping: Boolean) {
        if (songloaded) {
            for (midiChan in 0..15) {
                setPitchBendSensitivity(receiver, midiChan, 2)
            }
            if (looping) sequencer!!.loopCount = Sequencer.LOOP_CONTINUOUSLY else sequencer!!.loopCount = 0
            sequencer!!.start() // Start playing
        }
    }

    private fun setPitchBendSensitivity(receiver: Receiver?, midiChan: Int, semitones: Int) {
        sendRegParamChange(receiver, midiChan, 0, 0, 2)
    }

    private fun sendRegParamChange(receiver: Receiver?, midiChan: Int, paramMsb: Int, paramLsb: Int, valMsb: Int) {
        DavidMusicModule.sendControlChange(receiver, midiChan, 101, paramMsb)
        DavidMusicModule.sendControlChange(receiver, midiChan, 100, paramLsb)
        DavidMusicModule.sendControlChange(receiver, midiChan, 6, valMsb)
    }

    override fun StopSong(handle: Int) {
        sequencer!!.stop()
    }

    override fun UnRegisterSong(handle: Int) {
        // In theory, we should ask the sequencer to "forget" about the song.
        // However since we can register another without unregistering the first,
        // this is practically a dummy.
        songloaded = false
    }

    companion object {
        const val CHANGE_VOLUME = 7
        const val CHANGE_VOLUME_FINE = 9
        private fun sendControlChange(receiver: Receiver?, midiChan: Int, ctrlId: Int, value: Int) {
            val msg = ShortMessage()
            try {
                msg.setMessage(ShortMessage.CONTROL_CHANGE, midiChan, ctrlId, value)
            } catch (ex: InvalidMidiDataException) {
                throw RuntimeException(ex)
            }
            receiver?.send(msg, -1)
        }

        private fun sendSysexMessage(receiver: Receiver?, vararg message: Byte) {
            val msg = SysexMessage()
            try {
                msg.setMessage(message, message.size)
            } catch (ex: InvalidMidiDataException) {
                throw RuntimeException(ex)
            }
            receiver?.send(msg, -1)
        }
    }
}