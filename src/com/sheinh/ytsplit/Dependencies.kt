package com.sheinh.ytsplit

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.stage.Stage
import java.awt.Desktop
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


object Dependencies {
	val check by lazy {
		var yt = checkExe("youtube-dl")
		var ffmpeg = checkExe("ffmpeg")
		if (isWindows) {
			yt == yt || File("youtube-dl.exe").exists()
			ffmpeg = ffmpeg || File("ffmpeg.exe").exists()
			if (File("ffmpeg.exe").exists()) FFMPEG = File("ffmpeg.exe").absolutePath
			if (yt && ffmpeg) {
				if (File("ffmpeg.zip").exists()) File("ffmpeg.zip").delete()
			}
		}
		if (isMac) {
			if (yt && ffmpeg) return@lazy true
			val dir = File("/usr/local/bin/").toPath()
			YOUTUBE = dir.resolve("youtube-dl").toString()
			FFMPEG = dir.resolve("ffmpeg").toString()
			yt = checkExe(YOUTUBE)
			ffmpeg = checkExe(FFMPEG)
		}
		return@lazy yt && ffmpeg
	}
	private fun decompress() {
		if (File("ffmpeg.exe").exists()) return
		val fileZip = "ffmpeg.zip"
		val fis = FileInputStream(fileZip)
		val zis = ZipInputStream(fis)
		var zipEntry : ZipEntry? = zis.nextEntry
		while (zipEntry != null) {
			if (zipEntry.name.contains(Regex("ffmpeg\\.exe$"))) {
				decompressFile(zis)
				break
			}
			zipEntry = zis.nextEntry
		}
		zis.close()
		fis.close()
		try {
			if (File("ffmpeg.zip").exists()) File("ffmpeg.zip").delete()
		} finally {
		}
	}


	fun handleMacDependencies(stage : Stage) {
		val mycontroller = object {
			@FXML
			lateinit var tf1 : TextField
			@FXML
			lateinit var tf2 : TextField
			@FXML
			lateinit var open : Button
			@FXML
			lateinit var close : Button

			@FXML
			fun initialize() {
				tf1.focusedProperty().addListener { _, _, new ->
					if (new) Platform.runLater { tf1.selectAll() }
				}
				tf1.selectionProperty().addListener { _, old, new ->
					if (old != new) Platform.runLater { tf1.selectAll() }
				}
				tf2.focusedProperty().addListener { _, _, new ->
					if (new) Platform.runLater { tf2.selectAll() }
				}
				tf2.selectionProperty().addListener { _, old, new ->
					if (old != new) Platform.runLater { tf2.selectAll() }
				}
				open.onAction = EventHandler {
					val file = File("/Applications/Utilities/Terminal.app")
					Desktop.getDesktop().open(file)
				}
				close.onAction = EventHandler {
					Platform.runLater {
						stage.close()
					}
				}
			}
		}
		val fxmlLoader = FXMLLoader(javaClass.getResource("/SetupMac.fxml"))
		fxmlLoader.setController(mycontroller)
		val root = fxmlLoader.load<Any>() as Parent
		stage.title = "Dependencies"
		stage.scene = Scene(root)
		stage.isResizable = false
		stage.show()
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
		if (File(command).exists()) return true
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

	fun downloadZip() {
		System.setProperty("http.agent", "Chrome")
		data class Link(val filename : String, val date : LocalDateTime)
		if (File("ffmpeg.zip").exists()) return
		val url : URL
		var inputStr : InputStream? = null
		val br : BufferedReader

		try {
			/*
			url = URL("https://ffmpeg.zeranoe.com/builds/win64/static/")
			val form = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm",Locale.US)
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
				println(matcher.group(2))
				println(LocalDateTime.parse("2018-May-02 07:18",form))
				val date = LocalDateTime.parse(matcher.group(2))
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
			val lastFile = url.toString() + links.last().filename*/
			val website = URL(
				if (System.getProperty("os.arch").contains("64")) "https://ffmpeg.zeranoe.com/builds/win64/static/ffmpeg-latest-win64-static.zip"
				else "https://ffmpeg.zeranoe.com/builds/win32/static/ffmpeg-latest-win32-static.zip"
			)
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

