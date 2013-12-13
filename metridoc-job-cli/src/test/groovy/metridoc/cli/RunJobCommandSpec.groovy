package metridoc.cli

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/13/13
 * Time: 1:25 PM
 * To change this template use File | Settings | File Templates.
 */
class RunJobCommandSpec extends Specification {
    void "test adding imports to classpath" () {
        given:
        def complexJobPath = "src/testJobs/complexJob"
        def importsDir = new File(complexJobPath)
        if(!importsDir.exists()) {
            importsDir = new File("metridoc-job-cli/${complexJobPath}")
        }

        when:
        def classLoader = new GroovyClassLoader()
        RunJobCommand.addImportsToClassPath(classLoader, importsDir)

        then:
        classLoader.loadClass("entity.Bar")
    }
}
