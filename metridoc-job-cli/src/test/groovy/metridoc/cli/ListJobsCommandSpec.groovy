package metridoc.cli;

import java.io.File;
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: intern
 * Date: 10/30/13
 * Time: 1:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ListJobsCommandSpec extends Specification {

    def emptyOptions = new CliBuilder().parse([])
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    void "listJobs works when no directory set"() {
        MetridocMain main = new MetridocMain(
            jobPath: temporaryFolder.root.path
        )

        when:
        new ListJobsCommand().run(emptyOptions)

        then:
        noExceptionThrown()
    }
}