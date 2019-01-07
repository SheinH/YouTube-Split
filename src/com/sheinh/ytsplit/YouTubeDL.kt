package com.sheinh.ytsplit

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.util.regex.Pattern


class YouTubeDL {
    lateinit var url : String
    lateinit var json: JsonObject
    lateinit var audioFile : Path
    lateinit var outputFiles : HashMap<Song,Path>
    lateinit var albumArt : Path
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
        process.waitFor()
        val input = process.input
        process.waitFor()
        println(input)
        json = JsonParser().parse(input).asJsonObject
    }

    fun getProperty(property : String) : String? = json.get(property).asString

    fun fetchAlbumArt(){
        val url = URL(getProperty("thumbnail"))
        val rbc = Channels.newChannel(url.openStream())
        val matcher = Pattern.compile("\\.(.+)\$").matcher(getProperty("thumbnail"))
        matcher.find()
        val ext = matcher.group(1)
        val thumbnailFile = File.createTempFile("thumbnail",ext)
        val fos = FileOutputStream("information.html")
        fos.channel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)
    }

    fun download(){
        val dest = File.createTempFile("audio","." + getProperty("ext"))
        Files.delete(dest.toPath())
        val pb = ProcessBuilder().loadEnv()
        pb.command(*WINDOWS_ARGS,YOUTUBE, "-f","bestaudio","--no-continue","-o","${dest.path}",url)
        val process = pb.start()
        println(process.input)
        println(process.error)
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
            val ext = encoding
            val outFileName = String.format("%02d", it.trackNo) + ". ${it.artist} - ${it.song}.${ext}"
            val command = ArrayList<String>(10)
            command.addAll(WINDOWS_ARGS)
            command.addAll(listOf("ffmpeg","-i",audioFile.toString()))
            command.addAll(listOf("-metadata","ARTIST=\"me\""))
            encodingParameters.forEach { command.add(it) }
            command.addAll(listOf("-ss",it.timestamp.toString()))
            if(it.endTime != null)
                command.addAll(listOf("-to",it.endTime.toString()))
            command.add(outFileName)
            val pb = ProcessBuilder().loadEnv()
            pb.directory(directory.toFile())
            pb.command(command)
            val proc = pb.start()
            println(proc.input)
            proc.waitFor()
            println(proc.error)
            outputFiles[it] = directory.resolve(outFileName)
            writeTag(it)
        }
    }
}