package net.ebdon.yttozara

import groovy.test.GroovyTestCase
import groovy.mock.interceptor.MockFor

/**
 * Test the YtToZara class.
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
class YtToZaraTest extends GroovyTestCase {

  @Override
  void tearDown() {
    super.tearDown()
    YtToZara.metaClass = null // Undo metaClass changes
  }

  void testMainInstallFfmpegCallsRun() {
    logger.info '> testMainInstallFfmpegCallsRun'

    MockFor systemMock = demandSystemExit(YtToZara.success)
    final String mainArgs = 'install-ffmpeg'
    Boolean runCalled = false

    YtToZara.metaClass.run = { final String[] runArgs ->
      logger.info 'Fielded call to YtToZara.run()'
      assert runArgs.size() == 1 && runArgs.first() == mainArgs
      runCalled = true
      YtToZara.success
    }

    systemMock.use {
      YtToZara.main('install-ffmpeg')
    }
    assert runCalled
    logger.info '< testMainInstallFfmpegCallsRun'
  }

  void testRunCallsFfmpegInstallerInstall() {
    logger.info '> testRunCallsFfmpegInstallerInstall'

    Boolean installCalled = false
    MockFor ffmpegInstallerMock = MockFor(FfmpegInstaller).tap {
      demand.install {
        logger.info 'Fielded call to FfmpegInstaller.install()'
        installCalled = true
        YtToZara.success
      }
    }

    ffmpegInstallerMock.use {
      YtToZara ytToZara = new YtToZara()
      final int result = ytToZara.run(['install-ffmpeg'] as String[])
      assert result == YtToZara.success
    }
    assert installCalled
    logger.info '< testRunCallsFfmpegInstallerInstall'
  }

  void testRunCallsInstallerInstallYtDlp() {
    logger.info '> testRunCallsInstallerInstallYtDlp'
    MockFor installerMock = MockFor(Installer).tap {
      demand.installYtDlp {
        logger.info 'Fielded call to install.installYtDlp()'
        YtToZara.success
      }
    }

    installerMock.use {
      assert new YtToZara().run('install-ytdlp') == YtToZara.success
    }
    logger.info '< testRunCallsInstallerInstallYtDlp'
  }

  void testMainTooManyArgs() {
    logger.info '> testMainTooManyArgs'

    MockFor systemMock = demandSystemExit(YtToZara.ytToZaraTooManyArgs)
    systemMock.use {
      YtToZara.main 'Too many arguments'.split()
    }
    logger.info '< testMainTooManyArgs'
  }

  void testMainCallsGuessMp3Tags() {
    logger.info '> testMainCallsGuessMp3Tags'
    Boolean guessMp3TagsCalled = false
    MockFor systemMock = demandSystemExit(YtToZara.success)

    YtToZara.metaClass.guessMp3Tags = { final String trackFileName ->
      logger.info 'Fielded call to YtToZara.guessMp3Tags()'
      guessMp3TagsCalled = true
    }

    systemMock.use {
      YtToZara.main 'some-track.mp3'
    }
    assert guessMp3TagsCalled
    logger.info '< testMainCallsGuessMp3Tags'
  }

  private MockFor demandSystemExit(final int expectedStatus) {
    MockFor(System).tap {
      demand.exit { int exitStatus ->
        assert exitStatus == expectedStatus
      }
    }
  }

  void testMainConvertYtToZaraCalled() {
    logger.info '> testMainConvertYtToZaraCalled'

    MockFor systemMock = demandSystemExit(YtToZara.success)

    Boolean convertYtToZaraCalled = false
    YtToZara.metaClass.convertYtToZara = {
      logger.info 'Fielded call to YtToZara.convertYtToZara()'
      convertYtToZaraCalled = true
      YtToZara.success
    }

    systemMock.use {
      YtToZara.main()
    }
    assert convertYtToZaraCalled
    logger.info '< testMainConvertYtToZaraCalled'
  }

  void testAddToTrackList() {
    String trackLine = 'boo.mp3'
    YtToZara ytToZara = new YtToZara()
    ytToZara.addToTrackList(trackLine)
    assert ytToZara.trackList.contains(trackLine)
  }

  void testTrimSilence() {
    logger.info 'testTrimSilence start'
    MockFor ffmpegMock = MockFor( Ffmpeg )
    ffmpegMock.demand.with {
      trimSilence { def trackList ->
        logger.info 'Trimming track list'
        assert trackList.size() > 0
        assert trackList.first()[-4..-1] == '.mp3'
      }
    }

    ffmpegMock.use {
      YtToZara yttozara = new YtToZara()
      yttozara.trackList = ['hello.mp3']
      yttozara.trimSilence()
    }
    logger.info 'testTrimSilence end'
  }

  void testGuessMp3Tags() {
    logger.info 'testGuessMp3Tags start'
    final String trackArtist = 'Me'
    final String trackTitle  = 'My Great Track'
    final String trackFileName = "$trackArtist - $trackTitle"

    MockFor ffmpegMock = MockFor( Ffmpeg )
    ffmpegMock.demand.with {
      applyTags { String fileName, String artist, String title ->
        assert fileName == trackFileName
        assert artist   == trackArtist
        assert title    == trackTitle
      }
    }

    MockFor fileMock = MockFor(File)
    fileMock.demand.exists { false } //jsonFile.exists() == false

    ffmpegMock.use {
      YtToZara yttozara = new YtToZara()
      yttozara.guessMp3Tags(trackFileName)
    }

    logger.info 'testGuessMp3Tags end'
  }

  void testJsonFileName() {
    logger.info '> testJsonFileName'
    final String expectedFileType = 'info.json'
    List<List<String>> testFileNames = [
      ['blah.mp3',"blah.${expectedFileType}"],
      ['c:\\someDir\\blah.mp3',"c:\\someDir\\blah.${expectedFileType}"],
    ]

    YtToZara ytToZara = new YtToZara()
    testFileNames.each { pair ->
      assert ytToZara.jsonFileName(pair.first()) == pair.last()
    }
    logger.info '< testJsonFileName'
  }
}
