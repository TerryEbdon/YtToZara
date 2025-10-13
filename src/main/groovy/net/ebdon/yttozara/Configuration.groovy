package net.ebdon.yttozara

import groovy.ant.AntBuilder
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager

/**
 * Handles loading and management of application configuration.
 * <p>
 * Loads configuration from a local file if present, otherwise falls back to a default resource.
 * Provides access to configuration properties and version compatibility checks.
 * </p>
 * <ul>
 *   <li>Uses Log4J2 for logging.</li>
 *   <li>Supports dynamic property access via {@code propertyMissing}.</li>
 * </ul>
 *
 * @author Terry Ebdon
 * @since v2.1.0
 */
@groovy.util.logging.Log4j2
class Configuration {
  static final String configFileName = 'config.groovy'
  final String currentDir = '.'
  Map config

  Configuration() {
    loadConfig()
    logConfig()
  }

  /**
   * Logs key configuration values for debugging purposes.
   */
  void logConfig() {
    log.with {
      debug "version         : ${config.version}"
      debug 'Silence detecion config:'
      debug "startPeriods  : ${config.silenceRemove.startPeriods}"
      debug "startSilence  : ${config.silenceRemove.startSilence}"
      debug "startThreshold: ${config.silenceRemove.startThreshold}"
      debug "stopSilence   : ${config.silenceRemove.stopSilence}"
      debug "stopDuration  : ${config.silenceRemove.stopDuration}"
      debug "startDuration : ${config.silenceRemove.startDuration}"
      debug "enabled       : ${config.silenceRemove.enabled}"
    }
  }

  /**
   * Loads configuration from file or classpath resource.
   * Logs the process and fails if no configuration can be loaded.
   */
  private void loadConfig() {
    log.info "Loading config from $configFileName"
    log.info 'Current folder is ' + currentDir
    log.debug 'package:  ' + getClass().packageName
    log.debug 'Class is: ' + getClass().name

    final File configFile = new File( configFileName )

    if ( configFile.exists() ) {
      log.info 'Using custom configuration from file:\n' +
        "  ${configFile.absolutePath}"
      config = new ConfigSlurper().parse( configFile.toURI().toURL())
    } else {
      log.info 'Using default configuration.'
      ClasspathResourceManager resourceManager = new ClasspathResourceManager()
      final Reader configScript = resourceManager.getReader(configFileName)
      if ( configScript ) {
        final String scriptText = configScript.text
        log.debug scriptText
        config = new ConfigSlurper().parse( scriptText )
        log.trace "config: ${config}"
      } else {
        new AntBuilder().fail "Couldn't load resource for configuration script."
      }
    }
   log.debug 'Resource loaded'
  }

  /**
   * Checks if the loaded configuration version matches the application version.
   *
   * @return {@code true} if major versions match, {@code false} otherwise
   */
  Boolean configMatchesApp() {
    Boolean versionMatched =
        major(appVersion) == major(config.version.toString())
    log.info versionMatched ?
      'Config is compatible with this app version' :
      'Config is NOT compatible with this app version'

    versionMatched
  }

  /**
   * Extracts the major version from a version string.
   *
   * @param versionNumber the version string (e.g. "v1.2.3")
   * @return the major version (e.g. "v1"), or "v-1" if unknown
   */
  final String major( final String versionNumber ) {
    final String unknownVersion = 'v-1'
    final int minVersionLength  = 'vX.Y.Z'.size()
    if (versionNumber?.size() >= minVersionLength ) {
      List<String> versionParts = versionNumber.split(/\./)
      versionParts.size() ? versionParts.first() : unknownVersion
    } else {
      unknownVersion
    }
  }

  /**
   * Gets the application version from package metadata.
   *
   * @return the application version string
   */
   String getAppVersion() {
    final Package myPackage  = this.class.package
    final String title       = myPackage.implementationTitle
    final String version     = myPackage.implementationVersion
    final String format      = '%s %s'

    log.debug String.format(format, title, version)
    version
  }

  /**
   * Provides dynamic access to configuration properties.
   *
   * @param propertyName the name of the property to look up
   * @return the value of the property, or {@code null} if not found
   */
  Object propertyMissing(String propertyName) {
    log.info "Lookup '$propertyName' in config"
    config[propertyName]
  }
}
