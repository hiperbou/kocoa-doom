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
package utils


import mochadoom.Loggers
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * Purpose of this pattern-interface: store Trait-specific class-wise context objects
 * and be able to get them in constant time.
 *
 * Simple usage:
 * You may read the theory below to understand, why and for what reason I wrote
 * TraitFactory. However, the simplest use is: create an interface extending Trait,
 * put there static final KeyChain object field, and declare some static ContextKey<>
 * fields in descending classes and/or interfaces for your objects using KeyChain.newKey.
 *
 * Then to initialize everything, just call TraitFactory.build() and the result
 * will be SharedContext to return on overriden method of Trait.
 *
 * TraitFactory.build utilizes (at the instantiation time, not in runtime) some
 * black reflection magic to free you from need to look for every Trait in line,
 * and call some registering function to add Objects to Keys in InsertConveyor.
 *
 * General contract of Trait:
 *
 * 0. In the constructor of object implementing the subset of Traits based
 * on this Trait, you must call TraitFactory.build(this, idCapacity);
 * Implementing this Trait otherwise means nothing.
 *
 * The result of TraitFactory.build(this, idCapacity); must be stored
 * and the overriden method getContext() must return it.
 *
 * You can use some static non-final int[] field of deepest Trait dependency
 * that is incremented by static initialization of all who depend on it,
 * to determine idCapacity, or just guess big enough on your own. Also you can
 * use helper object, KeyChain.
 *
 * 1. In a Trait of your subset, where you want to have some object in context, you
 * must create static final ContextKey fild. During the static final ContextKey
 * initialization, you can also hack into incrementing some static non-final
 * somewhere, to be sure all who do the same produce unique fast ContextKeys.
 *
 * You can create several ContextKeys per Trait and store several contexts,
 * and, if your preferedIds are unique, they will be still instant-fast.
 *
 * 2. You may want to be sure that all of your interfaces have created their context
 * objects and put them into the InsertConveyor. To do that, you should have a
 * method on the class using traits, that will descend into the top level traits,
 * then lower and lower until the last of the traits.
 *
 * ContextKey does not override hashCode and is a final class. So the hashCode()
 * method will be something like memory pointer, and uniqye per ContextKey.
 * Default context storage (FactoryContext.class) does not check it until
 * any new stored ContextKey have preferedId already taken, and reports different
 * context Object Class. If such happen, all associated contexts are moved
 * into HashMap and context acquisition will be since significantly slower.
 *
 * If your ContextKey does not overlap with another one, access to context Object
 * would be the most instant of all possible.
 *
 * 3. In use, call contextGet(ContextKey) or some helper methods to get
 * the Object from context. Alternatively, you can acquire the SharedContext.
 * The helper methods are better in case you fear nulls.
 *
 * As the SharedContext is Shared, you can use it and objects from it in any
 * descendants of the trait where you put this object into the context by key.
 *
 * If you made sure you never put two Objects of different type with two ContextKeys
 * with matching preferedIds and Class'es, the cost of get(ContextKey) will be
 * as negligible as one level of indirection + array access by int.
 */
object TraitFactory {
    private val LOGGER = Loggers.getLogger(TraitFactory::class.java.name)
    @Throws(IllegalArgumentException::class, IllegalAccessException::class)
    fun <T : Trait?> build(traitUser: T, usedChain: KeyChain): SharedContext {
        return build<T>(traitUser, usedChain.currentCapacity)
    }

    @Throws(IllegalArgumentException::class, IllegalAccessException::class)
    fun <T : Trait?> build(traitUser: T, idCapacity: Int): SharedContext {
        val c = FactoryContext(idCapacity)
        repeatRecursive(traitUser!!::class.java.getInterfaces(), c)
        return c
    }

