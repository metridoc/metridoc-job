package metridoc.cli

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/25/13
 * @author Tommy Barker
 */
class InstallMdocDependenciesCommandSpec extends Specification {

    void "test check for whether or not we should install dependencies"() {
        given:
        boolean answer

        when:
        answer = InstallMdocDependenciesCommand.dependenciesExistHelper("java.lang.String")

        then:
        answer

        when:
        answer = InstallMdocDependenciesCommand.dependenciesExistHelper("foo.bar.DoesNotExist")

        then:
        !answer

        when: "calling installDependencies by default"
        answer = InstallMdocDependenciesCommand.dependenciesExist()

        then: "the answer should be false since all dependencies will be available during unit tests"
        answer
    }

    void "test retrieving file name from url"() {
        when:
        def fileName = InstallMdocDependenciesCommand.getFileName("http://jcenter.bintray.com/org/grails/grails-spring/2.2.3/grails-spring-2.2.3.jar")

        then:
        "grails-spring-2.2.3.jar" == fileName
    }
}
