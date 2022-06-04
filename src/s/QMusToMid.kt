package s

import com.hiperbou.lang.times
import s.DoomIO.Companion.freadint
import s.DoomIO.Endian
import java.io.*

class QMusToMid {
    class Ptr<a>(var `val`: a) {
        fun get(): a {
            return `val`
        }

        fun set(newval: a) {
            `val` = newval
        }
    }

    class MUSheader {
        var ID = ByteArray(4) /* identifier "MUS" 0x1A */
        var ScoreLength = 0
        var ScoreStart = 0
        var channels /* count of primary channels */ = 0
        var SecChannels /* count of secondary channels (?) */ = 0
        var InstrCnt = 0
        var dummy = 0

        /* variable-length part starts here */
        lateinit var instruments: IntArray
    }

    class Track {
        var current: Long = 0
        var vel: Byte = 0
        var DeltaTime: Long = 0
        var LastEvent: Byte = 0
        var data /* Primary data */: ByteArray? = null
    }

    var TRACKBUFFERSIZE = 65536L /* 64 Ko */
    fun TWriteByte(MIDItrack: Int, byte_: Byte, track: Array<QMusToMid.Track?>) {
        val pos: Long
        pos = track[MIDItrack]!!.current
        if (pos < TRACKBUFFERSIZE) track[MIDItrack]!!.data!![pos.toInt()] = byte_ else {
            println(
                """
    ERROR : Track buffer full.
    Increase the track buffer size (option -size).
    
    """.trimIndent()
            )
            System.exit(1)
        }
        track[MIDItrack]!!.current++
    }

    fun TWriteVarLen(
        tracknum: Int, value: Long,
        track: Array<QMusToMid.Track?>
    ) {
        var value = value
        var buffer: Long
        buffer = value and 0x7fL
        while (7.let { value = value shr it; value } != 0L) {
            buffer = buffer shl 8
            buffer = buffer or 0x80L
            buffer += value and 0x7fL
        }
        while (true) {
            TWriteByte(tracknum, buffer.toByte(), track)
            buffer = if (buffer and 0x80L != 0L) buffer shr 8 else break
        }
    }

    fun ReadMUSheader(MUSh: MUSheader, file: InputStream): Int {
        return try {
            if (DoomIO.fread(MUSh.ID, 4, 1, file) != 1) return QMusToMid.COMUSFILE

            /*if( strncmp( MUSh->ID, MUSMAGIC, 4 ) ) 
	    return NOTMUSFILE ;*/if (freadint(file).also {
                    MUSh.ScoreLength = it
                } == -1) return QMusToMid.COMUSFILE
            if (freadint(file).also { MUSh.ScoreStart = it } == -1) return QMusToMid.COMUSFILE
            if (freadint(file).also { MUSh.channels = it } == -1) return QMusToMid.COMUSFILE
            if (freadint(file).also { MUSh.SecChannels = it } == -1) return QMusToMid.COMUSFILE
            if (freadint(file).also { MUSh.InstrCnt = it } == -1) return QMusToMid.COMUSFILE
            if (freadint(file).also { MUSh.dummy = it } == -1) return QMusToMid.COMUSFILE
            MUSh.instruments = IntArray(MUSh.InstrCnt)
            for (i in 0 until MUSh.InstrCnt) {
                if (freadint(file).also { MUSh.instruments[i] = it } == -1) {
                    return QMusToMid.COMUSFILE
                }
            }
            0
        } catch (e: Exception) {
            e.printStackTrace()
            QMusToMid.COMUSFILE
        }
    }

