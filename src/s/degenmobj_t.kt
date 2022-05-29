package s

class degenmobj_t : ISoundOrigin {
    private val x: Int
    private val y: Int
    private val z: Int

    constructor(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
        z = 0
    }

    override fun getX(): Int {
        return x
    }

    override fun getY(): Int {
        return y
    }

    override fun getZ(): Int {
        return z
    }
}