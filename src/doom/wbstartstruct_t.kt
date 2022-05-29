package doom


import data.Limits
import utils.GenericCopy

class wbstartstruct_t : Cloneable {
    var epsd // episode # (0-2)
            = 0

    // if true, splash the secret level
    var didsecret = false

    // previous and next levels, origin 0
    var last = 0
    var next = 0
    var maxkills = 0
    var maxitems = 0
    var maxsecret = 0
    var maxfrags = 0

    /** the par time  */
    var partime = 0

    /** index of this player in game  */
    var pnum = 0

    /** meant to be treated as a "struct", therefore assignments should be deep copies  */
    var plyr: Array<wbplayerstruct_t>

    init {
        plyr = GenericCopy.malloc({ wbplayerstruct_t() }, Limits.MAXPLAYERS)
    }

    public override fun clone(): wbstartstruct_t {
        var cl: wbstartstruct_t? = null
        try {
            cl = super.clone() as wbstartstruct_t
        } catch (e: CloneNotSupportedException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        /*cl.epsd=this.epsd;
            cl.didsecret=this.didsecret;
            cl.last=this.last;
            cl.next=this.next;
            cl.maxfrags=this.maxfrags;
            cl.maxitems=this.maxitems;
            cl.maxsecret=this.maxsecret;
            cl.maxkills=this.maxkills;
            cl.partime=this.partime;
            cl.pnum=this.pnum;*/for (i in cl!!.plyr.indices) {
            cl.plyr[i] = plyr[i].clone()
        }
        //cl.plyr=this.plyr.clone();
        return cl
    }
}