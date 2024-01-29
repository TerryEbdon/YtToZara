package net.ebdon.yttozara

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

class YtToZara {
  public static main(args) {
    if (args.size() == 0 ) {
      tee()
    } else {
      guessMp3Tags()
    }
  }

  static void guessMp3Tags( final String trackFileName ) {
    println "Guessing for $trackFileName"
    def trackDetails = trackFileName.split( ' - ')

    switch( trackDetails.size() ) {
      case 0:
      case 1:
        println 'Track file name missing expected seperators'
        break
      case 2:
        println "Artist: ${trackDetails.first()}"
        println "Title:  ${trackDetails.last()}"
        break
      default:
        println "Too many separators to decide."
    }
  }

  static void tee() {
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
}
