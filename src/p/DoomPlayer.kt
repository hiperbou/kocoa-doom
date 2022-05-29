package p

import m.fixed_t


interface DoomPlayer {
    fun AimLineAttack(
        t1: mobj_t?,
        angle: Int,
        distance: fixed_t?
    ): fixed_t?

    fun LineAttack(
        t1: mobj_t?,
        angle: Int,
        distance: fixed_t?,
        slope: fixed_t?,
        damage: Int
    )

    fun RadiusAttack(
        spot: mobj_t?,
        source: mobj_t?,
        damage: Int
    )

    fun TouchSpecialThing(
        special: mobj_t?,
        toucher: mobj_t?
    )

    fun DamageMobj(
        target: mobj_t?,
        inflictor: mobj_t?,
        source: mobj_t?,
        damage: Int
    )
}