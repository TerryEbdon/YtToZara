package net.ebdon.yttozara

import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project

/**
 * Unit tests for the Installer bootstrap class.
 *
 * Exercises Installer.installYtDlp() and Installer.installFfmpeg()
 * <p>
 * Tests cover:
 * <ul>
 *  <li> constructor message output level
 *  <li> successful download and copy/unzip flows
 * </ul>
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
class InstallerTest extends InstallerTestBase {

  void testConstructorSetsMessageOutputLevel() {
    logger.debug '> testConstructorSetsMessageOutputLevel'
    // Construct Installer - constructor sets messageOutputLevel on shared
    // AntBuilder
    Installer installer = new Installer(installDir.absolutePath)
    assert installer.ant.project.buildListeners[0].
      messageOutputLevel == Project.MSG_WARN
    logger.debug '< testConstructorSetsMessageOutputLevel'
  }

  void testInstallYtDlpCopiesWhenDownloaded() {
    logger.debug '> testInstallYtDlpCopiesWhenDownloaded'

    antMock.demand.get { Map args ->
      assert args.src == Installer.ytDlpUrl
      assert args.dest == Installer.downloadDir
      assert args.usetimestamp == true
    }

    antMock.demand.copy { Map args ->
      assert args.file == Installer.ytDlpFile
      assert args.todir == installDirAbsolutePath
      assert args.flatten == true
    }
    fileMock.demand.exists(2) { true }

    fileMock.use {
      projectMock.use {
        antMock.use {
          new Installer(installDirAbsolutePath).installYtDlp()
        }
      }
    }
    logger.debug '< testInstallYtDlpCopiesWhenDownloaded'
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
          assert new Installer(installDirAbsolutePath).
            installYtDlp() == YtToZara.ytDlpInstallFail
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
        final Installer installer = new Installer(installDirAbsolutePath)
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
