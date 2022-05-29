package pooling

class RoguePatchMap : GenericIntMap<Array<ByteArray?>?>() {
    init {
        patches = arrayOfNulls(DEFAULT_CAPACITY)
    }
}