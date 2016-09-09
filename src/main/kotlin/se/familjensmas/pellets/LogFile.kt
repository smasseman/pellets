package se.familjensmas.pellets

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

class LogFile private constructor(
						private val map : MutableMap<LocalDateTime, Int>, 
						val date : LocalDate, 
						val directory : File, 
						val name : String) {

	
	val total : Int
		get() = map.values.asSequence().sum()

	override fun toString() : String {
		return date.toString() + ":" + name
	}
	
	fun delete() {
		file(directory, date, name).delete()
	}
	
	fun save() {
		val file = file(directory, date, name)
		FileOutputStream(file).use { fileOut ->
			DataOutputStream(fileOut).use {
				for (hour in 0..23) {
					val time : LocalDateTime = date.atTime(hour, 0, 0)
					val value = map.getOrElse(time, {Int.MAX_VALUE})
					it.writeInt(value)
				}
			}
		}
		logger.debug("Written to $file: $map")
	}

	fun values() : List<Pair<LocalDateTime, Int>> {
		val result = LinkedList<Pair<LocalDateTime, Int>>()
		for (e in map.entries) {
			result.add(Pair(e.key, e.value))
		}
		return result
	}
	
	fun set(time : LocalTime, value : Int) : LogFile {
		val key = date.atTime(time).truncatedTo(ChronoUnit.HOURS)
		map.put(key, value)
		return this
	}
		
	fun add(time : LocalTime, value : Int) : LogFile {
		val key = date.atTime(time).truncatedTo(ChronoUnit.HOURS)
		val old = map.get(key)
		if( old == null ) {
			map.put(key, value)
		} else {
			map.put(key, value + old)
		}
		return this
	}
	
	//dir/name_20160128.dat
	companion object Factory {
		
		val logger : Logger = LoggerFactory.getLogger(Factory::class.java)

		private fun file(directory : File, date : LocalDate, name : String) = File(directory, "${name}_${date}.dat")
		
		fun create(directory : File, date : LocalDate, name : String) : LogFile {
			val m = LinkedHashMap<LocalDateTime, Int>()
			val dataFile = file(directory, date, name)
			if( dataFile.exists() ) {
				FileInputStream(dataFile).use { filestream ->
					DataInputStream(filestream).use {
						for (hour in 0..23) {
								val time : LocalDateTime = date.atTime(hour, 0, 0).truncatedTo(ChronoUnit.HOURS)
								val value = it.readInt()
								if( value != Int.MAX_VALUE)
									m.put(time, value)	
							}
					}
				}
			} else {
				//println("Created new empty log file backed with $dataFile")
			}
			return LogFile(m, date, directory, name)
		}
		
		fun createAll(directory : File, name : String, from : LocalDate, to : LocalDate) : List<LogFile> {
			val list = LinkedList<LogFile>()
			var current = from
			while( !current.isAfter(to) ) {
				val file = file(directory, current, name)
				if( file.canRead() ) 
					list.add(create(directory, current, name))
				else 
					logger.debug("No file: " + file)
				current = current.plusDays(1)
			}
			return list
		}
	}
	
	fun valueForHour(time : LocalDateTime) : Int {
		return map.getOrElse(time.truncatedTo(ChronoUnit.HOURS), {0})
	}
}