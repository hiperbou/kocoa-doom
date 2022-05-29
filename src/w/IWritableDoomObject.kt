package w

import java.io.DataOutputStream
import java.io.IOException

interface IWritableDoomObject {
    @Throws(IOException::class)
    fun write(dos: DataOutputStream)
}