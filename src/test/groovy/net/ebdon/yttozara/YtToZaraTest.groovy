package net.ebdon.yttozara

import groovy.test.GroovyTestCase
import groovy.mock.interceptor.MockFor

/**
 * Test the YtToZara class.
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
class YtToZaraTest extends GroovyTestCase {

  void testAddToTrackList() {
    String trackLine = 'boo.mp3'
    YtToZara ytToZara = new YtToZara()
    ytToZara.addToTrackList(trackLine)
    assert ytToZara.trackList.contains(trackLine)
  }

  void testTrimSilence() {
    logger.info 'testTrimSilence start'
    MockFor ffmpegMock = MockFor( Ffmpeg )
    ffmpegMock.demand.trimSilence { def trackList ->
      logger.info 'Trimming track list'
      assert trackList.size() > 0
      assert trackList.first()[-4..-1] == '.mp3'
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
    ffmpegMock.demand.applyTags { String fileName, String artist, String title ->
      assert fileName == trackFileName
      assert artist   == trackArtist
      assert title    == trackTitle
    }

    MockFor fileMock = MockFor(File)
    fileMock.demand.exists { false } //jsonFile.exists() == false

    ffmpegMock.use {
      YtToZara yttozara = new YtToZara()
      yttozara.guessMp3Tags(trackFileName)
    }

    logger.info 'testGuessMp3Tags end'
  }
}
