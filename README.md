[![Dependency review](https://github.com/TerryEbdon/YtToZara/actions/workflows/dependency-review.yml/badge.svg)](https://github.com/TerryEbdon/YtToZara/actions/workflows/dependency-review.yml)

# YtToZara

YtToZara is a command line tool that converts a YouTube playlist to a ZaraRadio
playlist, downloading tracks as required. Tracks are downloaded in MP3 format
using [yt-dlp]. Silence is trimmed from the start and end of each track using
[ffmpeg and ffprobe].

## Restrictions

This tool creates ZaraRadio `.lst` playlists. ZaraRadio is a Windows only app.
YtToZara has been tested with Windows 10 and Windows 11.

- YtToZara has been tested with [ZaraRadio 1.6.2 Free Edition].
- It has **not** been tested with ZaraStudio

### Non-Latin character sets

ZaraRadio will not play tracks that have non-Latin characters in the filename.
To work around this all punctuation characters and non-Latin characters are
converted to underscores.

## Prerequisites

### Java 17

YtToZara depends on Java 17, which must be on the Windows path.

### A recent version of Deno

Version 2.0.0, or later, of Deno is required. The latest version is
recommended.

Install Deno with the following command:

```Batchfile
winget install --id=DenoLand.Deno
```

### Other software

Earlier versions of YtToZara required [yt-dlp], [ffmpeg and ffprobe] to be on
the Windows path. Starting with v2.0.0 that's no longer a requirement, as
YtToZara will download its own copies of these apps.

## Installing YtToZara

1. Make sure that Java 17 is on the path.
2. Make sure that Deno is on the path.
3. Download the [latest release] (scroll down to `Assets` to find the ZIP file.)
4. Unzip the downloaded file into a folder.
5. Add the `YtToZara` folder to the user path.

## Usage

1. Use the command `YtToZaraUI` from a command prompt.
2. YtToZara will download its dependencies, if required.
3. When prompted, enter the URL of a YouTube playlist.

**Note:**
The playlist must be public or unlisted. This app does **not** work with private
playlists.

4. YtToZara will download the metadata and audio streams.
5. The audio files will be converted to MP3s.

**Note:**
The MP3s will have a bit-rate of 128 kbps. This is the highest bit-rate that
YouTube provides for free accounts.

6. YtToZara will trim silence from the beginning and end of each track. This
   works fine for speech and most music. If it causes you problems then please
   [log an issue] and I'll look into it.

7. YtToZara will normalise every track.
8. A ZaraRadio playlist will be created. The playlist will have the same name as
   the YouTube playlist.

[yt-dlp]: https://github.com/yt-dlp/yt-dlp
[ffmpeg and ffprobe]: https://www.ffmpeg.org/
[ZaraRadio 1.6.2 Free Edition]: http://www.zarastudio.es/download.php
[latest release]: https://github.com/TerryEbdon/YtToZara/releases/latest
[log an issue]: https://github.com/TerryEbdon/YtToZara/issues/new
