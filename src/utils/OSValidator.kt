package utils


import java.util.*

/** Half-assed way of finding the OS we're running under, shamelessly
 * ripped from:
 *
 * http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/
 * .
 * This is required, as some things in AWT don't work exactly consistently cross-OS
 * (AWT frame size is the first thing that goes wrong, but also mouse grabbing
 * behavior).
 *
 * TODO: replace with Apache Commons library?
 *
 * @author velktron
 */
object OSValidator {
    fun isWindows(): Boolean {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        //windows
        return os.indexOf("win") >= 0
    }

    fun isMac(): Boolean {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        //Mac
        return os.indexOf("mac") >= 0
    }

    fun isUnix(): Boolean {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        //linux or unix
        return os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0
    }

    fun isUnknown(): Boolean {
        return !OSValidator.isWindows() && !OSValidator.isUnix() && !OSValidator.isMac()
    }
}