package data

/**
 * SoundFX struct.
 *
 *
 *
 */
open class sfxinfo_t {
    constructor() {}

    /** up to 6-character name  */
    var name: String? = null

    /** Sfx singularity (only one at a time)  */
    var singularity = false

    /** Sfx priority  */
    var priority = 0

    // referenced sound if a link
    // MAES: since in pure hackish C style, a "0" value would be used as a boolean, we'll need to distinguish more
    // unambiguously. So for querying, look at the "linked" boolean or a getter.
    var linked = false
    var _link: sfxinfo_t? = null
    fun getLink(): sfxinfo_t? {
        return if (linked) _link else null
    }

    fun setLink(link: sfxinfo_t?) {
        this._link = link
    }

    // pitch if a link
    var pitch = 0

    // volume if a link
    var volume = 0

    /** sound data (used to be void*)  */
    lateinit var data: ByteArray

    // this is checked every second to see if sound
    // can be thrown out (if 0, then decrement, if -1,
    // then throw out, if > 0, then it is in use)
    var usefulness = 0

    // lump number of sfx
    var lumpnum = 0

    constructor(
        name: String?, singularity: Boolean, priority: Int,
        link: sfxinfo_t?, pitch: Int, volume: Int, data: ByteArray,
        usefulness: Int, lumpnum: Int
    ) {
        this.name = name
        this.singularity = singularity
        this.priority = priority
        this._link = link
        this.pitch = pitch
        this.volume = volume
        this.data = data
        this.usefulness = usefulness
        this.lumpnum = lumpnum
    }

    /** MAES: Call this constructor if you don't want a cross-linked sound.
     *
     * @param name
     * @param singularity
     * @param priority
     * @param pitch
     * @param volume
     * @param usefulness
     */
    constructor(
        name: String?, singularity: Boolean, priority: Int,
        pitch: Int, volume: Int, usefulness: Int
    ) {
        this.name = name
        this.singularity = singularity
        this.priority = priority
        linked = false
        this.pitch = pitch
        this.volume = volume
        this.usefulness = usefulness
    }

    constructor(
        name: String?, singularity: Boolean, priority: Int, linked: Boolean,
        pitch: Int, volume: Int, usefulness: Int
    ) {
        this.name = name
        this.singularity = singularity
        this.priority = priority
        this.linked = linked
        this.pitch = pitch
        this.volume = volume
        this.usefulness = usefulness
    }

    fun identify(array: Array<sfxinfo_t>): Int {
        for (i in array.indices) {
            if (array[i] === this) {
                return i
            }
        }
        // Duh
        return 0
    }
}