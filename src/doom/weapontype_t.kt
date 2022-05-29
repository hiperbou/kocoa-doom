package doom


/** The defined weapons,
 * including a marker indicating
 * user has not changed weapon.
 */
enum class weapontype_t {
    wp_fist, wp_pistol, wp_shotgun, wp_chaingun, wp_missile, wp_plasma, wp_bfg, wp_chainsaw, wp_supershotgun, NUMWEAPONS,  // No pending weapon change.
    wp_nochange;

    override fun toString(): String {
        return name
    }
}