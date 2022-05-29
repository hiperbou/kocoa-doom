package automap


/** used only in automap  */
class mline_t {
    var ax: Int
    var ay: Int
    var bx: Int
    var by: Int

    @JvmOverloads
    constructor(ax: Int = 0, ay: Int = 0, bx: Int = 0, by: Int = 0) {
        this.ax = ax
        this.ay = ay
        this.bx = bx
        this.by = by
    }

    constructor(ax: Double, ay: Double, bx: Double, by: Double) {
        this.ax = ax.toInt()
        this.ay = ay.toInt()
        this.bx = bx.toInt()
        this.by = by.toInt()
    } /*
    public mline_t(mpoint_t a, mpoint_t b) {
        this.a = a;
        this.b = b;
    }

    public mline_t(int ax,int ay,int bx,int by) {
        this.a = new mpoint_t(ax,ay);
        this.b = new mpoint_t(bx,by);
    }
        
    public mline_t(double ax,double ay,double bx,double by) {
        this.a = new mpoint_t(ax,ay);
        this.b = new mpoint_t(bx,by);
    }
    
    public mpoint_t a, b;
    public int ax;
    
    public String toString(){
        return a.toString()+" - "+ b.toString();
    }
    */
}