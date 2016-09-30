package se.familjensmas.pellets

import com.pi4j.io.gpio.GpioPinDigitalInput
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
open class PelletSkruven public @Autowired constructor(
        @Qualifier("skruvPin") val pin: GpioPinDigitalInput,
        val eventSink: ServerEventManager,
        val taskQueue: TaskQueue,
        val skruvLog: SkruvLog) {

    private val logger = LoggerFactory.getLogger(javaClass.name)

    init {
        val webListener = DigitalListenerFactory().delay(60 * 1000).create {
            sendEventToWeb(it.state)
        }
        sendEventToWeb(pin.state)

        val logListener = DigitalListenerFactory().delay(100).create {
            sendEventToLog(it.state)
        }
        sendEventToLog(PinState.HIGH)

        pin.addListener(webListener, logListener)
    }

    private fun sendEventToLog(s: PinState) {
        logger.debug("Schedule $s to log.")
        taskQueue.execute({
            skruvLog.notify(s == PinState.HIGH)
        })
    }

    private fun sendEventToWeb (s: PinState) {
        logger.debug("Schedule $s to web.")
        taskQueue.execute({
            eventSink.addEvent(ServerEvent("skruv", s.name))
        })

    }
}

