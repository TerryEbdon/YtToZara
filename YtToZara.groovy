package net.ebdon.yttozara

import groovy.ant.AntBuilder
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

class YtToZara {
  final AntBuilder ant = new AntBuilder()

  public static main(args) {
<<<<<<< HEAD
    YtToZara ytz = new YtToZara()
    if (args.size() == 0 ) {
      ytz.tee()
    } else {
      ytz.guessMp3Tags( args.first() )
    }
  }

  void guessMp3Tags( final String trackFileName ) {
=======
    if (args.size() == 0 ) {
      tee()
    } else {
      guessMp3Tags()
    }
  }

  static void guessMp3Tags( final String trackFileName ) {
>>>>>>> 7630a7159ae38bf512ec9c377f6156003a4ef8d3
    println "Guessing for $trackFileName"
    def trackDetails = trackFileName.split( ' - ')

    switch( trackDetails.size() ) {
      case 0:
      case 1:
        println 'Track file name missing expected seperators'
        break
      case 2:
<<<<<<< HEAD
        String artist = trackDetails.first()
        String title  = trackDetails.last()
        println "Artist: $artist"
        println "Title:  $title"
        final String outFileName = "out_$trackFileName"
        applyTags( trackFileName, outFileName, artist, title )
=======
        println "Artist: ${trackDetails.first()}"
        println "Title:  ${trackDetails.last()}"
>>>>>>> 7630a7159ae38bf512ec9c377f6156003a4ef8d3
        break
      default:
        println "Too many separators to decide."
    }
  }

<<<<<<< HEAD
  void tee() {
=======
  static void tee() {
>>>>>>> 7630a7159ae38bf512ec9c377f6156003a4ef8d3
    final String tsPattern = 'yyyy-MM-dd_HH-mm-ss-SSS'
    final DateTimeFormatter fmtTs = DateTimeFormatter.ofPattern(tsPattern)

    final ZoneId zoneId                  = ZoneId.of('Etc/UTC')
    final ZonedDateTime playListFileTime = ZonedDateTime.now(zoneId)

    final String timestamp    = playListFileTime.format(fmtTs)
    final String plFileName   = "pl_${timestamp}.txt"

    println "Creating playlist: $plFileName"
    File outFile = new File( plFileName )
    String line =''
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in))
    while ( line != null ) {
      line = br.readLine();
      if (line != null ) {
        println line
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
    // final String cmdPrefix  = "ffmpeg $logLevel ffmpeg "
    final String in         = "-i $q$inFileName$q"
    final String out        = "$q$outFileName$q"

    // final String cmd = "$cmdPrefix $in $q$inFileName$q $titleMd $artistMd $out"
    final String args = "$logLevel $in $titleMd $artistMd $out"

    ant.echo level:'debug', "args: $args"
    
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

    ant.echo level:'debug', "execOut = $execOut"
    ant.echo level:'debug', "execErr = $execErr"
    ant.echo level:'debug', "execRes = $execRes"

    if ( !execErr.empty ) {
      ant.echo level:'error', execErr
      ant.echo level:'warn', "out: $execOut"
      ant.echo level:'warn', "result: $execRes"
    }
  }
}
