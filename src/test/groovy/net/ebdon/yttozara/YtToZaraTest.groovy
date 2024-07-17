package net.ebdon.yttozara

import groovy.test.GroovyTestCase
import groovy.mock.interceptor.MockFor

/**
 * Test the YtToZara class.
 */
@groovy.util.logging.Log4j2('logger')
@Newify(MockFor)
class YtToZaraTest extends GroovyTestCase {

  void testBinPath() {
    assert new YtToZara().binPath.contains('bin')
  }

  void testAddToTrackList() {
    String trackLine = 'boo.mp3'
    YtToZara ytToZara = new YtToZara()
    ytToZara.addToTrackList(trackLine)
    assert ytToZara.trackList.contains(trackLine)
  }
}
