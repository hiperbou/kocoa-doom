package s
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer
import java.lang.reflect.Field

class DoomIO(var `is`: InputStream, var os: OutputStream) {
    enum class Endian {
        BIG, LITTLE
    }

    companion object {
        fun toUnsigned(signed: Byte): Int {
            var unsigned = signed.toInt() and 0xff
            unsigned = if (signed >= 0) signed.toInt() else 256 + signed
            unsigned = (256 + signed) % 256
            return unsigned
        }

        @Throws(IOException::class)
        fun fread(bytes: ByteArray?, size: Int, count: Int, file: InputStream): Int {
            var count = count
            var retour = 0
            do {
                if (file.read(bytes, retour * size, size) < size) return retour
                retour++
            } while (--count > 0)
            return retour
        }

        @JvmOverloads
        @Throws(IOException::class)
        fun freadint(file: InputStream, nbBytes: Int = 2): Int {
            val bytes = ByteArray(nbBytes)
            if (DoomIO.fread(bytes, nbBytes, 1, file) < 1) return -1
            var retour: Long = 0
            for (i in 0 until nbBytes) {
                retour += DoomIO.toUnsigned(bytes[i]) * Math.pow(256.0, i.toDouble()).toLong()
            }
            //toUnsigned(bytes[1])*256 + toUnsigned(bytes[0]);
            if (retour > Math.pow(256.0, nbBytes.toDouble()).toLong() / 2) retour -= Math.pow(256.0, nbBytes.toDouble())
                .toLong()
            return retour.toInt()
        }

        @Throws(IOException::class)
        fun fwrite2(ptr: ByteArray, offset: Int, size: Int, file: Any?): Int {
            DoomIO.fwrite(ptr, offset, size, 1, file)
            return 0
        }

        @Throws(IOException::class)
        fun fwrite2(ptr: ByteArray, size: Int, file: Any?): Int {
            return DoomIO.fwrite2(ptr, 0, size, file)
        }

        @Throws(IOException::class)
        fun fwrite2(ptr: ByteArray, file: Any?): Int {
            return DoomIO.fwrite2(ptr, 0, ptr.size, file)
        }

        @Throws(IOException::class)
        fun fwrite(bytes: String, size: Int, count: Int, file: Any?) {
            DoomIO.fwrite(DoomIO.toByteArray(bytes), size, count, file)
        }

        @Throws(IOException::class)
        fun fwrite(bytes: ByteArray, size: Int, count: Int, file: Any?) {
            DoomIO.fwrite(bytes, 0, size, count, file)
        }

        @Throws(IOException::class)
        fun fwrite(bytes: ByteArray, offset: Int, size: Int, count: Int, file: Any?) {
            if (file is OutputStream) {
                /*byte[] b = bytes;
			if (bytes.length < size) {
				b = new byte[size];
				copyBytes(from, to, offset)
			}*/
                file.write(bytes, offset, Math.min(bytes.size, size))
                for (i in bytes.size until size) file.write(
                    0.toByte().toInt()
                ) // padding effect if size is bigger than byte array
            }
            if (file is Writer) {
                val ch = CharArray(bytes.size)
                for (i in bytes.indices) {
                    ch[i] = DoomIO.toUnsigned(bytes[i]).toChar()
                }
                file.write(ch, offset, size)
            }
        }

        fun toByteArray(str: String): ByteArray {
            val retour = ByteArray(str.length)
            for (i in 0 until str.length) {
                retour[i] = (str[i].code and 0xFF).toByte()
            }
            return retour
        }

        fun toByteArray(str: Int): ByteArray {
            return DoomIO.toByteArray(str, 2)
        }

        var writeEndian = Endian.LITTLE
        fun byteIdx(i: Int, nbBytes: Int): Int {
            return if (DoomIO.writeEndian == Endian.BIG) i else nbBytes - 1 - i
        }

        fun copyBytes(from: ByteArray, to: ByteArray, offset: Int) {
            var offset = offset
            for (b in from) {
                to[offset++] = b
            }
        }

        fun toByteArray(str: Long, nbBytes: Int): ByteArray {
            return DoomIO.toByteArray(str.toInt(), nbBytes)
        }

        fun toByteArray(str: Short, nbBytes: Int): ByteArray {
            return DoomIO.toByteArray(str.toInt(), nbBytes)
        }

        fun toByteArray(str: IntArray, nbBytes: Int): ByteArray {
            val bytes = ByteArray(str.size * nbBytes)
            for (i in str.indices) {
                DoomIO.copyBytes(DoomIO.toByteArray(str[i], nbBytes), bytes, i * nbBytes)
            }
            return bytes
        }

        /*
		 public static byte[] toByteArray(boolean[] bools, int nbBytes) {
			 byte[] bytes = new byte[bools.length*nbBytes];
			 for (int i = 0; i < bools.length; i++) {
				 copyBytes(toByteArray(bools[i], nbBytes), bytes, i*nbBytes);
			 }
			 return bytes;
		 } */
        /*
		 public static byte[] toByteArray(Boolean bool, int nbBytes) {
			 int val = (bool?1:0);
			 return toByteArray(val, nbBytes);
		 }*/
        fun toByteArray(str: Int, nbBytes: Int): ByteArray {
            var `val` = str.toLong()
            if (`val` < 0) `val` = Math.pow(256.0, nbBytes.toDouble()).toLong() + `val`
            val bytes = ByteArray(nbBytes)
            var tmp = `val`
            for (i in 0 until nbBytes - 1) {
                bytes[DoomIO.byteIdx(i, nbBytes)] = (tmp % 256).toByte()
                tmp = tmp / 256
            }
            bytes[DoomIO.byteIdx(nbBytes - 1, nbBytes)] = tmp.toByte()
            return bytes
        }

        @Throws(NoSuchFieldException::class)
        private fun getField(clazz: Class<*>, fieldName: String): Field {
            return try {
                clazz.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                val superClass = clazz.superclass
                if (superClass == null) {
                    throw e
                } else {
                    DoomIO.getField(superClass, fieldName)
                }
            }
        }

        fun linkBA(obj: Any, fieldName: Any?, stream: Any?, size: Int) {
            if (stream is OutputStream) {
                try {
                    var `val`: Any? = null
                    if (fieldName is String) {
                        `val` = DoomIO.getField(obj.javaClass, fieldName as String).get(obj)
                        if (`val` is Enum<*>) {
                            `val` = (`val` as Enum<*>?)!!.ordinal
                        }
                    }
                    if (fieldName is Int) {
                        `val` = fieldName
                    }
                    val method =
                        DoomIO::class.java.getMethod("toByteArray", `val`!!.javaClass, Int::class.javaPrimitiveType)
                    val bytes = method.invoke(null, `val`, size) as ByteArray
                    stream.write(bytes)
                } catch (e: Exception) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
            if (stream is InputStream) {
                try {
                    if (fieldName is String) {
                        val field = obj.javaClass.getField(fieldName as String?)
                        DoomIO.assigner(obj, field, stream, size)
                    }
                    if (fieldName is Int) {
                        stream.read(ByteArray(size))
                    }
                } catch (e: Exception) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }


//		    	public static int freadint(InputStream file, int nbBytes) throws IOException {
        }

        @Throws(IOException::class, IllegalArgumentException::class, IllegalAccessException::class)
        fun assigner(obj: Any?, field: Field, `is`: InputStream, size: Int) {
            val c = field.type
            if (c.isArray) {
                val a = field[obj]
                val len = java.lang.reflect.Array.getLength(a)
                for (i in 0 until len) {
                    val `val`: Int = DoomIO.freadint(`is`, size)
                    val o = java.lang.reflect.Array.get(a, i)
                    java.lang.reflect.Array.set(a, i, DoomIO.assignValue(`val`, o, o.javaClass))
                }
                return
            }
            val `val`: Int = DoomIO.freadint(`is`, size)
            val v: Any = DoomIO.assignValue(`val`, field[obj], field.type)
            field[obj] = v

            /*Object[] enums = c.getEnumConstants();
				if (enums != null) {
					int val = DoomIO.freadint((InputStream)is, size);
					field.set(obj, enums[val]);
				}
				else {
					int val = DoomIO.freadint((InputStream)is, size);
					field.set(obj, val);
				}*/
        }

        fun assignValue(`val`: Int, objToReplace: Any?, classe: Class<*>): Any {
            if (classe.isAssignableFrom(Boolean::class.java) || classe.isAssignableFrom(Boolean::class.javaPrimitiveType)) {
                return if (`val` == 0) false else true
            }
            val enums = classe.getEnumConstants()
            if (enums != null) {
                //int val = DoomIO.freadint((InputStream)is, size);
                return enums[`val`]
                //field.set(obj, enums[val]);
            } else {
                //int val = DoomIO.freadint((InputStream)is, size);
                //field.set(obj, val);
            }
            return `val`
        }

        fun baToString(bytes: ByteArray): String {
            var str = ""
            var i = 0
            while (i < bytes.size && bytes[i].toInt() != 0) {
                str += Char(bytes[i].toUShort())
                i++
            }
            return str
        }

        fun indexOfArray(a: Array<Any>, o: Any): Int {
            for (i in a.indices) {
                if ( /*Array.get(a, i)*/a[i] === o) return i
            }
            return -1
        }
    }
}