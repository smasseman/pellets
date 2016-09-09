package se.familjensmas.pellets

import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.GpioPinDigitalInput
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import se.familjensmas.pi4j.RaspiProviderSimulator
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse

@Controller
class PelletsController public @Autowired constructor(val shunt: Shunt, val skruvPin: GpioPinDigitalInput) {

    private val logger = LoggerFactory.getLogger(javaClass.name)
    @Value("\${pwdFile}") lateinit var pwdFile: File

    @RequestMapping("/")
    fun index(response: HttpServletResponse) {
        logger.debug("Redirecting to index.")
        response.sendRedirect("index.html")
    }

    @RequestMapping("/pin/{pin}/{state}")
    @ResponseBody
    fun setPinState(@PathVariable("pin") pin: String, @PathVariable("state") state: PinState): Boolean {
        val p = RaspiPin.getPinByName("GPIO " + pin)
        val f = GpioFactory.getDefaultProvider()
        if (f is RaspiProviderSimulator) {
            logger.debug("Setting $p to $state")
            f.simulateStateChange(p, state)
            return true
        } else {
            return false
        }
    }

    @RequestMapping("/shunt/up")
    fun shuntUp(resp: HttpServletResponse, @RequestParam pwd: String) {
        shuntAction(resp, pwd) {
            shunt.levelUp()
        }
    }
    @RequestMapping("/shunt")
    fun shuntUp(resp: HttpServletResponse, @RequestParam pwd: String, @RequestParam value: Integer) {
        shuntAction(resp, pwd) {
            shunt.changeLevel(BigDecimal(value.toInt()))
        }
    }

    @RequestMapping("/shunt/down")
    fun shuntLowDown(resp: HttpServletResponse, @RequestParam pwd: String) {
        shuntAction(resp, pwd) {
            shunt.levelDown()
        }
    }

    private fun shuntAction(resp: HttpServletResponse, pwd: String, action: () -> Unit) {
        val real = String(Files.readAllBytes(pwdFile.toPath())).trim()
        if (real == pwd) {
            action.invoke()
        } else {
            resp.sendError(401, "Invalid password.")
        }
    }

    @RequestMapping("/currenttime")
    @ResponseBody
    fun time(): LocalDateTime {
        return SystemTime.nowAsLocalDateTime()
    }

    @RequestMapping("/plustime/{duration}/{unit}")
    @ResponseBody
    fun plusTime(@PathVariable("duration") duration: Long, @PathVariable("unit") unit: TimeUnit): Boolean {
        SystemTime.jumpForward(Duration.ofMillis(unit.toMillis(duration)))
        return true
    }

    @RequestMapping("/simulate/skruv/{state}")
    @ResponseBody
    fun simulateSkruvState(@PathVariable("state") state: PinState): Boolean {
        val gpioFac = GpioFactory.getDefaultProvider()
        if (gpioFac is RaspiProviderSimulator) {
            gpioFac.simulateStateChange(skruvPin.pin, state)
            logger.info("Simulated new pin state ${skruvPin.state}")
            return true
        } else {
            logger.info("Can not simualte state change with gpio factory is " + gpioFac.javaClass)
            return false
        }
    }
}