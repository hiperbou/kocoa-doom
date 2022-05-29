package utils


import utils.Throwers.Throwed
import java.io.PrintStream
import java.io.PrintWriter
import java.util.concurrent.*
import java.util.function.*
import java.util.function.Function

enum class Throwers {
    ;

    class Throwed(val t: Throwable) : RuntimeException(null, null, true, false) {
        @Synchronized
        override fun fillInStackTrace(): Throwable {
            return t.fillInStackTrace()
        }

        //@Synchronized
        override val cause: Throwable?
            get() = t.cause!!

        override fun getLocalizedMessage(): String {
            return t.localizedMessage
        }

        override val message: String?
            get() = t.message!!

        override fun getStackTrace(): Array<StackTraceElement> {
            return t.stackTrace
        }

        override fun setStackTrace(stackTrace: Array<StackTraceElement>) {
            t.stackTrace = stackTrace
        }

        @Synchronized
        override fun initCause(cause: Throwable): Throwable {
            return t.initCause(cause)
        }

        override fun printStackTrace() {
            t.printStackTrace()
        }

        override fun printStackTrace(s: PrintStream) {
            t.printStackTrace(s)
        }

        override fun printStackTrace(s: PrintWriter) {
            t.printStackTrace(s)
        }

        override fun toString(): String {
            return t.toString()
        }

        companion object {
            private const val serialVersionUID = 5802686109960804684L
        }
    }

    interface ThrowingCallable<T> {
        @Throws(Throwable::class)
        fun call(): T
    }

    interface ThrowingRunnable {
        @Throws(Throwable::class)
        fun run()
    }

    interface ThrowingConsumer<T> {
        @Throws(Throwable::class)
        fun accept(t: T)
    }

    interface ThrowingBiConsumer<T1, T2> {
        @Throws(Throwable::class)
        fun accept(t1: T1, t2: T2)
    }

    interface ThrowingPredicate<T> {
        @Throws(Throwable::class)
        fun test(t: T): Boolean
    }

    interface ThrowingBiPredicate<T1, T2> {
        @Throws(Throwable::class)
        fun test(t1: T1, t2: T2): Boolean
    }

    interface ThrowingFunction<T, R> {
        @Throws(Throwable::class)
        fun apply(t: T): R
    }

    interface ThrowingBiFunction<T1, T2, R> {
        @Throws(Throwable::class)
        fun apply(t1: T1, t2: T2): R
    }

    interface ThrowingSupplier<T> {
        @Throws(Throwable::class)
        fun get(): T
    }

    companion object {
        @SafeVarargs
        @Throws(Throwed::class)
        fun <T> callablfe(r: ThrowingCallable<T>, vararg cl: Class<out Throwable?>): Callable<T>? {
            return Callable {
                try {
                    r.call()
                } catch (e: Throwable) {
                    if (classifyMatching(e, *cl)) {
                        throw doThrow(e)
                    } else {
                        throw doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun runnable(r: ThrowingRunnable, vararg cl: Class<out Throwable?>): Runnable {
            return Runnable {
                try {
                    r.run()
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun <T> consumer(c: ThrowingConsumer<T>, vararg cl: Class<out Throwable?>): Consumer<T> {
            return Consumer { t: T ->
                try {
                    c.accept(t)
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun <T1, T2> biConsumer(c: ThrowingBiConsumer<T1, T2>, vararg cl: Class<out Throwable?>): BiConsumer<T1, T2> {
            return BiConsumer { t1: T1, t2: T2 ->
                try {
                    c.accept(t1, t2)
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun <T> predicate(p: ThrowingPredicate<T>, vararg cl: Class<out Throwable?>): Predicate<T> {
            return Predicate { t: T ->
                try {
                    p.test(t)
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun <T1, T2> biPredicate(
            p: ThrowingBiPredicate<T1, T2>,
            vararg cl: Class<out Throwable?>
        ): BiPredicate<T1, T2> {
            return BiPredicate { t1: T1, t2: T2 ->
                try {
                    p.test(t1, t2)
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun <T, R> function(f: ThrowingFunction<T, R>, vararg cl: Class<out Throwable?>): Function<T, R> {
            return Function { t: T ->
                try {
                    f.apply(t)
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun <T1, T2, R> biFunction(
            f: ThrowingBiFunction<T1, T2, R>,
            vararg cl: Class<out Throwable?>
        ): BiFunction<T1, T2, R> {
            return BiFunction { t1: T1, t2: T2 ->
                try {
                    f.apply(t1, t2)
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        @SafeVarargs
        @Throws(Throwed::class)
        fun <T> supplier(s: ThrowingSupplier<T>, vararg cl: Class<out Throwable?>): Supplier<T> {
            return Supplier {
                try {
                    s.get()
                } catch (e: Throwable) {
                    if (Throwers.classifyMatching(e, *cl)) {
                        throw Throwers.doThrow(e)
                    } else {
                        throw Throwers.doThrowE<RuntimeException>(e)
                    }
                }
            }
        }

        /**
         * Throw checked exception as runtime exception preserving stack trace The class of exception will be changed so it
         * will only trigger catch statements for new type
         *
         * @param e exception to be thrown
         * @return impossible
         * @throws Throwed
         */
        @Throws(Throwed::class)
        fun doThrow(e: Throwable): RuntimeException {
            throw Throwed(e)
        }

        /**
         * Throw checked exception as runtime exception preserving stack trace The class of exception will not be changed.
         * In example, an InterruptedException would then cause a Thread to be interrupted
         *
         * @param <E>
         * @param e exception to be thrown
         * @return impossible
         * @throws E (in runtime)
        </E> */
        //@Throws(E::class)
        private fun <E : Throwable> doThrowE(e: Throwable): RuntimeException {
            throw e// as E
        }

        @SafeVarargs
        private fun classifyMatching(ex: Throwable, vararg options: Class<out Throwable>): Boolean {
            for (o in options) {
                if (o.isInstance(ex)) {
                    return true
                }
            }
            return false
        }
    }
}