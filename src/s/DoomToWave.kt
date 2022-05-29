package s

import s.*
import s.DoomIO.Endian
import utils.C2JUtils
import java.nio.ByteOrder
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DoomToWave {
    class RIFFHEAD {
        var riff = ByteArray(4)
        var length = 0
        var wave = ByteArray(4)
        fun pack(b: ByteBuffer) {
            b.put(riff)
            b.putInt(length)
            b.put(wave)
        }

        fun size(): Int {
            return 12
        }
    }

    var headr = RIFFHEAD()

    class CHUNK {
        var name = ByteArray(4)
        var size = 0
        fun pack(b: ByteBuffer) {
            b.put(name)
            b.putInt(size)
        }

        fun size(): Int {
            return 8
        }
    }

    var headc = CHUNK()

    class WAVEFMT {
        var fmt = ByteArray(4) /* "fmt " */
        var fmtsize /*0x10*/ = 0
        var tag /*format tag. 1=PCM*/ = 0
        var channel /*1*/ = 0
        var smplrate = 0
        var bytescnd /*average bytes per second*/ = 0
        var align /*block alignment, in bytes*/ = 0
        var nbits /*specific to PCM format*/ = 0
        fun pack(b: ByteBuffer) {
            b.put(fmt)
            b.putInt(fmtsize)
            b.putChar(tag.toChar())
            b.putChar(channel.toChar())
            b.putInt(smplrate)
            b.putInt(bytescnd)
            b.putChar(align.toChar())
            b.putChar(nbits.toChar())
        }

        fun size(): Int {
            return 24
        }
    }

    var headf = WAVEFMT()
    var SIZEOF_WAVEFMT = 24

    class WAVEDATA /*data*/ {
        var data = ByteArray(4) /* "data" */
        var datasize = 0
        fun pack(b: ByteBuffer) {
            b.put(data)
            b.putInt(datasize)
        }
    }

    var headw = WAVEDATA()
    var SIZEOF_WAVEDATA = 8
    @Throws(IOException::class)
    fun SNDsaveSound(`is`: InputStream, os: OutputStream) {
        val type: Int = DoomIO.freadint(`is`, 2) //  peek_i16_le (buffer);
        val speed: Int = DoomIO.freadint(`is`, 2) //peek_u16_le (buffer + 2);
        var datasize: Int = DoomIO.freadint(`is`, 4) //peek_i32_le (buffer + 4);
        if (type != 3) println("Sound: weird type $type. Extracting anyway.")
        val headsize = 2 + 2 + 4
        val size = `is`.available()
        if (datasize > size) {
            println(
                "Sound %s: declared sample size %lu greater than lump size %lu ;" /*,
		lump_name (name), (unsigned long) datasize, (unsigned long) phys_size*/
            )
            println("Sound %s: truncating to lump size." /*, lump_name (name)*/)
            datasize = size
        } else if (datasize < size) {
            if ( /*fullSND == TRUE*/true) /* Save entire lump */ datasize = size else {
                /*Warning (
		"Sound %s: lump size %lu greater than declared sample size %lu ;",
		lump_name (name), (unsigned long) datasize, (unsigned long) phys_size);
	      Warning ("Sound %s: truncating to declared sample size.",
		  lump_name (name));*/
            }
        }
        DoomIO.writeEndian = Endian.BIG
        SNDsaveWave(`is`, os, speed, datasize)
    }

    @Throws(IOException::class)
    fun DMX2Wave(DMXSound: ByteArray?): ByteArray {
        val `is` = ByteBuffer.wrap(DMXSound)
        `is`.order(ByteOrder.LITTLE_ENDIAN)
        val type = 0x0000FFFF and `is`.short.toInt() //  peek_i16_le (buffer);
        val speed = 0x0000FFFF and `is`.short.toInt() //peek_u16_le (buffer + 2);
        var datasize = `is`.int //peek_i32_le (buffer + 4);
        if (type != 3) println("Sound: weird type $type. Extracting anyway.")
        val headsize = 2 + 2 + 4
        val size = `is`.remaining()
        if (datasize > size) {
            println(
                "Sound %s: declared sample size %lu greater than lump size %lu ;" /*,
			lump_name (name), (unsigned long) datasize, (unsigned long) phys_size*/
            )
            println("Sound %s: truncating to lump size." /*, lump_name (name)*/)
            datasize = size
        } else if (datasize < size) {
            if ( /*fullSND == TRUE*/true) /* Save entire lump */ datasize = size else {
                /*Warning (
			"Sound %s: lump size %lu greater than declared sample size %lu ;",
			lump_name (name), (unsigned long) datasize, (unsigned long) phys_size);
		      Warning ("Sound %s: truncating to declared sample size.",
			  lump_name (name));*/
            }
        }
        return SNDsaveWave(`is`, speed, datasize)
    }

    @Throws(IOException::class)
    protected fun SNDsaveWave(`is`: ByteBuffer, speed: Int, size: Int): ByteArray {

        // Size with header and data etc.
        val output = ByteArray(headr.size() + headf.size() + SIZEOF_WAVEDATA + 2 * size)
        val os = ByteBuffer.wrap(output)
        os.order(ByteOrder.LITTLE_ENDIAN)
        os.position(0)
        headr.riff = "RIFF".toByteArray()
        val siz = 4 + SIZEOF_WAVEFMT + SIZEOF_WAVEDATA + 2 * size
        headr.length = siz
        headr.wave = C2JUtils.toByteArray("WAVE")
        headr.pack(os)
        headf.fmt = C2JUtils.toByteArray("fmt ")
        headf.fmtsize = SIZEOF_WAVEFMT - 8
        headf.tag = 1
        headf.channel = 2 // Maes: HACK to force stereo lines.
        headf.smplrate = speed
        headf.bytescnd = 2 * speed // Ditto.
        headf.align = 1
        headf.nbits = 8
        headf.pack(os)
        headw.data = C2JUtils.toByteArray("data")
        headw.datasize = 2 * size
        //byte[] wtf=DoomIO.toByteArray(headw.datasize, 4);
        headw.pack(os)
        var tmp: Byte
        for (i in 0 until size) {
            tmp = `is`.get()
            os.put(tmp)
            os.put(tmp)
        }
        return os.array()
    }

    @Throws(IOException::class)
    fun SNDsaveWave(`is`: InputStream, os: OutputStream, speed: Int, size: Int) {
        var wsize: Int
        var sz = 0
        headr.riff = DoomIO.toByteArray("RIFF")
        val siz = 4 + SIZEOF_WAVEFMT + SIZEOF_WAVEDATA + size
        headr.length = siz
        headr.wave = DoomIO.toByteArray("WAVE")
        DoomIO.fwrite2(headr.riff, os)
        DoomIO.fwrite2(DoomIO.toByteArray(headr.length, 4), os)
        DoomIO.fwrite2(headr.wave, os)
        headf.fmt = DoomIO.toByteArray("fmt ")
        headf.fmtsize = SIZEOF_WAVEFMT - 8
        headf.tag = 1
        headf.channel = 1 // Maes: HACK to force stereo lines.
        headf.smplrate = speed
        headf.bytescnd = speed
        headf.align = 1
        headf.nbits = 8
        DoomIO.fwrite2(headf.fmt, os)
        DoomIO.fwrite2(DoomIO.toByteArray(headf.fmtsize, 4), os)
        DoomIO.fwrite2(DoomIO.toByteArray(headf.tag, 2), os)
        DoomIO.fwrite2(DoomIO.toByteArray(headf.channel, 2), os)
        DoomIO.fwrite2(DoomIO.toByteArray(headf.smplrate, 4), os)
        DoomIO.fwrite2(DoomIO.toByteArray(headf.bytescnd, 4), os)
        DoomIO.fwrite2(DoomIO.toByteArray(headf.align, 2), os)
        DoomIO.fwrite2(DoomIO.toByteArray(headf.nbits, 2), os)
        headw.data = DoomIO.toByteArray("data")
        headw.datasize = size
        DoomIO.fwrite2(headw.data, os)
        DoomIO.fwrite2(DoomIO.toByteArray(headw.datasize, 4), os)
        val shit = os as ByteArrayOutputStream
        val crap = shit.toByteArray()
        val bytes = ByteArray(DoomToWave.MEMORYCACHE)
        wsize = 0
        while (wsize < size) {
            sz = if (size - wsize > DoomToWave.MEMORYCACHE) DoomToWave.MEMORYCACHE else size - wsize
            `is`.read(bytes, 0, sz)
            os.write(bytes, 0, sz)
            wsize += sz
        }
    }

    companion object {
        var MEMORYCACHE = 0x8000
    }
}