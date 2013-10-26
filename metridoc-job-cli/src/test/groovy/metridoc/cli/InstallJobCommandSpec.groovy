package metridoc.cli

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 10/25/13
 * @author Tommy Barker
 */
class InstallJobCommandSpec extends Specification {

    def emptyOptions = new CliBuilder().parse([])
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()


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

    void "install a sub job in a zip file"() {
        given:
        def path = "src/test/testJobs/complexJob.zip"
        def inIntellij = !new File(path).exists()
        if(inIntellij) {
            path = "metridoc-job-cli/$path"
        }
        MetridocMain main = new MetridocMain(
                jobPath: temporaryFolder.root.path,
                args:["install", path, "metridoc-job-gorm"],
                exitOnFailure: false
        )

        when:
        main.run()

        then:
        noExceptionThrown()
        new File(temporaryFolder.root, "metridoc-job-gorm").exists()

        when:
        main = new MetridocMain(
                jobPath: temporaryFolder.root.path,
                args:["gorm"],
                exitOnFailure: false
        )
        String response = main.run()

        then:
        "gorm ran" == response
    }
}
