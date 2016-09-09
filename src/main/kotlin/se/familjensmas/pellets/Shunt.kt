package se.familjensmas.pellets

import com.pi4j.io.gpio.GpioPinDigitalOutput
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths

@Service
class Shunt @Autowired constructor(val serverEvents : ServerEventManager,
										  val taskQueue : TaskQueue,
										  @Qualifier("indoor") indoor : ListenerManager<BigDecimal>,
										  val shuntPin : GpioPinDigitalOutput) {

	private val SHUNT_LEVEL_FILENAME = "shuntlevel.txt"
	private var shuntLevel = readShuntLevel()
	private var shuntOn = false
	
	init {
		indoor.addListener(object : Listener<BigDecimal> {
			override fun event(event: BigDecimal) {
				tempChange(event)
			}
		})
		fireShuntEvent()
	}
	
	private fun tempChange(event : BigDecimal) {
		if( shuntOn && event.compareTo(shuntLevel) > 0 ) {
			shuntPin.low()
			serverEvents.addEvent(ServerEvent(category="shunt", data="OFF"))
		} else if( !shuntOn && event.compareTo(shuntLevel) < 0 ) {
			shuntPin.high()
			serverEvents.addEvent(ServerEvent(category="shunt", data="ON"))
		}
	}

	private fun readShuntLevel() : BigDecimal {
		return BigDecimal(String(Files.readAllBytes(Paths.get(SHUNT_LEVEL_FILENAME).toAbsolutePath())).trim())
	}

	private fun saveLevel() {
		Paths.get(SHUNT_LEVEL_FILENAME).toFile().writeBytes(shuntLevel.toString().toByteArray())
	}
	
	private fun fireShuntEvent() {
		serverEvents.addEvent(ServerEvent(category="shuntlevel", data=shuntLevel.toString()))
	}

	fun levelUp() {
		taskQueue.execute("Level up shunt.") {
			shuntLevel += BigDecimal.ONE
			saveLevel()
			fireShuntEvent()
		}
	}

	fun changeLevel(value: BigDecimal) {
		taskQueue.execute("Set shunt level.") {
			shuntLevel = value
			saveLevel()
			fireShuntEvent()
		}
	}

	fun levelDown() {
		taskQueue.execute({
			shuntLevel -= BigDecimal.ONE
			saveLevel()
			fireShuntEvent()
		})
	}

}