package se.familjensmas.pellets

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert
import java.math.BigDecimal

class TempLogTest {

	@Test
	fun testSome() {
		val logDir = File("/tmp")
		SystemTime.useFixed = true
		SystemTime.fixedTime = LocalDateTime.now().withHour(13).withMinute(10).withSecond(5)

		LogFile.create(logDir, SystemTime.fixedTime.toLocalDate(), "temptest").delete()
		
		val s = TempLog(logDir, "temptest")

		s.notify(BigDecimal(3))

		SystemTime.fixedTime = SystemTime.fixedTime.withHour(14).withMinute(10).withSecond(10)
		s.notify(BigDecimal(4))
		s.notify(BigDecimal(5))
		s.notify(BigDecimal(6))

		SystemTime.fixedTime = SystemTime.fixedTime.withHour(15).withMinute(10).withSecond(10)
		s.notify(BigDecimal(10))
		
		Assert.assertEquals(30, LogFile.create(logDir, SystemTime.fixedTime.toLocalDate(), "temptest").valueForHour(SystemTime.fixedTime.withHour(13).withMinute(10).withSecond(5)))
		Assert.assertEquals(50, LogFile.create(logDir, SystemTime.fixedTime.toLocalDate(), "temptest").valueForHour(SystemTime.fixedTime.withHour(14).withMinute(10).withSecond(5)))
		
	}
}
