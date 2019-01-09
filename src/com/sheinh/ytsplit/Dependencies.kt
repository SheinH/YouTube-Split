package com.sheinh.ytsplit

import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


object Dependencies {
	private fun decompress() {
		if (File("ffmpeg.exe").exists()) return
		val fileZip = "ffmpeg.zip"
		val zis = ZipInputStream(FileInputStream(fileZip))
		var zipEntry : ZipEntry? = zis.nextEntry
		while (zipEntry != null) {
			if (zipEntry.name.contains(Regex("ffmpeg\\.exe$"))) {
				decompressFile(zis)
				break
			}
			zipEntry = zis.nextEntry
		}
	}

	fun checkDependencies() : Boolean {
		val yt = checkExe("youtube-dl")
		var ffmpeg = checkExe("ffmpeg")
		if (isWindows) {
			yt == yt || File("youtube-dl.exe").exists()
			ffmpeg = ffmpeg || File("ffmpeg.exe").exists()
			if (File("ffmpeg.exe").exists()) FFMPEG = File("ffmpeg.exe").absolutePath
			if (yt && ffmpeg) {
				if (File("ffmpeg.zip").exists()) File("ffmpeg.zip").delete()
			}
		}
		return yt && ffmpeg
	}

	fun getDependencies() {
		if (!checkExe("ffmpeg") && isWindows) {
			val localFile = File("ffmpeg.exe")
			if (!localFile.exists()) {
				Dependencies.downloadZip()
				Dependencies.decompress()
			}
			FFMPEG = localFile.absolutePath
		}
		if (!checkExe("youtube-dl") && isWindows) {
			val localFile = File("youtube-dl.exe")
			if (!localFile.exists()) {
				Dependencies.extractYoutubeDL()
				YOUTUBE = "youtube-dl.exe"
			}
		}
	}


	private fun checkExe(command : String) : Boolean {
		val filename = if (isWindows) "$command.exe" else command
		val directories = ArrayList(System.getenv("PATH").split(File.pathSeparator))
		directories.add(System.getProperty("user.dir"))
		val paths = directories.map { Paths.get(it, filename) }
		val out = paths.find { Files.exists(it) }
		println(out)
		return out != null
	}

	private fun decompressFile(zis : ZipInputStream) {
		val buffer = ByteArray(1024)
		val newFile = File("ffmpeg.exe")
		val fos = FileOutputStream(newFile)
		var len : Int
		len = zis.read(buffer)
		while (len > 0) {
			fos.write(buffer, 0, len)
			len = zis.read(buffer)
		}
		fos.close()
		zis.closeEntry()
		zis.close()
		try {
			if (File("ffmpeg.zip").exists()) File("ffmpeg.zip").delete()
		} finally {
		}
	}

	private fun extractYoutubeDL() {
		if (File("youtube-dl.exe").exists()) return
		val `is` = javaClass.getResource("/youtube-dl.exe").openStream()
		val os = FileOutputStream("youtube-dl.exe")
		val b = ByteArray(2048)
		var length : Int
		length = `is`.read(b)
		while (length != -1) {
			os.write(b, 0, length)
			length = `is`.read(b)
		}

		`is`.close()
		os.close()
	}

	private fun downloadZip() {
		System.setProperty("http.agent", "Chrome")
		data class Link(val filename : String, val date : Date)
		if (File("ffmpeg.zip").exists()) return
		val url : URL
		var inputStr : InputStream? = null
		val br : BufferedReader

		try {
			url = URL("https://ffmpeg.zeranoe.com/builds/win64/static/")
			val form = SimpleDateFormat("yyyy-MMM-dd HH:mm")
			inputStr = url.openStream()  // throws an IOException
			br = BufferedReader(InputStreamReader(inputStr))

			val builder = StringBuilder()
			var line = br.readLine()
			while (line != null) {
				builder.append(line)
				builder.append('\n')
				line = br.readLine()
			}
			val out = builder.toString()
			val matcher =
				Pattern.compile("a href=\"([^\"]+)\".+<td>(\\d{4}-\\w{3}-\\d{1,2} \\d{2}:\\d{2})").matcher(out)
			val links = ArrayList<Link>()
			matcher.find()
			while (!matcher.hitEnd()) {
				val date = form.parse(matcher.group(2))
				links += Link(matcher.group(1), date)
				matcher.find()
			}
			links.sortBy {
				it.date
			}
			links.forEach { println(it.date) }
			println(links.last())
			println(builder)
			println(links[links.size - 1])
			val lastFile = url.toString() + links.last().filename
			val website = URL(lastFile)
			val rbc = Channels.newChannel(website.openStream())
			val fos = FileOutputStream("ffmpeg.zip")
			fos.channel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)
		} catch (mue : MalformedURLException) {
			mue.printStackTrace()
		} catch (ioe : IOException) {
			ioe.printStackTrace()
		} finally {
			try {
				inputStr?.close()
			} catch (ioe : IOException) {
				// nothing to see here
			}
		}
	}
}

