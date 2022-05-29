package hu


import doom.SourceCode.HU_Stuff
import doom.event_t
import rr.patch_t

interface IHeadsUp {
    fun Ticker()
    fun Erase()
    fun Drawer()

    @HU_Stuff.C(HU_Stuff.HU_Responder)
    fun Responder(ev: event_t): Boolean
    fun getHUFonts(): Array<patch_t>
    fun dequeueChatChar(): Char
    fun Init()
    fun setChatMacro(i: Int, s: String)
    fun Start()
    fun Stop()
}