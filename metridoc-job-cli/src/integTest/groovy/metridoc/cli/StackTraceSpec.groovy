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
 * Created with IntelliJ IDEA on 8/29/13
 * @author Tommy Barker
 */
class StackTraceSpec extends AbstractFunctionalSpec {

    void "by default just the error message is printed when a job has an error"() {
        when:
        int exitCode = runCommand(["src/testJobs/script/errorScript.groovy"])

        then:
        exitCode == 1
        output.contains("ERROR:")
        output.contains("Caused By")
        !output.contains("15") //Default error message does not have line number
    }

    void "stacktrace flag should be injectable"() {
        when: "stacktrace is an mdoc argument"
        int exitCode = runCommand(["--stacktrace", "src/testJobs/script/injectableStackTrace.groovy"])

        then:
        0 == exitCode
        output.contains("stacktrace is injectable")

        when: "stacktrace is a job argument"
        exitCode = runCommand(["src/testJobs/script/injectableStackTrace.groovy", "--stacktrace"])

        then:
        0 == exitCode
        output.contains("stacktrace is injectable")
    }

}
