package p


import m.*
import m.fixed_t.Companion.FRACBITS
import p.divline_t
import rr.line_t
import utils.C2JUtils

//
// P_MAPUTL
//
class divline_t {
    /** fixed_t  */
    var x = 0
    var y = 0
    var dx = 0
    var dy = 0

    /**
     * P_PointOnDivlineSide
     * Returns 0 or 1. (false or true)
     * @param x fixed
     * @param y fixed
     * @param divline_t
     */
    fun PointOnDivlineSide(
        x: Int,
        y: Int
    ): Boolean {


        // Using Killough's version.
        var x = x
        var y = y
        return if (dx == 0) if (x <= this.x) dy > 0 else dy < 0 else if (dy == 0) if (y <= this.y) dx < 0 else dx > 0 else if (dy xor dx xor this.x.let { x -= it; x } xor this.y.let { y -= it; y } < 0) dy xor x < 0 else fixed_t.FixedMul(
            y shr 8,
            dx shr 8
        ) >= fixed_t.FixedMul(
            dy shr 8, x shr 8
        )
        /*
    	    int PUREFUNC P_PointOnDivlineSide(fixed_t x, fixed_t y, const divline_t *line)
    	    {
    	      return
    	        !line->dx ? x <= line->x ? line->dy > 0 : line->dy < 0 :
    	        !line->dy ? y <= line->y ? line->dx < 0 : line->dx > 0 :
    	        (line->dy^line->dx^(x -= line->x)^(y -= line->y)) < 0 ? (line->dy^x) < 0 :
    	        FixedMul(y>>8, line->dx>>8) >= FixedMul(line->dy>>8, x>>8);
    	    }*/

        /*
      int dx;
      int dy;
      int left;
      int right;
      
      if (this.dx==0)
      {
      if (x <= this.x)
          return this.dy > 0;
      
      return this.dy < 0;
      }
      if (this.dy==0)
      {
      if (y <= this.y)
          return this.dx < 0;

      return this.dx > 0;
      }
      
      dx = (x - this.x);
      dy = (y - this.y);
      
      // try to quickly decide by looking at sign bits
      if ( ((this.dy ^ this.dx ^ dx ^ dy)&0x80000000) !=0)
      {
      if (((this.dy ^ dx) & 0x80000000) !=0)
          return true;       // (left is negative)
      return false;
      }
      
      left = FixedMul ( this.dy>>8, dx>>8 );
      right = FixedMul ( dy>>8 , this.dx>>8 );
      
      if (right < left)
      return false;       // front side
      return true;           // back side
      */
    }

    //
    //P_MakeDivline
    //
    fun MakeDivline(li: line_t) {
        x = li.v1x
        y = li.v1y
        dx = li.dx
        dy = li.dy
    }

    constructor(li: line_t) {
        x = li.v1x
        y = li.v1y
        dx = li.dx
        dy = li.dy
    }

    constructor() {
        // TODO Auto-generated constructor stub
    }

    /**
     * P_DivlineSide
     * Returns side 0 (front), 1 (back), or 2 (on).
     */
    fun DivlineSide(
        x: Int,
        y: Int
    ): Int {
        var left: Int
        var right: Int
        // Boom-style code. Da fack.
        // [Maes:] it is MUCH more corrent than the linuxdoom one, for whatever reason.
        return if (dx == 0) if (x == this.x) 2 else if (x <= this.x) C2JUtils.eval(dy > 0) else C2JUtils.eval(dy < 0) else if (dy == 0) if ((if (divline_t.olddemo) x else y) == this.y) 2 else if (y <= this.y) C2JUtils.eval(
            dx < 0
        ) else C2JUtils.eval(dx > 0) else if (dy == 0) if (y == this.y) 2 else if (y <= this.y) C2JUtils.eval(dx < 0) else C2JUtils.eval(
            dx > 0
        ) else if ((y - this.y shr FRACBITS) * (dx shr FRACBITS).also {
                right = it
            } < ((x - this.x shr FRACBITS) * (dy shr FRACBITS)).also {
                left = it
            }) 0 else if (right == left) 2 else 1

        /*  
 	    
 	    int	left,right,dx,dy;

 	    if (this.dx==0)
 	    {
 	    if (x==this.x)
 	        return 2;
 	    
 	    if (x <= this.x)
 	        return eval(this.dy > 0);

 	    return eval(this.y < 0);
 	    }
 	    
 	    if (this.dy==0)
 	    {
 	    if (x==this.y)
 	        return 2;

 	    if (y <= this.y)
 	        return eval(this.dx < 0);

 	    return eval(this.dx > 0);
 	    }
 	    
 	    dx = (x - this.x);
 	    dy = (y - this.y);

 	    left =  (this.dy>>FRACBITS) * (dx>>FRACBITS);
 	    right = (dy>>FRACBITS) * (this.dx>>FRACBITS);
 	    
 	    if (right < left)
 	    return 0;   // front side
 	    
 	    if (left == right)
 	    return 2;
 	    return 1;       // back side
 	    */
    }

    companion object {
        private const val olddemo = true
    }
}