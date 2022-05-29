package utils


import p.Resettable
import s.*
import w.InputStreamSugar
import java.io.*
import java.net.*
import java.util.*

/**
 * Some utilities that emulate C stlib methods or provide convenient functions
 * to do repetitive system and memory related stuff.
 *
 * @author Maes
 */
object C2JUtils {
    fun strcpy(s1: CharArray, s2: CharArray): CharArray {
        System.arraycopy(s2, 0, s1, 0, Math.min(s1.size, s2.size))
        return s1
    }

    fun strcpy(s1: CharArray, s2: CharArray, off: Int, len: Int): CharArray {
        for (i in 0 until len) {
            s1[i] = s2[i + off]
        }
        return s1
    }

    fun strcpy(s1: CharArray, s2: CharArray, off: Int): CharArray {
        for (i in 0 until Math.min(s1.size, s2.size - off)) {
            s1[i] = s2[i + off]
        }
        return s1
    }

    fun strcpy(s1: CharArray, s2: String): CharArray {
        for (i in 0 until Math.min(s1.size, s2.length)) {
            s1[i] = s2[i]
        }
        return s1
    }

    /** Return a byte[] array from the string's chars,
     * ANDed to the lowest 8 bits.
     *
     * @param str
     * @return
     */
    fun toByteArray(str: String): ByteArray {
        val retour = ByteArray(str.length)
        for (i in 0 until str.length) {
            retour[i] = (str[i].code and 0xFF).toByte()
        }
        return retour
    }

    /**
     * Finds index of first element of array matching key. Useful whenever an
     * "indexOf" property is required or you encounter the C-ism [pointer-
     * array_base] used to find array indices in O(1) time. However, since this
     * is just a dumb unsorted search, running time is O(n), so use this method
     * only sparingly and in scenarios where it won't occur very frequently
     * -once per level is probably OK-, but watch out for nested loops, and
     * cache the result whenever possible. Consider adding an index or ID type
     * of field to the searched type if you require to use this property too
     * often.
     *
     * @param array
     * @param key
     * @return
     */
    fun indexOf(array: Array<Any>, key: Any): Int {
        for (i in array.indices) {
            if (array[i] === key) {
                return i
            }
        }
        return -1
    }

    /**
     * Emulates C-style "string comparison". "Strings" are considered
     * null-terminated, and comparison is performed only up to the smaller of
     * the two.
     *
     * @param s1
     * @param s2
     * @return
     */
    fun strcmp(s1: CharArray, s2: CharArray): Boolean {
        var match = true
        for (i in 0 until Math.min(s1.size, s2.size)) {
            if (s1[i] != s2[i]) {
                match = false
                break
            }
        }
        return match
    }

    fun strcmp(s1: CharArray, s2: String): Boolean {
        return C2JUtils.strcmp(s1, s2.toCharArray())
    }

    /**
     * C-like string length (null termination).
     *
     * @param s1
     * @return
     */
    fun strlen(s1: CharArray?): Int {
        if (s1 == null) return 0
        var len = 0
        while (s1[len++].code > 0) {
            if (len >= s1.size) break
        }
        return len - 1
    }

    /**
     * Return a new String based on C-like null termination.
     *
     * @param s
     * @return
     */
    fun nullTerminatedString(s: CharArray?): String {
        if (s == null) return ""
        var len = 0
        while (s[len++].code > 0) {
            if (len >= s.size) break
        }
        return String(s, 0, len - 1)
    }

