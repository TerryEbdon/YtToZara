package net.ebdon.yttozara

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project
import groovy.json.JsonSlurper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3AudioHeader
import java.util.logging.Logger
import java.util.logging.Level

/**
 * Convert a youTube playlist into a ZaraRadio playlist.
 * Trim silence from the start and end of the downloaded tracks.
 */
@SuppressWarnings('CatchException')
@groovy.util.logging.Log4j2
class YtToZara {
  private static final Logger audioTagLogger = Logger.getLogger('org.jaudiotagger')

  final String outPrefix     = 'out_'
  final String logLevel      = '-loglevel error'
  final String q             = '"'
  final String currentDir    = '.'
  final String trackFileType = 'mp3'
  final AntBuilder ant       = new AntBuilder()

  List<String> trackList        = []
  List<List<String>> zaraTracks = []
  String playlistTitle          = ''
  String zaraPlFileName         = ''

  final String timestamp
  File jsonFile
  Object ytMetadata

  static void main(String[] args) {
    YtToZara ytz = new YtToZara()
    if (args.size() == 0 ) {
      ytz.convertYtToZara()
    } else {
      if (args.size() in 1..2) {
        final String path = args.last()
        switch (args.first()) {
          case 'install-ytdlp': {
            new Installer(path).installYtDlp()
            break
          }

          case 'install-ffmpeg': {
            new FfmpegInstaller(path).install()
            break
          }

          default: {
            ytz.guessMp3Tags( args.first() )
          }
        }
      }
    }
  }

  YtToZara() {
    audioTagLogger.level = Level.SEVERE
    ant.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN

    final String tsPattern = 'yyyy-MM-dd_HH-mm-ss-SSS'
    final DateTimeFormatter fmtTs = DateTimeFormatter.ofPattern(tsPattern)

    final ZoneId zoneId                  = ZoneId.of('Etc/UTC')
    final ZonedDateTime playListFileTime = ZonedDateTime.now(zoneId)

    timestamp = playListFileTime.format(fmtTs)
  }

  void convertYtToZara() {
    tee()
    analysePlaylist()
    trimSilence()
    normalise()
    tidyOutputFolder()
  }

  void trimSilence() {
    log.info 'Trimming silence from start and end of tracks'
    new Ffmpeg().trimSilence( trackList )
  }

  void normalise() {
    log.info "Normalising ${trackList.size()} tracks"
    new Ffmpeg().normalise( trackList )
  }

  void analysePlaylist() {
    log.info 'Download complete, analysing playlist data'
    populateZaraPlaylist()
    saveZaraPlayList()
  }

  void guessMp3Tags( final String trackFileName ) {
    log.info "Guessing for $trackFileName"
    parseYouTubeMetadata( trackFileName )
    grabPlayListTitle()
    if ( ytMetadata?.playlist_index == 1 ) {
      log.info "Playlist:        $playlistTitle"
      log.info "Playlist owner:  ${ytMetadata?.playlist_uploader}"
    }
    log.debug "Track No.        ${ytMetadata?.playlist_index}"
    // log.info ytMetadata?.description
    log.debug "YT Artist: ${ytMetadata?.artist}"
    log.debug "YT Album:  ${ytMetadata?.album}"
    log.debug "YT Track:  ${ytMetadata?.track}"
    log.debug "YT Irish:  ${ytIrish()}"
    String[] trackDetails = trackFileName.split( ' - ')

    switch ( trackDetails.size() ) {
      case 0:
      case 1:
        log.debug 'Track file name missing expected separators'
        break
      case 2:
        String artist = trackDetails.first()
        String title  = trackDetails.last()
        log.debug "Artist: $artist"
        log.debug "Title:  $title"

        new Ffmpeg().applyTags( trackFileName, artist, title )
        break
      default:
        log.debug 'Too many separators to decide.'
    }
  }

  Boolean ytIrish() {
    final String irishRegex =  /(?i)(\s+|^)irish(\s+|$)/
    ytMetadata?.description?.findAll( irishRegex )
  }

