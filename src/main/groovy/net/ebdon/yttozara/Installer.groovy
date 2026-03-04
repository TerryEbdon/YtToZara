package net.ebdon.yttozara

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Bootstrap class that downloads dependencies.
 */
@groovy.util.logging.Log4j2
abstract class Installer {
  static final String downloadDir  = System.getProperty('java.io.tmpdir')

  final AntBuilder ant
  final String installPath

  abstract int install()

  protected Installer(final String installPath) {
    this.installPath = installPath
    ant = new AntBuilder()
    ant.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN
  }

  /**
   * Format an elapsed duration from the given start time.
   *
   * <p>
   * Calculates the elapsed time (current time minus {@code startMillis}) and
   * formats it as minutes, seconds and milliseconds suitable for logging.
   *
   * @param startMillis  the start time in milliseconds (as returned by
   *                     System.currentTimeMillis())
   * @return String      human readable duration, e.g. "1m 23s 456ms"
   */
  final String duration(final long startMillis) {
    final long elapsed = System.currentTimeMillis() - startMillis

    final long millisPerSecond = 1000L
    final long millisPerMinute = 60L * millisPerSecond
    final long minutes         = elapsed / millisPerMinute
    final long remainder       = elapsed % millisPerMinute
    final long seconds         = remainder / millisPerSecond
    final long milliseconds    = remainder % millisPerSecond

    "${minutes}m ${seconds}s ${milliseconds}ms"
  }

  final Boolean getFfmpegGoodZipFile() {
    File zipFile = new File(ffmpegZipPath)
    zipFile.exists() && ffmpegChecksumIsGood
  }

  /**
   * Verify the ffmpeg zip file checksum using Ant's checksum task.
   *
   * <p>
   * This method invokes the Ant checksum task against the file returned by
   * {@code ffmpegZipPath} using the algorithm configured in
   * {@code ffmpegChecksumAlgorithm}. The Ant task verifies the
   * checksum and save the verification result in the {@code ffmpegIsGood}
   * project property.
   * <p>
   * Note: This is the correct way to compare the calculated and expected
   * checksums, as documented in the
   * <a href="https://ant.apache.org/manual/Tasks/checksum.html">Ant manual</a>.
   *
   * @return Boolean  true when Ant indicates the file checksum matches the
   *                  expected value, false otherwise
   */
  final Boolean getFfmpegChecksumIsGood() {
    log.info 'Verifying checksum'
    final long startMillis = System.currentTimeMillis()
    ant.checksum(
      file: ffmpegZipPath,
      algorithm: ffmpegChecksumAlgorithm,
      property: ffmpegExpectedSha,        // Will be compared to calc'd checksum
      verifyProperty: 'ffmpegIsGood'      // Ant returns 'true' or 'false'
    )

    @SuppressWarnings('DuplicateStringLiteral')
    final Boolean fileMatchesChecksum = ant.project.properties.ffmpegIsGood == 'true'
    if (fileMatchesChecksum) {
      log.info 'ffmpeg zip file matches expected checksum'
    } else {
      log.error 'ffmpeg install failed, zip file is corrupt'
    }
    log.info "Checksum operation time: ${duration(startMillis)}"
    fileMatchesChecksum
  }
}
