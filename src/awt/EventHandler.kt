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
import g.Signals.ScanCode
import g.Signals.getScanCode
import java.awt.AWTEvent
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.collections.HashSet


/**
 * This class is catching events thrown at him by someone, and sends them to underlying DOOM engine.
 * As a way to preserve vanilla until full understand the code, everything inside of underlying engine
 * is still considered black box and changes to it are minimal.
 * But I've tried to make high-level API on top level effective.
 *
 * For example, we do not need to create some MochaDoomInputEvent for unique combination of AWTEvent and
 * its type - it can be easily switched by ID value from AWTEvent's ow information method. Also, we can
 * certainly know which ScanCodes we will get and what are their minimal and max values, because
 * of using Enum for them (my favorite type of data structure, huh!)
 * And if we know that ScanCodes can only be something very limited, and every KeyEvent (an AWTEvent for keys)
 * will be translated into one of them, we only have to pre-create two copies of DOOM's event structure
 * for each entry of ScanCode Enum: one for press state, one for release.
 *
 * Note: SysRq / Print Screen key only sends release state, so it have to send press to underlying engine
 * on release or it will be ignored.
 * - Good Sign 2017/04/21
 *
 * The secondary purpose of this class is to automatically handle relations between different
 * event handlers. Everything like "when window is not in focus, don't process keys".
 * - Good Sign 2017/04/22
 *
 * New way of on-event actions and definitions:
 * Enums and lambdas are magic combination. Here you cave a static pre-defined constant pool of events and
 * abstract reactions on them that can work with many copies of Observer if you want, and with different ways
 * of listening for events.
 *
 * How to use:
 * define enum constant with arbitrary name (i.e. JOYSTICK_SOEMTHING)
 * then write its arguments:
 * first argument is AWTEvent id it will react to,
 * second argument is lambda accepting mapper. map function on mapper maps ActionMode to EventAction
 * ActionMode.PERFORM it what you want to do when the event occured.
 * Function or lambda you map through mapper.map method accepts Observer object and AWTEvent
 * (you can cast your AWTEvent assuming the proper id will correspond to the proper AWTEvent)
 * ActionMode.REVERT is what you want to do when some event negates effect of this event
 * (i.e. when the user switched to another application, clear joystick pressed button states)
 * - Good Sign 2017/04/24
 *
 * @author Good Sign
 */