    fun WriteMIDheader(ntrks: Int, division: Int, file: Any?): Int {
        try {
            //_D_: those two lines for testing purposes only
            //fisTest.close();
            //fisTest = new FileInputStream("C:\\Users\\David\\Desktop\\qmus2mid\\test.mid");
            DoomIO.fwrite(QMusToMid.MIDIMAGIC, 10, 1, file)
            DoomIO.fwrite2(DoomIO.toByteArray(ntrks), 2, file)
            DoomIO.fwrite2(DoomIO.toByteArray(division), 2, file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }

    fun last(e: Int): Byte {
        return (e and 0x80).toByte()
    }

    fun event_type(e: Int): Byte {
        return (e and 0x7F shr 4).toByte()
    }

    fun channel(e: Int): Byte {
        return (e and 0x0F).toByte()
    }

    fun TWriteString(
        tracknum: Char, string: String, length: Int,
        track: Array<QMusToMid.Track?>
    ) {
        length.times {
            TWriteByte(tracknum.code, string[it].code.toByte(), track)
        }
    }

    fun WriteTrack(tracknum: Int, file: Any?, track: Array<QMusToMid.Track?>) {
        var size: Long
        val quot: Int
        val rem: Int
        try {
            /* Do we risk overflow here ? */
            size = track[tracknum]!!.current + 4
            DoomIO.fwrite("MTrk", 4, 1, file)
            if (tracknum == 0) size += 33
            DoomIO.fwrite2(DoomIO.toByteArray(size.toInt(), 4), 4, file)
            if (tracknum == 0) DoomIO.fwrite(
                QMusToMid.TRACKMAGIC1 + "Quick MUS->MID ! by S.Bacquet",
                33,
                1,
                file
            )
            quot = (track[tracknum]!!.current / 4096).toInt()
            rem = (track[tracknum]!!.current - quot * 4096).toInt()
            DoomIO.fwrite(track[tracknum]!!.data!!, track[tracknum]!!.current.toInt(), 1, file)
            DoomIO.fwrite(QMusToMid.TRACKMAGIC2, 4, 1, file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun WriteFirstTrack(file: Any?) {
        try {
            val size: ByteArray = DoomIO.toByteArray(43, 4)
            DoomIO.fwrite("MTrk", 4, 1, file)
            DoomIO.fwrite2(size, 4, file)
            DoomIO.fwrite(QMusToMid.TRACKMAGIC3, 4, 1, file)
            DoomIO.fwrite("QMUS2MID (C) S.Bacquet", 22, 1, file)
            DoomIO.fwrite(QMusToMid.TRACKMAGIC4, 6, 1, file)
            DoomIO.fwrite(QMusToMid.TRACKMAGIC5, 7, 1, file)
            DoomIO.fwrite(QMusToMid.TRACKMAGIC6, 4, 1, file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun ReadTime(file: InputStream): Long {
        var time: Long = 0
        var byte_: Int
        do {
            byte_ = getc(file)
            if (byte_ != QMusToMid.EOF) time = (time shl 7) + (byte_ and 0x7F)
        } while (byte_ != QMusToMid.EOF && byte_ and 0x80 != 0)
        return time
    }

    fun FirstChannelAvailable(MUS2MIDchannel: ByteArray): Byte {

        val old15 = MUS2MIDchannel[15]
        var max: Byte = -1
        MUS2MIDchannel[15] = -1
        16.times { i ->
            if (MUS2MIDchannel[i] > max) max = MUS2MIDchannel[i]
        }
        MUS2MIDchannel[15] = old15
        return if (max.toInt() == 8) 10 else (max + 1).toByte()
    }

    @Throws(IOException::class)
    fun getc(`is`: InputStream): Int {
        return `is`.read()
    }

    @Throws(IOException::class)
    fun qmus2mid(
        mus: InputStream, mid: Any?, nodisplay: Boolean,
        division: Int, BufferSize: Int, nocomp: Boolean
    ): Int {
        var division = division
        val track = arrayOfNulls<QMusToMid.Track>(16)
        for (i in track.indices) track[i] = QMusToMid.Track()
        var TrackCnt = 0
        var et: Byte
        var MUSchannel: Byte
        var MIDIchannel: Byte
        var MIDItrack: Byte
        var NewEvent: Byte
        var event: Int
        var data: Int
        val r: Int
        val MUSh = MUSheader()
        var DeltaTime: Long
        var TotalTime: Long = 0
        var time: Long
        var min: Long
        var n: Long = 0
        val MUS2MIDcontrol = byteArrayOf(
            0,  /* Program change - not a MIDI control change */
            0x00,  /* Bank select */
            0x01,  /* Modulation pot */
            0x07,  /* Volume */
            0x0A,  /* Pan pot */
            0x0B,  /* Expression pot */
            0x5B,  /* Reverb depth */
            0x5D,  /* Chorus depth */
            0x40,  /* Sustain pedal */
            0x43,  /* Soft pedal */
            0x78,  /* All sounds off */
            0x7B,  /* All notes off */
            0x7E,  /* Mono */
            0x7F,  /* Poly */
            0x79 /* Reset all controllers */
        )
        val MIDIchan2track = ByteArray(16)
        val MUS2MIDchannel = ByteArray(16)
        var ouch = 0.toChar()
        var sec: Char
        DoomIO.writeEndian = Endian.LITTLE
        r = ReadMUSheader(MUSh, mus)
        if (r != 0) {
            return r
        }
        /* if( fseek( file_mus, MUSh.ScoreStart, SEEK_SET ) )
	    {
	      Close() ;
	      return MUSFILECOR ;
	    }*/if (!nodisplay) println(
            """$mus (${mus.available()}  bytes) contains ${MUSh.channels} melodic channel${if (MUSh.channels >= 2) "s" else ""}
"""
        )
        if (MUSh.channels > 15) /* <=> MUSchannels+drums > 16 */ {
            return QMusToMid.TOOMCHAN
        }

        16.times { i ->
            MUS2MIDchannel[i] = -1
            track[i]!!.current = 0
            track[i]!!.vel = 64
            track[i]!!.DeltaTime = 0
            track[i]!!.LastEvent = 0
            track[i]!!.data = null
        }
        if (BufferSize != 0) {
            TRACKBUFFERSIZE = BufferSize.toLong() shl 10
            if (!nodisplay) println("Track buffer size set to $BufferSize KB.\n")
        }
        if (!nodisplay) {
            println("Converting...")
        }
        event = getc(mus)
        et = event_type(event)
        MUSchannel = channel(event)
        while (et.toInt() != 6 && mus.available() > 0 && event != QMusToMid.EOF) {
            if (MUS2MIDchannel[MUSchannel.toInt()].toInt() == -1) {
                MUS2MIDchannel[MUSchannel.toInt()] =
                    if (MUSchannel.toInt() == 15) 9 else FirstChannelAvailable(MUS2MIDchannel)
                MIDIchannel = MUS2MIDchannel[MUSchannel.toInt()]
                MIDIchan2track[MIDIchannel.toInt()] = TrackCnt++.toByte()
                MIDItrack = MIDIchan2track[MIDIchannel.toInt()]
                if (ByteArray(TRACKBUFFERSIZE.toInt()).also { track[MIDItrack.toInt()]!!.data = it } == null) {
                    return QMusToMid.MEMALLOC
                }
            } else {
                MIDIchannel = MUS2MIDchannel[MUSchannel.toInt()]
                MIDItrack = MIDIchan2track[MIDIchannel.toInt()]
            }
            TWriteVarLen(MIDItrack.toInt(), track[MIDItrack.toInt()]!!.DeltaTime, track)
            track[MIDItrack.toInt()]!!.DeltaTime = 0
            when (et.toInt()) {
                0 -> {
                    NewEvent = (0x90 or MIDIchannel.toInt()).toByte()
                    if (NewEvent != track[MIDItrack.toInt()]!!.LastEvent || nocomp) {
                        TWriteByte(MIDItrack.toInt(), NewEvent, track)
                        track[MIDItrack.toInt()]!!.LastEvent = NewEvent
                    } else n++
                    data = getc(mus)
                    TWriteByte(MIDItrack.toInt(), data.toByte(), track)
                    TWriteByte(MIDItrack.toInt(), 0.toByte(), track)
                }
                1 -> {
                    NewEvent = (0x90 or MIDIchannel.toInt()).toByte()
                    if (NewEvent != track[MIDItrack.toInt()]!!.LastEvent || nocomp) {
                        TWriteByte(MIDItrack.toInt(), NewEvent, track)
                        track[MIDItrack.toInt()]!!.LastEvent = NewEvent
                    } else n++
                    data = getc(mus)
                    TWriteByte(MIDItrack.toInt(), (data and 0x7F).toByte(), track)
                    if (data and 0x80 != 0) track[MIDItrack.toInt()]!!.vel = getc(mus).toByte()
                    TWriteByte(MIDItrack.toInt(), track[MIDItrack.toInt()]!!.vel, track)
                }
                2 -> {
                    NewEvent = (0xE0 or MIDIchannel.toInt()).toByte()
                    if (NewEvent != track[MIDItrack.toInt()]!!.LastEvent || nocomp) {
                        TWriteByte(MIDItrack.toInt(), NewEvent, track)
                        track[MIDItrack.toInt()]!!.LastEvent = NewEvent
                    } else n++
                    data = getc(mus)
                    TWriteByte(MIDItrack.toInt(), (data and 1 shl 6).toByte(), track)
                    TWriteByte(MIDItrack.toInt(), (data shr 1).toByte(), track)
                }
                3 -> {
                    NewEvent = (0xB0 or MIDIchannel.toInt()).toByte()
                    if (NewEvent != track[MIDItrack.toInt()]!!.LastEvent || nocomp) {
                        TWriteByte(MIDItrack.toInt(), NewEvent, track)
                        track[MIDItrack.toInt()]!!.LastEvent = NewEvent
                    } else n++
                    data = getc(mus)
                    TWriteByte(MIDItrack.toInt(), MUS2MIDcontrol[data], track)
                    if (data == 12) TWriteByte(MIDItrack.toInt(), (MUSh.channels + 1).toByte(), track) else TWriteByte(
                        MIDItrack.toInt(),
                        0.toByte(),
                        track
                    )
                }
                4 -> {
                    data = getc(mus)
                    if (data != 0) {
                        NewEvent = (0xB0 or MIDIchannel.toInt()).toByte()
                        if (NewEvent != track[MIDItrack.toInt()]!!.LastEvent || nocomp) {
                            TWriteByte(MIDItrack.toInt(), NewEvent, track)
                            track[MIDItrack.toInt()]!!.LastEvent = NewEvent
                        } else n++
                        TWriteByte(MIDItrack.toInt(), MUS2MIDcontrol[data], track)
                    } else {
                        NewEvent = (0xC0 or MIDIchannel.toInt()).toByte()
                        if (NewEvent != track[MIDItrack.toInt()]!!.LastEvent || nocomp) {
                            TWriteByte(MIDItrack.toInt(), NewEvent, track)
                            track[MIDItrack.toInt()]!!.LastEvent = NewEvent
                        } else n++
                    }
                    data = getc(mus)
                    TWriteByte(MIDItrack.toInt(), data.toByte(), track)
                }
                5, 7 -> return QMusToMid.MUSFILECOR
                else -> {}
            }
            if (last(event).toInt() != 0) {
                DeltaTime = ReadTime(mus)
                TotalTime += DeltaTime
                TrackCnt.times {
                    track[it]!!.DeltaTime += DeltaTime
                }
            }
            event = getc(mus)
            if (event != QMusToMid.EOF) {
                et = event_type(event)
                MUSchannel = channel(event)
            } else ouch = 1.toChar()
        }
        if (!nodisplay) println("done !\n")
        if (ouch.code != 0) println(
            """WARNING : There are bytes missing at the end of $mus.
          The end of the MIDI file might not fit the original one.
"""
        )
        if (division == 0) division = 89 else if (!nodisplay) println("Ticks per quarter note set to $division.\n")
        if (!nodisplay) {
            if (division != 89) {
                time = TotalTime / 140
                min = time / 60
                sec = Char((time - min * 60).toUShort())
                //System.out.println( "Playing time of the MUS file : %u'%.2u''.\n", min, sec ) ;
            }
            time = TotalTime * 89 / (140 * division)
            min = time / 60
            sec = Char((time - min * 60).toUShort())
            if (division != 89) println("                    MID file") else println("Playing time: " + min + "min " + sec + "sec")
        }
        if (!nodisplay) {
            println("Writing...")
        }
        WriteMIDheader(TrackCnt + 1, division, mid)
        WriteFirstTrack(mid)
        TrackCnt.times {
            WriteTrack(it, mid, track)
        }
        if (!nodisplay) println("done !\n")
        if (!nodisplay && !nocomp) println(
            "Compression : %u%%.\n" /*,
	           (100 * n) / (n+ (long) ftell( mid ))*/
        )
        return 0
    }

    @Throws(IOException::class)
    fun convert(
        mus: String?, mid: String?, nodisplay: Boolean, div: Int,
        size: Int, nocomp: Boolean, ow: Ptr<Int?>?
    ): Int {
        val `is`: InputStream = BufferedInputStream(FileInputStream(File(mid)))
        val os: OutputStream = BufferedOutputStream(FileOutputStream(File(mid)))
        var error: Int
        //struct stat file_data ;
        val buffer = CharArray(30)


        /* we don't need _all_ that checking, do we ? */
        /* Answer : it's more user-friendly */
        /*#ifdef MSDOG

	  if( access( mus, 0 ) )
	    {
	      System.out.println( "ERROR : %s does not exist.\n", mus ) ;
	      return 1 ;
	    }

	  if( !access( mid, 0 ) )
	    {
	      if( !*ow )
	        {
	          System.out.println( "Can't overwrite %s.\n", mid ) ;
	          return 2 ;
	        }
	      if( *ow == 1 )
	        {
	          System.out.println( "%s exists : overwrite (Y=Yes,N=No,A=yes for All,Q=Quit)"
	                 " ? [Y]\b\b", mid ) ;
	          fflush( stdout ) ;
	          do
	            n = toupper( getxkey() ) ;
	          while( (n != 'Y') && (n != 'N') && (n != K_Return) && (n != 'A')
	                && (n != 'Q')) ;
	          switch( n )
	            {
	            case 'N' :
	              System.out.println( "N\n%s NOT converted.\n", mus ) ;
	              return 3 ;
	            case 'A' :
	              System.out.println( "A" ) ;
	              *ow = 2 ;
	              break ;
	            case 'Q' :
	              System.out.println( "Q\nQMUS2MID aborted.\n" ) ;
	              exit( 0 ) ;
	              break ;
	            default : break ;
	            }
	          System.out.println( "\n" ) ;
	        }
	    }
	#else*/
        /*if ( ow.get() == 0 ) {
	    file = fopen(mid, "r");
	    if ( file ) {
	      fclose(file);
	      System.out.println( "qmus2mid: file %s exists, not removed.\n", mid ) ;
	      return 2 ;
	    }
	  }*/
        /*#endif*/return convert(`is`, os, nodisplay, div, size, nocomp, ow)
    }

    @Throws(IOException::class)
    fun convert(
        mus: InputStream, mid: Any?, nodisplay: Boolean, div: Int,
        size: Int, nocomp: Boolean, ow: Ptr<Int?>?
    ): Int {
        val error = qmus2mid(mus, mid, nodisplay, div, size, nocomp)
        if (error != 0) {
            println("ERROR : ")
            when (error) {
                QMusToMid.NOTMUSFILE -> println("%s is not a MUS file.\n" /*, mus*/)
                QMusToMid.COMUSFILE -> println("Can't open %s for read.\n" /*, mus*/)
                QMusToMid.COTMPFILE -> println("Can't open temp file.\n")
                QMusToMid.CWMIDFILE -> println("Can't write %s (?).\n" /*, mid */)
                QMusToMid.MUSFILECOR -> println("%s is corrupted.\n" /*, mus*/)
                QMusToMid.TOOMCHAN -> println("%s contains more than 16 channels.\n" /*, mus*/)
                QMusToMid.MEMALLOC -> println("Not enough memory.\n")
                else -> {}
            }
            return 4
        }
        if (!nodisplay) {
            println("$mus converted successfully.\n")
            /*if( (file = fopen( mid, "rb" )) != NULL )
	        {
	          //stat( mid, &file_data ) ;
	          fclose( file ) ;
	          sSystem.out.println( buffer, " : %lu bytes", (long) file_data.st_size ) ;
	        }*/

            /*System.out.println( "%s (%scompressed) written%s.\n", mid, nocomp ? "NOT " : "",
	             file ? buffer : ""  ) ;*/
        }
        return 0
    }

    //	int CheckParm( char[] check, int argc, char *argv[] )
    //	{
    //	  int i;
    //
    //	  for ( i = 1 ; i<argc ; i++ )
    //	/*#ifdef MSDOG
    //	    if( !stricmp( check, argv[i] ) )
    //	#else*/
    //	    if( !strcmp( check, argv[i] ) )
    //	/*#endif*/
    //	      return i ;
    //
    //	  return 0;
    //	}
    fun PrintHeader() {
//	  System.out.println( "===============================================================================\n"
//	         "              Quick MUS->MID v2.0 ! (C) 1995,96 Sebastien Bacquet\n"
//	         "                        E-mail : bacquet@iie.cnam.fr\n"
//	         "===============================================================================\n" ) ;
    }

    fun PrintSyntax() {
//	  PrintHeader() ;
//	  System.out.println( 
//	#ifdef MSDOG
//	         "\nSyntax : QMUS2MID musfile1[.mus] {musfile2[.mus] ... | "
//	         "midifile.mid} [options]\n"
//	         "   Wildcards are accepted.\n"
//	         "   Options are :\n"
//	         "     -query    : Query before processing\n"
//	         "     -ow       : OK, overwrite (without query)\n"
//	#else
//	         "\nSyntax : QMUS2MID musfile midifile [options]\n"
//	         "   Options are :\n"
//	#endif
//	         "     -noow     : Don't overwrite !\n"
//	         "     -nodisp   : Display nothing ! (except errors)\n"
//	         "     -nocomp   : Don't compress !\n"
//	         "     -size ### : Set the track buffer size to ### (in KB). "
//	         "Default = 64 KB\n"
//	         "     -t ###    : Ticks per quarter note. Default = 89\n" 
//	         ) ;
    }

    fun main(argc: Int, argv: CharArray?): Int {
        val div = 0
        val ow = 1
        val nocomp = 0
        val size = 0
        var n: Int
        val nodisplay = false
        /*#ifdef MSDOG
	  int FileCount, query = 0, i, line = 0 ;
	  char mus[MAXPATH], mid[MAXPATH], drive[MAXDRIVE], middrive[MAXDRIVE],
	  dir[MAXDIR], middir[MAXDIR], musname[MAXFILE], midname[MAXFILE],
	  ext[MAXEXT] ;
	  struct stat s ;
	#else*/
        var mus: String
        var mid: String
        /*#endif*/


        /*#ifndef MSDOG
	  if ( !LittleEndian() ) {
	    System.out.println("\nSorry, this program presently only works on "
		   "little-endian machines... \n\n");
	    exit( EXIT_FAILURE ) ;
	  }
	#endif*/

        /*#ifdef MSDOG
	  if( (argc == 1) || (argv[1][0] == '-') )
	#else
	    if( argc < 3 )
	#endif
	      {
	        PrintSyntax() ;
	        exit( EXIT_FAILURE ) ;
	      }*/

        /*#ifdef MSDOG
	  if( (strrchr( argv[1], '*' ) != NULL) || (strrchr( argv[1], '?' ) != NULL) )
	    {
	      PrintHeader() ;
	      System.out.println( "Sorry, there is nothing matching %s...\n", argv[1] ) ;
	      exit( EXIT_FAILURE ) ;
	    }
	  strncpy( mus, argv[1], MAXPATH ) ;
	  strupr( mus ) ;
	  if( !(fnsplit( mus, drive, dir, musname, NULL ) & FILENAME) )
	    {
	      PrintSyntax() ;
	      exit( EXIT_FAILURE ) ;
	    }
	#else*/
        //strncpy( mus, argv[1], FILENAME_MAX ) ;
        //strncpy( mid, argv[2], FILENAME_MAX ) ;
        /*#endif*/

        /*#ifdef MSDOG
	  if( CheckParm( "-query", argc, argv ) )
	    query = 1 ;
	#endif*/

        /*  if( CheckParm( "-nodisp", argc, argv ) )
	    nodisplay = 1 ;
	  */if (!nodisplay) PrintHeader()

        /*if( (n = CheckParm( "-size", argc, argv )) != 0 )
	    size = atoi( argv[n+1] ) ;*/
        /*#ifdef MSDOG
	  if( CheckParm( "-ow", argc, argv ) )
	    ow += 1 ;
	#endif
	  if( CheckParm( "-noow", argc, argv ) )
	    ow -= 1 ;
	  if( (n = CheckParm( "-t", argc, argv )) != 0 )
	    div = atoi( argv[n+1] ) ;
	  if( CheckParm( "-nocomp", argc, argv ) )
	    nocomp = 1 ;*/

        /*#ifdef MSDOG
	  for( FileCount = 1 ; (FileCount < argc) && (argv[FileCount][0] != '-') ;
	      FileCount++ ) ;
	  FileCount-- ;
	  midname[0] = middrive[0] = middir[0] = 0 ;
	  if( FileCount == 2 )
	    {
	      if( fnsplit( argv[FileCount], middrive, middir, midname, ext )
	         & FILENAME )
	        {
	          if( stricmp( ext, ".MID" ) )
	            midname[0] = middrive[0] = middir[0] = 0 ;
	          else
	            {
	              strcpy( mid, argv[FileCount--] ) ;
	              strupr( mid ) ;
	            }
	        }
	      else
	        FileCount-- ;
	    }
	  if( FileCount > 2 )
	    {
	      if( fnsplit( argv[FileCount], middrive, middir, NULL, NULL ) & FILENAME )
	        midname[0] = middrive[0] = middir[0] = 0 ;
	      else
	        FileCount-- ;
	    }
	  for( i = 0 ; i < FileCount ; i++ )
	    {
	      strupr( argv[i+1] ) ;
	      n = fnsplit( argv[i+1], drive, dir, musname, ext ) ;
	      if( !(n & EXTENSION) || !stricmp( ext, ".MUS" ) )
	        {
	          stat( argv[i+1], &s ) ;
	          if( !S_ISDIR( s.st_mode ) )
	            {
	              fnmerge( mus, drive, dir, musname, ".MUS" ) ;
	              if( line && !nodisplay )
	                System.out.println( "\n" ) ;
	              if( query )
	                {
	                  System.out.println( "Convert %s ? (Y=Yes,N=No,A=yes for All,Q=Quit)"
	                         " [Y]\b\b", mus ) ;
	                  fflush( stdout ) ;
	                  do
	                    n = toupper( getxkey() ) ;
	                  while( (n != 'Y') && (n != 'N') && (n != K_Return) 
	                        && (n != 'A') && (n != 'Q')) ;
	                  switch( n )
	                    {
	                    case 'N' :
	                      System.out.println( "N\n%s NOT converted.\n", mus ) ;
	                      line = 1 ;
	                      continue ;
	                      break ;
	                    case 'Q' :
	                      System.out.println( "Q\nQMUS2MID aborted.\n" ) ;
	                      exit( 0 ) ;
	                      break ;
	                    case 'A' :
	                      query = 0 ;
	                      System.out.println( "A\n" ) ;
	                      break ;
	                    default :
	                      System.out.println( "\n" ) ;
	                      break ;
	                    }
	                }
	              if( !midname[0] )
	                {
	                  fnmerge( mid, middrive, middir, musname, ".MID" ) ;
	                  strupr( mid ) ;
	                }
	              convert( mus, mid, nodisplay, div, size, nocomp, &ow ) ;
	              line = 1 ;
	            }
	        }
	    }
	  if( !line && !nodisplay && !query )
	    System.out.println( "Sorry, there is no MUS file matching...\n" ) ;
	  
	#else*/
        //convert( mus, mid, nodisplay, div, size, nocomp, ow ) ;
/*	#endif*/return 0
    }

    companion object {
        const val NOTMUSFILE = 1 /* Not a MUS file */
        const val COMUSFILE = 2 /* Can't open MUS file */
        const val COTMPFILE = 3 /* Can't open TMP file */
        const val CWMIDFILE = 4 /* Can't write MID file */
        const val MUSFILECOR = 5 /* MUS file corrupted */
        const val TOOMCHAN = 6 /* Too many channels */
        const val MEMALLOC = 7 /* Memory allocation error */

        /* some (old) compilers mistake the "MUS\x1A" construct (interpreting
	   it as "MUSx1A")      */
        const val MUSMAGIC = "MUS\u001a" /* this seems to work */
        const val MIDIMAGIC = "MThd\u0000\u0000\u0000\u0006\u0000\u0001"
        const val TRACKMAGIC1 = "\u0000\u00ff\u0003\u001d"
        const val TRACKMAGIC2 = "\u0000\u00ff\u002f\u0000"
        const val TRACKMAGIC3 = "\u0000\u00ff\u0002\u0016"
        const val TRACKMAGIC4 = "\u0000\u00ff\u0059\u0002\u0000\u0000"
        const val TRACKMAGIC5 = "\u0000\u00ff\u0051\u0003\u0009\u00a3\u001a"
        const val TRACKMAGIC6 = "\u0000\u00ff\u002f\u0000"
        const val EOF = -1
    }
}