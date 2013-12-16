package metridoc.cli

import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/13/13
 * Time: 12:29 PM
 * To change this template use File | Settings | File Templates.
 */
class ImportJobsCommandIntSpec extends AbstractFunctionalSpec {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    void "nothing happens if there is not import file"() {
        given:
        String path = temporaryFolder.root.canonicalPath

        when:
        int exitCode = runCommand(["import-jobs", "$path"])

        then:
        0 == exitCode
        output.contains("[import.groovy] does not exist, no jobs to import")
    }

    void "test full blown import with running a job referencing it"() {
        setup:
        File importFile = new File(temporaryFolder.root, "import.groovy")
        importFile.createNewFile()
        def path = "src/testJobs/complexJob.zip"
        def jobPath = new File(path)
        if (!jobPath.exists()) {
            jobPath = new File("metridoc-job-cli/${path}")
        }
        importFile.text = "foo = [url: '${jobPath.toURI().toURL().toString()}', path: 'metridoc-job-gorm']"
        def groovyDir = new File(temporaryFolder.root, "src/main/groovy")
        groovyDir.mkdirs()
        def metridocScript = new File(groovyDir, "metridoc.groovy")
        metridocScript.createNewFile()
        metridocScript.text = """
            import metridoc.service.gorm.GormService
            import foo.FooBar

            configure()
            includeService(GormService).enableFor(FooBar)
        """

        when:
        int exitCode = runCommand(["import-jobs", temporaryFolder.root.canonicalPath])

        then:
        0 == exitCode
        noExceptionThrown()

        when:
        exitCode = runCommand([temporaryFolder.root.canonicalPath, "--mergeMetridocConfig=false", "--embeddedDataSource"])

        then:
        0 == exitCode
        noExceptionThrown()
    }
}
