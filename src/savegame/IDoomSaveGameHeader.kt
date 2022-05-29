package savegame


import defines.skill_t

/** A Save Game Header should be able to be loaded quickly and return
 * some basic info about it (name, version, game time, etc.) in an unified
 * manner, no matter what actual format you use for saving.
 *
 * @author admin
 */
interface IDoomSaveGameHeader {
    fun getName(): String?
    fun setName(name: String?)
    fun getGameskill(): skill_t?
    fun setGameskill(gameskill: skill_t?)
    fun getVersion(): String?
    fun setVersion(vcheck: String?)
    fun getGameepisode(): Int
    fun setGameepisode(gameepisode: Int)
    fun isProperend(): Boolean
    fun setWrongversion(wrongversion: Boolean)
    fun isWrongversion(): Boolean
    fun setLeveltime(leveltime: Int)
    fun getLeveltime(): Int
    fun setPlayeringame(playeringame: BooleanArray)
    fun getPlayeringame(): BooleanArray
    fun setGamemap(gamemap: Int)
    fun getGamemap(): Int
}