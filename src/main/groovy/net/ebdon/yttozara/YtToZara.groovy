package net.ebdon.yttozara

import groovy.ant.AntBuilder
import groovy.json.JsonSlurper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3AudioHeader
import java.util.logging.Logger
import java.util.logging.Level
import java.nio.file.FileSystemException

@groovy.util.logging.Log4j2
class YtToZara {
  public static Logger audioTagLogger = Logger.getLogger('org.jaudiotagger')
  
  final String outPrefix    = 'out_'
  final AntBuilder ant      = new AntBuilder()
  def trackList             = []
  def zaraTracks            = []
  def trackDetails          = []
  String playlistTitle      = ''
  String zaraPlFileName     = ''
  
  final String timestamp
  File jsonFile
  def ytMetadata

  public static main(args) {
    YtToZara ytz = new YtToZara()
    if (args.size() == 0 ) {
      ytz.tee()
      ytz.analysePlaylist()
      ytz.trimSilence()
      ytz.tidyOutputFolder()
    } else {
      ytz.guessMp3Tags( args.first() )
    }
  }

  YtToZara() {
    audioTagLogger.setLevel(Level.WARNING)
    final String tsPattern = 'yyyy-MM-dd_HH-mm-ss-SSS'
    final DateTimeFormatter fmtTs = DateTimeFormatter.ofPattern(tsPattern)

    final ZoneId zoneId                  = ZoneId.of('Etc/UTC')
    final ZonedDateTime playListFileTime = ZonedDateTime.now(zoneId)

    timestamp = playListFileTime.format(fmtTs)
  }

  void trimSilence() {
    log.info 'Trimming silence from start and end of tracks.'
    trackList.each { String trackFileName ->
      trimAudio( trackFileName )
    }
  }

  void trimAudio( final String mp3FileName ) {
    final String logLevel = '-loglevel error'
    final String q = '"'
    final String input = "-i $q$mp3FileName$q"
    final String trimmedFileName = "trimmed_$mp3FileName"
    final String untrimmedFileName = "untrimmed_$mp3FileName"

    final String trimTrackArgs = 
      'areverse,atrim=start=0.2,silenceremove=start_periods=1:start_silence=0.1:start_threshold=0.02:stop_silence=0.5'
    final String ffmpedArgs = "$logLevel -af $q$trimTrackArgs,$trimTrackArgs$q"
    final String argsLine = "-y $input $ffmpedArgs $q$trimmedFileName$q"
    log.debug "argsLine: $argsLine"
    ant.exec (
      dir               : '.',
      executable        : 'ffmpeg',
      outputproperty    : 'trimCmdOut',
      errorproperty     : 'trimCmdError',
      resultproperty    : 'trimCmdResult'
    ) {
      arg( line: argsLine )
    }
    final int execRes       = ant.project.properties.trimCmdResult.toInteger()
    final String execOut    = ant.project.properties.trimCmdOut
    final String execErr    = ant.project.properties.trimCmdError
    log.debug "trimAudio execOut = $execOut"
    log.debug "trimAudio execErr = $execErr"
    log.debug "trimAudio execRes = $execRes"

    if ( !execErr.empty ) {
      log.error 'Could not trim audio'
      log.error execErr
      log.warn "out: $execOut"
      log.warn "result: $execRes"
    } else {
      ant.delete file: mp3FileName, verbose: false, failonerror: true
      moveFile trimmedFileName, mp3FileName
    }
  }

  void moveFile( final String fromFileName, final String toFileName ) {
    ant.move(
      file: fromFileName, tofile: toFileName,
      failonerror: true, verbose: false, overwrite: true, force:true
    )
  }

  void analysePlaylist() {
    log.info "Download complete, analysing playlist data"
    createZaraPlaylist()
    saveZaraPlayList()
  }

  void guessMp3Tags( final String trackFileName ) {
    log.info "Guessing for $trackFileName"
    parseYouTubeMetadata( trackFileName )
    grabPlayListTitle()
    log.info "Playlist:        $playlistTitle"
    log.info "Playlist owner:  ${ytMetadata?.playlist_uploader}"
    log.info "Track No.        ${ytMetadata?.playlist_index}"
    // log.info ytMetadata?.description
    log.debug "YT Arist: ${ytMetadata?.artist}"
    log.debug "YT Album: ${ytMetadata?.album}"
    log.debug "YT Track: ${ytMetadata?.track}"
    log.debug "YT Irish: ${ytIrish()}"
    def trackDetails = trackFileName.split( ' - ')

    switch( trackDetails.size() ) {
      case 0:
      case 1:
        log.warn 'Track file name missing expected seperators'
        break
      case 2:
        String artist = trackDetails.first()
        String title  = trackDetails.last()
        log.info "Artist: $artist"
        log.info "Title:  $title"
        applyTags( trackFileName, artist, title )
        break
      default:
        log.warn "Too many separators to decide."
    }
  }

