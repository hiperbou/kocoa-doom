/**
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package doom


import g.Signals.ScanCode
import utils.C2JUtils
import java.awt.Point
import java.awt.Robot
import java.awt.event.MouseEvent
import java.util.function.*
import java.util.function.Function

// Event structure.
fun interface event_t {
    fun hasData(): Boolean {
        return false
    }

    fun isKey(): Boolean {
        return false
    }

    fun isKey(sc: ScanCode): Boolean {
        return false
    }

    fun <T> mapByKey(scMapper: Function<in ScanCode?, out T>): T {
        return scMapper.apply(null)
    }

    fun withKey(scConsumer: Consumer<in ScanCode>): Boolean {
        return false
    }

    fun ifKey(scCondition: Predicate<in ScanCode>): Boolean {
        return false
    }

    fun withKeyChar(scCharConsumer: IntConsumer): Boolean {
        return false
    }

    fun ifKeyChar(scCharCondition: IntPredicate): Boolean {
        return false
    }

    fun withKeyAsciiChar(scAsciiCharConsumer: IntConsumer): Boolean {
        return false
    }

    fun ifKeyAsciiChar(scCharCondition: IntPredicate): Boolean {
        return false
    }

    fun <T> withKey(scConsumer: Consumer<in T?>, extractor: Function<in ScanCode?, out T?>): Boolean {
        return false
    }

    fun <T> ifKey(scCondition: Predicate<in T?>, extractor: Function<in ScanCode?, out T?>): Boolean {
        return false
    }

    fun getSC(): ScanCode {
        return ScanCode.SC_NULL
    }

    fun isMouse(): Boolean {
        return false
    }

    fun isMouse(button: Int): Boolean {
        return false
    }

    fun <T> mapByMouse(mouseMapper: Function<in mouseevent_t?, out T>): T {
        return mouseMapper.apply(null)
    }

    fun withMouse(mouseConsumer: Consumer<mouseevent_t>): Boolean {
        return false
    }

    fun ifMouse(mouseCondition: Predicate<in mouseevent_t>): Boolean {
        return false
    }

    fun <T> withMouse(mouseConsumer: Consumer<in T?>, extractor: Function<in mouseevent_t?, out T?>): Boolean {
        return false
    }

    fun <T> ifMouse(mouseCondition: Predicate<in T?>, extractor: Function<in mouseevent_t?, out T?>): Boolean {
        return false
    }

    fun isJoy(): Boolean {
        return false
    }

    fun isJoy(button: Int): Boolean {
        return false
    }

    fun <T> mapByJoy(joyMapper: Function<in joyevent_t?, out T>): T {
        return joyMapper.apply(null)
    }

    fun withJoy(joyConsumer: Consumer<in joyevent_t>): Boolean {
        return false
    }

    fun ifJoy(joyCondition: Predicate<in joyevent_t>): Boolean {
        return false
    }

    fun <T> withJoy(joyConsumer: Consumer<in T?>, extractor: Function<in joyevent_t?, out T?>): Boolean {
        return false
    }

    fun <T> ifJoy(joyCondition: Predicate<in T?>, extractor: Function<in joyevent_t?, out T?>): Boolean {
        return false
    }

    fun type(): evtype_t
    fun isType(type: evtype_t): Boolean {
        return type() == type
    }

    fun isKey(sc: ScanCode, type: evtype_t): Boolean {
        return type() == type && isKey(sc)
    }

    fun ifKey(type: evtype_t, scCondition: Predicate<in ScanCode?>): Boolean {
        return if (type() == type) {
            ifKey(scCondition)
        } else false
    }

    fun withKey(type: evtype_t, scConsumer: Consumer<in ScanCode?>): Boolean {
        return if (type() == type) {
            this@event_t.withKey(scConsumer)
        } else false
    }

    fun withKey(sc: ScanCode, type: evtype_t, runnable: Runnable): Boolean {
        return if (type() == type) {
            withKey(sc, runnable)
        } else false
    }

    fun withKey(sc: ScanCode, runnable: Runnable): Boolean {
        if (isKey(sc)) {
            runnable.run()
            return true
        }
        return false
    }

    fun isMouse(button: Int, type: evtype_t): Boolean {
        return type() == type && isMouse(button)
    }

    fun ifMouse(type: evtype_t, mouseCondition: Predicate<in mouseevent_t>): Boolean {
        return if (type() == type) {
            ifMouse(mouseCondition)
        } else false
    }

    fun withMouse(type: evtype_t, mouseConsumer: Consumer<mouseevent_t>): Boolean {
        return if (type() == type) {
            this@event_t.withMouse(mouseConsumer)
        } else false
    }

    fun withMouse(button: Int, type: evtype_t, runnable: Runnable): Boolean {
        return if (type() == type) {
            withMouse(button, runnable)
        } else false
    }

    fun withMouse(button: Int, runnable: Runnable): Boolean {
        if (isMouse(button)) {
            runnable.run()
            return true
        }
        return false
    }

    fun isJoy(button: Int, type: evtype_t): Boolean {
        return type() == type && isJoy(button)
    }

    fun ifJoy(type: evtype_t, joyCondition: Predicate<in joyevent_t>): Boolean {
        return if (type() == type) {
            ifJoy(joyCondition)
        } else false
    }

    fun withJoy(type: evtype_t, joyConsumer: Consumer<in joyevent_t?>): Boolean {
        return if (type() == type) {
            this@event_t.withJoy(joyConsumer)
        } else false
    }

    fun withJoy(button: Int, type: evtype_t, runnable: Runnable): Boolean {
        return if (type() == type) {
            withJoy(button, runnable)
        } else false
    }

    fun withJoy(button: Int, runnable: Runnable): Boolean {
        if (isJoy(button)) {
            runnable.run()
            return true
        }
        return false
    }

    class keyevent_t(var type: evtype_t, var sc: ScanCode) : event_t {
        override fun hasData(): Boolean {
            return sc != ScanCode.SC_NULL
        }

        override fun type(): evtype_t {
            return type
        }

        override fun isKey(): Boolean {
            return true
        }

        override fun isKey(sc: ScanCode): Boolean {
            return this.sc == sc
        }

        override fun ifKey(scCondition: Predicate<in ScanCode>): Boolean {
            return scCondition.test(sc)
        }

        override fun withKey(scConsumer: Consumer<in ScanCode>): Boolean {
            scConsumer.accept(sc)
            return true
        }

        override fun ifKeyChar(scCharCondition: IntPredicate): Boolean {
            return scCharCondition.test(sc.c.code)
        }

        override fun withKeyChar(scCharConsumer: IntConsumer): Boolean {
            scCharConsumer.accept(sc.c.code)
            return true
        }

        override fun ifKeyAsciiChar(scAsciiCharCondition: IntPredicate): Boolean {
            return if (sc.c.code > 255) false else ifKeyChar(scAsciiCharCondition)
        }

        override fun withKeyAsciiChar(scAsciiCharConsumer: IntConsumer): Boolean {
            return if (sc.c.code > 255) false else withKeyChar(scAsciiCharConsumer)
        }

        override fun <T> ifKey(scCondition: Predicate<in T?>, extractor: Function<in ScanCode?, out T?>): Boolean {
            return scCondition.test(extractor.apply(sc))
        }

        override fun <T> withKey(scConsumer: Consumer<in T?>, extractor: Function<in ScanCode?, out T?>): Boolean {
            scConsumer.accept(extractor.apply(sc))
            return true
        }

        override fun <T> mapByKey(scMapper: Function<in ScanCode?, out T>): T {
            return scMapper.apply(sc)
        }

        override fun getSC(): ScanCode {
            return sc
        }
    }

    class mouseevent_t(
        @field:Volatile var type: evtype_t,
        @field:Volatile var buttons: Int,
        @field:Volatile var x: Int,
        @field:Volatile var y: Int
    ) : event_t {
        @Volatile
        var robotMove = false

        @Volatile
        var processed = true
        override fun hasData(): Boolean {
            return buttons != 0
        }

        fun buttonOn(ev: MouseEvent) {
            buttons = buttons or event_t.mouseBits(ev.button)
        }

        fun buttonOff(ev: MouseEvent) {
            buttons = buttons xor event_t.mouseBits(ev.button)
        }

        fun processedNotify() {
            processed = true
        }

        fun resetNotify() {
            processed = false
        }

        fun moveIn(ev: MouseEvent, centreX: Int, centreY: Int, drag: Boolean) {
            val mouseX = ev.x
            val mouseY = ev.y

            // Mouse haven't left centre of the window
            if (mouseX == centreX && mouseY == centreY) {
                return
            }

            // A pure move has no buttons.
            if (!drag) {
                buttons = 0
            }
            /**
             * Now also fix for -fasttic mode
             * - Good Sign 2017/05/07
             *
             * Fix bug with processing mouse: the DOOM underlying engine does not
             * react on the event as fast as it came, they are processed in constant time instead.
             *
             * In Mocha Doom, mouse events are not generated in bulks and sent to underlying DOOM engine,
             * instead the one only mouse event reused and resend modified if was consumed.
             *
             * So, if we have event system reacting faster then DOOM underlying engine,
             * mouse will be harder to move because the new move is forgotten earlier then processed.
             *
             * As a workaround, do not replace value in moveIn, and increment it instead,
             * and only when the underlying engine gives signal it has processed event, we clear x and y
             *
             * - Good Sign 2017/05/06
             */
            if (processed) {
                x = mouseX - centreX shl 2
                y = centreY - mouseY shl 2
            } else {
                x += mouseX - centreX shl 2
                y += centreY - mouseY shl 2
            }
        }

        fun moveIn(ev: MouseEvent, robot: Robot, windowOffset: Point, centreX: Int, centreY: Int, drag: Boolean) {
            moveIn(ev, centreX, centreY, drag)
            resetIn(robot, windowOffset, centreX, centreY)
        }

        fun resetIn(robot: Robot, windowOffset: Point, centreX: Int, centreY: Int) {
            // Mark that the next event will be from robot
            robotMove = true

            // Move the mouse to the window center
            robot.mouseMove(windowOffset.x + centreX, windowOffset.y + centreY)
        }

        override fun type(): evtype_t {
            return type
        }

        override fun isMouse(): Boolean {
            return true
        }

        override fun isMouse(button: Int): Boolean {
            return C2JUtils.flags(buttons, button)
        }

        override fun ifMouse(mouseCondition: Predicate<in mouseevent_t>): Boolean {
            return mouseCondition.test(this)
        }

        override fun withMouse(mouseConsumer: Consumer<mouseevent_t>): Boolean {
            mouseConsumer.accept(this)
            return true
        }

        override fun <T> ifMouse(
            mouseCondition: Predicate<in T?>,
            extractor: Function<in mouseevent_t?, out T?>
        ): Boolean {
            return mouseCondition.test(extractor.apply(this))
        }

        override fun <T> withMouse(
            mouseConsumer: Consumer<in T?>,
            extractor: Function<in mouseevent_t?, out T?>
        ): Boolean {
            mouseConsumer.accept(extractor.apply(this))
            return true
        }

        override fun <T> mapByMouse(mouseMapper: Function<in mouseevent_t?, out T>): T {
            return mouseMapper.apply(this)
        }
    }

    class joyevent_t(var type: evtype_t, var buttons: Int, var x: Int, var y: Int) : event_t {
        override fun hasData(): Boolean {
            return buttons != 0
        }

        override fun type(): evtype_t {
            return type
        }

        override fun isJoy(): Boolean {
            return true
        }

        override fun isJoy(button: Int): Boolean {
            return C2JUtils.flags(buttons, button)
        }

        override fun ifJoy(joyCondition: Predicate<in joyevent_t>): Boolean {
            return joyCondition.test(this)
        }

        override fun withJoy(joyConsumer: Consumer<in joyevent_t>): Boolean {
            joyConsumer.accept(this)
            return true
        }

        override fun <T> ifJoy(joyCondition: Predicate<in T?>, extractor: Function<in joyevent_t?, out T?>): Boolean {
            return joyCondition.test(extractor.apply(this))
        }

        override fun <T> withJoy(joyConsumer: Consumer<in T?>, extractor: Function<in joyevent_t?, out T?>): Boolean {
            joyConsumer.accept(extractor.apply(this))
            return true
        }

        override fun <T> mapByJoy(mouseMapper: Function<in joyevent_t?, out T>): T {
            return mouseMapper.apply(this)
        }
    }

    companion object {
        fun mouseBits(button: Int): Int {
            when (button) {
                MouseEvent.BUTTON1 -> return event_t.MOUSE_LEFT
                MouseEvent.BUTTON2 -> return event_t.MOUSE_RIGHT
                MouseEvent.BUTTON3 -> return event_t.MOUSE_MID
            }
            return 0
        }

        const val MOUSE_LEFT = 1
        const val MOUSE_RIGHT = 2
        const val MOUSE_MID = 4
        const val JOY_1 = 1
        const val JOY_2 = 2
        const val JOY_3 = 4
        const val JOY_4 = 8

        // Special FORCED and PAINFUL key and mouse cancel event.
        val EMPTY_EVENT = event_t { evtype_t.ev_null }
        val CANCEL_KEYS = event_t { evtype_t.ev_clear }
        val CANCEL_MOUSE: event_t = mouseevent_t(evtype_t.ev_mouse, 0, 0, 0)
    }
}