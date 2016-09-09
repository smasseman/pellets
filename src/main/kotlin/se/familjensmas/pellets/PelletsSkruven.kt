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
        val listener = DigitalListenerFactory().delay(500).create {
            logger.info("Skruv event: " + it.state)
            handleEvent(it.state)
        }
        pin.addListener(listener)
        handleEvent(pin.state)
    }

    private fun handleEvent(state: PinState) {
        synchronized(this) {
            logger.debug("Got skruv event: ${state.value}")
            eventSink.addEvent(ServerEvent("skruv", state.name))
            taskQueue.execute({ skruvLog.notify(state == PinState.HIGH) })
        }
    }
}

