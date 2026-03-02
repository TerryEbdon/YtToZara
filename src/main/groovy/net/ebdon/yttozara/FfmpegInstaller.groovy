package net.ebdon.yttozara

/**
 * Download Ffmpeg, verify checksum, and install in target directory
 */
@groovy.util.logging.Log4j2
class FfmpegInstaller extends Installer {

  FfmpegInstaller(final String installPath) {
    super(installPath)
  }

  int install() {
    log.debug '> Downloading and installing ffmpeg'
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
