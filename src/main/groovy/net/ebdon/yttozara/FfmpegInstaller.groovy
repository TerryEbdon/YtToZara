package net.ebdon.yttozara

/**
 * Download Ffmpeg, verify checksum, and install in target directory
 */
@groovy.util.logging.Log4j2
class FfmpegInstaller extends Installer {
  static final String ffmpegDownloadFail = 'ffmpeg download failed.'

  @SuppressWarnings(['LineLength','GetterMethodCouldBeProperty'])
  static String getDistributionUrl() {
    'https://github.com/BtbN/FFmpeg-Builds/releases/download/autobuild-2025-12-31-14-28/ffmpeg-n8.0.1-34-gbfa334de42-win64-lgpl-8.0.zip'
  }

  FfmpegInstaller(final String installPath) {
    super(installPath)
  }

  @Override
  String getPayloadPath() { // replaces getFfmpegZipPath()
    "$downloadDir$ffmpegZipFileName"
  }

  @Override
  @SuppressWarnings('GetterMethodCouldBeProperty')
  String getExpectedSha() {
    '60145617865cc8e9165a63dc220929f01ebfe17f0534b4e5977ed991d2e56c0e'
  }

  @Override
  int install() {
    log.debug '> Downloading and installing ffmpeg'
    final int downloadStatus = downloadFfmpeg()
    if (downloadStatus == YtToZara.success) {
      if (payloadIsGood) {
        unzipFfmpegAndLogStatus()
      } else {
        log.info  "Missing or corrupt file: $payloadPath"
        log.error ffmpegDownloadFail
        YtToZara.ffmpegInstallFail
      }
    } else {
      log.error ffmpegDownloadFail
      downloadStatus
    }
  }

  int downloadFfmpeg() {
    Boolean downloadThrewException = false
    if (new File(installPath).exists()) {
      log.info   "Downloading $ffmpegZipFileName"
      log.debug  "Downloading from $distributionUrl"
      try {
        ant.get(
          src:          distributionUrl,
          dest:         downloadDir,
          verbose:      false,
          usetimestamp: true,
        )
      } catch (org.apache.tools.ant.BuildException | IOException e) {
        log.error "Failed to download ffmpeg: ${e.message}"
        downloadThrewException = true
      }
      if (downloadThrewException) {
        YtToZara.ffmpegInstallFail
      } else {
        log.info "Downloaded  $ffmpegZipFileName"
        YtToZara.success
      }
    } else {
      log.error "Install path does not exist: $installPath"
      YtToZara.ffmpegInstallFail
    }
  }

  protected static String getFfmpegZipFileName() {
    distributionUrl.split('/').last()
  }

  Boolean unzipFfmpeg() {
    log.info "Unzipping into: $installPath"

    final long startMillis = System.currentTimeMillis()

    Boolean unzipped = false

    try {
      ant.unzip(
        src:  payloadPath,
        dest: installPath,
      ) {
        patternset {
          include name: '**/*.exe'
          exclude name: '**/ffplay.exe'
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
   * Attempt to unzip the ffmpeg distribution and log the result.
   *
   * <p>
   * Delegates to {@link #unzipFfmpeg()} and logs a message
   * indicating whether installation succeeded or failed.
   *
   * @return int  {@link YtToZara#success} when the unzip completed and files
   *              were installed; {@link YtToZara#ffmpegUnzipFail} when the
   *              unzip failed
   */
  final int unzipFfmpegAndLogStatus() {
    if (unzipFfmpeg()) {
      log.info 'ffmpeg installed for this app'
      YtToZara.success
    } else {
      log.error 'Failed to install ffmpeg for this app'
      YtToZara.ffmpegUnzipFail
    }
  }
}
