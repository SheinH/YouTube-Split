package com.sheinh.ytsplit

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.tag.FieldKey
import java.lang.IllegalArgumentException
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException


data class Timestamp(val hours : Int = 0, val minutes : Int = 0, val seconds : Int = 0) : Comparable<Timestamp> {
    val totalSeconds  by lazy {((hours * 60) + minutes) * 60 + seconds}
    override fun compareTo(other: Timestamp): Int =
        when{
            totalSeconds < other.totalSeconds -> -1
            totalSeconds == other.totalSeconds -> 0
            totalSeconds > other.totalSeconds -> 1
            else -> 0
        }
    override fun toString(): String =
        if(hours == 0)
            "${minutes}:${seconds}"
        else
            "${hours}:${minutes}:${seconds}"
}

data class Song(var song: String, var artist: String, var timestamp : Timestamp) : Comparable<Song>{
    var endTime : Timestamp? = null
    lateinit var album : String
    var trackNo = 0
    override fun compareTo(other: Song): Int = timestamp.compareTo(other.timestamp)

    fun writeTag(audioFile: AudioFile){
        audioFile.tag.setField(FieldKey.ARTIST,artist)
        audioFile.tag.setField(FieldKey.TITLE,song)
        if(trackNo!= null)
            audioFile.tag.setField(FieldKey.TRACK,trackNo.toString())
        audioFile.commit()
    }
}


internal object RegexStuff{

    const val timestampRegex = "(?<timestamp>(?:\\d{1,2})(?::\\d\\d){1,2})"
    const val artistRegex = "(?<artist>.*)"
    const val songRegex = "(?<song>.*)"

    fun inputToRegex(input : String) : Pattern {
        var string = input
        if(!string.contains("{TIME}"))
            throw IllegalArgumentException("Bad pattern")
        string = string.replace("{ARTIST}", artistRegex)
        string = string.replace("{SONG}", songRegex)
        string = string.replace("{TIME}", timestampRegex)
        string += "(?:\\r\\n|\\r|\\n)"
        println(string)
        return Pattern.compile(string)
    }

    fun matchSongs(matcher: Matcher): ArrayList<Song> {

        fun stringToTimestamp(input : String) : Timestamp {
            val split = input.split(':').map { it.toInt() }
            return if(split.size == 2)
                    Timestamp(minutes = split[0], seconds = split[1])
                else
                    Timestamp(split[0],split[1],split[2])
        }

        fun markEndTimes(list : List<Song>){
            for((index,song) in list.withIndex()){
                if(index == list.size - 1)
                    song.endTime = null
                else
                    song.endTime = list[index+1].timestamp
            }
        }

        fun safeMatch(matcher: Matcher,group : String) : String {
            try{
                return matcher.group(group).trim()
            }finally {
                return ""
            }
        }

        val list = ArrayList<Song>()
        matcher.find()
        while(!matcher.hitEnd()){
            val artist = safeMatch(matcher, "artist").trim()
            val song = safeMatch(matcher, "song").trim()
            val timestampString = safeMatch(matcher, "timestamp").trim()
            list.add(Song(song,artist,stringToTimestamp(timestampString)))
            matcher.find()
        }
        list.sort()
        markEndTimes(list)
        for((i,s) in list.withIndex())
            s.trackNo = i + 1
        return list
    }
}