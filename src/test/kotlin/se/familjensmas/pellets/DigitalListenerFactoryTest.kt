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
        var rise = 0L
        var fall = 0L
        val listener = DigitalListenerFactory().delay(1000).create {
            println(it)
            when (it.state) {
                PinState.HIGH -> rise = System.currentTimeMillis()
                PinState.LOW -> fall = System.currentTimeMillis()
            }
        }

        listener.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        listener.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))

        Thread.sleep(1300)
        Assert.assertTrue("Never got HIGH event", rise != 0L)
        Assert.assertTrue("Never got LOW event", fall != 0L)
        Assert.assertEquals(1000.0, (fall - rise).toDouble(), 100.toDouble())
    }

    @Test
    fun testSome() {
        var level = PinState.LOW

        val listener = DigitalListenerFactory().delay(500).create { level = it.state }

        listener.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        Thread.sleep(30)
        Assert.assertEquals(PinState.HIGH, level)

        listener.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
        Thread.sleep(30)
        Assert.assertEquals(PinState.HIGH, level)

        Thread.sleep(500)
        Assert.assertEquals(PinState.LOW, level)
    }

    @Test
    fun testSomeMore() {
        var gotLow = false

        val listener = DigitalListenerFactory().delay(500).create {
            if( it.state == PinState.LOW ) {
                println("LOOOOOW")
                gotLow = true
            }
        }

        val stopTime = 800 + System.currentTimeMillis()
        while( System.currentTimeMillis() < stopTime ) {
            println("Send high")
            listener.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
            Thread.sleep(100)
            println("Send low")
            listener.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
            Thread.sleep(100)
        }
        listener.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        Assert.assertFalse(gotLow)

        Thread.sleep(600)
        Assert.assertFalse(gotLow)
    }

    private fun createEvent(state: PinState): GpioPinDigitalStateChangeEvent? {
        return GpioPinDigitalStateChangeEvent(state.toString(), null, state);
    }

    @Test
    fun testMore() {
        var level = PinState.LOW
        val listner = DigitalListenerFactory().delay(10).create { level = it.state }

        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
        Thread.sleep(20)
        Assert.assertEquals(PinState.LOW, level)
    }


    @Test
    fun testMore2() {
        var level = PinState.LOW
        val listner = DigitalListenerFactory().delay(500).create { level = it.state }

        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.HIGH))
        listner.handleGpioPinDigitalStateChangeEvent(createEvent(PinState.LOW))
        Thread.sleep(10)
        Assert.assertEquals(PinState.HIGH, level)
        Thread.sleep(600)
        Assert.assertEquals(PinState.LOW, level)
    }

}
