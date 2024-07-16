package net.ebdon.yttozara

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Bootstrap class that downloads dependencies.
 */
@groovy.util.logging.Log4j2
class Installer {
  static final String tDlpDownloadFail = 'yt-dlp download failed.'

  static final String downloadDir   = System.getProperty('java.io.tmpdir')
  static final String github        = 'https://github.com'

  static final String ytDlpRepo    = "$github/yt-dlp/yt-dlp"
  static final String ytDlpversion = '2024.07.09'
  static final String ytDlpExe     = "yt-dlp.exe"
  static final String ytDlpLastest = "releases/download/${ytDlpversion}"
  static final String ytDlpUrl     = "$ytDlpRepo/$ytDlpLastest/$ytDlpExe"
  static final String ytDlpFile    = "$downloadDir/$ytDlpExe"

  static final AntBuilder ant = new AntBuilder()

  final String installPath

  Installer(final String installPath) {
    this.installPath = installPath
    ant.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN
  }

  void installYtDlp() {
    log.info  "Downloading: $ytDlpExe"
    log.debug "Exists before: ${new File(ytDlpFile).exists()}"
    log.debug "Path exists: ${new File(installPath).exists()}"

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
    } else {
      log.error ytDlpDownloadFail
      ant.fail  ytDlpDownloadFail
    }
  }
}
