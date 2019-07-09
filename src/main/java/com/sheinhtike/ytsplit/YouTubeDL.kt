package com.sheinhtike.ytsplit

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.id3.ID3v1Tag
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class YouTubeDL {
	var url: String?
		get() = urlProperty.value
		set(value) {
			urlProperty.set(value)
			jsonLoaded = false
		}
	val urlProperty = SimpleStringProperty()
	private lateinit var json: JsonObject
	var jsonLoaded = false
	private lateinit var audioFile: Path
	private lateinit var outputFiles: HashMap<Song, Path>
	val albumArtProperty = SimpleObjectProperty<Path>()
	var albumArt: Path
		get() = albumArtProperty.value
		set(value) {
			albumArtProperty.value = value
		}
	private lateinit var albumArtDefault: Path

	fun loadJsonData() {
		val pb = ProcessBuilder().loadEnv()
		pb.command(*WINDOWS_ARGS, Dependencies.youtubeDLPath.toString(), "-J", "-f", "bestaudio", url)
		println(Dependencies.youtubeDLPath.toString())
		val process = pb.start()
		val input = process.input
		println(input)
		process.waitFor()
		json = JsonParser().parse(input)
				.asJsonObject
		jsonLoaded = true
	}

	fun getProperty(property: String): String? = json.get(property).asString

	fun fetchAlbumArt() {
		val url = URL(getProperty("thumbnail"))
		val rbc = Channels.newChannel(url.openStream())
		val matcher = Pattern.compile("\\.(.{1,5})\$")
				.matcher(getProperty("thumbnail"))
		matcher.find()
		val ext = matcher.group(1)
		val thumbnailFile = File.createTempFile("thumbnail", ext)
		val fos = FileOutputStream(thumbnailFile)
		fos.channel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)
		albumArt = thumbnailFile.toPath()
		albumArtDefault = thumbnailFile.toPath()
	}

	fun setDefaultArt() {
		albumArt = albumArtDefault
	}

	private fun download() {
		val dest = File.createTempFile("audio", "." + getProperty("ext"))
		Files.delete(dest.toPath())
		val pb = ProcessBuilder().loadEnv()
		pb.command(
				*WINDOWS_ARGS, Dependencies.youtubeDLPath.toString(), "-f", "bestaudio", "--no-continue", "-o", dest.path, url
		)
		val process = pb.start()
		println(process.input)
		println(process.error)
		audioFile = dest.toPath()
	}

	private fun writeTag(song: Song, numTracks: Int) {
		val file = outputFiles[song]?.toFile()
		if (file != null) {
			when {
				file.extension == "mp3" -> {
					val audioFile = AudioFileIO.read(file) as MP3File

					// Removing ID3v1 tag
					if(audioFile.hasID3v1Tag()) {
						audioFile.delete(audioFile.iD3v1Tag)
					}

					// New ID3v1 tag
					val id3v1Tag = ID3v1Tag()

					// Setting fields
					if (song.artist.isNotBlank()) id3v1Tag.setField(FieldKey.ARTIST, song.artist)
					if (song.song.isNotBlank()) id3v1Tag.setField(FieldKey.TITLE, song.song)
					if (song.album.isNotBlank()) id3v1Tag.setField(FieldKey.ALBUM, song.album)
					if (song.trackNo != null) id3v1Tag.setField(FieldKey.TRACK, song.trackNo.toString())

					// Setting ID3v1 tag to audio file
					audioFile.iD3v1Tag = id3v1Tag

					// Removing ID3v2 tag
					if(audioFile.hasID3v2Tag()) {
						audioFile.delete(audioFile.iD3v2Tag)
					}

					// New ID3v2 tag
					val id3v2Tag = audioFile.tagOrCreateDefault

					// Setting fields
					if (song.artist.isNotBlank()) id3v2Tag.setField(FieldKey.ARTIST, song.artist)
					if (song.song.isNotBlank()) id3v2Tag.setField(FieldKey.TITLE, song.song)
					if (song.album.isNotBlank()) id3v2Tag.setField(FieldKey.ALBUM, song.album)
					if (song.trackNo != null) id3v2Tag.setField(FieldKey.TRACK, song.trackNo.toString())
					id3v2Tag.setField(FieldKey.TRACK_TOTAL, numTracks.toString())

					val art = ArtworkFactory.createArtworkFromFile(albumArt.toFile())
					id3v2Tag.setField(art)

					// Setting ID3v2 tag to audio file
					audioFile.tag = id3v2Tag

					// Commit
					audioFile.commit()
				}
				file.extension == "m4a" -> {
					val audioFile = AudioFileIO.read(file)
					val art = ArtworkFactory.createArtworkFromFile(albumArt.toFile())
					audioFile.tag.setField(art)
					if (song.artist.isNotBlank()) audioFile.tag.setField(FieldKey.ARTIST, song.artist)
					if (song.song.isNotBlank()) audioFile.tag.setField(FieldKey.TITLE, song.song)
					if (song.album.isNotBlank()) audioFile.tag.setField(FieldKey.ALBUM, song.album)
					if (song.trackNo != null) audioFile.tag.setField(FieldKey.TRACK, song.trackNo.toString())
					audioFile.tag.setField(FieldKey.TRACK_TOTAL, numTracks.toString())
					audioFile.commit()
				}
			}
		}
	}

	fun save(directory: Path, encoding: String, bitrate: Int, songs: List<Song>, updater: () -> Unit): Long {
		val startTime = System.nanoTime()
		download()
		updater()
		outputFiles = HashMap()
		val ffmpeg = FFmpeg(Dependencies.ffmpegPath.toString())
		songs.parallelStream()
				.forEach {
					// setup filename
					if (it.artist == null) it.artist = ""
					if (it.song == null) it.song = ""
					var outFileName =
							if (it.artist.isNotEmpty()) "${String.format("%02d", it.trackNo)}. ${it.song} - ${it.artist}.$encoding"
							else "${String.format("%02d", it.trackNo)}. ${it.song}.$encoding"
					outFileName = sanitizeFilename(outFileName)

					// setup command
					val builder = FFmpegBuilder()
							.setInput(audioFile.toString())
							.overrideOutputFiles(true)
							.addOutput(directory.resolve(outFileName).toString())
							.setAudioBitRate((1024 * bitrate).toLong())
							.setStartOffset(it.timestamp.totalSeconds.toLong(), TimeUnit.SECONDS)
					if (it.endTime != null) builder.addExtraArgs("-to", it.endTime.toString())
					val executor = FFmpegExecutor(ffmpeg)
					executor.createJob(builder.done())
							.run()
					outputFiles[it] = directory.resolve(outFileName)
					writeTag(it, songs.size)
					updater()
				}
		return System.nanoTime() - startTime
	}
}