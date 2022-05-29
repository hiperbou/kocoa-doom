package doom


import defines.skill_t

/** Groups functions formerly in d_game,
 * in case you want to provide a different implementation
 */
interface IDoomGame {
    fun ExitLevel()
    fun WorldDone()
    fun CheckDemoStatus(): Boolean

    /** Can be called by the startup code or M_Responder.
     * A normal game starts at map 1,
     * but a warp test can start elsewhere  */
    fun DeferedInitNew(skill: skill_t?, episode: Int, map: Int)

    /** Can be called by the startup code or M_Responder,
     * calls P_SetupLevel or W_EnterWorld.  */
    fun LoadGame(name: String?)

    /** Called by M_Responder.  */
    fun SaveGame(slot: Int, description: String)

    /** Takes a screenshot *NOW*
     *
     */
    fun ScreenShot()
    fun StartTitle()
    fun getGameAction(): gameaction_t
    fun setGameAction(ga: gameaction_t)

    // public void PlayerReborn(int player);
    fun DeathMatchSpawnPlayer(playernum: Int)
}