package com.sheinh.ytsplit

import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import com.google.gson.JsonParser
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileNotFoundException
import java.util.regex.Pattern
import java.nio.file.Files



object YouTubeDL {
    data class Encoding(val bitrate : Int, val codec : Codec)
    enum class Codec(val extension : String){
        opus("ogg"),m4a("m4a"),mp3("mp3");
        override fun toString(): String{
            return name
        }
    }
    lateinit var url : String
    val description
    get() = getJsonProperty("description")
    val title
    get() = getJsonProperty("title")
    val json by lazy {
        JsonParser().parse(run("youtube-dl","-J",url).input).asJsonObject
    }
    val encoding : Encoding
    get(){
        try {
            val codecStr = getJsonProperty("acodec")!!.toLowerCase()
            val codec = when{
                codecStr.contains("m4a") -> Codec.m4a
                else -> Codec.opus
            }
            val br = getJsonProperty("abr")?.toInt()
            return Encoding(br!!,codec)
        }
        finally {
            return Encoding(192,Codec.opus)
        }
    }
    const val YOUTUBEDL = "youtube-dl"
    const val BESTAUDIO = "bestaudio"

    fun ProcessBuilder.loadEnv() : ProcessBuilder {
        environment().put("PATH", System.getenv("PATH"))
        return this
    }
    private fun run(dir : File, command : List<String>): Process {
        val pb = ProcessBuilder()
        pb.command(command)
        pb.environment().put("PATH", System.getenv("PATH"))
        pb.directory(dir)
        val process = pb.start()
        val errCode = process.waitFor()
        return process
    }
    private fun run(vararg command: String): Process {
        val pb = ProcessBuilder()
        pb.command(*command)
        pb.environment().put("PATH", System.getenv("PATH"))
        val process = pb.start()
        val errCode = process.waitFor()
        return process
    }
    private fun getJsonProperty(name : String): String? {
        return json.get(name).asString
    }
    fun download(destination : File): String {
        if(destination.exists() && destination.isDirectory) {
            val proc = run(destination, listOf(YOUTUBEDL, "-f", BESTAUDIO, "--write-info-json", url))
            val out = proc.input
            var matcher = Pattern.compile("Destination: (.+)\\n").matcher(out)
            matcher.find()
            val outputFile = matcher.group(1)
            matcher = Pattern.compile("[info] Writing video description metadata as JSON to: (.+)\\n").matcher(out)
            matcher.find()
            return outputFile
        }
        else
            throw FileNotFoundException()
    }
    fun split(input : File, songs : List<Song> , encoding: Encoding){
        val encodingParameters = when(encoding.codec){
            Codec.opus -> listOf("-b:a",encoding.bitrate.toString() + "k","-c:a","libopus")
            else -> listOf("-b:a",encoding.bitrate.toString() + "k")
        }
        songs.parallelStream().forEach{
            val dir = input.parentFile
            val outFileName = String.format("%02d", it.trackNo) + ". ${it.artist} - ${it.song}.${encoding.codec.extension}"

            val pbuilder = ProcessBuilder().loadEnv()
            val command = ArrayList<String>(10)
            pbuilder.directory(dir)
            command.addAll(listOf("ffmpeg","-i",input.name))
            encodingParameters.forEach { command.add(it) }
            command.addAll(listOf("-ss",it.timestamp.toString()))
            if(it.endTime != null)
                command.addAll(listOf("-to",it.endTime.toString()))
            command.add(outFileName)
            pbuilder.command(command)
            val proc = pbuilder.start()
            proc.waitFor()
            if(proc.exitValue() != 0){
                println(proc.error)
            }
            it.audioFile = AudioFileIO.read(File(input.parentFile.path + File.separator + outFileName))
            it.writeTag()
            it.audioFile.commit()
        }
    }
    fun writeAlbum(song : Song, album : String){
        song.audioFile.tag.setField(FieldKey.ALBUM,album)
        song.audioFile.commit()
    }
}

private val Process.input: String
    get() {
        val stringBuilder = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.append(line).append(System.lineSeparator())
            line = bufferedReader.readLine()
        }
        bufferedReader.close()
        return stringBuilder.toString()
    }
private val Process.error: String
    get() {
        val stringBuilder = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(errorStream))
        var line: String? = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.append(line).append(System.lineSeparator())
            line = bufferedReader.readLine()
        }
        bufferedReader.close()
        return stringBuilder.toString()
    }