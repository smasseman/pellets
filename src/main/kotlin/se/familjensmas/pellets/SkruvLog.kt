package se.familjensmas.pellets

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class SkruvLog public @Autowired constructor(@Value("\${logdir}") val logDirectory : File){
	
	private var firstNotification = true
	private var lastNotification = LocalDateTime.now()
	private var lastState = false
	
	fun notify(currentState : Boolean) {
		if( firstNotification ) {
			firstNotification = false
		} else {
			if( currentState == lastState ) return
			if( !currentState ) {
				val now = SystemTime.nowAsLocalDateTime()
				while( !sameHourAsLastNotification(now) ) {
					writeToLog(lastNotification, secondsLeftOfHour(lastNotification))
					lastNotification = lastNotification.plusHours(1).truncatedTo(ChronoUnit.HOURS)
				}
				println("Now=$now, LastNotif=$lastNotification")
				writeToLog(now, (Duration.between(lastNotification, now).toMillis()/1000).toInt())
			}
		}
		lastState = currentState
		lastNotification = SystemTime.nowAsLocalDateTime()
	}
	
	private fun writeToLog(ts : LocalDateTime, seconds : Int ) {
		LogFile.create(logDirectory, ts.toLocalDate(), "skruv").add(ts.toLocalTime(), seconds).save()
	}
	
	private fun secondsLeftOfHour(d : LocalDateTime) : Int {
		return 60 * (59 - d.toLocalTime().minute) + (60 - d.toLocalTime().second)
	}
	
	private fun sameHourAsLastNotification(now : LocalDateTime) : Boolean {
		return now.truncatedTo(ChronoUnit.HOURS).equals(lastNotification.truncatedTo(ChronoUnit.HOURS))
	}
}
