package metridoc.cli

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/13/13
 * Time: 10:38 AM
 * To change this template use File | Settings | File Templates.
 */
class ImportJobsCommandSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    void "check preparing mdoc directory"() {
        def mdoc = new File(temporaryFolder.root, ".mdoc")
        def willBeDeleted = new File(mdoc, "willBeDeleted")

        when:
        ImportJobsCommand.prepareMdocDirectory(temporaryFolder.root)

        then:
        mdoc.exists()
        mdoc.isDirectory()
        willBeDeleted.mkdir()

        when:
        ImportJobsCommand.prepareMdocDirectory(temporaryFolder.root)

        then:
        !willBeDeleted.exists()
    }

    void "the target project we are importing for must be a directory and exist"() {
        when:
        def doesNotExist = new File(temporaryFolder.root, "doesNotExist")
        ImportJobsCommand.checkProject(doesNotExist)

        then:
        def error = thrown(AssertionError)
        error.message.contains(ImportJobsCommand.DOES_NOT_EXIST(doesNotExist))

        when:
        def notADirectory = new File(temporaryFolder.root, "notADirectory")
        notADirectory.createNewFile()
        ImportJobsCommand.checkProject(notADirectory)

        then:
        error = thrown(AssertionError)
        error.message.contains(ImportJobsCommand.NOT_DIRECTORY(notADirectory))

        when:
        def directory = new File(temporaryFolder.root, "directory")
        directory.mkdir()
        ImportJobsCommand.checkProject(directory)

        then:
        noExceptionThrown()
    }

    void "can test if the import file exists"() {
        when:
        boolean importExists = ImportJobsCommand.importFileExists(temporaryFolder.root)

        then:
        !importExists

        when:
        new File(temporaryFolder.root, ImportJobsCommand.IMPORT_FILE).createNewFile()
        importExists = ImportJobsCommand.importFileExists(temporaryFolder.root)

        then:
        importExists
    }

    void "test retrieving import hash"() {
        when:
        Map<String, ImportUrl> importHash = ImportJobsCommand.getImportHash(
                "foo=[url:'http://foo.com'];blam='baz';foobar=[noUrl:'blah']"
        )

        then:
        1 == importHash.size()
        "http://foo.com" == importHash.foo.url.toString()
    }

    void "test retrieving the destination"() {
        when:
        File destination = ImportJobsCommand.getDestination(temporaryFolder.root, new URL("http://foo.com/foo.zip"))

        then:
        "foo.zip" == destination.name
        temporaryFolder.root == destination.parentFile
    }

    void "if there is no import file, then nothing happens"() {
        when:
        ImportJobsCommand.addImports(temporaryFolder.root)

        then:
        noExceptionThrown()
    }

    void "test full blown import"() {
        setup:
        setupFullBlownImport()

        when:
        ImportJobsCommand.addImports(temporaryFolder.root)

        then:
        def mdoc = new File(temporaryFolder.root, ".mdoc")
        mdoc.exists()
        new File(mdoc, "metridoc-job-gorm").exists()
    }

    protected void setupFullBlownImport() {
        File importFile = new File(temporaryFolder.root, ImportJobsCommand.IMPORT_FILE)
        importFile.createNewFile()
        def path = "src/testJobs/complexJob.zip"
        def jobPath = new File(path)
        if (!jobPath.exists()) {
            jobPath = new File("metridoc-job-cli/${path}")
        }
        importFile.text = "foo = [url: '${jobPath.toURI().toURL().toString()}', path: 'metridoc-job-gorm']"
    }

    void "the command must be 'import-jobs' for command to run"() {
        setup:
        setupFullBlownImport()
        def args = ["import-jobs", temporaryFolder.root as String]
        def options = new CliBuilder().parse(args)
        def badArgs = ["blah", temporaryFolder.root as String]
        def badOptions = new CliBuilder().parse(badArgs)

        when:
        def result = new ImportJobsCommand().run(options)

        then:
        noExceptionThrown()
        result

        when:
        result = new ImportJobsCommand().run(badOptions)

        then:
        !result
    }
}
