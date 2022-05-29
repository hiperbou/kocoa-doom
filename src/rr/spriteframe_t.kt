package rr

/** Sprites are patches with a special naming convention
 * so they can be recognized by R_InitSprites.
 * The base name is NNNNFx or NNNNFxFx, with
 * x indicating the rotation, x = 0, 1-7.
 * The sprite and frame specified by a thing_t
 * is range checked at run time.
 * A sprite is a patch_t that is assumed to represent
 * a three dimensional object and may have multiple
 * rotations pre drawn.
 * Horizontal flipping is used to save space,
 * thus NNNNF2F5 defines a mirrored patch.
 * Some sprites will only have one picture used
 * for all views: NNNNF0
 */
class spriteframe_t : Cloneable {
    /** If false use 0 for any position.
     * Note: as eight entries are available,
     * we might as well insert the same name eight times.
     *
     * FIXME: this is used as a tri-state.
     * 0= false
     * 1= true
     * -1= cleared/indeterminate, which should not evaluate to either true or false.
     */
    var rotate = 0

    /** Lump to use for view angles 0-7.  */
    var lump: IntArray

    /** Flip bit (1 = flip) to use for view angles 0-7.  */
    var flip: ByteArray

    init {
        lump = IntArray(8)
        flip = ByteArray(8)
    }

    public override fun clone(): spriteframe_t {
        val response = spriteframe_t()
        response.rotate = rotate
        System.arraycopy(lump, 0, response.lump, 0, lump.size)
        System.arraycopy(flip, 0, response.flip, 0, flip.size)
        return response
    }
}