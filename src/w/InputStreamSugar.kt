package w

import utils.C2JUtils
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * As we know, Java can be a bit awkward when handling streams e.g. you can't
 * really skip at will without doing some nasty crud. This class helps doing
 * such crud. E.g. if we are dealing with a stream that has an underlying file,
 * we can try and skip directly by using the file channel, otherwise we can try
 * (eww) closing the stream, reopening it (ASSUMING WE KNOW THE SOURCE'S URI AND
 * TYPE), and then skipping.
 *
 * @author Maes
 */
object InputStreamSugar {
    const val UNKNOWN_TYPE = 0x0
    const val FILE = 0x1 // Local file. Easiest case
    const val NETWORK_FILE = 0x2
    const val ZIP_FILE = 0x4 // Zipped file
    const val BAD_URI = -1 // Bad or unparseable 

    /**
     * Creates an inputstream from a local file, network resource, or zipped
     * file (also over a network). If an entry name is specifid AND the type is
     * specified to be zip, then a zipentry with that name will be sought.
     *
     * @param resource
     * @param contained
     * @param type
     * @return
     */
    fun createInputStreamFromURI(
        resource: String?,
        entry: ZipEntry?, type: Int
    ): InputStream? {
        var `is`: InputStream? = null
        val u: URL

        // No entry specified or no zip type, try everything BUT zip.
        if (entry == null || !C2JUtils.flags(type, InputStreamSugar.ZIP_FILE)) {
            `is` = InputStreamSugar.getDirectInputStream(resource!!)
        } else {
            // Entry specified AND type specified to be zip
            // We might want to open even a zip file without looking 
            // for any particular entry.
            if (entry != null && C2JUtils.flags(type, InputStreamSugar.ZIP_FILE)) {
                var zis: ZipInputStream
                // Try it as a NET zip file
                try {
                    u = URL(resource)
                    zis = ZipInputStream(u.openStream())
                } catch (e: Exception) {
                    // Local zip file?
                    try {
                        // Open resource as local file-backed zip input stream, 
                        // and search proper entry.
                        zis = ZipInputStream(FileInputStream(resource))
                    } catch (e1: Exception) {
                        // Well, it's not that either.
                        // At this point we almost ran out of options
                        // Try a local file and that's it.
                        `is` = InputStreamSugar.getDirectInputStream(resource!!)
                        return `is`
                    }
                }

                // All OK?
                `is` = InputStreamSugar.getZipEntryStream(zis, entry.name)
                if (`is` != null) return `is`
            }
        }

        // At this point, you'll either get a stream or jack.
        return InputStreamSugar.getDirectInputStream(resource!!)
    }

    /** Match zip entries in a ZipInputStream based only on their name.
     * Luckily (?) ZipEntries do not keep references to their originating
     * streams, so opening/closing ZipInputStreams all the time won't result
     * in a garbage hell...I hope.
     *
     * @param zis
     * @param entryname
     * @return
     */
    private fun getZipEntryStream(zis: ZipInputStream, entryname: String): InputStream? {
        var ze: ZipEntry? = null
        try {
            while (zis.nextEntry.also { ze = it } != null) {
                // Directories cannot be opened
                if (ze!!.isDirectory) continue
                if (ze!!.name == entryname) {
                    return zis
                }
            }
        } catch (e: IOException) {
            // Get jack
            return null
        }

        // Get jack
        return null
    }

    private fun getDirectInputStream(resource: String): InputStream? {
        var `is`: InputStream? = null
        val u: URL
        try { // Is it a net resource?
            u = URL(resource)
            `is` = u.openStream()
        } catch (e: Exception) {
            // OK, not a valid URL or no network. We don't care.
            // Try opening as a local file.
            try {
                `is` = FileInputStream(resource)
            } catch (e1: FileNotFoundException) {
                // Well, it's not that either.
                // At this point we really ran out of options
                // and you'll get null
            }
        }
        return `is`
    }

