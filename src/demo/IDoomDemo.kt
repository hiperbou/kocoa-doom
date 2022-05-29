package demo


import defines.skill_t
import w.IWritableDoomObject

interface IDoomDemo : IWritableDoomObject {
    /** Get next demo command, in its raw format. Use
     * its own adapters if you need it converted to a
     * standard ticcmd_t.
     *
     * @return
     */
    fun getNextTic(): IDemoTicCmd?

    /** Record a demo command in the IDoomDemo's native format.
     * Use the IDemoTicCmd's objects adaptors to convert it.
     *
     * @param tic
     */
    fun putTic(tic: IDemoTicCmd)
    fun getVersion(): Int
    fun setVersion(version: Int)
    fun getSkill(): skill_t?
    fun setSkill(skill: skill_t?)
    fun getEpisode(): Int
    fun setEpisode(episode: Int)
    fun getMap(): Int
    fun setMap(map: Int)
    fun isDeathmatch(): Boolean
    fun setDeathmatch(deathmatch: Boolean)
    fun isRespawnparm(): Boolean
    fun setRespawnparm(respawnparm: Boolean)
    fun isFastparm(): Boolean
    fun setFastparm(fastparm: Boolean)
    fun isNomonsters(): Boolean
    fun setNomonsters(nomonsters: Boolean)
    fun getConsoleplayer(): Int
    fun setConsoleplayer(consoleplayer: Int)
    fun getPlayeringame(): BooleanArray
    fun setPlayeringame(playeringame: BooleanArray)
    fun resetDemo()

    companion object {
        /** Vanilla end demo marker, to append at the end of recorded demos  */
        const val DEMOMARKER = 0x80
    }
}