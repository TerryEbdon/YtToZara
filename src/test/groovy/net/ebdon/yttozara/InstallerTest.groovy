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
 *  <li> failure handling when downloads are missing or unzip fails
 * </ul>
 * Each test creates a temporary install directory in setUp() and cleans it up
 * in tearDown(). The tests assert behaviour rather than performing real
 * network or zip operations.
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
class InstallerTest extends AntTestBase {
  private File    installDir
  private String  installDirAbsolutePath

  @Override
  void setUp() {
    super.setUp()
    installDir = new File(System.getProperty('java.io.tmpdir'),
      "yttozara_test_inst_${System.nanoTime()}")
    installDir.mkdirs()
    installDirAbsolutePath = installDir.absolutePath
  }

  @Override
  void tearDown() {
    installDir.deleteDir()
    super.tearDown()
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
    // Ensure a simulated downloaded yt-dlp exists
    File downloaded = new File(Installer.ytDlpFile)
    downloaded.parentFile?.mkdirs()
    downloaded.createNewFile()
    downloaded.deleteOnExit()

    antMock.demand.get { Map args ->
      assert args.src == Installer.ytDlpUrl
      assert args.dest == Installer.downloadDir
      assert args.usetimestamp == true
    }

    antMock.demand.copy { Map args ->
      assert args.file == Installer.ytDlpFile
      assert args.todir == installDir.absolutePath || args.todir == installDir
      assert args.flatten == true
    }

    projectMock.use {
      antMock.use {
        new Installer(installDir.absolutePath).installYtDlp()
      }
    }
    logger.debug '< testInstallYtDlpCopiesWhenDownloaded'
  }

  void testInstallYtDlpFailsWhenNotDownloaded() {
    logger.debug '> testInstallYtDlpFailsWhenNotDownloaded'
    // Ensure no downloaded yt-dlp file exists
    File downloaded = new File(Installer.ytDlpFile)
    if (downloaded.exists()) { downloaded.delete() }

    antMock.demand.get { Map args -> /* allow call */ }
    antMock.demand.fail { String msg ->
      throw new BuildException(msg)
    }

    projectMock.use {
      antMock.use {
        assert installDir?.exists()
        assert installDir.absolutePath != null
        shouldFail(BuildException) {
          new Installer(installDir.absolutePath).installYtDlp()
        }
      }
    }
    logger.debug '< testInstallYtDlpFailsWhenNotDownloaded'
  }

  void testInstallFfmpegUnzipsWhenDownloaded() {
    logger.debug '> testInstallFfmpegUnzipsWhenDownloaded'

    antMock.demand.get { Map args ->
      assert args.src  == Installer.ffmpegUrl
      assert args.dest == Installer.downloadDir
    }

    antMock.demand.unzip { Map args, Closure closure ->
      assert args.src == Installer.ffmpegZipPath
      assert args.dest == installDirAbsolutePath
      assert closure != null // Closure is not invoked
    }

    fileMock.use {
      projectMock.use {
        antMock.use {
          new Installer(installDirAbsolutePath).installFfmpeg()
        }
      }
    }
    logger.debug '< testInstallFfmpegUnzipsWhenDownloaded'
  }

  private static MockFor getFileMock() {
    MockFor(File).tap {
      demand.exists(2) {
        logger.debug 'Returning true for File.exists()'
        true
      }
    }
  }

  void testInstallFfmpegHandlesUnzipExceptionGracefully() {
    logger.debug '> testInstallFfmpegHandlesUnzipExceptionGracefully'

    antMock.demand.with {
      get { Map args -> assert args.src == Installer.ffmpegUrl }
      unzip { Map args, Closure closure ->
        throw new BuildException('simulated unzip failure')
      }
    }

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

    antMock.demand.checksum { Map args ->
      assert args.file == Installer.ffmpegZipPath
      assert args.algorithm == 'SHA-256'
      assert args.property == Installer.ffmpegExpectedSha
      assert args.verifyProperty == 'ffmpegIsGood'
    }

    demandChecksumIsGood()

    projectMock.use {
      antMock.use {
        Installer installer = new Installer(installDirAbsolutePath)
        assert installer.ffmpegChecksumIsGood
      }
    }

    logger.debug '< testFfmpegChecksumIsGoodReturnsTrueWhenAntSetsPropertyTrue'
  }

  void testFfmpegChecksumIsGoodReturnsFalseWhenAntPropertyNotTrue() {
    logger.debug '> testFfmpegChecksumIsGoodReturnsFalseWhenAntPropertyNotTrue'

    antMock.demand.checksum { Map args ->
      // simulate checksum running but verification failing
    }

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
      antMock.demand.checksum { Map args ->
      }

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

    Boolean checksumCalled = false
    antMock.demand.checksum { Map args ->
      checksumCalled = true
      assert args.file == Installer.ffmpegZipPath
      assert args.algorithm == 'SHA-256'
      assert args.property == Installer.ffmpegExpectedSha
      assert args.verifyProperty == 'ffmpegIsGood'
    }

    demandChecksumIsGood()

    projectMock.use {
      antMock.use {
        Installer installer = new Installer(installDirAbsolutePath)
        boolean result = installer.ffmpegChecksumIsGood
        assert checksumCalled
        assert result
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

  private void demandChecksum( Boolean expectedStatus ) {
    demandGetProject()

    projectMock.demand.getProperties {
      ['ffmpegIsGood': expectedStatus ? 'true' : 'false', ]
    }
  }
}
