package doom


// Player states.
//
enum class playerstate_t {
    // Playing or camping.
    PST_LIVE,  // Dead on the ground, view follows killer.
    PST_DEAD,  // Ready to restart/respawn???
    PST_REBORN
}