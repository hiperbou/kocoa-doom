package defines


//Ammunition types defined.
enum class ammotype_t {
    am_clip,  // Pistol / chaingun ammo.
    am_shell,  // Shotgun / double barreled shotgun.
    am_cell,  // Plasma rifle, BFG.
    am_misl,  // Missile launcher.
    NUMAMMO,
    am_noammo // Unlimited for chainsaw / fist.
}