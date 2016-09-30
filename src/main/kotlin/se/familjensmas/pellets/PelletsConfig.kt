package se.familjensmas.pellets

import com.pi4j.io.gpio.*
import com.pi4j.wiringpi.Gpio
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.embedded.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import se.familjensmas.pi4j.DS1820Reader
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import javax.annotation.PostConstruct
import javax.annotation.Resource

@Configuration
open class PelletConfig {

    private val logger = LoggerFactory.getLogger(PelletConfig::class.java)
    private lateinit var serverEvents: ServerEventManager
    @Resource lateinit var queue: TaskQueue
    lateinit var indoor: ListenerManager<BigDecimal>
    @Value("\${logdir}") lateinit var logDirectory: File
    @Value("\${temp_file_outdoor}") lateinit var outdoorFile: File
    @Value("\${temp_file_indoor}") lateinit var indoorFile: File
    @Value("\${temp_file_coldwater}") lateinit var coldwaterFile: File
    @Value("\${temp_file_hotwater}") lateinit var hotwaterFile: File
    @Value("\${temp_file_tankwater}") lateinit var tankwaterFile: File
    var tempIndex = 0

    @PostConstruct
    open fun init() {
        serverEvents = ServerEventManager(java.util.concurrent.Executor { queue.execute(it) })

        val temps = arrayOf(createT("indoor", indoorFile),
                createT("outdoor", outdoorFile),
                createT("coldwater", coldwaterFile),
                createT("hotwater", hotwaterFile),
                createT("tankwater", tankwaterFile))
        indoor = temps[0]

        var tempIndex = 0
        var tempUpdater: () -> Unit = {}
        tempUpdater = {
            tempIndex++
            if (tempIndex == temps.size) tempIndex = 0
            temps[tempIndex].update()
            queue.schedule(Duration.ofSeconds(1), "Schedule next temp update.") {
                queue.executeWhenEmpty("Temp updater", tempUpdater)
            }
        }
        Thread {
            Thread.sleep(5000)
            queue.executeWhenEmpty("Temp Updater", tempUpdater)
        }.start()

    }

    inner class T(val name: String, val file: File) : ListenerManager<BigDecimal>(queue) {
        val reader = DS1820Reader()
        var last = BigDecimal(-500)

        init {
            reader.tempfile = file.absolutePath
        }

        fun update() {
            try {
                val current = reader.currentTemperature
                if (last.compareTo(current) != 0) {
                    logger.debug("$name is updated from $last to $current")
                    last = current
                    sendEvent(current)
                }
            } catch(e: Exception) {
                logger.debug("Failed to read $name: $e")
            }
        }

        override fun toString(): String = "Read current temp for $name"

    }


    private fun createT(name: String, file: File): T {
        val t = T(name, file)
        val tempLog = TempLog(logDirectory, name)
        t.addListener(object : Listener<BigDecimal> {
            override fun event(event: BigDecimal) {
                tempLog.notify(event)
            }

            override fun toString(): String = "Update temp log."

        })
        t.addListener(object : Listener<BigDecimal> {
            override fun event(event: BigDecimal) {
                serverEvents.addEvent(ServerEvent(category = name, data = event.toString()))
            }

            override fun toString(): String = "Update SSE."
        })
        return t
    }

    @Bean
    open fun indoor(): ListenerManager<BigDecimal> {
        return indoor
    }

    @Bean
    open fun serverEventManager(): ServerEventManager {
        return serverEvents
    }

    @Bean
    open fun servletRegistrationBean(): ServletRegistrationBean {
        return ServletRegistrationBean(serverEvents, "/events")
    }

    @Bean(name = arrayOf("shuntpin"))
    open fun shuntpin(): WriteablePin {
        val pinnr = RaspiPin.GPIO_01
        val gpioFac = GpioFactory.getInstance()
        return WriteableGpioPinWrapper(gpioFac.provisionDigitalOutputPin(pinnr, "shunt", PinState.LOW))
    }

    @Bean
    open fun skruvPin(): GpioPinDigitalInput {
        val pinnr = RaspiPin.GPIO_03
        val gpioFac = GpioFactory.getInstance()
        val p = gpioFac.provisionDigitalInputPin(pinnr, "skruvdetektor", PinPullResistance.PULL_UP)
        return p
    }

    @Bean
    open fun shuntLevelStorage(): ShuntLevelStorage {
        val SHUNT_LEVEL_FILENAME = "shuntlevel.txt"

        return object : ShuntLevelStorage {

            override fun readShuntLevel(): BigDecimal {
                return BigDecimal(String(Files.readAllBytes(Paths.get(SHUNT_LEVEL_FILENAME))))
            }

            override fun save(shuntLevel: BigDecimal) {
                Paths.get(SHUNT_LEVEL_FILENAME).toFile().writeBytes(shuntLevel.toString().toByteArray())
            }

        }

    }
}

