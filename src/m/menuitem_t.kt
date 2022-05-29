package m


import g.Signals.ScanCode

class menuitem_t {
    constructor(status: Int, name: String, routine: MenuRoutine, alphaKey: ScanCode?) {
        this.status = status
        this.name = name
        this.routine = routine
        this.alphaKey = alphaKey
    }

    constructor(status: Int, name: String, routine: MenuRoutine?) {
        this.status = status
        this.name = name
        this.routine = routine
    }

    /**
     * 0 = no cursor here, 1 = ok, 2 = arrows ok
     */
    var status: Int
    var name: String

    // choice = menu item #.
    // if status = 2,
    //   choice=0:leftarrow,1:rightarrow
    // MAES: OK... to probably we need some sort of "MenuRoutine" class for this one.
    // void	(*routine)(int choice);
    var routine: MenuRoutine? = null

    /**
     * hotkey in menu
     */
    var alphaKey: ScanCode? = null
}