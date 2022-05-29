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


import doom.CommandVariable
import mochadoom.Loggers
import utils.ResourceIO
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.function.*
import java.util.logging.Level

/**
 * New, object-oriented Console Variable Manager
 * Usage:
 * 1. Define CVars in CommandVariable Enum
 * 2. In program entry main function, create any ICommandLineManager and pass an instance to create CVarManager
 * 3. Use methods bool, present, get and with to check or get CVars
 *
 * @author Good Sign
 */
class CVarManager(commandList: List<String>) {
    private val cVarMap = EnumMap<CommandVariable, Array<Any?>?>(
        CommandVariable::class.java
    )

    init {
        println(processAllArgs(commandList).toString() + " command-line variables")
    }

    /**
     * Checks that CVar of switch-type is passed as Command Line Argument
     * @param cv
     * @return boolean
     */
    fun bool(cv: CommandVariable): Boolean {
        return cv.getType() == CommandVariable.Type.SWITCH && cVarMap.containsKey(cv)
    }

    /**
     * Checks that CVar of any type is passed as Command Line Argument with proper value(s)
     * @param cv
     * @return boolean
     */
    fun present(cv: CommandVariable): Boolean {
        return cVarMap[cv] != null
    }

    /**
     * Checks that CVar of any type is passed as Command Line Argument
     * @param cv
     * @return boolean
     */
    fun specified(cv: CommandVariable): Boolean {
        return cVarMap.containsKey(cv)
    }

    /**
     * Gets an Optional with or without a value of CVar argument at position
     * @param cv
     * @return Optional
     */
    operator fun <T> get(cv: CommandVariable, itemType: Class<T>, position: Int): Optional<T> {
        if (cv.arguments[position] == itemType) {
            if (!cVarMap.containsKey(cv)) {
                return Optional.empty()
            }
            val ret = cVarMap[cv]!![position] as T?
            return Optional.ofNullable(ret)
        }
        throw IllegalArgumentException("CVar argument at position " + position + " is not of class " + itemType.name)
    }

    /**
     * Tries to apply a CVar argument at position to the consuming function
     * The magic is that you declare a lambda function or reference some method
     * and the type of object will be automatically picked from what you hinted
     *
     * i.e. (String s) -> System.out.println(s) will try to get string,
     * (Object o) -> map.put(key, o) or o -> list.add(o.hashCode()) will try to get objects
     * and you dont have to specify class
     *
     * The drawback is the ClassCastException will be thrown if the value is neither
     * what you expected, nor a subclass of it
     *
     * @param cv
     * @param position
     * @param action
     * @return false if CVar is not passed as Command Line Argument or the consuming action is incompatible
     */
    fun <T> with(cv: CommandVariable, position: Int, action: Consumer<T>): Boolean {
        return try {
            val mapped = cVarMap[cv] ?: return false
            val item = mapped[position] as T
            action.accept(item)
            true
        } catch (ex: ClassCastException) {
            false
        }
    }

    /**
     * Tries to replace the CVar argument if already present or add it along with CVar
     * @param cv
     * @param value
     * @param position
     * @return false if invalid position or value class
     */
    fun <T> override(cv: CommandVariable, value: T, position: Int): Boolean {
        if (position < 0 || position >= cv.arguments.size) {
            return false
        }
        if (!cv.arguments[position].isInstance(value)) {
            return false
        }
        cVarMap.compute(cv) { key: CommandVariable?, array: Array<Any?>? ->
            var array = array
            if (array == null) {
                array = arrayOfNulls(cv.arguments.size)
            }
            array[position] = value
            array
        }
        return true
    }

    private fun readResponseFile(filename: String) {
        val r = ResponseReader()
        if (ResourceIO(filename).readLines(r as Consumer<String?>)) {
            println(String.format("Found response file %s, read %d command line variables", filename, r.cVarCount))
        } else {
            println(String.format("No such response file %s!", filename))
            System.exit(1)
        }
    }

    private fun processAllArgs(commandList: List<String>): Int {
        var cVarCount = 0
        var position = 0
        val limit = commandList.size
        while (limit > position) {
            position = processCVar(commandList, position)
            ++position
            ++cVarCount
        }
        return cVarCount
    }

