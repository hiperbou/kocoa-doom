package doom


/** Meant to provide a more lightweight alternative to Java's serialization model,
 * specifically for the purpose of sending
 * Objects implementing this can return references to one same byte array, with minimal
 * overhead. Since it's for send-only purposes, it won't matter if it's  modified.
 * *
 * But don't use it in lieu of CacheableDoomObject!
 *
 * @author admin
 */
interface IDatagramSerializable {
    /** Packs object into a byte array suitable to send over
     * datagram networks. Typically, objects cache this array
     * for later use, and is availabe through cached()
     *
     * @return
     */
    fun pack(): ByteArray

    /** Packs object into a byte array suitable to send over
     * datagram networks. The array is supplied externally
     * (good for daisy-chaining stuff into a single packet).
     *
     * @return
     */
    fun pack(buf: ByteArray, offset: Int)

    /** Deserializes an object from a given byte buffer.
     * Only the first (sizeof) bytes will be used, dependant
     * on each object's implementation. Will NOT also copy
     * the byte[] caches.
     */
    fun unpack(buf: ByteArray)

    /** Deserializes an object from a given byte buffer.
     * Only the first (sizeof) bytes will be used, starting
     * from a specified offset, dependant on each object's
     * implementation.
     */
    fun unpack(buf: ByteArray, offset: Int)

    /** Only use this if you are 100% sure that the object's content
     * won't have changed since the last call of pack().
     *
     * @return Should return the underlying byte[] array directly.
     */
    fun cached(): ByteArray
}