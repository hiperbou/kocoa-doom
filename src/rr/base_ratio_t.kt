package rr


class base_ratio_t(// Base width (unused)
    var base_width: Int, // Base height (used for wall visibility multiplier)
    var base_height: Int, // Psprite offset (needed for "tallscreen" modes)
    var psprite_offset: Int,
    // Width or height multiplier
    var multiplier: Int, gl_ratio: Float
) {
    var gl_ratio: Float

    init {
        this.gl_ratio = (base_ratio_t.RMUL * gl_ratio).toFloat()
    }

    companion object {
        const val RMUL = 1.6 / 1.333333
    }
}