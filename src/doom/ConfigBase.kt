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
package doom

import data.dstrings
import m.Settings
import mochadoom.Engine
import utils.OSValidator
import utils.ResourceIO
import java.util.*

/**
 * Manages loading different config files from different places
 * (the part about different places is still unfinished)
 *
 * @author Good Sign
 */
enum class ConfigBase(val defaultConfigName: String, val env: String) {
    WINDOWS("default.cfg", "USERPROFILE"), UNIX(".doomrc", "HOME");

    class Files @JvmOverloads constructor(
        val fileName: String, val comparator: Comparator<Settings> = Comparator.comparing(
            { obj: Settings -> obj.name }) { obj: String, anotherString: String? ->
            obj.compareTo(
                anotherString!!
            )
        }
    ) {
        var changed = true
        private var paths: Array<String>? = null
        fun firstValidPathIO(): Optional<ResourceIO> {
            return Arrays.stream(getPaths())
                .map { path: String? -> ResourceIO(path) }
                .filter { obj: ResourceIO -> obj.exists() }
                .findFirst()
        }

        fun workDirIO(): ResourceIO {
            return ResourceIO(ConfigBase.Files.getFolder() + fileName)
        }

        /**
         * Get file / paths combinations
         *
         * @return a one or more path to the file
         */
        private fun getPaths(): Array<String> {
            if (paths != null) {
                return paths!!
            }
            var getPath: String? = null
            try { // get it if have rights to do, otherwise ignore and use only current folder
                getPath = System.getenv(ConfigBase.CURRENT.env)
            } catch (ex: SecurityException) {
            }
            if (getPath == null || "" == getPath) {
                return arrayOf(ConfigBase.Files.folder!!)
            }
            getPath += System.getProperty("file.separator")
            return arrayOf<String>(
                /**
                 * Uncomment the next line and it will load default.cfg and mochadoom.cfg from user home dir
                 * I find it undesirable - it can load some unrelated file and even write it at exit
                 * - Good Sign 2017/04/19
                 */
                //getPath + folder + fileName,
                ConfigBase.Files.getFolder() + fileName
            ).also { paths = it }
        }

        companion object {
            private var folder: String? = null
            private fun getFolder(): String {
                return if (ConfigBase.Files.folder != null) ConfigBase.Files.folder!! else if (Engine.getCVM()
                        .bool(CommandVariable.SHDEV) ||
                    Engine.getCVM().bool(CommandVariable.REGDEV) ||
                    Engine.getCVM().bool(CommandVariable.FR1DEV) ||
                    Engine.getCVM().bool(CommandVariable.FRDMDEV) ||
                    Engine.getCVM().bool(CommandVariable.FR2DEV) ||
                    Engine.getCVM().bool(CommandVariable.COMDEV)
                ) dstrings.DEVDATA + System.getProperty("file.separator") else "".also {
                    ConfigBase.Files.folder = it
                }
            }
        }
    }

    companion object {
        /**
         * Early detection of the system and setting this is important to define global config Files
         */
        val CURRENT = if (OSValidator.isMac() || OSValidator.isUnix()) ConfigBase.UNIX else ConfigBase.WINDOWS

        /**
         * Reference these in Settings.java to set which file they will go on by default
         */
        val FILE_DOOM = ConfigBase.Files(ConfigBase.CURRENT.defaultConfigName) { obj: Settings, o: Settings ->
            obj.compareTo(o)
        }
        val FILE_MOCHADOOM = ConfigBase.Files("mochadoom.cfg")

        /**
         * To be able to look for config in several places
         * Still unfinished
         */
        fun getFiles(): List<ConfigBase.Files> {
            val ret: MutableList<ConfigBase.Files> = ArrayList()
            /**
             * If user supplied -config argument, it will only use the values from these files instead of defaults
             */
            if (!Engine.getCVM()
                    .with<Array<String>>(CommandVariable.CONFIG, 0) { fileNames ->
                        Arrays.stream(fileNames).map { fileName -> Files(fileName) }
                            .forEach { e -> ret.add(e) }
                    }
            /**
             * If there is no such argument, load default.cfg (or .doomrc) and mochadoom.cfg
             */
            ) {
                ret.add(ConfigBase.FILE_DOOM)
                ret.add(ConfigBase.FILE_MOCHADOOM)
            }
            return ret
        }
    }
}