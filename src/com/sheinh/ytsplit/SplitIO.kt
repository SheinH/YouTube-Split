package com.sheinh.ytsplit

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.nio.file.Path

class SplitIO {
    lateinit var url : String
    lateinit var json: JsonObject
    lateinit var audioFile : Path
    private val YOUTUBE = "youtube-dl"
    val acodec
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
        pb.command(YOUTUBE, "-J","-f","bestaudio",url)
        val process = pb.start()
        process.waitFor()
        json = JsonParser().parse(process.input).asJsonObject
    }

    fun getProperty(property : String) : String? = json.get(property).asString

    fun download(){
        val dest = File.createTempFile("","." + getProperty("ext"))
        val pb = ProcessBuilder().loadEnv()
        pb.command(YOUTUBE, "-f","bestaudio",url,"-o",dest.path)
        val process = pb.start()
        process.waitFor()
        audioFile = dest.toPath()
    }
}