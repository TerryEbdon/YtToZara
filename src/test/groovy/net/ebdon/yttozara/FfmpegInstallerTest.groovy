package net.ebdon.yttozara

import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Unit tests for the FfmpegInstaller bootstrap class.
 *
 * <p>
 * Tests cover:
 * <ul>
 *  <li> constructor message output level
 *  <li> successful download and copy/unzip flows
 *  <li> failure handling when downloads are missing, checksums don't match
 *       or unzip fails
 * </ul>
 */
@Newify([MockFor,FfmpegInstaller])
@groovy.util.logging.Log4j2('logger')
class FfmpegInstallerTest extends InstallerTestBase {

  void testConstructorSetsMessageOutputLevel() {
    logger.debug '> testConstructorSetsMessageOutputLevel'
    // Construct Installer - constructor sets messageOutputLevel on shared
    // AntBuilder
    FfmpegInstaller installer = FfmpegInstaller(installDir.absolutePath)
    assert installer.ant.project.buildListeners[0].
      messageOutputLevel == Project.MSG_WARN
    logger.debug '< testConstructorSetsMessageOutputLevel'
  }

  @Override
  Boolean validPayloadPath( String filePath ) {
    filePath =~ /.*ffmpeg-n.*zip$/
  }

  void testFfmpegGoodZipFileReturnsFalseWhenFileMissing() {
    logger.debug '> testFfmpegGoodZipFileReturnsFalseWhenFileMissing'

    MockFor(File).tap {
      demand.exists { ->
        logger.debug 'Mocked File.exists() -> false'
        false
      }
    }.use {
      antMock.demand.checksum(0) { }

      projectMock.use {
        antMock.use {
          FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
          assert installer.payloadIsGood == false
        }
      }
    }
    logger.debug '< testFfmpegGoodZipFileReturnsFalseWhenFileMissing'
  }

  /**
   * Helper that asserts the FfmpegInstaller.duration(...) result is within
   * tolerance of the expected elapsed milliseconds.
   */
  @Override
  void assertDurationApproximately(final long expectedElapsed) {
    final long startMillis = System.currentTimeMillis() - expectedElapsed

    projectMock.use {
      antMock.use {
        final FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
        final String dur = installer.duration(startMillis)
        final long parsed = parseDurationMillis(dur)

        assert parsed >= expectedElapsed
        assert parsed - expectedElapsed <= tolerance
      }
    }
  }

  @SuppressWarnings('JUnitTestMethodWithoutAssert')
  void testDurationZeroElapsed() {
    logger.debug '> testDurationZeroElapsed'
    durationZeroElapsed()
    logger.debug '< testDurationZeroElapsed'
  }

  @SuppressWarnings('JUnitTestMethodWithoutAssert')
  void testDurationShortElapsed() {
    logger.debug '> testDurationShortElapsed'
    durationShortElapsed()
    logger.debug '< testDurationShortElapsed'
  }

  @SuppressWarnings('JUnitTestMethodWithoutAssert')
  void testDurationLongElapsed() {
    logger.debug '> testDurationLongElapsed'
    durationLongElapsed()
    logger.debug '< testDurationLongElapsed'
  }

  void testInstallReturnsFfmpegInstallFailWhenZipMissing() {
    logger.debug '> testInstallReturnsFfmpegInstallFailWhenZipMissing'

    fileMock.demand.exists { false }

    runWithMocks(true) {
      final FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
      assert installer.install() == YtToZara.ffmpegInstallFail
    }

    logger.debug '< testInstallReturnsFfmpegInstallFailWhenZipMissing'
  }
}
