package m


import data.mobjtype_t
import p.ActiveStates

interface IRandom {
    fun P_Random(): Int
    fun M_Random(): Int
    fun ClearRandom()
    val index: Int
    fun P_Random(caller: Int): Int
    fun P_Random(message: String?): Int
    fun P_Random(caller: ActiveStates?, sequence: Int): Int
    fun P_Random(caller: ActiveStates?, type: mobjtype_t?, sequence: Int): Int
}