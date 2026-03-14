package net.ebdon.yttozara

/**
 * Bootstrap class that downloads dependencies.
 */
@groovy.util.logging.Log4j2
class YtDlpInstaller extends Installer {
  static final String ytDlpExe          = 'yt-dlp.exe'
  static final String ytDlpDownloadFail = 'yt-dlp download failed.'

  @SuppressWarnings('GetterMethodCouldBeProperty')
  static String getDistributionUrl() {
    'https://github.com/yt-dlp/yt-dlp/releases/download/2026.03.03/yt-dlp.exe'
  }

  YtDlpInstaller(final String installPath) {
    super(installPath)
  }

  @Override
  String getPayloadPath() {
    "$downloadDir/$ytDlpExe"
  }

  @Override
  @SuppressWarnings('GetterMethodCouldBeProperty')
  String getExpectedSha() {
    '554e868ca1df425d4fe90c224980f0862fe20e28dced6256461f16752d7a1218'
  }

  void download() {
    assert new File(installPath).exists()

    log.info  "Downloading: $ytDlpExe"
    ant.get (
      src:          distributionUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true
    )
    log.debug 'yt-dlp downloaded'
  }

  void copyPayloadToInstallPath() {
    log.info "Copying into: $installPath"
    ant.copy(
        file:  payloadPath,
        todir: installPath,
        flatten: true
    )
    log.info "$ytDlpExe copied"
  }

  @Override
  int install() {
    log.debug 'about to download'
    download()
    log.trace 'download returned'
    if (payloadIsGood) {
      log.info 'payload is good, about to copy'
      copyPayloadToInstallPath()
      YtToZara.success
    } else {
      log.error ytDlpDownloadFail
      YtToZara.ytDlpInstallFail
    }
  }
}
