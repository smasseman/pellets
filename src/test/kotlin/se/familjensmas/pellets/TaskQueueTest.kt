package se.familjensmas.pellets

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert
import java.util.*
import java.lang.Runnable
import java.time.Duration

class TaskQueueTest {

    @Test
    fun testSome() {
        val q = TaskQueue()
        var called = false
        q.execute { called = true }
        Thread.sleep(1000)
        Assert.assertTrue(called)
    }

    @Test
    fun testLowPrio() {
        val q = TaskQueue()
        val executed = LinkedList<String>()
        var slow1 = Runnable {
            Thread.sleep(500)
            executed.add("slow1")
        }
        var slow2 = Runnable {
            Thread.sleep(500)
            executed.add("slow2")
        }
        var lowPrio = Runnable {
            executed.add("lowPrio")
        }

        q.execute(slow1)
        q.executeWhenEmpty(lowPrio)
        q.execute(slow2)
        Thread.sleep(1500)
        Assert.assertEquals("slow1", executed.get(0))
        Assert.assertEquals("slow2", executed.get(1))
        Assert.assertEquals("lowPrio", executed.get(2))
    }

    @Test
    fun testScheduled() {
        val q = TaskQueue()
        val executed = LinkedList<String>()

        q.schedule(Duration.ofMillis(1000), "scheduled1") { executed.add("scheduled1") }
        q.schedule(Duration.ofMillis(1200), "scheduled2") { executed.add("scheduled2") }
        q.schedule(Duration.ofMillis(1100), "scheduled3") { executed.add("scheduled3") }
        q.execute("normal1") { executed.add("normal1"); Thread.sleep(300) }
        q.executeWhenEmpty("empty") { executed.add("empty") }
        q.execute("normal2") { executed.add("normal2"); Thread.sleep(300) }
        q.execute("normal3") { executed.add("normal3") }

        Thread.sleep(1500)
        println(executed)
        Assert.assertEquals(7, executed.size)
        Assert.assertEquals("normal1", executed[0])
        Assert.assertEquals("normal2", executed[1])
        Assert.assertEquals("normal3", executed[2])
        Assert.assertEquals("empty", executed[3])
        Assert.assertEquals("scheduled1", executed[4])
        Assert.assertEquals("scheduled3", executed[5])
        Assert.assertEquals("scheduled2", executed[6])
    }
}
