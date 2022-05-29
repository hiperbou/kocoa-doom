package p


/** Animating textures and planes
 * There is another anim_t used in wi_stuff, unrelated.
 *
 * @author admin
 */
class anim_t {
    constructor() {}
    constructor(
        istexture: Boolean, picnum: Int, basepic: Int, numpics: Int,
        speed: Int
    ) : super() {
        this.istexture = istexture
        this.picnum = picnum
        this.basepic = basepic
        this.numpics = numpics
        this.speed = speed
    }

    var istexture = false
    var picnum = 0
    var basepic = 0
    var numpics = 0
    var speed = 0
}