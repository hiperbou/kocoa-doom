package automap

class fline_t {
    /*
     * public fline_t(){
        a=new fpoint_t();
        b=new fpoint_t();
    }
    
    public fline_t(fpoint_t a, fpoint_t b){
        this.a=a;
        this.b=b;
    }
*/
    constructor(ax: Int, ay: Int, bx: Int, by: Int) {
        this.ay = ay
        this.ax = ax
        this.by = by
        this.bx = bx
    }

    constructor() {
        // TODO Auto-generated constructor stub
    }

    var ax = 0
    var ay = 0
    var bx = 0
    var by = 0 /*
    public fpoint_t a, b;

    public void reset() {
        this.a.x=0;
        this.a.y=0;
        this.b.x=0;
        this.b.y=0;
        
    }*/
}