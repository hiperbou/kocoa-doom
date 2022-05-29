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
package mochadoom

import awt.DoomWindow
import awt.EventBase
import awt.EventBase.*
import p.ActiveStates
import v.graphics.Patches
import java.awt.AWTEvent
import java.io.OutputStream
import java.util.*
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream


/**
 * Facility to manage Logger Levels for different classes
 * All of that should be used instead of System.err.println for debug
 *
 * @author Good Sign
 */
object Loggers {
    private val DEFAULT_LEVEL = Level.WARNING
    private val PARENT_LOGGERS_MAP = Stream.of(
        Level.FINE, Level.FINER, Level.FINEST, Level.INFO, Level.SEVERE, Level.WARNING
    ).collect(
        Collectors.toMap(
            { l -> l },
            { obj -> newLoggerHandlingLevel(obj) })
    )
    private val DEFAULT_LOGGER = PARENT_LOGGERS_MAP[DEFAULT_LEVEL]
    private val INDIVIDUAL_CLASS_LOGGERS = HashMap<String, Logger>()

    init {
        //INDIVIDUAL_CLASS_LOGGERS.put(EventObserver.class.getName(), PARENT_LOGGERS_MAP.get(Level.FINE));
        //INDIVIDUAL_CLASS_LOGGERS.put(TraitFactory.class.getName(), PARENT_LOGGERS_MAP.get(Level.FINER));
        INDIVIDUAL_CLASS_LOGGERS[ActiveStates::class.java.name] = PARENT_LOGGERS_MAP[Level.FINER] as Logger
        INDIVIDUAL_CLASS_LOGGERS[DoomWindow::class.java.name] = PARENT_LOGGERS_MAP[Level.FINE] as Logger
        INDIVIDUAL_CLASS_LOGGERS[Patches::class.java.name] = PARENT_LOGGERS_MAP[Level.INFO] as Logger
    }

    fun getLogger(className: String): Logger {
        val ret = Logger.getLogger(className)
        ret.parent = INDIVIDUAL_CLASS_LOGGERS.getOrDefault(className, DEFAULT_LOGGER)
        return ret
    }

    private var lastHandler: EventBase<*>? = null
    fun <EventHandler> LogEvent(
        logger: Logger,
        actionStateHolder: ActionStateHolder<EventHandler>,
        handler: EventHandler,
        event: AWTEvent
    ) where EventHandler : Enum<EventHandler>, EventHandler : EventBase<EventHandler> {
        if (!logger.isLoggable(Level.ALL) && lastHandler === handler) {
            return
        }
        lastHandler = handler
        val arrayGenerator = { it:Int -> arrayOfNulls<EventBase<EventHandler>>(it) }
        val depends = actionStateHolder
            .cooperations(handler, EventBase.RelationType.DEPEND)
            .stream()
            .filter { hdl: EventHandler ->
                actionStateHolder.hasActionsEnabled(
                    hdl,
                    ActionMode.DEPEND
                )
            }
            .toArray(arrayGenerator)
        val adjusts/*: HashMap<RelationType, HashSet<EventHandler>>*/ = actionStateHolder
            .adjustments(handler) //as HashMap<RelationType, HashSet<EventHandler>>
        val causes = actionStateHolder
            .cooperations(handler, EventBase.RelationType.CAUSE)
            .stream()
            .filter { hdl: EventHandler ->
                actionStateHolder.hasActionsEnabled(
                    hdl,
                    ActionMode.DEPEND
                )
            }
            .toArray(arrayGenerator)
        val reverts = actionStateHolder
            .cooperations(handler, EventBase.RelationType.REVERT)
            .stream()
            .filter { hdl: EventHandler ->
                actionStateHolder.hasActionsEnabled(
                    hdl,
                    ActionMode.DEPEND
                )
            }
            .toArray(arrayGenerator)
        if (logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST) {
            String.format(
                "\n\nENCOUNTERED EVENT: %s [%s] \n%s: %s \n%s \n%s: %s \n%s: %s \nOn event: %s",
                handler, ActionMode.PERFORM,
                EventBase.RelationType.DEPEND, Arrays.toString(depends),
                adjusts.entries.stream().collect(
                    { StringBuilder() },
                    { sb, (key, value) ->
                        sb.append(
                            key
                        ).append(' ').append(value).append('\n')
                    }
                ) { obj, s -> obj.append(s) },
                EventBase.RelationType.CAUSE, Arrays.toString(causes),
                EventBase.RelationType.REVERT, Arrays.toString(reverts),
                event
            )
        } else if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER) {
                String.format(
                    "\n\nENCOUNTERED EVENT: %s [%s] \n%s: %s \n%s \n%s: %s \n%s: %s \n",
                    handler, ActionMode.PERFORM,
                    EventBase.RelationType.DEPEND, Arrays.toString(depends),
                    adjusts.entries.stream().collect(
                        { StringBuilder() },
                        { sb, (key, value)->
                            sb.append(
                                key
                            ).append(' ').append(value).append('\n')
                        }
                    ) { obj, s -> obj.append(s) },
                    EventBase.RelationType.CAUSE, Arrays.toString(causes),
                    EventBase.RelationType.REVERT, Arrays.toString(reverts)
                )
            }
        } else {
            logger.log(Level.FINE) {
                String.format(
                    "\nENCOUNTERED EVENT: %s [%s]",
                    handler, ActionMode.PERFORM
                )
            }
        }
    }

    private fun newLoggerHandlingLevel(l: Level): Logger {
        val h = OutHandler()
        h.level = l
        val ret = Logger.getAnonymousLogger()
        ret.useParentHandlers = false
        ret.level = l
        ret.addHandler(h)
        return ret
    }

    private class OutHandler : ConsoleHandler() {
        @Synchronized
        @Throws(SecurityException::class)
        override fun setOutputStream(out: OutputStream) {
            super.setOutputStream(System.out)
        }
    }
}
