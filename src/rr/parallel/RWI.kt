package rr.parallel


import rr.drawfuns.ColVars

interface RWI<T, V> {
    interface Init<T, V> {
        fun InitRWIExecutors(num: Int, RWI: Array<ColVars<T, V>?>?): Array<RenderWallExecutor<T, V>?>?
    }

    interface Get<T, V> {
        fun getRWI(): Array<ColVars<T, V>>
        fun setExecutors(RWIExec: Array<RenderWallExecutor<T, V>>)
    }
}