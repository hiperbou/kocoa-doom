package p


import doom.SourceCode.P_Tick
import doom.thinker_t

interface ThinkerList {
    @P_Tick.C(P_Tick.P_AddThinker)
    fun AddThinker(thinker: thinker_t)

    @P_Tick.C(P_Tick.P_RemoveThinker)
    fun RemoveThinker(thinker: thinker_t)

    @P_Tick.C(P_Tick.P_InitThinkers)
    fun InitThinkers()
    fun getRandomThinker(): thinker_t
    fun getThinkerCap(): thinker_t
}