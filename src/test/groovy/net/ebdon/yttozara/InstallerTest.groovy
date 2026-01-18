package net.ebdon.yttozara

import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project
import org.apache.tools.ant.BuildException

/**
 * Unit tests for the Installer bootstrap class.
 *
 * Exercises Installer.installYtDlp() and Installer.installFfmpeg()
 * <p>
 * Tests cover:
 * <ul>
 *  <li> constructor message output level
 *  <li> successful download and copy/unzip flows
 *  <li> failure handling when downloads are missing, checksums don't match
 *       or unzip fails
 * </ul>
 * The tests assert behaviour rather than performing real
 * network or zip operations.
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
class InstallerTest extends AntTestBase {
  private File    installDir
  private String  installDirAbsolutePath
  private MockFor fileMock

  @Override
  void setUp() {
    super.setUp()
    final String root = File.listRoots().first()
    installDir = new File("${root}the-app-dir/bin")
    installDirAbsolutePath = installDir.absolutePath
    fileMock = MockFor(File)
  }

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

  @SuppressWarnings('JUnitTestMethodWithoutAssert')
  void testInstallFfmpegUnzipsWhenDownloaded() {
    logger.debug '> testInstallFfmpegUnzipsWhenDownloaded'

    preUnzipDemands()
    demandUnzip()
    fileMock.demand.exists(2) { true }

    fileMock.use {
      projectMock.use {
        antMock.use {
          new Installer(installDirAbsolutePath).installFfmpeg()
        }
      }
    }
    logger.debug '< testInstallFfmpegUnzipsWhenDownloaded'
  }

  private void demandFfmpegGetUrl() {
    antMock.demand.get { Map args ->
      assert args.src == Installer.ffmpegUrl
      assert args.verbose == false
      assert args.usetimestamp == true
    }
  }

  private void assertFfmpegUnzipArgs( Map args, Closure closure ) {
    logger.info '> assertFfmpegUnzipArgs'
    assert args.src == Installer.ffmpegZipPath
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
          new Installer(installDirAbsolutePath).installFfmpeg()
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
        Installer installer = new Installer(installDirAbsolutePath)
        assert installer.ffmpegChecksumIsGood
      }
    }

    logger.debug '< testFfmpegChecksumIsGoodReturnsTrueWhenAntSetsPropertyTrue'
  }

  private void demandFfmpegChecksum() {
    antMock.demand.checksum { Map args ->
      assert args.file == Installer.ffmpegZipPath
      assert args.algorithm == 'SHA-256'
      assert args.property == Installer.ffmpegExpectedSha
      assert args.verifyProperty == 'ffmpegIsGood'
    }
  }

  void testFfmpegChecksumIsGoodReturnsFalseWhenAntPropertyNotTrue() {
    logger.debug '> testFfmpegChecksumIsGoodReturnsFalseWhenAntPropertyNotTrue'

    demandFfmpegChecksum()
    demandChecksumIsBad()

    projectMock.use {
      antMock.use {
        Installer installer = new Installer(installDirAbsolutePath)
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
          Installer installer = new Installer(installDirAbsolutePath)
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
          Installer installer = new Installer(installDirAbsolutePath)
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
        Installer installer = new Installer(installDirAbsolutePath)
        assert installer.ffmpegChecksumIsGood
      }
    }

    logger.debug '< testFfmpegChecksumInvokesAntChecksumWithCorrectArguments'
  }

  private void demandChecksumIsGood() {
    demandChecksum true
  }

  private void demandChecksumIsBad() {
    demandChecksum false
  }

  private void demandChecksum(Boolean expectedStatus) {
    demandGetProject()

    projectMock.demand.getProperties {
      ['ffmpegIsGood': expectedStatus ? 'true' : 'false', ]
    }
  }
}
