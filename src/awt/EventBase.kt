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



import g.Signals.ScanCode
import java.awt.*
import java.util.*
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.IntSupplier

/**
 * The base for construction of Event handling dictionaries
 * EventHandler is a reference implementation of this base
 *
 * Note the type safety with generics. It could be a complex task, but you can avoid
 * unchecked casts and warnings suppression. Whoa... Make my head swirl around!
 * - Good Sign 2017/04/24
 *
 * @author Good Sign
 */
interface EventBase<Handler> : IntSupplier where Handler : Enum<Handler>, Handler : EventBase<Handler> {
    fun defaultEnabledActions(): Set<ActionMode>
    fun allActions(): Map<ActionMode, EventAction<Handler>>
    fun cooperations(): Map<EventBase.RelationType, Set<Handler>>
    fun adjustments(): Map<EventBase.RelationType, Set<Handler>>
    fun hasActions(vararg modes: ActionMode): Boolean {
        val actions = defaultEnabledActions()
        if (actions.isEmpty()) {
            return false
        }
        for (m in modes) {
            if (!actions.contains(m)) {
                return false
            }
        }
        return true
    }

    enum class KeyStateSatisfaction {
        SATISFIED_ATE, GENEOROUS_PASS, WANTS_MORE_ATE, WANTS_MORE_PASS
    }

    enum class ActionMode {
        PERFORM, DEPEND, CAUSE, REVERT
    }

    enum class RelationAffection {
        ENABLES, DISABLES, COOPERATES
    }

    enum class RelationType(val affection: RelationAffection, val affectedMode: ActionMode) {
        ENABLE(RelationAffection.ENABLES, ActionMode.PERFORM), ENABLE_DEPEND(
            RelationAffection.ENABLES,
            ActionMode.DEPEND
        ),
        ENABLE_CAUSE(RelationAffection.ENABLES, ActionMode.CAUSE), ENABLE_REVERT(
            RelationAffection.ENABLES,
            ActionMode.REVERT
        ),
        DISABLE(RelationAffection.DISABLES, ActionMode.PERFORM), DISABLE_DEPEND(
            RelationAffection.DISABLES,
            ActionMode.DEPEND
        ),
        DISABLE_CAUSE(RelationAffection.DISABLES, ActionMode.CAUSE), DISABLE_REVERT(
            RelationAffection.DISABLES,
            ActionMode.REVERT
        ),
        DEPEND(RelationAffection.COOPERATES, ActionMode.DEPEND), CAUSE(
            RelationAffection.COOPERATES,
            ActionMode.CAUSE
        ),
        REVERT(RelationAffection.COOPERATES, ActionMode.REVERT);

        override fun toString(): String {
            return String.format("%s on [%s]", affection, affectedMode)
        }
    }

    fun interface ActionMapper<Handler> where Handler : Enum<Handler>, Handler : EventBase<Handler> {
        fun map(mode: ActionMode?, action: EventAction<Handler>)
    }

    fun interface RelationMapper<Handler> where Handler : Enum<Handler>, Handler : EventBase<Handler> {
        fun map(type: EventBase.RelationType?, relations: Array<EventBase.Relation<Handler>>)
    }

    fun interface EventAction<Handler> where Handler : Enum<Handler>, Handler : EventBase<Handler> {
        fun act(obs: EventObserver<Handler>?, ev: AWTEvent)
    }

    fun interface KeyStateCallback<Handler> where Handler : Enum<Handler>, Handler : EventBase<Handler> {
        fun call(observer: EventObserver<Handler>): KeyStateSatisfaction?
    }

    class KeyStateInterest<Handler>(
        satisfiedCallback: KeyStateCallback<Handler>,
        interestFirstKey: ScanCode,
        vararg interestKeyChain: ScanCode?
    ) where Handler : Enum<Handler>, Handler : EventBase<Handler> {
        val interestSet: Set<ScanCode>
        val satisfiedCallback: KeyStateCallback<Handler>

        init {
            interestSet = EnumSet.of(interestFirstKey, *interestKeyChain)
            this.satisfiedCallback = satisfiedCallback
        }
    }

