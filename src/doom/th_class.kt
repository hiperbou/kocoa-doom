package doom

/** killough 8/29/98: threads of thinkers, for more efficient searches
 * cph 2002/01/13: for consistency with the main thinker list, keep objects
 * pending deletion on a class list too
 */
enum class th_class {
    th_delete, th_misc, th_friends, th_enemies, th_all;

    companion object {
        val NUMTHCLASS = th_class.values().size
    }
}