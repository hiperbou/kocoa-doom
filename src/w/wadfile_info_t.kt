package w

import java.io.InputStream
import java.util.zip.ZipEntry

// CPhipps - changed wad init
// We _must_ have the wadfiles[] the same as those actually loaded, so there 
// is no point having these separate entities. This belongs here.
class wadfile_info_t {
    var name // Also used as a resource identifier, so save with full path and all.
            : String? = null
    var entry // Secondary resource identifier e.g. files inside zip archives.
            : ZipEntry? = null
    var type // as per InputStreamSugar
            = 0
    var src: wad_source_t? = null
    var handle: InputStream? = null
    var cached // Whether we use local caching e.g. for URL or zips
            = false
    var maxsize: Long = -1 // Update when known for sure. Will speed up seeking.
}