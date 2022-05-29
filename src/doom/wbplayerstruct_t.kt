package doom

//
// INTERMISSION
// Structure passed e.g. to WI_Start(wb)
//
class wbplayerstruct_t : Cloneable {
    var `in` // whether the player is in game
            = false

    /** Player stats, kills, collected items etc.  */
    var skills = 0
    var sitems = 0
    var ssecret = 0
    var stime = 0
    var frags: IntArray

    /** current score on entry, modified on return  */
    var score = 0

    init {
        frags = IntArray(4)
    }

    public override fun clone(): wbplayerstruct_t {
        var r: wbplayerstruct_t? = null
        try {
            r = super.clone() as wbplayerstruct_t
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }
        /*r.in=this.in;
         r.skills=this.skills;
         r.sitems=this.sitems;
         r.ssecret=this.ssecret;
         r.stime=this.stime; */System.arraycopy(frags, 0, r!!.frags, 0, r.frags.size)
        // r.score=this.score;
        return r
    }
}