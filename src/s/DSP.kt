package s

class DSP {
    /**
     * QDSS Windowed Sinc ReSampling subroutine in Basic
     *
     * @param x
     * new sample point location (relative to old indexes) (e.g. every
     * other integer for 0.5x decimation)
     * @param indat
     * = original data array
     * @param alim
     * = size of data array
     * @param fmax
     * = low pass filter cutoff frequency
     * @param fsr
     * = sample rate
     * @param wnwdth
     * = width of windowed Sinc used as the low pass filter rem resamp()
     * returns a filtered new sample point
     */
    fun resamp(
        x: Float, indat: FloatArray, alim: Int, fmax: Float,
        fsr: Float, wnwdth: Int
    ): Float {
        var i: Int
        var j: Int
        var r_w: Float
        val r_g: Float
        var r_a: Float
        var r_snc: Int
        var r_y: Int // some local variables
        r_g = 2 * fmax / fsr // Calc gain correction factor
        r_y = 0
        i = -wnwdth / 2
        while (i < wnwdth / 2) {
            // For 1 window width
            j = (x + i).toInt() // Calc input sample index
            // calculate von Hann Window. Scale and calculate Sinc
            r_w = (0.5 - 0.5 * Math.cos(
                2 * Math.PI
                        * (0.5 + (j - x) / wnwdth)
            )).toFloat()
            r_a = (2 * Math.PI * (j - x) * fmax / fsr).toFloat()
            r_snc = 1
            if (Math.abs(r_a) > 0) r_snc = (Math.sin(r_a.toDouble()) / r_a).toInt()
            if (j >= 0 && j < alim) {
                r_y = (r_y + r_g * r_w * r_snc * indat[j]).toInt()
            }
            i++
        }
        return r_y.toFloat() // return new filtered sample
    }

