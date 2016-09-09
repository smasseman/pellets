package se.familjensmas.pellets

import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ServerEventManager(val eventSink: Executor) : HttpServlet() {

    data class Connection(val writer: PrintWriter, val async: AsyncContext)

    private val logger = LoggerFactory.getLogger(javaClass.name)
    private val eventByCategory: LinkedHashMap<String, ServerEvent> = LinkedHashMap()
    private val listeners: LinkedList<Connection> = LinkedList()

    fun addEvent(event: ServerEvent) = addQueue("Put $event on queue.", {
        eventByCategory.remove(event.category)
        eventByCategory.put(event.category, event)
        logger.debug("Added event: $event")
        addQueue("Notify all listeners about $event") {
            notifyListeners(event)
        }
    })

    private fun notifyListeners(event: ServerEvent) {
        logger.debug("Notify all ${listeners.size} listeners about ${event.category}")
        val iter = listeners.iterator()
        while (iter.hasNext()) {
            val con = iter.next()
            try {
                sendEvent(con, event)
            } catch(e: Exception) {
                logger.debug("Remove a listener since we got $e")
                iter.remove()
                trash(con)
            }
        }
    }

    private fun trash(con: Connection) {
        try {
            con.async.complete()
        } catch(ignored: Exception) {
        }
    }

    private fun sendEvent(con: Connection, e: ServerEvent) {
        val writer = con.writer
        writer.write("event: " + e.category + "\n")
        Scanner(e.data.trim()).use { scanner ->
            scanner.useDelimiter("\n")
            while (scanner.hasNext()) {
                writer.write("data: " + scanner.next() + "\n")
            }
        }
        writer.write("\n\n")
        writer.flush()
        if (writer.checkError())
            throw IOException("Write failed.")
        logger.debug("Written event: $e")
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        logger.debug("Got request: " + request.requestURI)

        val async = request.startAsync(request, response)
        async.timeout = TimeUnit.HOURS.toMillis(24)
        async.addListener(object : AsyncListener {
            override fun onComplete(event: AsyncEvent?) {
                logger.debug("onCompelete: $event")
            }

            override fun onTimeout(event: AsyncEvent?) {
                logger.debug("onTimeout: $event")
            }

            override fun onStartAsync(event: AsyncEvent?) {
                logger.debug("onStart: $event")
            }

            override fun onError(event: AsyncEvent?) {
                logger.debug("onError: $event")
            }
        })
        val con = Connection(async.response.writer, async)

        response.setHeader("Cache-Control", "no-cache")
        response.addHeader("X-Accel-Buffering", "no")
        response.contentType = "text/event-stream"
        response.characterEncoding = "UTF-8"

        addQueue("Sending old events to new listener.") {
            try {
                eventByCategory.values.forEach {
                    logger.debug("Sending old event '$it.category' to new listener.")
                    sendEvent(con, it)
                }
                listeners.add(con)
                logger.debug("New listener added.")
            } catch(e: Exception) {
                logger.debug("Failed to write to new listener. Trash it")
                trash(con)
            }
        }
    }

    private fun addQueue(description: String, f: () -> Unit) {
        eventSink.execute(object : Runnable {
            override fun run() {
                f.invoke()
            }

            override fun toString(): String = description
        })
    }
}