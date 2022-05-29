package m


/** General form for a classic, Doom-style menu with a bunch of
 * items and a drawing routine (menu_t's don't have action callbacks
 * proper, though).
 *
 * @author Maes
 */
class menu_t(
    /** # of menu items  */
    var numitems: Int,
    /**  previous menu  */
    var prevMenu: menu_t?,
    /** menu items  */
    var menuitems: Array<menuitem_t>,
    /** draw routine  */
    var routine: DrawRoutine,
    /**  x,y of menu  */
    var x: Int, var y: Int,
    /** last item user was on in menu  */
    var lastOn: Int
)