enum class EventHandler : EventBase<EventHandler> {
    KEY_PRESS(KeyEvent.KEY_PRESSED, { mapper: ActionMapper<EventHandler> ->
        mapper.map(
            ActionMode.REVERT
        ) { obj, ev -> obj!!.cancelKeys(ev) }
        mapper.map(ActionMode.PERFORM) { obj, ev ->
            obj!!.sendKeyDowns(ev)
        }
        mapper.map(
            ActionMode.DEPEND
        ) { observer, event ->
            // Add keyDown for Print Screen because he doesn't send one
            if (getScanCode((event as KeyEvent?)!!) === ScanCode.SC_PRTSCRN) {
                observer!!.feed(ScanCode.SC_PRTSCRN.doomEventDown)
            }
        }
    }, ActionMode.REVERT, ActionMode.PERFORM, ActionMode.DEPEND),
    KEY_RELEASE(KeyEvent.KEY_RELEASED,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(ActionMode.PERFORM) { obj, ev -> obj!!.sendKeyUps(ev) }
        }, ActionMode.PERFORM /*, ActionMode.DEPEND*/
    ),
    KEY_TYPE(KeyEvent.KEY_TYPED,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(ActionMode.PERFORM) { obj, ev -> obj!!.sendKeyUps(ev) }
        }, ActionMode.PERFORM
    ),
    MOUSE_PRESS(MouseEvent.MOUSE_PRESSED,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(ActionMode.REVERT) { obj: EventObserver<*>?, ev: AWTEvent? -> obj!!.cancelMouse(ev) }
            mapper.map(
                ActionMode.PERFORM
            ) { observer, ev ->
                observer!!.mouseEvent.buttonOn((ev as MouseEvent?)!!)
                observer.mouseEvent.y = 0
                observer.mouseEvent.x = observer.mouseEvent.y
                if (observer.mouseEvent.processed) {
                    observer.mouseEvent.resetNotify()
                    observer.feed(observer.mouseEvent)
                }
            }
        }, ActionMode.REVERT, ActionMode.PERFORM
    ),
    MOUSE_RELEASE(MouseEvent.MOUSE_RELEASED,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(
                ActionMode.PERFORM
            ) { observer, ev ->
                observer!!.mouseEvent.buttonOff((ev as MouseEvent?)!!)
                observer.mouseEvent.y = 0
                observer.mouseEvent.x = observer.mouseEvent.y
                if (observer.mouseEvent.processed) {
                    observer.mouseEvent.resetNotify()
                    observer.feed(observer.mouseEvent)
                }
            }
        }, ActionMode.PERFORM
    ),
    MOUSE_CLICK(MouseEvent.MOUSE_CLICKED,
        { mapper: ActionMapper<EventHandler> ->
            // Set input method and mouse cursor, move cursor to the centre
            mapper.map(ActionMode.PERFORM) { obj: EventObserver<*>?, event: AWTEvent? ->
                menuCaptureChanges(obj as EventObserver<EventHandler> , true)
            }
        }),
    MOUSE_MOVE(MouseEvent.MOUSE_MOVED,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(
                ActionMode.PERFORM,
                mouseMoveAction(false)
            )
        }, ActionMode.PERFORM
    ),
    MOUSE_DRAG(MouseEvent.MOUSE_DRAGGED,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(
                ActionMode.PERFORM,
                mouseMoveAction(true)
            )
        }, ActionMode.PERFORM
    ),
    WINDOW_ACTIVATE(
        WindowEvent.WINDOW_ACTIVATED,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(ActionMode.PERFORM) { obj: EventObserver<*>?, event: AWTEvent? ->
                obj!!.cancelKeys(event)
                if (mouseCaptured) obj.centreCursor(event)

            }
        },
        ActionMode.PERFORM,
        ActionMode.CAUSE
    ),
    WINDOW_DEICONIFY(
        WindowEvent.WINDOW_DEICONIFIED,
        ActionMode.PERFORM
    ),
    COMPONENT_RESIZE(ComponentEvent.COMPONENT_RESIZED, ActionMode.PERFORM),
    MOUSE_ENTER(
        MouseEvent.MOUSE_ENTERED,
        { mapper: ActionMapper<EventHandler> ->
            // Set input method and mouse cursor, move cursor to the centre
            /*mapper.map(ActionMode.PERFORM) { obj: EventObserver<*>?, event: AWTEvent? ->
                obj!!.centreCursor(
                    event
                )
            }*/
        }),
    WINDOW_OPEN(WindowEvent.WINDOW_OPENED,
        { mapper: ActionMapper<EventHandler> ->
            // Set input method and mouse cursor
            mapper.map(ActionMode.PERFORM) { obj: EventObserver<*>?, event: AWTEvent? ->
                menuCaptureChanges(obj as EventObserver<EventHandler>, false)
            }
        }, ActionMode.PERFORM
    ),
    WINDOW_GAIN_FOCUS(WindowEvent.WINDOW_GAINED_FOCUS,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(
                ActionMode.PERFORM
            ) { obj: EventObserver<*>?, event: AWTEvent? ->
                //wprintln("GAIN FOCUS!")
            }
        }, ActionMode.PERFORM
    ),
    WINDOW_LOSE_FOCUS(WindowEvent.WINDOW_LOST_FOCUS,
        { mapper: ActionMapper<EventHandler> ->
            mapper.map(
                ActionMode.PERFORM
            ) { obj: EventObserver<*>?, event: AWTEvent? ->
                obj!!.restoreCursor(event)
                obj.cancelKeys(event)
            }
        }, ActionMode.PERFORM
    ),
    COMPONENT_MOVE(ComponentEvent.COMPONENT_MOVED, ActionMode.PERFORM),
    MOUSE_EXIT(
        MouseEvent.MOUSE_EXITED, ActionMode.PERFORM
    );//,

    /**
     * We need to take charge of various scenarios such as what to do with mouse and keys when the
     * window lose focus, how to enter/return from alt-tab/full-screen switch, how to behave
     * when the use crucially drag our window over all the desktop...
     */
    /*
    RELATIONS(Consumer { relationMapper: RelationMapper<EventHandler> ->
        // Add keyDown for Print Screen because he doesn't send one
        relationMapper.map(RelationType.DEPEND, Relate(KEY_RELEASE, KEY_PRESS))
        /**
         * After the window is opened, it must disable its own event, but capture all the keyboard and mouse input
         */
        /**
         * After the window is opened, it must disable its own event, but capture all the keyboard and mouse input
         */
        relationMapper.map(
            RelationType.DISABLE,
            Relate(WINDOW_OPEN, WINDOW_OPEN)
        )
        relationMapper.map(
            RelationType.ENABLE,
            Relate(
                WINDOW_OPEN,
                WINDOW_LOSE_FOCUS,
                KEY_PRESS,
                KEY_RELEASE,
                KEY_TYPE,
                MOUSE_ENTER,
                MOUSE_MOVE,
                MOUSE_DRAG,
                MOUSE_PRESS,
                MOUSE_RELEASE
            )
        )
        /**
         * On any activation/reconfiguration/resize/restore-from-something, request focus in window
         */
        /**
         * On any activation/reconfiguration/resize/restore-from-something, request focus in window
         */
        relationMapper.map(
            RelationType.CAUSE,
            Relate(WINDOW_ACTIVATE, WINDOW_ACTIVATE)
        )
        relationMapper.map(
            RelationType.CAUSE,
            Relate(WINDOW_DEICONIFY, WINDOW_ACTIVATE)
        )
        relationMapper.map(
            RelationType.CAUSE,
            Relate(COMPONENT_RESIZE, WINDOW_ACTIVATE)
        )
        /**
         * This set of rules are for ultimately releasing any capture on mouse and keyboard,
         * and also releases all pressed keys and mouse buttons.
         *
         * Disables itself too, but enables event on the focus return.
         */
        /**
         * This set of rules are for ultimately releasing any capture on mouse and keyboard,
         * and also releases all pressed keys and mouse buttons.
         *
         * Disables itself too, but enables event on the focus return.
         */
        relationMapper.map(
            RelationType.REVERT,
            Relate(WINDOW_LOSE_FOCUS, KEY_PRESS, MOUSE_PRESS)
        )
        relationMapper.map(
            RelationType.DISABLE,
            Relate(
                WINDOW_LOSE_FOCUS,
                WINDOW_LOSE_FOCUS,
                KEY_PRESS,
                KEY_RELEASE,
                KEY_TYPE,
                MOUSE_MOVE,
                MOUSE_DRAG,
                MOUSE_PRESS,
                MOUSE_RELEASE,
                MOUSE_ENTER
            )
        )
        /**
         * The next set of rules is for active focus gain. It could be done in two ways:
         * natural, when window become visible topmost window with active borders,
         * and when you click with mouse into the unfocused window.
         *
         * For clicky way, it must cause window focus and immediate capture of the mouse.
         * Enables back losing focus, disables itself and natural focus gain.
         */
        /**
         * The next set of rules is for active focus gain. It could be done in two ways:
         * natural, when window become visible topmost window with active borders,
         * and when you click with mouse into the unfocused window.
         *
         * For clicky way, it must cause window focus and immediate capture of the mouse.
         * Enables back losing focus, disables itself and natural focus gain.
         */
        relationMapper.map(
            RelationType.ENABLE,
            Relate(WINDOW_LOSE_FOCUS, WINDOW_GAIN_FOCUS, MOUSE_CLICK)
        )
        relationMapper.map(
            RelationType.ENABLE,
            Relate(
                MOUSE_CLICK,
                WINDOW_LOSE_FOCUS,
                KEY_PRESS,
                KEY_RELEASE,
                KEY_TYPE,
                MOUSE_ENTER,
                MOUSE_MOVE,
                MOUSE_DRAG,
                MOUSE_PRESS,
                MOUSE_RELEASE
            )
        )
        relationMapper.map(
            RelationType.DISABLE,
            Relate(MOUSE_CLICK, WINDOW_GAIN_FOCUS, MOUSE_CLICK)
        )
        /**
         * For natural way, focus gain *must not* capture the mouse immediately, only after it enters the window.
         * Enables back losing focus, disables itself and clicky way of capture.
         */
        /**
         * For natural way, focus gain *must not* capture the mouse immediately, only after it enters the window.
         * Enables back losing focus, disables itself and clicky way of capture.
         */
        relationMapper.map(
            RelationType.ENABLE,
            Relate(
                WINDOW_GAIN_FOCUS,
                WINDOW_LOSE_FOCUS,
                KEY_PRESS,
                KEY_RELEASE,
                KEY_TYPE,
                MOUSE_ENTER
            )
        )
        relationMapper.map(
            RelationType.DISABLE,
            Relate(WINDOW_GAIN_FOCUS, WINDOW_GAIN_FOCUS)
        )
        /**
         * When the mouse returns to the window, it should be captured back, and the event disabled.
         */
        /**
         * When the mouse returns to the window, it should be captured back, and the event disabled.
         */
        relationMapper.map(
            RelationType.ENABLE,
            Relate(
                MOUSE_ENTER,
                MOUSE_MOVE,
                MOUSE_DRAG,
                MOUSE_PRESS,
                MOUSE_RELEASE
            )
        )
        relationMapper.map(
            RelationType.DISABLE,
            Relate(MOUSE_ENTER, MOUSE_ENTER)
        )
        /**
         * The last scenario is component move. Example of it - user drags the window by its head.
         * This way, first window is activated and gained focus, than component moved. Normally, the mouse would
         * go into the window position and MOUSE_ENTER will be processed. We do not need it. If the user drags window,
         * he then should manually click inside it to regain mouse capture - or alt-tab twice (regain window focus)
         */
        /**
         * The last scenario is component move. Example of it - user drags the window by its head.
         * This way, first window is activated and gained focus, than component moved. Normally, the mouse would
         * go into the window position and MOUSE_ENTER will be processed. We do not need it. If the user drags window,
         * he then should manually click inside it to regain mouse capture - or alt-tab twice (regain window focus)
         */
        relationMapper.map(
            RelationType.DISABLE,
            Relate(
                COMPONENT_MOVE,
                MOUSE_MOVE,
                MOUSE_DRAG,
                MOUSE_PRESS,
                MOUSE_RELEASE,
                MOUSE_ENTER
            )
        )
        relationMapper.map(
            RelationType.ENABLE,
            Relate(COMPONENT_MOVE, MOUSE_CLICK)
        )
    });
