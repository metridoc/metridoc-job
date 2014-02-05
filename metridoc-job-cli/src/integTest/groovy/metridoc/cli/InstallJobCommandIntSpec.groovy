/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.cli

import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 * Created with IntelliJ IDEA on 8/15/13
 * @author Tommy Barker
 */
class InstallJobCommandIntSpec extends AbstractFunctionalSpec {

    @Rule
    public TemporaryFolder temporaryFolder

    void "test install job"() {
        when:
        def bar1 = new File("${temporaryFolder.root.path}/metridoc-job-bar-0.1")
        def bar2 = new File("${temporaryFolder.root.path}/metridoc-job-bar-0.2")
        int exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "install", "src/testJobs/metridoc-job-bar-0.1.zip"])

        then:
        0 == exitCode
        bar1.exists()

        when:
        exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "install", "src/testJobs/metridoc-job-bar-0.2.zip"])

        then:
        0 == exitCode
        !bar1.exists()
        bar2.exists()

        cleanup:
        bar1.deleteDir()
        bar2.deleteDir()
    }

    void "test installing a directory"() {
        when:
        def simpleJobUnVersioned = new File("${temporaryFolder.root.path}/metridoc-job-simpleJob")
        int exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}","install", "src/testJobs/simpleJob"])

        then:
        0 == exitCode
        simpleJobUnVersioned.exists()

        when: "installing it again"
        exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "install", "src/testJobs/simpleJob"])

        then: "old one should be deleted, new one installed"
        output.contains("upgrading metridoc-job-simpleJob")
        0 == exitCode
        simpleJobUnVersioned.exists()

        when:
        if(!System.getProperty("os.name").contains("indows")) {
            exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "simpleJob", "--mergeMetridocConfig=false", "--embeddedDataSource"])
        }

        then:
        0 == exitCode
        if(!System.getProperty("os.name").contains("indows")) {
            output.contains("foo ran")
        }

        cleanup:
        simpleJobUnVersioned.deleteDir()
    }

    void "test installing from github"() {
        when:
        int exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "install", "https://github.com/metridoc/metridoc-job-illiad/archive/master.zip"])

        then:
        0 == exitCode

        when:
        exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "install", "https://github.com/metridoc/metridoc-job-illiad/archive/master.zip"])

        then:
        0 == exitCode
        output.contains("upgrading metridoc-job-illiad")
        new File("${temporaryFolder.root.path}/metridoc-job-illiad-master").exists()
    }

    void "test installing from the current directory"() {
        given:
        baseWorkDir = "src/testJobs/complexJob/metridoc-job-foo-0.1"

        when:
        int exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}","install", "."])

        then:
        0 == exitCode

        when:
        exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "foo"])

        then:
        0 == exitCode
        output.contains("complex foo ran")

        cleanup:
        baseWorkDir = System.getProperty("user.dir")
    }

    void "versioned and unversioned jobs should overrite each other"() {
        when:
        def simpleJobUnversioned = new File("${temporaryFolder.root.path}/metridoc-job-simpleJob")
        def simpleJobVersioned = new File("${temporaryFolder.root.path}/metridoc-job-simpleJob-master")
        int exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "install", "src/testJobs/simpleJob"])

        then:
        0 == exitCode
        simpleJobUnversioned.exists()
        !simpleJobVersioned.exists()

        when:
        exitCode = runCommand(["--jobPath=${temporaryFolder.root.path}", "install", "src/testJobs/simpleJob-master"])

        then:
        0 == exitCode
        !simpleJobUnversioned.exists()
        simpleJobVersioned.exists()

        cleanup:
        simpleJobUnversioned.deleteDir()
        simpleJobVersioned.deleteDir()
    }

    void "test install job with job imports"() {
        setup:
        File jobPath = temporaryFolder.newFolder("jobPath")
        File jobLocation = temporaryFolder.newFolder("metridoc-job-fooImports")
        File importFile = new File(jobLocation, "import.groovy")
        importFile.createNewFile()
        def path = "src/testJobs/complexJob.zip"
        def complexJobPath = new File(path)
        if (!complexJobPath.exists()) {
            complexJobPath = new File("metridoc-job-cli/${path}")
        }
        importFile.text = "foo = [url: '${complexJobPath.toURI().toURL().toString()}', path: 'metridoc-job-gorm']"
        def groovyDir = new File(jobLocation, "src/main/groovy")
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
        int exitCode = runCommand(["--jobPath=${jobPath.canonicalPath}", "install", jobLocation.canonicalPath])

        then:
        def fooImportsDir = new File(jobPath, "metridoc-job-fooImports")
        fooImportsDir.exists()
        fooImportsDir.isDirectory()
        def mdoc = new File(fooImportsDir, ".mdoc")
        mdoc.isDirectory()
        new File(mdoc, "metridoc-job-gorm").exists()

        0 == exitCode
        noExceptionThrown()

        when:
        exitCode = runCommand(["--jobPath=${jobPath.canonicalPath}", "fooImports", "--mergeMetridocConfig=false", "--embeddedDataSource"])

        then:
        0 == exitCode
        noExceptionThrown()
    }

    void "test installing a zip file with a path directly"() {
        setup:
        File jobPath = temporaryFolder.newFolder("jobPath")
        def path = "src/testJobs/complexJob.zip"
        def complexJobPath = new File(path)
        if (!complexJobPath.exists()) {
            path = "metridoc-job-cli/${path}"
        }

        when:
        int exitCode = runCommand(["--jobPath=${jobPath.canonicalPath}", "install", path, "metridoc-job-gorm"])

        then:
        0 == exitCode
        noExceptionThrown()

        1 == jobPath.listFiles().size()
        jobPath.listFiles()[0].name == "metridoc-job-gorm"

    }
}
