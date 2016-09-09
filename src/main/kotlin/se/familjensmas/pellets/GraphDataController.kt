package se.familjensmas.pellets

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId

@Controller
class GraphDataController public @Autowired constructor(@Value("\${logdir}") val logDirectory : File) {
	
	val logger = LoggerFactory.getLogger(this.javaClass)
	
	@RequestMapping("/graphdata") 
	@ResponseBody
	fun get() : ObjectNode {
		val factory : JsonNodeFactory = JsonNodeFactory.instance
		val root = factory.objectNode()
		for(name in arrayOf("outdoor","indoor", "tankwater", "hotwater", "coldwater", "skruv")) {
			val from = SystemTime.nowAsLocalDateTime().minusDays(10).toLocalDate()
			val to = SystemTime.nowAsLocalDateTime().toLocalDate()
			val logs = LogFile.createAll(logDirectory, name, from, to)
			logger.debug("Found logfile: $logs")
			var out = factory.arrayNode()
			for(log in logs) {
				val values : List<Pair<LocalDateTime, Int>> = log.values()
				for(value in values ) {
					val ts = value.first.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
					var v = BigDecimal(value.second).divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP).toFloat()
					if( name == "skruv" )
                        //Skruv data is seconds per hour
                        out.addArray().add(ts).add((v/60).toInt())
                    else
                        out.addArray().add(ts).add(v)
				}
			}
			root.set(name, out)
		}
		return root
	}
}