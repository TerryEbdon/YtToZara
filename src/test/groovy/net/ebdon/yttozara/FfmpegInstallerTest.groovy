package net.ebdon.yttozara

import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project
import org.apache.tools.ant.BuildException

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

  @SuppressWarnings('JUnitTestMethodWithoutAssert')
  void testInstallFfmpegUnzipsWhenDownloaded() {
    logger.debug '> testInstallFfmpegUnzipsWhenDownloaded'

    preUnzipDemands()
    demandUnzip()
    fileMock.demand.exists(2) { true }

    fileMock.use {
      projectMock.use {
        antMock.use {
          new FfmpegInstaller(installDirAbsolutePath).install()
        }
      }
    }
    logger.debug '< testInstallFfmpegUnzipsWhenDownloaded'
  }

  private void demandFfmpegGetUrl() {
    antMock.demand.get { Map args ->
      assert args.src == FfmpegInstaller.ffmpegUrl
      assert args.verbose == false
      assert args.usetimestamp == true
    }
  }

  private void assertFfmpegUnzipArgs( Map args, Closure closure ) {
    logger.info '> assertFfmpegUnzipArgs'
    assert args.src == FfmpegInstaller.ffmpegZipPath
    assert args.dest == installDirAbsolutePath
    assert closure != null // Closure is not invoked
    logger.info '< assertFfmpegUnzipArgs'
  }

  private void demandUnzip(Boolean withException = false) {
    logger.info "> demandUnzip >${withException}<"
    antMock.demand.unzip { Map args, Closure closure ->
      assertFfmpegUnzipArgs args, closure
      if (withException) {
        throw new BuildException('simulated unzip failure')
      }
    }
    logger.info '< demandUnzip'
  }

  private void preUnzipDemands() {
    demandFfmpegGetUrl()
    demandFfmpegChecksum()
    demandChecksumIsGood()
  }

  @SuppressWarnings('JUnitTestMethodWithoutAssert')
  void testInstallFfmpegHandlesUnzipExceptionGracefully() {
    logger.debug '> testInstallFfmpegHandlesUnzipExceptionGracefully'

    preUnzipDemands()
    demandUnzip true
    fileMock.demand.exists(2) { true }

    fileMock.use {
      projectMock.use {
        antMock.use {
          // installFfmpeg catches BuildException/IOException & should not throw
          new FfmpegInstaller(installDirAbsolutePath).install()
        }
      }
    }
    logger.debug '< testInstallFfmpegHandlesUnzipExceptionGracefully'
  }

  void testFfmpegChecksumIsGoodReturnsTrueWhenAntSetsPropertyTrue() {
    logger.debug '> testFfmpegChecksumIsGoodReturnsTrueWhenAntSetsPropertyTrue'

    demandFfmpegChecksum()
    demandChecksumIsGood()

    projectMock.use {
      antMock.use {
        FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
        assert installer.ffmpegChecksumIsGood
      }
    }

    logger.debug '< testFfmpegChecksumIsGoodReturnsTrueWhenAntSetsPropertyTrue'
  }

  private void demandFfmpegChecksum() {
    antMock.demand.checksum { Map args ->
      assert args.file == FfmpegInstaller.ffmpegZipPath
      assert args.algorithm == 'SHA-256'
      assert args.property == FfmpegInstaller.ffmpegExpectedSha
      assert args.verifyProperty == 'ffmpegIsGood'
    }
  }

  void testFfmpegChecksumIsGoodReturnsFalseWhenAntPropertyNotTrue() {
    logger.debug '> testFfmpegChecksumIsGoodReturnsFalseWhenAntPropertyNotTrue'

    demandFfmpegChecksum()
    demandChecksumIsBad()

    projectMock.use {
      antMock.use {
        FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
        assert installer.ffmpegChecksumIsGood == false
      }
    }

    logger.debug '< testFfmpegChecksumIsGoodReturnsFalseWhenAntPropertyNotTrue'
  }

  void testFfmpegGoodZipFileReturnsTrueWhenFileExistsAndChecksumGood() {
    logger.debug(
      '> testFfmpegGoodZipFileReturnsTrueWhenFileExistsAndChecksumGood'
    )
    // File.exists() will be called once by getFfmpegGoodZipFile
    MockFor(File).tap {
      demand.exists { ->
        logger.debug 'Mocked File.exists() -> true'
        true
      }
    }.use {
      demandFfmpegChecksum()
      demandChecksumIsGood()

      projectMock.use {
        antMock.use {
          FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
          assert installer.ffmpegGoodZipFile
        }
      }
    }

    logger.debug(
      '< testFfmpegGoodZipFileReturnsTrueWhenFileExistsAndChecksumGood'
    )
  }

  void testFfmpegGoodZipFileReturnsFalseWhenFileMissing() {
    logger.debug '> testFfmpegGoodZipFileReturnsFalseWhenFileMissing'

    // File.exists() returns false; checksum should not be invoked.
    MockFor(File).tap {
      demand.exists { ->
        logger.debug 'Mocked File.exists() -> false'
        false
      }
    }.use {
      // Do not expect ant.checksum to be called; ensure test fails if it is.
      antMock.demand.checksum(0) { }

      projectMock.use {
        antMock.use {
          FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
          assert installer.ffmpegGoodZipFile == false
        }
      }
    }

    logger.debug '< testFfmpegGoodZipFileReturnsFalseWhenFileMissing'
  }

  void testFfmpegChecksumInvokesAntChecksumWithCorrectArguments() {
    logger.debug '> testFfmpegChecksumInvokesAntChecksumWithCorrectArguments'

    demandFfmpegChecksum()
    demandChecksumIsGood()

    projectMock.use {
      antMock.use {
        FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
        assert installer.ffmpegChecksumIsGood
      }
    }

    logger.debug '< testFfmpegChecksumInvokesAntChecksumWithCorrectArguments'
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

  void testInstallReturnsFfmpegInstallFailWhenChecksumBad() {
    logger.debug '> testInstallReturnsFfmpegInstallFailWhenChecksumBad'

    demandFfmpegGetUrl()
    fileMock.demand.exists(2) { true }
    demandFfmpegChecksum()
    demandChecksumIsBad()

    runWithMocks(true) {
      final FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
      assert installer.install() == YtToZara.ffmpegInstallFail
    }

    logger.debug '< testInstallReturnsFfmpegInstallFailWhenChecksumBad'
  }

  void testInstallReturnsSuccessWhenZipGoodAndUnzipSucceeds() {
    logger.debug '> testInstallReturnsSuccessWhenZipGoodAndUnzipSucceeds'

    preUnzipDemands()
    demandUnzip()
    fileMock.demand.exists(2) { true }

    runWithMocks(true) {
      final FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
      assert installer.install() == YtToZara.success
    }

    logger.debug '< testInstallReturnsSuccessWhenZipGoodAndUnzipSucceeds'
  }

  void testInstallReturnsFfmpegUnzipFailWhenUnzipThrows() {
    logger.debug '> testInstallReturnsFfmpegUnzipFailWhenUnzipThrows'

    preUnzipDemands()
    demandUnzip(true)
    fileMock.demand.exists(2) { true }

    runWithMocks(true) {
      final FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
      assert installer.install() == YtToZara.ffmpegUnzipFail
    }

    logger.debug '< testInstallReturnsFfmpegUnzipFailWhenUnzipThrows'
  }

  void testInstallReturnsFfmpegInstallFailWhenDownloadThrows() {
    logger.debug '> testInstallReturnsFfmpegInstallFailWhenDownloadThrows'

    fileMock.demand.exists { true }  // installPath exists
    antMock.demand.get { Map args ->
      throw new BuildException('simulated download failure')
    }

    runWithMocks {
      final FfmpegInstaller installer = FfmpegInstaller(installDirAbsolutePath)
      assert installer.install() == YtToZara.ffmpegInstallFail
    }

    logger.debug '< testInstallReturnsFfmpegInstallFailWhenDownloadThrows'
  }
}
