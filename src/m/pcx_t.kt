package m


import w.IWritableDoomObject
import java.io.DataOutputStream
import java.io.IOException

/** Yeah, this is actually a PCX header implementation, and Mocha Doom
 * saved PCX screenshots. Implemented it back just to shot that it can be
 * done (will switch to PNG ASAP though).
 *
 * @author Maes
 */
class pcx_t : IWritableDoomObject {
    //
    // SCREEN SHOTS
    //
    // char -> byte Bytes.
    /** manufacturer byte, must be 10 decimal  */
    var manufacturer: Byte = 0

    /** PCX version number  */
    var version: Byte = 0

    /** run length encoding byte, must be 1  */
    var encoding: Byte = 0

    /** number of bits per pixel per bit plane  */
    var bits_per_pixel: Byte = 0

    /** image limits in pixels: Xmin, Ymin, Xmax, Ymax  */
    var xmin = 0.toChar()
    var ymin = 0.toChar()
    var xmax = 0.toChar()
    var ymax = 0.toChar()

    /** horizontal dots per inch when printed (unreliable)  */
    var hres = 0.toChar()

    /** vertical dots per inch when printed (unreliable)  */
    var vres = 0.toChar()

    /** 16-color palette (16 RGB triples between 0-255)
     * UNUSED in Doom.  */
    var palette = ByteArray(48)

    /** reserved, must be zero  */
    var reserved: Byte = 0

    /** number of bit planes  */
    var color_planes: Byte = 0

    /** video memory bytes per image row  */
    var bytes_per_line = 0.toChar()

    /** 16-color palette interpretation (unreliable) 0=color/b&w 1=grayscale  */
    var palette_type = 0.toChar()

    // Seems off-spec. However it's left all zeroed out.
    var filler = ByteArray(58)

    //unsigned char	data;
    lateinit var data: ByteArray
    @Throws(IOException::class)
    override fun write(f: DataOutputStream) {
        // char -> byte Bytes.
        f.writeByte(manufacturer.toInt())
        f.writeByte(version.toInt())
        f.writeByte(encoding.toInt())
        f.writeByte(bits_per_pixel.toInt())

        // unsigned short -> char
        f.writeChar(Swap.SHORT(xmin).toInt())
        f.writeChar(Swap.SHORT(ymin).toInt())
        f.writeChar(Swap.SHORT(xmax).toInt())
        f.writeChar(Swap.SHORT(ymax).toInt())
        f.writeChar(Swap.SHORT(hres).toInt())
        f.writeChar(Swap.SHORT(vres).toInt())
        f.write(palette)
        f.writeByte(reserved.toInt())
        f.writeByte(color_planes.toInt())
        // unsigned short -> char
        f.writeChar(Swap.SHORT(bytes_per_line).toInt())
        f.writeChar(Swap.SHORT(palette_type).toInt())
        f.write(filler)
        //unsigned char	data;		// unbounded
        f.write(data)
    }
}