package p


class slidename_t {
    constructor() {}
    constructor(
        frontFrame1: String?, frontFrame2: String?,
        frontFrame3: String?, frontFrame4: String?, backFrame1: String?,
        backFrame2: String?, backFrame3: String?, backFrame4: String?
    ) {
        this.frontFrame1 = frontFrame1
        this.frontFrame2 = frontFrame2
        this.frontFrame3 = frontFrame3
        this.frontFrame4 = frontFrame4
        this.backFrame1 = backFrame1
        this.backFrame2 = backFrame2
        this.backFrame3 = backFrame3
        this.backFrame4 = backFrame4
    }

    var frontFrame1: String? = null
    var frontFrame2: String? = null
    var frontFrame3: String? = null
    var frontFrame4: String? = null
    var backFrame1: String? = null
    var backFrame2: String? = null
    var backFrame3: String? = null
    var backFrame4: String? = null
}