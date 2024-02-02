package net.ebdon.yttozara

import groovy.ant.AntBuilder
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

@groovy.util.logging.Log4j2
class YtToZara {
  final String outPrefix    = 'out_'
  final AntBuilder ant      = new AntBuilder()
  def trackList             = []
  def trackDetails          = []

  public static main(args) {
    YtToZara ytz = new YtToZara()
    if (args.size() == 0 ) {
      ytz.tee()
      ytz.createZaraPlaylist()
    } else {
      ytz.guessMp3Tags( args.first() )
    }
  }

  void guessMp3Tags( final String trackFileName ) {
    log.info "Guessing for $trackFileName"
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

  void tee() {
    final String tsPattern = 'yyyy-MM-dd_HH-mm-ss-SSS'
    final DateTimeFormatter fmtTs = DateTimeFormatter.ofPattern(tsPattern)

    final ZoneId zoneId                  = ZoneId.of('Etc/UTC')
    final ZonedDateTime playListFileTime = ZonedDateTime.now(zoneId)

    final String timestamp    = playListFileTime.format(fmtTs)
    final String plFileName   = "pl_${timestamp}.txt"

    log.info "Creating playlist: $plFileName"
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
    println 'Creating Zara Playlist'
    trackList.each { String trackFileName ->
      guessMp3Tags(trackFileName)

    }
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
      ant.move( file:inFileName,  tofile: backupName )
      ant.move( file:outFileName, tofile: inFileName )
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
}
