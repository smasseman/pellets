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

    class E(val executionTime: Long) : Delayed {
        override fun getDelay(unit: TimeUnit): Long {
            val millis = executionTime - SystemTime.now()
            return unit.convert(millis, TimeUnit.MILLISECONDS)
        }

        override fun compareTo(other: Delayed?): Int {
            if (other == null)
                return 0
            else if (other is E)
                return java.lang.Long.compare(executionTime, other.executionTime)
            else
                return 0
        }

    }

    fun create(listener: (event: GpioPinDigitalStateChangeEvent) -> Unit): GpioPinListenerDigital {

        val queue = Executors.newSingleThreadScheduledExecutor()
        var lastFall: Long = Long.MAX_VALUE;
        var lastRise: Long = Long.MAX_VALUE
        var last = PinState.LOW

        fun rise(e: GpioPinDigitalStateChangeEvent) = Runnable {
            if( last == PinState.LOW) {
                last = PinState.HIGH
                listener.invoke(e)
            }
        }

        fun fall(e: GpioPinDigitalStateChangeEvent) = Runnable {
            val timeSinceLastFall = SystemTime.now() - lastFall
            if( timeSinceLastFall > delay) {
                last = PinState.LOW
                listener.invoke(e)
            }
        }

        val result = object : GpioPinListenerDigital {
            override fun handleGpioPinDigitalStateChangeEvent(e: GpioPinDigitalStateChangeEvent?) {
                if (e == null)
                    return;
                when(e.state) {
                    PinState.HIGH -> {
                        lastRise = SystemTime.now()
                        queue.schedule(rise(e), delay.toLong(), TimeUnit.MILLISECONDS)
                    }
                    PinState.LOW -> {
                        lastFall = SystemTime.now()
                        queue.schedule(fall(e), delay.toLong(), TimeUnit.MILLISECONDS)
                    }
                }
            }
        }
        return result
    }
}