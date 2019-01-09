package com.sheinh.ytsplit

import java.io.BufferedReader
import java.io.InputStreamReader

var YOUTUBE = "youtube-dl"
var FFMPEG = "ffmpeg"
val isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
internal var WINDOWS_ARGS = if (isWindows) arrayOf("cmd.exe", "/c") else emptyArray()
val Process.input: String
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
val Process.error: String
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

fun ProcessBuilder.loadEnv(): ProcessBuilder {
    environment()["PATH"] = System.getenv("PATH")
    return this
}