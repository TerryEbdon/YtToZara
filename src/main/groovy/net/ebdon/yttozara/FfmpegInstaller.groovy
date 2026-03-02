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
    final int downloadStatus = downloadFfmpeg()
    if ( downloadStatus == YtToZara.success ) {
      if (ffmpegGoodZipFile) {
        unzipFfmpegAndLogStatus()
      } else {
        log.info "Missing or corrupt file: $ffmpegZipPath"
        log.error ffmpegDownloadFail
        YtToZara.ffmpegInstallFail
      }
    } else {
      log.error ffmpegDownloadFail
      downloadStatus
    }
  }

  int downloadFfmpeg() {
    if ( new File(installPath).exists() ) {
      log.info   "Downloading $ffmpegZipFileName"
      log.debug  "Downloading from $ffmpegUrl"
      ant.get (
        src:          ffmpegUrl,
        dest:         downloadDir,
        verbose:      false,
        usetimestamp: true,
      )
      log.info "Downloaded  $ffmpegZipFileName"
      YtToZara.success
    } else {
      log.error "Install path does not exist: $installPath"
      YtToZara.ffmpegInstallFail
    }
  }
}
