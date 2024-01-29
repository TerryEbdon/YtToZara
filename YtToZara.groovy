package net.ebdon.yttozara

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

class YtToZara {
  public static main(args) {
    // assert args.size()
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
