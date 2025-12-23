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
'https://github.com/yt-dlp/yt-dlp/releases/download/2025.12.08/yt-dlp.exe'

static final String ffmpegUrl =
'https://github.com/BtbN/FFmpeg-Builds/releases/download/autobuild-2025-12-15-12-56/ffmpeg-n8.0.1-28-g9c93070155-win64-lgpl-8.0.zip'

  static String ffmpegChecksumAlgorithm = 'SHA-256'
  static String ffmpegExpectedSha =
    '7323c8cff8e439e952302d661fc267514d2c405f30047e8fccf7f6862f33e218'

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

  static String getFfmpegZipPath() {
    "$downloadDir$ffmpegZipFileName"
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
    log.info "Downloaded $ffmpegZipFileName"
  }

  void unzipFfmpeg() {
    log.debug 'ffmpeg downloaded'
    log.info  "Unzipping into: $installPath"
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
      log.debug 'ffmpeg unzipped'
    } catch (org.apache.tools.ant.BuildException | java.io.IOException exc) {
      log.error "Failed to unzip ffmpeg: ${exc.message}"
    }
  }

  Boolean getFfmpegGoodZipFile() {
    File zipFile = new File(ffmpegZipPath)
    zipFile.exists() && ffmpegChecksumIsGood
  }

  Boolean getFfmpegChecksumIsGood() {
    ant.checksum(
      file: ffmpegZipPath,
      algorithm: ffmpegChecksumAlgorithm,
      property: ffmpegExpectedSha,
      verifyProperty: 'ffmpegIsGood'
    )
    ant.project.properties.ffmpegIsGood == 'true'
  }

  void installFfmpeg() {
    downloadFfmpeg()

    if (new File(ffmpegZipPath).exists()) {
      unzipFfmpeg()
    } else {
      log.info "No such file: $ffmpegZipPath"
      log.error ffmpegDownloadFail
    }
  }
}
