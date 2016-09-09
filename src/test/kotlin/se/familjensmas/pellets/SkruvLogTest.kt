package se.familjensmas.pellets

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert

class SkruvLogTest {

	@Test
	fun testSome() {
		val logDir = File("/tmp")
		SystemTime.useFixed = true
		SystemTime.fixedTime = LocalDateTime.now().withHour(13).withMinute(10).withSecond(5)

		LogFile.create(logDir, SystemTime.fixedTime.toLocalDate(), "skruv").delete()
		
		val s = SkruvLog(logDir)

		s.notify(false)

		SystemTime.fixedTime = SystemTime.fixedTime.withHour(13).withMinute(10).withSecond(10)
		s.notify(true)

		SystemTime.fixedTime = SystemTime.fixedTime.plusSeconds(100)
		s.notify(false)
		
		Assert.assertEquals(100, LogFile.create(logDir, SystemTime.fixedTime.toLocalDate(), "skruv").valueForHour(SystemTime.fixedTime))
		
		SystemTime.fixedTime = SystemTime.fixedTime.plusSeconds(20)
		s.notify(true)
		
		SystemTime.fixedTime = SystemTime.fixedTime.plusSeconds(30)
		s.notify(false)
		
		Assert.assertEquals(130, LogFile.create(logDir, SystemTime.fixedTime.toLocalDate(), "skruv").valueForHour(SystemTime.fixedTime))
		
		SystemTime.fixedTime = SystemTime.fixedTime.withHour(14).withMinute(55).withSecond(0)
		s.notify(true)

		SystemTime.fixedTime = SystemTime.fixedTime.withHour(17).withMinute(10).withSecond(0)
		s.notify(false)

		val log = LogFile.create(logDir, SystemTime.fixedTime.toLocalDate(), "skruv")
		Assert.assertEquals(5*60, log.valueForHour(SystemTime.fixedTime.withHour(14)))
		Assert.assertEquals(60*60, log.valueForHour(SystemTime.fixedTime.withHour(15)))
		Assert.assertEquals(60*60, log.valueForHour(SystemTime.fixedTime.withHour(16)))
		Assert.assertEquals(10*60, log.valueForHour(SystemTime.fixedTime.withHour(17)))
	}
}
