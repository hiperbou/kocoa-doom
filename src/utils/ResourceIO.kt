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
package utils


import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Resource IO to automate read/write on configuration/resources
 *
 * @author Good Sign
 */
class ResourceIO {
    private val file: Path
    private val charset = Charset.forName("US-ASCII")

    constructor(file: File) {
        this.file = file.toPath()
    }

    constructor(file: Path) {
        this.file = file
    }

    constructor(path: String?) {
        file = FileSystems.getDefault().getPath(path)
    }

    fun exists(): Boolean {
        return Files.exists(file)
    }

    fun readLines(lineConsumer: Consumer<String?>): Boolean {
        if (Files.exists(file)) {
            try {
                Files.newBufferedReader(file, charset).use { reader ->
                    reader.forEachLine {
                        lineConsumer.accept(it)
                    }
                    return true
                }
            } catch (x: IOException) {
                System.err.format("IOException: %s%n", x)
                return false
            }
        }
        return false
    }

    fun writeLines(lineSupplier: Supplier<String?>, vararg options: OpenOption?): Boolean {
        try {
            Files.newBufferedWriter(file, charset, *options).use { writer ->
                var line:String? = lineSupplier.get()
                while (line != null) {
                    writer.write(line, 0, line.length)
                    writer.newLine()
                    line = lineSupplier.get()
                }
                return true
            }
        } catch (x: IOException) {
            System.err.format("IOException: %s%n", x)
            return false
        }
    }

    fun getFilename(): String {
        return file.toString()
    }
}