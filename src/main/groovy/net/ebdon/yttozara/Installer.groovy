package net.ebdon.yttozara

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Bootstrap class that downloads dependencies.
 */
@groovy.util.logging.Log4j2
class Installer {
  static final String ffmpegDownloadFail = 'ffmpeg download failed.'
  static final String tDlpDownloadFail = 'yt-dlp download failed.'

  static final String downloadDir   = System.getProperty('java.io.tmpdir')
  static final String github        = 'https://github.com'

  static final String ffmpegRepo    = "$github/BtbN/FFmpeg-Builds"
  static final String ffmpegZip     = 'ffmpeg-n7.1-latest-win64-lgpl-7.1.zip'
  static final String ffmpegLatest  = 'releases/download/latest'
  static final String ffmpegUrl     = "$ffmpegRepo/$ffmpegLatest/$ffmpegZip"
  static final String ffmpegFile    = "$downloadDir$ffmpegZip"

  static final String ytDlpRepo    = "$github/yt-dlp/yt-dlp"
  static final String ytDlpversion = '2025.10.22'
  static final String ytDlpExe     = 'yt-dlp.exe'
  static final String ytDlpLatest  = "releases/download/${ytDlpversion}"
  static final String ytDlpUrl     = "$ytDlpRepo/$ytDlpLatest/$ytDlpExe"
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

  void installFfmpeg() {
    log.trace "Exists before: ${new File(ffmpegFile).exists()}"
    log.trace "Path exists: ${new File(installPath).exists()}"

    assert new File(installPath).exists()

    log.info   "Downloading $ffmpegZip"
    log.debug  "Downloading from $ffmpegUrl"
    ant.get (
      src:          ffmpegUrl,
      dest:         downloadDir,
      verbose:      false,
      usetimestamp: true,
    )
    if (new File(ffmpegFile).exists()) {
      log.debug 'ffmpeg downloaded'
      log.info  "Unzipping into: $installPath"
      try {
        ant.unzip(
          src:  ffmpegFile,
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
    } else {
      log.error ffmpegDownloadFail
    }
  }
}
