# YtToZara

YtToZara is a command line tool that converts a YouTube playlist to a ZaraRadio playlist, downloading tracks as required.
Tracks are downloaded in MP3 format using [yt-dlp](https://github.com/yt-dlp/yt-dlp)
[ffmpeg and ffprobe](https://www.ffmpeg.org/) are used to trim silence from the start and end of each track.

## Restrictions
This tool creates ZaraRadio `.lst` playlists. ZaraRadio is a Windows only app for radio studio automation. YtToZara has been tested ib Windows 10 and Windows 11.

- YtToZara has been tested with [ZaraRadio 1.6.2 Free Edition](http://www.zarastudio.es/download.php).
- It has **not** been tested with ZaraStudio

### Non-Latin character sets
ZaraRadio will not play tracks that have non-lLatin characters in the nane. To work around this all punctuation characters and non-Latin characters are converted to underscores.

## Prerequisites

YtToZara depends on the following software:
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) -- This must be on the Windows path
- [ffmpeg and ffprobe](https://www.ffmpeg.org/) -- Both This must be on the Windows path
- Java 17

## Installing
- Make sure all of the above prerequistes are on the path
- Download the latest package and unzip into a folder

## Usage

`>YtToZara\YtToZara.cmd <url>`
Where `<url>` is the URL of a YouTube playlist.

**Note:**
The playlist must be public or unlisted

