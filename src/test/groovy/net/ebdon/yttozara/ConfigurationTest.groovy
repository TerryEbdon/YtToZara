package net.ebdon.yttozara

import groovy.test.GroovyTestCase
import groovy.mock.interceptor.MockFor
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager

/**
 * Unit tests for the {@link Configuration} class.
 * <p>
 * These tests verify configuration loading, logging, version matching and
 * major version extraction logic. Mocks are used to simulate file and resource
 * access, ensuring correct behaviour for both custom and default config files.
 * </p>
 *
 * @author Terry Ebdon
 * @since v1.0.0
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
class ConfigurationTest extends GroovyTestCase {
  final private String configFilePath = 'file:/test/path/config.groovy'

  void testConstructorLoadsConfigAndLogs() {
    final URI uri = new URI(configFilePath)

    MockFor fileMock = MockFor(File).tap {
      demand.exists { true }
      demand.getAbsolutePath { '/test/path/config.groovy' }
      demand.toURI { uri }
    }

    Map configMap = [
      version: 'v1.0.0',
      silenceRemove: [
        startPeriods: 2,
        startSilence: 0.5,
        startThreshold: 1.5,
        stopSilence: 2.0,
        stopDuration: 5.0,
        startDuration: 0.2,
        enabled: true,
      ],
    ]

    MockFor configSlurperMock = MockFor(ConfigSlurper).tap {
      demand.parse { URL url -> configMap }
    }

    fileMock.use {
      configSlurperMock.use {
        final Configuration config = new Configuration()

        // Verify config loaded correctly
        assert config.config.version == 'v1.0.0'
      }
    }
  }

  @SuppressWarnings('JUnitTestMethodWithoutAssert')
  void testLogConfigOutputsExpectedDebugLogs() {
    logger.info 'testLogConfigOutputsExpectedDebugLogs start'
    new Configuration().with {
      config = [
        version: 'v1.2.3',
        silenceRemove: [
          startPeriods   : 1,
          startSilence   : 0.5,
          stopSilence    : 0.5,
          startThreshold : '-26dB',
          startDuration  : 0,
          stopDuration   : 1,
          enabled        : true,
        ],
      ]
      logConfig()
    }
    // No assertion: just ensure no exception and logs are called
    logger.info 'testLogConfigOutputsExpectedDebugLogs end'
  }

  void testLoadLoadingCustomConfigIfFileExists() {
    final URI uri = new URI(configFilePath)

    MockFor fileMock = MockFor(File).tap {
      demand.exists { true }
      demand.getAbsolutePath { '/tmp/config.groovy' }
      demand.toURI { uri }
    }

    MockFor configSlurperMock = MockFor(ConfigSlurper).tap {
      demand.parse { urlOrText ->
        [
          version: 'v2.0.0',
          silenceRemove: [
            startPeriods   : 1,
            startSilence   : 0.5,
            stopSilence    : 0.5,
            startThreshold : '-26dB',
            startDuration  : 0,
            stopDuration   : 1,
            enabled        : true,
          ],
        ]
      }
    }

    fileMock.use {
      configSlurperMock.use {
        Configuration config = new Configuration()
        assert config.config.version == 'v2.0.0'
      }
    }
  }

  void testLoadConfigLoadsDefaultConfigIfFileDoesNotExist() {
    MockFor fileMock = MockFor(File)
    fileMock.demand.exists { false }

    MockFor resourceManagerMock = MockFor(ClasspathResourceManager)
    resourceManagerMock.demand.getReader { name ->
      new StringReader(
"""
version = 'v3.0.0'
silenceRemove {
  startPeriods   = 1
  startSilence   = 0.5
  stopSilence    = 0.5
  startThreshold = '-26dB'
  startDuration  = 0
  stopDuration   = 1
  enabled        = true
}
""")
    }

    MockFor configSlurperMock = MockFor(ConfigSlurper)
    configSlurperMock.demand.parse { text ->
      [
        version: 'v3.0.0',
        silenceRemove: [
          startPeriods   : 1,
          startSilence   : 0.5,
          stopSilence    : 0.5,
          startThreshold : '-26dB',
          startDuration  : 0,
          stopDuration   : 1,
          enabled        : true,
        ],
      ]
    }

    fileMock.use {
      resourceManagerMock.use {
        configSlurperMock.use {
          Configuration config = new Configuration()
          assert config.config.version == 'v3.0.0'
        }
      }
    }
  }

  void testConfigMatchesAppReturnsTrueForMatchingMajorVersion() {
    Configuration config = new Configuration()
    config.config = [version: 'v1.2.3']
    config.metaClass.getAppVersion = { -> 'v1.9.9' }
    assert config.configMatchesApp() == true
  }

  void testConfigMatchesAppReturnsFalseForNonMatchingMajorVersion() {
    Configuration config = new Configuration()
    config.config = [version: 'v2.2.3']
    config.metaClass.getAppVersion = { -> 'v1.9.9' }
    assert config.configMatchesApp() == false
  }

  void testMajorReturnsCorrectMajorVersion() {
    new Configuration().with {
      assert major('v1.2.3') == 'v1'
      assert major('v2.0.0') == 'v2'
      assert major('vX.Y.Z') == 'vX'
      assert major('bad') == 'v-1'
      assert major(null) == 'v-1'
    }
  }

  @Newify(Package)
  void testGetAppVersionReturnsCorrectVersionString() {
    MockFor packageMock = MockFor(Package).tap {
      demand.getImplementationTitle { -> 'YtToZara' }
      demand.getImplementationVersion { -> 'v1.2.3' }
    }

    packageMock.use {
      Configuration config = new Configuration()
      config.class.metaClass.getPackage = { -> Package }
      assert config.appVersion == 'v1.2.3'
    }
  }
}
