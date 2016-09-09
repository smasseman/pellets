package se.familjensmas.pellets

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import se.familjensmas.pi4j.RaspiProviderSimulator
import java.io.File
import java.util.*


@SpringBootApplication
open class PelletsApplication

fun random(min : Int, max : Int) : Int {
	return Random().nextInt(max-min) + min
}

fun main(args: Array<String>) {
	RaspiProviderSimulator.setupGpioSimulator()
	val os = System.getProperty("os.name")
	if (os == "Mac OS X") {
		generateFakeData()
	}
    SpringApplication.run(PelletsApplication::class.java, *args)
}

private val triple = Triple(0, 50, 60)

fun generateFakeData() {
    generate("outdoor",
            Triple(0,-3,0),
            Triple(1,-3,0),
            Triple(2,-3,0),
            Triple(3,-3,0),
            Triple(4,-3,0),
            Triple(5,-2,1),
            Triple(6,-2,1),
            Triple(7,-2,1),
            Triple(8,-1,1),
            Triple(9,-1,2),
            Triple(10,0,3),
            Triple(11,0,3),
            Triple(12,0,3),
            Triple(13,1,3),
            Triple(14,1,3),
            Triple(15,1,3),
            Triple(16,1,2),
            Triple(17,0,2),
            Triple(18,0,2),
            Triple(19,-1,1),
            Triple(20,-1,1),
            Triple(21,-2,0),
            Triple(22,-2,0),
            Triple(23,-2,0))
    generate("indoor",
            Triple(0,20,22),
            Triple(1,20,22),
            Triple(2,20,22),
            Triple(3,20,22),
            Triple(4,20,22),
            Triple(5,20,22),
            Triple(6,20,22),
            Triple(7,20,22),
            Triple(8,20,22),
            Triple(9,20,22),
            Triple(10,20,22),
            Triple(11,20,22),
            Triple(12,20,22),
            Triple(13,20,22),
            Triple(14,20,22),
            Triple(15,20,22),
            Triple(16,20,22),
            Triple(17,20,22),
            Triple(18,20,22),
            Triple(19,20,22),
            Triple(20,20,22),
            Triple(21,20,22),
            Triple(22,20,22),
            Triple(23,20,22))
    generate("coldwater",
            Triple(0,23,24),
            Triple(1,23,24),
            Triple(2,23,24),
            Triple(3,23,24),
            Triple(4,23,24),
            Triple(5,23,24),
            Triple(6,23,24),
            Triple(7,23,24),
            Triple(8,23,24),
            Triple(9,23,24),
            Triple(10,23,24),
            Triple(11,23,24),
            Triple(12,23,24),
            Triple(13,23,24),
            Triple(14,23,24),
            Triple(15,23,24),
            Triple(16,23,24),
            Triple(17,23,24),
            Triple(18,23,24),
            Triple(19,23,24),
            Triple(20,23,24),
            Triple(21,23,24),
            Triple(22,23,24),
            Triple(23,23,24))
    generate("hotwater",
            Triple(0,30,32),
            Triple(1,30,32),
            Triple(2,30,32),
            Triple(3,30,32),
            Triple(4,30,32),
            Triple(5,30,32),
            Triple(6,30,32),
            Triple(7,30,32),
            Triple(8,30,32),
            Triple(9,30,32),
            Triple(10,30,32),
            Triple(11,30,32),
            Triple(12,30,32),
            Triple(13,30,32),
            Triple(14,30,32),
            Triple(15,30,32),
            Triple(16,30,32),
            Triple(17,30,32),
            Triple(18,30,32),
            Triple(19,30,32),
            Triple(20,30,32),
            Triple(21,30,32),
            Triple(22,30,32),
            Triple(23,30,32))
    generate("tankwater",
            Triple(0,50,78),
            Triple(1,50,78),
            Triple(2,50,78),
            Triple(3,50,78),
            Triple(4,50,78),
            Triple(5,50,78),
            Triple(6,50,78),
            Triple(7,50,78),
            Triple(8,50,78),
            Triple(9,50,78),
            Triple(10,50,78),
            Triple(11,50,78),
            Triple(12,50,78),
            Triple(13,50,78),
            Triple(14,50,78),
            Triple(15,50,78),
            Triple(16,50,78),
            Triple(17,50,78),
            Triple(18,50,78),
            Triple(19,50,78),
            Triple(20,50,78),
            Triple(21,50,78),
            Triple(22,50,78),
            Triple(23,50,78))

    val now = SystemTime.nowAsLocalDateTime()
    SystemTime.fixedTime = SystemTime.nowAsLocalDateTime().minusDays(5)
    val sl = SkruvLog(File("/Users/jorgen/git/smasseman/pellets/data"))
    while( SystemTime.fixedTime.isBefore(now)) {
        val b = Math.random() > 0.99
        println("Notify $b for ${SystemTime.fixedTime} ${SystemTime.nowAsLocalDateTime()}")
        sl.notify(b)
        SystemTime.fixedTime = SystemTime.fixedTime.plusMinutes(5)
    }
    SystemTime.reset()

}

fun generate(name : String, vararg x : Triple<Int,Int,Int>) {
	var ts = SystemTime.nowAsLocalDateTime().minusDays(5)
	while( ts.isBefore(SystemTime.nowAsLocalDateTime())) {
		var min = 0
		var max = 0
		for(t in x) {
			if( t.first == ts.toLocalTime().hour ) {
				min = t.second
				max = t.third
			}
		}
		val temp = random(min, max)
		LogFile.create(File("/Users/jorgen/git/smasseman/pellets/data"), ts.toLocalDate(), name).set(ts.toLocalTime(), temp.times(10).plus(random(0,10))).save()
		ts = ts.plusHours(1)
	}

}
