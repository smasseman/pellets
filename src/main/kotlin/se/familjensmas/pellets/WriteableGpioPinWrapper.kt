package se.familjensmas.pellets

import com.pi4j.io.gpio.GpioPinDigitalOutput

class WriteableGpioPinWrapper(val gpio: GpioPinDigitalOutput) : WriteablePin {

    override fun set(state: OutputState) {
        when (state) {
            OutputState.DOWN -> gpio.low()
            OutputState.UP -> gpio.high()
        }
    }

    override fun toString(): String = gpio.name

}