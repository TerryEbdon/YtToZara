package net.ebdon.yttozara

import groovy.ant.AntBuilder
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

@groovy.util.logging.Log4j2
class YtToZara {
  final AntBuilder ant = new AntBuilder()

  public static main(args) {
    YtToZara ytz = new YtToZara()
    if (args.size() == 0 ) {
      ytz.tee()
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
        final String outFileName = "out_$trackFileName"
        applyTags( trackFileName, outFileName, artist, title )
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
      }
    }
  }

  void applyTags( String inFileName, outFileName, artist, title ) {
    final String q          = '"'
    final String md         = '-metadata'
    final String artistMd   = "$md artist=$q$artist$q $md album_artist=$q$artist$q"
    final String titleMd    = "$md title=$q$title$q"
    final String logLevel   = '-loglevel error'
    final String in         = "-i $q$inFileName$q"
    final String out        = "$q$outFileName$q"

    final String args = "$logLevel $in $titleMd $artistMd $out"

    log.debug "args: $args"
    
    ant.exec (
      dir               : '.',
      executable        : 'ffmpeg.exe',
      outputproperty    : 'cmdOut',
      errorproperty     : 'cmdError',
      resultproperty    : 'cmdResult' ) {
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
    }
  }
}
