package com.sheinhtike.ytsplit

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


object OS {
	val isWindows = System.getProperty("os.name").toLowerCase().contains("windows")
	val isMac : Boolean

	init {
		val os = System.getProperty("os.name").toLowerCase()
		isMac = (os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0)
	}
}

var currentDir : Path = Paths.get(".")


internal var WINDOWS_ARGS = if (OS.isWindows) arrayOf("cmd.exe", "/c") else emptyArray()
val Process.input : String
	get() {
		val stringBuilder = StringBuilder()
		val bufferedReader = BufferedReader(InputStreamReader(inputStream))
		var line : String? = bufferedReader.readLine()
		while (line != null) {
			stringBuilder.append(line).append(System.lineSeparator())
			line = bufferedReader.readLine()
		}
		bufferedReader.close()
		return stringBuilder.toString()
	}
val Process.error : String
	get() {
		val stringBuilder = StringBuilder()
		val bufferedReader = BufferedReader(InputStreamReader(errorStream))
		var line : String? = bufferedReader.readLine()
		while (line != null) {
			stringBuilder.append(line).append(System.lineSeparator())
			line = bufferedReader.readLine()
		}
		bufferedReader.close()
		return stringBuilder.toString()
	}

fun Path.exists() = Files.exists(this)


fun ProcessBuilder.loadEnv() : ProcessBuilder {
	environment()["PATH"] = System.getenv("PATH")
	return this
}

fun sanitizeFilename(inputName : String) : String {
	fun sanitizeMac(input : String) : String {
		return input.replace(":", "-").replace("/", ":")
	}

	fun sanitizeWin(input : String) : String {
		return input.replace("[/\\\\:*?\"<>|]".toRegex(), "-")
	}

	fun sanitizeLin(input : String) : String {
		return input.replace("[/><|:&]".toRegex(), "-")
	}
	return when {
		OS.isMac     -> sanitizeMac(inputName)
		OS.isWindows -> sanitizeWin(inputName)
		else         -> sanitizeLin(inputName)
	}
}
