/*
 * Copyright (C) 2017 Good Sign
 * Copyright (C) 2022 hiperbou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package awt


import awt.EventBase.*
import doom.event_t
import doom.event_t.mouseevent_t
import doom.evtype_t
import g.Signals
import g.Signals.ScanCode
import mochadoom.Loggers
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level

/**
 * Observer for AWTEvents. The description would be short in contrary to the description
 * of EventHandler Enum. This class uses rules in Handler extends Enum<Handler> & EventBase<Handler>
 * to react on AWTEvent events given to him by some listener (or by fake, don't matter) and feeds them
 * to someone who needs them (DOOM's internal event handling system)
 *
 * Also, you may use any Enum & EventBase dictionary, not just EventHandler.
 * It may be useful if you design a game with several modes or with several systems, or something,
 * and you need one part to react in one way, another part in another.
 *
 * @author Good Sign
</Handler></Handler> */
class EventObserver<Handler>(
    handlerClass: Class<Handler>,
    component: Component,
    doomEventConsumer: Consumer<in event_t>
) where Handler : Enum<Handler>, Handler : EventBase<Handler> {
    /**
     * NASTY hack to hide the cursor.
     *
     * Create a 'hidden' cursor by using a transparent image
     * ...return the invisible cursor
     * @author vekltron
     */
    private fun createHiddenCursor(): Cursor {
        val tk = Toolkit.getDefaultToolkit()
        val dim = tk.getBestCursorSize(2, 2)
        if (dim.width == 0 || dim.height == 0) {
            return initialCursor
        }
        val transparent = BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB)
        return tk.createCustomCursor(transparent, Point(1, 1), "HiddenCursor")
    }

    /**
     * This event here is used as a static scratch copy. When sending out
     * messages, its contents are to be actually copied (struct-like).
     * This avoids the continuous object creation/destruction overhead,
     * And it also allows creating "sticky" status.
     *
     * Also, as I've made event_t.mouseevent_t fields volatile, there is
     * no more need to synchronize on it in the multithread event listening solutions.
     */
    val mouseEvent = mouseevent_t(evtype_t.ev_mouse, 0, 0, 0)

    /**
     * Shared state of keys
     */
    protected val keyStateHolder: EventBase.KeyStateHolder<Handler>

    /**
     * Component (Canvas or JPanel, for exaple) to deal with
     */
    val component: Component

    /**
     * This one will be given all event_t's we produce there
     */
    private val doomEventConsumer: Consumer<in event_t>

    /**
     * Will be used to find Handler by AWTEvent's id
     */
    private val eventSortedHandlers: Array<Handler>

    /**
     * Shared state of actions
     */
    private val actionStateHolder: EventBase.ActionStateHolder<Handler>

    /**
     * Presumably a system Cursor, that is to be used on cursor restore.
     */
    private val initialCursor: Cursor

    /**
     * Ivisible cursor on the systems who support changing cursors
     */
    private val hiddenCursor: Cursor

    /**
     * To construct the Observer you only need to provide it with the class of Enum used
     * to contain dictinary, the Component it will be working on and acceptor of event_t's
     */
    init {
        actionStateHolder = ActionStateHolder(handlerClass, this)
        eventSortedHandlers = EventBase.sortHandlers<Handler>(handlerClass.enumConstants)
        this.doomEventConsumer = doomEventConsumer
        this.component = component
        initialCursor = component.cursor
        hiddenCursor = createHiddenCursor()
        keyStateHolder = EventBase.KeyStateHolder()
    }

    fun addInterest(interest: KeyStateInterest<Handler>): EventObserver<Handler> {
        keyStateHolder.addInterest(interest)
        return this
    }

    fun removeInterest(interest: KeyStateInterest<Handler>): EventObserver<Handler> {
        keyStateHolder.removeInterest(interest)
        return this
    }

    /**
     * This method is designed to acquire events from some kind of listener.
     * EventHandler class do not provide listener itself - but should work with any.
     */
    fun observe(ev: AWTEvent) {
        val maybe: Optional<Handler> = EventBase.findById<Handler>(eventSortedHandlers, ev.id)
        var handlerr: Handler? = null
        if (!maybe.isPresent || !actionStateHolder.hasActionsEnabled(
                maybe.get().also { handlerr = it },
                EventBase.ActionMode.PERFORM
            )
        ) {
            return
        }
        if (handlerr === EventHandler.WINDOW_ACTIVATE) {
            val u = 8
        }

        val handler = handlerr!!
        // In case of debug. If level > FINE (most of cases) it will not affect anything
        Loggers.LogEvent(EventObserver.LOGGER, actionStateHolder, handler, ev)
        actionStateHolder.run(handler, EventBase.ActionMode.PERFORM, ev)
        actionStateHolder.adjustments(handler).forEach { (relation: EventBase.RelationType, affected: MutableSet<Handler>) ->
            when (relation.affection) {
                EventBase.RelationAffection.ENABLES -> {
                    affected.forEach(Consumer { h: Handler ->
                        actionStateHolder.enableAction(
                            h,
                            relation.affectedMode
                        )
                    })
                    return@forEach
                }
                RelationAffection.DISABLES -> affected.forEach(Consumer { h: Handler ->
                    actionStateHolder.disableAction(
                        h,
                        relation.affectedMode
                    )
                })
                else -> {}
            }
        }
        actionStateHolder.cooperations(handler, EventBase.RelationType.CAUSE)
            .forEach(Consumer { h: Handler -> actionStateHolder.run(h, ActionMode.CAUSE, ev) })
        actionStateHolder.cooperations(handler, EventBase.RelationType.REVERT)
            .forEach(Consumer { h: Handler -> actionStateHolder.run(h, ActionMode.REVERT, ev) })
    }

    /**
     * The way to supply underlying engine with event generated by this class
     * This function is the last barrier between user input and DOOM's internal event hell.
     * So there are all user key interests checked.
     */
    fun feed(ev: event_t) {
        if (!ev.ifKey { sc: ScanCode? -> keyStateHolder.notifyKeyChange(this, sc, ev.isType(evtype_t.ev_keydown)) }) {
            doomEventConsumer.accept(ev)
        }
    }

    /**
     * Restore default system cursor over the window
     */
    fun restoreCursor(event: AWTEvent?) {
        component.cursor = initialCursor
    }

    /**
     * Hide cursor
     */
    protected fun modifyCursor(event: AWTEvent?) {
        component.inputContext.selectInputMethod(Locale.getDefault())
        component.cursor = hiddenCursor
    }

    /**
     * Move the cursor into the centre of the window. The event_t.mouseevent_t implementation
     * would set robotMove flag for us to be able to distinguish the Robot-caused moves
     * and not react on them (thus preventing look to be stuck or behave weird)
     * - Good Sign 2017/04/24
     */
    fun centreCursor(event: AWTEvent?) {
        val centreX = component.width shr 1
        val centreY = component.height shr 1
        if (component.isShowing) {
            MOUSE_ROBOT.ifPresent { rob ->
                mouseEvent.resetIn(
                    rob,
                    component.locationOnScreen,
                    centreX,
                    centreY
                )
            }
        }
        modifyCursor(event)
    }

    /**
     * Forcibly clear key events in the underlying engine
     */
    fun cancelKeys(ev: AWTEvent?) {
        feed(event_t.CANCEL_KEYS)
        keyStateHolder.removeAllKeys()
    }

    /**
     * Forcibly clear mouse events in the underlying engine, discard cursor modifications
     */
    fun cancelMouse(ev: AWTEvent?) {
        feed(event_t.CANCEL_MOUSE)
    }

    /**
     * Send key releases to underlying engine
     */
    fun sendKeyUps(ev: AWTEvent) {
        feed(Signals.getScanCode(ev as KeyEvent).doomEventUp)
        discardInputEvent(ev)
    }

    /**
     * Send key presses to underlying engine
     */
    fun sendKeyDowns(ev: AWTEvent) {
        feed(Signals.getScanCode(ev as KeyEvent).doomEventDown)
        discardInputEvent(ev)
    }

    /**
     * Consumes InputEvents so they will not pass further
     */
    protected fun discardInputEvent(ev: AWTEvent) {
        try {
            (ev as InputEvent).consume()
        } catch (ex: ClassCastException) {
            EventObserver.LOGGER.log(Level.SEVERE, null, ex)
        }
    }

    fun enableAction(h: Handler, mode: ActionMode) {
        actionStateHolder.enableAction(h, mode)
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE,
                Supplier { String.format("ENABLE ACTION: %s [%s]", h, mode) })
        }
    }

    fun disableAction(h: Handler, mode: ActionMode) {
        actionStateHolder.disableAction(h, mode)
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE
            ) { String.format("DISABLE ACTION: %s [%s]", h, mode) }
        }
    }

    @SafeVarargs
    fun mapRelation(h: Handler, type: EventBase.RelationType, vararg targets: Handler) {
        if (type.affection == EventBase.RelationAffection.COOPERATES) {
            actionStateHolder.mapCooperation(h, type, *targets)
        } else {
            actionStateHolder.mapAdjustment(h, type, *targets)
        }
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE
            ) { String.format("RELATION MAPPING: %s -> [%s] {%s}", h, type, Arrays.toString(targets)) }
        }
    }

    @SafeVarargs
    fun unmapRelation(h: Handler, type: EventBase.RelationType, vararg targets: Handler) {
        if (type.affection == EventBase.RelationAffection.COOPERATES) {
            actionStateHolder.unmapCooperation(h, type, *targets)
        } else {
            actionStateHolder.unmapAdjustment(h, type, *targets)
        }
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE
            ) { String.format("RELATION UNMAP: %s -> [%s] {%s}", h, type, Arrays.toString(targets)) }
        }
    }

    @SafeVarargs
    protected fun restoreRelation(h: Handler, type: EventBase.RelationType, vararg targets: Handler) {
        if (type.affection == RelationAffection.COOPERATES) {
            actionStateHolder.restoreCooperation(h, type, *targets)
        } else {
            actionStateHolder.restoreAdjustment(h, type, *targets)
        }
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE
            ) { String.format("RELATION RESTORE: %s -> [%s] {%s}", h, type, Arrays.toString(targets)) }
        }
    }

    fun mapAction(h: Handler, mode: ActionMode, remap: EventAction<Handler>) {
        actionStateHolder.mapAction(h, mode, remap)
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE
            ) { String.format("ACTION MAPPING (MAP): %s [%s]", h, mode) }
        }
    }

    protected fun remapAction(h: Handler, mode: ActionMode, remap: EventAction<Handler>) {
        actionStateHolder.remapAction(h, mode, remap)
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE
            ) { String.format("ACTION MAPPING (REMAP): %s [%s]", h, mode) }
        }
    }

    fun unmapAction(h: Handler, mode: ActionMode) {
        actionStateHolder.unmapAction(h, mode)
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(Level.FINE) { String.format("UNMAP ACTION: %s [%s]", h, mode) }
        }
    }

    protected fun restoreAction(h: Handler, mode: ActionMode) {
        actionStateHolder.restoreAction(h, mode)
        if (EventObserver.LOGGER.isLoggable(Level.FINE)) {
            EventObserver.LOGGER.log(
                Level.FINE
            ) { String.format("RESTORE ACTION: %s [%s]", h, mode) }
        }
    }

    companion object {
        val MOUSE_ROBOT: Optional<Robot> = EventObserver.createRobot()
        private val LOGGER = Loggers.getLogger(EventObserver::class.java.name)

        /**
         * The Robot does not necessary gets created. When not, it throws an exception.
         * We ignore that exception, and set Robot to null. So, any call to Robot
         * must first check against null. So I've just made it Optional<Robot> - for no headache.
         * - Good Sign 2017/04/24
         *
         * In my opinion, its better turn off mouse at all, then without Robot.
         * But the support to run without it, though untested, must be present.
         * - Good Sign 2017/04/22
         *
         * Create AWT Robot for forcing mouse
         *
         * @author Good Sign
         * @author vekltron
        </Robot> */
        private fun createRobot(): Optional<Robot> {
            try {
                return Optional.of(Robot())
            } catch (e: AWTException) {
                Loggers.getLogger(EventObserver::class.java.name)
                    .log(Level.SEVERE, "AWT Robot could not be created, mouse input focus will be loose!", e)
            }
            return Optional.empty()
        }
    }
}