  Boolean ytIrish() {
    final String irishRegex =  /(?i)(\s+|^)irish(\s+|$)/
    ytMetadata?.description?.findAll( irishRegex )
  }

  void tee() {
    final String plFileName   = "pl_${timestamp}.txt"

    log.info "Creating playlist as files download"
    File outFile = new File( plFileName )
    String line =''
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
    while ( line != null ) {
      line = br.readLine();
      if (line != null ) {
        line = line.replaceAll( /\.m4a$/, '.mp3')
        log.info "Downloading $line"
        outFile << line
        outFile << '\n'
        trackList << line
      }
    }
  }

  void createZaraPlaylist() {
    // def zaraTracks = []
    println "Loading ZaraRadio playlist"
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
    ['\\','/',':','"',' '].each {
      zaraPlFileName = zaraPlFileName.replace(it, '_')
    }
    log.info "Playlist file name: $zaraPlFileName"
    File zaraPlayList = new File( zaraPlFileName )
    zaraPlayList << String.format('%d%n', zaraTracks.size())
    zaraTracks.each { track ->
      zaraPlayList << track.join('\t')
      zaraPlayList << '\n'
    }
  }

  void grabPlayListTitle() {
    if ( playlistTitle.empty ) {
      final String ytPlTitle = ytMetadata?.playlist_title
      playlistTitle = ytPlTitle ?: ''
      log.debug "Changed playlistTitle  to $playlistTitle"
      log.debug "Changed zaraPlFileName to $zaraPlFileName"
    } else {
      log.debug "playlist_title not found"
    }
  }

  void parseYouTubeMetadata( final String trackFileName ) {
    final String jsonFileName = trackFileName.replaceAll( /\.mp3$/, '.info.json')
    jsonFile = new File( jsonFileName )
    if ( jsonFile.exists() ) {
      log.debug "Parsing JSON: $jsonFileName"
      ytMetadata = new JsonSlurper().parse( jsonFile )
    } else {
      log.warn "Mising: $jsonFileName"
      ytMetadata = null
    }
  }

  Long duration( File trackFile ) { // Based on SpotToZara.fixMetadata()
    AudioFile audioFile = AudioFileIO.read( trackFile )

    MP3AudioHeader audioHeader = audioFile.getAudioHeader();
    String newLengthStr = audioHeader.getTrackLength();
    Long newlength = newLengthStr.toLong()
    Long mins = newlength / 60
    Long secs = newlength % 60
    log.debug "Track length: $newLengthStr = $mins mins, $secs secs"
    newlength * 1000
  }

  void applyTags( String inFileName, artist, title ) {
    final String q              = '"'
    final String md             = '-metadata'
    final String nameMd         = "artist=$q$artist$q"
    final String artistMd       = "$md $nameMd $md album_$nameMd"
    final String titleMd        = "$md title=$q$title$q"
    final String logLevel       = '-loglevel error'
    final String in             = "-i $q$inFileName$q"
    final String outFileName    = outPrefix + cleanFileName( inFileName )
    final String out            = "$q$outFileName$q"

    final String args = "$logLevel $in $titleMd $artistMd $out"
    log.info "Tagging $inFileName"
    log.info "New name is $outFileName"
    log.debug "args: $args"

    ant.exec (
      dir               : '.',
      executable        : 'ffmpeg.exe',
      outputproperty    : 'cmdOut',
      errorproperty     : 'cmdError',
      resultproperty    : 'cmdResult'
    ) {
      arg( line: args )
    }

    final int execRes       = ant.project.properties.cmdResult.toInteger()
    final String execOut    = ant.project.properties.cmdOut
    final String execErr    = ant.project.properties.cmdError

    log.debug "execOut = $execOut"
    log.debug "execErr = $execErr"
    log.debug "execRes = $execRes"

    if ( !execErr.empty ) {
      log.error execErr
      log.warn "out: $execOut"
      log.warn "result: $execRes"
    } else {
      final String backupName = inFileName[0..-5]+'.bak'
      try {
        ant.move( file:inFileName,  tofile: backupName, failonerror: true )
        ant.move( file:outFileName, tofile: inFileName, failonerror: true )
      } catch (Exception exe) {
        log.error "applyTags failed to rename file $inFileName"
      }
    }
  }

  final String cleanFileName( inFileName ) {
    final String prefix       = /(?i)[\(\[]/
    final String official     = /Official\s+/
    final String hd           = /(HD\s+)?/
    final String remastered   = /(remastered\s+)?/
    final String music        = /(Music\s+)?/
    final String video        = /Video/
    final String suffix       = /[\)\]](?-i)/

    final String regex = "$prefix$official$hd$remastered$hd$music$video$suffix"
    inFileName.replaceAll( regex, '').replaceAll(/\s+\.mp3$/,'.mp3')
  }

  void tidyOutputFolder() {
    ant.delete {
      fileset( dir: '.' ) {
        include( name: '*.bak')
        include( name: '*.json')
      }
    }
  } 
}