*/
    val eventId: Int
    val enabled: MutableSet<ActionMode>
    val actions: MutableMap<ActionMode, EventAction<EventHandler>>
    val adjustments: MutableMap<RelationType, HashSet<EventHandler>>
    val cooperations: MutableMap<RelationType, HashSet<EventHandler>>

    /*
        private EventHandler(final Consumer<RelationMapper<EventHandler>> relationMapper) {
        this.eventId = -1;
        this.actions = Collections.emptyMap();
        this.adjustments = Collections.emptyMap();
        this.cooperations = Collections.emptyMap();
        this.enabled = Collections.emptySet();
        relationMapper.accept((type, relations) ->
            Stream.of(relations).forEach(relation ->
                (type.affection == RelationAffection.COOPERATES
                    ? relation.sourceHandler.cooperations
                    : relation.sourceHandler.adjustments
                ).compute(type, (t, set) -> {
                    (set == null ? (set = new HashSet<>()) : set).add(relation.targetHandler);
                    return set;
                })
            )
        );
    }
     */
    constructor(relationMapper: Consumer<RelationMapper<EventHandler>>) {

        eventId = -1
        actions = HashMap()
        adjustments = HashMap()
        cooperations = HashMap()
        enabled = HashSet()

        relationMapper.accept { type, relations ->
            Stream.of(*relations).forEach { relation ->
                ((if (type!!.affection === RelationAffection.COOPERATES)
                    relation.sourceHandler.cooperations
                else
                    relation.sourceHandler.adjustments
                        ) as HashMap<RelationType, HashSet<EventHandler>>).compute(type!!
                ) { t, set ->
                    var set = set
                    if (set == null) set = HashSet()
                    set.add(relation.targetHandler)
                    set
                }
            }
        }
    }

    constructor(eventId: Int, vararg enableModes: ActionMode) : this(eventId, null, *enableModes) {}
    constructor(eventId: Int, actionMapper: Consumer<ActionMapper<EventHandler>>?, vararg enableModes: ActionMode) {
        this.eventId = eventId
        actions = EnumMap(ActionMode::class.java)
        enabled = EnumSet.noneOf(ActionMode::class.java)
        enabled.addAll(Arrays.asList(*enableModes))
        adjustments = EnumMap(RelationType::class.java)
        cooperations = EnumMap(RelationType::class.java)

        //adjustments = EnumMap(RelationType::class.java)
        //cooperations = EnumMap(RelationType::class.java)
        actionMapper?.accept(ActionMapper { key, value -> actions.put(key!!, value) })
    }

    /**
     * Interface implementation
     */
    override fun defaultEnabledActions(): Set<ActionMode> {
        return enabled
    }

    override fun allActions(): Map<ActionMode, EventAction<EventHandler>> {
        return actions
    }

    override fun cooperations(): Map<RelationType, Set<EventHandler>> {
        return cooperations
    }

    override fun adjustments(): Map<RelationType, Set<EventHandler>> {
        return adjustments
    }

    /**
     * A hack to make this Enum implementation sortable by primitive integers in another way then by ordinal()
     * The hack consists of implementing IntSupplier interface, this method and EVENT_SORT Comparator constant
     */
    override fun getAsInt(): Int {
        return eventId
    }

    companion object {
        var mouseCaptured = false

        fun menuCaptureChanges(observer: EventObserver<EventHandler>, capture: Boolean) {
            mouseCaptured = capture
            if (capture) {
                observer.enableAction(MOUSE_MOVE, ActionMode.PERFORM)
                observer.enableAction(MOUSE_DRAG, ActionMode.PERFORM)
                observer.enableAction(MOUSE_PRESS, ActionMode.PERFORM)
                observer.enableAction(MOUSE_RELEASE, ActionMode.PERFORM)
                observer.enableAction(MOUSE_ENTER, ActionMode.PERFORM)
                observer.disableAction(MOUSE_CLICK, ActionMode.PERFORM)
                observer.centreCursor(null)
            } else {
                observer.disableAction(MOUSE_MOVE, ActionMode.PERFORM)
                observer.disableAction(MOUSE_DRAG, ActionMode.PERFORM)
                observer.disableAction(MOUSE_PRESS, ActionMode.PERFORM)
                observer.disableAction(MOUSE_RELEASE, ActionMode.PERFORM)
                observer.disableAction(MOUSE_ENTER, ActionMode.PERFORM)
                observer.enableAction(MOUSE_CLICK, ActionMode.PERFORM)
                observer.restoreCursor(null)
            }
        }

        fun fullscreenChanges(observer: EventObserver<EventHandler>, fullscreen: Boolean) {
            /**
             * Clear any holding keys
             */
            observer.cancelKeys(null)
            if (fullscreen) {
                /**
                 * When in full-screen mode, COMPONENT_RESIZE is fired when you get the game visible
                 * (immediately after switch, or after return from alt-tab)
                 */
                observer.mapRelation(
                    COMPONENT_RESIZE,
                    RelationType.ENABLE,
                    WINDOW_OPEN,
                    WINDOW_LOSE_FOCUS,
                    KEY_PRESS,
                    KEY_RELEASE,
                    KEY_TYPE,
                    MOUSE_ENTER,
                    MOUSE_MOVE,
                    MOUSE_DRAG,
                    MOUSE_PRESS,
                    MOUSE_RELEASE
                )
                /**
                 * COMPONENT_MOVE is fired often in full-screen mode and does not mean that used did
                 * something with the window frame, actually there is no frame, there is no sense - disable it
                 */
                observer.disableAction(COMPONENT_MOVE, ActionMode.PERFORM)
            } else {
                /**
                 * Remove full-screen COMPONENT_RESIZE relations, if they was added earlier
                 */
                observer.unmapRelation(COMPONENT_RESIZE, RelationType.ENABLE)
                /**
                 * Immediately after return from full-screen mode, a bunch of events will occur,
                 * some of them will cause mouse capture to be lost. Disable them.
                 */
                observer.disableAction(WINDOW_LOSE_FOCUS, ActionMode.PERFORM)
                observer.disableAction(COMPONENT_MOVE, ActionMode.PERFORM)
                /**
                 * The last of the bunch of events should be WINDOW_ACTIVATE, add a function to him
                 * to restore the proper reaction on events we have switched off. It also should remove
                 * this function after it fired.
                 */
                observer.mapAction(
                    WINDOW_ACTIVATE,
                    ActionMode.PERFORM
                ) { ob, ev ->
                    observer.unmapAction(WINDOW_ACTIVATE, ActionMode.PERFORM)
                    observer.enableAction(WINDOW_LOSE_FOCUS, ActionMode.PERFORM)
                    observer.enableAction(COMPONENT_MOVE, ActionMode.PERFORM)
                }
            }
        }
    }
}

private fun mouseMoveAction(isDrag: Boolean): EventAction<EventHandler> {
    return EventAction { observer, ev ->
        if(observer == null) return@EventAction
        // Do not send robot-generated moves (centering) to the underlying DOOM's event engine
        if (observer.mouseEvent.robotMove) {
            observer.mouseEvent.robotMove = false
            return@EventAction
        }
        val centreX = observer.component.width shr 1
        val centreY = observer.component.height shr 1
        if (observer.component.isShowing && EventObserver.MOUSE_ROBOT.isPresent) {
            val offset = observer.component.locationOnScreen
            observer.mouseEvent.moveIn(
                (ev as MouseEvent?)!!,
                EventObserver.MOUSE_ROBOT.get(),
                offset,
                centreX,
                centreY,
                isDrag
            )
        } else {
            observer.mouseEvent.moveIn((ev as MouseEvent?)!!, centreX, centreY, isDrag)
        }
        if (observer.mouseEvent.processed) {
            observer.mouseEvent.resetNotify()
            observer.feed(observer.mouseEvent)
        }
    }
}

