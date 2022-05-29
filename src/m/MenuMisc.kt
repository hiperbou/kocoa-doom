package m

import i.DoomSystem
import w.IWritableDoomObject
import java.awt.image.*
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import javax.imageio.ImageIO

//-----------------------------------------------------------------------------
//
// $Id: MenuMisc.java,v 1.29 2012/09/24 17:16:22 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
//
// DESCRIPTION:
//	Main loop menu stuff.
//	Default Config File.
//	PCX Screenshots.
//
//-----------------------------------------------------------------------------
abstract class MenuMisc {
    abstract var showMessages: Boolean

    companion object {
        const val rcsid = "\$Id: MenuMisc.java,v 1.29 2012/09/24 17:16:22 velktron Exp $"

        //
        // SCREEN SHOTS
        //
        fun WriteFile(name: String?, source: ByteArray?, length: Int): Boolean {
            val handle: OutputStream
            try {
                handle = FileOutputStream(name)
                handle.write(source, 0, length)
                handle.close()
            } catch (e: Exception) {
                DoomSystem.MiscError("Couldn't write file %s (%s)", name, e.message)
                return false
            }
            return true
        }

        fun WriteFile(name: String?, source: IWritableDoomObject): Boolean {
            val handle: DataOutputStream
            try {
                handle = DataOutputStream(FileOutputStream(name))
                source.write(handle)
                handle.close()
            } catch (e: Exception) {
                DoomSystem.MiscError("Couldn't write file %s (%s)", name, e.message)
                return false
            }
            return true
        }

        /** M_ReadFile
         * This version returns a variable-size ByteBuffer, so
         * we don't need to know a-priori how much stuff to read.
         *
         */
        fun ReadFile(name: String?): ByteBuffer? {
            val handle: BufferedInputStream
            val length: Int
            // struct stat fileinfo;
            val buf: ByteBuffer
            try {
                handle = BufferedInputStream(FileInputStream(name))
                length = handle.available()
                buf = ByteBuffer.allocate(length)
                handle.read(buf.array())
                handle.close()
            } catch (e: Exception) {
                DoomSystem.MiscError("Couldn't read file %s (%s)", name, e.message)
                return null
            }
            return buf
        }

        /** M_ReadFile  */
        fun ReadFile(name: String?, buffer: ByteArray): Int {
            val handle: BufferedInputStream
            val count: Int
            val length: Int
            // struct stat fileinfo;
            val buf: ByteArray
            try {
                handle = BufferedInputStream(FileInputStream(name))
                length = handle.available()
                buf = ByteArray(length)
                count = handle.read(buf)
                handle.close()
                if (count < length) throw Exception(
                    "Read only " + count + " bytes out of "
                            + length
                )
            } catch (e: Exception) {
                DoomSystem.MiscError("Couldn't read file %s (%s)", name, e.message)
                return -1
            }
            System.arraycopy(buf, 0, buffer, 0, Math.min(count, buffer.size))
            return length
        }

        //
        // WritePCXfile
        //
        fun WritePCXfile(
            filename: String?,
            data: ByteArray,
            width: Int,
            height: Int,
            palette: ByteArray
        ) {
            val length: Int
            val pcx: pcx_t
            val pack: ByteArray
            pcx = pcx_t()
            pack = ByteArray(width * height * 2) // allocate that much data, just in case.
            pcx.manufacturer = 0x0a // PCX id
            pcx.version = 5 // 256 color
            pcx.encoding = 1 // uncompressed
            pcx.bits_per_pixel = 8 // 256 color
            pcx.xmin = 0.toChar()
            pcx.ymin = 0.toChar()
            pcx.xmax = (width - 1).toChar()
            pcx.ymax = (height - 1).toChar()
            pcx.hres = width.toChar()
            pcx.vres = height.toChar()
            // memset (pcx->palette,0,sizeof(pcx->palette));
            pcx.color_planes = 1 // chunky image
            pcx.bytes_per_line = width.toChar()
            pcx.palette_type = 2.toChar() // not a grey scale
            //memset (pcx->filler,0,sizeof(pcx->filler));


            // pack the image
            //pack = &pcx->data;
            var p_pack = 0
            for (i in 0 until width * height) {
                if (data[i].toInt() and 0xc0 != 0xc0) pack[p_pack++] = data[i] else {
                    pack[p_pack++] = 0xc1.toByte()
                    pack[p_pack++] = data[i]
                }
            }

            // write the palette
            pack[p_pack++] = 0x0c // palette ID byte
            for (i in 0..767) pack[p_pack++] = palette[i]

            // write output file
            length = p_pack
            pcx.data = Arrays.copyOf(pack, length)
            var f: DataOutputStream? = null
            try {
                f = DataOutputStream(FileOutputStream(filename))
            } catch (e: FileNotFoundException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            try {
                //f.setLength(0);
                pcx.write(f!!)
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        fun WritePNGfile(imagename: String?, linear: ShortArray?, width: Int, height: Int) {
            val buf = BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB)
            val sh = buf.raster.dataBuffer as DataBufferUShort
            val shd = sh.data
            System.arraycopy(linear, 0, shd, 0, Math.min(linear!!.size, shd.size))
            try {
                ImageIO.write(buf, "PNG", File(imagename))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun WritePNGfile(imagename: String?, linear: IntArray?, width: Int, height: Int) {
            val buf = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val sh = buf.raster.dataBuffer as DataBufferInt
            val shd = sh.data
            System.arraycopy(linear, 0, shd, 0, Math.min(linear!!.size, shd.size))
            try {
                ImageIO.write(buf, "PNG", File(imagename))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun WritePNGfile(imagename: String?, linear: ByteArray?, width: Int, height: Int, icm: IndexColorModel?) {
            val buf = BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, icm)
            val sh = buf.raster.dataBuffer as DataBufferByte
            val shd = sh.data
            System.arraycopy(linear, 0, shd, 0, Math.min(linear!!.size, shd.size))
            try {
                ImageIO.write(buf, "PNG", File(imagename))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}