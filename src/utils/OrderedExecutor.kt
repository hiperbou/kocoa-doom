package utils


import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * An executor that make sure tasks submitted with the same key
 * will be executed in the same order as task submission
 * (order of calling the [.submit] method).
 *
 * Tasks submitted will be run in the given [Executor].
 * There is no restriction on how many threads in the given [Executor]
 * needs to have (it can be single thread executor as well as a cached thread pool).
 *
 * If there are more than one thread in the given [Executor], tasks
 * submitted with different keys may be executed in parallel, but never
 * for tasks submitted with the same key.
 *
 * * @param <K> type of keys.
</K> */
class OrderedExecutor<K>(private val executor: Executor) {
    private val tasks: MutableMap<K, Task?>

    /**
     * Constructs a `OrderedExecutor`.
     *
     * @param executor tasks will be run in this executor.
     */
    init {
        tasks = HashMap<K, Task?>()
    }

    /**
     * Adds a new task to run for the given key.
     *
     * @param key the key for applying tasks ordering.
     * @param runnable the task to run.
     */
    @Synchronized
    fun submit(key: K, runnable: Runnable) {
        var task: Task? = tasks[key]
        if (task == null) {
            task = Task()
            tasks[key] = task
        }
        task.add(runnable)
    }

    /**
     * Private inner class for running tasks for each key.
     * Each key submitted will have one instance of this class.
     */
    private inner class Task internal constructor() : Runnable {
        private val lock: Lock
        private val queue: Queue<Runnable>

        init {
            lock = ReentrantLock()
            queue = LinkedList()
        }

        fun add(runnable: Runnable) {
            val runTask: Boolean
            lock.lock()
            try {
                // Run only if no job is running.
                runTask = queue.isEmpty()
                queue.offer(runnable)
            } finally {
                lock.unlock()
            }
            if (runTask) {
                executor.execute(this)
            }
        }

        override fun run() {
            // Pick a task to run.
            val runnable: Runnable
            lock.lock()
            runnable = try {
                queue.peek()
            } finally {
                lock.unlock()
            }
            try {
                runnable.run()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            // Check to see if there are queued task, if yes, submit for execution.
            lock.lock()
            try {
                queue.poll()
                if (!queue.isEmpty()) {
                    executor.execute(this)
                }
            } finally {
                lock.unlock()
            }
        }
    }
}