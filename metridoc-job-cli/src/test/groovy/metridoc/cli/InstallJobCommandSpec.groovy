/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.
  */

package metridoc.cli

import org.junit.Rule
import org.junit.contrib.java.lang.system.Assertion
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

    void "test cliArgs validation"() {
        when:
        InstallJobCommand.validateCliArgs(["foo", "bar"])

        then:
        noExceptionThrown()

        when:
        InstallJobCommand.validateCliArgs(["foo", "bar", "baz"])

        then:
        noExceptionThrown()

        when:
        InstallJobCommand.validateCliArgs(["foo"])

        then:
        def error = thrown(AssertionError)
        error.message.contains(InstallJobCommand.BAD_CLI_ARGS)
    }

    void "job path must be set"() {
        when:
        def main = new MetridocMain()
        main.jobPath = null
        new InstallJobCommand(main: main).run(emptyOptions)

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
        def path = "src/testJobs/complexJob.zip"
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

    void "test getItemToInstall"() {
        when:
        def toInstall = InstallJobCommand.getItemToInstall("http://foo.com")

        then:
        toInstall instanceof URL

        when:
        File zipFile = temporaryFolder.newFile("bar.zip")
        toInstall = InstallJobCommand.getItemToInstall(zipFile.path)

        then:
        toInstall instanceof File

        when:
        InstallJobCommand.getItemToInstall("garbage")

        then:
        def error = thrown(AssertionError)
        error.message.contains("[garbage] does not exist")
    }

}
