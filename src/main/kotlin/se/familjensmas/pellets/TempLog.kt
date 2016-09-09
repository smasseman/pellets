package se.familjensmas.pellets

import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit

class TempLog (val directory : File, val name : String) {
	
	private val logger = LoggerFactory.getLogger(this.javaClass)
	private var loggingHour = SystemTime.nowAsLocalDateTime().truncatedTo(ChronoUnit.HOURS)
	private var tempSum = BigDecimal(0)
	private var tempCount = 0
	
	fun notify(currentTemp : BigDecimal) {
		val now = SystemTime.nowAsLocalDateTime()
		logger.debug("now=$now logginghour=$loggingHour temp=$currentTemp")
		if( !now.truncatedTo(ChronoUnit.HOURS).equals(loggingHour)) {
			tempSum = BigDecimal.ZERO
			tempCount = 0			
			loggingHour = now.truncatedTo(ChronoUnit.HOURS)
		}
		tempSum += currentTemp
		tempCount++
		val toLog = (tempSum.times(BigDecimal.TEN) ).divide(BigDecimal(tempCount), 0, RoundingMode.HALF_DOWN)
		LogFile.create(directory, loggingHour.toLocalDate(), name).set(loggingHour.toLocalTime(), toLog.intValueExact()).save()
	}
}