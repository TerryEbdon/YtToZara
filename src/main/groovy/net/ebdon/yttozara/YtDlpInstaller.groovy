package net.ebdon.yttozara

/**
 * Bootstrap class that downloads dependencies.
 */
@groovy.util.logging.Log4j2
class YtDlpInstaller extends Installer {
  static final String downloadDir       = System.getProperty('java.io.tmpdir')
  static final String ytDlpExe          = 'yt-dlp.exe'
  static final String ytDlpFile         = "$downloadDir/$ytDlpExe"
  static final String ytDlpDownloadFail = 'yt-dlp download failed.'

  static final String ytDlpUrl =
    'https://github.com/yt-dlp/yt-dlp/releases/download/2026.03.03/yt-dlp.exe'

  YtDlpInstaller(final String installPath) {
    super(installPath)
  }

  int install() {
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
}
