package se.familjensmas.pellets

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoField

object SystemTime {

    private var extra = 0L
    var fixedTime: LocalDateTime = LocalDateTime.MIN
        set(value) {
            field = value
            useFixed = true
        }
    var useFixed = false

    fun reset() {
        useFixed = false
        extra = 0
    }

    fun nowAsLocalDateTime(): LocalDateTime {
        if (useFixed) {
            return fixedTime
        } else {
            return LocalDateTime.now().plus(extra, ChronoField.MILLI_OF_DAY.getBaseUnit())
        }
    }

    fun jumpForward(time: Duration) {
        extra += time.toMillis()
    }

    fun now(): Long =
            if (useFixed)
                fixedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            else
                System.currentTimeMillis() + extra

    fun wait(lock: Object) {
        lock.wait()
    }

    fun wait(lock: Object, time: Duration) {
        if (time.toMillis() > 0)
            lock.wait(time.toMillis())
    }

}