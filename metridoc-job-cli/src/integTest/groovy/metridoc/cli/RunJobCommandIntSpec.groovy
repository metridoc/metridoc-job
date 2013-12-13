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
/**
 * Created with IntelliJ IDEA on 8/16/13
 * @author Tommy Barker
 */
class RunJobCommandIntSpec extends AbstractFunctionalSpec {

    def scriptLocation = "src/testJobs/script/simpleScript.groovy"

    def setup() {
        runCommand(["install", "src/testJobs/metridoc-job-bar-0.1.zip"])
    }

    def cleanup() {
        assert new File("${System.getProperty("user.home")}/.metridoc/jobs/metridoc-job-bar-0.1").deleteDir()
    }

    void "test running a script"() {
        when:
        int exitCode = runCommand([scriptLocation, "--mergeMetridocConfig=false", "--embeddedDataSource"])

        then:
        0 == exitCode
        output.contains("simpleScript ran")
    }

    void "run a complex job"() {
        when:
        int exitCode = runCommand(["bar"])

        then:
        0 == exitCode
        output.contains("bar has run")
    }

    void "run a job with arguments"() {
        when:
        int exitCode = runCommand(["bar", "foo", "bar"])

        then:
        0 == exitCode
        output.contains("bar has args [foo, bar]")
    }

    void "run a job in a directory with non standard root script when in the same directory"() {

        setup:
        baseWorkDir = "src/testJobs/complexJob/metridoc-job-foo-0.1"

        when:
        int exitCode = runCommand(["."])

        then:
        0 == exitCode
        output.contains("complex foo ran")

        cleanup:
        baseWorkDir = System.getProperty("user.dir")
    }

    void "a bad job name should return a reasonable message"() {
        when:
        int exitCode = runCommand(["asdasd"])

        then:
        exitCode > 0
        output.contains("[asdasd] is not a recognized job")
    }

    void "run a simple job from a directory"() {
        when:
        int exitCode = 0
        if (!System.getProperty("os.name").contains("indows")) {
            //this does not build in windows
            exitCode = runCommand(["src/testJobs/simpleJob", "--embeddedDataSource"])
        }

        then:
        0 == exitCode
    }
    
    void "run a remote script"() {
        when:
        int exitCode = runCommand(["https://raw.github.com/metridoc/metridoc-job/master/metridoc-job-cli/src/testJobs/script/simpleScript.groovy", "--embeddedDataSource", "--mergeMetridocConfig=false"])

        then:
        0 == exitCode
        output.contains("simpleScript ran")
    }
    

    void "test job with global properties"() {
        when:
        int exitCode = runCommand(["-logLevel", "debug", "src/testJobs/script/injectionWithGlobalProps.groovy", "-bar=foo", "-foo", "-stacktrace"])

        then:
        0 == exitCode
    }
}
