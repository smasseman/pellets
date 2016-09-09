package se.familjensmas.pellets

import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert
import java.util.*

class DigitalListenerFactoryTest {


    @Test
    fun testSpike() {
        var level = PinState.LOW
        var rise = 0L
        var fall = 0L
        val listner = DigitalListenerFactory().delay(1000).create {
            when (it.state) {
                PinState.HIGH -> {
                    rise = SystemTime.now();
                    println("high");
                }
                PinState.LOW -> {
                    fall = SystemTime.now();
                    println("low")
                }
            }
        }

        var start = SystemTime.now()
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        Thread.sleep(100)
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
        var stop = SystemTime.now()

        Thread.sleep(1500)
        Assert.assertEquals((stop - start).toDouble(), (fall - rise).toDouble(), 100.toDouble())
    }

    @Test
    fun testStress() {
        var level = PinState.LOW
        var rise = 0L
        var fall = 0L
        val listner = DigitalListenerFactory().delay(1000).create {
            when (it.state) {
                PinState.HIGH -> {
                    rise = SystemTime.now();
                    println("high");
                }
                PinState.LOW -> {
                    fall = SystemTime.now();
                    println("low")
                }
            }
        }

        val rnd = Random()
        var start = SystemTime.now()
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        while (SystemTime.now() - start < 1500) {
            Thread.sleep(rnd.nextInt(100).toLong())
            if( rnd.nextInt(1) == 0 ) {
                listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
            } else {
                listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
            }
        }
        var stop = SystemTime.now()
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))

        Thread.sleep(1500)
        Assert.assertEquals((stop - start).toDouble(), (fall - rise).toDouble(), 100.toDouble())
    }


    @Test
    fun testSome() {
        var level = PinState.LOW

        val listner = DigitalListenerFactory().delay(1000).create { level = it.state }

        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))

        Thread.sleep(500)
        Assert.assertEquals(PinState.LOW, level)
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))

        Thread.sleep(600)
        Assert.assertEquals(PinState.HIGH, level)
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))

        Thread.sleep(400)
        Assert.assertEquals(PinState.HIGH, level)
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))

        Thread.sleep(700)
        Assert.assertEquals(PinState.HIGH, level)

        Thread.sleep(500)
        Assert.assertEquals(PinState.LOW, level)
    }

    private fun createEvent(state: PinState): GpioPinDigitalStateChangeEvent? {
        return GpioPinDigitalStateChangeEvent("Dummy", null, state);
    }
}
