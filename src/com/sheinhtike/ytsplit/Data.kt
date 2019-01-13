package com.sheinhtike.ytsplit

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import java.util.regex.Matcher
import java.util.regex.Pattern


data class Timestamp(val hours : Int = 0, val minutes : Int = 0, val seconds : Int = 0) : Comparable<Timestamp> {
	val totalSeconds by lazy { ((hours * 60) + minutes) * 60 + seconds }
	override fun compareTo(other : Timestamp) : Int = when {
		totalSeconds < other.totalSeconds -> -1
		totalSeconds == other.totalSeconds -> 0
		totalSeconds > other.totalSeconds -> 1
		else -> 0
	}

	override fun toString() : String = if (hours == 0) "$minutes:$seconds"
	else "$hours:$minutes:$seconds"
}

class Song(song : String, artist : String, timestamp : Timestamp) : Comparable<Song> {
	private val songProperty = SimpleStringProperty(song)
	private val artistProperty = SimpleStringProperty(artist)
	private val timestampProperty = SimpleObjectProperty<Timestamp>(timestamp)
	private val trackNoProperty = SimpleIntegerProperty()
	var trackNo : Int?
		get() = trackNoProperty.value
		set(value) {
			trackNoProperty.value = value
		}
	var song : String
		get() = songProperty.value
		set(value) {
			songProperty.value = value
		}
	var artist : String
		get() = artistProperty.value
		set(value) {
			artistProperty.value = value
		}
	var timestamp : Timestamp
		get() = timestampProperty.value
		set(value) {
			timestampProperty.value = value
		}

	fun songProperty() = songProperty
	fun artistProperty() = artistProperty
	fun timestampProperty() = timestampProperty
	fun trackNoProperty() = trackNoProperty


	var endTime : Timestamp? = null
	lateinit var album : String

	override fun compareTo(other : Song) : Int = timestamp.compareTo(other.timestamp)
}

internal object SongRegex {

	internal val regexMap = mapOf(
		"{TIME}" to "(?<timestamp>(?:\\d{1,2})(?::\\d\\d){1,2}) *",
		"{ARTIST}" to "(?<artist>.+)",
		"{SONG}" to "(?<song>.+)"
	)

	fun inputToRegex(input : String) : Pattern {
		if (!input.contains("{TIME}")) throw IllegalArgumentException("Bad pattern")
		val allPatterns = regexMap.keys.joinToString("|") {
			it.replace(Regex("[\\{\\}]"), "\\\\$0")
		}
		val keyOrder = regexMap.keys.filter { input.contains(it) }.sortedBy { input.indexOf(it) }
		val between = input.split(Regex(allPatterns))
		println("Keyorder: $keyOrder, Between: $between")
		val builder = StringBuilder()
		for ((index, string) in between.withIndex()) {
			val sanitized = string.replace(Regex("[-.\\+*/?\\[^\\]$(){}=!<>|:\\\\]"), "\\\\$0")
			builder.append(sanitized)
			if (index < keyOrder.size) builder.append(regexMap[keyOrder[index]])
		}
		println("Final pattern: $builder")
		return Pattern.compile(builder.toString())
	}

	fun matchSongs(matcher : Matcher) : ArrayList<Song> {

		fun stringToTimestamp(input : String) : Timestamp {
			val split = input.split(':').map { it.toInt() }
			return if (split.size == 2) Timestamp(minutes = split[0], seconds = split[1])
			else Timestamp(split[0], split[1], split[2])
		}

		fun markEndTimes(list : List<Song>) {
			for ((index, song) in list.withIndex()) {
				if (index == list.size - 1) song.endTime = null
				else song.endTime = list[index + 1].timestamp
			}
		}

		fun safeMatch(matcher : Matcher, group : String) : String {
			return try {
				matcher.group(group).trim()
			} catch (e : IllegalArgumentException) {
				""
			}
		}

		val list = ArrayList<Song>()
		matcher.find()
		while (!matcher.hitEnd()) {
			val artist = safeMatch(matcher, "artist").trim()
			val song = safeMatch(matcher, "song").trim()
			val timestampString = safeMatch(matcher, "timestamp").trim()
			list.add(Song(song, artist, stringToTimestamp(timestampString)))
			matcher.find()
		}
		if (list.isEmpty()) throw IllegalArgumentException("Bad user regex")
		list.sort()
		markEndTimes(list)
		for ((i, s) in list.withIndex()) s.trackNo = i + 1
		return list
	}
}