package net.ebdon.yttozara

/**
 * Download Ffmpeg, verify checksum, and install in target directory
 */
@groovy.util.logging.Log4j2
class FfmpegInstaller extends Installer {
  static final String ffmpegDownloadFail = 'ffmpeg download failed.'

  @SuppressWarnings('LineLength')
  static final String ffmpegUrl = 'https://github.com/BtbN/FFmpeg-Builds/releases/download/autobuild-2025-12-31-14-28/ffmpeg-n8.0.1-34-gbfa334de42-win64-lgpl-8.0.zip'

  static String ffmpegChecksumAlgorithm = 'SHA-256'
  static String ffmpegExpectedSha =
    '60145617865cc8e9165a63dc220929f01ebfe17f0534b4e5977ed991d2e56c0e'

  FfmpegInstaller(final String installPath) {
    super(installPath)
  }

  int install() {
    log.debug '> Downloading and installing ffmpeg'
    final int downloadStatus = downloadFfmpeg()
    if (downloadStatus == YtToZara.success) {
      if (ffmpegGoodZipFile) {
        unzipFfmpegAndLogStatus()
      } else {
        log.info  'Missing or corrupt file: {}', ffmpegZipPath
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
      log.debug  "Downloading from $ffmpegUrl"
      try {
        ant.get(
          src:          ffmpegUrl,
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
  protected static String getFfmpegZipPath() {
    "$downloadDir$ffmpegZipFileName" // downloadDir contains the separator
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
