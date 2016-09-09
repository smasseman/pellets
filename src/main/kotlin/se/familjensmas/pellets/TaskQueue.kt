package se.familjensmas.pellets

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor
import javax.annotation.PreDestroy

/**
 * Created by jorgen on 2016-08-22.
 */
@Service
class TaskQueue : Executor {

    private val logger = LoggerFactory.getLogger(javaClass.name)
    private val highPrioTasks = LinkedList<Runnable>()
    private val lowPrioTasks = LinkedList<Runnable>()
    private val lock = Object()
    private val thread = Thread(Runnable { loop() })
    private var ignoreNewTasks = false
    private val delayed = LinkedList<DelayedCommand>()

    init {
        thread.start()
    }

    @PreDestroy
    fun shutdown() {
        synchronized(lock) {
            ignoreNewTasks = true
        }
    }

    private fun loop() {
        while (!Thread.interrupted()) {
            val command: Runnable = getNextTask()
            executeTask(command)
        }
    }

    private fun executeTask(command: Runnable) {
        val commandRunningThread = Thread.currentThread()
        val executionDurationWatcher = createExecutionDurationWatcher(command, commandRunningThread)
        executionDurationWatcher.start()
        val start = System.currentTimeMillis()
        try {
            command.run()
        } finally {
            val duration = System.currentTimeMillis() - start
            executionDurationWatcher.interrupt()
            logger.trace("$duration ms to execute " + command)
        }
    }

    private fun createExecutionDurationWatcher(command: Runnable, commandRunningThread: Thread): Thread {
        val executionDurationWatcher = Thread(Runnable {
            try {
                Thread.sleep(Duration.ofSeconds(1).toMillis())
                logger.warn("Long running task detected: $command")
                commandRunningThread.stackTrace.forEach {
                    logger.info("Trace: $it")
                }
            } catch(ignored: InterruptedException) {
            }
        })
        return executionDurationWatcher
    }

    private fun getNextTask(): Runnable {
        synchronized(lock) {
            while (!Thread.interrupted()) {
                if( !delayed.isEmpty()) {
                    val first = delayed.first
                    val millisToExecutionTime = first.whenToExecute - SystemTime.now()
                    if( millisToExecutionTime <= 0) {
                        highPrioTasks.add(delayed.removeFirst().command)
                    }
                }

                if (!highPrioTasks.isEmpty())
                    return highPrioTasks.removeFirst()

                if (!lowPrioTasks.isEmpty())
                    return lowPrioTasks.removeFirst()

                if( delayed.isEmpty()) {
                    logger.trace("Queue is empty. Sleep.")
                    SystemTime.wait(lock)
                } else {
                    val delay = delayed.first.whenToExecute - SystemTime.now()
                    var time = Duration.ofMillis(delay)
                    logger.trace("Sleep $time before scheduled task ${delayed.first.command} is ready.")
                    SystemTime.wait(lock, time)
                }
            }
            throw InterruptedException()
        }
    }

    fun execute(description: String, command: () -> Unit) {
        execute(object : Runnable {
            override fun run() = command.invoke()
            override fun toString() = description
        })
    }

    override fun execute(command: Runnable) = addTask(highPrioTasks, command)

    fun executeWhenEmpty(command: Runnable) =  addTask(lowPrioTasks, command)

    fun executeWhenEmpty(description: String, command: () -> Unit) {
        executeWhenEmpty(object : Runnable {
            override fun run() = command.invoke()
            override fun toString() = description
        })
    }

    private fun addTask(queue: LinkedList<Runnable>, command: Runnable) {
        synchronized(lock) {
            if (!ignoreNewTasks) {
                queue.add(command)
                lock.notify()
            }
        }
    }

    fun schedule(delay: Duration, command: Runnable) {
        synchronized(lock) {
            delayed.add(DelayedCommand(SystemTime.now() + delay.toMillis(), command))
            Collections.sort(delayed)
            lock.notify()
        }
    }

    data class DelayedCommand(val whenToExecute: Long, val command: Runnable) : Comparable<DelayedCommand> {
        override fun compareTo(other: DelayedCommand): Int {
            return whenToExecute.compareTo(other.whenToExecute)
        }
    }

    fun schedule(delay: Duration, description: String, command: () -> Unit) {
        schedule(delay, object : Runnable {
            override fun run() = command.invoke()
            override fun toString() = description
        })
    }

}