    class KeyStateHolder<Handler> where Handler : Enum<Handler>, Handler : EventBase<Handler> {
        private val holdingSet: MutableSet<ScanCode>
        private val keyInterests: LinkedHashSet<KeyStateInterest<Handler>>
        private val generator = IntFunction { arrayOfNulls<KeyStateInterest<Handler>>(it) }

        init {
            holdingSet = EnumSet.noneOf(ScanCode::class.java)
            keyInterests = LinkedHashSet()
        }

        fun removeAllKeys() {
            holdingSet.clear()
        }

        operator fun contains(sc: ScanCode): Boolean {
            return holdingSet.contains(sc)
        }

        fun addInterest(interest: KeyStateInterest<Handler>) {
            keyInterests.add(interest)
        }

        fun removeInterest(interest: KeyStateInterest<Handler>) {
            keyInterests.remove(interest)
        }

        fun matchInterest(check: KeyStateInterest<Handler>): Boolean {
            return holdingSet.containsAll(check.interestSet)
        }

        fun notifyKeyChange(observer: EventObserver<Handler>, code: ScanCode?, press: Boolean): Boolean {
            return if (press) {
                holdingSet.add(code!!)
                val matched = keyInterests.stream()
                    .filter { check: KeyStateInterest<Handler> -> matchInterest(check) }
                    .toArray(generator)
                var ret = false
                for (i in matched.indices) {
                    when (matched[i]!!.satisfiedCallback.call(observer)) {
                        KeyStateSatisfaction.SATISFIED_ATE -> {
                            ret = true
                            keyInterests.remove(matched[i])
                        }
                        KeyStateSatisfaction.GENEOROUS_PASS -> keyInterests.remove(matched[i])
                        KeyStateSatisfaction.WANTS_MORE_ATE -> ret = true
                        KeyStateSatisfaction.WANTS_MORE_PASS -> {}
                    }
                }
                ret
            } else {
                holdingSet.remove(code)
                false
            }
        }
    }

