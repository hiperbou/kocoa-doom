package rr


interface IDetailAware {
    fun setDetail(detailshift: Int)

    companion object {
        const val HIGH_DETAIL = 0
        const val LOW_DETAIL = 1
    }
}