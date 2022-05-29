package timing


import doom.CVarManager
import doom.CommandVariable
import doom.SourceCode.I_IBM

interface ITicker {
    @I_IBM.C(I_IBM.I_GetTime)
    fun GetTime(): Int

    companion object {
        fun createTicker(CVM: CVarManager): ITicker {
            return if (CVM.bool(CommandVariable.MILLIS)) {
                MilliTicker()
            } else if (CVM.bool(CommandVariable.FASTTIC) || CVM.bool(CommandVariable.FASTDEMO)) {
                DelegateTicker()
            } else {
                NanoTicker()
            }
        }
    }
}