    /**
     * Automatically "initializes" arrays of objects with their default
     * constuctor. It's better than doing it by hand, IMO. If you have a better
     * way, be my guest.
     *
     * @param os
     * @param c
     * @throws Exception
     * @throws
     */
    fun <T> initArrayOfObjects(os: Array<T?>, c: Class<T>) {
        try {
            for (i in os.indices) {
                os[i] = c.newInstance()
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + os.size
                        + " objects of class" + c.name + "!"
            )
            System.exit(-1)
        } catch (e: InstantiationException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + os.size
                        + " objects of class" + c.name + "!"
            )
            System.exit(-1)
        }
    }

    /**
     * Automatically "initializes" arrays of objects with their default
     * constuctor. It's better than doing it by hand, IMO. If you have a better
     * way, be my guest.
     *
     * @param os
     * @throws Exception
     * @throws
     */
    @Deprecated("")
    fun <T> initArrayOfObjects(os: Array<T>) {
        val c = os.javaClass.componentType as Class<T>
        try {
            for (i in os.indices) {
                os[i] = c.newInstance()
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + os.size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        } catch (e: InstantiationException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + os.size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        }
    }

    /**
     * Use of this method is very bad. It prevents refactoring measures. Also,
     * the use of reflection is acceptable on initialization, but in runtime it
     * causes performance loss. Use instead:
     * SomeType[] array = new SomeType[length];
     * Arrays.setAll(array, i -> new SomeType());
     *
     * - Good Sign 2017/05/07
     *
     * Uses reflection to automatically create and initialize an array of
     * objects of the specified class. Does not require casting on "reception".
     *
     * @param <T>
     * @param c
     * @param num
     * @return
     * @return
    </T> */
    @Deprecated("")
    fun <T> createArrayOfObjects(c: Class<T>, num: Int): Array<T> {
        val os = C2JUtils.getNewArray(c, num)!!
        try {
            for (i in os.indices) {
                os[i] = c.newInstance()
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            System.err.println("Failure to instantiate " + os.size + " objects of class " + c.name + "!")
            System.exit(-1)
        } catch (e: InstantiationException) {
            e.printStackTrace()
            System.err.println("Failure to instantiate " + os.size + " objects of class " + c.name + "!")
            System.exit(-1)
        }
        return os
    }

    /**
     * Uses reflection to automatically create and initialize an array of
     * objects of the specified class. Does not require casting on "reception".
     * Requires an instance of the desired class. This allows getting around
     * determining the runtime type of parametrized types.
     *
     *
     * @param <T>
     * @param instance An instance of a particular class.
     * @param num
     * @return
     * @return
    </T> */
    fun <T> createArrayOfObjects(instance: T, num: Int): Array<T> {
        val os: Array<T>
        val c = instance!!::class.java as Class<T>
        os = C2JUtils.getNewArray(c, num)!!
        try {
            for (i in os.indices) {
                os[i] = c.newInstance()
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            System.err.println(
                "Failure to instantiate " + os.size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        } catch (e: InstantiationException) {
            e.printStackTrace()
            System.err.println(
                "Failure to instantiate " + os.size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        }
        return os
    }

    /**
     * Automatically "initializes" arrays of objects with their default
     * constuctor. It's better than doing it by hand, IMO. If you have a better
     * way, be my guest.
     *
     * @param os
     * @param startpos inclusive
     * @param endpos non-inclusive
     * @throws Exception
     * @throws
     */
    fun <T> initArrayOfObjects(os: Array<T>, startpos: Int, endpos: Int) {
        val c = os.javaClass.componentType as Class<T>
        try {
            for (i in startpos until endpos) {
                os[i] = c.newInstance()
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + os.size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        } catch (e: InstantiationException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + os.size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        }
    }

    /** This method gets eventually inlined, becoming very fast  */
    fun toUnsignedByte(b: Byte): Int {
        return 0x000000FF and b.toInt()
    }

    // Optimized array-fill methods designed to operate like C's memset.
    fun memset(array: BooleanArray, value: Boolean, len: Int) {
        if (len > 0) array[0] = value
        var i = 1
        while (i < len) {
            System.arraycopy(array, 0, array, i, if (len - i < i) len - i else i)
            i += i
        }
    }

    fun memset(array: ByteArray, value: Byte, len: Int) {
        if (len > 0) array[0] = value
        var i = 1
        while (i < len) {
            System.arraycopy(array, 0, array, i, if (len - i < i) len - i else i)
            i += i
        }
    }

    fun memset(array: CharArray, value: Char, len: Int) {
        if (len > 0) array[0] = value
        var i = 1
        while (i < len) {
            System.arraycopy(array, 0, array, i, if (len - i < i) len - i else i)
            i += i
        }
    }

    fun memset(array: IntArray, value: Int, len: Int) {
        if (len > 0) array[0] = value
        var i = 1
        while (i < len) {
            System.arraycopy(array, 0, array, i, if (len - i < i) len - i else i)
            i += i
        }
    }

    fun memset(array: ShortArray, value: Short, len: Int) {
        if (len > 0) {
            array[0] = value
        }
        var i = 1
        while (i < len) {
            System.arraycopy(array, 0, array, i, if (len - i < i) len - i else i)
            i += i
        }
    }

    fun unsigned(num: Int): Long {
        return 0xFFFFFFFFL and num.toLong()
    }

    fun unsigned(num: Short): Char {
        return Char(num.toUShort())
    }

    /**
     * Convenient alias for System.arraycopy(src, 0, dest, 0, length);
     *
     * @param dest
     * @param src
     * @param length
     */
    fun memcpy(dest: Any?, src: Any?, length: Int) {
        System.arraycopy(src, 0, dest, 0, length)
    }

    fun testReadAccess(URI: String?): Boolean {
        var `in`: InputStream

        // This is bullshit.
        if (URI == null) {
            return false
        }
        if (URI.length == 0) {
            return false
        }
        try {
            `in` = FileInputStream(URI)
        } catch (e: FileNotFoundException) {
            // Not a file...
            val u: URL
            u = try {
                URL(URI)
            } catch (e1: MalformedURLException) {
                return false
            }
            `in` = try {
                u.openConnection().getInputStream()
            } catch (e1: IOException) {
                return false
            }
        }
        if (`in` != null) {
            try {
                `in`.close()
            } catch (e: IOException) {
            }
            return true
        }
        // All is well. Go on...
        return true
    }

    fun testWriteAccess(URI: String?): Boolean {
        var out: OutputStream

        // This is bullshit.
        if (URI == null) {
            return false
        }
        if (URI.length == 0) {
            return false
        }
        try {
            out = FileOutputStream(URI)
        } catch (e: FileNotFoundException) {
            // Not a file...
            val u: URL
            u = try {
                URL(URI)
            } catch (e1: MalformedURLException) {
                return false
            }
            out = try {
                u.openConnection().getOutputStream()
            } catch (e1: IOException) {
                return false
            }
        }
        if (out != null) {
            try {
                out.close()
            } catch (e: IOException) {
            }
            return true
        }
        // All is well. Go on...
        return true
    }

    /**
     * Returns true if flags are included in arg. Synonymous with (flags &
     * arg)!=0
     *
     * @param flags
     * @param arg
     * @return
     */
    fun flags(flags: Int, arg: Int): Boolean {
        return flags and arg != 0
    }

    fun flags(flags: Long, arg: Long): Boolean {
        return flags and arg != 0L
    }

    /**
     * Returns 1 for true and 0 for false. Useful, given the amount of
     * "arithmetic" logical functions in legacy code. Synonymous with
     * (expr?1:0);
     *
     * @param flags
     * @param arg
     * @return
     */
    fun eval(expr: Boolean): Int {
        return if (expr) 1 else 0
    }

    /**
     * Returns 1 for non-null and 0 for null objects. Useful, given the amount
     * of "existential" logical functions in legacy code. Synonymous with
     * (expr!=null);
     *
     * @param flags
     * @param arg
     * @return
     */
    fun eval(expr: Any?): Boolean {
        return expr != null
    }

    /**
     * Returns true for expr!=0, false otherwise.
     *
     * @param flags
     * @param arg
     * @return
     */
    fun eval(expr: Int): Boolean {
        return expr != 0
    }

    /**
     * Returns true for expr!=0, false otherwise.
     *
     * @param flags
     * @param arg
     * @return
     */
    fun eval(expr: Long): Boolean {
        return expr != 0L
    }

    fun resetAll(r: Array<Resettable>) {
        for (r1 in r) {
            r1.reset()
        }
    }

    /**
     * Useful for unquoting strings, since StringTokenizer won't do it for us.
     * Returns null upon any failure.
     *
     * @param s
     * @param c
     * @return
     */
    fun unquote(s: String, c: Char): String? {
        val firstq = s.indexOf(c)
        val lastq = s.lastIndexOf(c)
        // Indexes valid?
        return if (C2JUtils.isQuoted(s, c))
            s.substring(firstq + 1, lastq)
        else null
    }

    fun isQuoted(s: String, c: Char): Boolean {
        val q1 = s.indexOf(c)
        val q2 = s.lastIndexOf(c)
        val c1: Char
        val c2: Char

        // Indexes valid?
        if (q1 != -1 && q2 != -1) {
            if (q1 < q2) {
                c1 = s[q1]
                c2 = s[q2]
                return c1 == c2
            }
        }
        return false
    }

    fun unquoteIfQuoted(s: String, c: Char): String {
        val tmp = C2JUtils.unquote(s, c)
        return tmp ?: s
    }

    /**
     * Return either 0 or a hashcode
     *
     * @param o
     */
    fun pointer(o: Any?): Int {
        return o?.hashCode() ?: 0
    }

    fun checkForExtension(filename: String?, ext: String?): Boolean {

        // Null filenames satisfy null extensions.
        var filename = filename
        if ((filename == null || filename.isEmpty()) && (ext == null || ext.isEmpty())) {
            return true
        } else if (filename == null) { // avoid NPE - Good Sign 2017/05/07
            filename = ""
        }
        val separator = System.getProperty("file.separator")

        // Remove the path upto the filename.
        val lastSeparatorIndex = filename.lastIndexOf(separator)
        if (lastSeparatorIndex != -1) {
            filename = filename.substring(lastSeparatorIndex + 1)
        }
        val realext: String

        // Get extension separator. It SHOULD be . on all platforms, right?
        val pos = filename.lastIndexOf('.')
        if (pos >= 0 && pos <= filename.length - 2) { // Extension present

            // Null comparator on valid extension
            if (ext == null || ext.isEmpty()) return false
            realext = filename.substring(pos + 1)
            return realext.compareTo(ext, ignoreCase = true) == 0
        } else if (ext == null || ext.isEmpty()) { // No extension, and null/empty comparator
            return true
        }

        // No extension, and non-null/nonempty comparator.
        return false
    }

    /** Return the filename without extension, and stripped
     * of the path.
     *
     * @param s
     * @return
     */
    fun removeExtension(s: String): String {
        val separator = System.getProperty("file.separator")
        val filename: String

        // Remove the path upto the filename.
        val lastSeparatorIndex = s.lastIndexOf(separator)
        filename = if (lastSeparatorIndex == -1) {
            s
        } else {
            s.substring(lastSeparatorIndex + 1)
        }

        // Remove the extension.
        val extensionIndex = filename.lastIndexOf('.')
        return if (extensionIndex == -1) {
            filename
        } else filename.substring(0, extensionIndex)
    }

    /**
     * This method is supposed to return the "name" part of a filename. It was
     * intended to return length-limited (max 8 chars) strings to use as lump
     * indicators. There's normally no need to enforce this behavior, as there's
     * nothing preventing the engine from INTERNALLY using lump names with >8
     * chars. However, just to be sure...
     *
     * @param path
     * @param limit  Set to any value >0 to enforce a length limit
     * @param whole keep extension if set to true
     * @return
     */
    fun extractFileBase(path: String?, limit: Int, whole: Boolean): String? {
        if (path == null) return path
        var src = path.length - 1
        val separator = System.getProperty("file.separator")
        src = path.lastIndexOf(separator) + 1
        if (src < 0) // No separator
            src = 0
        var len = path.lastIndexOf('.')
        if (whole || len < 0) len = path.length - src // No extension.
        else len -= src

        // copy UP to the specific number of characters, or all        
        if (limit > 0) len = Math.min(limit, len)
        return path.substring(src, src + len)
    }

    /** Maes: File intead of "inthandle"  */
    fun filelength(handle: File): Long {
        return try {
            handle.length()
        } catch (e: Exception) {
            System.err.println("Error fstating")
            -1
        }
    }

    fun <T> resize(oldarray: Array<T?>, newsize: Int): Array<T?>? {
        if (oldarray[0] != null) {
            return C2JUtils.resize(oldarray[0], oldarray, newsize)
        }
        val cls: T
        return try {
            cls = oldarray.javaClass.componentType.newInstance() as T
            C2JUtils.resize(cls, oldarray, newsize)
        } catch (e: IllegalAccessException) {
            System.err.println("Cannot autodetect type in resizeArray.\n")
            null
        } catch (e: InstantiationException) {
            System.err.println("Cannot autodetect type in resizeArray.\n")
            null
        }
    }

    /** Generic array resizing method. Calls Arrays.copyOf but then also
     * uses initArrayOfObject for the "abundant" elements.
     *
     * @param <T>
     * @param instance
     * @param oldarray
     * @param newsize
     * @return
    </T> */
    fun <T> resize(instance: T, oldarray: Array<T>, newsize: Int): Array<T> {
        //  Hmm... nope.
        if (newsize <= oldarray.size) {
            return oldarray
        }

        // Copy old array with built-in stuff.
        val tmp = Arrays.copyOf(oldarray, newsize)

        // Init the null portions as well
        C2JUtils.initArrayOfObjects(tmp, oldarray.size, tmp.size)
        System.out.printf("Old array of type %s resized. New capacity: %d\n", instance!!::class.java, newsize)
        return tmp
    }

    /** Resize an array without autoinitialization. Same as Arrays.copyOf(..), just
     * prints a message.
     *
     * @param <T>
     * @param oldarray
     * @param newsize
     * @return
    </T> */
    fun <T> resizeNoAutoInit(oldarray: Array<T>?, newsize: Int): Array<T> {
        // For non-autoinit types, this is enough.
        val tmp = Arrays.copyOf(oldarray, newsize)
        System.out.printf(
            "Old array of type %s resized without auto-init. New capacity: %d\n",
            tmp.javaClass.componentType, newsize
        )
        return tmp
    }

    fun <T> getNewArray(instance: T, size: Int): Array<T>? {
        val c = instance!!::class.java as Class<T>
        try {
            return java.lang.reflect.Array.newInstance(c, size) as Array<T>
        } catch (e: NegativeArraySizeException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        }
        return null
    }

    fun <T> getNewArray(size: Int, instance: T): Array<T> {
        val c = instance!!::class.java as Class<T>
        return C2JUtils.getNewArray(c, size)!!
    }

    fun <T> getNewArray(c: Class<T>, size: Int): Array<T>? {
        try {
            return java.lang.reflect.Array.newInstance(c, size) as Array<T>
        } catch (e: NegativeArraySizeException) {
            e.printStackTrace()
            System.err.println(
                "Failure to allocate " + size
                        + " objects of class " + c.name + "!"
            )
            System.exit(-1)
        }
        return null
    }

    /**
     * Try to guess whether a URI represents a local file, a network any of the
     * above but zipped. Returns
     *
     * @param URI
     * @return an int with flags set according to InputStreamSugar
     */
    fun guessResourceType(URI: String?): Int {
        var result = 0
        var `in`: InputStream

        // This is bullshit.
        if (URI == null || URI.length == 0) {
            return InputStreamSugar.BAD_URI
        }
        try {
            `in` = FileInputStream(File(URI))
            // It's a file
            result = result or InputStreamSugar.FILE
        } catch (e: FileNotFoundException) {
            // Not a file...
            val u: URL
            u = try {
                URL(URI)
            } catch (e1: MalformedURLException) {
                return InputStreamSugar.BAD_URI
            }
            try {
                `in` = u.openConnection().getInputStream()
                result = result or InputStreamSugar.NETWORK_FILE
            } catch (e1: IOException) {
                return InputStreamSugar.BAD_URI
            }
        }

        // Try guessing if it's a ZIP file. A bit lame, really
        // TODO: add proper validation, and maybe MIME type checking
        // for network streams, for cases that we can't really
        // tell from extension alone.
        if (C2JUtils.checkForExtension(URI, "zip")) {
            result = result or InputStreamSugar.ZIP_FILE
        }
        try {
            `in`.close()
        } catch (e: IOException) {
        }

        // All is well. Go on...
        return result
    }
}