package net.ebdon.yttozara

import groovy.test.GroovyTestCase
import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Test the Ffmpeg class.
 */
@Newify([Ffmpeg,MockFor])
@groovy.util.logging.Log4j2('logger')
class FfmpegTest extends GroovyTestCase {
  private MockFor antMock
  private MockFor projectMock

  @Override
  void setUp() {
    super.setUp()
    antMock     = MockFor(AntBuilder)
    projectMock = MockFor(Project)

    constructorDemands()
  }

  private void constructorDemands() {
    projectMock.demand.getMSG_WARN { 1 }    // Used in Ffmpeg constructor
    projectMock.demand.getBuildListeners {  // Used in Ffmpeg constructor
      [
        [messageOutputLevel:4,],
      ]
    }

    antMock.demand.getProject {
      logger.debug 'getProject() called'
      new Project()
    }
  }

  void testBinPath() {
    logger.info 'testBinPath start'
    assert new Ffmpeg().binPath.contains('bin')
    logger.info 'testBinPath end'
  }

  void testTrimSilenceNoTracks() {
    logger.info 'testTrimSilenceNoTracks start'
    List<String> tracks = []
    antMock.demand.exec(0) { Map args, Closure closure -> }
      // ant.exec must not be called when tracks is empty
    projectMock.use {
      antMock.use {
        new Ffmpeg().trimSilence(tracks)
      }
    }
    assert tracks.empty
    logger.info 'testTrimSilenceNoTracks end'
  }

  private Boolean runTrackTask( String taskName, String trackFileName, Closure trackClosure) {
    Boolean result = false
    final String tempFileName = "${taskName}_$trackFileName"
    antMock.demand.with {
      exec(0..1) { Map args, Closure execClosure ->
        logger.info "Task $taskName in ant.exec()"
        assert args.executable.contains('ffmpeg')
      }
      getProject(0..1) { new Project() }

      move(0..1) { Map args ->
        logger.info "move() called with ${args}"
        assert args.file == tempFileName
        assert args.tofile == trackFileName
        assert args.overwrite == true
      }
    }

    projectMock.demand.getProperties(0..1) {
      [
        'cmdOut'   : 'blah',
        'cmdError' : '',
        'cmdResult': '0',
      ]
    }

    projectMock.use {
      antMock.use {
        result = trackClosure.call()
      }
    }
    result
  }

  void testTrimAudioDisabled() {
    logger.info 'testTrimAudioDisabled start'

    MockFor config = MockFor(Configuration).tap {
      demand.with {
        loadConfig  { logger.debug 'Mock Configuration loaded' }
        logConfig   { logger.debug 'Mock Configuration logged' }
        getSilenceRemove(2) { [enabled: false,] }
        getConfigFileName { 'corrupt-config.groovy' }
      }
    }

    antMock.demand.with {
      exec(0) { Map args, Closure execClosure ->
        assert args.executable.contains(
          'ant.exec must not be called when silence trimming disabled')
      }
    }

    final String trackFileName  = 'wibble.wav'
    config.use {
      projectMock.use {
        antMock.use {
          assert Ffmpeg().trimAudio(trackFileName) == false
        }
      }
    }
    logger.info 'testTrimAudioDisabled end'
  }

  void testTrimAudioEnabled() {
    logger.info 'testTrimAudioEnabled start'

    MockFor config = MockFor(Configuration).tap {
      demand.with {
        loadConfig  { logger.debug 'Mock Configuration loaded' }
        logConfig   { logger.debug 'Mock Configuration logged' }
        getSilenceRemove(2) {
          [
            enabled: true,
            startPeriods: 0,
            startSilence: 0,
            stopSilence: 0,
            startThreshold: 0,
            startDuration: 0,
            stopDuration: 0,
            detection: 0,
          ]
        }
      }
    }

    config.use {
      final String trackFileName  = 'wibble.wav'
      runTrackTask('trimSilence', trackFileName) {
        assert Ffmpeg().trimAudio(trackFileName)
      }
    }
    logger.info 'testTrimAudioEnabled end'
  }

  /**
   * Test that normalisation is performed when enabled by configuration.
   *
   * Sets up a mock Configuration with normalisation enabled then delegates to
   * the helper runNormaliseAudioEnabled(true) which arranges Ant/Project mocks
   * and invokes Ffmpeg.normaliseAudio.
   */
  void testNormaliseAudioEnabled() {
    logger.info 'testNormaliseAudioEnabled start'
    assert runNormaliseAudioEnabled(true)
    logger.info 'testNormaliseAudioEnabled end'
  }

  /**
   * Test that normalisation behaves correctly when disabled by configuration.
   *
   * Sets up a mock Configuration with normalisation disabled and verifies the
   * behaviour via the helper runNormaliseAudioEnabled(false).
   */
  void testNormaliseAudioDisabled() {
    logger.info 'testNormaliseAudioDisabled start'
    assert runNormaliseAudioEnabled(false)
    logger.info 'testNormaliseAudioDisabled end'
  }

  /**
   * Helper that prepares a mocked Configuration and runs the loudnorm task.
   *
   * @param runEnabled  whether normalisation should be enabled in the mock
   *                    config
   * @return Boolean    result returned by runTrackTask which invokes
   *                    Ffmpeg.normaliseAudio
   */
  private Boolean runNormaliseAudioEnabled( Boolean runEnabled ) {
    Boolean result = false
    MockFor config = MockFor(Configuration).tap {
      demand.with {
        loadConfig  { logger.debug 'Mock Configuration loaded' }
        logConfig   { logger.debug 'Mock Configuration logged' }
        getNormalise {
          [
            enabled: runEnabled,
            integratedLoudnessTarget: -1,
          ]
        }
      }
    }

    final String trackFileName = 'normaliseEnabled.wav'
    config.use {
      result = runTrackTask('loudnorm', trackFileName) {
        Ffmpeg().normaliseAudio(trackFileName)
      }
    }
    result
  }

  void testNormaliseNoTracks() {
    logger.info 'testNormaliseNoTracks start'
    antMock.demand.exec(0) { Map args -> } // Must not be called

    List<String> tracks = []
    projectMock.use {
      antMock.use {
        assert Ffmpeg().normalise(tracks)
      }
    }
    assert tracks.empty
    logger.info 'testNormaliseNoTracks end'
  }

  void testApplyTags() {
    logger.info 'testApplyTags start'
    final String trackFileName = 'tagMe.wav'

    runTrackTask('applyTags', trackFileName) {
      assert Ffmpeg().applyTags( trackFileName, 'Singy Songster', 'My Song' )
    }

    logger.info 'testApplyTags end'
  }
}
