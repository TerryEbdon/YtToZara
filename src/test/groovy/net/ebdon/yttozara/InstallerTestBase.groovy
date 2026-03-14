package net.ebdon.yttozara

import groovy.mock.interceptor.MockFor

/**
 * Base class for the Installer bootstrap test classes.
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
abstract class InstallerTestBase extends AntTestBase {
  protected final long tolerance = 200L
  protected final long msPerSec = 1000L

  protected File    installDir
  protected String  installDirAbsolutePath
  protected MockFor fileMock

  protected InstallerTestBase() {
    super()
  }

  @Override
  void setUp() {
    super.setUp()
    final String root = File.listRoots().first()
    installDir = new File("${root}the-app-dir/bin")
    installDirAbsolutePath = installDir.absolutePath
    fileMock = MockFor(File)
  }

  abstract void testConstructorSetsMessageOutputLevel()
  abstract void testDurationZeroElapsed()
  abstract void testDurationShortElapsed()
  abstract void testDurationLongElapsed()
  abstract void assertDurationApproximately(final long expectedElapsed)
  abstract Boolean validPayloadPath( String filePath )

  protected void demandChecksumIsGood() {
    demandChecksum true
  }

  protected void demandChecksumIsBad() {
    demandChecksum false
  }

  protected void demandChecksum(Boolean expectedStatus) {
    demandGetProject()

    projectMock.demand.getProperties {
      logger.info 'Getting properties in InstallerTestBase.demandChecksum()'
      ['checksumVerified': expectedStatus ? 'true' : 'false', ]
    }
    antMock.demand.checksum { Map args ->
      logger.info "In checksum with args $args"
    }
  }

  /**
   * Parse a duration string of the form "Xm Ys Zms" and return total
   * milliseconds.
   */
  protected long parseDurationMillis(final String duration) {
    final java.util.regex.Matcher matcher = (duration =~ /(\d+)m (\d+)s (\d+)ms/)
    assert matcher.matches()
    final long minutes = matcher[0][1] as long
    final long seconds = matcher[0][2] as long
    final long milliseconds = matcher[0][3] as long
    minutes * 60_000L + seconds * msPerSec + milliseconds
  }

  void durationZeroElapsed() {
    logger.debug '> durationZeroElapsed'
    final long desiredElapsed = 0L
    assertDurationApproximately(desiredElapsed)
    logger.debug '< durationZeroElapsed'
  }

  void durationShortElapsed() {
    logger.debug '> durationShortElapsed'
    final long desiredElapsed = 123L
    assertDurationApproximately(desiredElapsed)
    logger.debug '< durationShortElapsed'
  }

  @SuppressWarnings('DuplicateNumberLiteral')
  void durationLongElapsed() {
    logger.debug '> durationLongElapsed'
    final long seconds = 2L
    final long desiredElapsed =
      (1L * 60L * msPerSec) + (seconds * msPerSec) + 345L // 1m 2s 345ms
    assertDurationApproximately(desiredElapsed)
    logger.debug '< durationLongElapsed'
  }

  /**
   * Run a closure with projectMock and antMock active, optionally also
   * wrapping with fileMock.
   *
   * @param useFileMock  when true fileMock.use() wraps the inner mocks;
   *                     defaults to true
   * @param c            the closure to execute inside the mock contexts
   */
  void runWithMocks(final boolean useFileMock = true, final Closure c) {
    if (useFileMock) {
      fileMock.use {
        projectMock.use {
          antMock.use {
            c.call()
          }
        }
      }
    } else {
      projectMock.use {
        antMock.use {
          c.call()
        }
      }
    }
  }

  protected Boolean validAlgorithm( String algName) {
    algName == 'SHA-256'
  }

  protected Boolean validChecksumSize(String checksum) {
    checksum.length() == 64
  }

  protected void demandChecksum() {
    logger.info 'In InstallerTestBase.demandChecksum()'
    antMock.demand.checksum { Map args ->
      logger.info 'in mock checksum()'
      assert validPayloadPath(args.file)
      assert validAlgorithm(args.algorithm)
      assert validChecksumSize(args.property)
      assert args.verifyProperty == 'checksumVerified'
      true
    }
  }
}
