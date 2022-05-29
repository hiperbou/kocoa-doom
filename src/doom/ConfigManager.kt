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


import m.Settings
import utils.ParseString
import utils.QuoteType
import utils.ResourceIO
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.regex.Pattern

/**
 * Loads and saves game cfg files
 *
 * @author Good Sign
 */


class ConfigManager {
    private val configFiles: List<ConfigBase.Files> = ConfigBase.getFiles()
    private val configMap = EnumMap<Settings, Any>(Settings::class.java)

    enum class UpdateStatus {
        UNCHANGED, UPDATED, INVALID
    }

    init {
        LoadDefaults()
    }

    inline fun <reified T>tryChangeSetting(setting: Settings, value: String, configMap:EnumMap<Settings, Any>): UpdateStatus {
        return try {
            val parse = ParseString.parseString(value) as T
            setting.hasChange(configMap.put(setting, parse) != parse)
        } catch (e:Exception) {
            UpdateStatus.INVALID
        }
    }
    fun update(setting: Settings, value: String): UpdateStatus {
        return when {
            setting.valueType.superclass == Enum::class.java -> {
                // Enum search by name
                val enumerated: Any = java.lang.Enum.valueOf(setting.valueType as Class<out Enum<*>?>, value)!!
                setting.hasChange(configMap.put(setting, enumerated) != enumerated)
            }
            setting.valueType == String::class.java -> { setting.hasChange(configMap.put(setting, value) != value) }
            setting.valueType == Char::class.java -> { tryChangeSetting<Char>(setting, value, configMap) }
            setting.valueType == Long::class.java -> { tryChangeSetting<Long>(setting, value, configMap) }
            setting.valueType == Int::class.java ->  { tryChangeSetting<Int>(setting, value, configMap) }
            setting.valueType == Boolean::class.java ->  { tryChangeSetting<Boolean>(setting, value, configMap) }
            else -> UpdateStatus.INVALID
        }
    }

    fun update(setting: Settings, value: Any): UpdateStatus {
        return if (setting.valueType == String::class.java) {
            setting.hasChange(configMap.put(setting, value.toString()) != value.toString())
        } else UpdateStatus.INVALID
    }

    fun update(setting: Settings, value: Int): UpdateStatus {
        if (setting.valueType == Int::class.java) {
            return setting.hasChange(configMap.put(setting, value) != value)
        } else if (setting.valueType == String::class.java) {
            val valStr = Integer.toString(value)
            return setting.hasChange(configMap.put(setting, valStr) != valStr)
        } else if (setting.valueType.superclass == Enum::class.java) {
            val enumValues = setting.valueType.getEnumConstants()!! //TODO: this could be null
            if (value >= 0 && value < enumValues.size) {
                return setting.hasChange(configMap.put(setting, enumValues[value]) != enumValues[value])
            }
        }
        return UpdateStatus.INVALID
    }

    fun update(setting: Settings, value: Long): UpdateStatus {
        if (setting.valueType == Long::class.java) {
            return setting.hasChange(configMap.put(setting, value) != value)
        } else if (setting.valueType == String::class.java) {
            val valStr = java.lang.Long.toString(value)
            return setting.hasChange(configMap.put(setting, valStr) != valStr)
        }
        return UpdateStatus.INVALID
    }

    fun update(setting: Settings, value: Double): UpdateStatus {
        if (setting.valueType == Double::class.java) {
            return setting.hasChange(configMap.put(setting, value) != value)
        } else if (setting.valueType == String::class.java) {
            val valStr = java.lang.Double.toString(value)
            return setting.hasChange(configMap.put(setting, valStr) != valStr)
        }
        return UpdateStatus.INVALID
    }

    fun update(setting: Settings, value: Char): UpdateStatus {
        if (setting.valueType == Char::class.java) {
            return setting.hasChange(configMap.put(setting, value) != value)
        } else if (setting.valueType == String::class.java) {
            val valStr = Character.toString(value)
            return setting.hasChange(configMap.put(setting, valStr) != valStr)
        }
        return UpdateStatus.INVALID
    }

    fun update(setting: Settings, value: Boolean): UpdateStatus {
        if (setting.valueType == Boolean::class.java) {
            return setting.hasChange(configMap.put(setting, value) != value)
        } else if (setting.valueType == String::class.java) {
            val valStr = java.lang.Boolean.toString(value)
            return setting.hasChange(configMap.put(setting, valStr) != valStr)
        }
        return UpdateStatus.INVALID
    }

