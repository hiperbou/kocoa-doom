package m


import doom.DoomMain
import doom.event_t

/** A dummy menu, useful for testers that do need a defined
 * menu object.
 *
 * @author Maes
 */
class DummyMenu<T, V>(DOOM: DoomMain<T, V>) : AbstractDoomMenu<T, V>(DOOM!!) {
    override fun Responder(ev: event_t): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun Ticker() {
        // TODO Auto-generated method stub
    }

    override fun Drawer() {
        // TODO Auto-generated method stub
    }

    override fun Init() {
        // TODO Auto-generated method stub
    }

    override fun StartControlPanel() {
        // TODO Auto-generated method stub
    }

    override var showMessages: Boolean
        get() = false
        set(value) {}

    override var screenBlocks: Int
        get() = 0
        set(value) {}

    override val detailLevel: Int
        get() = 0

    override fun ClearMenus() {
        // TODO Auto-generated method stub
    }
}