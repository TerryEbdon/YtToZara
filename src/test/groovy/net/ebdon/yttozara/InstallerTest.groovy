package net.ebdon.yttozara

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
@groovy.util.logging.Log4j2('logger')
class InstallerTest extends AntTestBase {
  private File    installDir

  @Override
  void setUp() {
    super.setUp()
    installDir = new File(System.getProperty('java.io.tmpdir'),
      "yttozara_test_inst_${System.nanoTime()}")
    installDir.mkdirs()
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
      assert args.usetimestamp == true || args.usetimestamp == null
    }

    antMock.demand.copy { Map args ->
      assert args.file == Installer.ytDlpFile
      assert args.todir == installDir.absolutePath || args.todir == installDir
      assert args.flatten == true || args.flatten == null
    }

    projectMock.use {
      antMock.use {
        new Installer(installDir.absolutePath).installYtDlp()
      }
    }
    logger.debug '< testInstallYtDlpCopiesWhenDownloaded'
  }

  @groovy.test.NotYetImplemented
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

    File ffZip = new File(Installer.ffmpegFile) // simulated ffmpeg zip file
    ffZip.parentFile?.mkdirs()
    ffZip.createNewFile()
    ffZip.deleteOnExit()

    antMock.demand.get { Map args ->
      assert args.src  == Installer.ffmpegUrl
      assert args.dest == Installer.downloadDir
    }

    antMock.demand.unzip { Map args, Closure closure ->
      assert args.src == Installer.ffmpegFile
      assert args.dest == installDir.absolutePath || args.dest == installDir
      // Closure is not invoked
      assert closure != null
    }

    projectMock.use {
      antMock.use {
        new Installer(installDir.absolutePath).installFfmpeg()
      }
    }
    logger.debug '< testInstallFfmpegUnzipsWhenDownloaded'
  }

  void testInstallFfmpegHandlesUnzipExceptionGracefully() {
    logger.debug '> testInstallFfmpegHandlesUnzipExceptionGracefully'
    File ffZip = new File(Installer.ffmpegFile)
    ffZip.parentFile?.mkdirs()
    ffZip.createNewFile()
    ffZip.deleteOnExit()

    antMock.demand.with {
      get { Map args -> assert args.src == Installer.ffmpegUrl }
      antMock.demand.unzip { Map args, Closure closure ->
        throw new BuildException('simulated unzip failure')
      }
    }

    projectMock.use {
      antMock.use {
        // installFfmpeg catches BuildException/IOException and should not throw
        new Installer(installDir.absolutePath).installFfmpeg()
      }
    }
  logger.debug '< testInstallFfmpegHandlesUnzipExceptionGracefully'
  }
}