    private fun export(setting: Settings): String {
        return setting.quoteType().map { qt: QuoteType ->
            StringBuilder()
                .append(setting.name)
                .append("\t\t")
                .append(qt.quoteChar)
                .append(configMap[setting])
                .append(qt.quoteChar)
                .toString()
        }.orElseGet {
            StringBuilder()
                .append(setting.name)
                .append("\t\t")
                .append(configMap[setting])
                .toString()
        }
    }

    fun equals(setting: Settings, obj: Any): Boolean {
        return obj == configMap[setting]
    }

    fun <T> getValue(setting: Settings, valueType: Class<T>): T {
        if (setting.valueType == valueType) {
            return configMap[setting] as T
        } else if (valueType == String::class.java) {
            return configMap[setting].toString() as T
        } else if (setting.valueType == String::class.java) {
            if (valueType == Char::class.java || valueType == Long::class.java || valueType == Int::class.java || valueType == Boolean::class.java) {
                val parse: Any = ParseString.parseString(configMap[setting].toString())
                if (valueType.isInstance(parse)) {
                    return parse as T
                }
            }
        } else if (valueType == Int::class.java && setting.valueType.superclass == Enum::class.java) {
            return (configMap[setting] as Enum<*>?)!!.ordinal as T
        }
        throw IllegalArgumentException("Unsupported cast: " + setting.valueType + " to " + valueType)
    }

    fun SaveDefaults() {
        Settings.SETTINGS_MAP.forEach { file: ConfigBase.Files, settings: EnumSet<Settings> ->
            // do not write unless there is changes
            if (!file.changed) {
                return@forEach //TODO check return was @forEach?
            }

            // choose existing config file or create one in current working directory
            val rio = file.firstValidPathIO().orElseGet { file.workDirIO() }
            val it = settings.stream().sorted(file.comparator).iterator()
            if (rio.writeLines({
                    if (it.hasNext()) {
                        return@writeLines export(it.next())
                    }
                    null
                }, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                // we wrote successfully - so it will not try to write it again, unless something really change
                file.changed = false
            }
        }
    }

    /**
     * Handles variables and settings from default.cfg and other config files
     * They can be load even earlier then other systems
     */
    private fun LoadDefaults() {
        Arrays.stream(Settings.values())
            .forEach { setting: Settings -> configMap[setting] = setting.defaultValue }
        print("M_LoadDefaults: Load system defaults.\n")
        configFiles.forEach { file: ConfigBase.Files ->
            val maybeRIO = file.firstValidPathIO()
            /**
             * Each file successfully read marked as not changed, and as changed - those who don't exist
             *
             */
            /**
             * Each file successfully read marked as not changed, and as changed - those who don't exist
             *
             */
            file.changed = !(maybeRIO.isPresent && readFoundConfig(file, maybeRIO.get()))
        }

        // create files who don't exist (it will skip those with changed = false - all who exists)
        SaveDefaults()
    }

    private fun readFoundConfig(file: ConfigBase.Files, rio: ResourceIO): Boolean {
        print(String.format("M_LoadDefaults: Using config %s.\n", rio.getFilename()))
        if (rio.readLines { line: String? ->
                val split: Array<String> = SPLITTER.split(line, 2)
                if (split.size < 2) {
                    return@readLines
                }
                val name = split[0]
                try {
                    val setting = Settings.valueOf(name)
                    val value = setting.quoteType()
                        .filter { qt -> qt == QuoteType.DOUBLE }
                        .map { qt -> qt.unQuote(split[1]) }
                        .orElse(split[1])!!
                    if (update(setting, value) == UpdateStatus.INVALID) {
                        System.err.printf("WARNING: invalid config value for: %s in %s \n", name, rio.getFilename())
                    } else {
                        setting.rebase(file)
                    }
                } catch (ex: IllegalArgumentException) {
                }
            }) {
            return true // successfully read a file
        }

        // Something went bad, but this won't destroy successfully read values, though.
        System.err.printf("Can't read the settings file %s\n", rio.getFilename())
        return false
    }

    companion object {
        private val SPLITTER = Pattern.compile("[ \t\n\r\u000c]+")
    }
}