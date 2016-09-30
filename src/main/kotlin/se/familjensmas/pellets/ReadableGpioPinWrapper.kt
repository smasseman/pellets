package se.familjensmas.pellets

import com.pi4j.io.gpio.GpioPinDigitalInput
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.event.GpioPinListener
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import java.util.concurrent.Executor

/**
 * Created by jorgen on 2016-09-26.
 */
class ReadableGpioPinWrapper(val pin: GpioPinDigitalInput, scheduler: Executor) :
        ListenerManager<ReadablePin.PinState>(scheduler), ReadablePin {

    init {
        pin.addListener(GpioPinListenerDigital {
            when (pin.state) {
                PinState.HIGH -> sendEvent(ReadablePin.PinState.HIGH)
                PinState.LOW -> sendEvent(ReadablePin.PinState.LOW)
            }
        })
    }
    override fun read(): ReadablePin.PinState {
        return when (pin.state) {
            PinState.HIGH -> ReadablePin.PinState.HIGH
            PinState.LOW -> ReadablePin.PinState.LOW
        }
    }

}