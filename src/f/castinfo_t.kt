package f


import data.mobjtype_t

/**
 * Final DOOM 2 animation Casting by id Software. in order of appearance
 */
class castinfo_t {
    var name: String? = null
    var type: mobjtype_t? = null

    constructor() {}
    constructor(name: String?, type: mobjtype_t?) {
        this.name = name
        this.type = type
    }
}