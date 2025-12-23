package net.ebdon.yttozara

import groovy.mock.interceptor.MockFor
import groovy.ant.AntBuilder
import org.apache.tools.ant.Project
import groovy.test.GroovyTestCase

/**
 * Base test class providing common Ant/Project mocks for unit tests that
 * exercise AntBuilder-based functionality.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *  <li>Provide reusable MockFor instances for AntBuilder and Project
 *  <li>Initialise common constructor demands used by classes that construct
 *      an AntBuilder/Project pair (for example Ffmpeg and Installer)
 *  <li>Keep tests concise by centralising mock setup in setUp()
 * </ul>
 *
 * Usage:
 * <pre>
 * class MyAntUsingTest extends AntTestBase {
 *   void testSomething() {
 *     projectMock.use {
 *       antMock.use {
 *         // exercise code that uses new AntBuilder() / Project
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * Notes:
 * <ul>
 *  <li>setUp() initialises antMock and projectMock and calls
 *      constructorDemands()
 *  <li>constructorDemands() sets expectations for Project.getMSG_WARN,
 *      Project.getBuildListeners and AntBuilder.getProject so tests that
 *      construct objects depending on those calls do not hit the real
 *      Ant implementation
 * </ul>
 */
@Newify(MockFor)
@groovy.util.logging.Log4j2('logger')
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AntTestBase extends GroovyTestCase {
  MockFor antMock
  MockFor projectMock

  @Override
  void setUp() {
    super.setUp()
    antMock     = MockFor(AntBuilder)
    projectMock = MockFor(Project)
    constructorDemands()
  }

  private void constructorDemands() {
    projectMock.demand.getMSG_WARN { 1 }
    projectMock.demand.getBuildListeners {
      [
        [messageOutputLevel:4,],
      ]
    }

    demandGetProject()
  }

  void demandGetProject() {
    antMock.demand.getProject {
      logger.trace 'getProject() called'
      new Project()
    }
  }
}
