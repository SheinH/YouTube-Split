package com.sheinh.ytsplit

import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream



object Dependancies {
    fun decompress() {
        if(File("ffmpeg.exe").exists())
            return
        val fileZip = "ffmpeg.zip"
        val zis = ZipInputStream(FileInputStream(fileZip))
        var zipEntry: ZipEntry? = zis.nextEntry
        while (zipEntry != null) {
            if (zipEntry.name.contains(Regex("ffmpeg\\.exe$"))) {
                decompressFile(zis, zipEntry)
            }
            zipEntry = zis.nextEntry
        }
    }

    fun decompressFile(zis: ZipInputStream, zipEntry: ZipEntry) {
        val buffer = ByteArray(1024)
        val newFile = File("ffmpeg.exe")
        val fos = FileOutputStream(newFile)
        var len: Int
        len = zis.read(buffer)
        while (len > 0) {
            fos.write(buffer, 0, len)
            len = zis.read(buffer)
        }
        fos.close()
        val zip = File("ffmpeg.zip").toPath()
        Files.deleteIfExists(zip)
    }

    fun extractYoutubeDL(){
        if(File("youtube-dl.exe").exists())
            return
        val `is` = javaClass.getResource("/youtube-dl.exe").openStream()
//sets the output stream to a system folder
        val os = FileOutputStream("youtube-dl.exe")

//2048 here is just my preference
        val b = ByteArray(2048)
        var length: Int
        length = `is`.read(b)
        while (length != -1) {
            os.write(b, 0, length)
            length = `is`.read(b)
        }

        `is`.close()
        os.close()
    }

    fun downloadZip() {
        data class Link(val filename : String, val date : Date)
        if(File("ffmpeg.zip").exists())
            return
        val url: URL
        var inputStr: InputStream? = null
        val br: BufferedReader
        var line: String

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
            val matcher = Pattern.compile("a href=\"([^\"]+)\".+<td>(\\d{4}-\\w{3}-\\d{1,2} \\d{2}:\\d{2})").matcher(out)
            val links = ArrayList<Link>()
            matcher.find()
            while(!matcher.hitEnd()){
                val date = form.parse(matcher.group(2))
                links += Link(matcher.group(1),date)
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
        } catch (mue: MalformedURLException) {
            mue.printStackTrace()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } finally {
            try {
                inputStr?.close()
            } catch (ioe: IOException) {
                // nothing to see here
            }

        }
    }
}

