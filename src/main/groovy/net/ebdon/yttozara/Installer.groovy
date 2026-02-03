package net.ebdon.yttozara

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Bootstrap class that downloads dependencies.
 */
@groovy.util.logging.Log4j2
class Installer {
  static final String ffmpegDownloadFail = 'ffmpeg download failed.'
  static final String ytDlpDownloadFail  = 'yt-dlp download failed.'

  static final String downloadDir  = System.getProperty('java.io.tmpdir')
  static final String ytDlpExe     = 'yt-dlp.exe'
  static final String ytDlpFile    = "$downloadDir/$ytDlpExe"

  static final String ytDlpUrl =
    'https://github.com/yt-dlp/yt-dlp/releases/download/2026.01.31/yt-dlp.exe'

  @SuppressWarnings('LineLength')
  static final String ffmpegUrl = 'https://github.com/BtbN/FFmpeg-Builds/releases/download/autobuild-2025-12-31-14-28/ffmpeg-n8.0.1-34-gbfa334de42-win64-lgpl-8.0.zip'

  static String ffmpegChecksumAlgorithm = 'SHA-256'
  static String ffmpegExpectedSha =
    '60145617865cc8e9165a63dc220929f01ebfe17f0534b4e5977ed991d2e56c0e'

  final AntBuilder ant

  final String installPath

  Installer(final String installPath) {
    this.installPath = installPath
    ant = new AntBuilder()
    ant.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN
  }

  static String getFfmpegZipFileName() {
    ffmpegUrl.split('/').last()
  }

  /**
   * Return the full path to the downloaded ffmpeg zip file.
   *
   * <p>
   * The returned value is formed by combining the configured download
   * directory and the ffmpeg zip file name (as returned by
   * {@code getFfmpegZipFileName()}). This is always correct on Microsoft
   * Windows, which is the only supported platform/environment.
   *
   * Note: Do <b>NOT</b> normalise the path via {@link java.io.File}, as that
   * breaks the unit test. Fixing that would require excessive complexity.
   *
   * @return String  absolute path to the ffmpeg zip file in the download dir
   */
  static String getFfmpegZipPath() {
    "$downloadDir$ffmpegZipFileName" // downloadDir contains the separator
  }

  int installYtDlp() {
    log.info  "Downloading: $ytDlpExe"

    assert new File(installPath).exists()

    ant.get (
      src:          ytDlpUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true
    )

    if (new File(ytDlpFile).exists()) {
      log.debug 'yt-dlp downloaded'
      log.info "Copying into: $installPath"
      ant.copy(
         file:  ytDlpFile,
         todir: installPath,
         flatten: true
      )
      log.debug 'yt-dlp copied'
      YtToZara.success
    } else {
      log.error ytDlpDownloadFail
      YtToZara.ytDlpInstallFail
    }
  }

  void downloadFfmpeg() {
    assert new File(installPath).exists()

    log.info   "Downloading $ffmpegZipFileName"
    log.debug  "Downloading from $ffmpegUrl"
    ant.get (
      src:          ffmpegUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true,
    )
    log.info "Downloaded  $ffmpegZipFileName"
  }

  Boolean unzipFfmpeg() {
    log.info  "Unzipping into: $installPath"

    final long startMillis = System.currentTimeMillis()

    Boolean unzipped = false

    try {
      ant.unzip(
        src:  ffmpegZipPath,
        dest: installPath,
      ) {
        patternset {
          include name: '**/*.exe'
        }
        mapper type: 'flatten'
      }
      unzipped = true
      log.info 'ffmpeg distribution unzipped'
    } catch (org.apache.tools.ant.BuildException | java.io.IOException exc) {
      log.error "Failed to unzip ffmpeg: ${exc.message}"
    } finally {
      log.info "Unzip operation time: ${duration(startMillis)}"
    }
    unzipped
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

  Boolean getFfmpegGoodZipFile() {
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
  Boolean getFfmpegChecksumIsGood() {
    log.info 'Verifying checksum'
    final long startMillis = System.currentTimeMillis()
    ant.checksum(
      file: ffmpegZipPath,
      algorithm: ffmpegChecksumAlgorithm,
      property: ffmpegExpectedSha,        // Will be compared to calc'd checksum
      verifyProperty: 'ffmpegIsGood'      // Ant returns 'true' or 'false'
    )

    @SuppressWarnings('DuplicateStringLiteral')
    Boolean fileMatchesChecksum = ant.project.properties.ffmpegIsGood == 'true'
    if (fileMatchesChecksum) {
      log.info 'ffmpeg zip file matches expected checksum'
    } else {
      log.error 'ffmpeg install failed, zip file is corrupt'
    }
    log.info "Checksum operation time: ${duration(startMillis)}"
    fileMatchesChecksum
  }

  int unzipFfmpegAndLogStatus() {
    if (unzipFfmpeg()) {
      log.info 'ffmpeg installed for this app'
      YtToZara.success
    } else {
      log.error 'Failed to install ffmpeg for this app'
      YtToZara.ffmpegUnzipFail
    }
  }

  int installFfmpeg() {
    log.debug 'Downloading and installing ffmpeg'
    downloadFfmpeg()
    if (ffmpegGoodZipFile) {
      unzipFfmpegAndLogStatus()
    } else {
      log.info "Missing or corrupt file: $ffmpegZipPath"
      log.error ffmpegDownloadFail
      YtToZara.ffmpegInstallFail
    }
  }
}