    @Throws(IllegalAccessException::class, SecurityException::class, IllegalArgumentException::class)
    private fun repeatRecursive(traitUserInteraces: Array<Class<*>>, c: FactoryContext) {
        for (cls in traitUserInteraces) {
            val declaredFields = cls.declaredFields
            for (f in declaredFields) {
                val modifiers = f.modifiers
                if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                    val fieldClass = f.type
                    if (fieldClass == ContextKey::class.java) {
                        val key = ContextKey::class.java.cast(f[null])
                        c.put(key, key.contextConstructor)
                        LOGGER.fine { String.format("%s for %s", c[key]!!.javaClass, f.declaringClass) }
                    }
                }
            }
            repeatRecursive(cls.interfaces, c)
        }
    }

    private fun getParameterizedTypes(`object`: Any): Array<Type>? {
        val superclassType = `object`.javaClass.genericSuperclass
        return if (!ParameterizedType::class.java.isAssignableFrom(superclassType.javaClass)) {
            null
        } else (superclassType as ParameterizedType).actualTypeArguments
    }

    interface Trait {
        fun getContext(): SharedContext
        fun <T> contextGet(key: ContextKey<T>, defaultValue: T): T {
            val got = getContext().get(key)
            return got ?: defaultValue
        }

        fun <T> contextRequire(key: ContextKey<T>): T {
            //TODO: the original code breaks because of reflection use this works at runtime, buy could be optimized as the original code
            return getContext().getOrPut(key) ?: throw defaultException(key).get()!!
            //return getContext()[key] ?: throw defaultException(key).get()!!
        }

        @Throws(Throwable::class)
        fun <T, E : Throwable?> contextRequire(key: ContextKey<T>, exceptionSupplier: Supplier<E>): T {
            return getContext()[key] ?: throw exceptionSupplier.get() as Throwable
        }

        fun <T> contextTest(key: ContextKey<T>, predicate: Predicate<T>): Boolean {
            val got = getContext().get(key)
            return if (got == null) false else predicate.test(got)
        }

        fun <T> contextWith(key: ContextKey<T>, consumer: Consumer<T>) {
            val got = getContext().get(key)
            if (got != null) {
                consumer.accept(got)
            }
        }

        fun <T, R> contextMap(key: ContextKey<T>, mapper: Function<T, R>, defaultValue: R): R {
            val got = getContext().get(key)
            return if (got != null) {
                mapper.apply(got)
            } else {
                defaultValue
            }
        }

        fun defaultException(key: ContextKey<*>?): Supplier<out RuntimeException?> {
            return Supplier<RuntimeException?> { SharedContextException(key, this.javaClass) }
        }
    }

    class ContextKey<T>(val traitClass: Class<out Trait>, val preferredId: Int, val contextConstructor: Supplier<T>) {
        override fun toString(): String {
            return String.format("context in the Trait %s (preferred id: %d)", traitClass, preferredId)
        }
    }

    class KeyChain {
        var currentCapacity = 0
        fun <T> newKey(traitClass: Class<out Trait>, contextConstructor: Supplier<T>): ContextKey<T> {
            return ContextKey(traitClass, currentCapacity++, contextConstructor)
        }
    }

    interface SharedContext {
        operator fun <T> get(key: ContextKey<T>): T?
        fun <T>getOrPut(key:ContextKey<T>):T
    }

    internal class FactoryContext(idCapacity: Int) : InsertConveyor, SharedContext {
        private val traitMap: HashMap<ContextKey<*>?, Any?> = HashMap()
        private var keys: Array<ContextKey<*>?>? = arrayOfNulls<ContextKey<*>?>(idCapacity)
        private var contexts: Array<Any?>? = arrayOfNulls(idCapacity)
        private var hasMap = false

        override fun put(key: ContextKey<*>, context: Supplier<*>) {
            if (!hasMap) {
                if (key.preferredId >= 0 && key.preferredId < keys!!.size) {
                    // return in the case of duplicate initialization of trait
                    if (keys!![key.preferredId] == key) {
                        TraitFactory.LOGGER.finer { "Already found, skipping: $key" }
                        return
                    } else if (keys!![key.preferredId] == null) {
                        keys!![key.preferredId] = key
                        contexts!![key.preferredId] = context.get()
                        return
                    }
                }
                hasMap = true
                for (i in keys!!.indices) {
                    traitMap!![keys!![i]] = contexts!![i]
                }
                keys = null
                contexts = null
            }
            traitMap!![key] = context.get()
        }

        override fun <T> get(key: ContextKey<T>): T? {
            if (hasMap) {
                return traitMap!![key] as T?
            } else if (key.preferredId >= 0 && key.preferredId < keys!!.size) {
                return contexts!![key.preferredId] as T?
            }
            return null
        }

        override fun <T> getOrPut(key: ContextKey<T>): T {
            val b = get(key)
            if(b != null) return b
            put(key, key.contextConstructor)
            return get(key)!!
        }
    }

    interface InsertConveyor {
        fun put(key: ContextKey<*>, context: Supplier<*>)
        fun putObj(key: ContextKey<*>, context: Any) {
            put(key) { context }
        }
    }

    private class SharedContextException internal constructor(key: ContextKey<*>?, topLevel: Class<out Trait?>?) :
        RuntimeException(
            String.format(
                "Trait context %s is not initialized when used by %s or"
                        + "is dereferencing a null pointer when required to do not",
                key, topLevel
            )
        ) {
        companion object {
            private const val serialVersionUID = 5356800492346200764L
        }
    }
}