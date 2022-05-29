package data


class musicinfo_t {
    constructor() {}
    constructor(name: String?) {
        this.name = name
    }

    constructor(name: String?, lumpnum: Int) {
        this.name = name
        this.lumpnum = lumpnum
    }

    // up to 6-character name
    var name: String? = null

    // lump number of music
    var lumpnum = 0

    // music data
    var data: ByteArray? = ByteArray(0)

    // music handle once registered
    var handle = 0
}