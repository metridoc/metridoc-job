package metridoc.cli

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/25/13
 * @author Tommy Barker
 */
class InstallJobCommandSpec extends Specification {

    def emptyOptions = new CliBuilder().parse([])

    void "job path must be set"() {
        when:
        new InstallJobCommand().run(emptyOptions)

        then:
        thrown(AssertionError)
    }

    void "options must not be null"() {
        given:
        MetridocMain main = new MetridocMain(jobPath: "foo")

        when:
        new InstallJobCommand(main: main).run(null)

        then:
        thrown(AssertionError)
    }
}
