package rr

/**
 * A sprite definition:
 * a number of animation frames.
 */
class spritedef_t {
    /** the very least, primitive fields won't bomb,
     * and copy constructors can do their job.
     */
    constructor() {}
    constructor(numframes: Int) {
        this.numframes = numframes
        spriteframes = arrayOfNulls(numframes)
    }

    constructor(frames: Array<spriteframe_t>) {
        numframes = frames.size
        spriteframes = arrayOfNulls(numframes)
        // copy shit over...
        for (i in 0 until numframes) {
            spriteframes[i] = frames[i].clone()
        }
    }

    /** Use this constructor, as we usually need less than 30 frames
     * It will actually clone the frames.
     */
    fun copy(from: Array<spriteframe_t>, maxframes: Int) {
        numframes = maxframes
        spriteframes = arrayOfNulls(maxframes)
        // copy shit over...
        for (i in 0 until maxframes) {
            spriteframes[i] = from[i].clone()
        }
    }

    var numframes = 0
    lateinit var spriteframes: Array<spriteframe_t?>
}