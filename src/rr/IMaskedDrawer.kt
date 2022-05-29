package rr

/** Draws any masked stuff -sprites, textures, or special 3D floors  */
interface IMaskedDrawer<T, V> : IDetailAware {
    /** Cache the sprite manager, if possible  */
    fun cacheSpriteManager(SM: ISpriteManager)
    fun DrawMasked()
    fun setPspriteIscale(i: Int)
    fun setPspriteScale(i: Int)

    /**
     * For serial masked drawer, just complete the column function. For
     * parallel version, store rendering instructions and execute later on.
     * HINT: you need to discern between masked and non-masked draws.
     */
    fun completeColumn()

    companion object {
        const val BASEYCENTER = 100
    }
}