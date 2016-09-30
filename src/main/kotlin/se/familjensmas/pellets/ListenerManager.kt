package se.familjensmas.pellets

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executor

open class ListenerManager<T>(val scheduler: Executor) {

    private val logger = LoggerFactory.getLogger(javaClass.name)
    private val listeners = LinkedList<Listener<T>>()

    fun addListener(listener: Listener<T>) {
        scheduler.execute(Runnable { listeners.add(listener) })
    }

    fun sendEvent(event: T) {
        scheduler.execute(Runnable {
            listeners.forEach {
                try {
                    it.event(event)
                } catch(t: Throwable) {
                    logger.warn("Notification failed when calling $it with $event", t)
                }
            }
        })
    }
}