    /**
     * Attempt to do the Holy Grail of Java Streams, aka seek to a particular
     * position. With some types of stream, this is possible if you poke deep
     * enough. With others, it's not, and you can only close & reopen them
     * (provided you know how to do that) and then skip to a particular position
     *
     * @param is
     * @param pos
     * The desired position
     * @param URI
     * Information which can help reopen a stream, e.g. a filename, URL,
     * or zip file.
     * @peram entry If we must look into a zipfile entry
     * @return the skipped stream. Might be a totally different object.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun streamSeek(
        `is`: InputStream?, pos: Long,
        size: Long, URI: String?, entry: ZipEntry?, type: Int
    ): InputStream? {
        var `is` = `is`
        if (`is` == null) return `is`

        // If we know our actual position in the stream, we can aid seeking
        // forward

        /*
         * Too buggy :-/ pity if (knownpos>=0 && knownpos<=pos){ if
         * (pos==knownpos) return is; try{ final long mustskip=pos-knownpos;
         * long skipped=0; while (skipped<mustskip){
         * skipped+=is.skip(mustskip-skipped);
         * System.out.printf("Must skip %d skipped %d\n",mustskip,skipped); }
         * return is; } catch (Exception e){ // We couldn't skip cleanly.
         * Swallow up and try normally. System.err.println("Couldn't skip"); } }
         */

        // This is a more reliable method, although it's less than impressive in
        // results.
        if (size > 0) {
            try {
                val available = `is`.available().toLong()
                val guesspos = size - available
                // The stream is at a position before or equal to
                // our desired one. We can attempt skipping forward.
                if (guesspos > 0 && guesspos <= pos) {
                    var skipped: Long = 0
                    val mustskip = pos - guesspos
                    // Repeat skipping until proper amount reached 
                    while (skipped < mustskip) skipped += `is`.skip(mustskip - skipped)
                    return `is`
                }
            } catch (e: Exception) {
                // We couldn't skip cleanly. Swallow up and try normally.
            }
        }


        // Cast succeeded
        if (`is` is FileInputStream) {
            return try {
                `is`.channel.position(pos)
                `is`
            } catch (e: IOException) {
                // Ouch. Do a dumb close & reopening.
                `is`.close()
                `is` = InputStreamSugar.createInputStreamFromURI(URI, null, 1)
                `is`!!.skip(pos)
                `is`
            }
        }

        // Cast succeeded
        if (`is` is ZipInputStream) {
            // ZipInputStreams are VERY dumb. so...
            `is`.close()
            `is` = InputStreamSugar.createInputStreamFromURI(URI, entry, type)
            `is`!!.skip(pos)
            return `is`
        }
        try { // Is it a net resource? We have to reopen it :-/
            // long a=System.nanoTime();
            val u = URL(URI)
            val nis = u.openStream()
            nis.skip(pos)
            `is`.close()
            // long b=System.nanoTime();
            // System.out.printf("Network stream seeked WITH closing %d\n",(b-a)/1000);
            return nis
        } catch (e: Exception) {
        }

        // TODO: zip handling?
        return `is`
    }

    @Throws(IOException::class)
    fun getAllEntries(zis: ZipInputStream): List<ZipEntry> {
        val zes = ArrayList<ZipEntry>()
        var z: ZipEntry
        while (zis.nextEntry.also { z = it } != null) {
            zes.add(z)
        }
        return zes
    }

    /** Attempts to return a stream size estimate. Only guaranteed to work 100%
     * for streams representing local files, and zips (if you have the entry).
     *
     * @param is
     * @param z
     * @return
     */
    fun getSizeEstimate(`is`: InputStream, z: ZipEntry?): Long {
        if (`is` is FileInputStream) {
            try {
                return `is`.channel.size()
            } catch (e: IOException) {
            }
        }
        if (`is` is FileInputStream) {
            if (z != null) return z.size
        }

        // Last ditch
        return try {
            `is`.available().toLong()
        } catch (e: IOException) {
            try {
                `is`.available().toLong()
            } catch (e1: IOException) {
                -1
            }
        }
    }
}