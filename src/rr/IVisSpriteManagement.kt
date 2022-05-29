package rr

/** A sprite manager does everything but drawing the sprites. It creates lists
 * of sprites-per-sector, sorts them, and stuff like that.
 * that gory visibiliy
 *
 * @author velkton
 *
 * @param <V>
</V> */
interface IVisSpriteManagement<V> : ILimitResettable {
    fun AddSprites(sec: sector_t)

    /** Cache the sprite manager, if possible  */
    fun cacheSpriteManager(SM: ISpriteManager)
    fun SortVisSprites()
    fun getNumVisSprites(): Int
    fun getVisSprites(): Array<vissprite_t<V?>>
    fun ClearSprites()
}