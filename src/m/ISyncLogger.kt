package m


import java.io.IOException

interface ISyncLogger {
    @Throws(IOException::class)
    fun debugStart()
    fun debugEnd()
    fun sync(format: String?, vararg args: Any?)
}