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
 * @author Tommy Barker
 */
class ListJobsCommandIntSpec extends AbstractFunctionalSpec {

    void "test list jobs"() {
        when:
        runCommand(["install", "src/testJobs/metridoc-job-bar-0.1.zip"])
        int exitCode = runCommand(["list-jobs"])

        then:
        0 == exitCode
        output.contains("Available Jobs:")
        output.contains(" --> bar (v0.1)")
    }

    void "test installing a job with no version"() {
        when:
        runCommand(["install", "src/testJobs/simpleJob"])
        int exitCode = runCommand(["list-jobs"])

        then:
        0 == exitCode
        output.contains("Available Jobs:")
        output.contains(" --> simpleJob")

        cleanup:
        def home = System.getProperty("user.home")
        new File("$home/.metridoc/jobs/metridoc-job-simpleJob").deleteDir()
    }
}
