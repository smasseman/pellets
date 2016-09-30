package se.familjensmas.pellets

import com.pi4j.io.gpio.GpioPinDigitalOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths

@Service
class Shunt @Autowired constructor(val serverEvents: ServerEventManager,
                                   val shuntLevelStorage: ShuntLevelStorage,
                                   @Qualifier("indoor") indoor: ListenerManager<BigDecimal>,
                                   val shuntPin: WriteablePin) {

    private val logger = LoggerFactory.getLogger(javaClass.name)
    private var shuntLevel = shuntLevelStorage.readShuntLevel()
    private var shuntOn = false
    private var currentTemp: BigDecimal = BigDecimal(100)

    init {
        shuntPin.low()
        indoor.addListener(object : Listener<BigDecimal> {
            override fun event(event: BigDecimal) {
                tempChange(event)
            }
        })
        pushShuntLevelSSEvent()
    }

    private fun tempChange(event: BigDecimal) {
        currentTemp = event;
        updateShuntRelay()
    }

    private fun pushShuntLevelSSEvent() {
        serverEvents.addEvent(ServerEvent(category = "shuntlevel", data = shuntLevel.toString()))
    }

    fun levelUp() {
        changeLevel(shuntLevel + BigDecimal.ONE)
    }


    fun changeLevel(value: BigDecimal) {
        shuntLevel = value
        shuntLevelStorage.save(shuntLevel)
        pushShuntLevelSSEvent()
        updateShuntRelay()
    }

    private fun updateShuntRelay() {
        if (shuntOn && currentTemp.compareTo(shuntLevel) > 0) {
            shuntPin.low()
            shuntOn = false
            logger.debug("Shunt pin $shuntPin set to low.")
            serverEvents.addEvent(ServerEvent(category = "shunt", data = "OFF"))
        } else if (!shuntOn && currentTemp.compareTo(shuntLevel) < 0) {
            shuntPin.high()
            shuntOn = true
            logger.debug("Shunt pin $shuntPin set to high.")
            serverEvents.addEvent(ServerEvent(category = "shunt", data = "ON"))
        } else {
            logger.trace("Ignore this event. shuntOn=${shuntOn}, shuntLevel=${shuntLevel}, current=$currentTemp")
        }
    }

    fun levelDown() {
        changeLevel(shuntLevel - BigDecimal.ONE)
    }
}

