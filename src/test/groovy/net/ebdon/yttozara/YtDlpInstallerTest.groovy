package net.ebdon.yttozara

import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Unit tests for the Installer bootstrap class.
 *
 * Exercises Installer.installYtDlp()
 * <p>
 * Tests cover:
 * <ul>
 *  <li> constructor message output level
 *  <li> successful download and copy flows
 * </ul>
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
class YtDlpInstallerTest extends InstallerTestBase {

  void testConstructorSetsMessageOutputLevel() {
    logger.debug '> testConstructorSetsMessageOutputLevel'
    // Construct Installer - constructor sets messageOutputLevel on shared
    // AntBuilder
    Installer installer = new YtDlpInstaller(installDir.absolutePath)
    assert installer.ant.project.buildListeners[0].
      messageOutputLevel == Project.MSG_WARN
    logger.debug '< testConstructorSetsMessageOutputLevel'
  }

  @Override Boolean validPayloadPath( String filePath ) {
    filePath.contains(YtDlpInstaller.ytDlpExe)
  }

  void testInstallYtDlpFailsWhenNotDownloaded() {
    logger.debug '> testInstallYtDlpFailsWhenNotDownloaded'

    antMock.demand.get { Map args -> /* allow call */ }
    fileMock.demand.exists { true }
    fileMock.demand.exists { false }

    fileMock.use {
      projectMock.use {
        antMock.use {
          assert installDirAbsolutePath != null
          assert new YtDlpInstaller(installDirAbsolutePath).
            install() == YtToZara.ytDlpInstallFail
        }
      }
    }
    logger.debug '< testInstallYtDlpFailsWhenNotDownloaded'
  }

  /**
   * Helper that asserts the Installer.duration(...) result is within
   * tolerance of the expected elapsed milliseconds.
   */
  @Override
  void assertDurationApproximately(final long expectedElapsed) {
    final long startMillis = System.currentTimeMillis() - expectedElapsed

    projectMock.use {
      antMock.use {
        final Installer installer = new YtDlpInstaller(installDirAbsolutePath)
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
}
