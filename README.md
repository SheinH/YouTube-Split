# YouTube-Split
YouTube Split is an application for ripping full-length albums off youtube
![Initial View](https://i.imgur.com/Num5jno.png)

First, the user enters a YouTube URL and presses the button next to the URL field.
The application will immediately download the video's title and description.
Then the user must enter a pattern to scan the description with.

Example Pattern:
```{TIMESTAMP} {ARTIST} - {SONG}```

The pattern is matched to the description to get a track list for the album.
Each track begins at its timestamp and ends at the timestamp of the next track.

![Tracks](https://i.imgur.com/xK3lpkU.png)

The user must then choose the bitrate and format of the songs along with an output folder.
Finally, the user can press download to save the album.

![Output](https://i.imgur.com/jjUU7ld.png)

Each song is named with the track number, song title, and artist name.
The songs also contain metadata tags based on the user input which can be recognized by external applications.
