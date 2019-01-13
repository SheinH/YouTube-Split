package com.sheinhtike.ytsplit

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
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


object Dependencies {
	lateinit var ffmpegPath : Path
	lateinit var youtubeDLPath : Path

	val arePresent by lazy {
		var yt = isInPath("youtube-dl")
		var ffmpeg = isInPath("ffmpeg")
		if (OS.isWindows) {
			yt = File("youtube-dl.exe").exists()
			ffmpeg = File("ffmpeg.exe").exists()
			if (ffmpeg) {
				if (File("ffmpeg.zip").exists()) File("ffmpeg.zip").delete()
			}
		}
		return@lazy yt && ffmpeg
	}

	fun loadPaths() {
		if (OS.isWindows) {
			val ytPath = Paths.get("ffmpeg.exe")
			val ffPath = Paths.get("youtube-dl.exe")
			if (ytPath == null || ffPath == null) {
				throw FileNotFoundException("Files not found in path")
			}
			ffmpegPath = ffPath
			youtubeDLPath = ytPath
		} else {
			val directories = ArrayList(System.getenv("PATH").split(File.pathSeparator))
			val paths = ArrayList(directories.map { Paths.get(it) })
			paths.add(Paths.get("."))
			val ytPath = paths.findLast { Files.exists(it.resolve("youtube-dl")) }
			val ffPath = paths.findLast { Files.exists(it.resolve("ffmpeg")) }
			if (ytPath == null || ffPath == null) {
				throw FileNotFoundException("Files not found in path")
			}
			ffmpegPath = ffPath.resolve("ffmpeg")
			youtubeDLPath = ytPath.resolve("youtube-dl")
		}
	}

	private fun decompress() {
		fun decompressFile(zis : ZipInputStream) {
			val buffer = ByteArray(1024)
			val newFile = File("ffmpeg.exe")
			val fos = FileOutputStream(newFile)
			var len : Int
			len = zis.read(buffer)
			while (len > 0) {
				fos.write(buffer, 0, len)
				len = zis.read(buffer)
			}
			zis.closeEntry()
			fos.close()
		}

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
		zis.closeEntry()
		zis.close()
		fis.close()
		try {
			if (File("ffmpeg.zip").exists()) File("ffmpeg.zip").delete()
		} finally {
		}
	}


	fun showMacInstructions(stage : Stage) {
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

	fun getDependenciesWin() {
		if (!isInPath("ffmpeg") && OS.isWindows) {
			val localFile = Paths.get(".", "ffmpeg.exe")
			if (!localFile.exists()) {
				downloadZip()
				decompress()
			}
			ffmpegPath = localFile.toAbsolutePath()
		}
		if (!isInPath("youtube-dl") && OS.isWindows) {
			val localFile = Paths.get(".", "youtube-dl.exe")
			if (!localFile.exists()) {
				extractYoutubeDL()
			}
			youtubeDLPath = localFile.toAbsolutePath()
		}
	}

	private fun isInPath(command : String) : Boolean {
		if (File(command).exists()) return true
		val filename = if (OS.isWindows) "$command.exe" else command
		val directories = ArrayList(System.getenv("PATH").split(File.pathSeparator))
		directories.add(System.getProperty("user.dir"))
		val paths = directories.map { Paths.get(it, filename) }
		val out = paths.find { it.exists() }
		println(out)
		return out != null
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