    companion object {
        /*
     * Ron Nicholson's QDSS ReSampler cookbook recipe QDSS = Quick, Dirty,
     * Simple and Short Version 0.1b - 2007-Aug-01 Copyright 2007 Ronald H.
     * Nicholson Jr. No warranties implied. Error checking, optimization, and
     * quality assessment of the "results" is left as an exercise for the
     * student. (consider this code Open Source under a BSD style license) IMHO.
     * YMMV. http://www.nicholson.com/rhn/dsp.html
     */
        /**
         * R. Nicholson's QDDS FIR filter generator cookbook recipe QDDS = Quick,
         * Dirty, Dumb and Short version 0.6b - 2006-Dec-14, 2007-Sep-30 No
         * warranties implied. Error checking, optimization, and quality assessment
         * of the "results" is left as an exercise for the student. (consider this
         * code Open Source under a BSD style license) Some example filter
         * parameters:
         *
         * @param fsr
         * = 44100 : rem set fsr = sample rate
         * @param fc
         * = 0 : rem set fc = 0 for lowpass fc = center frequency for
         * bandpass filter fc = fsr/2 for a highpass
         * @param bw
         * = 3000 : rem bw = bandwidth, range 0 .. fsr/2 and bw >= fsr/n bw =
         * 3 db corner frequency for a lowpass bw = half the 3 db passband
         * for a bandpass filter
         * @param nt
         * = 128 : rem nt = number of taps + 1 (nt must be even) nt should be
         * > fsr / bw transition band will be around 3.5 * fsr / nt depending
         * on the window applied and ripple spec.
         * @param g
         * = 1 : rem g = filter gain for bandpass g = 0.5 , half the gain for
         * a lowpass filter
         * @return array of FIR taps
         */
        fun wsfiltgen(nt: Int, fc: Double, fsr: Double, bw: Double, g: Double): DoubleArray {
            val fir = DoubleArray(nt) //
            // fir(0) = 0
            // fir(1) is the first tap
            // fir(nt/2) is the middle tap
            // fir(nt-1) is the last tap
            var a: Double
            var ys: Double
            var yg: Double
            var yf: Double
            var yw: Double
            for (i in 1 until nt) {
                a = (i - nt / 2) * 2.0 * Math.PI * bw / fsr // scale Sinc width
                ys = 1.0
                if (Math.abs(a) > 0) ys = Math.sin(a) / a // calculate Sinc function
                yg = g * (4.0 * bw / fsr) // correct window gain
                yw = 0.54 - 0.46 * Math.cos(i * 2.0 * Math.PI / nt) // Hamming
                // window
                yf = Math.cos((i - nt / 2) * 2.0 * Math.PI * fc / fsr) // spectral
                // shift to
                // fc
                fir[i] = yf * yw * yg * ys // rem assign fir coeff.
            }
            return fir
        }

        @JvmStatic
        fun main(argv: Array<String>) {
            val fir: DoubleArray = DSP.wsfiltgen(128, 11025 / 2.0, 22050.0, 22050 * 3.0 / 4, 0.5)
            println(fir)
        }

        fun crudeResample(input: ByteArray?, factor: Int): ByteArray? {
            if (input == null || input.size < 1) return null
            val LEN = input.size
            val res = ByteArray(LEN * factor)
            var k = 0
            var start: Float
            var end: Float
            res[0] = input[0]
            for (i in 0 until LEN) {
                start = if (i == 0) 127f else (0xFF and input[i].toInt()).toFloat()
                end = if (i < LEN - 1) (0xFF and input[i + 1].toInt()).toFloat() else 127f
                val slope = ((end - start) / factor).toDouble()
                res[k] = input[i]
                //res[k+factor]=input[i+1];
                for (j in 1 until factor) {
                    val ratio = j / factor.toDouble()
                    val value = start + slope * ratio
                    val bval = Math.round(value).toByte()
                    res[k + j] = bval
                }
                k += factor
            }
            return res
        }

        fun filter(input: ByteArray, samplerate: Int, cutoff: Int) {
            val tmp = DoubleArray(input.size)

            // Normalize
            for (i in input.indices) {
                tmp[i] = (0xFF and input[i].toInt()) / 255.0
            }
            DSP.filter(tmp, samplerate, cutoff.toDouble(), tmp.size)

            // De-normalize
            for (i in input.indices) {
                input[i] = (0xFF and (tmp[i] * 255.0).toInt()).toByte()
            }
        }

        /** Taken from here
         * http://baumdevblog.blogspot.gr/2010/11/butterworth-lowpass-filter-coefficients.html
         */
        private fun getLPCoefficientsButterworth2Pole(
            samplerate: Int,
            cutoff: Double,
            ax: DoubleArray,
            by: DoubleArray
        ) {
            val PI = 3.1415926535897932385
            val sqrt2 = 1.4142135623730950488
            val QcRaw = 2 * PI * cutoff / samplerate // Find cutoff frequency in [0..PI]
            val QcWarp = Math.tan(QcRaw) // Warp cutoff frequency
            val gain = 1 / (1 + sqrt2 / QcWarp + 2 / (QcWarp * QcWarp))
            by[2] = (1 - sqrt2 / QcWarp + 2 / (QcWarp * QcWarp)) * gain
            by[1] = (2 - 2 * 2 / (QcWarp * QcWarp)) * gain
            by[0] = 1.0
            ax[0] = 1 * gain
            ax[1] = 2 * gain
            ax[2] = 1 * gain
        }

        fun filter(samples: DoubleArray, smp: Int, cutoff: Double, count: Int) {
            // Essentially a 3-tap filter?
            val ax = DoubleArray(3)
            val by = DoubleArray(3)
            val xv = DoubleArray(3)
            val yv = DoubleArray(3)
            DSP.getLPCoefficientsButterworth2Pole(smp, cutoff, ax, by)
            for (i in 0 until count) {
                xv[2] = xv[1]
                xv[1] = xv[0]
                xv[0] = samples[i]
                yv[2] = yv[1]
                yv[1] = yv[0]
                yv[0] = ax[0] * xv[0] + ax[1] * xv[1] + ax[2] * xv[2] - by[1] * yv[0] - by[2] * yv[1]
                samples[i] = yv[0]
            }
        }
    }
}