    /**
     * Enable/disable and remaps of actions is actually reflected here. It is only initial template in the Handler
     */
    class ActionStateHolder<Handler>(
        hClass: Class<Handler>,
        observer: EventObserver<Handler>
    ) where Handler : Enum<Handler>, Handler : EventBase<Handler> {
        private val enabledActions: Map<Handler, MutableSet<ActionMode>>
        private val actionsMap: Map<Handler, MutableMap<ActionMode, EventAction<Handler>>>
        private val cooperationMap: Map<Handler, MutableMap<RelationType, MutableSet<Handler>>>
        private val adjustmentMap: Map<Handler, MutableMap<RelationType, MutableSet<Handler>>>
        private val observer: EventObserver<Handler>
        private val emptyEnumSet: EnumSet<Handler>
        fun hasActionsEnabled(h: Handler, vararg modes: ActionMode): Boolean {
            val actions: Set<ActionMode> = enabledActions[h]!!
            if (actions.isEmpty()) {
                return false
            }
            for (m in modes) {
                if (!actions.contains(m)) {
                    return false
                }
            }
            return true
        }

        init {
            val values = hClass.enumConstants
            enabledActions = populate<MutableSet<ActionMode>>(
                hClass,
                values,
                /*Function<Handler, MutableSet<ActionMode?>>*/ { h: Handler ->
                    val set: Set<ActionMode> = h!!.defaultEnabledActions()
                    if (set.isEmpty()) EnumSet.noneOf(ActionMode::class.java) else EnumSet.copyOf(set)
                })
            actionsMap = populate<MutableMap<ActionMode, EventAction<Handler>>>(
                hClass,
                values,
                /*Function<Handler, MutableMap<ActionMode?, EventAction<Handler?>?>>*/ { h ->
                    val map: Map<ActionMode, EventAction<Handler>> = h!!.allActions()
                    if (map.isEmpty()) EnumMap(ActionMode::class.java) else EnumMap<ActionMode, EventAction<Handler>>(
                        map
                    )
                })
            cooperationMap = populate<MutableMap<EventBase.RelationType, MutableSet<Handler>>>(
                hClass,
                values,
                /*Function<Handler, MutableMap<EventBase.RelationType?, Set<Handler?>?>>*/{ h ->
                    deepCopyMap(
                        h!!.cooperations()
                    )
                })
            adjustmentMap = populate<MutableMap<EventBase.RelationType, MutableSet<Handler>>>(
                hClass,
                values,
                /*Function<Handler, MutableMap<EventBase.RelationType?, Set<Handler?>?>>*/ { h ->
                    deepCopyMap(
                        h!!.adjustments()
                    )
                })
            this.observer = observer
            emptyEnumSet = EnumSet.noneOf(hClass)
        }

        private fun deepCopyMap(map: Map<RelationType, Set<Handler>>): MutableMap<RelationType, MutableSet<Handler>> {
            if (map.isEmpty()) {
                return EnumMap(RelationType::class.java)
            }

            // shallow copy first
            val copy = EnumMap(map)
            // now values
            copy.replaceAll { r, l -> EnumSet.copyOf(l) }
            return copy as MutableMap<RelationType, MutableSet<Handler>>
        }

        private fun <V> populate(
            hClass: Class<Handler>,
            values: Array<Handler>,
            mapper: Function<in Handler, out V>
        ): Map<Handler, V> {
            return Arrays.stream(values).collect(
                { EnumMap(hClass) },
                { m: EnumMap<Handler, V>, h: Handler ->
                    m[h] = mapper.apply(h)
                }) { obj: EnumMap<Handler, V>, m: EnumMap<Handler, V> ->
                obj.putAll(
                    m
                )
            }
        }

        fun run(h: Handler, mode: ActionMode, ev: AWTEvent): ActionStateHolder<Handler> {
            if (enabledActions[h]!!.contains(mode)) {
                Optional.ofNullable(
                    actionsMap[h]!![mode]
                ).ifPresent { action: EventAction<Handler> -> action.act(observer, ev) }
            }
            return this
        }

        fun cooperations(h: Handler): Map<EventBase.RelationType, MutableSet<Handler>> {
            return cooperationMap[h]!!
        }

        fun adjustments(h: Handler): Map<EventBase.RelationType, MutableSet<Handler>> {
            return adjustmentMap[h]!!
        }

        fun cooperations(h: Handler, type: EventBase.RelationType): Set<Handler> {
            return cooperationMap[h]!!.getOrDefault(type, emptyEnumSet)
        }

        fun adjustments(h: Handler, type: EventBase.RelationType): Set<Handler> {
            return adjustmentMap[h]!!.getOrDefault(type, emptyEnumSet)
        }

        @SafeVarargs
        fun unmapCooperation(
            h: Handler,
            type: EventBase.RelationType,
            vararg targets: Handler
        ): ActionStateHolder<Handler> {
            val set = cooperationMap[h]!![type]
            if (set == null || set.isEmpty()) {
                return this
            }
            if (targets.size == 0) {
                set.clear()
            } else {
                set.removeAll(Arrays.asList(*targets))
            }
            return this
        }

        @SafeVarargs
        fun mapCooperation(
            h: Handler,
            mode: EventBase.RelationType,
            vararg targets: Handler
        ): ActionStateHolder<Handler> {
            cooperationMap[h]!!.compute(mode) { m, set ->
                var set = set
                if (set == null) {
                    set = EnumSet.copyOf(emptyEnumSet)
                }
                set!!.addAll(Arrays.asList(*targets))
                set
            }
            return this
        }

        @SafeVarargs
        fun restoreCooperation(
            h: Handler,
            mode: EventBase.RelationType,
            vararg targets: Handler
        ): ActionStateHolder<Handler> {
            val orig = h!!.adjustments()[mode]
            if (orig != null) {
                val a: MutableSet<Handler> = EnumSet.copyOf(orig)
                val b = cooperationMap[h]!![mode]
                a.retainAll(Arrays.asList(*targets))
                b!!.addAll(a)
            } else {
                cooperationMap[h]!!.remove(mode)
            }
            return this
        }

        @SafeVarargs
        fun unmapAdjustment(
            h: Handler,
            type: EventBase.RelationType,
            vararg targets: Handler
        ): ActionStateHolder<Handler> {
            val set = adjustmentMap[h]!![type]
            if (set == null || set.isEmpty()) {
                return this
            }
            if (targets.size == 0) {
                set.clear()
            } else {
                set.removeAll(Arrays.asList(*targets))
            }
            return this
        }

        @SafeVarargs
        fun mapAdjustment(
            h: Handler,
            mode: EventBase.RelationType,
            vararg targets: Handler
        ): ActionStateHolder<Handler> {
            adjustmentMap[h]!!.compute(mode) { m, set ->
                var set = set
                if (set == null) {
                    set = EnumSet.copyOf(emptyEnumSet)
                }
                set!!.addAll(Arrays.asList(*targets))
                set
            }
            return this
        }

        @SafeVarargs
        fun restoreAdjustment(
            h: Handler,
            mode: EventBase.RelationType,
            vararg targets: Handler
        ): ActionStateHolder<Handler> {
            val orig = h!!.adjustments()[mode]
            if (orig != null) {
                val a: MutableSet<Handler> = EnumSet.copyOf(orig)
                val b = adjustmentMap[h]!![mode]
                a.retainAll(Arrays.asList(*targets))
                b!!.addAll(a)
            } else {
                adjustmentMap[h]!!.remove(mode)
            }
            return this
        }

        fun enableAction(h: Handler, mode: ActionMode): ActionStateHolder<Handler> {
            enabledActions[h]!!.add(mode)
            return this
        }

        fun disableAction(h: Handler, mode: ActionMode): ActionStateHolder<Handler> {
            enabledActions[h]!!.remove(mode)
            return this
        }

        fun unmapAction(h: Handler, mode: ActionMode): ActionStateHolder<Handler> {
            actionsMap[h]!!.remove(mode)
            return this
        }

        fun mapAction(h: Handler, mode: ActionMode, remap: EventAction<Handler>): ActionStateHolder<Handler> {
            actionsMap[h]!![mode] = remap
            return this
        }

        fun remapAction(h: Handler, mode: ActionMode, remap: EventAction<Handler>): ActionStateHolder<Handler> {
            actionsMap[h]!!.replace(mode, remap)
            return this
        }

        fun restoreAction(h: Handler, mode: ActionMode): ActionStateHolder<Handler> {
            val a: EventAction<Handler>? = h!!.allActions().get(mode)
            if (a != null) {
                actionsMap[h]!![mode] = a
            } else {
                actionsMap[h]!!.remove(mode)
            }
            return this
        }
    }

    class Relation<Handler>(
        val sourceHandler: Handler,
        val targetHandler: Handler
    ) where Handler : Enum<Handler>, Handler : EventBase<Handler>

    companion object {
        val EVENT_SORT = Comparator.comparingInt { obj: IntSupplier -> obj.asInt }
        fun <H> sortHandlers(values: Array<H>): Array<H> where H : Enum<H>, H : EventBase<H> {
            Arrays.sort(values, EVENT_SORT)
            return values
        }

        fun <H> findById(values: Array<H>, eventId: Int): Optional<H> where H : Enum<H>, H : EventBase<H> {
            val index = Arrays.binarySearch(values, IntSupplier { eventId }, EventBase.EVENT_SORT)
            return if (index < 0) {
                Optional.empty()
            } else Optional.of(values[index])
        }

        @SafeVarargs
        fun <H> Relate(src: H, vararg dests: H): Array<EventBase.Relation<H>?> where H : Enum<H>, H : EventBase<H> {
            val arrayer = IntFunction<Array<EventBase.Relation<H>?>> { arrayOfNulls(it) }
            return Arrays.stream(dests)
                .map { dest: H -> EventBase.Relation(src, dest) }
                .toArray(arrayer)
        }
    }
}