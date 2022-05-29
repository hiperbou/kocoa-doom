package w

import java.io.InputStream

/*
typedef struct
{
  // WARNING: order of some fields important (see info.c).

  char  name[9];
  int   size;

    // killough 4/17/98: namespace tags, to prevent conflicts between resources
  enum {
    ns_global=0,
    ns_sprites,
    ns_flats,
    ns_colormaps,
    ns_prboom,
    ns_demos,
    ns_hires //e6y
  } li_namespace; // haleyjd 05/21/02: renamed from "namespace"

  wadfile_info_t *wadfile;
  int position;
  wad_source_t source;
  int flags; //e6y
} lumpinfo_t; */
class lumpinfo_t : Cloneable {
    var name: String? = null
    var handle: InputStream? = null
    var position: Long = 0
    var size: Long = 0

    // A 32-bit hash which should be enough for searching through hashtables.
    var hash = 0

    // A 64-bit hash that just maps an 8-char string to a long num, good for hashing
    // or for direct comparisons.
    //public long stringhash;
    // Intepreting the first 32 bits of their name as an int. Used in initsprites.
    var intname = 0

    // public int next;
    //public int index;
    // For BOOM compatibility
    var namespace: li_namespace? = null
    var wadfile: wadfile_info_t? = null
    override fun hashCode(): Int {
        return hash
    }

    override fun toString(): String {
        return name + " " + Integer.toHexString(hash)
    }

    public override fun clone(): lumpinfo_t {
        val tmp = lumpinfo_t()
        tmp.name = name // Well... a reference will do.
        tmp.handle = handle
        tmp.position = position
        tmp.size = size
        tmp.hash = hash
        tmp.intname = intname
        tmp.namespace = namespace
        tmp.wadfile = wadfile
        return tmp
    }
}