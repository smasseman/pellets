package se.familjensmas.pellets

import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DigitalListenerFactory {

    private var delay: Int = 0

    fun delay(delay: Int): DigitalListenerFactory {
        this.delay = delay
        return this
    }

    fun create(listener: (event: GpioPinDigitalStateChangeEvent) -> Unit): GpioPinListenerDigital {

        val queue = Executors.newSingleThreadScheduledExecutor()
        var lastFall: Long = Long.MAX_VALUE;
        var lastRise: Long = Long.MAX_VALUE
        var last = PinState.LOW

        fun rise(e: GpioPinDigitalStateChangeEvent) = Runnable {
            if (last == PinState.LOW) {
                //println("Rise")
                last = PinState.HIGH
                listener.invoke(e)
            } else {
                //println("Rise ignored")
            }
        }

        fun fall(e: GpioPinDigitalStateChangeEvent) = Runnable {
            val timeSinceLastFall = SystemTime.now() - lastFall
            val timeSinceLastRise = SystemTime.now() - lastRise
            if (timeSinceLastFall > delay - 1 && timeSinceLastRise > delay - 1) {
                //println("Fall")
                last = PinState.LOW
                listener.invoke(e)
            } else {
                //println("Fall ignored")
            }
        }

        val result = object : GpioPinListenerDigital {
            override fun handleGpioPinDigitalStateChangeEvent(e: GpioPinDigitalStateChangeEvent?) {
                if (e == null)
                    return;
                when (e.state) {

                    null -> {
                        /*Do nothing.*/
                    }

                    PinState.HIGH -> {
                        lastRise = SystemTime.now()
                        println("Schedule Rise")
                        queue.schedule(rise(e), 1, TimeUnit.MILLISECONDS)
                    }

                    PinState.LOW -> {
                        lastFall = SystemTime.now()
                        println("Schedule Fall")
                        queue.schedule(fall(e), delay.toLong(), TimeUnit.MILLISECONDS)
                    }

                }
            }
        }
        return result
    }
}