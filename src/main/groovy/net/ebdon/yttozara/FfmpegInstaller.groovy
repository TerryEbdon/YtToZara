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
    log.info 'Installing ffmpeg'
    installFfmpeg()
  }
}