    private fun processCVar(commandList: List<String>, position: Int): Int {
        val arg = commandList[position]
        if (!isCommandArgument(arg)) {
            return position
        }
        val cVarPrefix = arg[0]
        val cVarName = arg.substring(1)
        if (cVarPrefix == '@') {
            readResponseFile(cVarName)
            return position
        } else try {
            val cVar = CommandVariable.valueOf(cVarName.uppercase(Locale.getDefault()))
            if (cVar.prefix == cVarPrefix) {
                return when (cVar.getType()) {
                    CommandVariable.Type.PARAMETER -> {
                        cVarMap[cVar] = null
                        processCVarSubArgs(commandList, position, cVar)
                    }
                    CommandVariable.Type.VARARG -> processCVarSubArgs(commandList, position, cVar)
                    CommandVariable.Type.SWITCH -> {
                        cVarMap[cVar] = null
                        position
                    }
                    else -> {
                        cVarMap[cVar] = null
                        position
                    }
                }
            }
        } catch (ex: IllegalArgumentException) {
        } // ignore
        return position
    }

    private fun processCVarSubArgs(commandList: List<String>, position: Int, cVar: CommandVariable): Int {
        var position = position
        val cVarMappings = arrayOfNulls<Any>(cVar.arguments.size)
        for (j in cVar.arguments.indices) {
            if (cVar.arguments[j].isArray) {
                val elementClass = cVar.arguments[j].componentType
                val mapping = processVarArg(elementClass, commandList, position + 1)
                cVarMappings[j] = mapping
                position += mapping.size
                if (mapping.size == 0) {
                    break
                }
            } else if (processValue(cVar.arguments[j], commandList, position + 1).also {
                    cVarMappings[j] = it
                } == null) {
                break
            } else {
                ++position
            }
        }
        cVarMap[cVar] = cVarMappings
        return position
    }

    private fun processValue(elementClass: Class<*>, commandList: List<String>, position: Int): Any? {
        if (position < commandList.size) {
            val arg = commandList[position]
            if (!isCommandArgument(arg)) {
                return formatArgValue(elementClass, arg)
            }
        }
        return null
    }

    private fun processVarArg(elementClass: Class<*>, commandList: List<String>, position: Int): Array<Any> {
        var position = position
        val list: MutableList<Any> = ArrayList()
        var value: Any
        while (processValue(elementClass, commandList, position).also { value = it!! } != null) {
            list.add(value)
            ++position
        }
        // as String[] instanceof Object[], upcast
        return list.toArray(java.lang.reflect.Array.newInstance(elementClass, list.size) as IntFunction<Array<*>>)
    }

    private fun formatArgValue(format: Class<*>, arg: String): Any? {
        if (format == Int::class.java) {
            return try {
                arg.toInt()
            } catch (ex: NumberFormatException) {
                Loggers.getLogger(CommandVariable::class.java.name).log(Level.WARNING, null, ex)
                null
            }
        } else if (format == String::class.java) {
            return arg
        }
        return try {
            format.getDeclaredConstructor(String::class.java).newInstance(arg)
        } catch (ex: NoSuchMethodException) {
            Loggers.getLogger(CommandVariable::class.java.name).log(Level.SEVERE, null, ex)
            null
        } catch (ex: SecurityException) {
            Loggers.getLogger(CommandVariable::class.java.name).log(Level.SEVERE, null, ex)
            null
        } catch (ex: InstantiationException) {
            Loggers.getLogger(CommandVariable::class.java.name).log(Level.SEVERE, null, ex)
            null
        } catch (ex: IllegalAccessException) {
            Loggers.getLogger(CommandVariable::class.java.name).log(Level.SEVERE, null, ex)
            null
        } catch (ex: IllegalArgumentException) {
            Loggers.getLogger(CommandVariable::class.java.name).log(Level.SEVERE, null, ex)
            null
        } catch (ex: InvocationTargetException) {
            Loggers.getLogger(CommandVariable::class.java.name).log(Level.SEVERE, null, ex)
            null
        }
    }

    private fun isCommandArgument(arg: String): Boolean {
        if (arg.length < CommandVariable.MIN_CVAR_LENGTH) return false
        when (arg[0]) {
            '-', '+', '@' -> return true
        }
        return false
    }

    private inner class ResponseReader : Consumer<String> {
        var cVarCount = 0
        override fun accept(line: String) {
            cVarCount += processAllArgs(Arrays.asList(*line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()))
        }
    }
}