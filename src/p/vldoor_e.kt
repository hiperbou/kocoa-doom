package p


//
// P_DOORS
//
enum class vldoor_e {
    normal, close30ThenOpen, close, open, raiseIn5Mins, blazeRaise, blazeOpen, blazeClose;

    companion object {
        val VALUES = vldoor_e.values().size
    }
}