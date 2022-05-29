package defines


import doom.CommandVariable

/**
 * Game mode handling - identify IWAD version to handle IWAD dependend animations etc.
 */
enum class GameMode(// Well, no IWAD found.  
    val devDir: String, val version: DoomVersion, val devVar: CommandVariable
) {
    shareware("data_se", DoomVersion.DOOM1_WAD, CommandVariable.SHDEV),  // DOOM 1 shareware, E1, M9
    registered("data_se", DoomVersion.DOOM_WAD, CommandVariable.REGDEV),  // DOOM 1 registered, E3, M27
    commercial("cdata", DoomVersion.DOOM2_WAD, CommandVariable.COMDEV),  // DOOM 2 retail, E1 M34

    // DOOM 2 german edition not handled
    retail("data_se", DoomVersion.DOOMU_WAD, CommandVariable.REGDEV),  // DOOM 1 retail, E4, M36
    pack_tnt("cdata", DoomVersion.TNT_WAD, CommandVariable.COMDEV),  // TNT mission pack
    pack_plut("cdata", DoomVersion.PLUTONIA_WAD, CommandVariable.COMDEV),  // Plutonia pack
    pack_xbla(
        "cdata",
        DoomVersion.XBLA_WAD,
        CommandVariable.COMDEV
    ),  // XBLA Doom. How you got hold of it, I don't care :-p
    freedm("cdata", DoomVersion.FREEDM_WAD, CommandVariable.FRDMDEV),  // FreeDM
    freedoom1("data_se", DoomVersion.FREEDOOM1_WAD, CommandVariable.FR1DEV),  // Freedoom phase 1 
    freedoom2("cdata", DoomVersion.FREEDOOM2_WAD, CommandVariable.FR2DEV),  // Freedoom phase 2
    indetermined("data_se", DoomVersion.DOOM1_WAD, CommandVariable.SHDEV);

    fun hasTexture2(): Boolean {
        return (this == GameMode.shareware || this == GameMode.freedoom2 || this != GameMode.commercial)
    }

    companion object {
        fun forVersion(v: DoomVersion?): GameMode? {
            when (v) {
                DoomVersion.DOOM1_WAD -> return shareware
                DoomVersion.DOOM2F_WAD, DoomVersion.DOOM2_WAD -> return commercial
                DoomVersion.DOOMU_WAD, DoomVersion.UDOOM_WAD -> return retail
                DoomVersion.DOOM_WAD -> return registered
                DoomVersion.FREEDM_WAD -> return freedm
                DoomVersion.FREEDOOM1_WAD -> return freedoom1
                DoomVersion.FREEDOOM2_WAD -> return freedoom2
                DoomVersion.PLUTONIA_WAD -> return pack_plut
                DoomVersion.TNT_WAD -> return pack_tnt
                DoomVersion.XBLA_WAD -> return pack_xbla
            }
            return null
        }
    }
}