package net.ebdon.yttozara

import groovy.ant.AntBuilder
import org.apache.tools.ant.Project
import java.nio.file.Paths

/**
 * Use `ffmpeg` to process audio files.
 */
@groovy.util.logging.Log4j2
class Ffmpeg {

  final String logLevel   = '-loglevel error'
  final String q          = '"'
  final String currentDir = '.'

  final AntBuilder ant    = new AntBuilder()

  Configuration config

  /**
  * Initializes the Ffmpeg class, sets Ant logging level, loads configuration,
  * and assigns config values to instance fields. All loaded values are logged.
  */
  Ffmpeg() {
    ant.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN
    config = new Configuration()
  }

  void trimSilence( List<String> trackList ) {
    if (config.silenceRemove.enabled) {
      log.info 'Trimming silence from start and end of tracks.'
      trackList.each { String trackFileName ->
        trimAudio( trackFileName )
      }
    } else {
      silenceTrimmingDisabled()
    }
  }

  private Boolean silenceTrimmingDisabled() {
    log.info 'Silence trimming is disabled.'
    false
  }

  String getBinPath() {
    String jarPath = Paths.get(
      this.class.protectionDomain.
        codeSource.location.toURI()
    )

    "${jarPath}\\..\\..\\bin"
  }

  /**
  * Normalises a list of audio tracks.
  *
  * Each track in the provided list is processed for loudness normalisation. If
  * any track fails to normalise, the method returns {@code false}.
  *
  * @param trackList List of audio file names to be normalised
  * @return {@code true} if all tracks are successfully normalised,
  * {@code false} otherwise
  */
  Boolean normalise( List<String> trackList ) {
    log.info 'Normalising tracks'
    int failedCount = 0
    trackList.each { String trackFileName ->
      failedCount += normaliseAudio( trackFileName ) ? 0 : 1
    }
    failedCount == 0
  }

  /**
  * Normalise an MP3 file with single-pass
  * <a href="https://ffmpeg.org/ffmpeg-filters.html#loudnorm">loudnorm</a>
  * filter
  *
  * @param Name of audio file to be normalised
  * @return {@code true} if all successfully normalised, {@code false} otherwise
  */
  Boolean normaliseAudio( final String mp3FileName ) {
    final String integratedLoudnessTarget = '-13'
    final String filter = "loudnorm=I=$integratedLoudnessTarget"
    filterTrack( 'loudnorm', filter, mp3FileName)
  }

  Boolean filterTrack( String filterName, String filter, String mp3FileName ) {
    final String task  = "-af $filter"

    runTask( filterName, task, mp3FileName )
  }

  Boolean runTask( String taskName, String task, String trackFileName ) {
    log.info "${taskName}: $trackFileName"
    final String tempFileName   = "${taskName}_$trackFileName"
    final String replaceOutFile = '-y'
    final String input          = "-i $q$trackFileName$q"
    final String inFileArg      = "$replaceOutFile $input"
    final String outFileArg     = "$q$tempFileName$q"
    final String argsLine       = "$inFileArg $logLevel $task $outFileArg"
    log.debug argsLine

    ant.exec (
      dir               : currentDir,
      executable        : "${binPath}\\ffmpeg",
      outputproperty    : 'cmdOut',
      errorproperty     : 'cmdError',
      resultproperty    : 'cmdResult',
    ) {
      arg( line: argsLine )
    }

    final Map antProperties = ant.project.properties
    final int execRes       = antProperties.cmdResult.toInteger()
    final String execOut    = antProperties.cmdOut
    final String execErr    = antProperties.cmdError
    log.debug "$taskName execOut = $execOut"
    log.debug "$taskName execErr = $execErr"
    log.debug "$taskName execRes = $execRes"

    if ( execErr.empty ) {
      moveFile tempFileName, trackFileName
    } else {
      log.error "Task failed: $taskName"
      log.error execErr
      log.info "out: $execOut"
      log.info "result: $execRes"
    }
    execErr.empty
  }

  /**
  * Trims silence from the start and end of the given audio file using ffmpeg's
  * silenceremove filter.
  *
  * @param mp3FileName the name of the MP3 file to process
  * Applies silence removal and reverses the audio to trim both edges.
  */
  Boolean trimAudio( final String mp3FileName ) {
    final Map configSilenceRemove = config.silenceRemove
    if (configSilenceRemove.enabled) {
      log.info "Trimming silence from $mp3FileName"
      final String trimTrackEdgeArgs =
        "silenceremove=${configSilenceRemove.startPeriods}:" +
        "start_duration=${configSilenceRemove.startDuration}:" +
        "start_threshold=${configSilenceRemove.startThreshold}:" +
        "stop_silence=${configSilenceRemove.stopSilence}:" +
        'detection=peak,' +
        'aformat=dblp,' +
        'areverse'

      log.debug trimTrackEdgeArgs
      final String filter = "$q${trimTrackEdgeArgs},${trimTrackEdgeArgs}$q"

      filterTrack('trimSilence', filter, mp3FileName)
    } else {
      silenceTrimmingDisabled()
    }
  }

  Boolean applyTags( String mp3FileName, String artist, String title ) {
    final String md             = '-metadata'
    final String nameMd         = "artist=$q$artist$q"
    final String artistMd       = "$md $nameMd $md album_$nameMd"
    final String titleMd        = "$md title=$q$title$q"

    final String task = "$titleMd $artistMd"
    log.info "Tagging $mp3FileName"
    log.debug "task: $task"

    runTask('applyTags', task, mp3FileName)
  }

  void moveFile( final String fromFileName, final String toFileName ) {
    ant.move(
      file: fromFileName, tofile: toFileName,
      failonerror: true, verbose: false, overwrite: true, force:true
    )
  }
}
