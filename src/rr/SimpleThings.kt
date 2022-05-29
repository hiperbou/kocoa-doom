package rr

import v.scale.VideoScale

/**
 * A very "simple" things class which just does serial rendering and uses all
 * the base methods from AbstractThings.
 *
 * @author velktron
 * @param <T>
 * @param <V>
</V></T> */
class SimpleThings<T, V>(vs: VideoScale, R: SceneRenderer<T, V>) : AbstractThings<T, V>(vs, R) {
    override fun completeColumn() {
        colfunc!!.invoke()
    }
}