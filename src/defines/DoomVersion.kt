/*
 * Copyright (C) 2017 Good Sign
 * Copyright (C) 2022 hiperbou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package defines

import doom.DoomMain
import mochadoom.Loggers
import utils.C2JUtils
import java.util.*
import java.util.logging.Level

enum class DoomVersion(val wadFileName: String) {
    DOOM2F_WAD("doom2f.wad"), DOOM2_WAD("doom2.wad"), PLUTONIA_WAD("plutonia.wad"), TNT_WAD("tnt.wad"), XBLA_WAD("xbla.wad"), DOOMU_WAD(
        "doomu.wad"
    ),
    UDOOM_WAD("udoom.wad"), DOOM_WAD("doom.wad"), DOOM1_WAD("doom1.wad"), FREEDM_WAD("freedm.wad"), FREEDOOM1_WAD("freedoom1.wad"), FREEDOOM2_WAD(
        "freedoom2.wad"
    );

    companion object {
        /**
         * Try all versions in given doomwaddir
         *
         * @return full path to the wad of success
         */
        fun tryAllWads(DOOM: DoomMain<*, *>, doomwaddir: String): String? {
            for (v in DoomVersion.values()) {
                val vFullPath = doomwaddir + '/' + v.wadFileName
                if (C2JUtils.testReadAccess(vFullPath)) {
                    DOOM.setGameMode(GameMode.forVersion(v))
                    if (v == DoomVersion.DOOM2F_WAD) {
                        // C'est ridicule!
                        // Let's handle languages in config files, okay?
                        DOOM.language = Language_t.french
                        println("French version\n")
                    }
                    return vFullPath
                }
            }
            return null
        }

        /**
         * Try only one IWAD.
         *
         * @param iwad
         * @param doomwaddir
         * @return game mode
         */
        fun tryOnlyOne(iwad: String, doomwaddir: String): GameMode? {
            try {
                // Is it a known and valid version?
                val v = DoomVersion.valueOf(iwad.trim { it <= ' ' }.uppercase(Locale.getDefault()).replace('.', '_'))
                val tmp: GameMode = GameMode.forVersion(v)!!

                // Can we read it?
                if (tmp != null && C2JUtils.testReadAccess(doomwaddir + iwad)) {
                    return tmp // Yes, so communicate the gamemode back.
                }
            } catch (ex: IllegalArgumentException) {
                Loggers.getLogger(DoomVersion::class.java.name).log(Level.WARNING, iwad, ex)
            }

            // It's either invalid or we can't read it.
            // Fuck that.
            return null
        }
    }
}