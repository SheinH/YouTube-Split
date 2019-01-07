package com.sheinh.ytsplit

import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

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
fun ProcessBuilder.loadEnv() : ProcessBuilder {
    environment().put("PATH", System.getenv("PATH"))
    return this
}