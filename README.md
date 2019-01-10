# YouTube-Split
YouTube Split is an application for ripping full-length albums off youtube
![Initial View](https://i.imgur.com/ymekLAD.png)

The program works by downloading the description of a video.
If the video has timestamps in the description paired with song names, YouTube Split can read this data.
The program pattern matches the description based on the following syntax:

Example Pattern:
```{TIMESTAMP} {ARTIST} - {SONG}```


![Tracks](https://i.imgur.com/xNo8nQH.png)

A track listing is automatically generated by the program. Track titles, artists, and album names can be edited next.
The cover art can also be changed. The user must then select an audio format and bitrate to save as, along with a target directory.

![Output](https://i.imgur.com/X37DL9S.png)

Finally, after hitting download, the program outputs each song to the target folder as separate files. 
Each of the filenames are fully formatted and each file comes with metadata info along with cover art.
This means that other programs and devices can recognize these music files.
