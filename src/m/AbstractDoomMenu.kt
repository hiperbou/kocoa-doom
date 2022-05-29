package m


import doom.DoomMain

abstract class AbstractDoomMenu<T, V>(  ////////////////////// CONTEXT ///////////////////
    val DOOM: DoomMain<T, V>
) : IDoomMenu