  void tee() {
    final String plFileName   = "pl_${timestamp}.txt"

    log.info 'Creating playlist as files download'
    File outFile = new File( plFileName )
    String line = ''
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
    while ( line != null ) {
      line = br.readLine();
      if (line != null ) {
        teeTrack( line, outFile )
      }
    }
  }

  void teeTrack( final String line, final File outFile ) {
    final String mp3Line = line.replaceAll( /\.m4a$/, ".${trackFileType}")
    log.info "Downloading $line"
    outFile << line
    outFile << '\n'
    addToTrackList( mp3Line )
  }

  void addToTrackList( String line ) {
    trackList << line
  }

  void populateZaraPlaylist() {
    log.info 'Loading ZaraRadio playlist'
    trackList.each { String trackFileName ->
      // parseYouTubeMetadata( trackFileName )
      if ( !trackFileName?.empty ) {
        guessMp3Tags(trackFileName)
        File trackFile = new File( trackFileName )
        if ( trackFile.exists() ) {
          zaraTracks << [duration(trackFile),trackFile.absolutePath]
        } else {
          log.error "Can't find file $trackFileName"
        }
      }
    }
  }

  void saveZaraPlayList() {
    log.info "Saving playlist: $playlistTitle"
    zaraPlFileName = playlistTitle?.empty ? timestamp : playlistTitle
    zaraPlFileName += '.lst'
    ['\\','/',':',q,' '].each {
      zaraPlFileName = zaraPlFileName.replace(it, '_')
    }
    log.info "Playlist file name: $zaraPlFileName"
    File zaraPlayList = new File( zaraPlFileName )
    zaraPlayList << String.format('%d%n', zaraTracks.size())
    zaraTracks.each { track ->
      zaraPlayList << track.join('\t')
      zaraPlayList << System.lineSeparator()
    }
  }

  void grabPlayListTitle() {
    if ( playlistTitle.empty ) {
      final String ytPlTitle = ytMetadata?.playlist_title
      playlistTitle = ytPlTitle ?: ''
      log.debug "Changed playlistTitle  to $playlistTitle"
      log.debug "Changed zaraPlFileName to $zaraPlFileName"
    } else {
      log.debug 'playlist_title not found'
    }
  }

  void parseYouTubeMetadata( final String trackFileName ) {
    final String metadataFileName = jsonFileName(trackFileName)
    jsonFile = new File( metadataFileName )
    if ( jsonFile.exists() ) {
      log.debug "Parsing JSON: $metadataFileName"
      ytMetadata = new JsonSlurper().parse( jsonFile )
    } else {
      log.debug "Missing: ${jsonFile.absolutePath}"
      ytMetadata = null
    }
  }

  final String jsonFileName(final String trackFileName) {
    trackFileName.replaceAll(/\.${trackFileType}$/, '.info.json')
  }

  Long duration( File trackFile ) { // Based on SpotToZara.fixMetadata()
    AudioFile audioFile = AudioFileIO.read( trackFile )

    MP3AudioHeader audioHeader = audioFile.audioHeader
    String newLengthStr = audioHeader.trackLength
    Long newLength = newLengthStr.toLong()
    final long secsPerMin = 60
    Long mins = newLength / secsPerMin
    Long secs = newLength % secsPerMin
    log.debug "Track length: $newLengthStr = $mins mins, $secs secs"
    newLength * 1000
  }

  final String cleanFileName( String inFileName ) {
    final String prefix       = /(?i)[\(\[]/
    final String official     = /Official\s+/
    final String hd           = /(HD\s+)?/
    final String remastered   = /(remastered\s+)?/
    final String music        = /(Music\s+)?/
    final String video        = /Video/
    final String suffix       = /[\)\]](?-i)/

    final String regex = "$prefix$official$hd$remastered$hd$music$video$suffix"
    inFileName.replaceAll( regex, '').
      replaceAll(/\s+\.${trackFileType}$/,".${trackFileType}")
  }

  void tidyOutputFolder() {
    ant.delete(verbose: false) {
      fileset( dir: currentDir ) {
        include( name: '*.bak')
        include( name: '*.json')
      }
    }
  }
}
