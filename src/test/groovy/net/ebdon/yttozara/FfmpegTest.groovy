package net.ebdon.yttozara

import groovy.test.GroovyTestCase
import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Test the Ffmpeg class.
 */
@Newify(MockFor)
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

    antMock.demand.getProject { new Project() }
  }

  void testBinPath() {
    logger.info 'testBinPath start'
    assert new Ffmpeg().binPath.contains('bin')
    logger.info 'testBinPath end'
  }

  void testTrimSilenceNoTracks() {
    logger.info 'testTrimSilenceNoTracks start'
    List<String> tracks = []

    antMock.demand.exec(0) { Map args -> } // Must not be called
    projectMock.use {
      antMock.use {
        new Ffmpeg().trimSilence(tracks)
      }
    }
    logger.info 'testTrimSilenceNoTracks end'
  }

  private void runTrackTask( String taskName, String trackFileName, Closure trackClosure) {
    final String tempFileName = "${taskName}_$trackFileName"
    antMock.demand.with {
      exec { Map args, Closure execClosure ->
        logger.info "Task $taskName in ant.exec()"
        assert args.executable.contains('ffmpeg')
      }
      getProject { new Project() }

      move { Map args ->
        logger.info "move() called with ${args}"
        assert args.file == tempFileName
        assert args.tofile == trackFileName
        assert args.overwrite == true
      }
    }

    projectMock.demand.getProperties {
      [
        'cmdOut'   : 'blah',
        'cmdError' : '',
        'cmdResult': '0',
      ]
    }

    projectMock.use {
      antMock.use {
        trackClosure.call()
      }
    }
  }

  void testTrimAudio() {
    logger.info 'testTrimAudio start'

    final String trackFileName  = 'wibble.wav'
    runTrackTask('trimSilence', trackFileName) {
      new Ffmpeg().trimAudio(trackFileName)
    }

    logger.info 'testTrimAudio end'
  }

  void testNormaliseNoTracks() {
    logger.info 'testNormaliseNoTracks start'
    antMock.demand.exec(0) { Map args -> } // Must not be called

    List<String> tracks = []
    projectMock.use {
      antMock.use {
        new Ffmpeg().normalise(tracks)
      }
    }
    logger.info 'testNormaliseNoTracks end'
  }

  void testApplyTags() {
    logger.info 'testApplyTags start'
    final String trackFileName = 'tagMe.wav'

    runTrackTask('applyTags', trackFileName) {
      new Ffmpeg().applyTags( trackFileName, 'Singy Songster', 'My Song' )
    }

    logger.info 'testApplyTags end'
  }
}
