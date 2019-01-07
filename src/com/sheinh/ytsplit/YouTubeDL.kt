package com.sheinh.ytsplit

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.nio.file.Path

class YouTubeDL {
    lateinit var url : String
    lateinit var json: JsonObject
    lateinit var audioFile : Path
    lateinit var outputFiles : HashMap<Song,Path>
    private val YOUTUBE get() = "youtube-dl"
    private val WINDOWS_ARGS = if(isWindows) arrayOf("cmd.exe","/c") else arrayOf()
    val acodec : String
    get(){
        val acd = getProperty("acodec")
        return when{
            acd == null -> "opus"
            acd.contains("m4a") -> "m4a"
            acd.contains("opus") -> "opus"
            else -> "opus"
        }
    }

    fun loadJsonData(){
        val pb = ProcessBuilder().loadEnv()
        pb.command(*WINDOWS_ARGS,YOUTUBE, "-J","-f","bestaudio",url)
        val process = pb.start()
        if(!isWindows)
            process.waitFor()
        val input = process.input
        println(input)
        json = JsonParser().parse(input).asJsonObject
    }

    fun getProperty(property : String) : String? = json.get(property).asString

    fun download(){
        val dest = File.createTempFile("","." + getProperty("ext"))
        val pb = ProcessBuilder().loadEnv()
        pb.command(*WINDOWS_ARGS,YOUTUBE, "-f","bestaudio",url,"-o",dest.path)
        val process = pb.start()
        if(!isWindows)
            process.waitFor()
        audioFile = dest.toPath()
    }

    fun writeTag(song : Song){
        val file = outputFiles[song]?.toFile()
        if(file!= null){
            val audiofile = AudioFileIO.read(file)
            song.writeTag(audiofile)
        }
    }

    fun save(directory : Path, encoding : String, bitrate : Int, songs : List<Song>){
        val encodingParameters = when(encoding){
            "opus" -> listOf("-b:a",bitrate.toString() + "k","-c:a","libopus")
            else -> listOf("-b:a",bitrate.toString() + "k")
        }
        outputFiles = HashMap()
        songs.forEach{
            val ext = when(encoding){
                "opus" -> ".ogg"
                else -> encoding
            }
            val outFileName = String.format("%02d", it.trackNo) + ". ${it.artist} - ${it.song}.${ext}"
            val command = ArrayList<String>(10)
            command.addAll(WINDOWS_ARGS)
            command.addAll(listOf("ffmpeg","-i",audioFile.toString()))
            encodingParameters.forEach { command.add(it) }
            command.addAll(listOf("-ss",it.timestamp.toString()))
            if(it.endTime != null)
                command.addAll(listOf("-to",it.endTime.toString()))
            command.add(outFileName)
            val pb = ProcessBuilder().loadEnv()
            pb.directory(directory.toFile())
            pb.command(command)
            val proc = pb.start()
            outputFiles[it] = directory.resolve(outFileName)
            writeTag(it)
        }
    }
}