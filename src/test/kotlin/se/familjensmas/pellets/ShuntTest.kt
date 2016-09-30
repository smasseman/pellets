package se.familjensmas.pellets

import com.pi4j.io.gpio.GpioPinDigitalOutput
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Qualifier
import java.math.BigDecimal
import java.util.concurrent.Executor

class ShuntTest {

    @Test
    fun testSome() {
        val taskQueue = Executor { it.run() }

        val serverEvents = ServerEventManager(taskQueue)
        var savedShuntLevel = BigDecimal(20);
        val shuntLevelStorage = object : ShuntLevelStorage {
            override fun save(shuntLevel: BigDecimal) { savedShuntLevel = shuntLevel }
            override fun readShuntLevel() = savedShuntLevel
        }
        val indoor = ListenerManager<BigDecimal>(taskQueue)

        var pinState = OutputState.UP
        val shuntPin = object : WriteablePin {
            override fun set(state: OutputState) {
                pinState = state
            }
        }
        val shuntService = Shunt(serverEvents, shuntLevelStorage, indoor, shuntPin)

        indoor.sendEvent(BigDecimal(19))
        shuntService.changeLevel(BigDecimal(20))
        Assert.assertEquals(OutputState.UP, pinState)

        indoor.sendEvent(BigDecimal(23))
        //Now indoor is 23 and level is 20 so shunt should be LOW
        Assert.assertEquals(OutputState.DOWN, pinState)

        indoor.sendEvent(BigDecimal(13))
        Assert.assertEquals(OutputState.UP, pinState)

        indoor.sendEvent(BigDecimal(21))
        Assert.assertEquals(OutputState.DOWN, pinState)

        shuntService.changeLevel(BigDecimal(23))
        Assert.assertEquals(OutputState.UP, pinState)

        shuntService.changeLevel(BigDecimal(19))
        Assert.assertEquals(OutputState.DOWN, pinState)



    }
}
