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
  abstract String getPayloadPath()
  abstract String getExpectedSha()

  final String checksumAlgorithmName = 'SHA-256'

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

  /**
   * Check that the downloaded file exists and passes checksum verification.
   *
   * <p>
   * Returns true only when the payload file is present on disk and the
   * checksum matches the expected value. If either check fails the method
   * returns false without throwing.
   *
   * @return Boolean  true when the payload file exists and the checksum is good;
   *                  false otherwise
   */
  protected Boolean getPayloadIsGood() {
    if (new File(payloadPath).exists()) {
      log.debug 'payload file DOES exist, running checksum'
      checksumIsGood
    } else {
      log.error "Downloaded file not found: $payloadPath"
      false
    }
  }

  /**
   * Verify the payload's checksum using Ant's checksum task.
   *
   * <p>
   * This method invokes the Ant checksum task against the file
   * using the algorithm configured in
   * {@code getChecksumAlgorithmName}. The Ant task verifies the
   * checksum and saves the verification result in the {@code checksumVerified}
   * project property.
   * <p>
   * Note: This is the correct way to compare the calculated and expected
   * checksums, as documented in the
   * <a href="https://ant.apache.org/manual/Tasks/checksum.html">Ant manual</a>.
   *
   * @return Boolean  true when Ant indicates the file checksum matches the
   *                  expected value, false otherwise
   */
  final Boolean getChecksumIsGood() {
    log.info 'Verifying checksum'
    final long startMillis = System.currentTimeMillis()
    ant.checksum(
      file:           payloadPath,
      algorithm:      checksumAlgorithmName,
      property:       expectedSha,        // Will be compared to calc'd checksum
      verifyProperty: 'checksumVerified'  // Ant returns 'true' or 'false'
    )
    log.debug 'returned from ant.checksum()'
    // @SuppressWarnings('DuplicateStringLiteral')
    final Boolean fileMatchesChecksum = ant.project.properties.checksumVerified == 'true'
    log.info "Checksum operation time: ${duration(startMillis)}"

    if (fileMatchesChecksum) {
      log.info 'Download has correct checksum'
    } else {
      log.error 'Checksum does not verify. Download is corrupt'
    }
    fileMatchesChecksum
  }
}
