package w

object JadDecompress {
    const val WINDOW_SIZE = 4096
    const val LOOKAHEAD_SIZE = 16
    const val LENSHIFT = 4 /* this must be log2(LOOKAHEAD_SIZE) */
    fun decode(input: ByteArray, output: ByteArray) {
        /*
         * #ifdef JAGUAR decomp_input = input; decomp_output = output;
         * gpufinished = zero; gpucodestart = (int)&decomp_start; while
         * (!I_RefreshCompleted () ) ; #else
         */
        var getidbyte = 0
        var len: Int
        var pos: Int
        var i: Int
        var source_ptr: Int
        var input_ptr = 0
        var output_ptr = 0
        var idbyte = 0
        while (true) {

            /* get a new idbyte if necessary */
            if (getidbyte == 0) idbyte = 0xFF and input[input_ptr++].toInt()
            getidbyte = getidbyte + 1 and 7
            if (idbyte and 1 != 0) {
                /* decompress */
                pos = 0xFF and input[input_ptr++].toInt() shl JadDecompress.LENSHIFT
                pos = pos or (0xFF and input[input_ptr].toInt() shr JadDecompress.LENSHIFT)
                source_ptr = output_ptr - pos - 1
                len = (0xFF and input[input_ptr++].toInt() and 0xf) + 1
                if (len == 1) break
                i = 0
                while (i < len) {
                    output[output_ptr++] = output[source_ptr++]
                    i++
                }
            } else {
                output[output_ptr++] = input[input_ptr++]
            }
            idbyte = idbyte shr 1
        }
        System.out.printf("Expanded %d to %d\n", input_ptr, output_ptr)
    }
}