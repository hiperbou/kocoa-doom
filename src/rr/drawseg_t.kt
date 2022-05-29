package rr

//
// ?
//
class drawseg_t {
    /** MAES: was pointer. Not array?  */
    var curline: seg_t? = null
    var x1 = 0
    var x2 = 0

    /** fixed_t  */
    var scale1 = 0
    var scale2 = 0
    var scalestep = 0

    /** 0=none, 1=bottom, 2=top, 3=both  */
    var silhouette = 0

    /** do not clip sprites above this (fixed_t)  */
    var bsilheight = 0

    /** do not clip sprites below this (fixed_t)  */
    var tsilheight = 0

    /** Indexes to lists for sprite clipping,
     * all three adjusted so [x1] is first value.  */
    private var psprtopclip = 0
    private var psprbottomclip = 0
    private var pmaskedtexturecol = 0

    /** Pointers to the actual lists   */
    private var sprtopclip: ShortArray? = null
    private var sprbottomclip: ShortArray? = null
    private var maskedtexturecol: ShortArray? = null

    ///////////////// Accessor methods to simulate mid-array pointers ///////////
    fun setSprTopClip(array: ShortArray?, index: Int) {
        sprtopclip = array
        psprtopclip = index
    }

    fun setSprBottomClip(array: ShortArray?, index: Int) {
        sprbottomclip = array
        psprbottomclip = index
    }

    fun setMaskedTextureCol(array: ShortArray?, index: Int) {
        maskedtexturecol = array
        pmaskedtexturecol = index
    }

    fun getSprTopClip(index: Int): Short {
        return sprtopclip!![psprtopclip + index]
    }

    fun getSprBottomClip(index: Int): Short {
        return sprbottomclip!![psprbottomclip + index]
    }

    fun getMaskedTextureCol(index: Int): Short {
        return maskedtexturecol!![pmaskedtexturecol + index]
    }

    fun getSprTopClipList(): ShortArray? {
        return sprtopclip
    }

    fun getSprBottomClipList(): ShortArray? {
        return sprbottomclip
    }

    fun getMaskedTextureColList(): ShortArray? {
        return maskedtexturecol
    }

    fun getSprTopClipPointer(): Int {
        return psprtopclip
    }

    fun getSprBottomClipPointer(): Int {
        return psprbottomclip
    }

    fun getMaskedTextureColPointer(): Int {
        return pmaskedtexturecol
    }

    fun setSprTopClipPointer(index: Int) {
        psprtopclip = index
    }

    fun setSprBottomClipPointer(index: Int) {
        psprbottomclip = index
    }

    fun setMaskedTextureColPointer(index: Int) {
        pmaskedtexturecol = index
    }

    fun nullSprTopClip(): Boolean {
        return sprtopclip == null
    }

    fun nullSprBottomClip(): Boolean {
        return sprbottomclip == null
    }

    fun nullMaskedTextureCol(): Boolean {
        return maskedtexturecol == null
    }
}