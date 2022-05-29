package p


import p.mobj_t

interface ISightChecker {
    fun setZStartTopBOttom(zstart: Int, top: Int, bottom: Int)
    fun setSTrace(t1: mobj_t?, t2: mobj_t?)
    fun CrossBSPNode(bspnum: Int): Boolean
}