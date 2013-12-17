package metridoc.utils

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created by tbarker on 12/16/13.
 */
class ArchiveMethodsSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    void "test converting zip name to directory file"() {

        given:
        def parent = temporaryFolder.newFolder("foo")
        when:
        def file = ArchiveMethods.convertZipNameToDirectory(parent, new File("metridoc-job-bar-v0.1.zip"))

        then:
        "metridoc-job-bar-v0.1" == file.name
        file.isDirectory()
        file.parentFile == parent
    }

    void "test dealing with optional paths"() {
        given:
        def parent = temporaryFolder.newFolder("foo")

        when:
        def file = ArchiveMethods.convertZipNameToDirectory(parent, new File("metridoc-job-bar-v0.1.zip"), "foobar")

        then:
        "foobar" == file.name
        file.isDirectory()
        file.parentFile == parent
    }
}
